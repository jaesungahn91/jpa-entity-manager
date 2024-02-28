package persistence.sql.dml;

import jakarta.persistence.GenerationType;
import persistence.sql.QueryBuilder;
import persistence.sql.ddl.domain.Column;
import persistence.sql.ddl.domain.Columns;
import persistence.sql.ddl.domain.Table;
import persistence.sql.dml.domain.Value;
import persistence.sql.dml.domain.Values;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InsertQueryBuilder implements QueryBuilder {

    private static final String INSERT_QUERY = "INSERT INTO %s (%s) VALUES (%s);";

    private final Columns columns;
    private final Table table;
    private final Values values;

    public InsertQueryBuilder(Object object) {
        Class<?> clazz = object.getClass();
        this.table = new Table(clazz);
        this.columns = new Columns(createColumns(clazz));
        this.values = new Values(createValues(object, clazz));
    }

    private List<Column> createColumns(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(this::isNotTransientAnnotationPresent)
                .filter(this::isNotPrimaryKeyAndAutoGenerated)
                .map(Column::new)
                .collect(Collectors.toList());
    }

    private boolean isNotPrimaryKeyAndAutoGenerated(Field field) {
        return !field.isAnnotationPresent(jakarta.persistence.GeneratedValue.class) || field.getAnnotation(jakarta.persistence.GeneratedValue.class).strategy().equals(GenerationType.SEQUENCE);
    }

    private List<Value> createValues(Object object, Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(this::isNotTransientAnnotationPresent)
                .filter(this::isNotPrimaryKeyAndAutoGenerated)
                .map(field -> new Value(new Column(field), object))
                .collect(Collectors.toList());
    }

    @Override
    public String build() {
        return String.format(
                INSERT_QUERY,
                table.getName(),
                generateColumns(),
                generateValues()
        );
    }

    private String generateColumns() {
        return columns.getColumns().stream()
                .map(Column::getName)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(COMMA));
    }

    private String generateValues() {
        return values.getValues().stream()
                .map(Value::getValue)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(COMMA));
    }
}