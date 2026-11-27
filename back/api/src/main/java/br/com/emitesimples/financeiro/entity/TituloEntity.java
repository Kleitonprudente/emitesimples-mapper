package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Titulo;
import br.com.emitesimples.financeiro.builder.TituloEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

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
@Entity
@Table(name = "TITULO")
public class TituloEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TIPO", length=7)
    private String tipo;

    @Column(name="DESCRICAO", length=255)
    private String descricao;

    @Column(name="OBSERVACAO", length=255)
    private String observacao;

    @Column(name="ID_PESSOA", length=36)
    private String idPessoa;

    @Column(name="VALOR", precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name="QUITADO", precision = 15, scale = 2)
    private BigDecimal quitado;

    @Column(name="MULTA_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal multaPercentual;

    @Column(name="JUROS_MENSAL_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal jurosMensalPercentual;

    @Column(name="VENCIMENTO")
    private LocalDate vencimento;

    @Column(name="FORMA_PREVISTA", length=20)
    private String formaPrevista;

    @Column(name="PARCELA", precision = 2)
    private Integer parcela;

    @Column(name="TOTAL_PARCELAS", precision = 2)
    private Integer totalParcelas;

    @Column(name="ID_LANCAMENTO", length=36)
    private String idLancamento;

    @Column(name="ORIGEM_TIPO", length=10)
    private String origemTipo;

    @Column(name="ID_ORIGEM", length=36)
    private String idOrigem;

    @Column(name="ORIGEM_NUMERO", length=40)
    private String origemNumero;

    @Column(name="SITUACAO", length=10)
    private String situacao;

    @Column(name="PARA_REVISAO")
    private Boolean paraRevisao;

    @Column(name="CANCELADO_EM")
    private LocalDateTime canceladoEm;

    @Column(name="APROVADO_EM")
    private LocalDateTime aprovadoEm;

    @Column(name="ID_APROVADO_POR", length=36)
    private String idAprovadoPor;

    @Column(name="ID_CRIADO_POR", length=36)
    private String idCriadoPor;

    @Column(name="ID_CRIADO_POR_CLIENTE", length=36)
    private String idCriadoPorCliente;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CATEGORIA_ID", nullable = true)
    private CategoriaFinanceiraEntity categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public TituloEntity() {
    }

    public TituloEntity(
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
        CategoriaFinanceiraEntity categoria,
        EmpresaEntity empresa
    ) {
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

    public TituloEntity(TituloEntityBuilder builder) {
        this.version = builder.version;
        this.tipo = builder.tipo;
        this.descricao = builder.descricao;
        this.observacao = builder.observacao;
        this.idPessoa = builder.idPessoa;
        this.valor = builder.valor;
        this.quitado = builder.quitado;
        this.multaPercentual = builder.multaPercentual;
        this.jurosMensalPercentual = builder.jurosMensalPercentual;
        this.vencimento = builder.vencimento;
        this.formaPrevista = builder.formaPrevista;
        this.parcela = builder.parcela;
        this.totalParcelas = builder.totalParcelas;
        this.idLancamento = builder.idLancamento;
        this.origemTipo = builder.origemTipo;
        this.idOrigem = builder.idOrigem;
        this.origemNumero = builder.origemNumero;
        this.situacao = builder.situacao;
        this.paraRevisao = builder.paraRevisao;
        this.canceladoEm = builder.canceladoEm;
        this.aprovadoEm = builder.aprovadoEm;
        this.idAprovadoPor = builder.idAprovadoPor;
        this.idCriadoPor = builder.idCriadoPor;
        this.idCriadoPorCliente = builder.idCriadoPorCliente;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.categoria = builder.categoria;
        this.empresa = builder.empresa;
    }

    public static TituloEntityBuilder builderOf() {
        return new TituloEntityBuilder();
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
