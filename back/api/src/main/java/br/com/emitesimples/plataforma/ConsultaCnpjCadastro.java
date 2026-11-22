package br.com.emitesimples.plataforma;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Dados do CNPJ.ws no formato dos campos da Empresa, para o front preencher o cadastro. Ausente = null (chave sempre presente). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConsultaCnpjCadastro(String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
                                   String cep, String logradouro, String numeroEndereco, String complemento,
                                   String bairro, MunicipioRef municipio, String uf, String codigoIbge,
                                   Integer regimeTributario, String inscricaoEstadual, String situacaoCadastral,
                                   String dataInicioAtividade, List<Socio> socios) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MunicipioRef(String id, String nome, String uf, String codigoIbge) {
    }

    /** Socio do QSA do CNPJ.ws; tipo e qualificacao ausentes = null. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Socio(String nome, String tipo, String qualificacao) {
    }
}
