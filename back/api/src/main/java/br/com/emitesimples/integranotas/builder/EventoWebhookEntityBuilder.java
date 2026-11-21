package br.com.emitesimples.integranotas.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.integranotas.domain.EventoWebhook;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;

/*
 * EventoWebhook entity builder
*/
public class EventoWebhookEntityBuilder {
    public LocalDateTime version;
    public String origem;
    public String cnpjCpf;
    public String tipo;
    public String chave;
    public Boolean sucesso;
    public Integer codigo;
    public String mensagem;
    public String identificacao;
    public LocalDateTime dataRecebimento;
    public String resultadoAssinatura;
    public String estadoProcessamento;
    public LocalDateTime dataProcessamento;
    public String id;
    public String ambiente;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public NotaEntity nota;

    public EventoWebhookEntityBuilder() {
    }
    public EventoWebhookEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public EventoWebhookEntityBuilder withOrigem(String origem) {
        this.origem = origem;
        return this;
    }

    public EventoWebhookEntityBuilder withCnpjCpf(String cnpjCpf) {
        this.cnpjCpf = cnpjCpf;
        return this;
    }

    public EventoWebhookEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public EventoWebhookEntityBuilder withChave(String chave) {
        this.chave = chave;
        return this;
    }

    public EventoWebhookEntityBuilder withSucesso(Boolean sucesso) {
        this.sucesso = sucesso;
        return this;
    }

    public EventoWebhookEntityBuilder withCodigo(Integer codigo) {
        this.codigo = codigo;
        return this;
    }

    public EventoWebhookEntityBuilder withMensagem(String mensagem) {
        this.mensagem = mensagem;
        return this;
    }

    public EventoWebhookEntityBuilder withIdentificacao(String identificacao) {
        this.identificacao = identificacao;
        return this;
    }

    public EventoWebhookEntityBuilder withDataRecebimento(LocalDateTime dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
        return this;
    }

    public EventoWebhookEntityBuilder withResultadoAssinatura(String resultadoAssinatura) {
        this.resultadoAssinatura = resultadoAssinatura;
        return this;
    }

    public EventoWebhookEntityBuilder withEstadoProcessamento(String estadoProcessamento) {
        this.estadoProcessamento = estadoProcessamento;
        return this;
    }

    public EventoWebhookEntityBuilder withDataProcessamento(LocalDateTime dataProcessamento) {
        this.dataProcessamento = dataProcessamento;
        return this;
    }

    public EventoWebhookEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EventoWebhookEntityBuilder withAmbiente(String ambiente) {
        this.ambiente = ambiente;
        return this;
    }

    public EventoWebhookEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public EventoWebhookEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public EventoWebhookEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public EventoWebhookEntityBuilder withNota(NotaEntity nota) {
        this.nota = nota;
        return this;
    }

    public EventoWebhookEntity build() {
        return new EventoWebhookEntity(this);
    }

    public EventoWebhookEntityBuilder withModel(EventoWebhook eventoWebhook) {
        return this.withVersion(eventoWebhook.getVersion())
                .withOrigem(eventoWebhook.getOrigem())
                .withCnpjCpf(eventoWebhook.getCnpjCpf())
                .withTipo(eventoWebhook.getTipo())
                .withChave(eventoWebhook.getChave())
                .withSucesso(eventoWebhook.getSucesso())
                .withCodigo(eventoWebhook.getCodigo())
                .withMensagem(eventoWebhook.getMensagem())
                .withIdentificacao(eventoWebhook.getIdentificacao())
                .withDataRecebimento(eventoWebhook.getDataRecebimento())
                .withResultadoAssinatura(eventoWebhook.getResultadoAssinatura())
                .withEstadoProcessamento(eventoWebhook.getEstadoProcessamento())
                .withDataProcessamento(eventoWebhook.getDataProcessamento())
                .withId(eventoWebhook.getId())
                .withAmbiente(eventoWebhook.getAmbiente())
                .withDataCriacao(eventoWebhook.getDataCriacao())
                .withDataAtualizacao(eventoWebhook.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(eventoWebhook.getEmpresa()).build())
                .withNota(NotaEntity.builderOf().withModel(eventoWebhook.getNota()).build());
    }

    public List<EventoWebhookEntity> withModelList(List<EventoWebhook> eventoWebhookList) {
        if (eventoWebhookList == null) {
            return null;
        }

        List<EventoWebhookEntity> list = new ArrayList<EventoWebhookEntity>(eventoWebhookList.size());
        for (EventoWebhook eventoWebhook: eventoWebhookList) {
           list.add(withModel(eventoWebhook).build());
        }
        return list;
    }

}
