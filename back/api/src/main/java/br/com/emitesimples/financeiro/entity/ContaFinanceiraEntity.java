package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.ContaFinanceira;
import br.com.emitesimples.financeiro.builder.ContaFinanceiraEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Onde o dinheiro da empresa está: o caixa da loja, uma conta no banco ou uma 
* carteira digital. Cada movimento do livro (Fase 3) acontece em uma conta.
 * O saldo NUNCA é guardado: é sempre calculado como saldo inicial mais a soma do 
* livro até a data.
 * Regras documentadas e não expressas no mapa (docs/financeiro/README.md, F1-H1), 
* implementadas na migração e em listener à mão:
 * o nome é único por empresa; o código do banco, quando informado, existe no 
* cadastro de Banco; a data do saldo inicial não é futura;
 * cada forma tem no máximo uma conta padrão e, ao marcar outra, a anterior é 
* desmarcada na mesma transação;
 * conta com movimentos tem saldo inicial e data travados (Fase 3). Nunca se apaga: 
* só se desativa. Escrita só do administrador da empresa.
 * Vive no contexto financeiro (domain.financeiro), que referencia a Empresa 
* (configuracao).

*/
@Entity
@Table(name = "CONTAFINANCEIRA")
public class ContaFinanceiraEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=100)
    private String nome;

    @Column(name="TIPO", length=10)
    private String tipo;

    @Column(name="ATIVO")
    private Boolean ativo;

    @Column(name="CODIGO_BANCO", length=4)
    private String codigoBanco;

    @Column(name="AGENCIA", length=10)
    private String agencia;

    @Column(name="DADOS_DO_BANCO", length=150)
    private String dadosDoBanco;

    @Column(name="SALDO_INICIAL", precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name="DATA_SALDO_INICIAL")
    private LocalDate dataSaldoInicial;

    @Column(name="PADRAO_DINHEIRO")
    private Boolean padraoDinheiro;

    @Column(name="PADRAO_PIX")
    private Boolean padraoPix;

    @Column(name="PADRAO_BOLETO")
    private Boolean padraoBoleto;

    @Column(name="PADRAO_CARTAO")
    private Boolean padraoCartao;

    @Column(name="PADRAO_TRANSFERENCIA")
    private Boolean padraoTransferencia;
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

    public ContaFinanceiraEntity() {
    }

    public ContaFinanceiraEntity(
        String nome,
        String tipo,
        Boolean ativo,
        String codigoBanco,
        String agencia,
        String dadosDoBanco,
        BigDecimal saldoInicial,
        LocalDate dataSaldoInicial,
        Boolean padraoDinheiro,
        Boolean padraoPix,
        Boolean padraoBoleto,
        Boolean padraoCartao,
        Boolean padraoTransferencia,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa
    ) {
        this.nome = nome;
        this.tipo = tipo;
        this.ativo = ativo;
        this.codigoBanco = codigoBanco;
        this.agencia = agencia;
        this.dadosDoBanco = dadosDoBanco;
        this.saldoInicial = saldoInicial;
        this.dataSaldoInicial = dataSaldoInicial;
        this.padraoDinheiro = padraoDinheiro;
        this.padraoPix = padraoPix;
        this.padraoBoleto = padraoBoleto;
        this.padraoCartao = padraoCartao;
        this.padraoTransferencia = padraoTransferencia;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }

    public ContaFinanceiraEntity(ContaFinanceiraEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.tipo = builder.tipo;
        this.ativo = builder.ativo;
        this.codigoBanco = builder.codigoBanco;
        this.agencia = builder.agencia;
        this.dadosDoBanco = builder.dadosDoBanco;
        this.saldoInicial = builder.saldoInicial;
        this.dataSaldoInicial = builder.dataSaldoInicial;
        this.padraoDinheiro = builder.padraoDinheiro;
        this.padraoPix = builder.padraoPix;
        this.padraoBoleto = builder.padraoBoleto;
        this.padraoCartao = builder.padraoCartao;
        this.padraoTransferencia = builder.padraoTransferencia;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
    }

    public static ContaFinanceiraEntityBuilder builderOf() {
        return new ContaFinanceiraEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setCodigoBanco(String codigoBanco) {
        this.codigoBanco = codigoBanco;
    }

    public String getCodigoBanco() {
        return this.codigoBanco;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public String getAgencia() {
        return this.agencia;
    }

    public void setDadosDoBanco(String dadosDoBanco) {
        this.dadosDoBanco = dadosDoBanco;
    }

    public String getDadosDoBanco() {
        return this.dadosDoBanco;
    }
    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }
    public BigDecimal getSaldoInicial() {
        return this.saldoInicial;
    }

    public void setDataSaldoInicial(LocalDate dataSaldoInicial) {
        this.dataSaldoInicial = dataSaldoInicial;
    }

    public LocalDate getDataSaldoInicial() {
        return this.dataSaldoInicial;
    }

    public void setPadraoDinheiro(Boolean padraoDinheiro) {
        this.padraoDinheiro = padraoDinheiro;
    }

    public Boolean getPadraoDinheiro() {
        return this.padraoDinheiro;
    }

    public void setPadraoPix(Boolean padraoPix) {
        this.padraoPix = padraoPix;
    }

    public Boolean getPadraoPix() {
        return this.padraoPix;
    }

    public void setPadraoBoleto(Boolean padraoBoleto) {
        this.padraoBoleto = padraoBoleto;
    }

    public Boolean getPadraoBoleto() {
        return this.padraoBoleto;
    }

    public void setPadraoCartao(Boolean padraoCartao) {
        this.padraoCartao = padraoCartao;
    }

    public Boolean getPadraoCartao() {
        return this.padraoCartao;
    }

    public void setPadraoTransferencia(Boolean padraoTransferencia) {
        this.padraoTransferencia = padraoTransferencia;
    }

    public Boolean getPadraoTransferencia() {
        return this.padraoTransferencia;
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

}
