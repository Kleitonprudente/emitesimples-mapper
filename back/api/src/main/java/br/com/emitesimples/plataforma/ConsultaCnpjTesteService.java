package br.com.emitesimples.plataforma;

import br.com.emitesimples.security.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Teste da consulta de CNPJ: limite proprio por usuario, validacao do CNPJ, e chamada ao CNPJ.ws com a URL base e o token
 * SALVOS. O token e lido so aqui, em variavel local, e entregue ao cliente; nunca e guardado nem devolvido.
 */
@ApplicationScoped
public class ConsultaCnpjTesteService {

    @Inject
    ConfiguracaoConsultaCnpjService config;

    @Inject
    ConsultaCnpjCliente cliente;

    @Inject
    LimiteTesteCnpj limite;

    /** URL validada, CNPJ normalizado e token, prontos para a chamada. */
    record Preparo(String url, String cnpj, String token) {
    }

    public ConsultaCnpjResumo testar(String usuarioId, String cnpjBruto) {
        limite.consumir(usuarioId);
        Preparo p = preparar(cnpjBruto);
        return cliente.consultar(p.url(), p.cnpj(), p.token());
    }

    /** Normaliza o CNPJ e le a configuracao salva (token, URL revalidada). Ordem dos erros: cnpj, token, url. */
    Preparo preparar(String cnpjBruto) {
        String cnpj = normalizar(cnpjBruto);
        String token = config.tokenAtual();
        if (token == null) {
            throw new ApiException(409, "token_nao_configurado",
                    "Nenhum token do CNPJ.ws está configurado. Informe o token na configuração da consulta de CNPJ.");
        }
        String url;
        try {
            url = config.validarUrl(config.obter().urlBase()); // revalida: a politica pode ter mudado desde a gravacao
        } catch (ApiException e) {
            throw new ApiException(409, "url_base_nao_permitida",
                    "A URL base configurada não é permitida. Corrija a configuração da consulta de CNPJ.");
        }
        return new Preparo(url, cnpj, token);
    }

    /** Aceita 14 digitos, com ou sem . / - ; confere os digitos verificadores. */
    static String normalizar(String bruto) {
        if (bruto == null || !bruto.strip().matches("[0-9./-]{14,18}")) {
            throw invalido();
        }
        String d = bruto.strip().replaceAll("[./-]", "");
        if (d.length() != 14 || d.chars().distinct().count() == 1) {
            throw invalido();
        }
        int d1 = digito(d, 12);
        int d2 = digito(d, 13);
        if (d.charAt(12) - '0' != d1 || d.charAt(13) - '0' != d2) {
            throw invalido();
        }
        return d;
    }

    /** Digito verificador na posicao {@code n} (12 ou 13), pesos 2..9 da direita para a esquerda sobre os n primeiros. */
    static int digito(String d, int n) {
        int soma = 0;
        int peso = 2;
        for (int i = n - 1; i >= 0; i--) {
            soma += (d.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    private static ApiException invalido() {
        return new ApiException(400, "cnpj_invalido", "CNPJ inválido. Informe 14 dígitos, com ou sem pontuação.");
    }
}
