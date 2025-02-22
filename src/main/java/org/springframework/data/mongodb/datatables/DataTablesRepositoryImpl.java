package org.springframework.data.mongodb.datatables;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class DataTablesRepositoryImpl<T, ID extends Serializable> extends SimpleMongoRepository<T, ID>
        implements DataTablesRepository<T, ID> {

    private final MongoEntityInformation<T, ID> metadata;
    private final MongoOperations mongoOperations;

    /**
     * Creates a new {@link SimpleMongoRepository} for the given {@link MongoEntityInformation} and {@link MongoTemplate}.
     *
     * @param metadata        must not be {@literal null}.
     * @param mongoOperations must not be {@literal null}.
     */
    public DataTablesRepositoryImpl(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
        super(metadata, mongoOperations);
        this.metadata = metadata;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public DataTablesOutput<T> findAll(DataTablesInput input) {
        return findAll(input, emptyList(), emptyList(), null);
    }

    @Override
    public DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria) {
        return findAll(input, additionalCriteria, null, null);
    }

    @Override
    public DataTablesOutput<T> findAll(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria) {
        return findAll(input, additionalCriteria, preFilteringCriteria, null);
    }

    @Override
    public DataTablesOutput<T> findAll(DataTablesInput input, Collection<Criteria> additionalCriteria, Collection<Criteria> preFilteringCriteria) {
        return findAll(input, additionalCriteria, preFilteringCriteria, null);
    }

    @Override
    public <R> DataTablesOutput<R> findAll(DataTablesInput input, Function<T, R> converter) {
        return findAll(input, emptyList(), emptyList(), converter);
    }

    @Override
    public <R> DataTablesOutput<R> findAll(DataTablesInput input, Criteria additionalCriteria, Criteria preFilteringCriteria, Function<T, R> converter) {
        List<Criteria> additionalCriteriaList = additionalCriteria == null ? emptyList() : singletonList(additionalCriteria);
        List<Criteria> preFilteringCriteriaList = preFilteringCriteria == null ? emptyList() : singletonList(preFilteringCriteria);
        return findAll(input, additionalCriteriaList, preFilteringCriteriaList, converter);
    }

    private <R> DataTablesOutput<R> findAll(DataTablesInput input, Collection<Criteria> additionalCriteria, Collection<Criteria> preFilteringCriteria, Function<T, R> converter) {
        DataTablesOutput<R> output = new DataTablesOutput<>();
        output.setDraw(input.getDraw());
        if (input.getLength() == 0) {
            return output;
        }

        try {
            int inputLength = input.getLength();
            if (inputLength > -1) {
                input.setLength(inputLength + 1);
            }

            DataTablesCriteria criteria = new DataTablesCriteria(input, preFilteringCriteria, additionalCriteria);

            if (!input.isCountingRecordsDisabled()) {
                long recordsTotal = count(preFilteringCriteria);
                output.setRecordsTotal(recordsTotal);
                if (recordsTotal == 0) {
                    return output;
                }
                long recordsFiltered = mongoOperations.count(criteria.toCountQuery(), metadata.getCollectionName());
                output.setRecordsFiltered(recordsFiltered);
                if (recordsFiltered == 0) {
                    return output;
                }
            }

            List<T> data = mongoOperations.find(criteria.toQuery(), metadata.getJavaType(), metadata.getCollectionName());

            if (inputLength > -1) {
                if (data.size() == inputLength + 1) {
                    output.setHasNext(true);
                    data.remove(inputLength);
                } else {
                    output.setHasNext(false);
                }
            } else {
                output.setHasNext(false);
            }

            output.setData(converter == null ? (List<R>) data : data.stream().map(converter).collect(toList()));

        } catch (Exception e) {
            output.setError(e.toString());
        }

        return output;
    }

    private long count(Collection<Criteria> preFilteringCriteria) {
        if (preFilteringCriteria == null || preFilteringCriteria.isEmpty() || preFilteringCriteria.stream().allMatch(Objects::isNull)) {
            return mongoOperations.estimatedCount(metadata.getCollectionName());
        } else {
            Query preFilteringQuery = new Query();
            for (Criteria criteria : preFilteringCriteria) {
                if (criteria != null) {
                    preFilteringQuery.addCriteria(criteria);
                }
            }

            return mongoOperations.count(preFilteringQuery, metadata.getCollectionName());
        }
    }

}
