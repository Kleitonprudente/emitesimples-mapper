package br.com.emitesimples.plataforma;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resumo da consulta de teste ao CNPJ.ws. Campos ausentes na resposta do provedor saem como null (sempre presentes no
 * JSON). {@code urlConsultada} NUNCA contem o token.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConsultaCnpjResumo(String cnpj, String razaoSocial, String nomeFantasia, String situacaoCadastral,
                                 String dataInicioAtividade, String porte, String naturezaJuridica,
                                 Atividade atividadePrincipal, String capitalSocial, Endereco endereco, String email,
                                 String telefone, Boolean simples, Boolean mei, int quantidadeSocios, int duracaoMs,
                                 String urlConsultada) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Atividade(String codigo, String descricao) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Endereco(String logradouro, String numero, String complemento, String bairro, String cep,
                           String municipio, String uf) {
    }
}
