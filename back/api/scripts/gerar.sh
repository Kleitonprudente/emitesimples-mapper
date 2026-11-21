#!/usr/bin/env bash
# Gera o codigo Java e o OpenAPI a partir do mapa que esta na nuvem (projeto `emitesimples` do CLI mi).
# NAO faz `mi push`: so le o mapa ja publicado. Rode no Git Bash (no PowerShell `mi` e alias de Move-Item).
#
# A saida de cada gerador e sobrescrita por inteiro: nao edite o codigo gerado. O que e escrito a mao vive em
# exceptions/, results/ e tenant/ (fora dos pacotes de contexto), e nunca e apagado por este script.
set -u

PROJETO="${MI_PROJETO:-emitesimples}"
RAIZ="br.com.emitesimples"
AQUI="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$AQUI/src/main/java/br/com/emitesimples"
YAML="$AQUI/docs/openapi"

command -v mi >/dev/null 2>&1 || { echo "mi nao encontrado no PATH (use o Git Bash)" >&2; exit 2; }
[ -d "$HOME/.mapperidea" ] || { echo "HOME ($HOME) nao tem .mapperidea: o mi daria 'Error loading authorization'" >&2; exit 2; }

# contexto:Entidade, na ordem do mapa
ENTIDADES=(
  configuracao:Empresa configuracao:AtividadeEmpresa configuracao:QsaEmpresa
  cadastros:Municipio cadastros:ImportacaoMunicipio cadastros:Pessoa cadastros:Contato cadastros:Servico
  usuarios:Usuario usuarios:VinculoUsuario usuarios:ClienteAcesso usuarios:TokenRenovacao usuarios:ConfirmacaoCadastro
  nfse:Nota nfse:Cancelamento
  integranotas:ChamadaGateway integranotas:EventoWebhook
  admin:Plano admin:AssinaturaEmpresa admin:FaturaCrm admin:HistoricoSituacaoEmpresa
)
CONTEXTOS=(configuracao cadastros usuarios nfse integranotas admin)

# Entidades SEM recurso REST gerado (guardam hash de token; o endpoint publico de confirmacao e o de refresh sao escritos a mao)
SEM_RESOURCE="TokenRenovacao ConfirmacaoCadastro"

# limpa so o que e gerado (pacotes de contexto), nunca exceptions/results/tenant
for ctx in "${CONTEXTOS[@]}"; do rm -rf "$OUT/$ctx"; done
mkdir -p "$YAML"

for par in "${ENTIDADES[@]}"; do
  ctx="${par%%:*}"; e="${par##*:}"
  PKG="$RAIZ.domain.$ctx"
  dir="$OUT/$ctx"
  mkdir -p "$dir"/{domain,entity,builder,repository,mapper,resource}
  mi generate "$PROJETO" quarkus domain     modelName="$e" package="$PKG" > "$dir/domain/$e.java"
  mi generate "$PROJETO" quarkus entity     modelName="$e" package="$PKG" > "$dir/entity/${e}Entity.java"
  mi generate "$PROJETO" quarkus builder    modelName="$e" package="$PKG" > "$dir/builder/${e}EntityBuilder.java"
  mi generate "$PROJETO" quarkus repository modelName="$e" package="$PKG" > "$dir/repository/${e}Repository.java"
  mi generate "$PROJETO" quarkus mapper     modelName="$e" package="$PKG" > "$dir/mapper/${e}Mapper.java"
  # SEM recurso REST: entidades que guardam hash de token e nao podem ter CRUD exposto (o endpoint e escrito a mao)
  case " $SEM_RESOURCE " in
    *" $e "*) rmdir "$dir/resource" 2>/dev/null ;;
    *) mi generate "$PROJETO" quarkus resource modelName="$e" package="$PKG" > "$dir/resource/${e}Resource.java" ;;
  esac
done

for ctx in "${CONTEXTOS[@]}"; do
  mi generate "$PROJETO" swagger restAPI packageName="$RAIZ.domain.$ctx" > "$YAML/openapi-$ctx.yaml"
done

# `mi generate` devolve 0 mesmo quando falha e escreve o erro DENTRO do arquivo gerado; parametro errado
# (por exemplo className no lugar de modelName) gera arquivo VAZIO sem erro. Confira as duas coisas.
FALHOU=0
if grep -rlE 'EMI[0-9]{4}|@TODO|TRUNCADA|ERRO NO MODELO' "$OUT" "$YAML" 2>/dev/null; then
  echo "FALHA: a geracao deixou marcador de erro nos arquivos acima" >&2; FALHOU=1
fi
VAZIOS="$(find "$OUT" "$YAML" -type f -empty 2>/dev/null)"
if [ -n "$VAZIOS" ]; then
  echo "FALHA: arquivos vazios (parametro errado?):" >&2; echo "$VAZIOS" >&2; FALHOU=1
fi

N_JAVA="$(find "$OUT" -name '*.java' | wc -l)"
echo "Gerados: $N_JAVA arquivos Java (esperado: 21 entidades x 6 camadas, menos 2 resources = 124, mais 4 escritas a mao = 128) e $(ls "$YAML" | wc -l) OpenAPI."
exit "$FALHOU"
