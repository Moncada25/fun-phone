#!/bin/bash

# Quick SHA-1 Extractor para Fun Phone Release Build
# Intenta extraer SHA-1 automáticamente con credenciales comunes

echo "🔐 Extrayendo SHA-1 de my-release-key.jks..."
echo ""

KEYSTORE="my-release-key.jks"
ALIAS="my-key-alias"

# Array de contraseñas comunes a probar
PASSWORDS=("password123" "bookverse" "funphone" "android" "123456" "")

for PASS in "${PASSWORDS[@]}"; do
    if [ -z "$PASS" ]; then
        echo "⏩ Intentando sin contraseña..."
    else
        echo "🔑 Probando con: $PASS"
    fi
    
    RESULT=$(keytool -list -v -keystore "$KEYSTORE" -alias "$ALIAS" \
             -storepass "$PASS" -keypass "$PASS" 2>&1)
    
    if [[ $RESULT != *"Wrong password"* ]] && [[ $RESULT != *"Cannot recover"* ]]; then
        echo "✅ ¡Contraseña correcta encontrada!"
        echo ""
        echo "═══════════════════════════════════════════════════"
        echo "$RESULT" | grep -A 2 "Certificate fingerprints:"
        echo "═══════════════════════════════════════════════════"
        echo ""
        echo "📋 Copia el SHA1 de arriba y agrégalo a Firebase Console"
        echo ""
        echo "🌐 Firebase Console:"
        echo "   https://console.firebase.google.com"
        echo "   → Project Settings → Tu app Android"
        echo "   → Add fingerprint"
        echo ""
        exit 0
    fi
done

echo ""
echo "❌ No se pudo encontrar la contraseña automáticamente"
echo ""
echo "Por favor, ejecuta manualmente:"
echo ""
echo "  keytool -list -v -keystore key_prod.jks -alias my-key-alias"
echo ""
echo "E ingresa tu contraseña cuando te la pida."
echo ""
