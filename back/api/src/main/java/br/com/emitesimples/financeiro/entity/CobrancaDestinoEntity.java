package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CobrancaDestino;
import br.com.emitesimples.financeiro.builder.CobrancaDestinoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/*
* Tabela GLOBAL (sem tenant) que acha a empresa quando o webhook do provedor chega 
* sem sessão (docs/financeiro/README.md, 9.5, Fase 7; referência §9.2).
 * Guarda só o hash SHA-256 do segredo do webhook, nunca o segredo. O webhook só 
* identifica a cobrança: quem muda o estado é o sincronizador, que sempre consulta 
* o provedor.
 * Não tem CRUD gerado: só a integração (financeiroop) grava.

*/
@Entity
@Table(name = "COBRANCADESTINO")
public class CobrancaDestinoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="ID_EMPRESA", length=36)
    private String idEmpresa;

    @Column(name="SEGREDO_HASH", length=64)
    private String segredoHash;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREDENCIAL_ID", nullable = false)
    private CredencialCobrancaEntity credencial;

    public CobrancaDestinoEntity() {
    }

    public CobrancaDestinoEntity(
        String idEmpresa,
        String segredoHash,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        CredencialCobrancaEntity credencial
    ) {
        this.idEmpresa = idEmpresa;
        this.segredoHash = segredoHash;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.credencial = credencial;
    }

    public CobrancaDestinoEntity(CobrancaDestinoEntityBuilder builder) {
        this.version = builder.version;
        this.idEmpresa = builder.idEmpresa;
        this.segredoHash = builder.segredoHash;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.credencial = builder.credencial;
    }

    public static CobrancaDestinoEntityBuilder builderOf() {
        return new CobrancaDestinoEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setIdEmpresa(String idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getIdEmpresa() {
        return this.idEmpresa;
    }

    public void setSegredoHash(String segredoHash) {
        this.segredoHash = segredoHash;
    }

    public String getSegredoHash() {
        return this.segredoHash;
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

    public void setCredencial(CredencialCobrancaEntity credencial) {
        this.credencial = credencial;
    }

    public CredencialCobrancaEntity getCredencial() {
        return this.credencial;
    }

}
