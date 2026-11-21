package br.com.emitesimples.configuracao.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import br.com.emitesimples.configuracao.entity.AtividadeEmpresaEntity;
import br.com.emitesimples.configuracao.entity.QsaEmpresaEntity;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;

/*
 * Empresa entity builder
*/
public class EmpresaEntityBuilder {
    public LocalDateTime version;
    public String nomeFantasia;
    public String razaoSocial;
    public String documento;
    public String rgInscricaoEstadual;
    public String inscricaoMunicipal;
    public String email;
    public String whatsapp;
    public String telefone;
    public String logradouro;
    public String numeroEndereco;
    public String complemento;
    public String bairro;
    public String cep;
    public Integer regimeTributario;
    public Integer regimeIbsCbs;
    public byte[] logo;
    public String urlWebhook;
    public String situacao;
    public LocalDateTime dataConfirmacao;
    public String referenciaCrm;
    public String statusCadastro;
    public LocalDateTime dataCadastroGateway;
    public String referenciaTokenEmpresa;
    public String referenciaCredenciaisPrefeitura;
    public String referenciaCertificado;
    public LocalDate validadeCertificado;
    public String situacaoCertificado;
    public String id;
    public String ambiente;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public List<AtividadeEmpresaEntity> atividades;
    public List<QsaEmpresaEntity> qsa;
    public MunicipioEntity municipio;

    public EmpresaEntityBuilder() {
    }
    public EmpresaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public EmpresaEntityBuilder withNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
        return this;
    }

    public EmpresaEntityBuilder withRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
        return this;
    }

    public EmpresaEntityBuilder withDocumento(String documento) {
        this.documento = documento;
        return this;
    }

    public EmpresaEntityBuilder withRgInscricaoEstadual(String rgInscricaoEstadual) {
        this.rgInscricaoEstadual = rgInscricaoEstadual;
        return this;
    }

    public EmpresaEntityBuilder withInscricaoMunicipal(String inscricaoMunicipal) {
        this.inscricaoMunicipal = inscricaoMunicipal;
        return this;
    }

    public EmpresaEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public EmpresaEntityBuilder withWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public EmpresaEntityBuilder withTelefone(String telefone) {
        this.telefone = telefone;
        return this;
    }

    public EmpresaEntityBuilder withLogradouro(String logradouro) {
        this.logradouro = logradouro;
        return this;
    }

    public EmpresaEntityBuilder withNumeroEndereco(String numeroEndereco) {
        this.numeroEndereco = numeroEndereco;
        return this;
    }

    public EmpresaEntityBuilder withComplemento(String complemento) {
        this.complemento = complemento;
        return this;
    }

    public EmpresaEntityBuilder withBairro(String bairro) {
        this.bairro = bairro;
        return this;
    }

    public EmpresaEntityBuilder withCep(String cep) {
        this.cep = cep;
        return this;
    }

    public EmpresaEntityBuilder withRegimeTributario(Integer regimeTributario) {
        this.regimeTributario = regimeTributario;
        return this;
    }

    public EmpresaEntityBuilder withRegimeIbsCbs(Integer regimeIbsCbs) {
        this.regimeIbsCbs = regimeIbsCbs;
        return this;
    }

    public EmpresaEntityBuilder withLogo(byte[] logo) {
        this.logo = logo;
        return this;
    }

    public EmpresaEntityBuilder withUrlWebhook(String urlWebhook) {
        this.urlWebhook = urlWebhook;
        return this;
    }

    public EmpresaEntityBuilder withSituacao(String situacao) {
        this.situacao = situacao;
        return this;
    }

    public EmpresaEntityBuilder withDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
        return this;
    }

    public EmpresaEntityBuilder withReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
        return this;
    }

    public EmpresaEntityBuilder withStatusCadastro(String statusCadastro) {
        this.statusCadastro = statusCadastro;
        return this;
    }

    public EmpresaEntityBuilder withDataCadastroGateway(LocalDateTime dataCadastroGateway) {
        this.dataCadastroGateway = dataCadastroGateway;
        return this;
    }

    public EmpresaEntityBuilder withReferenciaTokenEmpresa(String referenciaTokenEmpresa) {
        this.referenciaTokenEmpresa = referenciaTokenEmpresa;
        return this;
    }

    public EmpresaEntityBuilder withReferenciaCredenciaisPrefeitura(String referenciaCredenciaisPrefeitura) {
        this.referenciaCredenciaisPrefeitura = referenciaCredenciaisPrefeitura;
        return this;
    }

    public EmpresaEntityBuilder withReferenciaCertificado(String referenciaCertificado) {
        this.referenciaCertificado = referenciaCertificado;
        return this;
    }

    public EmpresaEntityBuilder withValidadeCertificado(LocalDate validadeCertificado) {
        this.validadeCertificado = validadeCertificado;
        return this;
    }

    public EmpresaEntityBuilder withSituacaoCertificado(String situacaoCertificado) {
        this.situacaoCertificado = situacaoCertificado;
        return this;
    }

    public EmpresaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EmpresaEntityBuilder withAmbiente(String ambiente) {
        this.ambiente = ambiente;
        return this;
    }

    public EmpresaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public EmpresaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public EmpresaEntityBuilder withAtividades(List<AtividadeEmpresaEntity> atividades) {
        this.atividades = atividades;
        return this;
    }

    public EmpresaEntityBuilder withQsa(List<QsaEmpresaEntity> qsa) {
        this.qsa = qsa;
        return this;
    }

    public EmpresaEntityBuilder withMunicipio(MunicipioEntity municipio) {
        this.municipio = municipio;
        return this;
    }

    public EmpresaEntity build() {
        return new EmpresaEntity(this);
    }

    public EmpresaEntityBuilder withModel(Empresa empresa) {
        return this.withVersion(empresa.getVersion())
                .withNomeFantasia(empresa.getNomeFantasia())
                .withRazaoSocial(empresa.getRazaoSocial())
                .withDocumento(empresa.getDocumento())
                .withRgInscricaoEstadual(empresa.getRgInscricaoEstadual())
                .withInscricaoMunicipal(empresa.getInscricaoMunicipal())
                .withEmail(empresa.getEmail())
                .withWhatsapp(empresa.getWhatsapp())
                .withTelefone(empresa.getTelefone())
                .withLogradouro(empresa.getLogradouro())
                .withNumeroEndereco(empresa.getNumeroEndereco())
                .withComplemento(empresa.getComplemento())
                .withBairro(empresa.getBairro())
                .withCep(empresa.getCep())
                .withRegimeTributario(empresa.getRegimeTributario())
                .withRegimeIbsCbs(empresa.getRegimeIbsCbs())
                .withLogo(empresa.getLogo())
                .withUrlWebhook(empresa.getUrlWebhook())
                .withSituacao(empresa.getSituacao())
                .withDataConfirmacao(empresa.getDataConfirmacao())
                .withReferenciaCrm(empresa.getReferenciaCrm())
                .withStatusCadastro(empresa.getStatusCadastro())
                .withDataCadastroGateway(empresa.getDataCadastroGateway())
                .withValidadeCertificado(empresa.getValidadeCertificado())
                .withSituacaoCertificado(empresa.getSituacaoCertificado())
                .withId(empresa.getId())
                .withAmbiente(empresa.getAmbiente())
                .withDataCriacao(empresa.getDataCriacao())
                .withDataAtualizacao(empresa.getDataAtualizacao())
                .withMunicipio(MunicipioEntity.builderOf().withModel(empresa.getMunicipio()).build());
    }

    public List<EmpresaEntity> withModelList(List<Empresa> empresaList) {
        if (empresaList == null) {
            return null;
        }

        List<EmpresaEntity> list = new ArrayList<EmpresaEntity>(empresaList.size());
        for (Empresa empresa: empresaList) {
           list.add(withModel(empresa).build());
        }
        return list;
    }

}
