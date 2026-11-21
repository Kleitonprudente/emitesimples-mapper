package br.com.emitesimples.security;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Corpo uniforme de erro devolvido ao front. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroApi(String codigo, String mensagem, Object detalhes) {
}
