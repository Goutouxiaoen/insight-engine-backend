<#
  Smoke: model gateway -- vendor CRUD + API key encryption path (stage 6, IF 7.1)

  Verifies (the security-critical parts in bold):
    1) POST /api/v1/model/vendor with apiKey -> 200, and the response/page **never contains the plaintext key**;
    2) page returns maskedHint (sk-****xxxx) + hasApiKey=true;
    3) PUT without apiKey -> **key unchanged** (secretId stays the same, maskedHint unchanged);
    4) PUT with a new apiKey -> maskedHint/cipher updated, secretId unchanged;
    5) DELETE -> gone from page;
    6) negative: duplicate code -> 1001; unknown id -> 1004.

  DB check is done separately (this script only speaks HTTP):
    select id, masked_hint, left(cipher_text,12) as cipher_head, iv, algo, kek_version from ie_secret where id = <secretId>;

  Usage:
    powershell -File scripts/smoke-model-vendor.ps1 -UmsUrl http://localhost:17101 -ModelUrl http://localhost:17103 -AdminPassword <pwd>

  Notes:
    * ASCII only (PowerShell 5.1 mis-decodes UTF-8-without-BOM on zh-CN Windows).
    * The instance under test must have INSIGHT_SECRET_KEK set, otherwise key writes fail by design (9999).
    * Probe rows are deleted at the end (vendor row logical-deleted; ie_secret row is left for audit by design --
      hard-delete it with SQL if you want a clean table: DELETE FROM ie_secret WHERE name LIKE 'SmokeVendor%').
#>
param(
  [string]$UmsUrl = 'http://localhost:7101',
  [string]$ModelUrl = 'http://localhost:7103',
  [string]$AdminAccount = 'admin@example.com',
  [Parameter(Mandatory = $true)][string]$AdminPassword
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("mvend-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$code = "smoke$ts"

function Body($n, $j) { $f = Join-Path $tmp $n; [IO.File]::WriteAllText($f, $j, (New-Object Text.UTF8Encoding($false))); return $f }
function Req($m, $u, $tk, $bf) {
  $a = @('-s', '-X', $m, '-o', (Join-Path $tmp 'r.json'), '-w', '%{http_code}', '--max-time', '30')
  if ($tk) { $a += @('-H', "Authorization: Bearer $tk") }
  if ($bf) { $a += @('-H', 'Content-Type: application/json', '-d', "@$bf") }
  $a += $u
  $http = (& curl.exe @a | Out-String).Trim()
  $raw = [IO.File]::ReadAllText((Join-Path $tmp 'r.json'), (New-Object Text.UTF8Encoding($false)))
  $o = $null; try { $o = $raw | ConvertFrom-Json } catch { }
  [PSCustomObject]@{ Http = $http; Obj = $o; Raw = $raw }
}
function Check($name, $ok, $detail) {
  if ($ok) { Write-Host ("  PASS  {0}  {1}" -f $name, $detail) } else { $script:fail++; Write-Host ("  FAIL  {0}  {1}" -f $name, $detail) }
}

Write-Host "== 0) admin login =="
$loginJson = '{"account":"' + $AdminAccount + '","password":"' + $AdminPassword + '"}'
$r = Req 'POST' "$UmsUrl/auth/login" $null (Body 'a.json' $loginJson)
$T = $r.Obj.data.token
if (-not $T) { throw "admin login failed: $($r.Obj.message)" }
Write-Host "  login ok"

$plain = "sk-smoke-$ts-ABCD7890"

Write-Host "== 1) create vendor with apiKey (plaintext must never come back) =="
$r = Req 'POST' "$ModelUrl/api/v1/model/vendor" $T (Body 'c.json' ('{"code":"' + $code + '","name":"SmokeVendor' + $ts + '","baseUrl":"https://dashscope.aliyuncs.com/compatible-mode/v1","apiKey":"' + $plain + '","type":"CHAT"}'))
Write-Host ("  HTTP={0} code={1} id={2}" -f $r.Http, $r.Obj.code, $r.Obj.data)
$vid = $r.Obj.data
Check 'create vendor -> 200' ($r.Obj.code -eq 0) ''
Check 'create response does NOT leak plaintext key' (-not ($r.Raw -like "*$plain*")) ''

Write-Host "== 2) page -> maskedHint + hasApiKey =="
$r = Req 'GET' "$ModelUrl/api/v1/model/vendor/page?keyword=$code&pageNum=1&pageSize=10" $T $null
$row = $r.Obj.data.records[0]
Write-Host ("  maskedHint={0} hasApiKey={1} type={2} enabled={3}" -f $row.maskedHint, $row.hasApiKey, $row.type, $row.enabled)
Check 'page does NOT leak plaintext key' (-not ($r.Raw -like "*$plain*")) ''
Check 'maskedHint format = sk-****7890' ($row.maskedHint -eq 'sk-****7890') ("got " + $row.maskedHint)
Check 'hasApiKey = true' ($row.hasApiKey -eq $true) ''

Write-Host "== 3) update WITHOUT apiKey -> key must stay unchanged =="
$r = Req 'PUT' "$ModelUrl/api/v1/model/vendor/$vid" $T (Body 'u1.json' '{"name":"SmokeVendorRenamed"}')
Write-Host ("  HTTP={0} code={1}" -f $r.Http, $r.Obj.code)
$r = Req 'GET' "$ModelUrl/api/v1/model/vendor/page?keyword=$code&pageNum=1&pageSize=10" $T $null
$row2 = $r.Obj.data.records[0]
Check 'name updated' ($row2.name -eq 'SmokeVendorRenamed') ("got " + $row2.name)
Check 'maskedHint unchanged (key untouched)' ($row2.maskedHint -eq 'sk-****7890') ("got " + $row2.maskedHint)

Write-Host "== 4) update WITH new apiKey -> mask changes, same secret row =="
$r = Req 'PUT' "$ModelUrl/api/v1/model/vendor/$vid" $T (Body 'u2.json' ('{"apiKey":"sk-rotated-' + $ts + '-WXYZ4321"}'))
Write-Host ("  HTTP={0} code={1}" -f $r.Http, $r.Obj.code)
$r = Req 'GET' "$ModelUrl/api/v1/model/vendor/page?keyword=$code&pageNum=1&pageSize=10" $T $null
$row3 = $r.Obj.data.records[0]
Check 'maskedHint rotated' ($row3.maskedHint -eq 'sk-****4321') ("got " + $row3.maskedHint)

Write-Host "== 5) negative cases =="
$r = Req 'POST' "$ModelUrl/api/v1/model/vendor" $T (Body 'd.json' ('{"code":"' + $code + '","name":"dup","baseUrl":"https://x","type":"CHAT"}'))
Check 'duplicate code -> 1001' ($r.Obj.code -eq 1001) ("code=" + $r.Obj.code)
$r = Req 'PUT' "$ModelUrl/api/v1/model/vendor/99999999" $T (Body 'u3.json' '{"name":"nope"}')
Check 'update unknown id -> 1004' ($r.Obj.code -eq 1004) ("code=" + $r.Obj.code)

Write-Host "== 6) delete =="
$r = Req 'DELETE' "$ModelUrl/api/v1/model/vendor/$vid" $T $null
Check 'delete -> 200' ($r.Obj.code -eq 0) ''
$r = Req 'GET' "$ModelUrl/api/v1/model/vendor/page?keyword=$code&pageNum=1&pageSize=10" $T $null
Check 'deleted vendor no longer listed' ($r.Obj.data.records.Count -eq 0) ("count=" + $r.Obj.data.records.Count)

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
if ($fail -eq 0) { Write-Host "RESULT: ALL PASS" } else { Write-Host ("RESULT: {0} FAILED" -f $fail); exit 1 }
