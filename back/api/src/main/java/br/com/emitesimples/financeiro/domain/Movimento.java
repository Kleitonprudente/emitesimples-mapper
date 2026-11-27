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
* O livro de movimentos: o dinheiro de fato entrando ou saindo de uma conta numa 
* data (uma linha do extrato). O coração do financeiro.
 * IMUTÁVEL: o banco recusa UPDATE e DELETE; erro se corrige com estorno (uma linha 
* nova, de sentido inverso). Não tem CRUD gerado:
 * só o LivroDeMovimentos (pacote financeiroop) grava aqui (baixar, estornar, 
* lançar avulso, transferir), com as regras de
 * docs/financeiro/README.md (F3-H1 a F3-H8). O valor é sempre maior que zero e o 
* sentido dá o sinal; numa linha de baixa,
 * valor = quitado + juros + multa − desconto; numa linha sem título, as quatro são 
* zero. A data nunca é futura nem anterior à abertura da conta.
 * O saldo de uma conta é sempre calculado: saldo inicial mais a soma do livro. 
* Vive no contexto financeiro (domain.financeiro).

*/
public class Movimento {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 24, message = "Attribute tipo must have 24 characters maximum.")
    private String tipo;

    @NotEmpty(message = "The attribute sentido can't be empty.")
    @Size(max = 7, message = "Attribute sentido must have 7 characters maximum.")
    private String sentido;

    @NotNull(message = "The attribute data can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate data;

    @Size(max = 255, message = "Attribute descricao must have 255 characters maximum.")
    private String descricao;

    @Size(max = 20, message = "Attribute forma must have 20 characters maximum.")
    private String forma;

    @NotNull(message = "The attribute valor can't be null.")
    private BigDecimal valor;

    @NotNull(message = "The attribute quitado can't be null.")
    private BigDecimal quitado;

    @NotNull(message = "The attribute juros can't be null.")
    private BigDecimal juros;

    @NotNull(message = "The attribute multa can't be null.")
    private BigDecimal multa;

    @NotNull(message = "The attribute desconto can't be null.")
    private BigDecimal desconto;

    @Size(max = 36, message = "Attribute idLote must have 36 characters maximum.")
    private String idLote;

    @Size(max = 36, message = "Attribute idTransferencia must have 36 characters maximum.")
    private String idTransferencia;

    @Size(max = 36, message = "Attribute idEstornoDe must have 36 characters maximum.")
    private String idEstornoDe;

    @Size(max = 36, message = "Attribute idCriadoPor must have 36 characters maximum.")
    private String idCriadoPor;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute conta can't be null.")
    private ContaFinanceira conta;

    private Titulo titulo;

    private CategoriaFinanceira categoria;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public Movimento() {
        this.version = null;
        this.tipo = null;
        this.sentido = null;
        this.data = null;
        this.descricao = null;
        this.forma = null;
        this.valor = null;
        this.quitado = null;
        this.juros = null;
        this.multa = null;
        this.desconto = null;
        this.idLote = null;
        this.idTransferencia = null;
        this.idEstornoDe = null;
        this.idCriadoPor = null;
        this.id = null;
        this.dataCriacao = null;
        this.conta = null;
        this.titulo = null;
        this.categoria = null;
        this.empresa = null;
    }
    public Movimento(
        LocalDateTime version,
        String tipo,
        String sentido,
        LocalDate data,
        String descricao,
        String forma,
        BigDecimal valor,
        BigDecimal quitado,
        BigDecimal juros,
        BigDecimal multa,
        BigDecimal desconto,
        String idLote,
        String idTransferencia,
        String idEstornoDe,
        String idCriadoPor,
        String id,
        LocalDateTime dataCriacao,
        ContaFinanceira conta,
        Titulo titulo,
        CategoriaFinanceira categoria,
        Empresa empresa
    ) {
        this.version = version;
        this.tipo = tipo;
        this.sentido = sentido;
        this.data = data;
        this.descricao = descricao;
        this.forma = forma;
        this.valor = valor;
        this.quitado = quitado;
        this.juros = juros;
        this.multa = multa;
        this.desconto = desconto;
        this.idLote = idLote;
        this.idTransferencia = idTransferencia;
        this.idEstornoDe = idEstornoDe;
        this.idCriadoPor = idCriadoPor;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.conta = conta;
        this.titulo = titulo;
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

    public void setSentido(String sentido) {
        this.sentido = sentido;
    }

    public String getSentido() {
        return this.sentido;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalDate getData() {
        return this.data;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return this.descricao;
    }

    public void setForma(String forma) {
        this.forma = forma;
    }

    public String getForma() {
        return this.forma;
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
    public void setJuros(BigDecimal juros) {
        this.juros = juros;
    }
    public BigDecimal getJuros() {
        return this.juros;
    }
    public void setMulta(BigDecimal multa) {
        this.multa = multa;
    }
    public BigDecimal getMulta() {
        return this.multa;
    }
    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }
    public BigDecimal getDesconto() {
        return this.desconto;
    }

    public void setIdLote(String idLote) {
        this.idLote = idLote;
    }

    public String getIdLote() {
        return this.idLote;
    }

    public void setIdTransferencia(String idTransferencia) {
        this.idTransferencia = idTransferencia;
    }

    public String getIdTransferencia() {
        return this.idTransferencia;
    }

    public void setIdEstornoDe(String idEstornoDe) {
        this.idEstornoDe = idEstornoDe;
    }

    public String getIdEstornoDe() {
        return this.idEstornoDe;
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

    public void setConta(ContaFinanceira conta) {
        this.conta = conta;
    }

    public ContaFinanceira getConta() {
        return this.conta;
    }

    public void setTitulo(Titulo titulo) {
        this.titulo = titulo;
    }

    public Titulo getTitulo() {
        return this.titulo;
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
            this.sentido != null || 
            this.data != null || 
            this.descricao != null || 
            this.forma != null || 
            this.valor != null || 
            this.quitado != null || 
            this.juros != null || 
            this.multa != null || 
            this.desconto != null || 
            this.idLote != null || 
            this.idTransferencia != null || 
            this.idEstornoDe != null || 
            this.idCriadoPor != null || 
            this.dataCriacao != null || 
            this.conta != null || 
            this.titulo != null || 
            this.categoria != null || 
            this.empresa != null;
    }

}
