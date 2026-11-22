package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.integranotas.entity.ChamadaGatewayEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

/** Grava o log de cada chamada ao gateway (ChamadaGateway): so metadados; NUNCA payload, resposta completa nem token. */
@ApplicationScoped
public class NfseChamadaLog {

    private static final Logger LOG = Logger.getLogger(NfseChamadaLog.class);

    @Inject
    EntityManager em;

    void registrar(String empresaId, String notaId, String operacao, String metodo, String rota, String versao,
            Integer codigoHttp, Integer codigoRetorno, String mensagem, long duracaoMs, String chave,
            String identificacao, String resumo, String ambiente) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                var agora = AuthService.agora();
                em.persist(ChamadaGatewayEntity.builderOf()
                        .withOperacao(operacao).withMetodoHttp(metodo).withRota(rota).withVersaoApi(versao)
                        .withCodigoHttp(codigoHttp).withCodigoRetorno(codigoRetorno)
                        .withMensagemRetorno(NfseRetorno.limitar(mensagem, 500)).withDuracaoMs((int) duracaoMs)
                        .withDataChamada(agora).withChave(chave).withIdentificacao(identificacao)
                        .withResumoEnvio(NfseRetorno.limitar(resumo, 500)).withAmbiente(ambiente)
                        .withDataCriacao(agora).withDataAtualizacao(agora)
                        .withEmpresa(em.getReference(EmpresaEntity.class, empresaId))
                        .withNota(notaId == null ? null : em.getReference(NotaEntity.class, notaId))
                        .build());
            });
        } catch (RuntimeException e) {
            LOG.warnf("NFS-e: nao foi possivel registrar a chamada ao gateway (%s)", e.getClass().getSimpleName());
        }
    }
}
