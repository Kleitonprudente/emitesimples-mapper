package br.com.emitesimples.integranotas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.integranotas.domain.EventoWebhook;
import br.com.emitesimples.integranotas.builder.EventoWebhookEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;

/*
* Cada notificação recebida do gateway no endpoint de webhook do sistema. Registro 
* de auditoria e de idempotência.
 * ATENÇÃO: o corpo exato do webhook de emissão de NFS-e AINDA NÃO FOI CONFIRMADO 
* (as páginas Notificação com sucesso e Notificação com falha não foram lidas). 
* Este modelo cobre só o envelope comum conhecido (origem, cnpj_cpf, tipo) e os 
* campos chave, sucesso, código, mensagem e identificação. Quando o corpo for 
* confirmado, o mapa será revisado.
 * O XML e o PDF do webhook NÃO são guardados aqui: a Nota guarda os documentos.
 * Idempotência: a mesma notificação pode chegar até 3 vezes. Chave de deduplicação 
* sugerida: (empresa, ambiente, chave, código). O mapa não expressa unicidade 
* composta, ela precisa virar constraint única no banco, e uma repetição deve ser 
* reconhecida e marcada como ignorada em vez de reprocessada.
 * A empresa (emitente no IntegraNotas) é resolvida pelo cnpj_cpf e pelo ambiente. 
* A validade da assinatura é registrada, mas não há validação por header e a 
* assinatura na v2 não está confirmada.

*/
@Entity
@Table(name = "EVENTOWEBHOOK")
public class EventoWebhookEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="ORIGEM", length=30)
    private String origem;

    @Column(name="CNPJ_CPF", length=14)
    private String cnpjCpf;

    @Column(name="TIPO", length=30)
    private String tipo;

    @Column(name="CHAVE", length=50)
    private String chave;

    @Column(name="SUCESSO")
    private Boolean sucesso;

    @Column(name="CODIGO", precision = 4)
    private Integer codigo;

    @Column(name="MENSAGEM", length=500)
    private String mensagem;

    @Column(name="IDENTIFICACAO", length=40)
    private String identificacao;

    @Column(name="DATA_RECEBIMENTO")
    private LocalDateTime dataRecebimento;

    @Column(name="RESULTADO_ASSINATURA", length=15)
    private String resultadoAssinatura;

    @Column(name="ESTADO_PROCESSAMENTO", length=15)
    private String estadoProcessamento;

    @Column(name="DATA_PROCESSAMENTO")
    private LocalDateTime dataProcessamento;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="AMBIENTE", length=15)
    private String ambiente;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="NOTA_ID", nullable = true)
    private NotaEntity nota;

    public EventoWebhookEntity() {
    }

    public EventoWebhookEntity(
        String origem,
        String cnpjCpf,
        String tipo,
        String chave,
        Boolean sucesso,
        Integer codigo,
        String mensagem,
        String identificacao,
        LocalDateTime dataRecebimento,
        String resultadoAssinatura,
        String estadoProcessamento,
        LocalDateTime dataProcessamento,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        NotaEntity nota
    ) {
        this.origem = origem;
        this.cnpjCpf = cnpjCpf;
        this.tipo = tipo;
        this.chave = chave;
        this.sucesso = sucesso;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.identificacao = identificacao;
        this.dataRecebimento = dataRecebimento;
        this.resultadoAssinatura = resultadoAssinatura;
        this.estadoProcessamento = estadoProcessamento;
        this.dataProcessamento = dataProcessamento;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.nota = nota;
    }

    public EventoWebhookEntity(EventoWebhookEntityBuilder builder) {
        this.version = builder.version;
        this.origem = builder.origem;
        this.cnpjCpf = builder.cnpjCpf;
        this.tipo = builder.tipo;
        this.chave = builder.chave;
        this.sucesso = builder.sucesso;
        this.codigo = builder.codigo;
        this.mensagem = builder.mensagem;
        this.identificacao = builder.identificacao;
        this.dataRecebimento = builder.dataRecebimento;
        this.resultadoAssinatura = builder.resultadoAssinatura;
        this.estadoProcessamento = builder.estadoProcessamento;
        this.dataProcessamento = builder.dataProcessamento;
        this.id = builder.id;
        this.ambiente = builder.ambiente;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.nota = builder.nota;
    }

    public static EventoWebhookEntityBuilder builderOf() {
        return new EventoWebhookEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getOrigem() {
        return this.origem;
    }

    public void setCnpjCpf(String cnpjCpf) {
        this.cnpjCpf = cnpjCpf;
    }

    public String getCnpjCpf() {
        return this.cnpjCpf;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getChave() {
        return this.chave;
    }

    public void setSucesso(Boolean sucesso) {
        this.sucesso = sucesso;
    }

    public Boolean getSucesso() {
        return this.sucesso;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public Integer getCodigo() {
        return this.codigo;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return this.mensagem;
    }

    public void setIdentificacao(String identificacao) {
        this.identificacao = identificacao;
    }

    public String getIdentificacao() {
        return this.identificacao;
    }

    public void setDataRecebimento(LocalDateTime dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public LocalDateTime getDataRecebimento() {
        return this.dataRecebimento;
    }

    public void setResultadoAssinatura(String resultadoAssinatura) {
        this.resultadoAssinatura = resultadoAssinatura;
    }

    public String getResultadoAssinatura() {
        return this.resultadoAssinatura;
    }

    public void setEstadoProcessamento(String estadoProcessamento) {
        this.estadoProcessamento = estadoProcessamento;
    }

    public String getEstadoProcessamento() {
        return this.estadoProcessamento;
    }

    public void setDataProcessamento(LocalDateTime dataProcessamento) {
        this.dataProcessamento = dataProcessamento;
    }

    public LocalDateTime getDataProcessamento() {
        return this.dataProcessamento;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getAmbiente() {
        return this.ambiente;
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

    public void setNota(NotaEntity nota) {
        this.nota = nota;
    }

    public NotaEntity getNota() {
        return this.nota;
    }

}
