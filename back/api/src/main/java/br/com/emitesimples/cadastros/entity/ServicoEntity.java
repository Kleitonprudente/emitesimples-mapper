package br.com.emitesimples.cadastros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Servico;
import br.com.emitesimples.cadastros.builder.ServicoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Cadastro de serviço prestado, próprio de cada empresa e reutilizável na emissão. 
* Guarda o enquadramento fiscal padrão
 * (código da LC 116, ISS e PIS/COFINS) e valores padrão que a nota pode 
* sobrescrever.
 * Vive no contexto cadastros (domain.cadastros), que referencia a Empresa 
* (configuracao) e tem dependência mútua com ele, pois a Empresa referencia o 
* Municipio, que está em cadastros.

*/
@Entity
@Table(name = "SERVICO")
public class ServicoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=100)
    private String nome;

    @Column(name="CODIGO_INTERNO", length=20)
    private String codigoInterno;

    @Column(name="ATIVO")
    private Boolean ativo;

    @Column(name="CODIGO_SERVICO", length=6)
    private String codigoServico;

    @Column(name="CODIGO_TRIBUTACAO_MUNICIPIO", length=3)
    private String codigoTributacaoMunicipio;

    @Column(name="CODIGO_NBS", length=9)
    private String codigoNbs;

    @Column(name="DESCRICAO_CURTA", length=200)
    private String descricaoCurta;

    @Column(name="DISCRIMINACAO_PADRAO", length=2000)
    private String discriminacaoPadrao;

    @Column(name="VALOR_PADRAO", precision = 15, scale = 2)
    private BigDecimal valorPadrao;

    @Column(name="ALIQUOTA_ISS", precision = 5, scale = 2)
    private BigDecimal aliquotaIss;

    @Column(name="ISS_RETIDO")
    private Boolean issRetido;

    @Column(name="TIPO_OPERACAO_ISS", precision = 1)
    private Integer tipoOperacaoIss;

    @Column(name="CST_PIS_COFINS", length=2)
    private String cstPisCofins;

    @Column(name="PERCENTUAL_TRIBUTOS_SIMPLES", precision = 5, scale = 2)
    private BigDecimal percentualTributosSimples;
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

    public ServicoEntity() {
    }

    public ServicoEntity(
        String nome,
        String codigoInterno,
        Boolean ativo,
        String codigoServico,
        String codigoTributacaoMunicipio,
        String codigoNbs,
        String descricaoCurta,
        String discriminacaoPadrao,
        BigDecimal valorPadrao,
        BigDecimal aliquotaIss,
        Boolean issRetido,
        Integer tipoOperacaoIss,
        String cstPisCofins,
        BigDecimal percentualTributosSimples,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa
    ) {
        this.nome = nome;
        this.codigoInterno = codigoInterno;
        this.ativo = ativo;
        this.codigoServico = codigoServico;
        this.codigoTributacaoMunicipio = codigoTributacaoMunicipio;
        this.codigoNbs = codigoNbs;
        this.descricaoCurta = descricaoCurta;
        this.discriminacaoPadrao = discriminacaoPadrao;
        this.valorPadrao = valorPadrao;
        this.aliquotaIss = aliquotaIss;
        this.issRetido = issRetido;
        this.tipoOperacaoIss = tipoOperacaoIss;
        this.cstPisCofins = cstPisCofins;
        this.percentualTributosSimples = percentualTributosSimples;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }

    public ServicoEntity(ServicoEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.codigoInterno = builder.codigoInterno;
        this.ativo = builder.ativo;
        this.codigoServico = builder.codigoServico;
        this.codigoTributacaoMunicipio = builder.codigoTributacaoMunicipio;
        this.codigoNbs = builder.codigoNbs;
        this.descricaoCurta = builder.descricaoCurta;
        this.discriminacaoPadrao = builder.discriminacaoPadrao;
        this.valorPadrao = builder.valorPadrao;
        this.aliquotaIss = builder.aliquotaIss;
        this.issRetido = builder.issRetido;
        this.tipoOperacaoIss = builder.tipoOperacaoIss;
        this.cstPisCofins = builder.cstPisCofins;
        this.percentualTributosSimples = builder.percentualTributosSimples;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
    }

    public static ServicoEntityBuilder builderOf() {
        return new ServicoEntityBuilder();
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

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getCodigoInterno() {
        return this.codigoInterno;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setCodigoServico(String codigoServico) {
        this.codigoServico = codigoServico;
    }

    public String getCodigoServico() {
        return this.codigoServico;
    }

    public void setCodigoTributacaoMunicipio(String codigoTributacaoMunicipio) {
        this.codigoTributacaoMunicipio = codigoTributacaoMunicipio;
    }

    public String getCodigoTributacaoMunicipio() {
        return this.codigoTributacaoMunicipio;
    }

    public void setCodigoNbs(String codigoNbs) {
        this.codigoNbs = codigoNbs;
    }

    public String getCodigoNbs() {
        return this.codigoNbs;
    }

    public void setDescricaoCurta(String descricaoCurta) {
        this.descricaoCurta = descricaoCurta;
    }

    public String getDescricaoCurta() {
        return this.descricaoCurta;
    }

    public void setDiscriminacaoPadrao(String discriminacaoPadrao) {
        this.discriminacaoPadrao = discriminacaoPadrao;
    }

    public String getDiscriminacaoPadrao() {
        return this.discriminacaoPadrao;
    }
    public void setValorPadrao(BigDecimal valorPadrao) {
        this.valorPadrao = valorPadrao;
    }
    public BigDecimal getValorPadrao() {
        return this.valorPadrao;
    }
    public void setAliquotaIss(BigDecimal aliquotaIss) {
        this.aliquotaIss = aliquotaIss;
    }
    public BigDecimal getAliquotaIss() {
        return this.aliquotaIss;
    }

    public void setIssRetido(Boolean issRetido) {
        this.issRetido = issRetido;
    }

    public Boolean getIssRetido() {
        return this.issRetido;
    }

    public void setTipoOperacaoIss(Integer tipoOperacaoIss) {
        this.tipoOperacaoIss = tipoOperacaoIss;
    }

    public Integer getTipoOperacaoIss() {
        return this.tipoOperacaoIss;
    }

    public void setCstPisCofins(String cstPisCofins) {
        this.cstPisCofins = cstPisCofins;
    }

    public String getCstPisCofins() {
        return this.cstPisCofins;
    }
    public void setPercentualTributosSimples(BigDecimal percentualTributosSimples) {
        this.percentualTributosSimples = percentualTributosSimples;
    }
    public BigDecimal getPercentualTributosSimples() {
        return this.percentualTributosSimples;
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
