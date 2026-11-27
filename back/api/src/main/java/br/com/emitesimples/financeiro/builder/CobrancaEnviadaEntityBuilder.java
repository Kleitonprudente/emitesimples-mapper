package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CobrancaEnviada;
import br.com.emitesimples.financeiro.entity.CobrancaEnviadaEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * CobrancaEnviada entity builder
*/
public class CobrancaEnviadaEntityBuilder {
    public LocalDateTime version;
    public String idPessoa;
    public String origem;
    public LocalDate dia;
    public String email;
    public LocalDateTime enviadoEm;
    public String idEnviadoPor;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public TituloEntity titulo;
    public EmpresaEntity empresa;

    public CobrancaEnviadaEntityBuilder() {
    }
    public CobrancaEnviadaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withIdPessoa(String idPessoa) {
        this.idPessoa = idPessoa;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withOrigem(String origem) {
        this.origem = origem;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withDia(LocalDate dia) {
        this.dia = dia;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withIdEnviadoPor(String idEnviadoPor) {
        this.idEnviadoPor = idEnviadoPor;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withTitulo(TituloEntity titulo) {
        this.titulo = titulo;
        return this;
    }

    public CobrancaEnviadaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public CobrancaEnviadaEntity build() {
        return new CobrancaEnviadaEntity(this);
    }

    public CobrancaEnviadaEntityBuilder withModel(CobrancaEnviada cobrancaEnviada) {
        return this.withVersion(cobrancaEnviada.getVersion())
                .withIdPessoa(cobrancaEnviada.getIdPessoa())
                .withOrigem(cobrancaEnviada.getOrigem())
                .withDia(cobrancaEnviada.getDia())
                .withEmail(cobrancaEnviada.getEmail())
                .withEnviadoEm(cobrancaEnviada.getEnviadoEm())
                .withIdEnviadoPor(cobrancaEnviada.getIdEnviadoPor())
                .withId(cobrancaEnviada.getId())
                .withDataCriacao(cobrancaEnviada.getDataCriacao())
                .withDataAtualizacao(cobrancaEnviada.getDataAtualizacao())
                .withTitulo(TituloEntity.builderOf().withModel(cobrancaEnviada.getTitulo()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(cobrancaEnviada.getEmpresa()).build());
    }

    public List<CobrancaEnviadaEntity> withModelList(List<CobrancaEnviada> cobrancaEnviadaList) {
        if (cobrancaEnviadaList == null) {
            return null;
        }

        List<CobrancaEnviadaEntity> list = new ArrayList<CobrancaEnviadaEntity>(cobrancaEnviadaList.size());
        for (CobrancaEnviada cobrancaEnviada: cobrancaEnviadaList) {
           list.add(withModel(cobrancaEnviada).build());
        }
        return list;
    }

}
