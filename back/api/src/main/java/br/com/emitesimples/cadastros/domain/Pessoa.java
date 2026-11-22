package br.com.emitesimples.cadastros.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Cadastro de pessoa, física ou jurídica, que pode ser cliente, fornecedor ou 
* ambos (atributos cliente e fornecedor), reutilizável nas emissões, que na nota 
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
public class Pessoa {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute tipoPessoa can't be empty.")
    @Size(max = 10, message = "Attribute tipoPessoa must have 10 characters maximum.")
    private String tipoPessoa;

    @Size(max = 14, message = "Attribute documento must have 14 characters maximum.")
    private String documento;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 300, message = "Attribute nome must have 300 characters maximum.")
    private String nome;

    @Size(max = 300, message = "Attribute nomeFantasia must have 300 characters maximum.")
    private String nomeFantasia;

    @Size(max = 15, message = "Attribute inscricaoMunicipal must have 15 characters maximum.")
    private String inscricaoMunicipal;

    @Size(max = 20, message = "Attribute rgInscricaoEstadual must have 20 characters maximum.")
    private String rgInscricaoEstadual;

    private Integer tipoDestinatario;

    private Boolean consumidorFinal;

    @NotNull(message = "The attribute cliente can't be null.")
    private Boolean cliente;

    @NotNull(message = "The attribute fornecedor can't be null.")
    private Boolean fornecedor;

    @Size(max = 20, message = "Attribute telefone must have 20 characters maximum.")
    private String telefone;

    @Size(max = 60, message = "Attribute email must have 60 characters maximum.")
    private String email;

    @NotEmpty(message = "The attribute logradouro can't be empty.")
    @Size(max = 255, message = "Attribute logradouro must have 255 characters maximum.")
    private String logradouro;

    @NotEmpty(message = "The attribute numeroEndereco can't be empty.")
    @Size(max = 60, message = "Attribute numeroEndereco must have 60 characters maximum.")
    private String numeroEndereco;

    @Size(max = 256, message = "Attribute complemento must have 256 characters maximum.")
    private String complemento;

    @NotEmpty(message = "The attribute bairro can't be empty.")
    @Size(max = 60, message = "Attribute bairro must have 60 characters maximum.")
    private String bairro;

    @Size(max = 8, message = "Attribute cep must have 8 characters maximum.")
    private String cep;

    @Size(max = 4, message = "Attribute codigoPais must have 4 characters maximum.")
    private String codigoPais;

    @Size(max = 11, message = "Attribute codigoPostal must have 11 characters maximum.")
    private String codigoPostal;

    @Size(max = 60, message = "Attribute nomeMunicipioExterior must have 60 characters maximum.")
    private String nomeMunicipioExterior;

    @Size(max = 60, message = "Attribute nomeProvincia must have 60 characters maximum.")
    private String nomeProvincia;

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

    private Municipio cidade;

    public Pessoa() {
        this.version = null;
        this.tipoPessoa = null;
        this.documento = null;
        this.nome = null;
        this.nomeFantasia = null;
        this.inscricaoMunicipal = null;
        this.rgInscricaoEstadual = null;
        this.tipoDestinatario = null;
        this.consumidorFinal = null;
        this.cliente = null;
        this.fornecedor = null;
        this.telefone = null;
        this.email = null;
        this.logradouro = null;
        this.numeroEndereco = null;
        this.complemento = null;
        this.bairro = null;
        this.cep = null;
        this.codigoPais = null;
        this.codigoPostal = null;
        this.nomeMunicipioExterior = null;
        this.nomeProvincia = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.cidade = null;
    }
    public Pessoa(
        LocalDateTime version,
        String tipoPessoa,
        String documento,
        String nome,
        String nomeFantasia,
        String inscricaoMunicipal,
        String rgInscricaoEstadual,
        Integer tipoDestinatario,
        Boolean consumidorFinal,
        Boolean cliente,
        Boolean fornecedor,
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
        Empresa empresa,
        Municipio cidade
    ) {
        this.version = version;
        this.tipoPessoa = tipoPessoa;
        this.documento = documento;
        this.nome = nome;
        this.nomeFantasia = nomeFantasia;
        this.inscricaoMunicipal = inscricaoMunicipal;
        this.rgInscricaoEstadual = rgInscricaoEstadual;
        this.tipoDestinatario = tipoDestinatario;
        this.consumidorFinal = consumidorFinal;
        this.cliente = cliente;
        this.fornecedor = fornecedor;
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

    public void setCliente(Boolean cliente) {
        this.cliente = cliente;
    }

    public Boolean getCliente() {
        return this.cliente;
    }

    public void setFornecedor(Boolean fornecedor) {
        this.fornecedor = fornecedor;
    }

    public Boolean getFornecedor() {
        return this.fornecedor;
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

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empresa getEmpresa() {
        return this.empresa;
    }

    public void setCidade(Municipio cidade) {
        this.cidade = cidade;
    }

    public Municipio getCidade() {
        return this.cidade;
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
            this.tipoPessoa != null || 
            this.documento != null || 
            this.nome != null || 
            this.nomeFantasia != null || 
            this.inscricaoMunicipal != null || 
            this.rgInscricaoEstadual != null || 
            this.tipoDestinatario != null || 
            this.consumidorFinal != null || 
            this.cliente != null || 
            this.fornecedor != null || 
            this.telefone != null || 
            this.email != null || 
            this.logradouro != null || 
            this.numeroEndereco != null || 
            this.complemento != null || 
            this.bairro != null || 
            this.cep != null || 
            this.codigoPais != null || 
            this.codigoPostal != null || 
            this.nomeMunicipioExterior != null || 
            this.nomeProvincia != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.cidade != null;
    }

}
