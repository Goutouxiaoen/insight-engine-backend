<#
  Smoke: workspace-scoped authorization (layer 2, TD 7.5 / IF 3.0)

  Core assertion: 同一个 token（perms 是跨空间并集）在不同空间的判定结果必须不同：
    - M 在 W1 是 ws_admin（有 member:create）→ 邀请成员 200
    - M 在 W2 是 end_user（无 member:create）→ 邀请成员 403/2006   <- 第二层拦截
    - GET /workspace/{id}/my-permissions 返回的是"该空间权限"，与 token 的并集不同

  Usage:
    powershell -File scripts/smoke-workspace-permission.ps1 -UmsUrl http://localhost:7101 -WsUrl http://localhost:7102 -AdminPassword <pwd>

  Notes:
    * ASCII only (PowerShell 5.1 mis-reads UTF-8 without BOM on zh-CN Windows).
    * Role ids come from init.sql seed: 3 = ws_admin, 5 = end_user.
    * Test users cannot be deleted via API (no such endpoint) -> leftover rows are harmless but
      can be removed with SQL if needed:
        DELETE FROM ie_member WHERE user_id IN (SELECT id FROM ie_user WHERE email LIKE 'smoke-wsperm-%');
        DELETE FROM ie_user   WHERE email LIKE 'smoke-wsperm-%';
#>
param(
  [string]$UmsUrl = 'http://localhost:7101',
  [string]$WsUrl  = 'http://localhost:7102',
  [string]$AdminAccount = 'admin@example.com',
  [Parameter(Mandatory = $true)][string]$AdminPassword
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("wsperm-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

function Decode($jwt) {
  $p = ($jwt -split '\.')[1]
  $p = $p.Replace('-', '+').Replace('_', '/')
  switch ($p.Length % 4) { 2 { $p += '==' } 3 { $p += '=' } }
  return ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json)
}
function Body($n, $j) { $f = Join-Path $tmp $n; [IO.File]::WriteAllText($f, $j, (New-Object Text.UTF8Encoding($false))); return $f }
function Req($m, $u, $tk, $bf) {
  $a = @('-s', '-X', $m, '-o', (Join-Path $tmp 'r.json'), '-w', '%{http_code}')
  if ($tk) { $a += @('-H', "Authorization: Bearer $tk") }
  if ($bf) { $a += @('-H', 'Content-Type: application/json', '-d', "@$bf") }
  $a += $u
  $http = (& curl.exe @a | Out-String).Trim()
  $raw = [IO.File]::ReadAllText((Join-Path $tmp 'r.json'), [Text.Encoding]::UTF8)
  $o = $null; try { $o = $raw | ConvertFrom-Json } catch { }
  [PSCustomObject]@{ Http = $http; Obj = $o }
}
function Check($name, $ok, $detail) {
  if ($ok) { Write-Host ("  PASS  {0}  {1}" -f $name, $detail) } else { $script:fail++; Write-Host ("  FAIL  {0}  {1}" -f $name, $detail) }
}

Write-Host "== 0) admin login =="
$r = Req 'POST' "$UmsUrl/auth/login" $null (Body 'a.json' ("{""account"":""$AdminAccount"",""password"":""$AdminPassword""}"))
$TAdmin = $r.Obj.data.token
if (-not $TAdmin) { throw "admin login failed: $($r.Obj.message)" }
Write-Host ("  admin perms={0} ws_id={1}" -f (Decode $TAdmin).perms.Count, (Decode $TAdmin).ws_id)

Write-Host "== 1) create two workspaces =="
$r = Req 'POST' "$WsUrl/api/v1/workspace" $TAdmin (Body 'w1.json' ("{""orgId"":1,""name"":""ScopeA"",""code"":""scope-a-$ts""}"))
$W1 = $r.Obj.data
$r = Req 'POST' "$WsUrl/api/v1/workspace" $TAdmin (Body 'w2.json' ("{""orgId"":1,""name"":""ScopeB"",""code"":""scope-b-$ts""}"))
$W2 = $r.Obj.data
if (-not $W1 -or -not $W2) { throw 'create workspace failed' }
Write-Host ("  W1={0} W2={1}" -f $W1, $W2)

Write-Host "== 2) create users M (under test) and N (invite target) =="
$mailM = "smoke-wsperm-$ts-m@example.com"; $mailN = "smoke-wsperm-$ts-n@example.com"
Req 'POST' "$UmsUrl/api/v1/user" $TAdmin (Body 'um.json' ("{""email"":""$mailM"",""nickname"":""WsPermM"",""password"":""Passw0rd123"",""roleId"":5}")) | Out-Null
Req 'POST' "$UmsUrl/api/v1/user" $TAdmin (Body 'un.json' ("{""email"":""$mailN"",""nickname"":""WsPermN"",""password"":""Passw0rd123"",""roleId"":5}")) | Out-Null
Write-Host ("  M={0} N={1}" -f $mailM, $mailN)

Write-Host "== 3) M joins W1 as ws_admin(3) and W2 as end_user(5) =="
$r = Req 'POST' "$WsUrl/api/v1/member/invite" $TAdmin (Body 'i1.json' ("{""workspaceId"":$W1,""email"":""$mailM"",""roleId"":3}"))
Write-Host ("  invite M->W1: HTTP={0} code={1}" -f $r.Http, $r.Obj.code)
$r = Req 'POST' "$WsUrl/api/v1/member/invite" $TAdmin (Body 'i2.json' ("{""workspaceId"":$W2,""email"":""$mailM"",""roleId"":5}"))
Write-Host ("  invite M->W2: HTTP={0} code={1}" -f $r.Http, $r.Obj.code)

Write-Host "== 4) M login: token perms is the UNION across spaces =="
$r = Req 'POST' "$UmsUrl/auth/login" $null (Body 'm.json' ("{""account"":""$mailM"",""password"":""Passw0rd123""}"))
$TM = $r.Obj.data.token
if (-not $TM) { throw "M login failed: $($r.Obj.message)" }
$PM = Decode $TM
Write-Host ("  M roles={0} perms={1} has_member:create={2}" -f ($PM.roles -join ','), $PM.perms.Count, ($PM.perms -contains 'member:create'))
Check 'M token carries member:create (union)' ($PM.perms -contains 'member:create') 'so @PreAuthorize passes; layer 2 decides'

Write-Host "== 5) M switches to W1 (ws_admin there) =="
$r = Req 'POST' "$WsUrl/api/v1/workspace/switch" $TM (Body 's1.json' ("{""workspaceId"":$W1}"))
$TM1 = $r.Obj.data.token
if (-not $TM1) { throw "switch failed: $($r.Obj.message)" }
$r = Req 'GET' "$WsUrl/api/v1/workspace/$W1/my-permissions" $TM1 $null
Write-Host ("  my-permissions W1: roles={0} perms={1}" -f ($r.Obj.data.roles -join ','), $r.Obj.data.permissions.Count)
Check 'my-permissions W1 = ws_admin scope' ($r.Obj.data.permissions.Count -eq 27) ("count={0}" -f $r.Obj.data.permissions.Count)

Write-Host "== 6) M invites N into W1 -> layer 2 allows =="
$r = Req 'POST' "$WsUrl/api/v1/member/invite" $TM1 (Body 'inv1.json' ("{""workspaceId"":$W1,""email"":""$mailN"",""roleId"":5}"))
Write-Host ("  HTTP={0} code={1} msg={2}" -f $r.Http, $r.Obj.code, $r.Obj.message)
Check 'invite into W1 (M is ws_admin) -> 200' ($r.Obj.code -eq 0) ''

Write-Host "== 7) M invites N into W2 -> LAYER 2 must reject =="
$r = Req 'POST' "$WsUrl/api/v1/member/invite" $TM1 (Body 'inv2.json' ("{""workspaceId"":$W2,""email"":""$mailN"",""roleId"":5}"))
Write-Host ("  HTTP={0} code={1} msg={2}" -f $r.Http, $r.Obj.code, $r.Obj.message)
Check 'invite into W2 (M is end_user) -> 403/2006' (($r.Http -eq '403') -and ($r.Obj.code -eq 2006)) ("HTTP={0} code={1}" -f $r.Http, $r.Obj.code)

Write-Host "== 8) my-permissions of W2 differs from token perms =="
$r = Req 'GET' "$WsUrl/api/v1/workspace/$W2/my-permissions" $TM1 $null
Write-Host ("  my-permissions W2: roles={0} perms={1}" -f ($r.Obj.data.roles -join ','), $r.Obj.data.permissions.Count)
Check 'my-permissions W2 = end_user scope' (($r.Obj.data.permissions.Count -eq 7) -and -not ($r.Obj.data.permissions -contains 'member:create')) ("count={0}" -f $r.Obj.data.permissions.Count)

Write-Host "== 9) cleanup: delete both test workspaces with ADMIN token =="
foreach ($w in @($W1, $W2)) {
  $r = Req 'DELETE' "$WsUrl/api/v1/workspace/$w" $TAdmin $null
  Write-Host ("  delete ws {0}: HTTP={1} code={2}" -f $w, $r.Http, $r.Obj.code)
  Check ("delete ws {0} -> 200" -f $w) ($r.Obj.code -eq 0) ("HTTP={0}" -f $r.Http)
}

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "(test users M/N remain; no user-delete API -- see header notes for the SQL cleanup)"
if ($fail -eq 0) { Write-Host "RESULT: ALL PASS" } else { Write-Host ("RESULT: {0} FAILED" -f $fail); exit 1 }
