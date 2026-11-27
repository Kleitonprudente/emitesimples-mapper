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
public class ContaFinanceira {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 100, message = "Attribute nome must have 100 characters maximum.")
    private String nome;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 10, message = "Attribute tipo must have 10 characters maximum.")
    private String tipo;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @Size(max = 4, message = "Attribute codigoBanco must have 4 characters maximum.")
    private String codigoBanco;

    @Size(max = 10, message = "Attribute agencia must have 10 characters maximum.")
    private String agencia;

    @Size(max = 150, message = "Attribute dadosDoBanco must have 150 characters maximum.")
    private String dadosDoBanco;

    @NotNull(message = "The attribute saldoInicial can't be null.")
    private BigDecimal saldoInicial;

    @NotNull(message = "The attribute dataSaldoInicial can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataSaldoInicial;

    @NotNull(message = "The attribute padraoDinheiro can't be null.")
    private Boolean padraoDinheiro;

    @NotNull(message = "The attribute padraoPix can't be null.")
    private Boolean padraoPix;

    @NotNull(message = "The attribute padraoBoleto can't be null.")
    private Boolean padraoBoleto;

    @NotNull(message = "The attribute padraoCartao can't be null.")
    private Boolean padraoCartao;

    @NotNull(message = "The attribute padraoTransferencia can't be null.")
    private Boolean padraoTransferencia;

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

    public ContaFinanceira() {
        this.version = null;
        this.nome = null;
        this.tipo = null;
        this.ativo = null;
        this.codigoBanco = null;
        this.agencia = null;
        this.dadosDoBanco = null;
        this.saldoInicial = null;
        this.dataSaldoInicial = null;
        this.padraoDinheiro = null;
        this.padraoPix = null;
        this.padraoBoleto = null;
        this.padraoCartao = null;
        this.padraoTransferencia = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
    }
    public ContaFinanceira(
        LocalDateTime version,
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
        Empresa empresa
    ) {
        this.version = version;
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
            this.nome != null || 
            this.tipo != null || 
            this.ativo != null || 
            this.codigoBanco != null || 
            this.agencia != null || 
            this.dadosDoBanco != null || 
            this.saldoInicial != null || 
            this.dataSaldoInicial != null || 
            this.padraoDinheiro != null || 
            this.padraoPix != null || 
            this.padraoBoleto != null || 
            this.padraoCartao != null || 
            this.padraoTransferencia != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null;
    }

}
