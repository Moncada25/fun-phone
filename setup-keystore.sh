#!/bin/bash

# Script de configuración automática para keystore.properties
# Fun Phone - Secure Credentials Setup

echo "=========================================="
echo "  Fun Phone - Keystore Setup"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

KEYSTORE_PROPS="keystore.properties"
TEMPLATE="keystore.properties.template"

if [ -f "$KEYSTORE_PROPS" ]; then
    echo -e "${YELLOW}⚠️  $KEYSTORE_PROPS ya existe.${NC}"
    echo ""
    read -p "¿Deseas sobrescribirlo? (s/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo -e "${BLUE}ℹ️  Manteniendo archivo existente.${NC}"
        exit 0
    fi
fi

echo -e "${BLUE}📝 Configurando credenciales de keystore...${NC}"
echo ""

# Solicitar información
echo -e "${YELLOW}Nombre del archivo keystore:${NC}"
read -p "  (default: key_prod.jks): " KEYSTORE_FILE
KEYSTORE_FILE=${KEYSTORE_FILE:-key_prod.jks}

if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${RED}❌ Error: $KEYSTORE_FILE no encontrado${NC}"
    echo -e "   Por favor, verifica que el archivo existe en el directorio actual."
    exit 1
fi

echo -e "${YELLOW}Contraseña del keystore:${NC}"
read -s -p "  " STORE_PASSWORD
echo ""

echo -e "${YELLOW}Alias de la clave:${NC}"
read -p "  (default: my-key-alias): " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-my-key-alias}

echo -e "${YELLOW}Contraseña de la clave:${NC}"
read -s -p "  " KEY_PASSWORD
echo ""

# Crear archivo
cat > "$KEYSTORE_PROPS" << EOL
# Keystore Configuration for Release Builds
# ⚠️ NEVER commit this file to version control!

RELEASE_STORE_FILE=$KEYSTORE_FILE
RELEASE_STORE_PASSWORD=$STORE_PASSWORD
RELEASE_KEY_ALIAS=$KEY_ALIAS
RELEASE_KEY_PASSWORD=$KEY_PASSWORD

# Generated: $(date)
EOL

echo ""
echo -e "${GREEN}✅ $KEYSTORE_PROPS creado exitosamente${NC}"
echo ""

# Verificar configuración
echo -e "${BLUE}🔍 Verificando configuración...${NC}"
echo ""

if keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass "$STORE_PASSWORD" -keypass "$KEY_PASSWORD" &> /dev/null; then
    echo -e "${GREEN}✅ Credenciales verificadas correctamente${NC}"
    echo ""
    
    # Mostrar SHA-1
    echo -e "${BLUE}📋 SHA-1 del keystore:${NC}"
    echo ""
    keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass "$STORE_PASSWORD" -keypass "$KEY_PASSWORD" 2>&1 | grep "SHA1:"
    echo ""
else
    echo -e "${RED}❌ Error: No se pudieron verificar las credenciales${NC}"
    echo -e "   Por favor, verifica que la información sea correcta."
    rm -f "$KEYSTORE_PROPS"
    exit 1
fi

echo "=========================================="
echo -e "${GREEN}✅ Configuración completada${NC}"
echo ""
echo -e "${YELLOW}Próximos pasos:${NC}"
echo "1. Copia el SHA-1 de arriba"
echo "2. Agrégalo a Firebase Console"
echo "3. Descarga nuevo google-services.json"
echo "4. Ejecuta: ${BLUE}./gradlew clean bundleRelease${NC}"
echo ""
echo "=========================================="
