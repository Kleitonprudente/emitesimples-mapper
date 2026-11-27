package br.com.emitesimples.cadastros.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.entity.ContatoEntity;

/*
 * Pessoa entity builder
*/
public class PessoaEntityBuilder {
    public LocalDateTime version;
    public String tipoPessoa;
    public String documento;
    public String nome;
    public String nomeFantasia;
    public String inscricaoMunicipal;
    public String rgInscricaoEstadual;
    public Integer tipoDestinatario;
    public Boolean consumidorFinal;
    public Boolean cliente;
    public Boolean fornecedor;
    public Boolean naoEnviarCobranca;
    public String telefone;
    public String email;
    public String logradouro;
    public String numeroEndereco;
    public String complemento;
    public String bairro;
    public String cep;
    public String codigoPais;
    public String codigoPostal;
    public String nomeMunicipioExterior;
    public String nomeProvincia;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public MunicipioEntity cidade;
    public List<ContatoEntity> contatos;

    public PessoaEntityBuilder() {
    }
    public PessoaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public PessoaEntityBuilder withTipoPessoa(String tipoPessoa) {
        this.tipoPessoa = tipoPessoa;
        return this;
    }

    public PessoaEntityBuilder withDocumento(String documento) {
        this.documento = documento;
        return this;
    }

    public PessoaEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public PessoaEntityBuilder withNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
        return this;
    }

    public PessoaEntityBuilder withInscricaoMunicipal(String inscricaoMunicipal) {
        this.inscricaoMunicipal = inscricaoMunicipal;
        return this;
    }

    public PessoaEntityBuilder withRgInscricaoEstadual(String rgInscricaoEstadual) {
        this.rgInscricaoEstadual = rgInscricaoEstadual;
        return this;
    }

    public PessoaEntityBuilder withTipoDestinatario(Integer tipoDestinatario) {
        this.tipoDestinatario = tipoDestinatario;
        return this;
    }

    public PessoaEntityBuilder withConsumidorFinal(Boolean consumidorFinal) {
        this.consumidorFinal = consumidorFinal;
        return this;
    }

    public PessoaEntityBuilder withCliente(Boolean cliente) {
        this.cliente = cliente;
        return this;
    }

    public PessoaEntityBuilder withFornecedor(Boolean fornecedor) {
        this.fornecedor = fornecedor;
        return this;
    }

    public PessoaEntityBuilder withNaoEnviarCobranca(Boolean naoEnviarCobranca) {
        this.naoEnviarCobranca = naoEnviarCobranca;
        return this;
    }

    public PessoaEntityBuilder withTelefone(String telefone) {
        this.telefone = telefone;
        return this;
    }

    public PessoaEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public PessoaEntityBuilder withLogradouro(String logradouro) {
        this.logradouro = logradouro;
        return this;
    }

    public PessoaEntityBuilder withNumeroEndereco(String numeroEndereco) {
        this.numeroEndereco = numeroEndereco;
        return this;
    }

    public PessoaEntityBuilder withComplemento(String complemento) {
        this.complemento = complemento;
        return this;
    }

    public PessoaEntityBuilder withBairro(String bairro) {
        this.bairro = bairro;
        return this;
    }

    public PessoaEntityBuilder withCep(String cep) {
        this.cep = cep;
        return this;
    }

    public PessoaEntityBuilder withCodigoPais(String codigoPais) {
        this.codigoPais = codigoPais;
        return this;
    }

    public PessoaEntityBuilder withCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
        return this;
    }

    public PessoaEntityBuilder withNomeMunicipioExterior(String nomeMunicipioExterior) {
        this.nomeMunicipioExterior = nomeMunicipioExterior;
        return this;
    }

    public PessoaEntityBuilder withNomeProvincia(String nomeProvincia) {
        this.nomeProvincia = nomeProvincia;
        return this;
    }

    public PessoaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public PessoaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public PessoaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public PessoaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public PessoaEntityBuilder withCidade(MunicipioEntity cidade) {
        this.cidade = cidade;
        return this;
    }

    public PessoaEntityBuilder withContatos(List<ContatoEntity> contatos) {
        this.contatos = contatos;
        return this;
    }

    public PessoaEntity build() {
        return new PessoaEntity(this);
    }

    public PessoaEntityBuilder withModel(Pessoa pessoa) {
        return this.withVersion(pessoa.getVersion())
                .withTipoPessoa(pessoa.getTipoPessoa())
                .withDocumento(pessoa.getDocumento())
                .withNome(pessoa.getNome())
                .withNomeFantasia(pessoa.getNomeFantasia())
                .withInscricaoMunicipal(pessoa.getInscricaoMunicipal())
                .withRgInscricaoEstadual(pessoa.getRgInscricaoEstadual())
                .withTipoDestinatario(pessoa.getTipoDestinatario())
                .withConsumidorFinal(pessoa.getConsumidorFinal())
                .withCliente(pessoa.getCliente())
                .withFornecedor(pessoa.getFornecedor())
                .withNaoEnviarCobranca(pessoa.getNaoEnviarCobranca())
                .withTelefone(pessoa.getTelefone())
                .withEmail(pessoa.getEmail())
                .withLogradouro(pessoa.getLogradouro())
                .withNumeroEndereco(pessoa.getNumeroEndereco())
                .withComplemento(pessoa.getComplemento())
                .withBairro(pessoa.getBairro())
                .withCep(pessoa.getCep())
                .withCodigoPais(pessoa.getCodigoPais())
                .withCodigoPostal(pessoa.getCodigoPostal())
                .withNomeMunicipioExterior(pessoa.getNomeMunicipioExterior())
                .withNomeProvincia(pessoa.getNomeProvincia())
                .withId(pessoa.getId())
                .withDataCriacao(pessoa.getDataCriacao())
                .withDataAtualizacao(pessoa.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(pessoa.getEmpresa()).build())
                .withCidade(MunicipioEntity.builderOf().withModel(pessoa.getCidade()).build());
    }

    public List<PessoaEntity> withModelList(List<Pessoa> pessoaList) {
        if (pessoaList == null) {
            return null;
        }

        List<PessoaEntity> list = new ArrayList<PessoaEntity>(pessoaList.size());
        for (Pessoa pessoa: pessoaList) {
           list.add(withModel(pessoa).build());
        }
        return list;
    }

}
