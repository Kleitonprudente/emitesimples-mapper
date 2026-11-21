#!/usr/bin/env bash
# Gera o par RSA de DESENVOLVIMENTO em keys/ (private.pem em PKCS#8 e public.pem).
#
# ATENCAO: essas chaves existem SO para desenvolvimento local. NUNCA as commite (keys/ e *.pem estao no .gitignore)
# e NUNCA as use em producao: em producao gere um par proprio, guarde a privada num cofre de segredos e aponte
# smallrye.jwt.sign.key.location e mp.jwt.verify.publickey.location para ele.
set -eu

AQUI="$(cd "$(dirname "$0")/.." && pwd)"
DESTINO="$AQUI/keys"
OPENSSL="${OPENSSL:-}"
if [ -z "$OPENSSL" ]; then
  if command -v openssl >/dev/null 2>&1; then OPENSSL="openssl"; else OPENSSL="/mingw64/bin/openssl"; fi
fi

if [ -f "$DESTINO/private.pem" ] && [ "${1:-}" != "--forcar" ]; then
  echo "keys/private.pem ja existe; nada foi feito (use --forcar para regerar e invalidar todos os tokens)." >&2
  exit 0
fi

mkdir -p "$DESTINO"
"$OPENSSL" genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$DESTINO/private.pem" 2>/dev/null
"$OPENSSL" rsa -in "$DESTINO/private.pem" -pubout -out "$DESTINO/public.pem" 2>/dev/null
chmod 600 "$DESTINO/private.pem" 2>/dev/null || true

echo "Par RSA de DESENVOLVIMENTO gerado em: $DESTINO"
echo "AVISO: NUNCA commite estas chaves e NUNCA as use em producao."
