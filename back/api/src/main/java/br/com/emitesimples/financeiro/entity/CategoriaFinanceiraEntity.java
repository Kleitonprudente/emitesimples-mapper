package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CategoriaFinanceira;
import br.com.emitesimples.financeiro.builder.CategoriaFinanceiraEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Classifica receitas e despesas para relatórios. Hierarquia de UM nível: uma 
* categoria de topo (sem pai) agrupa filhas; o pai é só agrupador.
 * Título, movimento avulso e categoria padrão da configuração só usam categoria 
* SEM filhas (folha). Nome exibido "Pai › Filha".
 * Regras documentadas e não expressas no mapa (docs/financeiro/README.md, F1-H2), 
* implementadas na migração (trigger) e em listener à mão:
 * nome único por empresa e tipo; o pai é da mesma empresa, do mesmo tipo, ativo e 
* de topo; ninguém é pai de si mesmo;
 * quem tem filhas não vira filha nem muda de tipo; só vira pai a categoria fora de 
* uso (não é padrão da configuração e, nas Fases 2 e 3,
 * não tem título nem movimento); inativar pai com filha ativa e ativar filha sob 
* pai inativo são recusados; as dicas perdem os sinais < e >.
 * Nunca se apaga: só se desativa. Escrita só do administrador da empresa.

*/
@Entity
@Table(name = "CATEGORIAFINANCEIRA")
public class CategoriaFinanceiraEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=100)
    private String nome;

    @Column(name="TIPO", length=10)
    private String tipo;

    @Column(name="DICAS", length=500)
    private String dicas;

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
    @JoinColumn(name="CATEGORIA_PAI_ID", nullable = true)
    private CategoriaFinanceiraEntity categoriaPai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public CategoriaFinanceiraEntity() {
    }

    public CategoriaFinanceiraEntity(
        String nome,
        String tipo,
        String dicas,
        Boolean ativo,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        CategoriaFinanceiraEntity categoriaPai,
        EmpresaEntity empresa
    ) {
        this.nome = nome;
        this.tipo = tipo;
        this.dicas = dicas;
        this.ativo = ativo;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.categoriaPai = categoriaPai;
        this.empresa = empresa;
    }

    public CategoriaFinanceiraEntity(CategoriaFinanceiraEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.tipo = builder.tipo;
        this.dicas = builder.dicas;
        this.ativo = builder.ativo;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.categoriaPai = builder.categoriaPai;
        this.empresa = builder.empresa;
    }

    public static CategoriaFinanceiraEntityBuilder builderOf() {
        return new CategoriaFinanceiraEntityBuilder();
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

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setDicas(String dicas) {
        this.dicas = dicas;
    }

    public String getDicas() {
        return this.dicas;
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

    public void setCategoriaPai(CategoriaFinanceiraEntity categoriaPai) {
        this.categoriaPai = categoriaPai;
    }

    public CategoriaFinanceiraEntity getCategoriaPai() {
        return this.categoriaPai;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
