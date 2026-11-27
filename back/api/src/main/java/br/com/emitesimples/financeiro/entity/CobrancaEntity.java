package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Cobranca;
import br.com.emitesimples.financeiro.builder.CobrancaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

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
@Entity
@Table(name = "COBRANCA")
public class CobrancaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="PROVEDOR", length=20)
    private String provedor;

    @Column(name="TIPO", length=10)
    private String tipo;

    @Column(name="ESTADO", length=20)
    private String estado;

    @Column(name="VALOR", precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name="VENCIMENTO")
    private LocalDate vencimento;

    @Column(name="MULTA_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal multaPercentual;

    @Column(name="JUROS_MENSAL_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal jurosMensalPercentual;

    @Column(name="ID_EXTERNO", length=100)
    private String idExterno;

    @Column(name="LINHA_DIGITAVEL", length=60)
    private String linhaDigitavel;

    @Column(name="CODIGO_DE_BARRAS", length=60)
    private String codigoDeBarras;

    @Column(name="URL_DO_BOLETO", length=500)
    private String urlDoBoleto;

    @Column(name="PIX_COPIA_E_COLA", length=500)
    private String pixCopiaECola;

    @Column(name="URL_DO_PIX", length=500)
    private String urlDoPix;

    @Column(name="URL_DO_LINK", length=500)
    private String urlDoLink;

    @Column(name="PAGO_EM")
    private LocalDateTime pagoEm;

    @Column(name="VALOR_PAGO", precision = 15, scale = 2)
    private BigDecimal valorPago;

    @Column(name="MEIO_PAGO", length=10)
    private String meioPago;

    @Column(name="TARIFA", precision = 15, scale = 2)
    private BigDecimal tarifa;

    @Column(name="ID_BAIXA_MOVIMENTO", length=36)
    private String idBaixaMovimento;

    @Column(name="ID_TARIFA_MOVIMENTO", length=36)
    private String idTarifaMovimento;

    @Column(name="MOTIVO", length=200)
    private String motivo;

    @Column(name="RESPOSTA_DO_PROVEDOR", length=200)
    private String respostaDoProvedor;

    @Column(name="ULTIMA_CONSULTA_EM")
    private LocalDateTime ultimaConsultaEm;

    @Column(name="ID_CRIADO_POR", length=36)
    private String idCriadoPor;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="TITULO_ID", nullable = false)
    private TituloEntity titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CONTA_ID", nullable = false)
    private ContaFinanceiraEntity conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public CobrancaEntity() {
    }

    public CobrancaEntity(
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
        TituloEntity titulo,
        ContaFinanceiraEntity conta,
        EmpresaEntity empresa
    ) {
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

    public CobrancaEntity(CobrancaEntityBuilder builder) {
        this.version = builder.version;
        this.provedor = builder.provedor;
        this.tipo = builder.tipo;
        this.estado = builder.estado;
        this.valor = builder.valor;
        this.vencimento = builder.vencimento;
        this.multaPercentual = builder.multaPercentual;
        this.jurosMensalPercentual = builder.jurosMensalPercentual;
        this.idExterno = builder.idExterno;
        this.linhaDigitavel = builder.linhaDigitavel;
        this.codigoDeBarras = builder.codigoDeBarras;
        this.urlDoBoleto = builder.urlDoBoleto;
        this.pixCopiaECola = builder.pixCopiaECola;
        this.urlDoPix = builder.urlDoPix;
        this.urlDoLink = builder.urlDoLink;
        this.pagoEm = builder.pagoEm;
        this.valorPago = builder.valorPago;
        this.meioPago = builder.meioPago;
        this.tarifa = builder.tarifa;
        this.idBaixaMovimento = builder.idBaixaMovimento;
        this.idTarifaMovimento = builder.idTarifaMovimento;
        this.motivo = builder.motivo;
        this.respostaDoProvedor = builder.respostaDoProvedor;
        this.ultimaConsultaEm = builder.ultimaConsultaEm;
        this.idCriadoPor = builder.idCriadoPor;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.titulo = builder.titulo;
        this.conta = builder.conta;
        this.empresa = builder.empresa;
    }

    public static CobrancaEntityBuilder builderOf() {
        return new CobrancaEntityBuilder();
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

    public void setTitulo(TituloEntity titulo) {
        this.titulo = titulo;
    }

    public TituloEntity getTitulo() {
        return this.titulo;
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
