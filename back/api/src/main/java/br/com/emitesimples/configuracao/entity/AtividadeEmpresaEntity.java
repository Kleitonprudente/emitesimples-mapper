package br.com.emitesimples.configuracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.configuracao.domain.AtividadeEmpresa;
import br.com.emitesimples.configuracao.builder.AtividadeEmpresaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/*
* Atividade econômica (CNAE) de uma Empresa, com tipo, código CNAE e descrição. 
* Uma empresa tem uma atividade principal e várias secundárias. A regra de no 
* máximo uma principal por empresa é da aplicação.
 * Existe só dentro da empresa e no mesmo contexto, por isso é detalhe da Empresa 
* ([m]/[o]). O tenant é a própria empresa. Não tem ambiente: herda o da Empresa.
 * Vive no contexto configuracao (domain.configuracao).

*/
@Entity
@Table(name = "ATIVIDADEEMPRESA")
public class AtividadeEmpresaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TIPO", length=10)
    private String tipo;

    @Column(name="CODIGO_CNAE", length=7)
    private String codigoCnae;

    @Column(name="DESCRICAO", length=300)
    private String descricao;
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

    public AtividadeEmpresaEntity() {
    }

    public AtividadeEmpresaEntity(
        String tipo,
        String codigoCnae,
        String descricao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa
    ) {
        this.tipo = tipo;
        this.codigoCnae = codigoCnae;
        this.descricao = descricao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }

    public AtividadeEmpresaEntity(AtividadeEmpresaEntityBuilder builder) {
        this.version = builder.version;
        this.tipo = builder.tipo;
        this.codigoCnae = builder.codigoCnae;
        this.descricao = builder.descricao;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
    }

    public static AtividadeEmpresaEntityBuilder builderOf() {
        return new AtividadeEmpresaEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setCodigoCnae(String codigoCnae) {
        this.codigoCnae = codigoCnae;
    }

    public String getCodigoCnae() {
        return this.codigoCnae;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return this.descricao;
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
