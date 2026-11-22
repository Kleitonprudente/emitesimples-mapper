package br.com.emitesimples.cadastros.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Servico;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * Servico entity builder
*/
public class ServicoEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String codigoInterno;
    public Boolean ativo;
    public String codigoServico;
    public String codigoTributacaoMunicipio;
    public String codigoNbs;
    public String descricaoCurta;
    public String discriminacaoPadrao;
    public BigDecimal valorPadrao;
    public BigDecimal aliquotaIss;
    public Boolean issRetido;
    public Integer tipoOperacaoIss;
    public String cstPisCofins;
    public BigDecimal percentualTributosSimples;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;

    public ServicoEntityBuilder() {
    }
    public ServicoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ServicoEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public ServicoEntityBuilder withCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
        return this;
    }

    public ServicoEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public ServicoEntityBuilder withCodigoServico(String codigoServico) {
        this.codigoServico = codigoServico;
        return this;
    }

    public ServicoEntityBuilder withCodigoTributacaoMunicipio(String codigoTributacaoMunicipio) {
        this.codigoTributacaoMunicipio = codigoTributacaoMunicipio;
        return this;
    }

    public ServicoEntityBuilder withCodigoNbs(String codigoNbs) {
        this.codigoNbs = codigoNbs;
        return this;
    }

    public ServicoEntityBuilder withDescricaoCurta(String descricaoCurta) {
        this.descricaoCurta = descricaoCurta;
        return this;
    }

    public ServicoEntityBuilder withDiscriminacaoPadrao(String discriminacaoPadrao) {
        this.discriminacaoPadrao = discriminacaoPadrao;
        return this;
    }
    public ServicoEntityBuilder withValorPadrao(BigDecimal valorPadrao) {
        this.valorPadrao = valorPadrao;
        return this;
    }
    public ServicoEntityBuilder withAliquotaIss(BigDecimal aliquotaIss) {
        this.aliquotaIss = aliquotaIss;
        return this;
    }

    public ServicoEntityBuilder withIssRetido(Boolean issRetido) {
        this.issRetido = issRetido;
        return this;
    }

    public ServicoEntityBuilder withTipoOperacaoIss(Integer tipoOperacaoIss) {
        this.tipoOperacaoIss = tipoOperacaoIss;
        return this;
    }

    public ServicoEntityBuilder withCstPisCofins(String cstPisCofins) {
        this.cstPisCofins = cstPisCofins;
        return this;
    }
    public ServicoEntityBuilder withPercentualTributosSimples(BigDecimal percentualTributosSimples) {
        this.percentualTributosSimples = percentualTributosSimples;
        return this;
    }

    public ServicoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ServicoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ServicoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ServicoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ServicoEntity build() {
        return new ServicoEntity(this);
    }

    public ServicoEntityBuilder withModel(Servico servico) {
        return this.withVersion(servico.getVersion())
                .withNome(servico.getNome())
                .withCodigoInterno(servico.getCodigoInterno())
                .withAtivo(servico.getAtivo())
                .withCodigoServico(servico.getCodigoServico())
                .withCodigoTributacaoMunicipio(servico.getCodigoTributacaoMunicipio())
                .withCodigoNbs(servico.getCodigoNbs())
                .withDescricaoCurta(servico.getDescricaoCurta())
                .withDiscriminacaoPadrao(servico.getDiscriminacaoPadrao())
                .withValorPadrao(servico.getValorPadrao())
                .withAliquotaIss(servico.getAliquotaIss())
                .withIssRetido(servico.getIssRetido())
                .withTipoOperacaoIss(servico.getTipoOperacaoIss())
                .withCstPisCofins(servico.getCstPisCofins())
                .withPercentualTributosSimples(servico.getPercentualTributosSimples())
                .withId(servico.getId())
                .withDataCriacao(servico.getDataCriacao())
                .withDataAtualizacao(servico.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(servico.getEmpresa()).build());
    }

    public List<ServicoEntity> withModelList(List<Servico> servicoList) {
        if (servicoList == null) {
            return null;
        }

        List<ServicoEntity> list = new ArrayList<ServicoEntity>(servicoList.size());
        for (Servico servico: servicoList) {
           list.add(withModel(servico).build());
        }
        return list;
    }

}
