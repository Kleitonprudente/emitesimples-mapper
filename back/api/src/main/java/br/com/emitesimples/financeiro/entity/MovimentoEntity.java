package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Movimento;
import br.com.emitesimples.financeiro.builder.MovimentoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

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
@Entity
@Table(name = "MOVIMENTO")
public class MovimentoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TIPO", length=24)
    private String tipo;

    @Column(name="SENTIDO", length=7)
    private String sentido;

    @Column(name="DATA")
    private LocalDate data;

    @Column(name="DESCRICAO", length=255)
    private String descricao;

    @Column(name="FORMA", length=20)
    private String forma;

    @Column(name="VALOR", precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name="QUITADO", precision = 15, scale = 2)
    private BigDecimal quitado;

    @Column(name="JUROS", precision = 15, scale = 2)
    private BigDecimal juros;

    @Column(name="MULTA", precision = 15, scale = 2)
    private BigDecimal multa;

    @Column(name="DESCONTO", precision = 15, scale = 2)
    private BigDecimal desconto;

    @Column(name="ID_LOTE", length=36)
    private String idLote;

    @Column(name="ID_TRANSFERENCIA", length=36)
    private String idTransferencia;

    @Column(name="ID_ESTORNO_DE", length=36)
    private String idEstornoDe;

    @Column(name="ID_CRIADO_POR", length=36)
    private String idCriadoPor;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CONTA_ID", nullable = false)
    private ContaFinanceiraEntity conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="TITULO_ID", nullable = true)
    private TituloEntity titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CATEGORIA_ID", nullable = true)
    private CategoriaFinanceiraEntity categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public MovimentoEntity() {
    }

    public MovimentoEntity(
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
        ContaFinanceiraEntity conta,
        TituloEntity titulo,
        CategoriaFinanceiraEntity categoria,
        EmpresaEntity empresa
    ) {
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

    public MovimentoEntity(MovimentoEntityBuilder builder) {
        this.version = builder.version;
        this.tipo = builder.tipo;
        this.sentido = builder.sentido;
        this.data = builder.data;
        this.descricao = builder.descricao;
        this.forma = builder.forma;
        this.valor = builder.valor;
        this.quitado = builder.quitado;
        this.juros = builder.juros;
        this.multa = builder.multa;
        this.desconto = builder.desconto;
        this.idLote = builder.idLote;
        this.idTransferencia = builder.idTransferencia;
        this.idEstornoDe = builder.idEstornoDe;
        this.idCriadoPor = builder.idCriadoPor;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.conta = builder.conta;
        this.titulo = builder.titulo;
        this.categoria = builder.categoria;
        this.empresa = builder.empresa;
    }

    public static MovimentoEntityBuilder builderOf() {
        return new MovimentoEntityBuilder();
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

    public void setConta(ContaFinanceiraEntity conta) {
        this.conta = conta;
    }

    public ContaFinanceiraEntity getConta() {
        return this.conta;
    }

    public void setTitulo(TituloEntity titulo) {
        this.titulo = titulo;
    }

    public TituloEntity getTitulo() {
        return this.titulo;
    }

    public void setCategoria(CategoriaFinanceiraEntity categoria) {
        this.categoria = categoria;
    }

    public CategoriaFinanceiraEntity getCategoria() {
        return this.categoria;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
