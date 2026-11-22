package br.com.emitesimples.cep;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Corpos de resposta de /api/cep. */
public final class CepDtos {

    private CepDtos() {
    }

    public record MunicipioResumo(String id, String nome, String uf, String codigoIbge) {
    }

    /** Resposta de GET /api/cep/{cep}: o endereço do ViaCEP mais a linha de Municipio com o mesmo código IBGE (ou null). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CepResposta(String cep, String logradouro, String complemento, String bairro, String uf,
                              String codigoIbge, MunicipioResumo municipio) {
    }

    /** Endereço como o ViaCEP devolveu (sem a linha de Municipio, que sempre é consultada na hora). */
    public record Endereco(String cep, String logradouro, String complemento, String bairro, String uf,
                           String codigoIbge) {
    }
}
