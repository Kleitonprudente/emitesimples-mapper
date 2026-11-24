package br.com.emitesimples.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.ConfirmacaoCadastro;
import br.com.emitesimples.usuarios.builder.ConfirmacaoCadastroEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Confirmação do signup por link de e-mail. O link enviado ao usuário leva o 
* token; o sistema guarda SÓ o hash do token, nunca o token. Ao confirmar, a 
* empresa passa de PENDENTE_CONFIRMACAO para ATIVA e a dataConfirmacao dela é 
* preenchida.
 * O endpoint de confirmação é PÚBLICO (o usuário ainda não tem sessão): esta 
* entidade NÃO deve ter recurso REST exposto, é regra da aplicação escrita à mão.

*/
@Entity
@Table(name = "CONFIRMACAOCADASTRO")
public class ConfirmacaoCadastroEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TOKEN_HASH", length=255)
    private String tokenHash;

    @Column(name="EMAIL", length=160)
    private String email;

    @Column(name="DATA_EXPIRACAO")
    private LocalDateTime dataExpiracao;

    @Column(name="DATA_CONFIRMACAO")
    private LocalDateTime dataConfirmacao;

    @Column(name="ESTADO", length=10)
    private String estado;
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

    public ConfirmacaoCadastroEntity() {
    }

    public ConfirmacaoCadastroEntity(
        String tokenHash,
        String email,
        LocalDateTime dataExpiracao,
        LocalDateTime dataConfirmacao,
        String estado,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        UsuarioEntity usuario,
        EmpresaEntity empresa
    ) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.dataExpiracao = dataExpiracao;
        this.dataConfirmacao = dataConfirmacao;
        this.estado = estado;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.usuario = usuario;
        this.empresa = empresa;
    }

    public ConfirmacaoCadastroEntity(ConfirmacaoCadastroEntityBuilder builder) {
        this.version = builder.version;
        this.tokenHash = builder.tokenHash;
        this.email = builder.email;
        this.dataExpiracao = builder.dataExpiracao;
        this.dataConfirmacao = builder.dataConfirmacao;
        this.estado = builder.estado;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.usuario = builder.usuario;
        this.empresa = builder.empresa;
    }

    public static ConfirmacaoCadastroEntityBuilder builderOf() {
        return new ConfirmacaoCadastroEntityBuilder();
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

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public LocalDateTime getDataExpiracao() {
        return this.dataExpiracao;
    }

    public void setDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
    }

    public LocalDateTime getDataConfirmacao() {
        return this.dataConfirmacao;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
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
