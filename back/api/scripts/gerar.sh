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
  financeiro:Banco financeiro:ContaFinanceira financeiro:CategoriaFinanceira financeiro:CondicaoPagamento financeiro:ConfiguracaoFinanceira
  financeiro:Titulo financeiro:Movimento
  financeiro:LembreteCobranca financeiro:CobrancaEnviada financeiro:CredencialCobranca financeiro:CobrancaDestino financeiro:Cobranca
)
CONTEXTOS=(configuracao cadastros usuarios nfse integranotas admin financeiro)

# Entidades SEM recurso REST gerado: as que guardam hash de token (o endpoint publico de confirmacao e o de refresh sao escritos a
# mao) e o Titulo e o Movimento do financeiro (so os servicos da financeiroop gravam neles; docs/financeiro/README.md, A9, B4 e C3)
# Fase 6 e 7 do financeiro (docs/financeiro/README.md, 9.3 e 9.5): lembretes, cobrancas enviadas, credencial (referencias a segredo),
# destino de webhook (global) e cobranca: so os servicos da financeiroop gravam.
# ClienteAcesso (B3, 9.4): guarda o hash do segredo; criar e revogar so pelo endpoint a mao.
SEM_RESOURCE="TokenRenovacao ConfirmacaoCadastro Titulo Movimento LembreteCobranca CobrancaEnviada CredencialCobranca CobrancaDestino Cobranca ClienteAcesso"

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
echo "Gerados: $N_JAVA arquivos Java (gerados esperados: 28 entidades x 6 camadas, menos 4 resources = 164; o total inclui os pacotes escritos a mao) e $(ls "$YAML" | wc -l) OpenAPI."
exit "$FALHOU"
