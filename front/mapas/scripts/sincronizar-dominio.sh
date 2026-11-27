#!/usr/bin/env bash
# Sincroniza o dominio (somente-leitura) do back para o front.
#   sincronizar-dominio.sh              copia back/mapas/<Contexto>/*.mi -> front/mapas/dominio/<Contexto>/
#   sincronizar-dominio.sh --verificar  nao copia; sai com 1 listando divergentes
# Tambem cobre generators/struct.mi (necessario para validar com struct).
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONT="$(cd "$AQUI/.." && pwd)"
BACK="$(cd "$FRONT/../../back/mapas" && pwd)" || { echo "back/mapas nao encontrado" >&2; exit 2; }
CONTEXTOS="Configuracao Cadastros Usuarios Nfse IntegraNotas Admin Financeiro"

MODO=copiar
case "${1:-}" in
  "") ;;
  --verificar) MODO=verificar ;;
  *) echo "uso: $0 [--verificar]" >&2; exit 2 ;;
esac

# pares origem|destino (relativos a back/mapas e front/mapas)
PARES=()
for ctx in $CONTEXTOS; do
  for f in "$BACK/$ctx"/*.mi; do
    [ -e "$f" ] || continue
    PARES+=("$ctx/$(basename "$f")|dominio/$ctx/$(basename "$f")")
  done
done
PARES+=("generators/struct.mi|generators/struct.mi")

DIV=()
for par in "${PARES[@]}"; do
  src="$BACK/${par%%|*}"; dst="$FRONT/${par##*|}"
  if [ ! -f "$dst" ] || ! cmp -s "$src" "$dst"; then
    DIV+=("${par##*|}")
    if [ "$MODO" = copiar ]; then
      mkdir -p "$(dirname "$dst")"
      cp "$src" "$dst"
      echo "copiado: ${par##*|}"
    fi
  fi
done

# arquivos no front sem origem no back (removidos/renomeados)
ORFAOS=()
for ctx in $CONTEXTOS; do
  for f in "$FRONT/dominio/$ctx"/*.mi; do
    [ -e "$f" ] || continue
    [ -f "$BACK/$ctx/$(basename "$f")" ] || ORFAOS+=("dominio/$ctx/$(basename "$f")")
  done
done

if [ "$MODO" = verificar ]; then
  if [ ${#DIV[@]} -eq 0 ] && [ ${#ORFAOS[@]} -eq 0 ]; then
    echo "OK: dominio do front identico ao do back (${#PARES[@]} arquivos)."; exit 0
  fi
  for d in "${DIV[@]}"; do echo "DIVERGENTE: $d"; done
  for o in "${ORFAOS[@]}"; do echo "ORFAO (sem origem no back): $o"; done
  echo "Rode scripts/sincronizar-dominio.sh para sincronizar." >&2
  exit 1
fi
for o in "${ORFAOS[@]}"; do echo "AVISO orfao (remova a mao se for o caso): $o"; done
echo "Sincronizado: ${#DIV[@]} arquivo(s) alterado(s) de ${#PARES[@]}."
