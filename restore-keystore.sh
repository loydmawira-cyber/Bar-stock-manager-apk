#!/usr/bin/env bash
set -e

KEYSTORE_FILE="./debug.keystore"

if [ -n "$RELEASE_KEYSTORE_BASE64" ]; then
  echo "Restoring persistent keystore from RELEASE_KEYSTORE_BASE64 secret..."
  echo "$RELEASE_KEYSTORE_BASE64" | base64 -d > "$KEYSTORE_FILE"
elif [ -f "./debug.keystore.base64" ]; then
  echo "Restoring persistent keystore from ./debug.keystore.base64..."
  base64 -d ./debug.keystore.base64 > "$KEYSTORE_FILE"
elif [ -f "./app/keystore.base64" ]; then
  echo "Restoring persistent keystore from ./app/keystore.base64..."
  base64 -d ./app/keystore.base64 > "$KEYSTORE_FILE"
else
  echo "ERROR: debug.keystore.base64 not found and no secret provided!"
  exit 1
fi

echo "Verifying keystore fingerprint:"
keytool -list -v -keystore "$KEYSTORE_FILE" -storepass android -alias androiddebugkey | grep "SHA256:"
echo "Keystore ready for signing."
