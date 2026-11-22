package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.plataforma.LimiteJanelaFixa;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Limite do envio de certificado por usuario (janela fixa de 1 minuto, em memoria): 429 com Retry-After ao estourar. */
@ApplicationScoped
public class LimiteCertificado {

    @ConfigProperty(name = "emitesimples.certificado.limite-por-minuto", defaultValue = "10")
    int limite;

    private final LimiteJanelaFixa janela = new LimiteJanelaFixa(() -> limite);

    void limparTudo() {
        janela.limparTudo();
    }

    public void consumir(String chave) {
        janela.consumir(chave);
    }
}
