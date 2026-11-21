package br.com.emitesimples.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.HistoricoSituacaoEmpresa;
import br.com.emitesimples.admin.builder.HistoricoSituacaoEmpresaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
* Auditoria de cada mudança da situação da conta da empresa. É IMUTÁVEL: o 
* registro nunca é editado nem apagado (regra da aplicação). Responde quem mudou, 
* quando e por quê. Não tem ambiente: herda a situação da empresa.
 * Quando a mudança é automática, por exemplo a confirmação pelo link do e-mail, o 
* responsável fica vazio.

*/
@Entity
@Table(name = "HISTORICOSITUACAOEMPRESA")
public class HistoricoSituacaoEmpresaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="SITUACAO_ANTERIOR", length=20)
    private String situacaoAnterior;

    @Column(name="SITUACAO_NOVA", length=20)
    private String situacaoNova;

    @Column(name="MOTIVO", length=500)
    private String motivo;

    @Column(name="ORIGEM", length=20)
    private String origem;

    @Column(name="DATA_HORA")
    private LocalDateTime dataHora;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="RESPONSAVEL_ID", nullable = true)
    private UsuarioEntity responsavel;

    public HistoricoSituacaoEmpresaEntity() {
    }

    public HistoricoSituacaoEmpresaEntity(
        String situacaoAnterior,
        String situacaoNova,
        String motivo,
        String origem,
        LocalDateTime dataHora,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        UsuarioEntity responsavel
    ) {
        this.situacaoAnterior = situacaoAnterior;
        this.situacaoNova = situacaoNova;
        this.motivo = motivo;
        this.origem = origem;
        this.dataHora = dataHora;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.responsavel = responsavel;
    }

    public HistoricoSituacaoEmpresaEntity(HistoricoSituacaoEmpresaEntityBuilder builder) {
        this.version = builder.version;
        this.situacaoAnterior = builder.situacaoAnterior;
        this.situacaoNova = builder.situacaoNova;
        this.motivo = builder.motivo;
        this.origem = builder.origem;
        this.dataHora = builder.dataHora;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.responsavel = builder.responsavel;
    }

    public static HistoricoSituacaoEmpresaEntityBuilder builderOf() {
        return new HistoricoSituacaoEmpresaEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setSituacaoAnterior(String situacaoAnterior) {
        this.situacaoAnterior = situacaoAnterior;
    }

    public String getSituacaoAnterior() {
        return this.situacaoAnterior;
    }

    public void setSituacaoNova(String situacaoNova) {
        this.situacaoNova = situacaoNova;
    }

    public String getSituacaoNova() {
        return this.situacaoNova;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return this.motivo;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getOrigem() {
        return this.origem;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public LocalDateTime getDataHora() {
        return this.dataHora;
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

    public void setResponsavel(UsuarioEntity responsavel) {
        this.responsavel = responsavel;
    }

    public UsuarioEntity getResponsavel() {
        return this.responsavel;
    }

}
