package br.com.emitesimples.admin.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.FaturaCrm;
import br.com.emitesimples.admin.entity.FaturaCrmEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.admin.entity.AssinaturaEmpresaEntity;

/*
 * FaturaCrm entity builder
*/
public class FaturaCrmEntityBuilder {
    public LocalDateTime version;
    public String crmFaturaId;
    public String numero;
    public BigDecimal valor;
    public LocalDate dataEmissao;
    public LocalDate dataVencimento;
    public LocalDate dataPagamento;
    public String estado;
    public String linkFatura;
    public LocalDateTime dataSincronizacao;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public AssinaturaEmpresaEntity assinatura;

    public FaturaCrmEntityBuilder() {
    }
    public FaturaCrmEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public FaturaCrmEntityBuilder withCrmFaturaId(String crmFaturaId) {
        this.crmFaturaId = crmFaturaId;
        return this;
    }

    public FaturaCrmEntityBuilder withNumero(String numero) {
        this.numero = numero;
        return this;
    }
    public FaturaCrmEntityBuilder withValor(BigDecimal valor) {
        this.valor = valor;
        return this;
    }

    public FaturaCrmEntityBuilder withDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
        return this;
    }

    public FaturaCrmEntityBuilder withDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
        return this;
    }

    public FaturaCrmEntityBuilder withDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
        return this;
    }

    public FaturaCrmEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }

    public FaturaCrmEntityBuilder withLinkFatura(String linkFatura) {
        this.linkFatura = linkFatura;
        return this;
    }

    public FaturaCrmEntityBuilder withDataSincronizacao(LocalDateTime dataSincronizacao) {
        this.dataSincronizacao = dataSincronizacao;
        return this;
    }

    public FaturaCrmEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public FaturaCrmEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public FaturaCrmEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public FaturaCrmEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public FaturaCrmEntityBuilder withAssinatura(AssinaturaEmpresaEntity assinatura) {
        this.assinatura = assinatura;
        return this;
    }

    public FaturaCrmEntity build() {
        return new FaturaCrmEntity(this);
    }

    public FaturaCrmEntityBuilder withModel(FaturaCrm faturaCrm) {
        return this.withVersion(faturaCrm.getVersion())
                .withCrmFaturaId(faturaCrm.getCrmFaturaId())
                .withNumero(faturaCrm.getNumero())
                .withValor(faturaCrm.getValor())
                .withDataEmissao(faturaCrm.getDataEmissao())
                .withDataVencimento(faturaCrm.getDataVencimento())
                .withDataPagamento(faturaCrm.getDataPagamento())
                .withEstado(faturaCrm.getEstado())
                .withLinkFatura(faturaCrm.getLinkFatura())
                .withDataSincronizacao(faturaCrm.getDataSincronizacao())
                .withId(faturaCrm.getId())
                .withDataCriacao(faturaCrm.getDataCriacao())
                .withDataAtualizacao(faturaCrm.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(faturaCrm.getEmpresa()).build())
                .withAssinatura(AssinaturaEmpresaEntity.builderOf().withModel(faturaCrm.getAssinatura()).build());
    }

    public List<FaturaCrmEntity> withModelList(List<FaturaCrm> faturaCrmList) {
        if (faturaCrmList == null) {
            return null;
        }

        List<FaturaCrmEntity> list = new ArrayList<FaturaCrmEntity>(faturaCrmList.size());
        for (FaturaCrm faturaCrm: faturaCrmList) {
           list.add(withModel(faturaCrm).build());
        }
        return list;
    }

}
