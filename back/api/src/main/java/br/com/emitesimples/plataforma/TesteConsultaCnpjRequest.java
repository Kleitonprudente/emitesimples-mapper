package br.com.emitesimples.plataforma;

/** Corpo do POST /api/admin/configuracoes/consulta-cnpj/testar. O CNPJ pode vir com ou sem pontuacao. */
public record TesteConsultaCnpjRequest(String cnpj) {
}
