#!/usr/bin/env bash
# bloquear-codigo-gerado.sh — hook PreToolUse (Write|Edit)
#
# TRAVA: o codigo gerado nunca se edita a mao. Os geradores e a documentacao mandam: se a saida esta errada ou incompleta,
# o conserto e no MAPA (.mi) ou no GERADOR (.mi), e a saida volta a ser gerada. Em duvida, PERGUNTE ao usuario.
#
# Exit 0 = permite. Exit 2 = bloqueia (a mensagem em stderr volta para quem tentou editar).
# Escape consciente (so o usuario): EMITE_PERMITIR_EDICAO_GERADA=1 no ambiente da sessao.

set -uo pipefail

[ "${EMITE_PERMITIR_EDICAO_GERADA:-0}" = "1" ] && exit 0

INPUT=$(cat)
if command -v jq &>/dev/null 2>&1; then
  FILE_PATH=$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // .file_path // ""' 2>/dev/null || true)
else
  FILE_PATH=$(printf '%s' "$INPUT" | grep -oE '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 \
    | sed 's/.*"file_path"[[:space:]]*:[[:space:]]*"\(.*\)"/\1/')
fi
[ -z "${FILE_PATH:-}" ] && exit 0

# normaliza: barras invertidas do Windows e "\\" duplicadas do JSON viram "/"
FP=$(printf '%s' "$FILE_PATH" | sed 's#\\\\#/#g; s#\\#/#g')

block() {
  {
    printf '\n[trava: codigo gerado] BLOQUEADO: %s\n' "$1"
    printf 'Arquivo: %s\n' "$FILE_PATH"
    printf 'O codigo gerado e sobrescrito por inteiro pelo gerar.sh e nao se edita a mao.\n'
    printf 'Conserte o MAPA (.mi) ou o GERADOR (.mi) e gere de novo (o mi push e o gerar.sh so com autorizacao do usuario).\n'
    printf 'Se o mapa ou o gerador nao cobre o que voce precisa, PARE e PERGUNTE ao usuario: nao improvise nem desfaca a saida na mao.\n\n'
  } >&2
  exit 2
}

# 1) back: pacotes inteiramente gerados (o gerar.sh apaga e recria; o que e escrito a mao vive em outros pacotes)
printf '%s' "$FP" | grep -qE '/back/api/src/main/java/br/com/emitesimples/(configuracao|cadastros|usuarios|nfse|integranotas|admin)/' \
  && block "pacote Java gerado (configuracao, cadastros, usuarios, nfse, integranotas, admin)"

# 2) back: OpenAPI gerado
printf '%s' "$FP" | grep -qE '/back/api/docs/openapi/' && block "OpenAPI gerado (back/api/docs/openapi)"

# 3) front: saidas geradas
printf '%s' "$FP" | grep -qE '/front/web/src/gen/' && block "front/web/src/gen e gerado"
printf '%s' "$FP" | grep -qE '/routeTree\.gen\.ts$' && block "routeTree.gen.ts e gerado pelo roteador"

# 4) front/mapas/dominio e COPIA somente-leitura do dominio do back (use scripts/sincronizar-dominio.sh)
printf '%s' "$FP" | grep -qE '/front/mapas/dominio/' && block "front/mapas/dominio e copia do back: edite back/mapas e rode sincronizar-dominio.sh"

# 5) front: telas e rotas geradas, reconhecidas pelo cabecalho 'GERADO por' (testes escritos a mao nao o tem)
if printf '%s' "$FP" | grep -qE '/front/web/src/(features/(cadastros|configuracao)|routes)/' && [ -f "$FILE_PATH" ]; then
  head -n 3 "$FILE_PATH" 2>/dev/null | grep -q 'GERADO por' && block "arquivo do front com cabecalho 'GERADO por'"
fi

exit 0
