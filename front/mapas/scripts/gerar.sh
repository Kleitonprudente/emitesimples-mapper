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

# entidade <Entidade> <contexto> <slug-feature> <slug-rota> <Lista> <Formulario>
# Dominio (zodSchema/apiClient) e as 6 saidas de tela de uma entidade. O pacote das janelas e
# $RAIZ.window.<contexto>.<slug-feature>; o destino das telas, web/src/features/<contexto>/<slug-feature>.
entidade() {
  local ent="$1" ctx="$2" slug="$3" rota="$4" lista="$5" form="$6"
  local arq; arq="$(echo "${ent:0:1}" | tr 'A-Z' 'a-z')${ent:1}"
  local jan="$RAIZ.window.$ctx.$slug" f="$WEB/features/$ctx/$slug"
  local dom="$RAIZ.domain.$ctx"

  # dominio (uma vez por entidade)
  gerar "$WEB/gen/schemas/$arq.ts" zodSchema modelName="$ent" package="$dom"
  gerar "$WEB/gen/api/$arq.ts"     apiClient modelName="$ent" package="$dom"

  # telas (uma vez por janela)
  gerar "$f/columns.tsx"             screenColumns    modelName="$lista" package="$jan"
  gerar "$f/${lista}Screen.tsx"      screenPage       modelName="$lista" package="$jan"
  gerar "$f/$form.tsx"               screenForm       modelName="$form"  package="$jan"
  gerar "$f/${form}Screen.tsx"       screenEditorById modelName="$form"  package="$jan"
  gerar "$WEB/routes/_authenticated/$rota/index.tsx" screenRoute modelName="$lista" package="$jan"
  gerar "$WEB/routes/_authenticated/$rota/\$id.tsx"  screenRoute modelName="$form"  package="$jan"
}

# saidas de versoes anteriores do mapa (formulario unico substituiu Criar/Editar)
F="$WEB/features/cadastros/pessoa"
rm -f "$F/CriarPessoa.tsx" "$F/EditarPessoa.tsx" "$F/EditarPessoaScreen.tsx"

entidade Pessoa    cadastros pessoa    pessoas    GestaoPessoas    FormularioPessoa
entidade Municipio cadastros municipio municipios GestaoMunicipios FormularioMunicipio
entidade Empresa   configuracao empresa empresas   GestaoEmpresas   FormularioEmpresa

# manifesto (um para o projeto todo)
gerar "$WEB/gen/screens.ts" screenManifest

[ "$FALHOU" = 0 ] || { echo "Geracao incompleta: veja as FALHA acima." >&2; exit 1; }
echo "Geracao concluida. Pendencias deixadas pelos geradores:"
grep -rn '@TODO' "$WEB/gen" "$WEB/features" "$WEB/routes" 2>/dev/null || echo "  (nenhuma)"
