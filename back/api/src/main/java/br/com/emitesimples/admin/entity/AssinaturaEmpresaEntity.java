package br.com.emitesimples.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.AssinaturaEmpresa;
import br.com.emitesimples.admin.builder.AssinaturaEmpresaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
* Vínculo de uma empresa a um plano, feito pelo suporte. Não tem ambiente: herda a 
* situação da empresa.
 * Regra da aplicação (o mapa não a expressa, e o banco a reforça com índice 
* parcial): no máximo UMA assinatura ativa por empresa. Trocar de plano encerra a 
* assinatura anterior (preenche dataFim) e cria outra; nada é apagado.
 * A cobrança é do CRM: a assinatura é enviada ao CRM, e a referenciaCrm guarda o 
* id dela lá.

*/
@Entity
@Table(name = "ASSINATURAEMPRESA")
public class AssinaturaEmpresaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="ESTADO", length=10)
    private String estado;

    @Column(name="DATA_INICIO")
    private LocalDate dataInicio;

    @Column(name="DATA_FIM")
    private LocalDate dataFim;

    @Column(name="REFERENCIA_CRM", length=50)
    private String referenciaCrm;
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
    @JoinColumn(name="PLANO_ID", nullable = false)
    private PlanoEntity plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="VINCULADA_POR_ID", nullable = true)
    private UsuarioEntity vinculadaPor;

    public AssinaturaEmpresaEntity() {
    }

    public AssinaturaEmpresaEntity(
        String estado,
        LocalDate dataInicio,
        LocalDate dataFim,
        String referenciaCrm,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        PlanoEntity plano,
        UsuarioEntity vinculadaPor
    ) {
        this.estado = estado;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.referenciaCrm = referenciaCrm;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.plano = plano;
        this.vinculadaPor = vinculadaPor;
    }

    public AssinaturaEmpresaEntity(AssinaturaEmpresaEntityBuilder builder) {
        this.version = builder.version;
        this.estado = builder.estado;
        this.dataInicio = builder.dataInicio;
        this.dataFim = builder.dataFim;
        this.referenciaCrm = builder.referenciaCrm;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.plano = builder.plano;
        this.vinculadaPor = builder.vinculadaPor;
    }

    public static AssinaturaEmpresaEntityBuilder builderOf() {
        return new AssinaturaEmpresaEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataInicio() {
        return this.dataInicio;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public LocalDate getDataFim() {
        return this.dataFim;
    }

    public void setReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
    }

    public String getReferenciaCrm() {
        return this.referenciaCrm;
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

    public void setPlano(PlanoEntity plano) {
        this.plano = plano;
    }

    public PlanoEntity getPlano() {
        return this.plano;
    }

    public void setVinculadaPor(UsuarioEntity vinculadaPor) {
        this.vinculadaPor = vinculadaPor;
    }

    public UsuarioEntity getVinculadaPor() {
        return this.vinculadaPor;
    }

}
