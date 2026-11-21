package br.com.emitesimples.configuracao.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import br.com.emitesimples.exceptions.InvalidInputException;
import jakarta.persistence.criteria.*;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.tenant.EscopoTenant;
@ApplicationScoped
public class EmpresaRepository implements PanacheRepositoryBase<EmpresaEntity, String> {

    @Inject EntityManager em;

    /**
     * Escopo de tenant do acesso (isolamento multi-tenant). Governante desta entidade:
     * <b>id</b>, declarado com
     * {@code tenantKey} no mapa .mi.
     *
     * <p>O predicado sai daqui e nao de parametro: filtro de query e conveniencia do
     * usuario, escopo e do sistema, e os dois convivem com o do sistema mandando.
     */
    @Inject EscopoTenant escopo;
    /**
     * Descarrega as gravacoes pendentes <b>dentro</b> do try do resource: a violacao
     * estoura aqui e nao no commit (onde viraria 500 e vazaria SQL). Unicidade
     * (SQLState 23505) e conflito de versao viram 409; as demais violacoes viram 400,
     * sem expor o SQL.
     */
    public void gravarAgora() {
        try {
            em.flush();
        } catch (PersistenceException pe) {
            throw traduzirFalhaDeGravacao(pe);
        }
    }
    private WebApplicationException traduzirFalhaDeGravacao(PersistenceException pe) {
        if (pe instanceof jakarta.persistence.OptimisticLockException) {
            return new ClientErrorException("O registro foi alterado por outra operacao. Recarregue e tente de novo.", 409);
        }
        Throwable causa = pe;
        for (int i = 0; causa != null && i < 10; i++, causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                return new ClientErrorException("Ja existe um registro com os mesmos dados unicos.", 409);
            }
        }
        return new InvalidInputException("Nao foi possivel gravar o registro: dados invalidos ou em conflito com uma restricao.", pe.getCause());
    }

    /**
     * Busca por id <b>dentro do escopo</b>. Registro de outro tenant nao e
     * encontrado — e {@code Optional.empty()} vira 404 no resource, nunca 403: 403
     * confirmaria que o id existe, que e metade do que um enumerador quer saber.
     *
     * <p>Sobrescreve o metodo do Panache de proposito: e por ele que passam tambem o
     * PUT, o PATCH e os mappers de relacionamento.
     */
    @Override
    public Optional<EmpresaEntity> findByIdOptional(String id) {
        if (escopo.irrestrito()) {
            return find("id = ?1", id).firstResultOptional();
        }
        return find("id = ?1 and id = ?2", id, escopo.exigir("id"))
                .firstResultOptional();
    }

    /** Gemeo escopado de {@code findByIdOptional} — o Panache expoe os dois, e servico usa este. */
    @Override
    public EmpresaEntity findById(String id) {
        return findByIdOptional(id).orElse(null);
    }

    /** Remocao escopada: id de outro tenant nao e encontrado, logo nao e removido. */
    @Override
    public boolean deleteById(String id) {
        return findByIdOptional(id).map(e -> { delete(e); return true; }).orElse(false);
    }

    /**
     * Grava <b>impondo</b> o tenant do contexto — nao valida o que veio no body.
     *
     * <p>Validar deixaria a decisao de "o que fazer quando diverge" espalhada pelos
     * resources; impor tira a escolha do chamador, que e o ponto.
     */
    @Override
    public void persist(EmpresaEntity entity) {
        if (!escopo.irrestrito()) {
            entity.setId(escopo.exigir("id"));
        }
        em.persist(entity);
    }

    public List<EmpresaEntity> findWithFilters(
        int offset,
        int pageSize,
        Sort sort,
        String nomeFantasia,
        String razaoSocial,
        String documento,
        String rgInscricaoEstadual,
        String inscricaoMunicipal,
        String email,
        String whatsapp,
        String telefone,
        String logradouro,
        String numeroEndereco,
        String complemento,
        String bairro,
        String cep,
        String regimeTributario,
        String regimeIbsCbs,
        String urlWebhook,
        String situacao,
        String dataConfirmacao,
        String referenciaCrm,
        String statusCadastro,
        String dataCadastroGateway,
       String validadeCertificado,
        String situacaoCertificado,
        String id,
        String ambiente,
        String dataCriacao,
        String dataAtualizacao,
        String municipioCodigoIbge,
        String municipioNome,
        String municipioUf,
        String municipioId
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EmpresaEntity> cq = cb.createQuery(EmpresaEntity.class);
        Root<EmpresaEntity> root = cq.from(EmpresaEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, nomeFantasia, razaoSocial, documento, rgInscricaoEstadual, inscricaoMunicipal, email, whatsapp, telefone, logradouro, numeroEndereco, complemento, bairro, cep, regimeTributario, regimeIbsCbs, urlWebhook, situacao, dataConfirmacao, referenciaCrm, statusCadastro, dataCadastroGateway, validadeCertificado, situacaoCertificado, id, ambiente, dataCriacao, dataAtualizacao, municipioCodigoIbge, municipioNome, municipioUf, municipioId);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        if (sort != null) {
            List<Order> orders = new ArrayList<>();
            sort.getColumns().forEach(col -> {
                if (col.getDirection() == Sort.Direction.Ascending) orders.add(cb.asc(root.get(col.getName())));
                else orders.add(cb.desc(root.get(col.getName())));
            });
            cq.orderBy(orders);
        }
        TypedQuery<EmpresaEntity> query = em.createQuery(cq);
        query.setFirstResult(offset);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    public long countWithFilters(
        String nomeFantasia,
        String razaoSocial,
        String documento,
        String rgInscricaoEstadual,
        String inscricaoMunicipal,
        String email,
        String whatsapp,
        String telefone,
        String logradouro,
        String numeroEndereco,
        String complemento,
        String bairro,
        String cep,
        String regimeTributario,
        String regimeIbsCbs,
        String urlWebhook,
        String situacao,
        String dataConfirmacao,
        String referenciaCrm,
        String statusCadastro,
        String dataCadastroGateway,
       String validadeCertificado,
        String situacaoCertificado,
        String id,
        String ambiente,
        String dataCriacao,
        String dataAtualizacao,
        String municipioCodigoIbge,
        String municipioNome,
        String municipioUf,
        String municipioId
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<EmpresaEntity> root = cq.from(EmpresaEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, nomeFantasia, razaoSocial, documento, rgInscricaoEstadual, inscricaoMunicipal, email, whatsapp, telefone, logradouro, numeroEndereco, complemento, bairro, cep, regimeTributario, regimeIbsCbs, urlWebhook, situacao, dataConfirmacao, referenciaCrm, statusCadastro, dataCadastroGateway, validadeCertificado, situacaoCertificado, id, ambiente, dataCriacao, dataAtualizacao, municipioCodigoIbge, municipioNome, municipioUf, municipioId);
        cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));

        return em.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<EmpresaEntity> root,
        String nomeFantasia,
        String razaoSocial,
        String documento,
        String rgInscricaoEstadual,
        String inscricaoMunicipal,
        String email,
        String whatsapp,
        String telefone,
        String logradouro,
        String numeroEndereco,
        String complemento,
        String bairro,
        String cep,
        String regimeTributario,
        String regimeIbsCbs,
        String urlWebhook,
        String situacao,
        String dataConfirmacao,
        String referenciaCrm,
        String statusCadastro,
        String dataCadastroGateway,
       String validadeCertificado,
        String situacaoCertificado,
        String id,
        String ambiente,
        String dataCriacao,
        String dataAtualizacao,
        String municipioCodigoIbge,
        String municipioNome,
        String municipioUf,
        String municipioId
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (!escopo.irrestrito()) {
            predicates.add(cb.equal(root.get("id"), escopo.exigir("id")));
        }

        if (nomeFantasia != null) getPredicateFromField(predicates, "nomeFantasia", nomeFantasia, root, cb, true);
        if (razaoSocial != null) getPredicateFromField(predicates, "razaoSocial", razaoSocial, root, cb, true);
        if (documento != null) getPredicateFromField(predicates, "documento", documento, root, cb, false);
        if (rgInscricaoEstadual != null) getPredicateFromField(predicates, "rgInscricaoEstadual", rgInscricaoEstadual, root, cb, false);
        if (inscricaoMunicipal != null) getPredicateFromField(predicates, "inscricaoMunicipal", inscricaoMunicipal, root, cb, false);
        if (email != null) getPredicateFromField(predicates, "email", email, root, cb, false);
        if (whatsapp != null) getPredicateFromField(predicates, "whatsapp", whatsapp, root, cb, false);
        if (telefone != null) getPredicateFromField(predicates, "telefone", telefone, root, cb, false);
        if (logradouro != null) getPredicateFromField(predicates, "logradouro", logradouro, root, cb, false);
        if (numeroEndereco != null) getPredicateFromField(predicates, "numeroEndereco", numeroEndereco, root, cb, false);
        if (complemento != null) getPredicateFromField(predicates, "complemento", complemento, root, cb, false);
        if (bairro != null) getPredicateFromField(predicates, "bairro", bairro, root, cb, false);
        if (cep != null) getPredicateFromField(predicates, "cep", cep, root, cb, false);
        if (regimeTributario != null) getPredicateFromField(predicates, "regimeTributario", regimeTributario, root, cb, false);
        if (regimeIbsCbs != null) getPredicateFromField(predicates, "regimeIbsCbs", regimeIbsCbs, root, cb, false);
        if (urlWebhook != null) getPredicateFromField(predicates, "urlWebhook", urlWebhook, root, cb, false);
        if (situacao != null) getPredicateFromField(predicates, "situacao", situacao, root, cb, false);
        if (dataConfirmacao != null) getPredicateFromField(predicates, "dataConfirmacao", dataConfirmacao, root, cb, false);
        if (referenciaCrm != null) getPredicateFromField(predicates, "referenciaCrm", referenciaCrm, root, cb, false);
        if (statusCadastro != null) getPredicateFromField(predicates, "statusCadastro", statusCadastro, root, cb, false);
        if (dataCadastroGateway != null) getPredicateFromField(predicates, "dataCadastroGateway", dataCadastroGateway, root, cb, false);
        if (validadeCertificado != null) getPredicateFromField(predicates, "validadeCertificado", validadeCertificado, root, cb, false);
        if (situacaoCertificado != null) getPredicateFromField(predicates, "situacaoCertificado", situacaoCertificado, root, cb, false);
        if (id != null) getPredicateFromField(predicates, "id", id, root, cb, false);
        if (ambiente != null) getPredicateFromField(predicates, "ambiente", ambiente, root, cb, false);
        if (dataCriacao != null) getPredicateFromField(predicates, "dataCriacao", dataCriacao, root, cb, false);
        if (dataAtualizacao != null) getPredicateFromField(predicates, "dataAtualizacao", dataAtualizacao, root, cb, false);
        if (municipioId != null) {
            predicates.add(cb.equal(root.join("municipio", JoinType.INNER).get("id"), municipioId));
        }
        return predicates;
    }

    public List<Predicate> getPredicateFromField(List<Predicate> predicates, String attributeName, String value, Root<EmpresaEntity> root, CriteriaBuilder criteriaBuilder, boolean unaccent) {
        String fieldType = root.get(attributeName).getJavaType().getName();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (value.startsWith("upper(")) {
            predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get(attributeName)), value.toUpperCase()));
        } else if (value.contains("*")) {
            if (unaccent) {
                Expression<String> fieldExpr = criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get(attributeName)));
                Expression<String> valueExpr = criteriaBuilder.function("unaccent", String.class, criteriaBuilder.literal(value.toLowerCase().replace("*", "%")));
                predicates.add(criteriaBuilder.like(fieldExpr, valueExpr));
            } else {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(attributeName)), value.toLowerCase().replace("*","%")));
            }
        } else if (value.startsWith("less(")) {
            value = value.substring(value.indexOf("(")+1, value.indexOf(")"));
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.lessThan(root.get(attributeName), Integer.decode(value)));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.lessThan(root.get(attributeName), Double.parseDouble(value)));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.lessThan(root.get(attributeName), new BigDecimal(value)));
            } else if (fieldType.equals("java.lang.Boolean")) {
                predicates.add(criteriaBuilder.lessThan(root.get(attributeName), Boolean.parseBoolean(value))); 
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.lessThan(root.get(attributeName),  LocalDate.parse(value, formatter))); 
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.lessThan(root.get(attributeName),  LocalDateTime.parse(value, formatterDateTime))); 
            }
        } else if (value.startsWith("lessEquals(")) {
            value = value.substring(value.indexOf("(")+1, value.indexOf(")"));
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attributeName), Integer.decode(value)));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attributeName), Double.parseDouble(value)));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attributeName), new BigDecimal(value)));
            } else if (fieldType.equals("java.lang.Boolean")) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attributeName), Boolean.parseBoolean(value))); 
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attributeName),  LocalDate.parse(value, formatter))); 
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(attributeName),  LocalDateTime.parse(value, formatterDateTime))); 
            }
        } else if (value.startsWith("notEquals(")) {
            value = value.substring(value.indexOf("(")+1, value.indexOf(")"));
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.notEqual(root.get(attributeName), Integer.decode(value)));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.notEqual(root.get(attributeName), Double.parseDouble(value)));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.notEqual(root.get(attributeName), new BigDecimal(value)));
            } else if (fieldType.equals("java.lang.Boolean")) {
                predicates.add(criteriaBuilder.notEqual(root.get(attributeName), Boolean.parseBoolean(value))); 
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.notEqual(root.get(attributeName),  LocalDate.parse(value, formatter))); 
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.notEqual(root.get(attributeName),  LocalDateTime.parse(value, formatterDateTime))); 
            }
        } else if (value.startsWith("greater(")) {
            value = value.substring(value.indexOf("(")+1, value.indexOf(")"));
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.greaterThan(root.get(attributeName), Integer.decode(value)));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.greaterThan(root.get(attributeName), Double.parseDouble(value)));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.greaterThan(root.get(attributeName), new BigDecimal(value)));
            } else if (fieldType.equals("java.lang.Boolean")) {
                predicates.add(criteriaBuilder.greaterThan(root.get(attributeName), Boolean.parseBoolean(value))); 
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.greaterThan(root.get(attributeName), LocalDate.parse(value, formatter))); 
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.greaterThan(root.get(attributeName), LocalDateTime.parse(value, formatterDateTime))); 
            }
            
        } else if (value.startsWith("greaterEquals(")) {
            value = value.substring(value.indexOf("(")+1, value.indexOf(")"));
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attributeName), Integer.decode(value)));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attributeName), Double.parseDouble(value)));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attributeName), new BigDecimal(value)));
            } else if (fieldType.equals("java.lang.Boolean")) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attributeName), Boolean.parseBoolean(value))); 
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attributeName), LocalDate.parse(value, formatter))); 
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(attributeName), LocalDateTime.parse(value, formatterDateTime))); 
            }
        } else if (value.startsWith("between(")) {
            String[] betweenValues = value.substring(8, value.indexOf(")")).split(",");
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.between(root.get(attributeName), Integer.decode(betweenValues[0]), Integer.decode(betweenValues[1])));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.between(root.get(attributeName), Double.parseDouble(betweenValues[0]), Double.parseDouble(betweenValues[1])));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.between(root.get(attributeName), new BigDecimal(betweenValues[0].trim()), new BigDecimal(betweenValues[1].trim())));
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.between(root.get(attributeName), LocalDate.parse(betweenValues[0], formatter), LocalDate.parse(betweenValues[1], formatter)));
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.between(root.get(attributeName), LocalDateTime.parse(betweenValues[0], formatterDateTime), LocalDateTime.parse(betweenValues[1], formatterDateTime)));
            } else {
                predicates.add(criteriaBuilder.between(root.get(attributeName), betweenValues[0], betweenValues[1]));
            }
        } else if (value.startsWith("is(null)")) {
            predicates.add(criteriaBuilder.isNull(root.get(attributeName)));
        } else if (value.startsWith("is(!null)")) {
            predicates.add(criteriaBuilder.isNotNull(root.get(attributeName)));
        } else {
            if (fieldType.equals("java.lang.Integer")) {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), Integer.decode(value)));
            } else if (fieldType.equals("java.lang.Double")) {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), Double.parseDouble(value)));
            } else if (fieldType.equals("java.math.BigDecimal")) {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), new BigDecimal(value)));
            } else if (fieldType.equals("java.lang.Boolean")) {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), Boolean.parseBoolean(value))); 
            } else if (fieldType.equals("java.time.LocalDate")) {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), LocalDate.parse(value, formatter))); 
            } else if (fieldType.equals("java.time.LocalDateTime")) {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), LocalDateTime.parse(value, formatterDateTime))); 
            } else {
                predicates.add(criteriaBuilder.equal(root.get(attributeName), value));  
            }
        }    
        return predicates;
    }


}
