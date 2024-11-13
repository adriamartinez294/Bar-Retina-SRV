#!/bin/bash

source ./config.env

# Obtener configuración de los parámetros
USER=${1:-$DEFAULT_USER}
SERVER_PORT=${2:-$DEFAULT_SERVER_PORT}

echo "Usuario: $USER"
echo "Puerto del servidor: $SERVER_PORT"

JAR_NAME="server-package.jar"
JAR_PATH="./target/$JAR_NAME"

cd ..

# Generar el archivo '.jar'
rm -f "$JAR_PATH"
./run.sh com.server.Main build

# Verificar si se generó el archivo JAR
if [[ ! -f "$JAR_PATH" ]]; then
    echo "Error: No se ha encontrado el archivo JAR: $JAR_PATH"
    cd proxmox
    exit 1
fi

# Enviar el JAR al servidor mediante SCP
echo "Transfiriendo $JAR_NAME al servidor remoto..."
scp -P "$SERVER_PORT" "$JAR_PATH" "$USER@ieticloudpro.ieti.cat:~/"
if [[ $? -ne 0 ]]; then
    echo "Error durante la transferencia SCP"
    cd proxmox
    exit 1
fi

# Conectar al servidor para detener el proceso antiguo y ejecutar el nuevo
ssh -t -p "$SERVER_PORT" "$USER@ieticloudpro.ieti.cat" << EOF
    cd "\$HOME/"
    PID=\$(ps aux | grep 'java -jar $JAR_NAME' | grep -v 'grep' | awk '{print \$2}')
    if [ -n "\$PID" ]; then
      # Detener el proceso si se encuentra
      kill \$PID
      echo "Antiguo proceso $JAR_NAME con PID \$PID detenido."
    else
      echo "No se ha encontrado el proceso $JAR_NAME."
    fi
    sleep 1
    setsid nohup java -jar $JAR_NAME > output.log 2>&1 &
    sleep 1
    PID=\$(ps aux | grep 'java -jar $JAR_NAME' | grep -v 'grep' | awk '{print \$2}')
    echo "Nuevo proceso $JAR_NAME con PID \$PID iniciado."
    exit
EOF

cd proxmox
