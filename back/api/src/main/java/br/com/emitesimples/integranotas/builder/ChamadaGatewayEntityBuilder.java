package br.com.emitesimples.integranotas.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.integranotas.domain.ChamadaGateway;
import br.com.emitesimples.integranotas.entity.ChamadaGatewayEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;

/*
 * ChamadaGateway entity builder
*/
public class ChamadaGatewayEntityBuilder {
    public LocalDateTime version;
    public String operacao;
    public String metodoHttp;
    public String rota;
    public String versaoApi;
    public Integer codigoHttp;
    public Integer codigoRetorno;
    public String mensagemRetorno;
    public Integer duracaoMs;
    public LocalDateTime dataChamada;
    public String chave;
    public String identificacao;
    public String resumoEnvio;
    public String id;
    public String ambiente;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public NotaEntity nota;

    public ChamadaGatewayEntityBuilder() {
    }
    public ChamadaGatewayEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ChamadaGatewayEntityBuilder withOperacao(String operacao) {
        this.operacao = operacao;
        return this;
    }

    public ChamadaGatewayEntityBuilder withMetodoHttp(String metodoHttp) {
        this.metodoHttp = metodoHttp;
        return this;
    }

    public ChamadaGatewayEntityBuilder withRota(String rota) {
        this.rota = rota;
        return this;
    }

    public ChamadaGatewayEntityBuilder withVersaoApi(String versaoApi) {
        this.versaoApi = versaoApi;
        return this;
    }

    public ChamadaGatewayEntityBuilder withCodigoHttp(Integer codigoHttp) {
        this.codigoHttp = codigoHttp;
        return this;
    }

    public ChamadaGatewayEntityBuilder withCodigoRetorno(Integer codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
        return this;
    }

    public ChamadaGatewayEntityBuilder withMensagemRetorno(String mensagemRetorno) {
        this.mensagemRetorno = mensagemRetorno;
        return this;
    }

    public ChamadaGatewayEntityBuilder withDuracaoMs(Integer duracaoMs) {
        this.duracaoMs = duracaoMs;
        return this;
    }

    public ChamadaGatewayEntityBuilder withDataChamada(LocalDateTime dataChamada) {
        this.dataChamada = dataChamada;
        return this;
    }

    public ChamadaGatewayEntityBuilder withChave(String chave) {
        this.chave = chave;
        return this;
    }

    public ChamadaGatewayEntityBuilder withIdentificacao(String identificacao) {
        this.identificacao = identificacao;
        return this;
    }

    public ChamadaGatewayEntityBuilder withResumoEnvio(String resumoEnvio) {
        this.resumoEnvio = resumoEnvio;
        return this;
    }

    public ChamadaGatewayEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ChamadaGatewayEntityBuilder withAmbiente(String ambiente) {
        this.ambiente = ambiente;
        return this;
    }

    public ChamadaGatewayEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ChamadaGatewayEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ChamadaGatewayEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ChamadaGatewayEntityBuilder withNota(NotaEntity nota) {
        this.nota = nota;
        return this;
    }

    public ChamadaGatewayEntity build() {
        return new ChamadaGatewayEntity(this);
    }

    public ChamadaGatewayEntityBuilder withModel(ChamadaGateway chamadaGateway) {
        return this.withVersion(chamadaGateway.getVersion())
                .withOperacao(chamadaGateway.getOperacao())
                .withMetodoHttp(chamadaGateway.getMetodoHttp())
                .withRota(chamadaGateway.getRota())
                .withVersaoApi(chamadaGateway.getVersaoApi())
                .withCodigoHttp(chamadaGateway.getCodigoHttp())
                .withCodigoRetorno(chamadaGateway.getCodigoRetorno())
                .withMensagemRetorno(chamadaGateway.getMensagemRetorno())
                .withDuracaoMs(chamadaGateway.getDuracaoMs())
                .withDataChamada(chamadaGateway.getDataChamada())
                .withChave(chamadaGateway.getChave())
                .withIdentificacao(chamadaGateway.getIdentificacao())
                .withResumoEnvio(chamadaGateway.getResumoEnvio())
                .withId(chamadaGateway.getId())
                .withAmbiente(chamadaGateway.getAmbiente())
                .withDataCriacao(chamadaGateway.getDataCriacao())
                .withDataAtualizacao(chamadaGateway.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(chamadaGateway.getEmpresa()).build())
                .withNota(NotaEntity.builderOf().withModel(chamadaGateway.getNota()).build());
    }

    public List<ChamadaGatewayEntity> withModelList(List<ChamadaGateway> chamadaGatewayList) {
        if (chamadaGatewayList == null) {
            return null;
        }

        List<ChamadaGatewayEntity> list = new ArrayList<ChamadaGatewayEntity>(chamadaGatewayList.size());
        for (ChamadaGateway chamadaGateway: chamadaGatewayList) {
           list.add(withModel(chamadaGateway).build());
        }
        return list;
    }

}
