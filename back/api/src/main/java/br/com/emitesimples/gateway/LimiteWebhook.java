package br.com.emitesimples.gateway;

import br.com.emitesimples.plataforma.LimiteJanelaFixa;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Limites do webhook publico em janela de 1 minuto: por IP de origem (antes do token) e por empresa (depois do token). */
@ApplicationScoped
public class LimiteWebhook {

    @ConfigProperty(name = "emitesimples.webhook.limite-por-ip-por-minuto", defaultValue = "120")
    int limiteIp;

    @ConfigProperty(name = "emitesimples.webhook.limite-por-empresa-por-minuto", defaultValue = "120")
    int limiteEmpresa;

    private final LimiteJanelaFixa porIp = new LimiteJanelaFixa(() -> limiteIp);
    private final LimiteJanelaFixa porEmpresa = new LimiteJanelaFixa(() -> limiteEmpresa);

    void limparTudo() {
        porIp.limparTudo();
        porEmpresa.limparTudo();
    }

    void consumirIp(String ip) {
        porIp.consumir(ip == null ? "?" : ip);
    }

    void consumirEmpresa(String empresaId) {
        porEmpresa.consumir(empresaId);
    }
}
