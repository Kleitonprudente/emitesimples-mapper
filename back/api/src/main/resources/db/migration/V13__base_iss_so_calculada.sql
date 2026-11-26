-- V13: base do ISS e ISS retido passam a ser SO calculados (decisao do dono, 2026-11-25, depois da homologacao; ver
-- docs/homologacao/2026-11-25-tributacao). O governo ignora a base informada e recalcula vBC = vServ - vDescIncond; o percentual
-- da V12 nao tinha efeito na nota autorizada. NOTA.VALOR_BASE_CALCULO_ISS e NOTA.VALOR_ISS_RETIDO continuam (o sistema calcula).
ALTER TABLE SERVICO DROP COLUMN PERCENTUAL_BASE_CALCULO_ISS;
ALTER TABLE NOTA DROP COLUMN PERCENTUAL_BASE_CALCULO_ISS;
