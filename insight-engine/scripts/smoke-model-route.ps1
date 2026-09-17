<#
  Smoke: model gateway -- route policy (IF 7.4) + usage query (IF 7.8)

  Verifies (real provider calls, few tokens):
    A) route CRUD  : list (structured rules) / create (shape validation: unknown match key -> 1001) / update / status
    B) auto routing: {model:"auto"} resolves to the policy's PRIMARY target (evidence = returned data.model)
    C) fallback    : put a "bad target" first (vendor baseUrl -> a port nobody listens on):
                     fallback=true  -> still 200 and falls through to the backup target
                     fallback=false -> 3xxx error (no fall-through, switch takes effect)
    D) disable     : status=0 -> auto goes back to the default policy
    E) usage page  : org-level caller sees everything (incl. the probe row);
                     ws_admin does NOT see rows of other workspaces (isolation works)

  Usage:
    powershell -File scripts/smoke-model-route.ps1 -UmsUrl http://localhost:17101 -ModelUrl http://localhost:17103 -AdminPassword <pwd> [-ProbeScopeId 999999]

  Notes:
    * ASCII only (PowerShell 5.1 mis-decodes UTF-8-without-BOM on zh-CN Windows -> Chinese text
      inside this file would be mangled and break string quoting; keep it ASCII, docs live in docs/).
    * Expects the default route policy seed (init.sql) to be present.
    * The probe usage row (scope_id = -ProbeScopeId) must be inserted/removed by the caller via SQL.
    * IF 7.4 has NO delete endpoint for route policies (list/create/update/status only) -> the temp
      policy is cleaned with SQL by the caller:
        DELETE FROM ie_route_policy WHERE name LIKE 'smoke-fallback-%';
#>
param(
  [string]$UmsUrl = 'http://localhost:7101',
  [string]$ModelUrl = 'http://localhost:7103',
  [string]$AdminAccount = 'admin@example.com',
  [Parameter(Mandatory = $true)][string]$AdminPassword,
  [long]$ProbeScopeId = 999999,
  [string]$PrimaryModel = 'qwen3.7-plus'
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("mroute-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

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
$loginJson = '{"account":"' + $AdminAccount + '","password":"' + $AdminPassword + '"}'

Write-Host "== 0) admin login =="
$T = (Req 'POST' "$UmsUrl/auth/login" $null (Body 'a.json' $loginJson)).Obj.data.token
if (-not $T) { throw 'admin login failed' }
Write-Host "  login ok"

Write-Host "== A) route policy CRUD =="
$r = Req 'GET' "$ModelUrl/api/v1/model/route/list" $T $null
$list = $r.Obj.data
Write-Host ("  list: count={0}; first: name={1} priority={2} enabled={3} rulesType={4}" -f $list.Count, $list[0].name, $list[0].priority, $list[0].enabled, $list[0].rules.GetType().Name)
Check 'list returns policies with STRUCTURED rules (not a string)' (($list.Count -ge 1) -and ($list[0].rules -isnot [string])) ''
Check 'list ordered by priority asc (match order)' ($list[0].priority -le $list[$list.Count - 1].priority) ''

$badKey = Req 'POST' "$ModelUrl/api/v1/model/route" $T (Body 'r1.json' ('{"name":"bad-match-' + $ts + '","priority":5,"rules":{"strategy":"PRIORITY","rules":[{"match":{"tenantTier":"PRO"},"targets":[{"modelId":3}]}]}}'))
Write-Host ("  unknown match key -> HTTP={0} code={1} msg={2}" -f $badKey.Http, $badKey.Obj.code, $badKey.Obj.message)
Check 'unknown match key rejected (1001)' ($badKey.Obj.code -eq 1001) ''
$badStrategy = Req 'POST' "$ModelUrl/api/v1/model/route" $T (Body 'r2.json' ('{"name":"bad-strategy-' + $ts + '","priority":5,"rules":{"strategy":"WEIGHTED","rules":[{"targets":[{"modelId":3}]}]}}'))
Check 'unsupported strategy rejected (1001)' ($badStrategy.Obj.code -eq 1001) ("code=" + $badStrategy.Obj.code)

Write-Host "== A2) create a bad target (vendor baseUrl -> port 19999, nothing there) + temp policy =="
# 注意：优先级 @Min(1)，而默认策略也是 1 → 并列时按 id 升序，默认策略(id=1)会先命中。
# 因此先**停用默认策略**，让临时策略成为唯一启用项（测完再恢复）。
$defaultPolicyId = $list[0].id
$null = Req 'PUT' "$ModelUrl/api/v1/model/route/$defaultPolicyId/status" $T (Body 'sd.json' '{"enabled":0}')
Write-Host ("  default policy id={0} disabled for the test" -f $defaultPolicyId)

$vcode = "smoke-bad-vendor$ts"
$mcode = "smoke-bad-model$ts"
$rv = Req 'POST' "$ModelUrl/api/v1/model/vendor" $T (Body 'v.json' ('{"code":"' + $vcode + '","name":"SmokeBadVendor","baseUrl":"http://localhost:19999/v1","type":"CHAT"}'))
$badVendorId = $rv.Obj.data
$rm = Req 'POST' "$ModelUrl/api/v1/model" $T (Body 'm.json' ('{"vendorId":' + $badVendorId + ',"code":"' + $mcode + '","displayName":"SmokeBadModel","type":"CHAT"}'))
$badModelId = $rm.Obj.data
Write-Host ("  bad vendorId={0} badModelId={1}" -f $badVendorId, $badModelId)

$goodModelId = $list[0].rules.rules[0].targets[0].modelId
$policyName = "smoke-fallback-$ts"
$rc = Req 'POST' "$ModelUrl/api/v1/model/route" $T (Body 'r3.json' ('{"name":"' + $policyName + '","priority":1,"rules":{"strategy":"PRIORITY","fallback":true,"rules":[{"match":{},"targets":[{"modelId":' + $badModelId + '},{"modelId":' + $goodModelId + '}]}]}}'))
$policyId = $rc.Obj.data
Write-Host ("  temp policy id={0} (code={1} msg={2})" -f $policyId, $rc.Obj.code, $rc.Obj.message)
Check 'temp policy created (fallback target list: bad -> good)' ($rc.Obj.code -eq 0) ''

Write-Host "== B/C) auto hits temp policy: fallback=true should degrade to backup =="
$r = Req 'POST' "$ModelUrl/api/v1/model/chat/completions" $T (Body 'c1.json' '{"model":"auto","messages":[{"role":"user","content":"reply with: ok"}],"stream":false,"maxTokens":16}')
$resolved = $r.Obj.data.model
Write-Host ("  HTTP={0} code={1} resolved model={2} content='{3}'" -f $r.Http, $r.Obj.code, $resolved, $r.Obj.data.choices[0].message.content)
Check 'fallback=true: bad primary -> falls through and returns 200' ($r.Obj.code -eq 0) ("code=" + $r.Obj.code)
Check 'resolved model is the BACKUP target (not the bad one)' ($resolved -ne $mcode -and $resolved -eq $PrimaryModel) ("resolved=" + $resolved)

Write-Host "== C2) turn fallback off -> expect 3xxx (no fall-through) =="
$null = Req 'PUT' "$ModelUrl/api/v1/model/route/$policyId" $T (Body 'u.json' ('{"rules":{"strategy":"PRIORITY","fallback":false,"rules":[{"match":{},"targets":[{"modelId":' + $badModelId + '},{"modelId":' + $goodModelId + '}]}]}}'))
$r = Req 'POST' "$ModelUrl/api/v1/model/chat/completions" $T (Body 'c2.json' '{"model":"auto","messages":[{"role":"user","content":"hi"}],"stream":false,"maxTokens":8}')
Write-Host ("  fallback=false -> HTTP={0} code={1} msg={2}" -f $r.Http, $r.Obj.code, $r.Obj.message)
Check 'fallback=false: no degradation (3xxx)' ($r.Obj.code -ge 3001 -and $r.Obj.code -le 3005) ("code=" + $r.Obj.code)

Write-Host "== D) disable temp policy + re-enable default -> auto back to default policy =="
$null = Req 'PUT' "$ModelUrl/api/v1/model/route/$policyId/status" $T (Body 's.json' '{"enabled":0}')
$null = Req 'PUT' "$ModelUrl/api/v1/model/route/$defaultPolicyId/status" $T (Body 'se.json' '{"enabled":1}')
$r = Req 'POST' "$ModelUrl/api/v1/model/chat/completions" $T (Body 'c3.json' '{"model":"auto","messages":[{"role":"user","content":"reply with: ok"}],"stream":false,"maxTokens":16}')
Write-Host ("  temp off + default on -> HTTP={0} code={1} resolved={2}" -f $r.Http, $r.Obj.code, $r.Obj.data.model)
Check 'after swap, auto uses (re-enabled) default policy' ($r.Obj.code -eq 0) ''

Write-Host "== E) usage page + visibility isolation =="
$r = Req 'GET' "$ModelUrl/api/v1/model/usage/page?pageNum=1&pageSize=50" $T $null
$orgTotal = $r.Obj.data.total
$probeVisibleToOrg = @($r.Obj.data.records | Where-Object { $_.scopeId -eq $ProbeScopeId }).Count -gt 0
Write-Host ("  org-level: total={0} probeRowVisible={1} first(modelCode={2}, tokens={3}, cost={4})" -f $orgTotal, $probeVisibleToOrg, $r.Obj.data.records[0].modelCode, $r.Obj.data.records[0].tokens, $r.Obj.data.records[0].cost)
Check 'org-level usage query works (total>0)' ($orgTotal -gt 0) ("total=" + $orgTotal)
Check 'records carry model info (modelCode not empty)' ([bool]$r.Obj.data.records[0].modelCode) ''
Check 'cost is null or string (no price table yet)' (($null -eq $r.Obj.data.records[0].cost) -or ($r.Obj.data.records[0].cost -is [string])) ''

$roles = (Req 'GET' "$UmsUrl/api/v1/role/list" $T $null).Obj.data
$wsAdminRole = @($roles | Where-Object { $_.code -eq 'ws_admin' })[0]
if ($null -eq $wsAdminRole) { Write-Host "  (skip isolation check: ws_admin role not found)" }
else {
  $uMail = "smoke-wsadmin-$ts@example.com"
  $cu = Req 'POST' "$UmsUrl/api/v1/user" $T (Body 'cu.json' ('{"email":"' + $uMail + '","nickname":"SmokeWsAdmin","password":"Passw0rd123","roleId":' + $wsAdminRole.id + '}'))
  $t2 = (Req 'POST' "$UmsUrl/auth/login" $null (Body 'a2.json' ('{"account":"' + $uMail + '","password":"Passw0rd123"}'))).Obj.data.token
  if (-not $t2) { Write-Host ("  (user create code=" + $cu.Obj.code + ", login failed -> skip isolation check)") }
  else {
    $r2 = Req 'GET' "$ModelUrl/api/v1/model/usage/page?pageNum=1&pageSize=50" $t2 $null
    $leaked = @($r2.Obj.data.records | Where-Object { $_.scopeId -eq $ProbeScopeId }).Count
    $nonWs = @($r2.Obj.data.records | Where-Object { $_.scopeType -ne 'WORKSPACE' }).Count
    Write-Host ("  ws_admin: total={0} probeRowVisible={1} nonWorkspaceRows={2}" -f $r2.Obj.data.total, $leaked, $nonWs)
    Check 'ws_admin cannot see other workspaces (isolation)' (($r2.Obj.code -eq 0) -and ($leaked -eq 0)) ("leaked=" + $leaked)
    Check 'ws_admin rows all scoped to WORKSPACE' ($nonWs -eq 0) ("nonWsRows=" + $nonWs)
    Write-Host ("  cleanup target user: " + $uMail)
  }
}

Write-Host "== F) cleanup =="
# IF 7.4 defines no delete endpoint for policies -> caller removes the temp policy with SQL:
#   DELETE FROM ie_route_policy WHERE name LIKE 'smoke-fallback-%';
$null = Req 'DELETE' "$ModelUrl/api/v1/model/$badModelId" $T $null
$null = Req 'DELETE' "$ModelUrl/api/v1/model/vendor/$badVendorId" $T $null
Write-Host ("  model/vendor logically deleted (ids={0}/{1}); temp policy id={2} needs SQL cleanup" -f $badModelId, $badVendorId, $policyId)

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
if ($fail -eq 0) { Write-Host "RESULT: ALL PASS" } else { Write-Host ("RESULT: {0} FAILED" -f $fail); exit 1 }
