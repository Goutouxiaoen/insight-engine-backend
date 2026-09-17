<#
  Qwen (Aliyun DashScope) connectivity smoke test -- verify the key BEFORE writing any Java (stage 6 prep)

  Why run this first:
    1) Confirms API key / base_url / model name are valid -- separates "key problem" from "code problem" at zero cost;
    2) Confirms stream=true really returns SSE (the core acceptance item of the model gateway);
    3) Confirms the embedding model outputs 1024 dims (matches ie_chunk.embedding constraint, avoids kb-stage rework).

  Key source (either way; the script only prints a mask, never the full key):
    A) environment variable:  $env:QWEN_API_KEY='sk-xxxx'    (recommended, nothing on disk)
    B) file:                  -ApiKeyFile D:\path\qwen.key   (one line; keep it under a .gitignore'd path)

  Usage:
    powershell -File scripts/smoke-qwen-connectivity.ps1
    powershell -File scripts/smoke-qwen-connectivity.ps1 -Model qwen-turbo -EmbeddingModel text-embedding-v3

  Notes:
    * ASCII only -- PowerShell 5.1 mis-decodes UTF-8-without-BOM on zh-CN Windows (Chinese text inside
      this file would be mangled and break string quoting). Keep it ASCII; Chinese docs live in docs/.
    * Sends exactly 3 requests (1 non-stream chat + 1 stream chat + 1 embedding) -- negligible cost.
    * On failure it prints HTTP status + the server error body (never the key) to classify 401/403/404/429.
#>
param(
  [string]$BaseUrl = 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  [string]$Model = 'qwen-plus',
  [string]$EmbeddingModel = 'text-embedding-v3',
  [string]$ApiKeyFile = '',
  # 该套餐不含 embedding 模型时置 1 跳过第 3 步（如千问AI平台 Token Plan Lite：14 个模型里无 embedding）
  [switch]$SkipEmbedding
)
$ErrorActionPreference = 'Stop'
$tmp = Join-Path $env:TEMP ("qwen-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$fail = 0

function Body($n, $j) { $f = Join-Path $tmp $n; [IO.File]::WriteAllText($f, $j, (New-Object Text.UTF8Encoding($false))); return $f }

# ---------- load key (masked output only) ----------
$key = $env:QWEN_API_KEY
if (-not $key -and $ApiKeyFile) {
  if (-not (Test-Path $ApiKeyFile)) { throw "ApiKeyFile not found: $ApiKeyFile" }
  $key = ([IO.File]::ReadAllText($ApiKeyFile, (New-Object Text.UTF8Encoding($false)))).Trim()
}
if (-not $key) { throw "No API key. Set env QWEN_API_KEY='sk-...' or pass -ApiKeyFile <path>." }
Write-Host ("key loaded: {0}***{1} (len={2})" -f $key.Substring(0, [Math]::Min(3, $key.Length)), $key.Substring([Math]::Max(0, $key.Length - 4)), $key.Length)
Write-Host ("base_url : {0}" -f $BaseUrl)

# ---------- 1) non-stream chat ----------
Write-Host ""
Write-Host "== 1) POST /chat/completions (stream=false) =="
$b = Body 'c1.json' (("{""model"":""$Model"",""messages"":[{""role"":""user"",""content"":""reply with: ok""}],""max_tokens"":16,""stream"":false}"))
$code = (& curl.exe -s -o "$tmp\c1.out" -w '%{http_code}' --max-time 60 -X POST "$BaseUrl/chat/completions" -H "Authorization: Bearer $key" -H 'Content-Type: application/json' -d "@$b")
$raw = [IO.File]::ReadAllText("$tmp\c1.out", (New-Object Text.UTF8Encoding($false)))
Write-Host "HTTP=$code"
$o = $null; try { $o = $raw | ConvertFrom-Json } catch { }
if ($o -and $o.choices) {
  Write-Host ("model={0}  reply={1}" -f $o.model, $o.choices[0].message.content)
  Write-Host ("usage: prompt={0} completion={1} total={2}" -f $o.usage.prompt_tokens, $o.usage.completion_tokens, $o.usage.total_tokens)
  Write-Host "RESULT1: OK"
} else {
  Write-Host ("error body: " + $raw)
  Write-Host "RESULT1: FAILED"; $fail++
}

# ---------- 2) stream chat (SSE) ----------
Write-Host ""
Write-Host "== 2) POST /chat/completions (stream=true, SSE) =="
$b = Body 'c2.json' (("{""model"":""$Model"",""messages"":[{""role"":""user"",""content"":""count 1 to 5""}],""max_tokens"":32,""stream"":true}"))
$sse = (& curl.exe -s -N --max-time 60 -X POST "$BaseUrl/chat/completions" -H "Authorization: Bearer $key" -H 'Content-Type: application/json' -d "@$b" | Out-String)
$dataLines = @($sse -split "`n" | Where-Object { $_ -match '^data:' })
$hasDone = $sse -match '\[DONE\]'
Write-Host ("chunks with 'data:' = {0}; contains [DONE] = {1}" -f $dataLines.Count, $hasDone)
if ($dataLines.Count -ge 1) {
  $first = $dataLines[0].Trim()
  Write-Host ("first chunk: " + $first.Substring(0, [Math]::Min(120, $first.Length)))
}
if ($hasDone) { Write-Host "RESULT2: OK" } else { Write-Host ("error/abnormal body: " + $sse.Substring(0, [Math]::Min(300, $sse.Length))); Write-Host "RESULT2: FAILED"; $fail++ }

# ---------- 3) embedding (dimension check for kb stage) ----------
Write-Host ""
if ($SkipEmbedding) {
  Write-Host ""
  Write-Host "== 3) embeddings -> SKIPPED (-SkipEmbedding) =="
} else {
Write-Host ""
Write-Host "== 3) POST /embeddings (dimensions=1024) =="
$b = Body 'c3.json' (("{""model"":""$EmbeddingModel"",""input"":""insight engine connectivity test"",""dimensions"":1024}"))
$code = (& curl.exe -s -o "$tmp\c3.out" -w '%{http_code}' --max-time 60 -X POST "$BaseUrl/embeddings" -H "Authorization: Bearer $key" -H 'Content-Type: application/json' -d "@$b")
$raw3 = [IO.File]::ReadAllText("$tmp\c3.out", (New-Object Text.UTF8Encoding($false)))
Write-Host "HTTP=$code"
$o3 = $null; try { $o3 = $raw3 | ConvertFrom-Json } catch { }
if ($o3 -and $o3.data) {
  Write-Host ("model={0} vector_len={1} (ie_chunk.embedding constraint = 1024)" -f $o3.model, $o3.data[0].embedding.Count)
  if ($o3.data[0].embedding.Count -eq 1024) { Write-Host "RESULT3: OK" } else { Write-Host "RESULT3: OK but dim mismatch -> adjust ie_chunk.embedding or pass dimensions" }
} else {
  Write-Host ("error body: " + $raw3)
  Write-Host "RESULT3: FAILED"; $fail++
}
}

Remove-Item -Force -Recurse $tmp -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "(key was never printed in full and never persisted by this script)"
if ($fail -eq 0) { Write-Host "SUMMARY: ALL OK" } else { Write-Host ("SUMMARY: {0} check(s) FAILED" -f $fail); exit 1 }
