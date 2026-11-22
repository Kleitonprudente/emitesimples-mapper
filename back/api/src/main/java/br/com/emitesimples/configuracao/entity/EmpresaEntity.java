package br.com.emitesimples.configuracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.builder.EmpresaEntityBuilder;
import jakarta.persistence.FetchType;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;

/*
* Empresa usuária do sistema e tenant do modelo. Cada empresa só emite e só 
* enxerga as próprias notas.
 * NUNCA é apagada, só DESATIVADA (situacao = desativada): a aplicação não deve 
* expor a remoção. Nasce no signup com o documento, o e-mail e o WhatsApp 
* informados, em situacao pendente_confirmacao, e fica ativa quando o usuário 
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
@Entity
@Table(name = "EMPRESA")
public class EmpresaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME_FANTASIA", length=60)
    private String nomeFantasia;

    @Column(name="RAZAO_SOCIAL", length=60)
    private String razaoSocial;

    @Column(name="DOCUMENTO", length=14)
    private String documento;

    @Column(name="RG_INSCRICAO_ESTADUAL", length=20)
    private String rgInscricaoEstadual;

    @Column(name="INSCRICAO_MUNICIPAL", length=20)
    private String inscricaoMunicipal;

    @Column(name="EMAIL", length=60)
    private String email;

    @Column(name="WHATSAPP", length=20)
    private String whatsapp;

    @Column(name="TELEFONE", length=14)
    private String telefone;

    @Column(name="LOGRADOURO", length=60)
    private String logradouro;

    @Column(name="NUMERO_ENDERECO", length=60)
    private String numeroEndereco;

    @Column(name="COMPLEMENTO", length=60)
    private String complemento;

    @Column(name="BAIRRO", length=60)
    private String bairro;

    @Column(name="CEP", length=8)
    private String cep;

    @Column(name="REGIME_TRIBUTARIO", precision = 1)
    private Integer regimeTributario;

    @Column(name="REGIME_IBS_CBS", precision = 1)
    private Integer regimeIbsCbs;

    // SEM @Lob, e isso e deliberado: em PostgreSQL, @Lob byte[] mapeia para OID
    // (large object), nao para BYTEA. Large object tem ciclo de vida PROPRIO --
    // apagar a linha NAO apaga o objeto, quem apaga e lo_unlink --, entao toda
    // entidade que zere um campo binario vazaria: objeto orfao em pg_largeobject,
    // sem dono, sem referencia e sem ninguem para notar.
    // Comportamento confirmado por teste de integracao em Postgres real.
    // Em H2 o defeito e INVISIVEL (o schema de teste sai da propria anotacao).
    // Quem precisar de large object de verdade tem caso diferente e merece
    // declaracao explicita, nao default.
    // O `length` NAO e decorativo: sem ele, o H2 cria BINARY VARYING(255) e um
    // upload de 300 bytes ja estoura. Postgres ignora tamanho em BYTEA, entao a
    // anotacao so tem efeito onde o schema sai da anotacao -- que e o perfil de
    // teste. Sem isto, o defeito e invisivel em producao e constante no teste.
    // Tamanho: o parametro do mapa quando existe; sem ele, o padrao explicito e
    // length = 1048576 (1 MiB). O @Column sempre sai com length, como o comentario exige.
    @Column(name="LOGO", length = 1048576)
    private byte[] logo;

    @Column(name="URL_WEBHOOK", length=200)
    private String urlWebhook;

    @Column(name="SITUACAO", length=20)
    private String situacao;

    @Column(name="DATA_CONFIRMACAO")
    private LocalDateTime dataConfirmacao;

    @Column(name="REFERENCIA_CRM", length=50)
    private String referenciaCrm;

    @Column(name="NFSE_HABILITADA")
    private Boolean nfseHabilitada;

    @Column(name="SERIE_NFSE", length=5)
    private String serieNfse;

    @Column(name="ULTIMO_NUMERO_NFSE", precision = 9)
    private Integer ultimoNumeroNfse;

    @Column(name="STATUS_CADASTRO", length=20)
    private String statusCadastro;

    @Column(name="DATA_CADASTRO_GATEWAY")
    private LocalDateTime dataCadastroGateway;

    @Column(name="REFERENCIA_TOKEN_EMPRESA", length=100)
    private String referenciaTokenEmpresa;

    @Column(name="REFERENCIA_CREDENCIAIS_PREFEITURA", length=100)
    private String referenciaCredenciaisPrefeitura;

    @Column(name="REFERENCIA_CERTIFICADO", length=100)
    private String referenciaCertificado;

    @Column(name="VALIDADE_CERTIFICADO")
    private LocalDate validadeCertificado;

    @Column(name="SITUACAO_CERTIFICADO", length=20)
    private String situacaoCertificado;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="AMBIENTE", length=15)
    private String ambiente;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy="empresa", fetch = FetchType.LAZY)
    private List<AtividadeEmpresaEntity> atividades;

    @OneToMany(mappedBy="empresa", fetch = FetchType.LAZY)
    private List<QsaEmpresaEntity> qsa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="MUNICIPIO_ID", nullable = false)
    private MunicipioEntity municipio;

    public EmpresaEntity() {
    }

    public EmpresaEntity(
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
        String referenciaTokenEmpresa,
        String referenciaCredenciaisPrefeitura,
        String referenciaCertificado,
        LocalDate validadeCertificado,
        String situacaoCertificado,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        List<AtividadeEmpresaEntity> atividades,
        List<QsaEmpresaEntity> qsa,
        MunicipioEntity municipio
    ) {
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
        this.referenciaTokenEmpresa = referenciaTokenEmpresa;
        this.referenciaCredenciaisPrefeitura = referenciaCredenciaisPrefeitura;
        this.referenciaCertificado = referenciaCertificado;
        this.validadeCertificado = validadeCertificado;
        this.situacaoCertificado = situacaoCertificado;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.atividades = atividades;
        this.qsa = qsa;
        this.municipio = municipio;
    }

    public EmpresaEntity(EmpresaEntityBuilder builder) {
        this.version = builder.version;
        this.nomeFantasia = builder.nomeFantasia;
        this.razaoSocial = builder.razaoSocial;
        this.documento = builder.documento;
        this.rgInscricaoEstadual = builder.rgInscricaoEstadual;
        this.inscricaoMunicipal = builder.inscricaoMunicipal;
        this.email = builder.email;
        this.whatsapp = builder.whatsapp;
        this.telefone = builder.telefone;
        this.logradouro = builder.logradouro;
        this.numeroEndereco = builder.numeroEndereco;
        this.complemento = builder.complemento;
        this.bairro = builder.bairro;
        this.cep = builder.cep;
        this.regimeTributario = builder.regimeTributario;
        this.regimeIbsCbs = builder.regimeIbsCbs;
        this.logo = builder.logo;
        this.urlWebhook = builder.urlWebhook;
        this.situacao = builder.situacao;
        this.dataConfirmacao = builder.dataConfirmacao;
        this.referenciaCrm = builder.referenciaCrm;
        this.nfseHabilitada = builder.nfseHabilitada;
        this.serieNfse = builder.serieNfse;
        this.ultimoNumeroNfse = builder.ultimoNumeroNfse;
        this.statusCadastro = builder.statusCadastro;
        this.dataCadastroGateway = builder.dataCadastroGateway;
        this.referenciaTokenEmpresa = builder.referenciaTokenEmpresa;
        this.referenciaCredenciaisPrefeitura = builder.referenciaCredenciaisPrefeitura;
        this.referenciaCertificado = builder.referenciaCertificado;
        this.validadeCertificado = builder.validadeCertificado;
        this.situacaoCertificado = builder.situacaoCertificado;
        this.id = builder.id;
        this.ambiente = builder.ambiente;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.atividades = builder.atividades;
        this.qsa = builder.qsa;
        this.municipio = builder.municipio;
    }

    public static EmpresaEntityBuilder builderOf() {
        return new EmpresaEntityBuilder();
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

    public void setReferenciaTokenEmpresa(String referenciaTokenEmpresa) {
        this.referenciaTokenEmpresa = referenciaTokenEmpresa;
    }

    public String getReferenciaTokenEmpresa() {
        return this.referenciaTokenEmpresa;
    }

    public void setReferenciaCredenciaisPrefeitura(String referenciaCredenciaisPrefeitura) {
        this.referenciaCredenciaisPrefeitura = referenciaCredenciaisPrefeitura;
    }

    public String getReferenciaCredenciaisPrefeitura() {
        return this.referenciaCredenciaisPrefeitura;
    }

    public void setReferenciaCertificado(String referenciaCertificado) {
        this.referenciaCertificado = referenciaCertificado;
    }

    public String getReferenciaCertificado() {
        return this.referenciaCertificado;
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

    public void setAtividades(List<AtividadeEmpresaEntity> atividades) {
        this.atividades = atividades;
    }

    public List<AtividadeEmpresaEntity> getAtividades() {
        return this.atividades;
    }

    public void setQsa(List<QsaEmpresaEntity> qsa) {
        this.qsa = qsa;
    }

    public List<QsaEmpresaEntity> getQsa() {
        return this.qsa;
    }

    public void setMunicipio(MunicipioEntity municipio) {
        this.municipio = municipio;
    }

    public MunicipioEntity getMunicipio() {
        return this.municipio;
    }

}
