package br.com.emitesimples.cadastros.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Contato;
import br.com.emitesimples.cadastros.entity.ContatoEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * Contato entity builder
*/
public class ContatoEntityBuilder {
    public LocalDateTime version;
    public String tipoContato;
    public String nome;
    public String whatsapp;
    public String email;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public PessoaEntity pessoa;
    public EmpresaEntity empresa;

    public ContatoEntityBuilder() {
    }
    public ContatoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ContatoEntityBuilder withTipoContato(String tipoContato) {
        this.tipoContato = tipoContato;
        return this;
    }

    public ContatoEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public ContatoEntityBuilder withWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public ContatoEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public ContatoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ContatoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ContatoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ContatoEntityBuilder withPessoa(PessoaEntity pessoa) {
        this.pessoa = pessoa;
        return this;
    }

    public ContatoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ContatoEntity build() {
        return new ContatoEntity(this);
    }

    public ContatoEntityBuilder withModel(Contato contato) {
        return this.withVersion(contato.getVersion())
                .withTipoContato(contato.getTipoContato())
                .withNome(contato.getNome())
                .withWhatsapp(contato.getWhatsapp())
                .withEmail(contato.getEmail())
                .withId(contato.getId())
                .withDataCriacao(contato.getDataCriacao())
                .withDataAtualizacao(contato.getDataAtualizacao())
                .withPessoa(PessoaEntity.builderOf().withModel(contato.getPessoa()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(contato.getEmpresa()).build());
    }

    public List<ContatoEntity> withModelList(List<Contato> contatoList) {
        if (contatoList == null) {
            return null;
        }

        List<ContatoEntity> list = new ArrayList<ContatoEntity>(contatoList.size());
        for (Contato contato: contatoList) {
           list.add(withModel(contato).build());
        }
        return list;
    }

}
