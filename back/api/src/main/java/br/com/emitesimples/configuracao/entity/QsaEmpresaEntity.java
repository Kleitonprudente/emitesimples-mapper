package br.com.emitesimples.configuracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.configuracao.domain.QsaEmpresa;
import br.com.emitesimples.configuracao.builder.QsaEmpresaEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

/*
* Item do quadro de sócios e administradores (QSA) de uma Empresa, com nome, CPF, 
* qualificação e data de entrada na sociedade. Uma empresa tem vários.
 * Existe só dentro da empresa e no mesmo contexto, por isso é detalhe da Empresa 
* ([m]/[o]). O tenant é a própria empresa. Não tem ambiente: herda o da Empresa.
 * Vive no contexto configuracao (domain.configuracao).

*/
@Entity
@Table(name = "QSAEMPRESA")
public class QsaEmpresaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=300)
    private String nome;

    @Column(name="CPF", length=14)
    private String cpf;

    @Column(name="QUALIFICACAO", length=100)
    private String qualificacao;

    @Column(name="DATA_ENTRADA_SOCIEDADE")
    private LocalDate dataEntradaSociedade;
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

    public QsaEmpresaEntity() {
    }

    public QsaEmpresaEntity(
        String nome,
        String cpf,
        String qualificacao,
        LocalDate dataEntradaSociedade,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa
    ) {
        this.nome = nome;
        this.cpf = cpf;
        this.qualificacao = qualificacao;
        this.dataEntradaSociedade = dataEntradaSociedade;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }

    public QsaEmpresaEntity(QsaEmpresaEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.cpf = builder.cpf;
        this.qualificacao = builder.qualificacao;
        this.dataEntradaSociedade = builder.dataEntradaSociedade;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
    }

    public static QsaEmpresaEntityBuilder builderOf() {
        return new QsaEmpresaEntityBuilder();
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

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setQualificacao(String qualificacao) {
        this.qualificacao = qualificacao;
    }

    public String getQualificacao() {
        return this.qualificacao;
    }

    public void setDataEntradaSociedade(LocalDate dataEntradaSociedade) {
        this.dataEntradaSociedade = dataEntradaSociedade;
    }

    public LocalDate getDataEntradaSociedade() {
        return this.dataEntradaSociedade;
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
