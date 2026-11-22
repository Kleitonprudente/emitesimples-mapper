package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.plataforma.LimiteJanelaFixa;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Limite de criar/enviar NFS-e por usuario (padrao 30/min, janela fixa, em memoria): excedido = 429. */
@ApplicationScoped
public class LimiteEnvioNfse {

    @ConfigProperty(name = "emitesimples.nfse.limite-envios-por-minuto", defaultValue = "30")
    int limite;

    private final LimiteJanelaFixa janela = new LimiteJanelaFixa(() -> limite);

    public void consumir(String usuarioId) {
        janela.consumir(usuarioId == null ? "?" : usuarioId);
    }
}
