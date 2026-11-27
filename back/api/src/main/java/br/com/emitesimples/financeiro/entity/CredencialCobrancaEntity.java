package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CredencialCobranca;
import br.com.emitesimples.financeiro.builder.CredencialCobrancaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Integração de uma conta financeira com um provedor de cobrança (boleto, Pix, 
* link): docs/financeiro/README.md, 9.5, Fase 7; referência §9.2.
 * Uma por conta. O token e o segredo do webhook NUNCA ficam aqui: só a referência 
* ao cofre de segredos (SegredoService), como o token da Empresa.
 * O sellerId é único no sistema inteiro: o dinheiro nunca pode cair na conta 
* errada. Não tem CRUD gerado: a tela de integração (financeiroop) grava.

*/
@Entity
@Table(name = "CREDENCIALCOBRANCA")
public class CredencialCobrancaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="PROVEDOR", length=20)
    private String provedor;

    @Column(name="ENDPOINT", length=255)
    private String endpoint;

    @Column(name="SELLER_ID", length=100)
    private String sellerId;

    @Column(name="REFERENCIA_TOKEN", length=36)
    private String referenciaToken;

    @Column(name="REFERENCIA_SEGREDO_WEBHOOK", length=36)
    private String referenciaSegredoWebhook;

    @Column(name="ATIVO")
    private Boolean ativo;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CONTA_ID", nullable = false)
    private ContaFinanceiraEntity conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public CredencialCobrancaEntity() {
    }

    public CredencialCobrancaEntity(
        String provedor,
        String endpoint,
        String sellerId,
        String referenciaToken,
        String referenciaSegredoWebhook,
        Boolean ativo,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        ContaFinanceiraEntity conta,
        EmpresaEntity empresa
    ) {
        this.provedor = provedor;
        this.endpoint = endpoint;
        this.sellerId = sellerId;
        this.referenciaToken = referenciaToken;
        this.referenciaSegredoWebhook = referenciaSegredoWebhook;
        this.ativo = ativo;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.conta = conta;
        this.empresa = empresa;
    }

    public CredencialCobrancaEntity(CredencialCobrancaEntityBuilder builder) {
        this.version = builder.version;
        this.provedor = builder.provedor;
        this.endpoint = builder.endpoint;
        this.sellerId = builder.sellerId;
        this.referenciaToken = builder.referenciaToken;
        this.referenciaSegredoWebhook = builder.referenciaSegredoWebhook;
        this.ativo = builder.ativo;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.conta = builder.conta;
        this.empresa = builder.empresa;
    }

    public static CredencialCobrancaEntityBuilder builderOf() {
        return new CredencialCobrancaEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setProvedor(String provedor) {
        this.provedor = provedor;
    }

    public String getProvedor() {
        return this.provedor;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerId() {
        return this.sellerId;
    }

    public void setReferenciaToken(String referenciaToken) {
        this.referenciaToken = referenciaToken;
    }

    public String getReferenciaToken() {
        return this.referenciaToken;
    }

    public void setReferenciaSegredoWebhook(String referenciaSegredoWebhook) {
        this.referenciaSegredoWebhook = referenciaSegredoWebhook;
    }

    public String getReferenciaSegredoWebhook() {
        return this.referenciaSegredoWebhook;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataCriacao() {
        return this.dataCriacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return this.dataAtualizacao;
    }

    public void setConta(ContaFinanceiraEntity conta) {
        this.conta = conta;
    }

    public ContaFinanceiraEntity getConta() {
        return this.conta;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
