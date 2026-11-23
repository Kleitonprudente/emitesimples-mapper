-- V8: ImportacaoMunicipio guarda o usuario so como referencia de auditoria (idUsuario, CN-REL-002), sem chave estrangeira para
-- USUARIO: o contexto cadastros deixa de depender de usuarios. Os ids ja gravados sao copiados; o historico sobrevive mesmo que
-- o usuario seja apagado depois. A tabela e global (sem RLS, V3) e os GRANTs por tabela da V3 cobrem a coluna nova.
ALTER TABLE importacaomunicipio ADD COLUMN id_usuario varchar(36);
UPDATE importacaomunicipio SET id_usuario = usuario_id;
ALTER TABLE importacaomunicipio ALTER COLUMN id_usuario SET NOT NULL;

DROP INDEX IF EXISTS ix_importacaomunicipio_usuario;
ALTER TABLE importacaomunicipio DROP COLUMN usuario_id; -- leva junto a FK fkhtalf1w4ofunsv5kmekddlgxr
CREATE INDEX ix_importacaomunicipio_id_usuario ON importacaomunicipio (id_usuario);
