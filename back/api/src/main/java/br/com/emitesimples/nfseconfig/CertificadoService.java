package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfseconfig.CertificadoEnvioService.Desfecho;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Certificado digital A1 da empresa da sessao: valida o .pfx em memoria (senha, validade, CNPJ), guarda pfx+senha como UM segredo
 * cifrado no cofre, grava so metadados na Empresa (referencia, validade, situacao) e, se a NFS-e esta habilitada e o emitente tem
 * token, envia ao IntegraNotas. A senha e o arquivo nunca vao para log, excecao, resposta nem toString.
 */
@ApplicationScoped
public class CertificadoService {

    private static final Logger LOG = Logger.getLogger(CertificadoService.class);
    static final Pattern SENHA = Pattern.compile("^[A-Za-z0-9]{1,60}$");
    static final String AVISO_CNPJ = "nao_foi_possivel_conferir_cnpj";

    /** Resposta do POST (e base do bloco {@code certificado} do GET da empresa, sem {@code avisos}). */
    public record CertificadoResposta(String situacao, LocalDate validade, boolean pendenteDeEnvio, Long venceEmDias,
                                      List<String> avisos) {
    }

    @ConfigProperty(name = "emitesimples.certificado.max-bytes", defaultValue = "262144")
    long maxBytes;

    @Inject
    EscopoTenant escopo;

    @Inject
    SegredoService segredos;

    @Inject
    CertificadoEnvioService envio;

    @Inject
    LimiteCertificado limite;

    @Inject
    TravaEmpresa trava;

    /**
     * @param usuarioId quem envia (limite de taxa por usuario)
     * @param nomeArquivo nome informado pelo navegador (so a extensao importa)
     */
    public CertificadoResposta enviar(String usuarioId, String nomeArquivo, long tamanho, Supplier<byte[]> leitor,
            String senha) {
        limite.consumir(usuarioId);
        String empresaId = escopo.empresaIdOuNulo();
        if (empresaId == null) {
            throw new ApiException(403, "empresa_nao_selecionada",
                    "Esta operação vale para a empresa da sessão. Entre na empresa para continuar.");
        }
        // 1) forma do pedido
        if (leitor == null) {
            throw new ApiException(400, "arquivo_obrigatorio", "Envie o certificado no campo \"arquivo\" (multipart/form-data).");
        }
        if (tamanho > maxBytes) {
            throw new ApiException(413, "arquivo_grande", "O arquivo do certificado passa do limite de " + (maxBytes / 1024) + " KB.");
        }
        String nome = nomeArquivo == null ? "" : nomeArquivo.strip().toLowerCase(Locale.ROOT);
        if (!nome.endsWith(".pfx") && !nome.endsWith(".p12")) {
            throw new ApiException(400, "arquivo_tipo_invalido", "Envie o certificado A1 em arquivo .pfx ou .p12.");
        }
        if (senha == null || !SENHA.matcher(senha).matches()) {
            throw new ApiException(400, "certificado_senha_invalida",
                    "A senha do certificado deve ter só letras e números, sem acentos, até 60 caracteres. "
                            + "A IntegraNotas não aceita outros caracteres.");
        }
        // 2-4) abre, confere validade e CNPJ
        byte[] pfx = leitor.get();
        if (pfx.length > maxBytes) {
            throw new ApiException(413, "arquivo_grande", "O arquivo do certificado passa do limite de " + (maxBytes / 1024) + " KB.");
        }
        CertificadoLeitor.Lido lido = CertificadoLeitor.ler(pfx, senha, Instant.now());
        String documento = QuarkusTransaction.requiringNew().call(() -> envio.carregar(empresaId, false).getDocumento());
        List<String> avisos = new ArrayList<>();
        conferirTitular(documento, lido.cnpjs(), avisos);

        // 5-6) guarda e, se der, envia
        return trava.executar(empresaId, () -> guardarEEnviar(empresaId, pfx, senha, lido, avisos));
    }

    private static void conferirTitular(String documentoDaEmpresa, Set<String> cnpjs, List<String> avisos) {
        String doc = documentoDaEmpresa == null ? "" : documentoDaEmpresa.replaceAll("\\D", "");
        if (doc.length() != 14 || cnpjs.isEmpty()) {
            avisos.add(AVISO_CNPJ); // empresa com CPF, ou certificado sem CNPJ legivel: aceita, mas avisa
            return;
        }
        if (!cnpjs.contains(doc)) {
            throw new ApiException(400, "certificado_nao_pertence", "O certificado pertence a outro CNPJ.");
        }
    }

    private CertificadoResposta guardarEEnviar(String empresaId, byte[] pfx, String senha, CertificadoLeitor.Lido lido,
            List<String> avisos) {
        String novo = segredos.guardar(CertificadoEnvioService.empacotar(pfx, senha));
        String[] antigo = new String[1];
        boolean[] podeEnviar = new boolean[1];
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                EmpresaEntity e = envio.carregar(empresaId, true);
                antigo[0] = e.getReferenciaCertificado();
                e.setReferenciaCertificado(novo);
                e.setValidadeCertificado(lido.validade());
                e.setSituacaoCertificado(CertificadoSituacao.NAO_ENVIADO);
                e.setDataAtualizacao(AuthService.agora());
                podeEnviar[0] = Boolean.TRUE.equals(e.getNfseHabilitada()) && e.getReferenciaTokenEmpresa() != null;
            });
        } catch (RuntimeException ex) {
            segredos.remover(novo); // nada referencia o segredo novo
            throw ex;
        }
        if (antigo[0] != null) {
            segredos.remover(antigo[0]); // o certificado anterior e substituido
        }
        if (podeEnviar[0]) {
            Desfecho d = envio.enviarGuardado(empresaId);
            if (!d.enviado()) {
                LOG.infof("Certificado: guardado, envio recusado/falhou (empresa=%s, codigo=%s)", empresaId, d.falha().getCodigo());
                throw d.falha();
            }
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = envio.carregar(empresaId, false);
            return new CertificadoResposta(CertificadoSituacao.situacao(e), e.getValidadeCertificado(),
                    CertificadoSituacao.pendenteDeEnvio(e), CertificadoSituacao.venceEmDias(e), List.copyOf(avisos));
        });
    }
}
