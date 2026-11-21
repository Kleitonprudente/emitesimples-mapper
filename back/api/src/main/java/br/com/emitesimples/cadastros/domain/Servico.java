package br.com.emitesimples.cadastros.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Cadastro de serviço prestado, próprio de cada empresa e reutilizável na emissão. 
* Guarda o enquadramento fiscal padrão
 * (código da LC 116, ISS e PIS/COFINS) e valores padrão que a nota pode 
* sobrescrever.
 * Vive no contexto cadastros (domain.cadastros), que referencia a Empresa 
* (configuracao) e tem dependência mútua com ele, pois a Empresa referencia o 
* Municipio, que está em cadastros.

*/
public class Servico {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 100, message = "Attribute nome must have 100 characters maximum.")
    private String nome;

    @Size(max = 20, message = "Attribute codigoInterno must have 20 characters maximum.")
    private String codigoInterno;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @NotEmpty(message = "The attribute codigoServico can't be empty.")
    @Size(max = 6, message = "Attribute codigoServico must have 6 characters maximum.")
    private String codigoServico;

    @Size(max = 3, message = "Attribute codigoTributacaoMunicipio must have 3 characters maximum.")
    private String codigoTributacaoMunicipio;

    @Size(max = 9, message = "Attribute codigoNbs must have 9 characters maximum.")
    private String codigoNbs;

    @Size(max = 200, message = "Attribute descricaoCurta must have 200 characters maximum.")
    private String descricaoCurta;

    @Size(max = 2000, message = "Attribute discriminacaoPadrao must have 2000 characters maximum.")
    private String discriminacaoPadrao;

    private BigDecimal valorPadrao;

    private BigDecimal aliquotaIss;

    @NotNull(message = "The attribute issRetido can't be null.")
    private Boolean issRetido;

    @NotNull(message = "The attribute tipoOperacaoIss can't be null.")
    private Integer tipoOperacaoIss;

    @Size(max = 2, message = "Attribute cstPisCofins must have 2 characters maximum.")
    private String cstPisCofins;

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

    public Servico() {
        this.version = null;
        this.nome = null;
        this.codigoInterno = null;
        this.ativo = null;
        this.codigoServico = null;
        this.codigoTributacaoMunicipio = null;
        this.codigoNbs = null;
        this.descricaoCurta = null;
        this.discriminacaoPadrao = null;
        this.valorPadrao = null;
        this.aliquotaIss = null;
        this.issRetido = null;
        this.tipoOperacaoIss = null;
        this.cstPisCofins = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
    }
    public Servico(
        LocalDateTime version,
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
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa
    ) {
        this.version = version;
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
            this.codigoInterno != null || 
            this.ativo != null || 
            this.codigoServico != null || 
            this.codigoTributacaoMunicipio != null || 
            this.codigoNbs != null || 
            this.descricaoCurta != null || 
            this.discriminacaoPadrao != null || 
            this.valorPadrao != null || 
            this.aliquotaIss != null || 
            this.issRetido != null || 
            this.tipoOperacaoIss != null || 
            this.cstPisCofins != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null;
    }

}
