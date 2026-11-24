package br.com.emitesimples.configuracao.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.cadastros.domain.Municipio;

/*
* Empresa usuária do sistema e tenant do modelo. Cada empresa só emite e só 
* enxerga as próprias notas.
 * NUNCA é apagada, só DESATIVADA (situacao = DESATIVADA): a aplicação não deve 
* expor a remoção. Nasce no signup com o documento, o e-mail e o WhatsApp 
* informados, em situacao PENDENTE_CONFIRMACAO, e fica ATIVA quando o usuário 
* clica no link do e-mail (ConfirmacaoCadastro).
 * É identificada por um documento único (CNPJ ou CPF). Tem atividades 
* (AtividadeEmpresa) e quadro de sócios (QsaEmpresa) como detalhes ([m]/[o]).
 * Regra de edição, da aplicação: o usuário da empresa só altera razaoSocial, 
* nomeFantasia e o endereço; o resto é alterável só pelo administrador da 
* plataforma ou suporte. É cadastrada no gateway IntegraNotas, onde se chama 
* "emitente", e o gateway devolve o token do emitente,
 * separado por ambiente. Token, credenciais da prefeitura e certificado ficam no 
* cofre, aqui só há referência.
 * Vive no contexto configuracao (domain.configuracao), sozinha. Há dependência 
* mútua com o contexto cadastros: a Empresa referencia o Municipio (cadastros), e 
* Pessoa e Servico (cadastros) referenciam a Empresa. Empresa e Municipio devem 
* ser modelados juntos.

*/
public class Empresa {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nomeFantasia can't be empty.")
    @Size(max = 60, message = "Attribute nomeFantasia must have 60 characters maximum.")
    private String nomeFantasia;

    @NotEmpty(message = "The attribute razaoSocial can't be empty.")
    @Size(max = 60, message = "Attribute razaoSocial must have 60 characters maximum.")
    private String razaoSocial;

    @NotEmpty(message = "The attribute documento can't be empty.")
    @Size(max = 14, message = "Attribute documento must have 14 characters maximum.")
    private String documento;

    @Size(max = 20, message = "Attribute rgInscricaoEstadual must have 20 characters maximum.")
    private String rgInscricaoEstadual;

    @Size(max = 20, message = "Attribute inscricaoMunicipal must have 20 characters maximum.")
    private String inscricaoMunicipal;

    @NotEmpty(message = "The attribute email can't be empty.")
    @Size(max = 60, message = "Attribute email must have 60 characters maximum.")
    private String email;

    @NotEmpty(message = "The attribute whatsapp can't be empty.")
    @Size(max = 20, message = "Attribute whatsapp must have 20 characters maximum.")
    private String whatsapp;

    @NotEmpty(message = "The attribute telefone can't be empty.")
    @Size(max = 14, message = "Attribute telefone must have 14 characters maximum.")
    private String telefone;

    @NotEmpty(message = "The attribute logradouro can't be empty.")
    @Size(max = 60, message = "Attribute logradouro must have 60 characters maximum.")
    private String logradouro;

    @NotEmpty(message = "The attribute numeroEndereco can't be empty.")
    @Size(max = 60, message = "Attribute numeroEndereco must have 60 characters maximum.")
    private String numeroEndereco;

    @Size(max = 60, message = "Attribute complemento must have 60 characters maximum.")
    private String complemento;

    @NotEmpty(message = "The attribute bairro can't be empty.")
    @Size(max = 60, message = "Attribute bairro must have 60 characters maximum.")
    private String bairro;

    @NotEmpty(message = "The attribute cep can't be empty.")
    @Size(max = 8, message = "Attribute cep must have 8 characters maximum.")
    private String cep;

    @NotNull(message = "The attribute regimeTributario can't be null.")
    private Integer regimeTributario;

    private Integer regimeIbsCbs;

    private byte[] logo;

    @Size(max = 200, message = "Attribute urlWebhook must have 200 characters maximum.")
    private String urlWebhook;

    @NotEmpty(message = "The attribute situacao can't be empty.")
    @Size(max = 20, message = "Attribute situacao must have 20 characters maximum.")
    private String situacao;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataConfirmacao;

    @Size(max = 50, message = "Attribute referenciaCrm must have 50 characters maximum.")
    private String referenciaCrm;

    @NotNull(message = "The attribute nfseHabilitada can't be null.")
    private Boolean nfseHabilitada;

    @Size(max = 5, message = "Attribute serieNfse must have 5 characters maximum.")
    private String serieNfse;

    @NotNull(message = "The attribute ultimoNumeroNfse can't be null.")
    private Integer ultimoNumeroNfse;

    @NotEmpty(message = "The attribute statusCadastro can't be empty.")
    @Size(max = 20, message = "Attribute statusCadastro must have 20 characters maximum.")
    private String statusCadastro;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCadastroGateway;

    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate validadeCertificado;

    @NotEmpty(message = "The attribute situacaoCertificado can't be empty.")
    @Size(max = 20, message = "Attribute situacaoCertificado must have 20 characters maximum.")
    private String situacaoCertificado;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotEmpty(message = "The attribute ambiente can't be empty.")
    @Size(max = 15, message = "Attribute ambiente must have 15 characters maximum.")
    private String ambiente;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute municipio can't be null.")
    private Municipio municipio;

    public Empresa() {
        this.version = null;
        this.nomeFantasia = null;
        this.razaoSocial = null;
        this.documento = null;
        this.rgInscricaoEstadual = null;
        this.inscricaoMunicipal = null;
        this.email = null;
        this.whatsapp = null;
        this.telefone = null;
        this.logradouro = null;
        this.numeroEndereco = null;
        this.complemento = null;
        this.bairro = null;
        this.cep = null;
        this.regimeTributario = null;
        this.regimeIbsCbs = null;
        this.logo = null;
        this.urlWebhook = null;
        this.situacao = null;
        this.dataConfirmacao = null;
        this.referenciaCrm = null;
        this.nfseHabilitada = null;
        this.serieNfse = null;
        this.ultimoNumeroNfse = null;
        this.statusCadastro = null;
        this.dataCadastroGateway = null;
        this.validadeCertificado = null;
        this.situacaoCertificado = null;
        this.id = null;
        this.ambiente = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.municipio = null;
    }
    public Empresa(
        LocalDateTime version,
        String nomeFantasia,
        String razaoSocial,
        String documento,
        String rgInscricaoEstadual,
        String inscricaoMunicipal,
        String email,
        String whatsapp,
        String telefone,
        String logradouro,
        String numeroEndereco,
        String complemento,
        String bairro,
        String cep,
        Integer regimeTributario,
        Integer regimeIbsCbs,
        byte[] logo,
        String urlWebhook,
        String situacao,
        LocalDateTime dataConfirmacao,
        String referenciaCrm,
        Boolean nfseHabilitada,
        String serieNfse,
        Integer ultimoNumeroNfse,
        String statusCadastro,
        LocalDateTime dataCadastroGateway,
        LocalDate validadeCertificado,
        String situacaoCertificado,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Municipio municipio
    ) {
        this.version = version;
        this.nomeFantasia = nomeFantasia;
        this.razaoSocial = razaoSocial;
        this.documento = documento;
        this.rgInscricaoEstadual = rgInscricaoEstadual;
        this.inscricaoMunicipal = inscricaoMunicipal;
        this.email = email;
        this.whatsapp = whatsapp;
        this.telefone = telefone;
        this.logradouro = logradouro;
        this.numeroEndereco = numeroEndereco;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cep = cep;
        this.regimeTributario = regimeTributario;
        this.regimeIbsCbs = regimeIbsCbs;
        this.logo = logo;
        this.urlWebhook = urlWebhook;
        this.situacao = situacao;
        this.dataConfirmacao = dataConfirmacao;
        this.referenciaCrm = referenciaCrm;
        this.nfseHabilitada = nfseHabilitada;
        this.serieNfse = serieNfse;
        this.ultimoNumeroNfse = ultimoNumeroNfse;
        this.statusCadastro = statusCadastro;
        this.dataCadastroGateway = dataCadastroGateway;
        this.validadeCertificado = validadeCertificado;
        this.situacaoCertificado = situacaoCertificado;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.municipio = municipio;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getNomeFantasia() {
        return this.nomeFantasia;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getRazaoSocial() {
        return this.razaoSocial;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getDocumento() {
        return this.documento;
    }

    public void setRgInscricaoEstadual(String rgInscricaoEstadual) {
        this.rgInscricaoEstadual = rgInscricaoEstadual;
    }

    public String getRgInscricaoEstadual() {
        return this.rgInscricaoEstadual;
    }

    public void setInscricaoMunicipal(String inscricaoMunicipal) {
        this.inscricaoMunicipal = inscricaoMunicipal;
    }

    public String getInscricaoMunicipal() {
        return this.inscricaoMunicipal;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getWhatsapp() {
        return this.whatsapp;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getLogradouro() {
        return this.logradouro;
    }

    public void setNumeroEndereco(String numeroEndereco) {
        this.numeroEndereco = numeroEndereco;
    }

    public String getNumeroEndereco() {
        return this.numeroEndereco;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getComplemento() {
        return this.complemento;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getBairro() {
        return this.bairro;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getCep() {
        return this.cep;
    }

    public void setRegimeTributario(Integer regimeTributario) {
        this.regimeTributario = regimeTributario;
    }

    public Integer getRegimeTributario() {
        return this.regimeTributario;
    }

    public void setRegimeIbsCbs(Integer regimeIbsCbs) {
        this.regimeIbsCbs = regimeIbsCbs;
    }

    public Integer getRegimeIbsCbs() {
        return this.regimeIbsCbs;
    }

    public void setLogo(byte[] logo) {
        this.logo = logo;
    }

    public byte[] getLogo() {
        return this.logo;
    }

    public void setUrlWebhook(String urlWebhook) {
        this.urlWebhook = urlWebhook;
    }

    public String getUrlWebhook() {
        return this.urlWebhook;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getSituacao() {
        return this.situacao;
    }

    public void setDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
    }

    public LocalDateTime getDataConfirmacao() {
        return this.dataConfirmacao;
    }

    public void setReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
    }

    public String getReferenciaCrm() {
        return this.referenciaCrm;
    }

    public void setNfseHabilitada(Boolean nfseHabilitada) {
        this.nfseHabilitada = nfseHabilitada;
    }

    public Boolean getNfseHabilitada() {
        return this.nfseHabilitada;
    }

    public void setSerieNfse(String serieNfse) {
        this.serieNfse = serieNfse;
    }

    public String getSerieNfse() {
        return this.serieNfse;
    }

    public void setUltimoNumeroNfse(Integer ultimoNumeroNfse) {
        this.ultimoNumeroNfse = ultimoNumeroNfse;
    }

    public Integer getUltimoNumeroNfse() {
        return this.ultimoNumeroNfse;
    }

    public void setStatusCadastro(String statusCadastro) {
        this.statusCadastro = statusCadastro;
    }

    public String getStatusCadastro() {
        return this.statusCadastro;
    }

    public void setDataCadastroGateway(LocalDateTime dataCadastroGateway) {
        this.dataCadastroGateway = dataCadastroGateway;
    }

    public LocalDateTime getDataCadastroGateway() {
        return this.dataCadastroGateway;
    }

    public void setValidadeCertificado(LocalDate validadeCertificado) {
        this.validadeCertificado = validadeCertificado;
    }

    public LocalDate getValidadeCertificado() {
        return this.validadeCertificado;
    }

    public void setSituacaoCertificado(String situacaoCertificado) {
        this.situacaoCertificado = situacaoCertificado;
    }

    public String getSituacaoCertificado() {
        return this.situacaoCertificado;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getAmbiente() {
        return this.ambiente;
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

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public Municipio getMunicipio() {
        return this.municipio;
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
            this.nomeFantasia != null || 
            this.razaoSocial != null || 
            this.documento != null || 
            this.rgInscricaoEstadual != null || 
            this.inscricaoMunicipal != null || 
            this.email != null || 
            this.whatsapp != null || 
            this.telefone != null || 
            this.logradouro != null || 
            this.numeroEndereco != null || 
            this.complemento != null || 
            this.bairro != null || 
            this.cep != null || 
            this.regimeTributario != null || 
            this.regimeIbsCbs != null || 
            this.logo != null || 
            this.urlWebhook != null || 
            this.situacao != null || 
            this.dataConfirmacao != null || 
            this.referenciaCrm != null || 
            this.nfseHabilitada != null || 
            this.serieNfse != null || 
            this.ultimoNumeroNfse != null || 
            this.statusCadastro != null || 
            this.dataCadastroGateway != null || 
            this.validadeCertificado != null || 
            this.situacaoCertificado != null || 
            this.ambiente != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.municipio != null;
    }

}
