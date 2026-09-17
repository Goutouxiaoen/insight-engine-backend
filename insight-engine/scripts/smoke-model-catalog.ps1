<#
  Smoke: model gateway -- model catalog CRUD (stage 6, IF 7.2 / 7.3)

  Verifies:
    1) create vendor -> create model under it -> 200, id returned
    2) page returns vendorCode/vendorName (redundant display fields) and **prices as strings** (IF 2.5)
    3) duplicate (vendorId, code) -> 1001
    4) unknown vendorId -> 1001 (no orphan model row)
    5) update: displayName/prices change; vendorId+code NOT changeable (not in the body at all)
    6) delete -> gone from page

  Usage:
    powershell -File scripts/smoke-model-catalog.ps1 -UmsUrl http://localhost:17101 -ModelUrl http://localhost:17103 -AdminPassword <pwd>

  Notes:
    * ASCII only (PowerShell 5.1 mis-decodes UTF-8-without-BOM on zh-CN Windows).
    * The instance under test needs application-local.yml (DB/Redis passwords); KEK not required here
      (this script creates a vendor WITHOUT apiKey on purpose, so the model-catalog path is tested in isolation).
    * Probe rows are logically deleted at the end; hard-delete with SQL if you want a clean table:
        DELETE FROM ie_model WHERE code LIKE 'smoke-model%'; DELETE FROM ie_model_vendor WHERE code LIKE 'smoke-vendor%';
#>
param(
  [string]$UmsUrl = 'http://localhost:7101',
  [string]$ModelUrl = 'http://localhost:7103',
  [string]$AdminAccount = 'admin@example.com',
  [Parameter(Mandatory = $true)][string]$AdminPassword
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("mcat-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$vcode = "smoke-vendor$ts"
$mcode = "smoke-model$ts"

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
$T = (Req 'POST' "$UmsUrl/auth/login" $null (Body 'a.json' $loginJson)).Obj.data.token
if (-not $T) { throw 'admin login failed' }
Write-Host "  login ok"

Write-Host "== 1) setup: vendor (no apiKey) + model =="
$r = Req 'POST' "$ModelUrl/api/v1/model/vendor" $T (Body 'v.json' ('{"code":"' + $vcode + '","name":"SmokeVendor","baseUrl":"http://localhost:11434/v1","type":"CHAT"}'))
$vid = $r.Obj.data
Check 'create vendor (no apiKey -> hasApiKey=false)' ($r.Obj.code -eq 0) ("code=" + $r.Obj.code)
$r = Req 'POST' "$ModelUrl/api/v1/model" $T (Body 'm.json' ('{"vendorId":' + $vid + ',"code":"' + $mcode + '","displayName":"Smoke Model","type":"CHAT","contextWindow":131072,"inputPricePer1k":"0.000800","outputPricePer1k":"0.002000"}'))
$mid = $r.Obj.data
Write-Host ("  model created id={0} (HTTP={1} code={2})" -f $mid, $r.Http, $r.Obj.code)
Check 'create model -> 200' ($r.Obj.code -eq 0) ("code=" + $r.Obj.code)

Write-Host "== 2) page: redundant vendor fields + prices as STRINGS (IF 2.5) =="
$r = Req 'GET' "$ModelUrl/api/v1/model/page?vendorId=$vid&pageNum=1&pageSize=10" $T $null
$row = $r.Obj.data.records[0]
Write-Host ("  vendorCode={0} vendorName={1} priceIn={2} priceOut={3}" -f $row.vendorCode, $row.vendorName, $row.inputPricePer1k, $row.outputPricePer1k)
Check 'vendorCode/vendorName filled' (($row.vendorCode -eq $vcode) -and ($row.vendorName -eq 'SmokeVendor')) ''
Check 'prices serialized as JSON strings' (($r.Raw -match '"inputPricePer1k":"0\.000800"') -and ($r.Raw -match '"outputPricePer1k":"0\.002000"')) 'expect quoted 6-decimal strings'

Write-Host "== 3) negative: duplicate (vendorId, code) + unknown vendor =="
$r = Req 'POST' "$ModelUrl/api/v1/model" $T (Body 'm2.json' ('{"vendorId":' + $vid + ',"code":"' + $mcode + '","type":"CHAT"}'))
Check 'duplicate model code -> 1001' ($r.Obj.code -eq 1001) ("code=" + $r.Obj.code)
$r = Req 'POST' "$ModelUrl/api/v1/model" $T (Body 'm3.json' ('{"vendorId":99999999,"code":"' + $mcode + '-x","type":"CHAT"}'))
Check 'unknown vendorId -> 1001 (no orphan)' ($r.Obj.code -eq 1001) ("code=" + $r.Obj.code)

Write-Host "== 4) update: displayName + price, then verify =="
$r = Req 'PUT' "$ModelUrl/api/v1/model/$mid" $T (Body 'u.json' '{"displayName":"Smoke Model v2","inputPricePer1k":"0.001200","enabled":0}')
Check 'update model -> 200' ($r.Obj.code -eq 0) ("code=" + $r.Obj.code)
$r = Req 'GET' "$ModelUrl/api/v1/model/page?vendorId=$vid&pageNum=1&pageSize=10" $T $null
$row2 = $r.Obj.data.records[0]
Write-Host ("  after update: displayName={0} priceIn={1} enabled={2} code={3}" -f $row2.displayName, $row2.inputPricePer1k, $row2.enabled, $row2.code)
Check 'displayName updated' ($row2.displayName -eq 'Smoke Model v2') ("got " + $row2.displayName)
Check 'inputPricePer1k updated (string)' ($row2.inputPricePer1k -eq '0.001200') ("got " + $row2.inputPricePer1k)
Check 'code unchanged (immutable)' ($row2.code -eq $mcode) ("got " + $row2.code)
Check 'enabled updated to 0' ($row2.enabled -eq 0) ("got " + $row2.enabled)

Write-Host "== 5) cleanup: delete model + vendor =="
$r = Req 'DELETE' "$ModelUrl/api/v1/model/$mid" $T $null
Check 'delete model -> 200' ($r.Obj.code -eq 0) ''
$r = Req 'GET' "$ModelUrl/api/v1/model/page?vendorId=$vid&pageNum=1&pageSize=10" $T $null
Check 'deleted model not listed' ($r.Obj.data.records.Count -eq 0) ("count=" + $r.Obj.data.records.Count)
$r = Req 'DELETE' "$ModelUrl/api/v1/model/vendor/$vid" $T $null
Check 'delete vendor -> 200' ($r.Obj.code -eq 0) ''

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
if ($fail -eq 0) { Write-Host "RESULT: ALL PASS" } else { Write-Host ("RESULT: {0} FAILED" -f $fail); exit 1 }
