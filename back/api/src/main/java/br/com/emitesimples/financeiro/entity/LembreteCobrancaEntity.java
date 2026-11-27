package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.LembreteCobranca;
import br.com.emitesimples.financeiro.builder.LembreteCobrancaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Marca de que um lembrete automático de cobrança já saiu para um título 
* (docs/financeiro/README.md, 9.3, Fase 6; referência §9.1): cada aviso sai UMA 
* vez.
 * O job diário (9h, por empresa, só com lembretesAtivos na configuração) grava 
* aqui antes de enviar; o índice único (título, tipo, dias) impede repetir.
 * Não tem CRUD gerado: só o serviço de cobrança (financeiroop) grava. Vive no 
* contexto financeiro (domain.financeiro).

*/
@Entity
@Table(name = "LEMBRETECOBRANCA")
public class LembreteCobrancaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="TIPO", length=10)
    private String tipo;

    @Column(name="DIAS", precision = 2)
    private Integer dias;

    @Column(name="ENVIADO_EM")
    private LocalDateTime enviadoEm;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="TITULO_ID", nullable = false)
    private TituloEntity titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public LembreteCobrancaEntity() {
    }

    public LembreteCobrancaEntity(
        String tipo,
        Integer dias,
        LocalDateTime enviadoEm,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        TituloEntity titulo,
        EmpresaEntity empresa
    ) {
        this.tipo = tipo;
        this.dias = dias;
        this.enviadoEm = enviadoEm;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.titulo = titulo;
        this.empresa = empresa;
    }

    public LembreteCobrancaEntity(LembreteCobrancaEntityBuilder builder) {
        this.version = builder.version;
        this.tipo = builder.tipo;
        this.dias = builder.dias;
        this.enviadoEm = builder.enviadoEm;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.titulo = builder.titulo;
        this.empresa = builder.empresa;
    }

    public static LembreteCobrancaEntityBuilder builderOf() {
        return new LembreteCobrancaEntityBuilder();
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

    public void setDias(Integer dias) {
        this.dias = dias;
    }

    public Integer getDias() {
        return this.dias;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public LocalDateTime getEnviadoEm() {
        return this.enviadoEm;
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

    public void setTitulo(TituloEntity titulo) {
        this.titulo = titulo;
    }

    public TituloEntity getTitulo() {
        return this.titulo;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
