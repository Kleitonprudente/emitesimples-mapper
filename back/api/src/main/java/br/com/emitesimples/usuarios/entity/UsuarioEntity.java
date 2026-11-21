package br.com.emitesimples.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.builder.UsuarioEntityBuilder;

/*
* Identidade de uma pessoa que entra no sistema. É global: uma pessoa pode ter 
* vínculo com várias empresas, por isso não tem tenantKey, e o isolamento por 
* empresa está no VinculoUsuario. Vive no contexto usuarios (domain.usuarios), que 
* depende de configuracao e cadastros.
 * O e-mail é único no sistema inteiro (o mapa não expressa unicidade, ela é regra 
* de negócio e índice único no banco).
 * NUNCA guarda a senha, só o hash. NUNCA guarda token.

*/
@Entity
@Table(name = "USUARIO")
public class UsuarioEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=120)
    private String nome;

    @Column(name="EMAIL", length=160)
    private String email;

    @Column(name="WHATSAPP", length=20)
    private String whatsapp;

    @Column(name="SENHA_HASH", length=255)
    private String senhaHash;

    @Column(name="ATIVO")
    private Boolean ativo;

    @Column(name="ADMINISTRADOR_PLATAFORMA")
    private Boolean administradorPlataforma;

    @Column(name="DATA_ULTIMO_ACESSO")
    private LocalDateTime dataUltimoAcesso;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    public UsuarioEntity() {
    }

    public UsuarioEntity(
        String nome,
        String email,
        String whatsapp,
        String senhaHash,
        Boolean ativo,
        Boolean administradorPlataforma,
        LocalDateTime dataUltimoAcesso,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
    ) {
        this.nome = nome;
        this.email = email;
        this.whatsapp = whatsapp;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
        this.administradorPlataforma = administradorPlataforma;
        this.dataUltimoAcesso = dataUltimoAcesso;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    public UsuarioEntity(UsuarioEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.email = builder.email;
        this.whatsapp = builder.whatsapp;
        this.senhaHash = builder.senhaHash;
        this.ativo = builder.ativo;
        this.administradorPlataforma = builder.administradorPlataforma;
        this.dataUltimoAcesso = builder.dataUltimoAcesso;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
    }

    public static UsuarioEntityBuilder builderOf() {
        return new UsuarioEntityBuilder();
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

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getWhatsapp() {
        return this.whatsapp;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getSenhaHash() {
        return this.senhaHash;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setAdministradorPlataforma(Boolean administradorPlataforma) {
        this.administradorPlataforma = administradorPlataforma;
    }

    public Boolean getAdministradorPlataforma() {
        return this.administradorPlataforma;
    }

    public void setDataUltimoAcesso(LocalDateTime dataUltimoAcesso) {
        this.dataUltimoAcesso = dataUltimoAcesso;
    }

    public LocalDateTime getDataUltimoAcesso() {
        return this.dataUltimoAcesso;
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
