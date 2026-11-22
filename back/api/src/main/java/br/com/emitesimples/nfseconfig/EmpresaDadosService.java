package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/**
 * Dados cadastrais da EMPRESA DA SESSAO, editados pelo administrador da propria empresa. A empresa e sempre a do token; a escrita
 * e a mao (EntityManager sob a RLS da empresa) porque o {@code /api/Empresa} gerado e so da plataforma. Nao mexe em ambiente,
 * situacao, referencias do cofre, gateway, certificado nem campos {@code nfse*}.
 */
@ApplicationScoped
public class EmpresaDadosService {

    private static final Logger LOG = Logger.getLogger(EmpresaDadosService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    public record MunicipioDto(String id, String nome, String uf, String codigoIbge) {
    }

    public record CertificadoResumo(String situacao, LocalDate validade, boolean pendenteDeEnvio, Long venceEmDias) {
    }

    public record EmpresaDto(String razaoSocial, String nomeFantasia, String documento, String inscricaoMunicipal,
                             String rgInscricaoEstadual, String email, String whatsapp, String telefone, String cep,
                             String logradouro, String numeroEndereco, String complemento, String bairro,
                             MunicipioDto municipio, Integer regimeTributario, Integer regimeIbsCbs,
                             boolean documentoEditavel, CertificadoResumo certificado) {
    }

    public record MunicipioRef(String id) {
    }

    public record EmpresaEntrada(String razaoSocial, String nomeFantasia, String documento, String inscricaoMunicipal,
                                 String rgInscricaoEstadual, String email, String whatsapp, String telefone, String cep,
                                 String logradouro, String numeroEndereco, String complemento, String bairro,
                                 MunicipioRef municipio, Integer regimeTributario, Integer regimeIbsCbs) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    public EmpresaDto obter() {
        String id = empresaDaSessao();
        return QuarkusTransaction.requiringNew().call(() -> dto(carregar(id, false)));
    }

    public EmpresaDto atualizar(EmpresaEntrada in) {
        String id = empresaDaSessao();
        if (in == null) {
            throw invalido(List.of(erro("corpo", "Informe os dados da empresa.")));
        }
        try {
            return QuarkusTransaction.requiringNew().call(() -> gravar(id, in));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            if (violacaoUnicidade(e)) {
                throw new ApiException(409, "documento_duplicado", "Já existe uma empresa cadastrada com este documento.");
            }
            LOG.warnf("Empresa: gravacao falhou (empresa=%s, %s)", id, e.getClass().getSimpleName());
            throw e;
        }
    }

    private EmpresaDto gravar(String id, EmpresaEntrada in) {
        EmpresaEntity e = carregar(id, true);
        boolean editavel = documentoEditavel(e);
        List<Map<String, String>> erros = new ArrayList<>();

        String razao = obrigatorio(erros, "razaoSocial", in.razaoSocial(), 60, "a razão social");
        String fantasia = obrigatorio(erros, "nomeFantasia", in.nomeFantasia(), 60, "o nome fantasia");
        String logradouro = obrigatorio(erros, "logradouro", in.logradouro(), 60, "o logradouro");
        String numero = obrigatorio(erros, "numeroEndereco", in.numeroEndereco(), 60, "o número do endereço");
        String bairro = obrigatorio(erros, "bairro", in.bairro(), 60, "o bairro");
        String complemento = opcional(erros, "complemento", in.complemento(), 60);
        String im = opcional(erros, "inscricaoMunicipal", in.inscricaoMunicipal(), 20);
        String ie = opcional(erros, "rgInscricaoEstadual", in.rgInscricaoEstadual(), 20);
        String whatsapp = opcional(erros, "whatsapp", in.whatsapp(), 20);

        String email = opcional(erros, "email", in.email(), 60);
        if (email != null && !EMAIL.matcher(email).matches()) {
            erros.add(erro("email", "Informe um e-mail válido."));
        }

        String telefone = null;
        if (!vazio(in.telefone())) {
            telefone = digitos(in.telefone());
            if (telefone.length() < 6 || telefone.length() > 14) {
                erros.add(erro("telefone", "Informe o telefone com 6 a 14 dígitos."));
            }
        }

        String cep = digitos(in.cep());
        if (cep.length() != 8) {
            erros.add(erro("cep", "Informe o CEP com 8 dígitos."));
        }

        Integer crt = in.regimeTributario();
        if (crt == null || crt < 1 || crt > 4) {
            erros.add(erro("regimeTributario", "Informe o regime tributário de 1 a 4."));
        }
        Integer cibs = in.regimeIbsCbs();
        if (cibs != null && (cibs < 0 || cibs > 4)) {
            erros.add(erro("regimeIbsCbs", "O regime do IBS/CBS deve ficar de 0 a 4."));
        }

        MunicipioEntity municipio = null;
        if (in.municipio() == null || vazio(in.municipio().id())) {
            erros.add(erro("municipio", "Informe o município."));
        } else {
            municipio = em.find(MunicipioEntity.class, in.municipio().id().strip());
            if (municipio == null) {
                erros.add(erro("municipio", "Município não encontrado."));
            }
        }

        String documento = null;
        if (editavel) {
            documento = digitos(in.documento());
            if (documento.length() != 11 && documento.length() != 14) {
                erros.add(erro("documento", "Informe o CNPJ (14 dígitos) ou o CPF (11 dígitos)."));
            }
        }

        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        if (!editavel && !vazio(in.documento()) && !digitos(in.documento()).equals(digitos(e.getDocumento()))) {
            throw new ApiException(409, "documento_bloqueado",
                    "O documento não pode ser alterado depois que o emitente foi cadastrado no IntegraNotas.");
        }

        e.setRazaoSocial(razao);
        e.setNomeFantasia(fantasia);
        if (editavel) {
            e.setDocumento(documento);
        }
        e.setInscricaoMunicipal(im);
        e.setRgInscricaoEstadual(ie);
        e.setEmail(email);
        e.setWhatsapp(whatsapp);
        e.setTelefone(telefone);
        e.setCep(cep);
        e.setLogradouro(logradouro);
        e.setNumeroEndereco(numero);
        e.setComplemento(complemento);
        e.setBairro(bairro);
        e.setMunicipio(municipio);
        e.setRegimeTributario(crt);
        e.setRegimeIbsCbs(cibs);
        e.setDataAtualizacao(AuthService.agora());
        em.flush(); // a violacao do indice unico do documento aparece aqui (e vira 409), nunca com a mensagem da constraint
        return dto(e);
    }

    // ------------------------------------------------------------------------------------------- internos

    private static boolean documentoEditavel(EmpresaEntity e) {
        return !"cadastrado".equals(e.getStatusCadastro());
    }

    private static EmpresaDto dto(EmpresaEntity e) {
        MunicipioEntity m = e.getMunicipio();
        MunicipioDto md = m == null ? null : new MunicipioDto(m.getId(), m.getNome(), m.getUf(), m.getCodigoIbge());
        CertificadoResumo c = new CertificadoResumo(CertificadoSituacao.situacao(e), e.getValidadeCertificado(),
                CertificadoSituacao.pendenteDeEnvio(e), CertificadoSituacao.venceEmDias(e));
        return new EmpresaDto(e.getRazaoSocial(), e.getNomeFantasia(), e.getDocumento(), e.getInscricaoMunicipal(),
                e.getRgInscricaoEstadual(), e.getEmail(), e.getWhatsapp(), e.getTelefone(), e.getCep(), e.getLogradouro(),
                e.getNumeroEndereco(), e.getComplemento(), e.getBairro(), md, e.getRegimeTributario(),
                e.getRegimeIbsCbs(), documentoEditavel(e), c);
    }

    private String empresaDaSessao() {
        String id = escopo.empresaIdOuNulo();
        if (id == null) {
            throw new ApiException(403, "empresa_nao_selecionada",
                    "Esta operação vale para a empresa da sessão. Entre na empresa para continuar.");
        }
        return id;
    }

    private EmpresaEntity carregar(String empresaId, boolean travar) {
        var q = em.createQuery("select e from EmpresaEntity e where e.id = :id", EmpresaEntity.class)
                .setParameter("id", empresaId);
        if (travar) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return q.getResultStream().findFirst()
                .orElseThrow(() -> new ApiException(404, "nao_encontrado", "Empresa não encontrada."));
    }

    private static boolean violacaoUnicidade(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static String obrigatorio(List<Map<String, String>> erros, String campo, String valor, int max, String nome) {
        if (vazio(valor)) {
            erros.add(erro(campo, "Informe " + nome + "."));
            return null;
        }
        String t = valor.strip();
        if (t.length() > max) {
            erros.add(erro(campo, "Use no máximo " + max + " caracteres."));
        }
        return t;
    }

    private static String opcional(List<Map<String, String>> erros, String campo, String valor, int max) {
        if (vazio(valor)) {
            return null;
        }
        String t = valor.strip();
        if (t.length() > max) {
            erros.add(erro(campo, "Use no máximo " + max + " caracteres."));
        }
        return t;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String digitos(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }

    private static ApiException invalido(List<Map<String, String>> campos) {
        return new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos", campos));
    }
}
