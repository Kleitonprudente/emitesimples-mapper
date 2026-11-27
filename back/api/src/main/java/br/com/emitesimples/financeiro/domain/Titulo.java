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
* A promessa de receber ou pagar um valor numa data: uma conta a receber ou a 
* pagar. Uma linha por parcela; as parcelas (ou recorrências)
 * de um mesmo lançamento manual compartilham o idLancamento. O saldo (valor menos 
* quitado) e o "vencido" são calculados, nunca gravados.
 * O título só muda de situação quando o livro de movimentos grava uma baixa (Fase 
* 3) ou quando é cancelado.
 * NÃO tem CRUD gerado: só os serviços escritos à mão (pacote financeiroop) gravam 
* aqui, com as regras de docs/financeiro/README.md (F2-H1 a F2-H6):
 * parcelado e recorrente excludentes; rateio com o resíduo na última parcela; 
* pessoa com o papel certo; categoria ativa, do tipo certo e sem filhas;
 * edição só em aberto ou parcial e valor só em título manual sem baixa; 
* cancelamento recusado com baixa.
 * A pessoa é guardada como idPessoa (referência bruta), para o histórico 
* sobreviver a mudanças no cadastro. Nunca se apaga: cancela-se.
 * Vive no contexto financeiro (domain.financeiro), que referencia a Empresa 
* (configuracao).

*/
public class Titulo {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 7, message = "Attribute tipo must have 7 characters maximum.")
    private String tipo;

    @NotEmpty(message = "The attribute descricao can't be empty.")
    @Size(max = 255, message = "Attribute descricao must have 255 characters maximum.")
    private String descricao;

    @Size(max = 255, message = "Attribute observacao must have 255 characters maximum.")
    private String observacao;

    @Size(max = 36, message = "Attribute idPessoa must have 36 characters maximum.")
    private String idPessoa;

    @NotNull(message = "The attribute valor can't be null.")
    private BigDecimal valor;

    @NotNull(message = "The attribute quitado can't be null.")
    private BigDecimal quitado;

    private BigDecimal multaPercentual;

    private BigDecimal jurosMensalPercentual;

    @NotNull(message = "The attribute vencimento can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate vencimento;

    @Size(max = 20, message = "Attribute formaPrevista must have 20 characters maximum.")
    private String formaPrevista;

    @NotNull(message = "The attribute parcela can't be null.")
    private Integer parcela;

    @NotNull(message = "The attribute totalParcelas can't be null.")
    private Integer totalParcelas;

    @Size(max = 36, message = "Attribute idLancamento must have 36 characters maximum.")
    private String idLancamento;

    @NotEmpty(message = "The attribute origemTipo can't be empty.")
    @Size(max = 10, message = "Attribute origemTipo must have 10 characters maximum.")
    private String origemTipo;

    @Size(max = 36, message = "Attribute idOrigem must have 36 characters maximum.")
    private String idOrigem;

    @Size(max = 40, message = "Attribute origemNumero must have 40 characters maximum.")
    private String origemNumero;

    @NotEmpty(message = "The attribute situacao can't be empty.")
    @Size(max = 10, message = "Attribute situacao must have 10 characters maximum.")
    private String situacao;

    @NotNull(message = "The attribute paraRevisao can't be null.")
    private Boolean paraRevisao;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime canceladoEm;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime aprovadoEm;

    @Size(max = 36, message = "Attribute idAprovadoPor must have 36 characters maximum.")
    private String idAprovadoPor;

    @Size(max = 36, message = "Attribute idCriadoPor must have 36 characters maximum.")
    private String idCriadoPor;

    @Size(max = 36, message = "Attribute idCriadoPorCliente must have 36 characters maximum.")
    private String idCriadoPorCliente;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    private CategoriaFinanceira categoria;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public Titulo() {
        this.version = null;
        this.tipo = null;
        this.descricao = null;
        this.observacao = null;
        this.idPessoa = null;
        this.valor = null;
        this.quitado = null;
        this.multaPercentual = null;
        this.jurosMensalPercentual = null;
        this.vencimento = null;
        this.formaPrevista = null;
        this.parcela = null;
        this.totalParcelas = null;
        this.idLancamento = null;
        this.origemTipo = null;
        this.idOrigem = null;
        this.origemNumero = null;
        this.situacao = null;
        this.paraRevisao = null;
        this.canceladoEm = null;
        this.aprovadoEm = null;
        this.idAprovadoPor = null;
        this.idCriadoPor = null;
        this.idCriadoPorCliente = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.categoria = null;
        this.empresa = null;
    }
    public Titulo(
        LocalDateTime version,
        String tipo,
        String descricao,
        String observacao,
        String idPessoa,
        BigDecimal valor,
        BigDecimal quitado,
        BigDecimal multaPercentual,
        BigDecimal jurosMensalPercentual,
        LocalDate vencimento,
        String formaPrevista,
        Integer parcela,
        Integer totalParcelas,
        String idLancamento,
        String origemTipo,
        String idOrigem,
        String origemNumero,
        String situacao,
        Boolean paraRevisao,
        LocalDateTime canceladoEm,
        LocalDateTime aprovadoEm,
        String idAprovadoPor,
        String idCriadoPor,
        String idCriadoPorCliente,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        CategoriaFinanceira categoria,
        Empresa empresa
    ) {
        this.version = version;
        this.tipo = tipo;
        this.descricao = descricao;
        this.observacao = observacao;
        this.idPessoa = idPessoa;
        this.valor = valor;
        this.quitado = quitado;
        this.multaPercentual = multaPercentual;
        this.jurosMensalPercentual = jurosMensalPercentual;
        this.vencimento = vencimento;
        this.formaPrevista = formaPrevista;
        this.parcela = parcela;
        this.totalParcelas = totalParcelas;
        this.idLancamento = idLancamento;
        this.origemTipo = origemTipo;
        this.idOrigem = idOrigem;
        this.origemNumero = origemNumero;
        this.situacao = situacao;
        this.paraRevisao = paraRevisao;
        this.canceladoEm = canceladoEm;
        this.aprovadoEm = aprovadoEm;
        this.idAprovadoPor = idAprovadoPor;
        this.idCriadoPor = idCriadoPor;
        this.idCriadoPorCliente = idCriadoPorCliente;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.categoria = categoria;
        this.empresa = empresa;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return this.descricao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getObservacao() {
        return this.observacao;
    }

    public void setIdPessoa(String idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getIdPessoa() {
        return this.idPessoa;
    }
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
    public BigDecimal getValor() {
        return this.valor;
    }
    public void setQuitado(BigDecimal quitado) {
        this.quitado = quitado;
    }
    public BigDecimal getQuitado() {
        return this.quitado;
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

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public LocalDate getVencimento() {
        return this.vencimento;
    }

    public void setFormaPrevista(String formaPrevista) {
        this.formaPrevista = formaPrevista;
    }

    public String getFormaPrevista() {
        return this.formaPrevista;
    }

    public void setParcela(Integer parcela) {
        this.parcela = parcela;
    }

    public Integer getParcela() {
        return this.parcela;
    }

    public void setTotalParcelas(Integer totalParcelas) {
        this.totalParcelas = totalParcelas;
    }

    public Integer getTotalParcelas() {
        return this.totalParcelas;
    }

    public void setIdLancamento(String idLancamento) {
        this.idLancamento = idLancamento;
    }

    public String getIdLancamento() {
        return this.idLancamento;
    }

    public void setOrigemTipo(String origemTipo) {
        this.origemTipo = origemTipo;
    }

    public String getOrigemTipo() {
        return this.origemTipo;
    }

    public void setIdOrigem(String idOrigem) {
        this.idOrigem = idOrigem;
    }

    public String getIdOrigem() {
        return this.idOrigem;
    }

    public void setOrigemNumero(String origemNumero) {
        this.origemNumero = origemNumero;
    }

    public String getOrigemNumero() {
        return this.origemNumero;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getSituacao() {
        return this.situacao;
    }

    public void setParaRevisao(Boolean paraRevisao) {
        this.paraRevisao = paraRevisao;
    }

    public Boolean getParaRevisao() {
        return this.paraRevisao;
    }

    public void setCanceladoEm(LocalDateTime canceladoEm) {
        this.canceladoEm = canceladoEm;
    }

    public LocalDateTime getCanceladoEm() {
        return this.canceladoEm;
    }

    public void setAprovadoEm(LocalDateTime aprovadoEm) {
        this.aprovadoEm = aprovadoEm;
    }

    public LocalDateTime getAprovadoEm() {
        return this.aprovadoEm;
    }

    public void setIdAprovadoPor(String idAprovadoPor) {
        this.idAprovadoPor = idAprovadoPor;
    }

    public String getIdAprovadoPor() {
        return this.idAprovadoPor;
    }

    public void setIdCriadoPor(String idCriadoPor) {
        this.idCriadoPor = idCriadoPor;
    }

    public String getIdCriadoPor() {
        return this.idCriadoPor;
    }

    public void setIdCriadoPorCliente(String idCriadoPorCliente) {
        this.idCriadoPorCliente = idCriadoPorCliente;
    }

    public String getIdCriadoPorCliente() {
        return this.idCriadoPorCliente;
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

    public void setCategoria(CategoriaFinanceira categoria) {
        this.categoria = categoria;
    }

    public CategoriaFinanceira getCategoria() {
        return this.categoria;
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
            this.tipo != null || 
            this.descricao != null || 
            this.observacao != null || 
            this.idPessoa != null || 
            this.valor != null || 
            this.quitado != null || 
            this.multaPercentual != null || 
            this.jurosMensalPercentual != null || 
            this.vencimento != null || 
            this.formaPrevista != null || 
            this.parcela != null || 
            this.totalParcelas != null || 
            this.idLancamento != null || 
            this.origemTipo != null || 
            this.idOrigem != null || 
            this.origemNumero != null || 
            this.situacao != null || 
            this.paraRevisao != null || 
            this.canceladoEm != null || 
            this.aprovadoEm != null || 
            this.idAprovadoPor != null || 
            this.idCriadoPor != null || 
            this.idCriadoPorCliente != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.categoria != null || 
            this.empresa != null;
    }

}
