package br.com.emitesimples.cep;

import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cep.CepDtos.CepResposta;
import br.com.emitesimples.cep.CepDtos.Endereco;
import br.com.emitesimples.cep.CepDtos.MunicipioResumo;
import br.com.emitesimples.cep.ViaCepCliente.Resultado;
import br.com.emitesimples.security.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/** Consulta de CEP: formato, limite por usuario, cache em memoria e cruzamento com a tabela Municipio. */
@ApplicationScoped
public class CepService {

    private static final Logger LOG = Logger.getLogger(CepService.class);
    private static final Pattern FORMATO = Pattern.compile("\\d{8}|\\d{5}-\\d{3}");
    static final Duration TTL_ACHADO = Duration.ofHours(24);
    static final Duration TTL_NAO_ACHADO = Duration.ofHours(1);
    static final int MAX_CACHE = 5_000;

    private record Entrada(Endereco endereco, Instant expira) {
    }

    @Inject
    ViaCepCliente cliente;

    @Inject
    LimiteTaxa limite;

    @Inject
    EntityManager em;

    /** LRU simples e sincronizada: so guarda o que o ViaCEP respondeu (achado ou nao achado), nunca falha. */
    private final Map<String, Entrada> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entrada> eldest) {
            return size() > MAX_CACHE;
        }
    };
    private volatile Clock relogio = Clock.systemUTC();

    void usarRelogio(Clock novo) {
        this.relogio = novo;
    }

    void limparCache() {
        synchronized (cache) {
            cache.clear();
        }
        this.relogio = Clock.systemUTC();
    }

    public CepResposta consultar(String usuarioId, String cepBruto) {
        limite.consumir(usuarioId);
        String cep = normalizar(cepBruto);
        Endereco e = buscar(cep);
        if (e == null) {
            throw new ApiException(404, "cep_nao_encontrado", "CEP não encontrado.");
        }
        return new CepResposta(cep, e.logradouro(), e.complemento(), e.bairro(), e.uf(), e.codigoIbge(), municipio(e.codigoIbge()));
    }

    static String normalizar(String cepBruto) {
        if (cepBruto == null || !FORMATO.matcher(cepBruto).matches()) {
            throw new ApiException(400, "cep_invalido", "CEP inválido. Informe 8 dígitos, com ou sem hífen.");
        }
        return cepBruto.replace("-", "");
    }

    private Endereco buscar(String cep) {
        Instant agora = relogio.instant();
        synchronized (cache) {
            Entrada c = cache.get(cep);
            if (c != null) {
                if (agora.isBefore(c.expira())) {
                    return c.endereco();
                }
                cache.remove(cep);
            }
        }
        Resultado r = cliente.buscar(cep);
        Duration ttl = r.endereco() == null ? TTL_NAO_ACHADO : TTL_ACHADO;
        synchronized (cache) {
            cache.put(cep, new Entrada(r.endereco(), relogio.instant().plus(ttl)));
        }
        LOG.debugf("CEP %s*** consultado no ViaCEP.", cep.substring(0, 5));
        return r.endereco();
    }

    private MunicipioResumo municipio(String codigoIbge) {
        if (codigoIbge == null || codigoIbge.isBlank()) {
            return null;
        }
        return em.createQuery("select m from MunicipioEntity m where m.codigoIbge = :c", MunicipioEntity.class)
                .setParameter("c", codigoIbge)
                .getResultStream().findFirst()
                .map(m -> new MunicipioResumo(m.getId(), m.getNome(), m.getUf(), m.getCodigoIbge()))
                .orElse(null);
    }
}
