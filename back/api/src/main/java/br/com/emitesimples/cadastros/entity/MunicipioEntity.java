package br.com.emitesimples.cadastros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Municipio;
import br.com.emitesimples.cadastros.builder.MunicipioEntityBuilder;

/*
* Município brasileiro identificado pelo código IBGE de 7 dígitos, exceto o 
* registro especial Exterior (UF EX), que não tem IBGE e carrega o código 0 da 
* tabela oficial; esse 0 nunca é enviado ao IntegraNotas (para pessoa no exterior 
* o envio manda uf = EX e omite o código do município). Dado de referência 
* compartilhado:
 * endereço de Empresa aponta para ele, e a Pessoa o referencia por cidade e UF 
* (relação cidade): a tela mostra cidade e UF e esconde o IBGE, que está direto 
* aqui.
 * O par (nome, UF) é único, regra documentada e não expressa no mapa. O Municipio 
* é buscado por nome e UF. Todo registro tem código: o IBGE de 7 dígitos ou, no 
* Exterior, o 0.
 * Vive no contexto cadastros (domain.cadastros). Há dependência mútua entre os 
* contextos configuracao e cadastros:
 * a Empresa (configuracao) referencia o Municipio, e Pessoa e Servico (cadastros) 
* referenciam a Empresa.
 * Municipio e Empresa devem ser modelados juntos.

*/
@Entity
@Table(name = "MUNICIPIO")
public class MunicipioEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="CODIGO_IBGE", length=7)
    private String codigoIbge;

    @Column(name="NOME", length=100)
    private String nome;

    @Column(name="UF", length=2)
    private String uf;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    public MunicipioEntity() {
    }

    public MunicipioEntity(
        String codigoIbge,
        String nome,
        String uf,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
    ) {
        this.codigoIbge = codigoIbge;
        this.nome = nome;
        this.uf = uf;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public MunicipioEntity(MunicipioEntityBuilder builder) {
        this.version = builder.version;
        this.codigoIbge = builder.codigoIbge;
        this.nome = builder.nome;
        this.uf = builder.uf;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
    }

    public static MunicipioEntityBuilder builderOf() {
        return new MunicipioEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setCodigoIbge(String codigoIbge) {
        this.codigoIbge = codigoIbge;
    }

    public String getCodigoIbge() {
        return this.codigoIbge;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getUf() {
        return this.uf;
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

}
