#!/bin/bash

# Script para extraer SHA-1 y SHA-256 del keystore de release
# Fun Phone - Release Keystore Info Extractor

echo "=========================================="
echo "  Fun Phone - Release Keystore SHA-1"
echo "=========================================="
echo ""

KEYSTORE_FILE="my-release-key.jks"
DEFAULT_ALIAS="my-key-alias"

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${RED}❌ Error: Archivo $KEYSTORE_FILE no encontrado${NC}"
    exit 1
fi

echo -e "${BLUE}📦 Keystore encontrado:${NC} $KEYSTORE_FILE"
echo ""

# Primero intentar listar los alias sin mostrar certificados (no necesita password para algunos keystores)
echo -e "${YELLOW}📋 Listando alias en el keystore...${NC}"
echo ""
keytool -list -keystore "$KEYSTORE_FILE" 2>&1

echo ""
echo "=========================================="
echo ""

# Pedir alias
echo -e "${YELLOW}🔑 Ingresa el alias de tu clave${NC}"
echo -e "   (Presiona Enter para usar el default: ${BLUE}$DEFAULT_ALIAS${NC})"
read -p "   Alias: " ALIAS

if [ -z "$ALIAS" ]; then
    ALIAS="$DEFAULT_ALIAS"
fi

echo ""
echo -e "${BLUE}🔍 Extrayendo información para alias:${NC} $ALIAS"
echo ""

# Extraer información completa
echo -e "${YELLOW}📝 Información completa del certificado:${NC}"
echo "=========================================="
keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$ALIAS"

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Proceso completado${NC}"
echo ""
echo -e "${YELLOW}📋 INSTRUCCIONES:${NC}"
echo "1. Busca en el output arriba la línea que dice:"
echo "   ${BLUE}Certificate fingerprints:${NC}"
echo ""
echo "2. Copia el valor de ${GREEN}SHA1${NC} (tiene formato XX:XX:XX:...)"
echo "   Ejemplo: ${BLUE}A1:B2:C3:D4:E5:F6:...${NC}"
echo ""
echo "3. Copia también el ${GREEN}SHA256${NC} (por si lo necesitas)"
echo ""
echo "4. Ve a Firebase Console:"
echo "   ${BLUE}https://console.firebase.google.com${NC}"
echo ""
echo "5. Project Settings → Tu app Android"
echo ""
echo "6. En 'SHA certificate fingerprints', haz clic en:"
echo "   ${GREEN}Add fingerprint${NC}"
echo ""
echo "7. Pega el SHA-1 que copiaste"
echo ""
echo "8. Guarda y descarga nuevo google-services.json"
echo ""
echo "=========================================="
