package br.com.emitesimples.nfse.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.nfse.domain.Cancelamento;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;

/*
 * Cancelamento entity builder
*/
public class CancelamentoEntityBuilder {
    public LocalDateTime version;
    public Integer codigoMotivo;
    public String motivo;
    public LocalDateTime dataHoraAprovacao;
    public LocalDateTime dataHoraDescarte;
    public LocalDateTime dataHoraRecusa;
    public String estado;
    public Integer codigoRetorno;
    public String mensagemRetorno;
    public LocalDateTime dataHoraEvento;
    public byte[] xml;
    public byte[] pdf;
    public String id;
    public String ambiente;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public UsuarioEntity preparadoPorUsuario;
    public ClienteAcessoEntity preparadoPorCliente;
    public UsuarioEntity aprovadoPor;
    public UsuarioEntity descartadoPor;
    public NotaEntity nota;

    public CancelamentoEntityBuilder() {
    }
    public CancelamentoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CancelamentoEntityBuilder withCodigoMotivo(Integer codigoMotivo) {
        this.codigoMotivo = codigoMotivo;
        return this;
    }

    public CancelamentoEntityBuilder withMotivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    public CancelamentoEntityBuilder withDataHoraAprovacao(LocalDateTime dataHoraAprovacao) {
        this.dataHoraAprovacao = dataHoraAprovacao;
        return this;
    }

    public CancelamentoEntityBuilder withDataHoraDescarte(LocalDateTime dataHoraDescarte) {
        this.dataHoraDescarte = dataHoraDescarte;
        return this;
    }

    public CancelamentoEntityBuilder withDataHoraRecusa(LocalDateTime dataHoraRecusa) {
        this.dataHoraRecusa = dataHoraRecusa;
        return this;
    }

    public CancelamentoEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }

    public CancelamentoEntityBuilder withCodigoRetorno(Integer codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
        return this;
    }

    public CancelamentoEntityBuilder withMensagemRetorno(String mensagemRetorno) {
        this.mensagemRetorno = mensagemRetorno;
        return this;
    }

    public CancelamentoEntityBuilder withDataHoraEvento(LocalDateTime dataHoraEvento) {
        this.dataHoraEvento = dataHoraEvento;
        return this;
    }

    public CancelamentoEntityBuilder withXml(byte[] xml) {
        this.xml = xml;
        return this;
    }

    public CancelamentoEntityBuilder withPdf(byte[] pdf) {
        this.pdf = pdf;
        return this;
    }

    public CancelamentoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CancelamentoEntityBuilder withAmbiente(String ambiente) {
        this.ambiente = ambiente;
        return this;
    }

    public CancelamentoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CancelamentoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CancelamentoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public CancelamentoEntityBuilder withPreparadoPorUsuario(UsuarioEntity preparadoPorUsuario) {
        this.preparadoPorUsuario = preparadoPorUsuario;
        return this;
    }

    public CancelamentoEntityBuilder withPreparadoPorCliente(ClienteAcessoEntity preparadoPorCliente) {
        this.preparadoPorCliente = preparadoPorCliente;
        return this;
    }

    public CancelamentoEntityBuilder withAprovadoPor(UsuarioEntity aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
        return this;
    }

    public CancelamentoEntityBuilder withDescartadoPor(UsuarioEntity descartadoPor) {
        this.descartadoPor = descartadoPor;
        return this;
    }

    public CancelamentoEntityBuilder withNota(NotaEntity nota) {
        this.nota = nota;
        return this;
    }

    public CancelamentoEntity build() {
        return new CancelamentoEntity(this);
    }

    public CancelamentoEntityBuilder withModel(Cancelamento cancelamento) {
        return this.withVersion(cancelamento.getVersion())
                .withCodigoMotivo(cancelamento.getCodigoMotivo())
                .withMotivo(cancelamento.getMotivo())
                .withDataHoraAprovacao(cancelamento.getDataHoraAprovacao())
                .withDataHoraDescarte(cancelamento.getDataHoraDescarte())
                .withDataHoraRecusa(cancelamento.getDataHoraRecusa())
                .withEstado(cancelamento.getEstado())
                .withCodigoRetorno(cancelamento.getCodigoRetorno())
                .withMensagemRetorno(cancelamento.getMensagemRetorno())
                .withDataHoraEvento(cancelamento.getDataHoraEvento())
                .withXml(cancelamento.getXml())
                .withPdf(cancelamento.getPdf())
                .withId(cancelamento.getId())
                .withAmbiente(cancelamento.getAmbiente())
                .withDataCriacao(cancelamento.getDataCriacao())
                .withDataAtualizacao(cancelamento.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(cancelamento.getEmpresa()).build())
                .withPreparadoPorUsuario(UsuarioEntity.builderOf().withModel(cancelamento.getPreparadoPorUsuario()).build())
                .withPreparadoPorCliente(ClienteAcessoEntity.builderOf().withModel(cancelamento.getPreparadoPorCliente()).build())
                .withAprovadoPor(UsuarioEntity.builderOf().withModel(cancelamento.getAprovadoPor()).build())
                .withDescartadoPor(UsuarioEntity.builderOf().withModel(cancelamento.getDescartadoPor()).build())
                .withNota(NotaEntity.builderOf().withModel(cancelamento.getNota()).build());
    }

    public List<CancelamentoEntity> withModelList(List<Cancelamento> cancelamentoList) {
        if (cancelamentoList == null) {
            return null;
        }

        List<CancelamentoEntity> list = new ArrayList<CancelamentoEntity>(cancelamentoList.size());
        for (Cancelamento cancelamento: cancelamentoList) {
           list.add(withModel(cancelamento).build());
        }
        return list;
    }

}
