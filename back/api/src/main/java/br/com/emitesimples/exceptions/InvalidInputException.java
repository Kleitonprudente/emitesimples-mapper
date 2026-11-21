package br.com.emitesimples.exceptions;

/**
 * Classe de suporte escrita à mão: o código gerado a importa, mas nenhum gerador a produz.
 */
public class InvalidInputException extends jakarta.ws.rs.WebApplicationException {
    public InvalidInputException(String message) {
        super(message, 400);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause, 400);
    }
}
