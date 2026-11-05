# Prepara la carpeta "payload" y compila el instalador
$ErrorActionPreference = 'Stop'

$aqui    = $PSScriptRoot                         # carpeta instalador
$raiz    = Split-Path -Parent $aqui              # carpeta del proyecto del agente
$jar     = Join-Path $raiz 'target\sfmantto-agente.jar'
$payload = Join-Path $aqui 'payload'

foreach ($f in @($jar,
                 (Join-Path $aqui 'WinSW-x64.exe'),
                 (Join-Path $aqui 'ca.crt'),
                 (Join-Path $aqui 'sfmantto-agente.xml'),
                 (Join-Path $aqui 'agente.iss'))) {
    if (-not (Test-Path $f)) { throw "Falta el archivo: $f" }
}
foreach ($c in 'jdeps', 'jlink') {
    if (-not (Get-Command $c -ErrorAction SilentlyContinue)) {
        throw "No se encontró $c. Agrega la carpeta bin del JDK 21 al PATH."
    }
}

if (Test-Path $payload) { Remove-Item $payload -Recurse -Force }
New-Item -ItemType Directory $payload | Out-Null

Copy-Item $jar (Join-Path $payload 'sfmantto-agente.jar')
Copy-Item (Join-Path $aqui 'WinSW-x64.exe') (Join-Path $payload 'sfmantto-agente.exe')
Copy-Item (Join-Path $aqui 'sfmantto-agente.xml') $payload
Copy-Item (Join-Path $aqui 'ca.crt') $payload

# Java mínimo: los módulos que usa el agente más los de HTTPS
$salida  = & jdeps --print-module-deps --ignore-missing-deps --multi-release 21 $jar 2>$null
$modulos = ($salida | Select-Object -Last 1)
if (-not $modulos) { throw 'jdeps no devolvió la lista de módulos.' }
$modulos = $modulos.ToString().Trim()
$extra   = 'jdk.crypto.ec,java.naming,java.management,java.desktop,jdk.unsupported'

& jlink --add-modules "$modulos,$extra" --output (Join-Path $payload 'runtime') `
        --strip-debug --no-header-files --no-man-pages --compress=zip-6
if ($LASTEXITCODE -ne 0) { throw 'jlink falló.' }

$mb = [math]::Round(((Get-ChildItem $payload -Recurse | Measure-Object Length -Sum).Sum) / 1MB, 1)
Write-Host "Payload listo: $mb MB. Módulos: $modulos,$extra"

# Compilar el instalador si Inno Setup está instalado
$iscc = @("${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe", "$env:ProgramFiles\Inno Setup 6\ISCC.exe") |
        Where-Object { Test-Path $_ } | Select-Object -First 1
if ($iscc) {
    & $iscc (Join-Path $aqui 'agente.iss')
} else {
    Write-Host 'Inno Setup no está instalado: compila agente.iss a mano.'
}