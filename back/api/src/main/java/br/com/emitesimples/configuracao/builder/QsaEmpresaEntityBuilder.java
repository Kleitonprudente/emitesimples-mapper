package br.com.emitesimples.configuracao.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.configuracao.domain.QsaEmpresa;
import br.com.emitesimples.configuracao.entity.QsaEmpresaEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * QsaEmpresa entity builder
*/
public class QsaEmpresaEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String cpf;
    public String qualificacao;
    public LocalDate dataEntradaSociedade;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;

    public QsaEmpresaEntityBuilder() {
    }
    public QsaEmpresaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public QsaEmpresaEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public QsaEmpresaEntityBuilder withCpf(String cpf) {
        this.cpf = cpf;
        return this;
    }

    public QsaEmpresaEntityBuilder withQualificacao(String qualificacao) {
        this.qualificacao = qualificacao;
        return this;
    }

    public QsaEmpresaEntityBuilder withDataEntradaSociedade(LocalDate dataEntradaSociedade) {
        this.dataEntradaSociedade = dataEntradaSociedade;
        return this;
    }

    public QsaEmpresaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public QsaEmpresaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public QsaEmpresaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public QsaEmpresaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public QsaEmpresaEntity build() {
        return new QsaEmpresaEntity(this);
    }

    public QsaEmpresaEntityBuilder withModel(QsaEmpresa qsaEmpresa) {
        return this.withVersion(qsaEmpresa.getVersion())
                .withNome(qsaEmpresa.getNome())
                .withCpf(qsaEmpresa.getCpf())
                .withQualificacao(qsaEmpresa.getQualificacao())
                .withDataEntradaSociedade(qsaEmpresa.getDataEntradaSociedade())
                .withId(qsaEmpresa.getId())
                .withDataCriacao(qsaEmpresa.getDataCriacao())
                .withDataAtualizacao(qsaEmpresa.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(qsaEmpresa.getEmpresa()).build());
    }

    public List<QsaEmpresaEntity> withModelList(List<QsaEmpresa> qsaEmpresaList) {
        if (qsaEmpresaList == null) {
            return null;
        }

        List<QsaEmpresaEntity> list = new ArrayList<QsaEmpresaEntity>(qsaEmpresaList.size());
        for (QsaEmpresa qsaEmpresa: qsaEmpresaList) {
           list.add(withModel(qsaEmpresa).build());
        }
        return list;
    }

}
