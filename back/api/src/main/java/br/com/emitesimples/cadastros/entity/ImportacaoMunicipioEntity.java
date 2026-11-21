package br.com.emitesimples.cadastros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.ImportacaoMunicipio;
import br.com.emitesimples.cadastros.builder.ImportacaoMunicipioEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
* Histórico dos envios do arquivo de cidades (CSV do IBGE) feitos pelo 
* administrador da plataforma. É o rastro de quem alterou a tabela de municípios e 
* quando, e guarda o relatório de cada importação (totais e resumo das linhas 
* rejeitadas). Regra da importação, detalhada em docs/carga-municipios.md: 
* atualiza ou insere pelo código IBGE e nunca apaga cidades, pois Pessoa e Empresa 
* as referenciam; linhas inválidas são rejeitadas e relatadas, sem adivinhar.
 * Registro da plataforma, não de uma empresa: não tem empresa, não tem tenantKey e 
* não tem ambiente (não depende do gateway). Só o administrador da plataforma 
* envia; essa regra é da aplicação e o mapa não a expressa.
 * Vive no contexto cadastros (domain.cadastros) e referencia o Usuario do contexto 
* usuarios, que já referencia configuracao, que por sua vez referencia cadastros. 
* É mais uma volta no ciclo de dependência entre contextos, como a dependência 
* mútua entre configuracao e cadastros: não é erro, pois entre contextos a relação 
* é sempre referência, mas quebra o princípio de modelar do mais independente para 
* o mais dependente.

*/
@Entity
@Table(name = "IMPORTACAOMUNICIPIO")
public class ImportacaoMunicipioEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME_ARQUIVO", length=255)
    private String nomeArquivo;

    @Column(name="CODIFICACAO_DETECTADA", length=20)
    private String codificacaoDetectada;

    @Column(name="SEPARADOR_DETECTADO", length=5)
    private String separadorDetectado;

    @Column(name="ESTADO", length=23)
    private String estado;

    @Column(name="DATA_HORA_INICIO")
    private LocalDateTime dataHoraInicio;

    @Column(name="DATA_HORA_FIM")
    private LocalDateTime dataHoraFim;

    @Column(name="LINHAS_LIDAS", precision = 9)
    private Integer linhasLidas;

    @Column(name="LINHAS_INSERIDAS", precision = 9)
    private Integer linhasInseridas;

    @Column(name="LINHAS_ATUALIZADAS", precision = 9)
    private Integer linhasAtualizadas;

    @Column(name="LINHAS_IGUAIS", precision = 9)
    private Integer linhasIguais;

    @Column(name="LINHAS_REJEITADAS", precision = 9)
    private Integer linhasRejeitadas;

    @Column(name="RESUMO_REJEICOES", length=4000)
    private String resumoRejeicoes;
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

    public ImportacaoMunicipioEntity() {
    }

    public ImportacaoMunicipioEntity(
        String nomeArquivo,
        String codificacaoDetectada,
        String separadorDetectado,
        String estado,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer linhasLidas,
        Integer linhasInseridas,
        Integer linhasAtualizadas,
        Integer linhasIguais,
        Integer linhasRejeitadas,
        String resumoRejeicoes,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        UsuarioEntity usuario
    ) {
        this.nomeArquivo = nomeArquivo;
        this.codificacaoDetectada = codificacaoDetectada;
        this.separadorDetectado = separadorDetectado;
        this.estado = estado;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.linhasLidas = linhasLidas;
        this.linhasInseridas = linhasInseridas;
        this.linhasAtualizadas = linhasAtualizadas;
        this.linhasIguais = linhasIguais;
        this.linhasRejeitadas = linhasRejeitadas;
        this.resumoRejeicoes = resumoRejeicoes;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.usuario = usuario;
    }

    public ImportacaoMunicipioEntity(ImportacaoMunicipioEntityBuilder builder) {
        this.version = builder.version;
        this.nomeArquivo = builder.nomeArquivo;
        this.codificacaoDetectada = builder.codificacaoDetectada;
        this.separadorDetectado = builder.separadorDetectado;
        this.estado = builder.estado;
        this.dataHoraInicio = builder.dataHoraInicio;
        this.dataHoraFim = builder.dataHoraFim;
        this.linhasLidas = builder.linhasLidas;
        this.linhasInseridas = builder.linhasInseridas;
        this.linhasAtualizadas = builder.linhasAtualizadas;
        this.linhasIguais = builder.linhasIguais;
        this.linhasRejeitadas = builder.linhasRejeitadas;
        this.resumoRejeicoes = builder.resumoRejeicoes;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.usuario = builder.usuario;
    }

    public static ImportacaoMunicipioEntityBuilder builderOf() {
        return new ImportacaoMunicipioEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getNomeArquivo() {
        return this.nomeArquivo;
    }

    public void setCodificacaoDetectada(String codificacaoDetectada) {
        this.codificacaoDetectada = codificacaoDetectada;
    }

    public String getCodificacaoDetectada() {
        return this.codificacaoDetectada;
    }

    public void setSeparadorDetectado(String separadorDetectado) {
        this.separadorDetectado = separadorDetectado;
    }

    public String getSeparadorDetectado() {
        return this.separadorDetectado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
    }

    public LocalDateTime getDataHoraInicio() {
        return this.dataHoraInicio;
    }

    public void setDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public LocalDateTime getDataHoraFim() {
        return this.dataHoraFim;
    }

    public void setLinhasLidas(Integer linhasLidas) {
        this.linhasLidas = linhasLidas;
    }

    public Integer getLinhasLidas() {
        return this.linhasLidas;
    }

    public void setLinhasInseridas(Integer linhasInseridas) {
        this.linhasInseridas = linhasInseridas;
    }

    public Integer getLinhasInseridas() {
        return this.linhasInseridas;
    }

    public void setLinhasAtualizadas(Integer linhasAtualizadas) {
        this.linhasAtualizadas = linhasAtualizadas;
    }

    public Integer getLinhasAtualizadas() {
        return this.linhasAtualizadas;
    }

    public void setLinhasIguais(Integer linhasIguais) {
        this.linhasIguais = linhasIguais;
    }

    public Integer getLinhasIguais() {
        return this.linhasIguais;
    }

    public void setLinhasRejeitadas(Integer linhasRejeitadas) {
        this.linhasRejeitadas = linhasRejeitadas;
    }

    public Integer getLinhasRejeitadas() {
        return this.linhasRejeitadas;
    }

    public void setResumoRejeicoes(String resumoRejeicoes) {
        this.resumoRejeicoes = resumoRejeicoes;
    }

    public String getResumoRejeicoes() {
        return this.resumoRejeicoes;
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

}
