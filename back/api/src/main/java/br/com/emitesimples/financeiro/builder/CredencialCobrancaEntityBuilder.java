package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CredencialCobranca;
import br.com.emitesimples.financeiro.entity.CredencialCobrancaEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * CredencialCobranca entity builder
*/
public class CredencialCobrancaEntityBuilder {
    public LocalDateTime version;
    public String provedor;
    public String endpoint;
    public String sellerId;
    public String referenciaToken;
    public String referenciaSegredoWebhook;
    public Boolean ativo;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public ContaFinanceiraEntity conta;
    public EmpresaEntity empresa;

    public CredencialCobrancaEntityBuilder() {
    }
    public CredencialCobrancaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CredencialCobrancaEntityBuilder withProvedor(String provedor) {
        this.provedor = provedor;
        return this;
    }

    public CredencialCobrancaEntityBuilder withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public CredencialCobrancaEntityBuilder withSellerId(String sellerId) {
        this.sellerId = sellerId;
        return this;
    }

    public CredencialCobrancaEntityBuilder withReferenciaToken(String referenciaToken) {
        this.referenciaToken = referenciaToken;
        return this;
    }

    public CredencialCobrancaEntityBuilder withReferenciaSegredoWebhook(String referenciaSegredoWebhook) {
        this.referenciaSegredoWebhook = referenciaSegredoWebhook;
        return this;
    }

    public CredencialCobrancaEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public CredencialCobrancaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CredencialCobrancaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CredencialCobrancaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CredencialCobrancaEntityBuilder withConta(ContaFinanceiraEntity conta) {
        this.conta = conta;
        return this;
    }

    public CredencialCobrancaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public CredencialCobrancaEntity build() {
        return new CredencialCobrancaEntity(this);
    }

    public CredencialCobrancaEntityBuilder withModel(CredencialCobranca credencialCobranca) {
        return this.withVersion(credencialCobranca.getVersion())
                .withProvedor(credencialCobranca.getProvedor())
                .withEndpoint(credencialCobranca.getEndpoint())
                .withSellerId(credencialCobranca.getSellerId())
                .withReferenciaToken(credencialCobranca.getReferenciaToken())
                .withReferenciaSegredoWebhook(credencialCobranca.getReferenciaSegredoWebhook())
                .withAtivo(credencialCobranca.getAtivo())
                .withId(credencialCobranca.getId())
                .withDataCriacao(credencialCobranca.getDataCriacao())
                .withDataAtualizacao(credencialCobranca.getDataAtualizacao())
                .withConta(ContaFinanceiraEntity.builderOf().withModel(credencialCobranca.getConta()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(credencialCobranca.getEmpresa()).build());
    }

    public List<CredencialCobrancaEntity> withModelList(List<CredencialCobranca> credencialCobrancaList) {
        if (credencialCobrancaList == null) {
            return null;
        }

        List<CredencialCobrancaEntity> list = new ArrayList<CredencialCobrancaEntity>(credencialCobrancaList.size());
        for (CredencialCobranca credencialCobranca: credencialCobrancaList) {
           list.add(withModel(credencialCobranca).build());
        }
        return list;
    }

}
