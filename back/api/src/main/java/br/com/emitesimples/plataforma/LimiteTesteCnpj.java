package br.com.emitesimples.plataforma;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Limite PROPRIO do teste da consulta de CNPJ, por usuario (janela fixa de 1 minuto, em memoria). Cada teste gasta uma
 * consulta do plano do CNPJ.ws, por isso o padrao e baixo (10/min). Estourou: 429 muitas_tentativas com Retry-After.
 */
@ApplicationScoped
public class LimiteTesteCnpj {

    @ConfigProperty(name = "emitesimples.cnpj.limite-teste-por-minuto", defaultValue = "10")
    int limite;

    private final LimiteJanelaFixa janela = new LimiteJanelaFixa(() -> limite);

    void limparTudo() {
        janela.limparTudo();
    }

    public void consumir(String chave) {
        janela.consumir(chave);
    }
}
