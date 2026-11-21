package br.com.emitesimples.admin.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

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
public class FaturaCrm {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute crmFaturaId can't be empty.")
    @Size(max = 50, message = "Attribute crmFaturaId must have 50 characters maximum.")
    private String crmFaturaId;

    @Size(max = 30, message = "Attribute numero must have 30 characters maximum.")
    private String numero;

    @NotNull(message = "The attribute valor can't be null.")
    private BigDecimal valor;

    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataEmissao;

    @NotNull(message = "The attribute dataVencimento can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataVencimento;

    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataPagamento;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 10, message = "Attribute estado must have 10 characters maximum.")
    private String estado;

    @Size(max = 500, message = "Attribute linkFatura must have 500 characters maximum.")
    private String linkFatura;

    @NotNull(message = "The attribute dataSincronizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataSincronizacao;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    private AssinaturaEmpresa assinatura;

    public FaturaCrm() {
        this.version = null;
        this.crmFaturaId = null;
        this.numero = null;
        this.valor = null;
        this.dataEmissao = null;
        this.dataVencimento = null;
        this.dataPagamento = null;
        this.estado = null;
        this.linkFatura = null;
        this.dataSincronizacao = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.assinatura = null;
    }
    public FaturaCrm(
        LocalDateTime version,
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
        Empresa empresa,
        AssinaturaEmpresa assinatura
    ) {
        this.version = version;
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

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empresa getEmpresa() {
        return this.empresa;
    }

    public void setAssinatura(AssinaturaEmpresa assinatura) {
        this.assinatura = assinatura;
    }

    public AssinaturaEmpresa getAssinatura() {
        return this.assinatura;
    }

    @JsonIgnore
    public boolean isWeakReference() {
      return !this.isAnyNotKeyFieldsDefined()
              && (
                      this.isIdDefined()
              );
    }
    @JsonIgnore
    public boolean isIdDefined() {
        return this.id != null;
    }

    @JsonIgnore
    public boolean isAnyNotKeyFieldsDefined() {
        return 
            this.crmFaturaId != null || 
            this.numero != null || 
            this.valor != null || 
            this.dataEmissao != null || 
            this.dataVencimento != null || 
            this.dataPagamento != null || 
            this.estado != null || 
            this.linkFatura != null || 
            this.dataSincronizacao != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.assinatura != null;
    }

}
