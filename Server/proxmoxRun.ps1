# Leer las líneas del archivo config.env
$configFile = "config.env"
Get-Content $configFile | ForEach-Object {
    $line = $_
    if ($line -match "(\S+)\s*=\s*(.+)") {
        $key = $matches[1]
        $value = $matches[2]

        if ($value -like "*HOME*") {
            $value = $value -replace "HOME", "$HOME"
            $value = $value -replace '\$', ''
        }

        # Reemplazar las barras y eliminar comillas en valores
        $value = $value -replace '/', '\'
        $value = $value -replace '"', ''

        # Configurar la variable en PowerShell
        Set-Variable -Name $key -Value $value
    }
}

# Parámetros por defecto
$DEFAULT_USER = "barretina2"
$DEFAULT_SERVER_PORT = 20127

# Leer argumentos o usar valores por defecto
$USER = if ($args.Count -ge 1) { $args[0] } else { $DEFAULT_USER }
$SERVER_PORT = if ($args.Count -ge 2) { $args[1] } else { $DEFAULT_SERVER_PORT }

Write-Host "Usuario: $USER"
Write-Host "Puerto del servidor: $SERVER_PORT"

$JAR_NAME = "server-package.jar"
$JAR_PATH = "Server\target\$JAR_NAME"

# Cambiar al directorio raíz del proyecto
Set-Location ..

# Eliminar el archivo JAR existente si está presente
if (Test-Path $JAR_PATH) {
    Remove-Item -Force $JAR_PATH
}

# Ejecutar el comando para compilar el proyecto
Write-Host "Ejecutando el comando ./run.ps1 com.project.Server build..."
Set-Location Server
.\run.ps1 com.project.Server build

# Verificar si se generó el archivo JAR
if (-Not (Test-Path $JAR_PATH)) {
    Write-Host "Error: No se ha encontrado el archivo JAR: $JAR_PATH"
    exit 1
}

# Transferir el archivo JAR al servidor remoto usando SCP
Write-Host "Transfiriendo $JAR_NAME al servidor remoto..."
scp -P $SERVER_PORT $JAR_PATH "$USER@ieticloudpro.ieti.cat:~/"

# Verificar el resultado de SCP
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error durante la transferencia SCP"
    Set-Location Server/target
    exit 1
}

# Plantilla para los comandos SSH
$sshCommandTemplate = @'
cd $HOME
PID=$(ps aux | grep 'java -jar JAR_PLACEHOLDER' | grep -v 'grep' | awk '{print $2}')
if [ -n "$PID" ]; then
    kill $PID
    echo "Antiguo proceso JAR_PLACEHOLDER con PID $PID detenido."
else
    echo "No se encontró el proceso JAR_PLACEHOLDER."
fi
sleep 1
setsid nohup java -jar JAR_PLACEHOLDER > output.log 2>&1 &
sleep 1
PID=$(ps aux | grep 'java -jar JAR_PLACEHOLDER' | grep -v 'grep' | awk '{print $2}')
echo "Nuevo proceso JAR_PLACEHOLDER con PID $PID iniciado."
'@ -replace "`r", ""

# Reemplazar el placeholder del nombre del JAR en la plantilla
$sshCommand = $sshCommandTemplate -replace "JAR_PLACEHOLDER", $JAR_NAME

# Ejecutar comandos SSH en el servidor remoto
Write-Host "Ejecutando comandos SSH en el servidor remoto..."
ssh -p $SERVER_PORT "$USER@ieticloudpro.ieti.cat" $sshCommand

# Mantener la sesión SSH abierta
Write-Host "Iniciando shell remota para mantener la conexión abierta..."
ssh -p $SERVER_PORT "$USER@ieticloudpro.ieti.cat"

# Mostrar reglas actuales de iptables (se puede realizar también desde SSH si es necesario)
Write-Host "Mostrando las reglas actuales de iptables en la tabla NAT..."
ssh -p $SERVER_PORT "$USER@ieticloudpro.ieti.cat" "sudo iptables -t nat -L -n -v"

Write-Host "Despliegue completado. Sesión SSH aún activa."
