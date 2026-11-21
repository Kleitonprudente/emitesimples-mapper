package br.com.emitesimples.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.FaturaCrm;
import br.com.emitesimples.admin.builder.FaturaCrmEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Fatura RECEBIDA do CRM (PerfexCRM da Marke). É dado do CRM: só a sincronização a 
* escreve, e o usuário da empresa apenas LÊ as suas. Este sistema não processa 
* pagamento; o linkFatura leva a empresa a pagar no CRM. Não tem ambiente: herda a 
* situação da empresa.
 * EM ABERTO: como o CRM se comunica com o sistema (webhook ou consulta periódica) 
* não foi definido. O estado é PROVISÓRIO: espelha o estado no CRM e o mapeamento 
* exato ainda não foi definido. O log de sincronização com o CRM não foi modelado; 
* é pendência.

*/
@Entity
@Table(name = "FATURACRM")
public class FaturaCrmEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="CRM_FATURA_ID", length=50)
    private String crmFaturaId;

    @Column(name="NUMERO", length=30)
    private String numero;

    @Column(name="VALOR", precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name="DATA_EMISSAO")
    private LocalDate dataEmissao;

    @Column(name="DATA_VENCIMENTO")
    private LocalDate dataVencimento;

    @Column(name="DATA_PAGAMENTO")
    private LocalDate dataPagamento;

    @Column(name="ESTADO", length=10)
    private String estado;

    @Column(name="LINK_FATURA", length=500)
    private String linkFatura;

    @Column(name="DATA_SINCRONIZACAO")
    private LocalDateTime dataSincronizacao;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="ASSINATURA_ID", nullable = true)
    private AssinaturaEmpresaEntity assinatura;

    public FaturaCrmEntity() {
    }

    public FaturaCrmEntity(
        String crmFaturaId,
        String numero,
        BigDecimal valor,
        LocalDate dataEmissao,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        String estado,
        String linkFatura,
        LocalDateTime dataSincronizacao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        AssinaturaEmpresaEntity assinatura
    ) {
        this.crmFaturaId = crmFaturaId;
        this.numero = numero;
        this.valor = valor;
        this.dataEmissao = dataEmissao;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.estado = estado;
        this.linkFatura = linkFatura;
        this.dataSincronizacao = dataSincronizacao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.assinatura = assinatura;
    }

    public FaturaCrmEntity(FaturaCrmEntityBuilder builder) {
        this.version = builder.version;
        this.crmFaturaId = builder.crmFaturaId;
        this.numero = builder.numero;
        this.valor = builder.valor;
        this.dataEmissao = builder.dataEmissao;
        this.dataVencimento = builder.dataVencimento;
        this.dataPagamento = builder.dataPagamento;
        this.estado = builder.estado;
        this.linkFatura = builder.linkFatura;
        this.dataSincronizacao = builder.dataSincronizacao;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.assinatura = builder.assinatura;
    }

    public static FaturaCrmEntityBuilder builderOf() {
        return new FaturaCrmEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setCrmFaturaId(String crmFaturaId) {
        this.crmFaturaId = crmFaturaId;
    }

    public String getCrmFaturaId() {
        return this.crmFaturaId;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNumero() {
        return this.numero;
    }
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
    public BigDecimal getValor() {
        return this.valor;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataEmissao() {
        return this.dataEmissao;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public LocalDate getDataVencimento() {
        return this.dataVencimento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public LocalDate getDataPagamento() {
        return this.dataPagamento;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setLinkFatura(String linkFatura) {
        this.linkFatura = linkFatura;
    }

    public String getLinkFatura() {
        return this.linkFatura;
    }

    public void setDataSincronizacao(LocalDateTime dataSincronizacao) {
        this.dataSincronizacao = dataSincronizacao;
    }

    public LocalDateTime getDataSincronizacao() {
        return this.dataSincronizacao;
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

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

    public void setAssinatura(AssinaturaEmpresaEntity assinatura) {
        this.assinatura = assinatura;
    }

    public AssinaturaEmpresaEntity getAssinatura() {
        return this.assinatura;
    }

}
