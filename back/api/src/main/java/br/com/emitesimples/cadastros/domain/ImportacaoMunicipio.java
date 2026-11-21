package br.com.emitesimples.cadastros.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.usuarios.domain.Usuario;

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
public class ImportacaoMunicipio {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nomeArquivo can't be empty.")
    @Size(max = 255, message = "Attribute nomeArquivo must have 255 characters maximum.")
    private String nomeArquivo;

    @Size(max = 20, message = "Attribute codificacaoDetectada must have 20 characters maximum.")
    private String codificacaoDetectada;

    @Size(max = 5, message = "Attribute separadorDetectado must have 5 characters maximum.")
    private String separadorDetectado;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 23, message = "Attribute estado must have 23 characters maximum.")
    private String estado;

    @NotNull(message = "The attribute dataHoraInicio can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraInicio;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraFim;

    @NotNull(message = "The attribute linhasLidas can't be null.")
    private Integer linhasLidas;

    @NotNull(message = "The attribute linhasInseridas can't be null.")
    private Integer linhasInseridas;

    @NotNull(message = "The attribute linhasAtualizadas can't be null.")
    private Integer linhasAtualizadas;

    @NotNull(message = "The attribute linhasIguais can't be null.")
    private Integer linhasIguais;

    @NotNull(message = "The attribute linhasRejeitadas can't be null.")
    private Integer linhasRejeitadas;

    @Size(max = 4000, message = "Attribute resumoRejeicoes must have 4000 characters maximum.")
    private String resumoRejeicoes;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute usuario can't be null.")
    private Usuario usuario;

    public ImportacaoMunicipio() {
        this.version = null;
        this.nomeArquivo = null;
        this.codificacaoDetectada = null;
        this.separadorDetectado = null;
        this.estado = null;
        this.dataHoraInicio = null;
        this.dataHoraFim = null;
        this.linhasLidas = null;
        this.linhasInseridas = null;
        this.linhasAtualizadas = null;
        this.linhasIguais = null;
        this.linhasRejeitadas = null;
        this.resumoRejeicoes = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.usuario = null;
    }
    public ImportacaoMunicipio(
        LocalDateTime version,
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
        Usuario usuario
    ) {
        this.version = version;
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

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return this.usuario;
    }

    @JsonIgnore
    public boolean isWeakReference() {
      return !this.isAnyNotKeyFieldsDefined()
              && (
                      this.isIdDefined()
              );
    }
    @JsonIgnore
    public boolean isIdDefined() {
        return this.id != null;
    }

    @JsonIgnore
    public boolean isAnyNotKeyFieldsDefined() {
        return 
            this.nomeArquivo != null || 
            this.codificacaoDetectada != null || 
            this.separadorDetectado != null || 
            this.estado != null || 
            this.dataHoraInicio != null || 
            this.dataHoraFim != null || 
            this.linhasLidas != null || 
            this.linhasInseridas != null || 
            this.linhasAtualizadas != null || 
            this.linhasIguais != null || 
            this.linhasRejeitadas != null || 
            this.resumoRejeicoes != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.usuario != null;
    }

}
