package br.com.emitesimples.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.TokenRenovacao;
import br.com.emitesimples.usuarios.builder.TokenRenovacaoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Registro do token de renovação do login, que permite um JWT de acesso curto. 
* Guarda só o hash do token e o estado, para rotação (cada uso gera um novo e 
* marca o anterior) e detecção de reuso (token já usado ou revogado reapresentado 
* derruba a sessão).
 * NUNCA guarda o token, só o hash. O JWT de acesso não é guardado em lugar nenhum.

*/
@Entity
@Table(name = "TOKENRENOVACAO")
public class TokenRenovacaoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TOKEN_HASH", length=255)
    private String tokenHash;

    @Column(name="DATA_EXPIRACAO")
    private LocalDateTime dataExpiracao;

    @Column(name="USADO")
    private Boolean usado;

    @Column(name="REVOGADO")
    private Boolean revogado;

    @Column(name="DATA_USO")
    private LocalDateTime dataUso;

    @Column(name="DATA_REVOGACAO")
    private LocalDateTime dataRevogacao;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USUARIO_ID", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public TokenRenovacaoEntity() {
    }

    public TokenRenovacaoEntity(
        String tokenHash,
        LocalDateTime dataExpiracao,
        Boolean usado,
        Boolean revogado,
        LocalDateTime dataUso,
        LocalDateTime dataRevogacao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        UsuarioEntity usuario,
        EmpresaEntity empresa
    ) {
        this.tokenHash = tokenHash;
        this.dataExpiracao = dataExpiracao;
        this.usado = usado;
        this.revogado = revogado;
        this.dataUso = dataUso;
        this.dataRevogacao = dataRevogacao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.usuario = usuario;
        this.empresa = empresa;
    }

    public TokenRenovacaoEntity(TokenRenovacaoEntityBuilder builder) {
        this.version = builder.version;
        this.tokenHash = builder.tokenHash;
        this.dataExpiracao = builder.dataExpiracao;
        this.usado = builder.usado;
        this.revogado = builder.revogado;
        this.dataUso = builder.dataUso;
        this.dataRevogacao = builder.dataRevogacao;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.usuario = builder.usuario;
        this.empresa = builder.empresa;
    }

    public static TokenRenovacaoEntityBuilder builderOf() {
        return new TokenRenovacaoEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getTokenHash() {
        return this.tokenHash;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public LocalDateTime getDataExpiracao() {
        return this.dataExpiracao;
    }

    public void setUsado(Boolean usado) {
        this.usado = usado;
    }

    public Boolean getUsado() {
        return this.usado;
    }

    public void setRevogado(Boolean revogado) {
        this.revogado = revogado;
    }

    public Boolean getRevogado() {
        return this.revogado;
    }

    public void setDataUso(LocalDateTime dataUso) {
        this.dataUso = dataUso;
    }

    public LocalDateTime getDataUso() {
        return this.dataUso;
    }

    public void setDataRevogacao(LocalDateTime dataRevogacao) {
        this.dataRevogacao = dataRevogacao;
    }

    public LocalDateTime getDataRevogacao() {
        return this.dataRevogacao;
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

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public UsuarioEntity getUsuario() {
        return this.usuario;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
