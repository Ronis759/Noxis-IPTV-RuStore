# Подписывает собранный release-APK постоянным ключом Noxis IPTV.
# Ключ и пароли берутся из keys-backup в основном проекте; в этот проект они не копируются.
# Для сборки в GitHub Actions этот скрипт не нужен: там APK подписывается сразу при сборке
# (ключ берётся из Secrets репозитория), см. .github/workflows/build.yml.
$ErrorActionPreference = "Stop"

$root = "E:\OpenCode\Noxis-IPTV RuStore"
$src = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$outDir = Join-Path $root "releases"
$out = Join-Path $outDir "noxis-iptv-rustore-3.0.0.apk"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$secretsFile = "E:\OpenCode\Noxis-IPTV\keys-backup\Noxis-IPTV\secrets.txt"
# файл — несколько вкладок формата «Name: …» / «Secret: …»
$secrets = @{}
$raw = Get-Content $secretsFile
for ($i = 0; $i -lt $raw.Count; $i++) {
    $m = [regex]::Match($raw[$i], '^\s*Name:\s*([A-Za-z_]+)\s*$')
    if (-not $m.Success) { continue }
    $name = $m.Groups[1].Value
    for ($j = $i + 1; $j -lt [Math]::Min($raw.Count, $i + 4); $j++) {
        $s = [regex]::Match($raw[$j], '^\s*Secret:\s*(.+?)\s*$')
        if ($s.Success) { $secrets[$name] = $s.Groups[1].Value; break }
    }
}
$store = $secrets['KEYSTORE_PASSWORD']
$alias = $secrets['KEY_ALIAS']
$keypw = $secrets['KEY_PASSWORD']
if (-not $store -or -not $alias -or -not $keypw) { throw "В secrets.txt нет KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD" }

$keystore = "E:\OpenCode\Noxis-IPTV\keys-backup\Noxis-IPTV\release.keystore"
$apksigner = "C:\Users\GREAY\AppData\Local\Android\Sdk\build-tools\35.0.0\apksigner.bat"
$jarsigner = "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe"
$zipalign = "C:\Users\GREAY\AppData\Local\Android\Sdk\build-tools\35.0.0\zipalign.exe"

# выравнивание перед подписью (v2-подпись это требует)
Copy-Item $src ($out + ".unaligned") -Force
& $zipalign -f -p 4 ($out + ".unaligned") $out
Remove-Item ($out + ".unaligned")

# подпись (v1+v2+v3)
$args = @(
    "sign", "--ks", $keystore, "--ks-pass", "pass:$store", "--ks-key-alias", $alias,
    "--key-pass", "pass:$keypw", "--out", ($out + ".signed"), $out
)
& $apksigner @args
if ($LASTEXITCODE -ne 0) { throw "apksigner вернул код $LASTEXITCODE" }
Move-Item ($out + ".signed") $out -Force
# файл .idsig нужен только для установки через adb; в RuStore он не загружается
$idsig = $out + ".idsig"
if (Test-Path $idsig) { Remove-Item -Force $idsig }

& $apksigner verify --print-certs $out
$hash = (Get-FileHash $out -Algorithm SHA256).Hash.ToLower()
"APK: $out"
"SHA256: $hash"
$hash | Set-Content ($out + ".sha256") -NoNewline
