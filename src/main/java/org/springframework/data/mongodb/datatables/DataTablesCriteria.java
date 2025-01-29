package org.springframework.data.mongodb.datatables;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

public final class DataTablesCriteria {

    private final DataTablesInput input;
    private final Collection<Criteria> additionalCriteria;
    private final Collection<Criteria> preFilteringCriteria;

    DataTablesCriteria(DataTablesInput input, Collection<Criteria> preFilteringCriteria, Collection<Criteria> additionalCriteria) {
        this.input = input;
        this.additionalCriteria = additionalCriteria;
        this.preFilteringCriteria = preFilteringCriteria;
    }

    Query toQuery() {
        Query query = this.toCountQuery();
        addSort(query, input);
        return query;
    }

    Query toCountQuery() {
        Query query = new Query();

        addGlobalCriteria(query, input);
        input.getColumns().forEach(column -> this.addColumnCriteria(query, column));

        if (additionalCriteria != null) {
            for (Criteria criteria : additionalCriteria) {
                if (criteria != null) {
                    query.addCriteria(criteria);
                }
            }
        }
        if (preFilteringCriteria != null) {
            for (Criteria criteria : preFilteringCriteria) {
                if (criteria != null) {
                    query.addCriteria(criteria);
                }
            }
        }

        return query;
    }

    public static Criteria[] getGlobalCriteria(DataTablesInput input) {
        if (!hasText(input.getSearch().getValue())) return new Criteria[]{};

        return input.getColumns().stream()
                .filter(DataTablesInput.Column::isSearchable)
                .map(column -> createCriteria(column, input.getSearch()))
                .toArray(Criteria[]::new);
    }

    private void addGlobalCriteria(Query query, DataTablesInput input) {
        Criteria[] criteriaArray = getGlobalCriteria(input);

        if (criteriaArray.length == 1) {
            query.addCriteria(criteriaArray[0]);
        } else if (criteriaArray.length >= 2) {
            query.addCriteria(new Criteria().orOperator(criteriaArray));
        }
    }

    public static Criteria getColumnCriteria(DataTablesInput.Column column) {
        if ((column.isSearchable() || column.isSearchableIndependently()) && hasText(column.getSearch().getValue())) {
            return createColumnCriteria(column);
        }
        return null;
    }

    private void addColumnCriteria(Query query, DataTablesInput.Column column) {
        Criteria columnCriteria = getColumnCriteria(column);
        if (columnCriteria != null) {
            query.addCriteria(columnCriteria);
        }
    }

    private static Criteria createColumnCriteria(DataTablesInput.Column column) {
        String searchValue = column.getSearch().getValue();
        if ("true".equalsIgnoreCase(searchValue) || "false".equalsIgnoreCase(searchValue)) {
            return where(column.getData()).is(Boolean.valueOf(searchValue));
        } else {
            return createCriteria(column, column.getSearch());
        }
    }

    private static Criteria createCriteria(DataTablesInput.Column column, DataTablesInput.Search search) {
        String searchValue = search.getValue();
        if (search.isRegex()) {
            return where(column.getData()).regex(searchValue, "i");
        } else {
            return where(column.getData()).regex("^"+searchValue.trim());   // can use index!
        }
    }

    private void addSort(Query query, DataTablesInput input) {
        query.skip(input.getStart());
        query.limit(input.getLength());

        if (isEmpty(input.getOrder())) return;

        List<Sort.Order> orders = input.getOrder().stream()
                .filter(order -> isOrderable(input, order))
                .map(order -> toOrder(input, order)).collect(toList());
        query.with(by(orders));
    }

    private boolean isOrderable(DataTablesInput input, DataTablesInput.Order order) {
        boolean isWithinBounds = order.getColumn() < input.getColumns().size();
        return isWithinBounds && input.getColumns().get(order.getColumn()).isOrderable();
    }

    private Sort.Order toOrder(DataTablesInput input, DataTablesInput.Order order) {
        return new Sort.Order(
                order.getDir() == DataTablesInput.Order.Direction.asc ? Sort.Direction.ASC : Sort.Direction.DESC,
                input.getColumns().get(order.getColumn()).getData()
        );
    }
}