# Emite Simples: regras do fluxo (valem para todo agente e toda sessão)

> **Todo pedido de funcionalidade, função, alteração ou correção** começa carregando a skill `metodologia-mapper`
> (`.claude/skills/metodologia-mapper/SKILL.md`) e segue a esteira dela: documentação → mapa → gerador → geração → verificação.

## Regra de ouro: geradores e documentação mandam

O código deste projeto nasce do **mapa** (`.mi`) passando pelos **geradores** (`.mi`). A ordem de autoridade é:
**documentação (`docs/`, `front/docs/`, READMEs) → mapa → gerador → código gerado**.

1. **Nunca edite código gerado**, nem "ajuste" a saída na mão, nem desfaça a saída para esconder uma divergência. Se a saída
   está errada ou incompleta, o conserto é no mapa ou no gerador, e a saída é gerada de novo.
   - Gerados: `back/api/src/main/java/br/com/emitesimples/{configuracao,cadastros,usuarios,nfse,integranotas,admin,financeiro}/**`,
     `back/api/docs/openapi/**`, `front/web/src/gen/**`, `routeTree.gen.ts`, telas e rotas do front com o cabeçalho
     `GERADO por`, e `front/mapas/dominio/**` (cópia do back: use `front/mapas/scripts/sincronizar-dominio.sh`).
   - Um hook (`scripts/bloquear-codigo-gerado.sh`, registrado em `.claude/settings.json`) bloqueia essas edições. Não o
     contorne; o escape (`EMITE_PERMITIR_EDICAO_GERADA=1`) é só do usuário.
2. **Siga os guias.** Modelagem: `docs/domain-modeling-guide.md`. Geradores: `docs/generator-authoring-guide.md`. Telas:
   `front/docs/padroes-de-ui.md` e `front/docs/catalogo-componentes.md` (só componentes catalogados). Não invente
   propriedade, componente, função de dialeto ou padrão que o guia não documenta.
3. **Em caso de dúvida, PARE e PERGUNTE ao usuário.** Dúvida é: o guia não cobre, dois documentos divergem, o gerador não
   consegue produzir o que o mapa pede, uma decisão muda o contrato (API, banco, valores gravados), ou há mais de um
   caminho razoável. Faça uma pergunta objetiva com as opções e a sua recomendação; nunca escolha em silêncio.
4. **Divergência não se esconde.** Se mapa, gerador, documentação e código discordam, relate a divergência ao usuário
   (o que diz cada um) antes de qualquer correção.

## Onde vive o código escrito à mão

Só nos pacotes que o `gerar.sh` preserva: `auth, security, cep, plataforma, gateway, nfseconfig, nfseemissao, financeiroop, exceptions,
results, tenant` (back) e o runtime/telas à mão do front (`runtime/`, `lib/`, `mocks/`, `features/notas`, `features/admin`,
`features/empresa-config`, ...). Migrações Flyway são escritas à mão e **nunca** se altera uma já aplicada (crie a seguinte).

## Ações que exigem autorização explícita do usuário (uma por vez)

- `mi push` (nuvem do Mapper Idea): só com a frase de autorização do usuário, um push por autorização.
- Commit e push ao GitHub: só quando o usuário pedir.
- Emitir ou cancelar nota real: **nunca**. Só homologação, e só quando o usuário pedir.
- Containers Docker `erp-*` (de outro projeto): nunca tocar. Empresa nunca se apaga (só desativa). Segredos nunca em
  entidade, resposta ou log.

## Mecânica do dia a dia

- `mi` só no Git Bash (no PowerShell é alias de `Move-Item`). Pare o Vite antes de rodar `front/mapas/scripts/gerar.sh`.
- Vitest só no PowerShell com o diretório em `C:\` maiúsculo; nunca da raiz do repositório.
- Antes de entregar: back `mvn -B test`; front `npx tsc -b && npx oxlint && npx vitest run && npm run verificar:ui`.
