package br.com.emitesimples.plataforma;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Limite da consulta de CNPJ para preencher o cadastro, por usuario (padrao 30/min; separado do limite do teste). */
@ApplicationScoped
public class LimiteConsultaCnpj {

    @ConfigProperty(name = "emitesimples.cnpj.limite-consulta-por-minuto", defaultValue = "30")
    int limite;

    private final LimiteJanelaFixa janela = new LimiteJanelaFixa(() -> limite);

    void limparTudo() {
        janela.limparTudo();
    }

    public void consumir(String chave) {
        janela.consumir(chave);
    }
}
