package br.com.emitesimples.cadastros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.builder.PessoaEntityBuilder;
import jakarta.persistence.FetchType;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Cadastro de cliente, física ou jurídica, reutilizável nas emissões, que na nota 
* assume o papel de tomador do serviço. É por empresa: o mesmo documento pode 
* existir
 * uma vez em cada empresa, e uma empresa nunca enxerga a pessoa de outra.
 * Tem um só documento (CNPJ ou CPF, um ou outro). Ao enviar a NFS-e, o sistema 
* separa o documento em cnpj ou cpf, que no IntegraNotas são campos distintos.
 * A cidade é uma referência ao Municipio, que já traz a UF e o código IBGE: a tela 
* mostra cidade e UF e esconde o IBGE.
 * Os contatos são uma lista (Contato), com vários por pessoa. Na exportação o 
* endereço não tem município brasileiro e usa os campos de exterior.
 * Vive no contexto cadastros (domain.cadastros), que referencia a Empresa 
* (configuracao) e tem dependência mútua com ele, pois a Empresa referencia o 
* Municipio, que está em cadastros.

*/
@Entity
@Table(name = "PESSOA")
public class PessoaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TIPO_PESSOA", length=10)
    private String tipoPessoa;

    @Column(name="DOCUMENTO", length=14)
    private String documento;

    @Column(name="NOME", length=300)
    private String nome;

    @Column(name="NOME_FANTASIA", length=300)
    private String nomeFantasia;

    @Column(name="INSCRICAO_MUNICIPAL", length=15)
    private String inscricaoMunicipal;

    @Column(name="RG_INSCRICAO_ESTADUAL", length=20)
    private String rgInscricaoEstadual;

    @Column(name="TIPO_DESTINATARIO", precision = 1)
    private Integer tipoDestinatario;

    @Column(name="CONSUMIDOR_FINAL")
    private Boolean consumidorFinal;

    @Column(name="TELEFONE", length=20)
    private String telefone;

    @Column(name="EMAIL", length=60)
    private String email;

    @Column(name="LOGRADOURO", length=255)
    private String logradouro;

    @Column(name="NUMERO_ENDERECO", length=60)
    private String numeroEndereco;

    @Column(name="COMPLEMENTO", length=256)
    private String complemento;

    @Column(name="BAIRRO", length=60)
    private String bairro;

    @Column(name="CEP", length=8)
    private String cep;

    @Column(name="CODIGO_PAIS", length=4)
    private String codigoPais;

    @Column(name="CODIGO_POSTAL", length=11)
    private String codigoPostal;

    @Column(name="NOME_MUNICIPIO_EXTERIOR", length=60)
    private String nomeMunicipioExterior;

    @Column(name="NOME_PROVINCIA", length=60)
    private String nomeProvincia;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CIDADE_ID", nullable = true)
    private MunicipioEntity cidade;

    @OneToMany(mappedBy="pessoa", fetch = FetchType.LAZY)
    private List<ContatoEntity> contatos;

    public PessoaEntity() {
    }

    public PessoaEntity(
        String tipoPessoa,
        String documento,
        String nome,
        String nomeFantasia,
        String inscricaoMunicipal,
        String rgInscricaoEstadual,
        Integer tipoDestinatario,
        Boolean consumidorFinal,
        String telefone,
        String email,
        String logradouro,
        String numeroEndereco,
        String complemento,
        String bairro,
        String cep,
        String codigoPais,
        String codigoPostal,
        String nomeMunicipioExterior,
        String nomeProvincia,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        MunicipioEntity cidade,
        List<ContatoEntity> contatos
    ) {
        this.tipoPessoa = tipoPessoa;
        this.documento = documento;
        this.nome = nome;
        this.nomeFantasia = nomeFantasia;
        this.inscricaoMunicipal = inscricaoMunicipal;
        this.rgInscricaoEstadual = rgInscricaoEstadual;
        this.tipoDestinatario = tipoDestinatario;
        this.consumidorFinal = consumidorFinal;
        this.telefone = telefone;
        this.email = email;
        this.logradouro = logradouro;
        this.numeroEndereco = numeroEndereco;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cep = cep;
        this.codigoPais = codigoPais;
        this.codigoPostal = codigoPostal;
        this.nomeMunicipioExterior = nomeMunicipioExterior;
        this.nomeProvincia = nomeProvincia;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.cidade = cidade;
        this.contatos = contatos;
    }

    public PessoaEntity(PessoaEntityBuilder builder) {
        this.version = builder.version;
        this.tipoPessoa = builder.tipoPessoa;
        this.documento = builder.documento;
        this.nome = builder.nome;
        this.nomeFantasia = builder.nomeFantasia;
        this.inscricaoMunicipal = builder.inscricaoMunicipal;
        this.rgInscricaoEstadual = builder.rgInscricaoEstadual;
        this.tipoDestinatario = builder.tipoDestinatario;
        this.consumidorFinal = builder.consumidorFinal;
        this.telefone = builder.telefone;
        this.email = builder.email;
        this.logradouro = builder.logradouro;
        this.numeroEndereco = builder.numeroEndereco;
        this.complemento = builder.complemento;
        this.bairro = builder.bairro;
        this.cep = builder.cep;
        this.codigoPais = builder.codigoPais;
        this.codigoPostal = builder.codigoPostal;
        this.nomeMunicipioExterior = builder.nomeMunicipioExterior;
        this.nomeProvincia = builder.nomeProvincia;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.cidade = builder.cidade;
        this.contatos = builder.contatos;
    }

    public static PessoaEntityBuilder builderOf() {
        return new PessoaEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTipoPessoa(String tipoPessoa) {
        this.tipoPessoa = tipoPessoa;
    }

    public String getTipoPessoa() {
        return this.tipoPessoa;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getDocumento() {
        return this.documento;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getNomeFantasia() {
        return this.nomeFantasia;
    }

    public void setInscricaoMunicipal(String inscricaoMunicipal) {
        this.inscricaoMunicipal = inscricaoMunicipal;
    }

    public String getInscricaoMunicipal() {
        return this.inscricaoMunicipal;
    }

    public void setRgInscricaoEstadual(String rgInscricaoEstadual) {
        this.rgInscricaoEstadual = rgInscricaoEstadual;
    }

    public String getRgInscricaoEstadual() {
        return this.rgInscricaoEstadual;
    }

    public void setTipoDestinatario(Integer tipoDestinatario) {
        this.tipoDestinatario = tipoDestinatario;
    }

    public Integer getTipoDestinatario() {
        return this.tipoDestinatario;
    }

    public void setConsumidorFinal(Boolean consumidorFinal) {
        this.consumidorFinal = consumidorFinal;
    }

    public Boolean getConsumidorFinal() {
        return this.consumidorFinal;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
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

    public void setCodigoPais(String codigoPais) {
        this.codigoPais = codigoPais;
    }

    public String getCodigoPais() {
        return this.codigoPais;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getCodigoPostal() {
        return this.codigoPostal;
    }

    public void setNomeMunicipioExterior(String nomeMunicipioExterior) {
        this.nomeMunicipioExterior = nomeMunicipioExterior;
    }

    public String getNomeMunicipioExterior() {
        return this.nomeMunicipioExterior;
    }

    public void setNomeProvincia(String nomeProvincia) {
        this.nomeProvincia = nomeProvincia;
    }

    public String getNomeProvincia() {
        return this.nomeProvincia;
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

    public void setCidade(MunicipioEntity cidade) {
        this.cidade = cidade;
    }

    public MunicipioEntity getCidade() {
        return this.cidade;
    }

    public void setContatos(List<ContatoEntity> contatos) {
        this.contatos = contatos;
    }

    public List<ContatoEntity> getContatos() {
        return this.contatos;
    }

}
