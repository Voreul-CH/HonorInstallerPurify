$ErrorActionPreference = "Stop"
$base = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = "$base\tools"
$build = "$base\build"
$jreBin = "C:\Users\Administrator\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\vm\tools\app\jre\bin"
$java = "$jreBin\java.exe"
$keytool = "$jreBin\keytool.exe"
$bt = "$tools\build-tools"
$androidJar = "$tools\android.jar"
$apiJar = "$tools\api-82.jar"
$ecj = "$tools\ecj.jar"

$env:JAVA_HOME = Split-Path -Parent $jreBin
$env:PATH = "$jreBin;$env:PATH"

Write-Output "== 1/5 compile java =="
Remove-Item -Recurse -Force "$build\classes" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$build\classes" | Out-Null
& $java -jar $ecj -8 -nowarn -cp "$androidJar;$apiJar" -d "$build\classes" "$base\src\io\github\voreulch\installerpurify\MainHook.java"
if ($LASTEXITCODE -ne 0) { throw "ecj compile failed" }

Write-Output "== 2/5 dex =="
Remove-Item -Recurse -Force "$build\dex" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$build\dex" | Out-Null
& "$bt\d8.bat" --lib $androidJar --output "$build\dex" (Get-ChildItem "$build\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Output "== 3/5 aapt2 package =="
Remove-Item -Force "$build\res.zip", "$build\unsigned.apk" -ErrorAction SilentlyContinue
& "$bt\aapt2.exe" compile --dir "$base\res" -o "$build\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
& "$bt\aapt2.exe" link -o "$build\unsigned.apk" -I $androidJar --manifest "$base\AndroidManifest.xml" -A "$base\assets" "$build\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Output "== 4/5 add dex into apk =="
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$build\unsigned.apk", "Update")
try {
    $entry = $zip.GetEntry("classes.dex")
    if ($entry -ne $null) { $entry.Delete() }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$build\dex\classes.dex", "classes.dex") | Out-Null
} finally {
    $zip.Dispose()
}

Write-Output "== 4.5/5 zipalign =="
Remove-Item -Force "$build\aligned.apk" -ErrorAction SilentlyContinue
& "$bt\zipalign.exe" -f -p 4 "$build\unsigned.apk" "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }
Copy-Item -Force "$build\aligned.apk" "$build\unsigned.apk"

Write-Output "== 5/5 sign =="
if (-not (Test-Path "$build\ks.jks")) {
    & $keytool -genkeypair -alias installbypass -keyalg RSA -keysize 2048 -validity 10000 `
        -keystore "$build\ks.jks" -storepass installbypass -keypass installbypass `
        -dname "CN=InstallBypass, OU=Dev, O=Dev, L=CN, S=CN, C=CN"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}
Remove-Item -Force "$build\HonorInstallerPurify.apk" -ErrorAction SilentlyContinue
& "$bt\apksigner.bat" sign --ks "$build\ks.jks" --ks-pass pass:installbypass --key-pass pass:installbypass `
    --out "$build\HonorInstallerPurify.apk" "$build\unsigned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }
& "$bt\apksigner.bat" verify "$build\HonorInstallerPurify.apk"

Write-Output ""
Write-Output "BUILD OK: $build\HonorInstallerPurify.apk"
