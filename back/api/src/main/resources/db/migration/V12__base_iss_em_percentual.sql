-- V12: base de calculo do ISS informada em PERCENTUAL (decisao do dono, 2026-11-25; docs/historias-emissao-nota.md, "Tributacao
-- da nota"). O servico guarda o percentual padrao (vale para qualquer valor de nota) no lugar da base em R$ da V11; a nota guarda o
-- percentual usado e continua com VALOR_BASE_CALCULO_ISS (R$, o que vai ao IntegraNotas), calculado pelo sistema:
-- (valor - desconto incondicionado) x percentual / 100.
-- SERVICO.BASE_CALCULO_ISS (V11) sai: so existia em ambiente de desenvolvimento, sem uso em producao.
ALTER TABLE SERVICO DROP COLUMN BASE_CALCULO_ISS;
ALTER TABLE SERVICO ADD COLUMN PERCENTUAL_BASE_CALCULO_ISS numeric(5,2);
ALTER TABLE NOTA ADD COLUMN PERCENTUAL_BASE_CALCULO_ISS numeric(5,2);
