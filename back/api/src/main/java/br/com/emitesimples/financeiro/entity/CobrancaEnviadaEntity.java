package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CobrancaEnviada;
import br.com.emitesimples.financeiro.builder.CobrancaEnviadaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Registro de cada cobrança por e-mail (manual ou lembrete) mandada a um cliente 
* (docs/financeiro/README.md, 9.3, Fase 6; referência §9.1).
 * No máximo UMA por cliente por dia: o índice único (empresa, idPessoa, dia) 
* reserva o dia ANTES de mandar o e-mail; se o envio falha, a reserva é apagada.
 * Não tem CRUD gerado: só o serviço de cobrança (financeiroop) grava. A pessoa é 
* guardada como idPessoa (como no Título).

*/
@Entity
@Table(name = "COBRANCAENVIADA")
public class CobrancaEnviadaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="ID_PESSOA", length=36)
    private String idPessoa;

    @Column(name="ORIGEM", length=10)
    private String origem;

    @Column(name="DIA")
    private LocalDate dia;

    @Column(name="EMAIL", length=255)
    private String email;

    @Column(name="ENVIADO_EM")
    private LocalDateTime enviadoEm;

    @Column(name="ID_ENVIADO_POR", length=36)
    private String idEnviadoPor;
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

    public CobrancaEnviadaEntity() {
    }

    public CobrancaEnviadaEntity(
        String idPessoa,
        String origem,
        LocalDate dia,
        String email,
        LocalDateTime enviadoEm,
        String idEnviadoPor,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        TituloEntity titulo,
        EmpresaEntity empresa
    ) {
        this.idPessoa = idPessoa;
        this.origem = origem;
        this.dia = dia;
        this.email = email;
        this.enviadoEm = enviadoEm;
        this.idEnviadoPor = idEnviadoPor;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.titulo = titulo;
        this.empresa = empresa;
    }

    public CobrancaEnviadaEntity(CobrancaEnviadaEntityBuilder builder) {
        this.version = builder.version;
        this.idPessoa = builder.idPessoa;
        this.origem = builder.origem;
        this.dia = builder.dia;
        this.email = builder.email;
        this.enviadoEm = builder.enviadoEm;
        this.idEnviadoPor = builder.idEnviadoPor;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.titulo = builder.titulo;
        this.empresa = builder.empresa;
    }

    public static CobrancaEnviadaEntityBuilder builderOf() {
        return new CobrancaEnviadaEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setIdPessoa(String idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getIdPessoa() {
        return this.idPessoa;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getOrigem() {
        return this.origem;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public LocalDate getDia() {
        return this.dia;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public LocalDateTime getEnviadoEm() {
        return this.enviadoEm;
    }

    public void setIdEnviadoPor(String idEnviadoPor) {
        this.idEnviadoPor = idEnviadoPor;
    }

    public String getIdEnviadoPor() {
        return this.idEnviadoPor;
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
