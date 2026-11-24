package br.com.emitesimples.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.VinculoUsuario;
import br.com.emitesimples.usuarios.builder.VinculoUsuarioEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Liga um Usuario a uma Empresa com um papel. É aqui que está o isolamento: o 
* usuário só enxerga as empresas em que tem vínculo ativo, e a empresa do token 
* vem daqui.
 * Unicidade: um usuário tem no máximo um vínculo por empresa (par usuario, empresa 
* único).
 * Papel para scopes: administrador = consultar, preparar, emitir, cancelar; 
* operador = consultar, preparar, emitir e, SÓ se o administrador delegar 
* (podeCancelarNfse), cancelar; consulta = consultar. Emitir e cancelar exigem 
* confirmação humana, regra de fluxo fora deste mapa.

*/
@Entity
@Table(name = "VINCULOUSUARIO")
public class VinculoUsuarioEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="PAPEL", length=15)
    private String papel;

    @Column(name="PODE_CANCELAR_NFSE")
    private Boolean podeCancelarNfse;

    @Column(name="ATIVO")
    private Boolean ativo;
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

    public VinculoUsuarioEntity() {
    }

    public VinculoUsuarioEntity(
        String papel,
        Boolean podeCancelarNfse,
        Boolean ativo,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        UsuarioEntity usuario,
        EmpresaEntity empresa
    ) {
        this.papel = papel;
        this.podeCancelarNfse = podeCancelarNfse;
        this.ativo = ativo;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.usuario = usuario;
        this.empresa = empresa;
    }

    public VinculoUsuarioEntity(VinculoUsuarioEntityBuilder builder) {
        this.version = builder.version;
        this.papel = builder.papel;
        this.podeCancelarNfse = builder.podeCancelarNfse;
        this.ativo = builder.ativo;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.usuario = builder.usuario;
        this.empresa = builder.empresa;
    }

    public static VinculoUsuarioEntityBuilder builderOf() {
        return new VinculoUsuarioEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setPapel(String papel) {
        this.papel = papel;
    }

    public String getPapel() {
        return this.papel;
    }

    public void setPodeCancelarNfse(Boolean podeCancelarNfse) {
        this.podeCancelarNfse = podeCancelarNfse;
    }

    public Boolean getPodeCancelarNfse() {
        return this.podeCancelarNfse;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
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
