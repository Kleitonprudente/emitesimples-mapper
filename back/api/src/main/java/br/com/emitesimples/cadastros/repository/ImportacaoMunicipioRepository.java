package br.com.emitesimples.cadastros.repository;

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
import br.com.emitesimples.cadastros.entity.ImportacaoMunicipioEntity;
@ApplicationScoped
public class ImportacaoMunicipioRepository implements PanacheRepositoryBase<ImportacaoMunicipioEntity, String> {

    @Inject EntityManager em;
    /**
     * Descarrega as gravacoes pendentes <b>dentro</b> do try do resource: a violacao
     * estoura aqui e nao no commit (onde viraria 500 e vazaria SQL). Unicidade
     * (SQLState 23505) e conflito de versao viram 409; as demais violacoes viram 400,
     * sem expor o SQL. O 409 de unicidade leva a causa (nunca exposta) para o tratador de erros
     * poder dizer QUAL indice unico foi violado (mensagem no campo; ver ErrosHandler).
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
                return new ClientErrorException("Ja existe um registro com os mesmos dados unicos.", 409, pe);
            }
        }
        return new InvalidInputException("Nao foi possivel gravar o registro: dados invalidos ou em conflito com uma restricao.", pe.getCause());
    }

    public List<ImportacaoMunicipioEntity> findWithFilters(
        int offset,
        int pageSize,
        Sort sort,
        String nomeArquivo,
        String codificacaoDetectada,
        String separadorDetectado,
        String estado,
        String dataHoraInicio,
        String dataHoraFim,
        String linhasLidas,
        String linhasInseridas,
        String linhasAtualizadas,
        String linhasIguais,
        String linhasRejeitadas,
        String resumoRejeicoes,
        String id,
        String dataCriacao,
        String dataAtualizacao,
        String idUsuario
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ImportacaoMunicipioEntity> cq = cb.createQuery(ImportacaoMunicipioEntity.class);
        Root<ImportacaoMunicipioEntity> root = cq.from(ImportacaoMunicipioEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, nomeArquivo, codificacaoDetectada, separadorDetectado, estado, dataHoraInicio, dataHoraFim, linhasLidas, linhasInseridas, linhasAtualizadas, linhasIguais, linhasRejeitadas, resumoRejeicoes, id, dataCriacao, dataAtualizacao, idUsuario);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        if (sort != null) {
            List<Order> orders = new ArrayList<>();
            sort.getColumns().forEach(col -> {
                if (col.getDirection() == Sort.Direction.Ascending) orders.add(cb.asc(root.get(col.getName())));
                else orders.add(cb.desc(root.get(col.getName())));
            });
            cq.orderBy(orders);
        }
        TypedQuery<ImportacaoMunicipioEntity> query = em.createQuery(cq);
        query.setFirstResult(offset);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    public long countWithFilters(
        String nomeArquivo,
        String codificacaoDetectada,
        String separadorDetectado,
        String estado,
        String dataHoraInicio,
        String dataHoraFim,
        String linhasLidas,
        String linhasInseridas,
        String linhasAtualizadas,
        String linhasIguais,
        String linhasRejeitadas,
        String resumoRejeicoes,
        String id,
        String dataCriacao,
        String dataAtualizacao,
        String idUsuario
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ImportacaoMunicipioEntity> root = cq.from(ImportacaoMunicipioEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, nomeArquivo, codificacaoDetectada, separadorDetectado, estado, dataHoraInicio, dataHoraFim, linhasLidas, linhasInseridas, linhasAtualizadas, linhasIguais, linhasRejeitadas, resumoRejeicoes, id, dataCriacao, dataAtualizacao, idUsuario);
        cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));

        return em.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<ImportacaoMunicipioEntity> root,
        String nomeArquivo,
        String codificacaoDetectada,
        String separadorDetectado,
        String estado,
        String dataHoraInicio,
        String dataHoraFim,
        String linhasLidas,
        String linhasInseridas,
        String linhasAtualizadas,
        String linhasIguais,
        String linhasRejeitadas,
        String resumoRejeicoes,
        String id,
        String dataCriacao,
        String dataAtualizacao,
        String idUsuario
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (nomeArquivo != null) getPredicateFromField(predicates, "nomeArquivo", nomeArquivo, root, cb, false);
        if (codificacaoDetectada != null) getPredicateFromField(predicates, "codificacaoDetectada", codificacaoDetectada, root, cb, false);
        if (separadorDetectado != null) getPredicateFromField(predicates, "separadorDetectado", separadorDetectado, root, cb, false);
        if (estado != null) getPredicateFromField(predicates, "estado", estado, root, cb, false);
        if (dataHoraInicio != null) getPredicateFromField(predicates, "dataHoraInicio", dataHoraInicio, root, cb, false);
        if (dataHoraFim != null) getPredicateFromField(predicates, "dataHoraFim", dataHoraFim, root, cb, false);
        if (linhasLidas != null) getPredicateFromField(predicates, "linhasLidas", linhasLidas, root, cb, false);
        if (linhasInseridas != null) getPredicateFromField(predicates, "linhasInseridas", linhasInseridas, root, cb, false);
        if (linhasAtualizadas != null) getPredicateFromField(predicates, "linhasAtualizadas", linhasAtualizadas, root, cb, false);
        if (linhasIguais != null) getPredicateFromField(predicates, "linhasIguais", linhasIguais, root, cb, false);
        if (linhasRejeitadas != null) getPredicateFromField(predicates, "linhasRejeitadas", linhasRejeitadas, root, cb, false);
        if (resumoRejeicoes != null) getPredicateFromField(predicates, "resumoRejeicoes", resumoRejeicoes, root, cb, false);
        if (id != null) getPredicateFromField(predicates, "id", id, root, cb, false);
        if (dataCriacao != null) getPredicateFromField(predicates, "dataCriacao", dataCriacao, root, cb, false);
        if (dataAtualizacao != null) getPredicateFromField(predicates, "dataAtualizacao", dataAtualizacao, root, cb, false);
        if (idUsuario != null) getPredicateFromField(predicates, "idUsuario", idUsuario, root, cb, false);
        return predicates;
    }

    public List<Predicate> getPredicateFromField(List<Predicate> predicates, String attributeName, String value, Root<ImportacaoMunicipioEntity> root, CriteriaBuilder criteriaBuilder, boolean unaccent) {
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
