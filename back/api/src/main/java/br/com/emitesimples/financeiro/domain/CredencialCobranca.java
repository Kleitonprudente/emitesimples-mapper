package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Integração de uma conta financeira com um provedor de cobrança (boleto, Pix, 
* link): docs/financeiro/README.md, 9.5, Fase 7; referência §9.2.
 * Uma por conta. O token e o segredo do webhook NUNCA ficam aqui: só a referência 
* ao cofre de segredos (SegredoService), como o token da Empresa.
 * O sellerId é único no sistema inteiro: o dinheiro nunca pode cair na conta 
* errada. Não tem CRUD gerado: a tela de integração (financeiroop) grava.

*/
public class CredencialCobranca {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute provedor can't be empty.")
    @Size(max = 20, message = "Attribute provedor must have 20 characters maximum.")
    private String provedor;

    @Size(max = 255, message = "Attribute endpoint must have 255 characters maximum.")
    private String endpoint;

    @NotEmpty(message = "The attribute sellerId can't be empty.")
    @Size(max = 100, message = "Attribute sellerId must have 100 characters maximum.")
    private String sellerId;

    @Size(max = 36, message = "Attribute referenciaToken must have 36 characters maximum.")
    private String referenciaToken;

    @Size(max = 36, message = "Attribute referenciaSegredoWebhook must have 36 characters maximum.")
    private String referenciaSegredoWebhook;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute conta can't be null.")
    private ContaFinanceira conta;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public CredencialCobranca() {
        this.version = null;
        this.provedor = null;
        this.endpoint = null;
        this.sellerId = null;
        this.referenciaToken = null;
        this.referenciaSegredoWebhook = null;
        this.ativo = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.conta = null;
        this.empresa = null;
    }
    public CredencialCobranca(
        LocalDateTime version,
        String provedor,
        String endpoint,
        String sellerId,
        String referenciaToken,
        String referenciaSegredoWebhook,
        Boolean ativo,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        ContaFinanceira conta,
        Empresa empresa
    ) {
        this.version = version;
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

    public void setConta(ContaFinanceira conta) {
        this.conta = conta;
    }

    public ContaFinanceira getConta() {
        return this.conta;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empresa getEmpresa() {
        return this.empresa;
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
            this.provedor != null || 
            this.endpoint != null || 
            this.sellerId != null || 
            this.referenciaToken != null || 
            this.referenciaSegredoWebhook != null || 
            this.ativo != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.conta != null || 
            this.empresa != null;
    }

}
