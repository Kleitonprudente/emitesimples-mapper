package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Banco;
import br.com.emitesimples.financeiro.entity.BancoEntity;
import java.util.List;
import java.util.ArrayList;

/*
 * Banco entity builder
*/
public class BancoEntityBuilder {
    public LocalDateTime version;
    public String codigo;
    public String ispb;
    public String nome;
    public String nomeExtenso;
    public Boolean participaCompe;
    public Boolean integradoGateway;
    public String urlPassoAPasso;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;

    public BancoEntityBuilder() {
    }
    public BancoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public BancoEntityBuilder withCodigo(String codigo) {
        this.codigo = codigo;
        return this;
    }

    public BancoEntityBuilder withIspb(String ispb) {
        this.ispb = ispb;
        return this;
    }

    public BancoEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public BancoEntityBuilder withNomeExtenso(String nomeExtenso) {
        this.nomeExtenso = nomeExtenso;
        return this;
    }

    public BancoEntityBuilder withParticipaCompe(Boolean participaCompe) {
        this.participaCompe = participaCompe;
        return this;
    }

    public BancoEntityBuilder withIntegradoGateway(Boolean integradoGateway) {
        this.integradoGateway = integradoGateway;
        return this;
    }

    public BancoEntityBuilder withUrlPassoAPasso(String urlPassoAPasso) {
        this.urlPassoAPasso = urlPassoAPasso;
        return this;
    }

    public BancoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public BancoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public BancoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public BancoEntity build() {
        return new BancoEntity(this);
    }

    public BancoEntityBuilder withModel(Banco banco) {
        return this.withVersion(banco.getVersion())
                .withCodigo(banco.getCodigo())
                .withIspb(banco.getIspb())
                .withNome(banco.getNome())
                .withNomeExtenso(banco.getNomeExtenso())
                .withParticipaCompe(banco.getParticipaCompe())
                .withIntegradoGateway(banco.getIntegradoGateway())
                .withUrlPassoAPasso(banco.getUrlPassoAPasso())
                .withId(banco.getId())
                .withDataCriacao(banco.getDataCriacao())
                .withDataAtualizacao(banco.getDataAtualizacao());
    }

    public List<BancoEntity> withModelList(List<Banco> bancoList) {
        if (bancoList == null) {
            return null;
        }

        List<BancoEntity> list = new ArrayList<BancoEntity>(bancoList.size());
        for (Banco banco: bancoList) {
           list.add(withModel(banco).build());
        }
        return list;
    }

}
