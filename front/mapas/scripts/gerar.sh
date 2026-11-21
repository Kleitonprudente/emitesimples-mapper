#!/usr/bin/env bash
# Gera o codigo do front (src/gen e src/features) a partir do mapa que esta na nuvem (projeto `emitesimples-front`).
# NAO faz `mi push`: so le o mapa ja publicado. Rode no Git Bash (no PowerShell `mi` e alias de Move-Item).
#
# Cada saida vai para um temporario e so substitui o arquivo de destino se nao estiver vazia nem trouxer erro de
# modelo (`mi generate` devolve 0 mesmo quando falha e escreve o erro DENTRO da saida). O @TODO que o gerador
# deixa de proposito (campo sem suporte no runtime) NAO e erro: e listado ao final.
set -u

PROJETO="${MI_PROJETO:-emitesimples-front}"
AQUI="$(cd "$(dirname "$0")/.." && pwd)"
WEB="$AQUI/../web/src"
RAIZ="br.com.emitesimples"
DOM="$RAIZ.domain.cadastros"
JAN="$RAIZ.window.cadastros.pessoa"
F="$WEB/features/cadastros/pessoa"

command -v mi >/dev/null 2>&1 || { echo "mi nao encontrado no PATH (use o Git Bash)" >&2; exit 2; }
[ -d "$HOME/.mapperidea" ] || { echo "HOME ($HOME) nao tem .mapperidea" >&2; exit 2; }

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
FALHOU=0

# gerar <destino> <gerador> [parametros...]
gerar() {
  local destino="$1" gerador="$2"; shift 2
  local saida="$TMP/$(echo "$destino" | tr '/\\$' '___')"
  mi generate "$PROJETO" frontend "$gerador" "$@" > "$saida"
  if [ ! -s "$saida" ] || grep -qE 'EMI[0-9]{4}|\{\{|ERRO NO MODELO|TRUNCADA' "$saida"; then
    echo "FALHA: $gerador $* -> $destino (saida vazia ou com erro de modelo)" >&2; FALHOU=1; return
  fi
  mkdir -p "$(dirname "$destino")"
  tr -d '\r' < "$saida" > "$destino"
}

# dominio (uma vez por entidade)
gerar "$WEB/gen/schemas/pessoa.ts" zodSchema  modelName=Pessoa package="$DOM"
gerar "$WEB/gen/api/pessoa.ts"     apiClient  modelName=Pessoa package="$DOM"

# telas (uma vez por janela)
gerar "$F/columns.tsx"               screenColumns    modelName=GestaoPessoas package="$JAN"
gerar "$F/GestaoPessoasScreen.tsx"   screenPage       modelName=GestaoPessoas package="$JAN"
gerar "$F/CriarPessoa.tsx"           screenForm       modelName=CriarPessoa   package="$JAN"
gerar "$F/EditarPessoa.tsx"          screenForm       modelName=EditarPessoa  package="$JAN"
gerar "$F/EditarPessoaScreen.tsx"    screenEditorById modelName=EditarPessoa  package="$JAN"
gerar "$WEB/routes/_authenticated/pessoas/index.tsx"  screenRoute modelName=GestaoPessoas package="$JAN"
gerar "$WEB/routes/_authenticated/pessoas/\$id.tsx"   screenRoute modelName=EditarPessoa  package="$JAN"

# manifesto (um para o projeto todo)
gerar "$WEB/gen/screens.ts" screenManifest

[ "$FALHOU" = 0 ] || { echo "Geracao incompleta: veja as FALHA acima." >&2; exit 1; }
echo "Geracao concluida. Pendencias deixadas pelos geradores:"
grep -rn '@TODO' "$WEB/gen" "$WEB/features" "$WEB/routes" 2>/dev/null || echo "  (nenhuma)"
