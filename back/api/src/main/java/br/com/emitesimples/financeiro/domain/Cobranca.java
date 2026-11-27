package br.com.emitesimples.financeiro.domain;

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
* Uma cobrança gerada num provedor (boleto, Pix ou link de pagamento) para o saldo 
* de um título a receber (docs/financeiro/README.md, 9.5, Fase 7; referência 
* §9.2).
 * UMA cobrança viva por título (índice parcial em CRIANDO, AGUARDANDO e INCERTA). 
* A chamada ao provedor fica FORA da transação e nunca é repetida sozinha: sem 
* resposta, INCERTA.
 * Só o sincronizador muda o estado depois de criada; o pagamento dá a baixa pelo 
* livro (idempotente por idBaixaMovimento), excedente vira juros e a tarifa vira 
* saída avulsa.
 * Não tem CRUD gerado: só os serviços da financeiroop gravam.

*/
public class Cobranca {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute provedor can't be empty.")
    @Size(max = 20, message = "Attribute provedor must have 20 characters maximum.")
    private String provedor;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 10, message = "Attribute tipo must have 10 characters maximum.")
    private String tipo;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 20, message = "Attribute estado must have 20 characters maximum.")
    private String estado;

    @NotNull(message = "The attribute valor can't be null.")
    private BigDecimal valor;

    @NotNull(message = "The attribute vencimento can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate vencimento;

    private BigDecimal multaPercentual;

    private BigDecimal jurosMensalPercentual;

    @Size(max = 100, message = "Attribute idExterno must have 100 characters maximum.")
    private String idExterno;

    @Size(max = 60, message = "Attribute linhaDigitavel must have 60 characters maximum.")
    private String linhaDigitavel;

    @Size(max = 60, message = "Attribute codigoDeBarras must have 60 characters maximum.")
    private String codigoDeBarras;

    @Size(max = 500, message = "Attribute urlDoBoleto must have 500 characters maximum.")
    private String urlDoBoleto;

    @Size(max = 500, message = "Attribute pixCopiaECola must have 500 characters maximum.")
    private String pixCopiaECola;

    @Size(max = 500, message = "Attribute urlDoPix must have 500 characters maximum.")
    private String urlDoPix;

    @Size(max = 500, message = "Attribute urlDoLink must have 500 characters maximum.")
    private String urlDoLink;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime pagoEm;

    private BigDecimal valorPago;

    @Size(max = 10, message = "Attribute meioPago must have 10 characters maximum.")
    private String meioPago;

    private BigDecimal tarifa;

    @Size(max = 36, message = "Attribute idBaixaMovimento must have 36 characters maximum.")
    private String idBaixaMovimento;

    @Size(max = 36, message = "Attribute idTarifaMovimento must have 36 characters maximum.")
    private String idTarifaMovimento;

    @Size(max = 200, message = "Attribute motivo must have 200 characters maximum.")
    private String motivo;

    @Size(max = 200, message = "Attribute respostaDoProvedor must have 200 characters maximum.")
    private String respostaDoProvedor;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime ultimaConsultaEm;

    @Size(max = 36, message = "Attribute idCriadoPor must have 36 characters maximum.")
    private String idCriadoPor;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute titulo can't be null.")
    private Titulo titulo;

    @NotNull(message = "The attribute conta can't be null.")
    private ContaFinanceira conta;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public Cobranca() {
        this.version = null;
        this.provedor = null;
        this.tipo = null;
        this.estado = null;
        this.valor = null;
        this.vencimento = null;
        this.multaPercentual = null;
        this.jurosMensalPercentual = null;
        this.idExterno = null;
        this.linhaDigitavel = null;
        this.codigoDeBarras = null;
        this.urlDoBoleto = null;
        this.pixCopiaECola = null;
        this.urlDoPix = null;
        this.urlDoLink = null;
        this.pagoEm = null;
        this.valorPago = null;
        this.meioPago = null;
        this.tarifa = null;
        this.idBaixaMovimento = null;
        this.idTarifaMovimento = null;
        this.motivo = null;
        this.respostaDoProvedor = null;
        this.ultimaConsultaEm = null;
        this.idCriadoPor = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.titulo = null;
        this.conta = null;
        this.empresa = null;
    }
    public Cobranca(
        LocalDateTime version,
        String provedor,
        String tipo,
        String estado,
        BigDecimal valor,
        LocalDate vencimento,
        BigDecimal multaPercentual,
        BigDecimal jurosMensalPercentual,
        String idExterno,
        String linhaDigitavel,
        String codigoDeBarras,
        String urlDoBoleto,
        String pixCopiaECola,
        String urlDoPix,
        String urlDoLink,
        LocalDateTime pagoEm,
        BigDecimal valorPago,
        String meioPago,
        BigDecimal tarifa,
        String idBaixaMovimento,
        String idTarifaMovimento,
        String motivo,
        String respostaDoProvedor,
        LocalDateTime ultimaConsultaEm,
        String idCriadoPor,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Titulo titulo,
        ContaFinanceira conta,
        Empresa empresa
    ) {
        this.version = version;
        this.provedor = provedor;
        this.tipo = tipo;
        this.estado = estado;
        this.valor = valor;
        this.vencimento = vencimento;
        this.multaPercentual = multaPercentual;
        this.jurosMensalPercentual = jurosMensalPercentual;
        this.idExterno = idExterno;
        this.linhaDigitavel = linhaDigitavel;
        this.codigoDeBarras = codigoDeBarras;
        this.urlDoBoleto = urlDoBoleto;
        this.pixCopiaECola = pixCopiaECola;
        this.urlDoPix = urlDoPix;
        this.urlDoLink = urlDoLink;
        this.pagoEm = pagoEm;
        this.valorPago = valorPago;
        this.meioPago = meioPago;
        this.tarifa = tarifa;
        this.idBaixaMovimento = idBaixaMovimento;
        this.idTarifaMovimento = idTarifaMovimento;
        this.motivo = motivo;
        this.respostaDoProvedor = respostaDoProvedor;
        this.ultimaConsultaEm = ultimaConsultaEm;
        this.idCriadoPor = idCriadoPor;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.titulo = titulo;
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

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
    public BigDecimal getValor() {
        return this.valor;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public LocalDate getVencimento() {
        return this.vencimento;
    }
    public void setMultaPercentual(BigDecimal multaPercentual) {
        this.multaPercentual = multaPercentual;
    }
    public BigDecimal getMultaPercentual() {
        return this.multaPercentual;
    }
    public void setJurosMensalPercentual(BigDecimal jurosMensalPercentual) {
        this.jurosMensalPercentual = jurosMensalPercentual;
    }
    public BigDecimal getJurosMensalPercentual() {
        return this.jurosMensalPercentual;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public String getIdExterno() {
        return this.idExterno;
    }

    public void setLinhaDigitavel(String linhaDigitavel) {
        this.linhaDigitavel = linhaDigitavel;
    }

    public String getLinhaDigitavel() {
        return this.linhaDigitavel;
    }

    public void setCodigoDeBarras(String codigoDeBarras) {
        this.codigoDeBarras = codigoDeBarras;
    }

    public String getCodigoDeBarras() {
        return this.codigoDeBarras;
    }

    public void setUrlDoBoleto(String urlDoBoleto) {
        this.urlDoBoleto = urlDoBoleto;
    }

    public String getUrlDoBoleto() {
        return this.urlDoBoleto;
    }

    public void setPixCopiaECola(String pixCopiaECola) {
        this.pixCopiaECola = pixCopiaECola;
    }

    public String getPixCopiaECola() {
        return this.pixCopiaECola;
    }

    public void setUrlDoPix(String urlDoPix) {
        this.urlDoPix = urlDoPix;
    }

    public String getUrlDoPix() {
        return this.urlDoPix;
    }

    public void setUrlDoLink(String urlDoLink) {
        this.urlDoLink = urlDoLink;
    }

    public String getUrlDoLink() {
        return this.urlDoLink;
    }

    public void setPagoEm(LocalDateTime pagoEm) {
        this.pagoEm = pagoEm;
    }

    public LocalDateTime getPagoEm() {
        return this.pagoEm;
    }
    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }
    public BigDecimal getValorPago() {
        return this.valorPago;
    }

    public void setMeioPago(String meioPago) {
        this.meioPago = meioPago;
    }

    public String getMeioPago() {
        return this.meioPago;
    }
    public void setTarifa(BigDecimal tarifa) {
        this.tarifa = tarifa;
    }
    public BigDecimal getTarifa() {
        return this.tarifa;
    }

    public void setIdBaixaMovimento(String idBaixaMovimento) {
        this.idBaixaMovimento = idBaixaMovimento;
    }

    public String getIdBaixaMovimento() {
        return this.idBaixaMovimento;
    }

    public void setIdTarifaMovimento(String idTarifaMovimento) {
        this.idTarifaMovimento = idTarifaMovimento;
    }

    public String getIdTarifaMovimento() {
        return this.idTarifaMovimento;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return this.motivo;
    }

    public void setRespostaDoProvedor(String respostaDoProvedor) {
        this.respostaDoProvedor = respostaDoProvedor;
    }

    public String getRespostaDoProvedor() {
        return this.respostaDoProvedor;
    }

    public void setUltimaConsultaEm(LocalDateTime ultimaConsultaEm) {
        this.ultimaConsultaEm = ultimaConsultaEm;
    }

    public LocalDateTime getUltimaConsultaEm() {
        return this.ultimaConsultaEm;
    }

    public void setIdCriadoPor(String idCriadoPor) {
        this.idCriadoPor = idCriadoPor;
    }

    public String getIdCriadoPor() {
        return this.idCriadoPor;
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

    public void setTitulo(Titulo titulo) {
        this.titulo = titulo;
    }

    public Titulo getTitulo() {
        return this.titulo;
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
            this.tipo != null || 
            this.estado != null || 
            this.valor != null || 
            this.vencimento != null || 
            this.multaPercentual != null || 
            this.jurosMensalPercentual != null || 
            this.idExterno != null || 
            this.linhaDigitavel != null || 
            this.codigoDeBarras != null || 
            this.urlDoBoleto != null || 
            this.pixCopiaECola != null || 
            this.urlDoPix != null || 
            this.urlDoLink != null || 
            this.pagoEm != null || 
            this.valorPago != null || 
            this.meioPago != null || 
            this.tarifa != null || 
            this.idBaixaMovimento != null || 
            this.idTarifaMovimento != null || 
            this.motivo != null || 
            this.respostaDoProvedor != null || 
            this.ultimaConsultaEm != null || 
            this.idCriadoPor != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.titulo != null || 
            this.conta != null || 
            this.empresa != null;
    }

}
