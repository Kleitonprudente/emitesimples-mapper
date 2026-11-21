# Carga e administração da tabela de cidades (`Municipio`)

**Fonte**: arquivo `municipios.csv` baixado do site do IBGE pelo usuário (inspecionado em 2026-11-21) e decisões do usuário.
Este documento especifica a importação; a importação em si é código escrito à mão no `emitesimples-api` (o pack
Quarkus não gera upload de tabela inteira: o upload dele é preso a um registro e usa `long` no id, e os ids deste
projeto são UUID em texto).

## Quem controla

Só o **administrador da plataforma** altera a tabela, por **upload de arquivo novo** e por **correção manual**, quando o
IBGE muda algo. Os demais usuários só leem. Os recursos gerados não restringem a escrita: a regra de administrador é da
aplicação e ainda não existe (**lacuna de segurança enquanto isso não for feito**).

## Formato do arquivo (como veio em 2026-11-21)

- Separador `;`, fim de linha CRLF, **codificação Windows-1252** (não UTF-8). A importação deve detectar UTF-8 válido e,
  se não for, ler como Windows-1252; e aceitar `;`, `,` ou tabulação.
- Cabeçalho (com acentos): `CÓDIGO DO MUNICÍPIO - TOM`, `CÓDIGO DO MUNICÍPIO - IBGE`, `MUNICÍPIO - TOM`,
  `MUNICÍPIO - IBGE`, `UF`. Ler as colunas **pelo nome do cabeçalho**. As duas colunas **TOM** (código antigo da Receita,
  diferente do IBGE) são **ignoradas**.
- 5.571 linhas de dados: 5.570 municípios, mais a linha `EXTERIOR`. O código IBGE é único e não há par (nome, UF)
  repetido.

| Coluna do arquivo | Atributo de `Municipio` |
|---|---|
| `CÓDIGO DO MUNICÍPIO - IBGE` | `codigoIbge` (7 dígitos) |
| `MUNICÍPIO - IBGE` | `nome` (o nome com maiúsculas e acentos corretos) |
| `UF` | `uf` (2 letras) |

## O registro EXTERIOR (decisão do usuário: **fica**)

A linha `9707;0;EXTERIOR;;EX` **não é rejeitada**: ela é a "cidade" usada em notas para o exterior.
- UF `EX`. Código IBGE `0` (a tabela oficial não dá IBGE ao exterior). O nome do IBGE vem vazio no arquivo: o registro
  fica com o nome **`Exterior`** (decisão provisória, a confirmar com o usuário).
- **Ao enviar a NFS-e** de uma pessoa cuja cidade é `Exterior`, o sistema manda `uf = EX` e **não manda**
  `codigo_municipio` (no IntegraNotas ele é opcional). O IBGE `0` nunca vai ao gateway.
- A `Pessoa` estrangeira usa `cidade = Exterior` e os campos de exportação (`codigoPais`, `codigoPostal`, município e
  província no exterior). A `Empresa` **não** pode ter o `Exterior` como cidade (regra da aplicação).
- Por isso as UFs válidas são as **27 do Brasil mais `EX`**, e a regra "IBGE com 7 dígitos" vale para todos os
  registros **exceto** o `Exterior`.

## Regras da importação

1. **Atualiza ou insere pelo código IBGE** (`codigoIbge`). Nunca apaga: `Pessoa` e `Empresa` referenciam as cidades.
2. **Aceita** a linha se: o código tem 7 dígitos (ou é o `Exterior`: UF `EX` e código `0`), a UF é uma das 27 ou `EX`, e o
   nome não está vazio (no `Exterior` o nome é `Exterior`).
3. **Rejeita**, sem adivinhar, e informa no relatório com o número da linha e o motivo: código fora do padrão, UF
   inválida, nome vazio, código repetido dentro do arquivo.
4. Devolve um **relatório**: lidas, inseridas, atualizadas, iguais e rejeitadas.
5. A unicidade de `codigoIbge` e de (`nome`, `uf`) precisa ser **índice único no banco** (o mapa não a expressa).

## Linha a corrigir no arquivo de 2026-11-21

`9997;5003157;CORONEL SAPUCAIA;;MS`: o nome do IBGE está **vazio** (só existe o nome em maiúsculas da coluna TOM). O código
`5003157` é válido. A importação rejeita a linha e o administrador a corrige à mão (ou o nome é preenchido no arquivo
antes de enviar). Não se deve deduzir o nome pela caixa alta do TOM.

## Pendente

- Entidade de histórico dos envios (`ImportacaoMunicipio`): **modelada** (decisão do usuário em 2026-11-21), no contexto
  `cadastros`; ainda não validada no CLI nem gerada.
- Esquema do banco: **o Flyway é o dono** (decisão do usuário em 2026-11-21); o Hibernate só valida (`validate`). Os
  índices únicos (`codigo_ibge` e `nome` + `uf`) entram em migração.
- A regra de administrador para escrever em `Municipio`.
