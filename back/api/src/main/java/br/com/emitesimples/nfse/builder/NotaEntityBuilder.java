package br.com.emitesimples.nfse.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.nfse.domain.Nota;
import br.com.emitesimples.nfse.entity.NotaEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;

/*
 * Nota entity builder
*/
public class NotaEntityBuilder {
    public LocalDateTime version;
    public String numero;
    public String serie;
    public LocalDateTime dataEmissao;
    public LocalDate dataCompetencia;
    public String informacoesComplementares;
    public Integer regimeApuracao;
    public Integer regimeTributacao;
    public String idCondicaoPagamento;
    public String codigoServico;
    public String codigoTributacaoMunicipio;
    public String codigoNbs;
    public String discriminacao;
    public String descricao;
    public BigDecimal valorServicos;
    public BigDecimal valorRecebido;
    public BigDecimal valorDescontoCondicionado;
    public BigDecimal valorDescontoIncondicionado;
    public Integer tipoOperacaoIss;
    public BigDecimal aliquotaIss;
    public BigDecimal valorBaseCalculoIss;
    public BigDecimal valorIssRetido;
    public Boolean issRetido;
    public Integer responsavelRetencao;
    public String cstPisCofins;
    public Integer tipoRetencao;
    public BigDecimal valorBaseCalculo;
    public BigDecimal aliquotaPis;
    public BigDecimal valorPis;
    public BigDecimal aliquotaCofins;
    public BigDecimal valorCofins;
    public BigDecimal valorInss;
    public BigDecimal valorIr;
    public BigDecimal valorCsll;
    public LocalDateTime dataHoraAprovacao;
    public LocalDateTime dataHoraDescarte;
    public String estado;
    public Integer codigoRetorno;
    public String mensagemRetorno;
    public String errosRetorno;
    public String chave;
    public String numeroNfse;
    public String rpsNumero;
    public String rpsSerie;
    public String codigoVerificacao;
    public LocalDateTime dataHoraEvento;
    public String linkPdf;
    public byte[] xml;
    public byte[] pdf;
    public String id;
    public String ambiente;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public PessoaEntity pessoa;
    public ServicoEntity servico;
    public UsuarioEntity preparadoPorUsuario;
    public ClienteAcessoEntity preparadoPorCliente;
    public UsuarioEntity aprovadoPor;
    public UsuarioEntity descartadoPor;
    public List<CancelamentoEntity> cancelamentos;

    public NotaEntityBuilder() {
    }
    public NotaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public NotaEntityBuilder withNumero(String numero) {
        this.numero = numero;
        return this;
    }

    public NotaEntityBuilder withSerie(String serie) {
        this.serie = serie;
        return this;
    }

    public NotaEntityBuilder withDataEmissao(LocalDateTime dataEmissao) {
        this.dataEmissao = dataEmissao;
        return this;
    }

    public NotaEntityBuilder withDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
        return this;
    }

    public NotaEntityBuilder withInformacoesComplementares(String informacoesComplementares) {
        this.informacoesComplementares = informacoesComplementares;
        return this;
    }

    public NotaEntityBuilder withRegimeApuracao(Integer regimeApuracao) {
        this.regimeApuracao = regimeApuracao;
        return this;
    }

    public NotaEntityBuilder withRegimeTributacao(Integer regimeTributacao) {
        this.regimeTributacao = regimeTributacao;
        return this;
    }

    public NotaEntityBuilder withIdCondicaoPagamento(String idCondicaoPagamento) {
        this.idCondicaoPagamento = idCondicaoPagamento;
        return this;
    }

    public NotaEntityBuilder withCodigoServico(String codigoServico) {
        this.codigoServico = codigoServico;
        return this;
    }

    public NotaEntityBuilder withCodigoTributacaoMunicipio(String codigoTributacaoMunicipio) {
        this.codigoTributacaoMunicipio = codigoTributacaoMunicipio;
        return this;
    }

    public NotaEntityBuilder withCodigoNbs(String codigoNbs) {
        this.codigoNbs = codigoNbs;
        return this;
    }

    public NotaEntityBuilder withDiscriminacao(String discriminacao) {
        this.discriminacao = discriminacao;
        return this;
    }

    public NotaEntityBuilder withDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }
    public NotaEntityBuilder withValorServicos(BigDecimal valorServicos) {
        this.valorServicos = valorServicos;
        return this;
    }
    public NotaEntityBuilder withValorRecebido(BigDecimal valorRecebido) {
        this.valorRecebido = valorRecebido;
        return this;
    }
    public NotaEntityBuilder withValorDescontoCondicionado(BigDecimal valorDescontoCondicionado) {
        this.valorDescontoCondicionado = valorDescontoCondicionado;
        return this;
    }
    public NotaEntityBuilder withValorDescontoIncondicionado(BigDecimal valorDescontoIncondicionado) {
        this.valorDescontoIncondicionado = valorDescontoIncondicionado;
        return this;
    }

    public NotaEntityBuilder withTipoOperacaoIss(Integer tipoOperacaoIss) {
        this.tipoOperacaoIss = tipoOperacaoIss;
        return this;
    }
    public NotaEntityBuilder withAliquotaIss(BigDecimal aliquotaIss) {
        this.aliquotaIss = aliquotaIss;
        return this;
    }
    public NotaEntityBuilder withValorBaseCalculoIss(BigDecimal valorBaseCalculoIss) {
        this.valorBaseCalculoIss = valorBaseCalculoIss;
        return this;
    }
    public NotaEntityBuilder withValorIssRetido(BigDecimal valorIssRetido) {
        this.valorIssRetido = valorIssRetido;
        return this;
    }

    public NotaEntityBuilder withIssRetido(Boolean issRetido) {
        this.issRetido = issRetido;
        return this;
    }

    public NotaEntityBuilder withResponsavelRetencao(Integer responsavelRetencao) {
        this.responsavelRetencao = responsavelRetencao;
        return this;
    }

    public NotaEntityBuilder withCstPisCofins(String cstPisCofins) {
        this.cstPisCofins = cstPisCofins;
        return this;
    }

    public NotaEntityBuilder withTipoRetencao(Integer tipoRetencao) {
        this.tipoRetencao = tipoRetencao;
        return this;
    }
    public NotaEntityBuilder withValorBaseCalculo(BigDecimal valorBaseCalculo) {
        this.valorBaseCalculo = valorBaseCalculo;
        return this;
    }
    public NotaEntityBuilder withAliquotaPis(BigDecimal aliquotaPis) {
        this.aliquotaPis = aliquotaPis;
        return this;
    }
    public NotaEntityBuilder withValorPis(BigDecimal valorPis) {
        this.valorPis = valorPis;
        return this;
    }
    public NotaEntityBuilder withAliquotaCofins(BigDecimal aliquotaCofins) {
        this.aliquotaCofins = aliquotaCofins;
        return this;
    }
    public NotaEntityBuilder withValorCofins(BigDecimal valorCofins) {
        this.valorCofins = valorCofins;
        return this;
    }
    public NotaEntityBuilder withValorInss(BigDecimal valorInss) {
        this.valorInss = valorInss;
        return this;
    }
    public NotaEntityBuilder withValorIr(BigDecimal valorIr) {
        this.valorIr = valorIr;
        return this;
    }
    public NotaEntityBuilder withValorCsll(BigDecimal valorCsll) {
        this.valorCsll = valorCsll;
        return this;
    }

    public NotaEntityBuilder withDataHoraAprovacao(LocalDateTime dataHoraAprovacao) {
        this.dataHoraAprovacao = dataHoraAprovacao;
        return this;
    }

    public NotaEntityBuilder withDataHoraDescarte(LocalDateTime dataHoraDescarte) {
        this.dataHoraDescarte = dataHoraDescarte;
        return this;
    }

    public NotaEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }

    public NotaEntityBuilder withCodigoRetorno(Integer codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
        return this;
    }

    public NotaEntityBuilder withMensagemRetorno(String mensagemRetorno) {
        this.mensagemRetorno = mensagemRetorno;
        return this;
    }

    public NotaEntityBuilder withErrosRetorno(String errosRetorno) {
        this.errosRetorno = errosRetorno;
        return this;
    }

    public NotaEntityBuilder withChave(String chave) {
        this.chave = chave;
        return this;
    }

    public NotaEntityBuilder withNumeroNfse(String numeroNfse) {
        this.numeroNfse = numeroNfse;
        return this;
    }

    public NotaEntityBuilder withRpsNumero(String rpsNumero) {
        this.rpsNumero = rpsNumero;
        return this;
    }

    public NotaEntityBuilder withRpsSerie(String rpsSerie) {
        this.rpsSerie = rpsSerie;
        return this;
    }

    public NotaEntityBuilder withCodigoVerificacao(String codigoVerificacao) {
        this.codigoVerificacao = codigoVerificacao;
        return this;
    }

    public NotaEntityBuilder withDataHoraEvento(LocalDateTime dataHoraEvento) {
        this.dataHoraEvento = dataHoraEvento;
        return this;
    }

    public NotaEntityBuilder withLinkPdf(String linkPdf) {
        this.linkPdf = linkPdf;
        return this;
    }

    public NotaEntityBuilder withXml(byte[] xml) {
        this.xml = xml;
        return this;
    }

    public NotaEntityBuilder withPdf(byte[] pdf) {
        this.pdf = pdf;
        return this;
    }

    public NotaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public NotaEntityBuilder withAmbiente(String ambiente) {
        this.ambiente = ambiente;
        return this;
    }

    public NotaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public NotaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public NotaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public NotaEntityBuilder withPessoa(PessoaEntity pessoa) {
        this.pessoa = pessoa;
        return this;
    }

    public NotaEntityBuilder withServico(ServicoEntity servico) {
        this.servico = servico;
        return this;
    }

    public NotaEntityBuilder withPreparadoPorUsuario(UsuarioEntity preparadoPorUsuario) {
        this.preparadoPorUsuario = preparadoPorUsuario;
        return this;
    }

    public NotaEntityBuilder withPreparadoPorCliente(ClienteAcessoEntity preparadoPorCliente) {
        this.preparadoPorCliente = preparadoPorCliente;
        return this;
    }

    public NotaEntityBuilder withAprovadoPor(UsuarioEntity aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
        return this;
    }

    public NotaEntityBuilder withDescartadoPor(UsuarioEntity descartadoPor) {
        this.descartadoPor = descartadoPor;
        return this;
    }

    public NotaEntityBuilder withCancelamentos(List<CancelamentoEntity> cancelamentos) {
        this.cancelamentos = cancelamentos;
        return this;
    }

    public NotaEntity build() {
        return new NotaEntity(this);
    }

    public NotaEntityBuilder withModel(Nota nota) {
        return this.withVersion(nota.getVersion())
                .withNumero(nota.getNumero())
                .withSerie(nota.getSerie())
                .withDataEmissao(nota.getDataEmissao())
                .withDataCompetencia(nota.getDataCompetencia())
                .withInformacoesComplementares(nota.getInformacoesComplementares())
                .withRegimeApuracao(nota.getRegimeApuracao())
                .withRegimeTributacao(nota.getRegimeTributacao())
                .withIdCondicaoPagamento(nota.getIdCondicaoPagamento())
                .withCodigoServico(nota.getCodigoServico())
                .withCodigoTributacaoMunicipio(nota.getCodigoTributacaoMunicipio())
                .withCodigoNbs(nota.getCodigoNbs())
                .withDiscriminacao(nota.getDiscriminacao())
                .withDescricao(nota.getDescricao())
                .withValorServicos(nota.getValorServicos())
                .withValorRecebido(nota.getValorRecebido())
                .withValorDescontoCondicionado(nota.getValorDescontoCondicionado())
                .withValorDescontoIncondicionado(nota.getValorDescontoIncondicionado())
                .withTipoOperacaoIss(nota.getTipoOperacaoIss())
                .withAliquotaIss(nota.getAliquotaIss())
                .withValorBaseCalculoIss(nota.getValorBaseCalculoIss())
                .withValorIssRetido(nota.getValorIssRetido())
                .withIssRetido(nota.getIssRetido())
                .withResponsavelRetencao(nota.getResponsavelRetencao())
                .withCstPisCofins(nota.getCstPisCofins())
                .withTipoRetencao(nota.getTipoRetencao())
                .withValorBaseCalculo(nota.getValorBaseCalculo())
                .withAliquotaPis(nota.getAliquotaPis())
                .withValorPis(nota.getValorPis())
                .withAliquotaCofins(nota.getAliquotaCofins())
                .withValorCofins(nota.getValorCofins())
                .withValorInss(nota.getValorInss())
                .withValorIr(nota.getValorIr())
                .withValorCsll(nota.getValorCsll())
                .withDataHoraAprovacao(nota.getDataHoraAprovacao())
                .withDataHoraDescarte(nota.getDataHoraDescarte())
                .withEstado(nota.getEstado())
                .withCodigoRetorno(nota.getCodigoRetorno())
                .withMensagemRetorno(nota.getMensagemRetorno())
                .withErrosRetorno(nota.getErrosRetorno())
                .withChave(nota.getChave())
                .withNumeroNfse(nota.getNumeroNfse())
                .withRpsNumero(nota.getRpsNumero())
                .withRpsSerie(nota.getRpsSerie())
                .withCodigoVerificacao(nota.getCodigoVerificacao())
                .withDataHoraEvento(nota.getDataHoraEvento())
                .withLinkPdf(nota.getLinkPdf())
                .withXml(nota.getXml())
                .withPdf(nota.getPdf())
                .withId(nota.getId())
                .withAmbiente(nota.getAmbiente())
                .withDataCriacao(nota.getDataCriacao())
                .withDataAtualizacao(nota.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(nota.getEmpresa()).build())
                .withPessoa(PessoaEntity.builderOf().withModel(nota.getPessoa()).build())
                .withServico(ServicoEntity.builderOf().withModel(nota.getServico()).build())
                .withPreparadoPorUsuario(UsuarioEntity.builderOf().withModel(nota.getPreparadoPorUsuario()).build())
                .withPreparadoPorCliente(ClienteAcessoEntity.builderOf().withModel(nota.getPreparadoPorCliente()).build())
                .withAprovadoPor(UsuarioEntity.builderOf().withModel(nota.getAprovadoPor()).build())
                .withDescartadoPor(UsuarioEntity.builderOf().withModel(nota.getDescartadoPor()).build());
    }

    public List<NotaEntity> withModelList(List<Nota> notaList) {
        if (notaList == null) {
            return null;
        }

        List<NotaEntity> list = new ArrayList<NotaEntity>(notaList.size());
        for (Nota nota: notaList) {
           list.add(withModel(nota).build());
        }
        return list;
    }

}
