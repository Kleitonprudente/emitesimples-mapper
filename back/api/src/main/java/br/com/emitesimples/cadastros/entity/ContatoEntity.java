package br.com.emitesimples.cadastros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Contato;
import br.com.emitesimples.cadastros.builder.ContatoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Contato de uma Pessoa, com tipo, nome, WhatsApp e e-mail. Uma pessoa tem vários. 
* Existe só dentro da pessoa e no mesmo contexto, por isso é detalhe da Pessoa 
* ([m]/[o]).
 * Os sócios do QSA da consulta de CNPJ entram como contatos do tipo sócio. O 
* e-mail e o telefone principais ficam na própria Pessoa.
 * Vive no contexto cadastros (domain.cadastros) e referencia a Empresa 
* (configuracao) como tenant.

*/
@Entity
@Table(name = "CONTATO")
public class ContatoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TIPO_CONTATO", length=10)
    private String tipoContato;

    @Column(name="NOME", length=300)
    private String nome;

    @Column(name="WHATSAPP", length=20)
    private String whatsapp;

    @Column(name="EMAIL", length=60)
    private String email;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PESSOA_ID", nullable = false)
    private PessoaEntity pessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public ContatoEntity() {
    }

    public ContatoEntity(
        String tipoContato,
        String nome,
        String whatsapp,
        String email,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        PessoaEntity pessoa,
        EmpresaEntity empresa
    ) {
        this.tipoContato = tipoContato;
        this.nome = nome;
        this.whatsapp = whatsapp;
        this.email = email;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.pessoa = pessoa;
        this.empresa = empresa;
    }

    public ContatoEntity(ContatoEntityBuilder builder) {
        this.version = builder.version;
        this.tipoContato = builder.tipoContato;
        this.nome = builder.nome;
        this.whatsapp = builder.whatsapp;
        this.email = builder.email;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.pessoa = builder.pessoa;
        this.empresa = builder.empresa;
    }

    public static ContatoEntityBuilder builderOf() {
        return new ContatoEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTipoContato(String tipoContato) {
        this.tipoContato = tipoContato;
    }

    public String getTipoContato() {
        return this.tipoContato;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getWhatsapp() {
        return this.whatsapp;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataCriacao() {
        return this.dataCriacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return this.dataAtualizacao;
    }

    public void setPessoa(PessoaEntity pessoa) {
        this.pessoa = pessoa;
    }

    public PessoaEntity getPessoa() {
        return this.pessoa;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
