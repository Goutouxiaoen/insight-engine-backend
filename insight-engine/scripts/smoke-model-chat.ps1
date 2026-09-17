<#
  Smoke: model gateway -- chat completions (stage 6, IF 7.5)

  Verifies (against the REAL provider, so it costs a few tokens):
    1) non-stream  : POST /api/v1/model/chat/completions {model:"qwen3.7-plus", stream:false}
                     -> code=0, choices[0].message.content non-empty, usage.totalTokens > 0
    2) auto model  : {model:"auto"} -> server resolves a concrete model (returned in data.model)
    3) stream (SSE): {stream:true} -> >=1 "event: message" frame, exactly one "event: finish"
                     carrying usage, assembled content non-empty
    4) negatives   : unknown model -> 3001 (MODEL_NOT_FOUND)
  Then check the ledger (separate command, needs DB access):
    select id, scope_type, scope_id, biz_type, ref_id, quantity, trace_id, created_at
      from ie_usage_record order by id desc limit 5;

  Usage:
    powershell -File scripts/smoke-model-chat.ps1 -UmsUrl http://localhost:17101 -ModelUrl http://localhost:17103 -AdminPassword <pwd>

  Notes:
    * ASCII only (PowerShell 5.1 mis-decodes UTF-8-without-BOM on zh-CN Windows).
    * Requires: vendor has an encrypted apiKey (hasApiKey=true) + an enabled CHAT model.
    * heartbeat (15s) will NOT appear in a short stream -- it is asserted only when the stream lasts >15s.
#>
param(
  [string]$UmsUrl = 'http://localhost:7101',
  [string]$ModelUrl = 'http://localhost:7103',
  [string]$AdminAccount = 'admin@example.com',
  [string]$Model = 'qwen3.7-plus',
  [Parameter(Mandatory = $true)][string]$AdminPassword
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("mchat-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0

function Body($n, $j) { $f = Join-Path $tmp $n; [IO.File]::WriteAllText($f, $j, (New-Object Text.UTF8Encoding($false))); return $f }
function Req($m, $u, $tk, $bf) {
  $a = @('-s', '-X', $m, '-o', (Join-Path $tmp 'r.json'), '-w', '%{http_code}', '--max-time', '90')
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

Write-Host "== 1) non-stream chat (real call) =="
$r = Req 'POST' "$ModelUrl/api/v1/model/chat/completions" $T (Body 'c1.json' ('{"model":"' + $Model + '","messages":[{"role":"user","content":"reply with: ok"}],"stream":false,"maxTokens":32}'))
$content = $r.Obj.data.choices[0].message.content
$usage = $r.Obj.data.usage
Write-Host ("  HTTP={0} code={1} model={2} content='{3}'" -f $r.Http, $r.Obj.code, $r.Obj.data.model, $content)
Write-Host ("  usage: prompt={0} completion={1} total={2}" -f $usage.promptTokens, $usage.completionTokens, $usage.totalTokens)
Check 'non-stream -> code=0' ($r.Obj.code -eq 0) ("code=" + $r.Obj.code)
Check 'content non-empty' ([bool]$content) ''
Check 'usage.totalTokens > 0' ($usage.totalTokens -gt 0) ("total=" + $usage.totalTokens)

Write-Host "== 2) auto model resolves to a concrete model =="
$r = Req 'POST' "$ModelUrl/api/v1/model/chat/completions" $T (Body 'c2.json' '{"model":"auto","messages":[{"role":"user","content":"say: hi"}],"stream":false,"maxTokens":16}')
Write-Host ("  HTTP={0} code={1} resolved model={2}" -f $r.Http, $r.Obj.code, $r.Obj.data.model)
Check 'auto -> code=0 and model resolved (not literal "auto")' (($r.Obj.code -eq 0) -and ($r.Obj.data.model -ne 'auto')) ''

Write-Host "== 3) stream (SSE) =="
$sseRaw = (& curl.exe -s -N --max-time 90 -X POST "$ModelUrl/api/v1/model/chat/completions" -H "Authorization: Bearer $T" -H 'Content-Type: application/json' -d "@$(Body 'c3.json' ('{"model":"' + $Model + '","messages":[{"role":"user","content":"count 1 to 5"}],"stream":true,"maxTokens":64}'))" | Out-String)
$lines = $sseRaw -split "`n"
$msgCount = 0; $finishCount = 0; $heartbeatCount = 0; $assembled = ''
$finishUsage = $null
for ($i = 0; $i -lt $lines.Count; $i++) {
  $line = $lines[$i].TrimEnd()
  if ($line -eq 'event: message') { $msgCount++ ; if ($lines[$i + 1] -match '^data: (.*)$') { try { $assembled += (($Matches[1] | ConvertFrom-Json).delta.content) } catch { } } }
  elseif ($line -eq 'event: finish') { $finishCount++; if ($lines[$i + 1] -match '^data: (.*)$') { try { $finishUsage = ($Matches[1] | ConvertFrom-Json).usage } catch { } } }
  elseif ($line -eq 'event: heartbeat') { $heartbeatCount++ }
}
Write-Host ("  frames: message={0} finish={1} heartbeat={2}  assembled='{3}'" -f $msgCount, $finishCount, $heartbeatCount, $assembled)
Check 'SSE message frames >= 1' ($msgCount -ge 1) ("msgs=" + $msgCount)
Check 'exactly one finish frame' ($finishCount -eq 1) ("finish=" + $finishCount)
Check 'finish carries usage' ($null -ne $finishUsage) ''
Check 'assembled content non-empty' ([bool]$assembled) ("len=" + $assembled.Length)
if ($msgCount -gt 0 -and $heartbeatCount -eq 0) { Write-Host "  note: heartbeat not observed (stream shorter than 15s) -- expected" }

Write-Host "== 4) negative: unknown model =="
$r = Req 'POST' "$ModelUrl/api/v1/model/chat/completions" $T (Body 'c4.json' '{"model":"no-such-model-xyz","messages":[{"role":"user","content":"hi"}],"stream":false}')
Write-Host ("  HTTP={0} code={1} msg={2}" -f $r.Http, $r.Obj.code, $r.Obj.message)
Check 'unknown model -> 3001' ($r.Obj.code -eq 3001) ("code=" + $r.Obj.code)

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
if ($fail -eq 0) { Write-Host "RESULT: ALL PASS" } else { Write-Host ("RESULT: {0} FAILED" -f $fail); exit 1 }
