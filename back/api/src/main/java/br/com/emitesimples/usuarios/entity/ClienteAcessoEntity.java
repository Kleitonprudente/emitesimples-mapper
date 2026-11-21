package br.com.emitesimples.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.ClienteAcesso;
import br.com.emitesimples.usuarios.builder.ClienteAcessoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Credencial de máquina para os agentes do próprio usuário (fase 1), vinculada a 
* uma única empresa. Troca clientId e segredo por um JWT de acesso com a claim da 
* empresa e os scopes permitidos. Vive no contexto usuarios (domain.usuarios), que 
* depende de configuracao e cadastros.
 * Não é cliente OAuth de terceiros: registro dinâmico e consentimento ficam para a 
* fase 2.
 * O clientId é único no sistema. NUNCA guarda o segredo, só o hash, e NUNCA guarda 
* token.

*/
@Entity
@Table(name = "CLIENTEACESSO")
public class ClienteAcessoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=120)
    private String nome;

    @Column(name="CLIENT_ID", length=64)
    private String clientId;

    @Column(name="SEGREDO_HASH", length=255)
    private String segredoHash;

    @Column(name="SCOPES", length=100)
    private String scopes;

    @Column(name="ATIVO")
    private Boolean ativo;

    @Column(name="DATA_ULTIMO_USO")
    private LocalDateTime dataUltimoUso;

    @Column(name="DATA_EXPIRACAO")
    private LocalDateTime dataExpiracao;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public ClienteAcessoEntity() {
    }

    public ClienteAcessoEntity(
        String nome,
        String clientId,
        String segredoHash,
        String scopes,
        Boolean ativo,
        LocalDateTime dataUltimoUso,
        LocalDateTime dataExpiracao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa
    ) {
        this.nome = nome;
        this.clientId = clientId;
        this.segredoHash = segredoHash;
        this.scopes = scopes;
        this.ativo = ativo;
        this.dataUltimoUso = dataUltimoUso;
        this.dataExpiracao = dataExpiracao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }

    public ClienteAcessoEntity(ClienteAcessoEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.clientId = builder.clientId;
        this.segredoHash = builder.segredoHash;
        this.scopes = builder.scopes;
        this.ativo = builder.ativo;
        this.dataUltimoUso = builder.dataUltimoUso;
        this.dataExpiracao = builder.dataExpiracao;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
    }

    public static ClienteAcessoEntityBuilder builderOf() {
        return new ClienteAcessoEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setSegredoHash(String segredoHash) {
        this.segredoHash = segredoHash;
    }

    public String getSegredoHash() {
        return this.segredoHash;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getScopes() {
        return this.scopes;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setDataUltimoUso(LocalDateTime dataUltimoUso) {
        this.dataUltimoUso = dataUltimoUso;
    }

    public LocalDateTime getDataUltimoUso() {
        return this.dataUltimoUso;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public LocalDateTime getDataExpiracao() {
        return this.dataExpiracao;
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

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
