package br.com.emitesimples.exceptions;

/**
 * Classe de suporte escrita à mão: o código gerado a importa, mas nenhum gerador a produz.
 */
public class NotFoundException extends jakarta.ws.rs.WebApplicationException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}
