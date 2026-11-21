package br.com.emitesimples.admin.repository;

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
import br.com.emitesimples.admin.entity.HistoricoSituacaoEmpresaEntity;
import br.com.emitesimples.tenant.EscopoTenant;
@ApplicationScoped
public class HistoricoSituacaoEmpresaRepository implements PanacheRepositoryBase<HistoricoSituacaoEmpresaEntity, String> {

    @Inject EntityManager em;

    /**
     * Escopo de tenant do acesso (isolamento multi-tenant). Governante desta entidade:
     * <b>empresa</b>, declarado com
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
     * Busca por id <b>dentro do escopo</b>, pela relacao {@code empresa}.
     * Registro de outro tenant nao e encontrado — 404, nunca 403.
     */
    @Override
    public Optional<HistoricoSituacaoEmpresaEntity> findByIdOptional(String id) {
        if (escopo.irrestrito()) {
            return find("id = ?1", id).firstResultOptional();
        }
        return find("id = ?1 and empresa.id = ?2", id, escopo.exigir("empresa_id"))
                .firstResultOptional();
    }

    /** Gemeo escopado de {@code findByIdOptional} — o Panache expoe os dois, e servico usa este. */
    @Override
    public HistoricoSituacaoEmpresaEntity findById(String id) {
        return findByIdOptional(id).orElse(null);
    }

    /** Remocao escopada: id de outro tenant nao e encontrado, logo nao e removido. */
    @Override
    public boolean deleteById(String id) {
        return findByIdOptional(id).map(e -> { delete(e); return true; }).orElse(false);
    }
    /**
     * Resolve a <b>empresa</b> que o registro vai carregar. Fora do modo irrestrito e sempre
     * a da sessao (carrega so a referencia, sem exigir version do cliente); se o corpo
     * trouxer outra, e 403. No modo irrestrito vale a do corpo (null se ausente; o
     * persist a exige).
     */
    public br.com.emitesimples.configuracao.entity.EmpresaEntity empresaEfetiva(String empresaIdInformada) {
        if (!escopo.irrestrito()) {
            String daSessao = escopo.exigir("empresa_id");
            if (empresaIdInformada != null && !empresaIdInformada.equals(daSessao)) {
                throw new jakarta.ws.rs.ForbiddenException("O registro nao pode pertencer a outra empresa.");
            }
            return em.getReference(br.com.emitesimples.configuracao.entity.EmpresaEntity.class, daSessao);
        }
        if (empresaIdInformada == null || empresaIdInformada.isBlank()) {
            return null;
        }
        br.com.emitesimples.configuracao.entity.EmpresaEntity encontrada = em.find(br.com.emitesimples.configuracao.entity.EmpresaEntity.class, empresaIdInformada);
        if (encontrada == null) {
            throw new br.com.emitesimples.exceptions.NotFoundException("No entity Empresa found for Id: " + empresaIdInformada);
        }
        return encontrada;
    }
    /** Grava impondo a empresa da sessao (ver empresaEfetiva); no modo irrestrito ela e obrigatoria. */
    @Override
    public void persist(HistoricoSituacaoEmpresaEntity entity) {
        String informada = entity.getEmpresa() == null ? null : entity.getEmpresa().getId();
        br.com.emitesimples.configuracao.entity.EmpresaEntity efetiva = empresaEfetiva(informada);
        if (efetiva == null) {
            throw new InvalidInputException("O campo empresa e obrigatorio.");
        }
        entity.setEmpresa(efetiva);
        em.persist(entity);
    }
    /** PUT e PATCH nao movem o registro de empresa: outra no corpo e 403 (escopado) ou 400 (irrestrito). Ausente no corpo mantem a atual. */
    public void conferirEmpresaImutavel(HistoricoSituacaoEmpresaEntity entity, String empresaIdInformada) {
        if (empresaIdInformada == null) {
            return;
        }
        String atual = entity.getEmpresa() == null ? null : entity.getEmpresa().getId();
        if (empresaIdInformada.equals(atual)) {
            return;
        }
        if (!escopo.irrestrito()) {
            throw new jakarta.ws.rs.ForbiddenException("A empresa do registro nao pode ser alterada.");
        }
        throw new InvalidInputException("A empresa do registro nao pode ser alterada.");
    }

    public List<HistoricoSituacaoEmpresaEntity> findWithFilters(
        int offset,
        int pageSize,
        Sort sort,
        String situacaoAnterior,
        String situacaoNova,
        String motivo,
        String origem,
        String dataHora,
        String id,
        String dataCriacao,
        String dataAtualizacao,
        String empresaNomeFantasia,
        String empresaRazaoSocial,
        String empresaDocumento,
        String empresaRgInscricaoEstadual,
        String empresaId,
        String responsavelNome,
        String responsavelEmail,
        String responsavelId
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<HistoricoSituacaoEmpresaEntity> cq = cb.createQuery(HistoricoSituacaoEmpresaEntity.class);
        Root<HistoricoSituacaoEmpresaEntity> root = cq.from(HistoricoSituacaoEmpresaEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, situacaoAnterior, situacaoNova, motivo, origem, dataHora, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, responsavelNome, responsavelEmail, responsavelId);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        if (sort != null) {
            List<Order> orders = new ArrayList<>();
            sort.getColumns().forEach(col -> {
                if (col.getDirection() == Sort.Direction.Ascending) orders.add(cb.asc(root.get(col.getName())));
                else orders.add(cb.desc(root.get(col.getName())));
            });
            cq.orderBy(orders);
        }
        TypedQuery<HistoricoSituacaoEmpresaEntity> query = em.createQuery(cq);
        query.setFirstResult(offset);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    public long countWithFilters(
        String situacaoAnterior,
        String situacaoNova,
        String motivo,
        String origem,
        String dataHora,
        String id,
        String dataCriacao,
        String dataAtualizacao,
        String empresaNomeFantasia,
        String empresaRazaoSocial,
        String empresaDocumento,
        String empresaRgInscricaoEstadual,
        String empresaId,
        String responsavelNome,
        String responsavelEmail,
        String responsavelId
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<HistoricoSituacaoEmpresaEntity> root = cq.from(HistoricoSituacaoEmpresaEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, situacaoAnterior, situacaoNova, motivo, origem, dataHora, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, responsavelNome, responsavelEmail, responsavelId);
        cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));

        return em.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<HistoricoSituacaoEmpresaEntity> root,
        String situacaoAnterior,
        String situacaoNova,
        String motivo,
        String origem,
        String dataHora,
        String id,
        String dataCriacao,
        String dataAtualizacao,
        String empresaNomeFantasia,
        String empresaRazaoSocial,
        String empresaDocumento,
        String empresaRgInscricaoEstadual,
        String empresaId,
        String responsavelNome,
        String responsavelEmail,
        String responsavelId
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (!escopo.irrestrito()) {
            predicates.add(cb.equal(root.join("empresa", JoinType.INNER).get("id"), escopo.exigir("empresa_id")));
        }

        if (situacaoAnterior != null) getPredicateFromField(predicates, "situacaoAnterior", situacaoAnterior, root, cb, false);
        if (situacaoNova != null) getPredicateFromField(predicates, "situacaoNova", situacaoNova, root, cb, false);
        if (motivo != null) getPredicateFromField(predicates, "motivo", motivo, root, cb, false);
        if (origem != null) getPredicateFromField(predicates, "origem", origem, root, cb, false);
        if (dataHora != null) getPredicateFromField(predicates, "dataHora", dataHora, root, cb, false);
        if (id != null) getPredicateFromField(predicates, "id", id, root, cb, false);
        if (dataCriacao != null) getPredicateFromField(predicates, "dataCriacao", dataCriacao, root, cb, false);
        if (dataAtualizacao != null) getPredicateFromField(predicates, "dataAtualizacao", dataAtualizacao, root, cb, false);
        if (empresaId != null) {
            predicates.add(cb.equal(root.join("empresa", JoinType.INNER).get("id"), empresaId));
        }
        if (responsavelId != null) {
            predicates.add(cb.equal(root.join("responsavel", JoinType.INNER).get("id"), responsavelId));
        }
        return predicates;
    }

    public List<Predicate> getPredicateFromField(List<Predicate> predicates, String attributeName, String value, Root<HistoricoSituacaoEmpresaEntity> root, CriteriaBuilder criteriaBuilder, boolean unaccent) {
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
