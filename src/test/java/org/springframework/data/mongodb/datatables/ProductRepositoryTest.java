package org.springframework.data.mongodb.datatables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Before
    public void init() {
        productRepository.deleteAll();
        productRepository.save(Product.PRODUCT1);
        productRepository.save(Product.PRODUCT2);
        productRepository.save(Product.PRODUCT3);
    }

    private DataTablesInput getDefaultInput() {
        DataTablesInput input = new DataTablesInput();
        input.setColumns(asList(
                createColumn("id", true, true),
                createColumn("label", true, true),
                createColumn("isEnabled", true, true),
                createColumn("createdAt", true, true),
                createColumn("characteristics.key", true, true),
                createColumn("characteristics.value", true, true),
                createColumn("unknown", false, false)
        ));
        input.setSearch(new DataTablesInput.Search("", false));
        return input;
    }

    private DataTablesInput.Column createColumn(String columnName, boolean orderable, boolean searchable) {
        DataTablesInput.Column column = new DataTablesInput.Column();
        column.setData(columnName);
        column.setOrderable(orderable);
        column.setSearchable(searchable);
        column.setSearch(new DataTablesInput.Search("", true));
        return column;
    }

    @Test
    public void basic() {
        DataTablesOutput<Product> output = productRepository.findAll(getDefaultInput());
        assertThat(output.getDraw()).isEqualTo(1);
        assertThat(output.getError()).isNull();
        assertThat(output.isLastPage()).isTrue();
        assertThat(output.getRecordsTotal()).isEqualTo(3);
        assertThat(output.getData()).containsOnly(Product.PRODUCT1, Product.PRODUCT2, Product.PRODUCT3);
    }

    @Test
    public void paginated() {
        DataTablesInput input = getDefaultInput();
        input.setDraw(2);
        input.setLength(1);
        input.setStart(1);

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getDraw()).isEqualTo(2);
        assertThat(output.getError()).isNull();
        assertThat(output.isLastPage()).isFalse();
        assertThat(output.getRecordsTotal()).isEqualTo(3);
        assertThat(output.getData()).containsOnly(Product.PRODUCT2);
    }

    @Test
    public void sortAscending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(3, DataTablesInput.Order.Direction.asc)));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsSequence(Product.PRODUCT3, Product.PRODUCT1, Product.PRODUCT2);
    }

    @Test
    public void sortDescending() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(3, DataTablesInput.Order.Direction.desc)));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsSequence(Product.PRODUCT2, Product.PRODUCT1, Product.PRODUCT3);
    }

    @Test
    public void globalFilter() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search(" PROduct2  ", false));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).isEmpty();

        input.setSearch(new DataTablesInput.Search(" product2  ", false));

        output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT2);

    }

    @Test
    public void globalFilterRegex() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("^p\\w+uct2$", true));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT2);

        input.setSearch(new DataTablesInput.Search("^P\\w+ucT2$", true));

        output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT2);

    }

    @Test
    public void columnFilter() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("label").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search(" PROduct3  ", false)));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).isEmpty();

        input.getColumn("label").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search(" product3  ", false)));

        output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT3);
    }

    @Test
    public void columnFilterRegex() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("label").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("^p\\w+uct3$", true)));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT3);

        input.getColumn("label").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("^P\\w+Uct3$", true)));

        output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT3);

    }

    @Test
    public void booleanAttribute() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("isEnabled").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("true", false)));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT1, Product.PRODUCT2);
    }

    @Test
    public void empty() {
        DataTablesInput input = getDefaultInput();
        input.setLength(0);

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.isLastPage()).isFalse();
        assertThat(output.getData()).hasSize(0);
    }

    @Test
    public void all() {
        DataTablesInput input = getDefaultInput();
        input.setLength(-1);

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.isLastPage()).isTrue();
        assertThat(output.getRecordsTotal()).isEqualTo(3);
    }

    @Test
    public void subDocument() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("characteristics.key").ifPresent(column ->
                column.setSearch(new DataTablesInput.Search("key1", false)));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT1, Product.PRODUCT2);
    }

    @Test
    public void converter() {
        DataTablesOutput<String> output = productRepository.findAll(getDefaultInput(), Product::getLabel);

        assertThat(output.getData()).containsOnly("product1", "product2", "product3");
    }

    @Test
    public void additionalCriteria() {
        Criteria criteria = where("label").in("product1", "product2");

        DataTablesOutput<Product> output = productRepository.findAll(getDefaultInput(), criteria);
        assertThat(output.isLastPage()).isTrue();
        assertThat(output.getRecordsTotal()).isEqualTo(3);
        assertThat(output.getData()).containsOnly(Product.PRODUCT1, Product.PRODUCT2);
    }

    @Test
    public void preFilteringCriteria() {
        Criteria criteria = where("label").in("product2", "product3");

        DataTablesOutput<Product> output = productRepository.findAll(getDefaultInput(), null, criteria);
        assertThat(output.isLastPage()).isTrue();
        assertThat(output.getRecordsTotal()).isEqualTo(2);
        assertThat(output.getData()).containsOnly(Product.PRODUCT2, Product.PRODUCT3);
    }

    @Test
    public void columnNotSearchable() {
        DataTablesInput input = getDefaultInput();
        input.getColumn("label").ifPresent(column -> {
            column.setSearch(new DataTablesInput.Search(" PROduct3  ", false));
            column.setSearchable(false);
        });

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsOnly(Product.PRODUCT1, Product.PRODUCT2, Product.PRODUCT3);
    }

    @Test
    public void columnNotOrderable() {
        DataTablesInput input = getDefaultInput();
        input.setOrder(singletonList(new DataTablesInput.Order(3, DataTablesInput.Order.Direction.asc)));
        input.getColumn("createdAt").ifPresent(column ->
                column.setOrderable(false));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getData()).containsSequence(Product.PRODUCT1, Product.PRODUCT2, Product.PRODUCT3);
    }

    @Test
    public void multipleCriteria() {
        DataTablesInput input = getDefaultInput();
        input.setSearch(new DataTablesInput.Search("product3", false));

        DataTablesOutput<Product> output = productRepository.findAll(input);
        assertThat(output.getError()).isNull();
        assertThat(output.getData()).containsOnly(Product.PRODUCT3);

        Criteria idCriteria = Criteria.where("_id").is(3);
        Criteria labelCriteria = Criteria.where("label").ne("ahoj");
        Criteria greetingCriteria = Criteria.where("greeting").exists(false);

        // this doesn't work - duplicate 'null' key
        output = productRepository.findAll(input, new Criteria().andOperator(idCriteria, labelCriteria, greetingCriteria));
        assertThat(output.getError()).isNotNull();
        assertThat(output.getData()).isEmpty();

        // this works
        output = productRepository.findAll(input, null, Arrays.asList(idCriteria, labelCriteria, greetingCriteria));
        assertThat(output.getError()).isNull();
        assertThat(output.getData()).containsOnly(Product.PRODUCT3);

        output = productRepository.findAll(input, Arrays.asList(idCriteria, labelCriteria, greetingCriteria), null);
        assertThat(output.getError()).isNull();
        assertThat(output.getData()).containsOnly(Product.PRODUCT3);
    }
}