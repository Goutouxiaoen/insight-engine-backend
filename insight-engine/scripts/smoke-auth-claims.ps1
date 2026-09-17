<#
  Cross-entry consistency smoke test for JWT claims (see IF 3.0 claim table).
  Required by AGENTS rule 6: every issuing entry of the same semantic must be verified.

  Checks:
    1) login / refresh / switch-workspace -> roles and perms MUST be identical
    2) refresh MUST keep the current ws_id (not fall back to the default workspace)  <- fixed 2026-09-17
    3) switch -> ws_id = target workspace
    4) delete the CURRENT workspace -> 403/1003 (business guard reachable)
    5) old access token becomes invalid right after switch -> 401/2001

  Usage (pass the password as a parameter; never hardcode it in this file):
    powershell -File scripts/smoke-auth-claims.ps1 -UmsUrl http://localhost:7101 -WsUrl http://localhost:7102 -Password <your-pwd>
  Tip: to go through the gateway, point both URLs at http://localhost:7000.

  NOTE: this file is intentionally ASCII-only. PowerShell 5.1 reads UTF-8 files without BOM as
  ANSI(GBK) on zh-CN Windows, which garbles non-ASCII text and breaks string parsing.
#>
param(
  [string]$UmsUrl = 'http://localhost:7101',
  [string]$WsUrl  = 'http://localhost:7102',
  [string]$Account = 'admin@example.com',
  [Parameter(Mandatory = $true)][string]$Password
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("auth-claims-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0

function Decode($jwt) {
  $p = ($jwt -split '\.')[1]
  $p = $p.Replace('-', '+').Replace('_', '/')
  switch ($p.Length % 4) { 2 { $p += '==' } 3 { $p += '=' } }
  return ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json)
}
function Body($name, $json) {
  $f = Join-Path $tmp $name
  [IO.File]::WriteAllText($f, $json, (New-Object Text.UTF8Encoding($false)))
  return $f
}
function Req($method, $url, $token, $bodyFile) {
  $a = @('-s', '-X', $method, '-o', (Join-Path $tmp 'resp.json'), '-w', '%{http_code}')
  if ($token) { $a += @('-H', "Authorization: Bearer $token") }
  if ($bodyFile) { $a += @('-H', 'Content-Type: application/json', '-d', "@$bodyFile") }
  $a += $url
  $http = (& curl.exe @a | Out-String).Trim()
  $raw = [IO.File]::ReadAllText((Join-Path $tmp 'resp.json'), [Text.Encoding]::UTF8)
  $o = $null; try { $o = $raw | ConvertFrom-Json } catch { }
  [PSCustomObject]@{ Http = $http; Obj = $o }
}
function Check($name, $ok, $detail) {
  if ($ok) { Write-Host ("  PASS  {0}  {1}" -f $name, $detail) }
  else { $script:fail++; Write-Host ("  FAIL  {0}  {1}" -f $name, $detail) }
}
function Same($a, $b) { return (($a -join ',') -eq ($b -join ',')) }

Write-Host "== 1) login =="
$r = Req 'POST' "$UmsUrl/auth/login" $null (Body 'login.json' ("{""account"":""$Account"",""password"":""$Password""}"))
$tLogin = $r.Obj.data.token
$rLogin = $r.Obj.data.refreshToken
if (-not $tLogin) { throw "login failed: HTTP=$($r.Http) $($r.Obj.message)" }
$pLogin = Decode $tLogin
Write-Host ("  roles={0} perms={1} ws_id={2}" -f ($pLogin.roles -join ','), $pLogin.perms.Count, $pLogin.ws_id)

Write-Host "== 2) refresh (must keep the same ws_id) =="
$r = Req 'POST' "$UmsUrl/auth/refresh" $null (Body 'refresh.json' ("{""refreshToken"":""$rLogin""}"))
$tRefresh = $r.Obj.data.token
if (-not $tRefresh) { throw "refresh failed: HTTP=$($r.Http) $($r.Obj.message)" }
$pRefresh = Decode $tRefresh
Write-Host ("  roles={0} perms={1} ws_id={2}" -f ($pRefresh.roles -join ','), $pRefresh.perms.Count, $pRefresh.ws_id)
Check 'login vs refresh: roles identical' (Same $pLogin.roles $pRefresh.roles) ''
Check 'login vs refresh: perms identical' (Same $pLogin.perms $pRefresh.perms) ''
Check 'login vs refresh: ws_id kept' ($pLogin.ws_id -eq $pRefresh.ws_id) ("ws_id {0} -> {1}" -f $pLogin.ws_id, $pRefresh.ws_id)

Write-Host "== 3) switch workspace (only ws_id changes) =="
$code = "claim-check-" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$r = Req 'POST' "$WsUrl/api/v1/workspace" $tRefresh (Body 'cw.json' ("{""orgId"":1,""name"":""ClaimCheck"",""code"":""$code""}"))
$ws = $r.Obj.data
if (-not $ws) { throw "create workspace failed: HTTP=$($r.Http) $($r.Obj.message)" }
# NOTE: creating the workspace adds a membership (creator -> ws_admin), which legitimately changes the
# user's role set. So take a FRESH baseline AFTER that change, then compare switch against it.
$r = Req 'POST' "$UmsUrl/auth/login" $null (Body 'login2.json' ("{""account"":""$Account"",""password"":""$Password""}"))
$tBase = $r.Obj.data.token
if (-not $tBase) { throw "re-login failed: HTTP=$($r.Http) $($r.Obj.message)" }
$pBase = Decode $tBase
Write-Host ("  baseline(after create): roles={0} perms={1} ws_id={2}" -f ($pBase.roles -join ','), $pBase.perms.Count, $pBase.ws_id)
$r = Req 'POST' "$WsUrl/api/v1/workspace/switch" $tBase (Body 'sw.json' ("{""workspaceId"":$ws}"))
$tSwitch = $r.Obj.data.token
$rSwitch = $r.Obj.data.refreshToken
if (-not $tSwitch) { throw "switch failed: HTTP=$($r.Http) $($r.Obj.message)" }
$pSwitch = Decode $tSwitch
Write-Host ("  after switch: roles={0} perms={1} ws_id={2}" -f ($pSwitch.roles -join ','), $pSwitch.perms.Count, $pSwitch.ws_id)
Check 'baseline vs switch: roles identical' (Same $pBase.roles $pSwitch.roles) ''
Check 'baseline vs switch: perms identical' (Same $pBase.perms $pSwitch.perms) ''
Check 'switch: ws_id = target' ($pSwitch.ws_id -eq $ws) ("ws_id={0} target={1}" -f $pSwitch.ws_id, $ws)
Check 'old access token rejected after switch' ((Req 'GET' "$WsUrl/api/v1/workspace/page?pageNum=1&pageSize=1" $tBase $null).Obj.code -eq 2001) 'expect 2001'

Write-Host "== 4) refresh AFTER switch (must stay in the switched workspace) =="
$r = Req 'POST' "$UmsUrl/auth/refresh" $null (Body 'refresh2.json' ("{""refreshToken"":""$rSwitch""}"))
$tRefresh3 = $r.Obj.data.token
if (-not $tRefresh3) { throw "refresh after switch failed: HTTP=$($r.Http) $($r.Obj.message)" }
$pRefresh3 = Decode $tRefresh3
Write-Host ("  ws_id={0} perms={1}" -f $pRefresh3.ws_id, $pRefresh3.perms.Count)
Check 'refresh after switch: ws_id still target' ($pRefresh3.ws_id -eq $ws) ("ws_id={0} target={1}" -f $pRefresh3.ws_id, $ws)
Check 'refresh after switch: perms identical' (Same $pBase.perms $pRefresh3.perms) ''

Write-Host "== 5) delete CURRENT workspace (business guard reachable) =="
$r = Req 'DELETE' "$WsUrl/api/v1/workspace/$ws" $tRefresh3 $null
Write-Host ("  HTTP={0} code={1} msg={2}" -f $r.Http, $r.Obj.code, $r.Obj.message)
Check 'delete current workspace -> 1003' ($r.Obj.code -eq 1003) 'expect 1003'

Write-Host "== 6) cleanup: switch back then delete temp workspace =="
$r = Req 'POST' "$WsUrl/api/v1/workspace/switch" $tRefresh3 (Body 'sw1.json' '{"workspaceId":1}')
$tBack = $r.Obj.data.token
$r = Req 'DELETE' "$WsUrl/api/v1/workspace/$ws" $tBack $null
Check 'delete non-current workspace -> 200' ($r.Http -eq '200') ("HTTP={0}" -f $r.Http)

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
if ($fail -eq 0) { Write-Host "RESULT: ALL PASS" }
else { Write-Host ("RESULT: {0} FAILED" -f $fail); exit 1 }
