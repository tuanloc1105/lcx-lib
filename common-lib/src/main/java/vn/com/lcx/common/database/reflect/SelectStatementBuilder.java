package vn.com.lcx.common.database.reflect;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import vn.com.lcx.common.annotation.ColumnName;
import vn.com.lcx.common.annotation.SubTable;
import vn.com.lcx.common.annotation.TableName;
import vn.com.lcx.common.constant.CommonConstant;
import vn.com.lcx.common.constant.JavaSqlResultSetConstant;
import vn.com.lcx.common.database.pageable.Direction;
import vn.com.lcx.common.database.pageable.Pageable;
import vn.com.lcx.common.database.type.SubTableEntry;
import vn.com.lcx.common.utils.LogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static vn.com.lcx.common.constant.CommonConstant.BUILDER_MAP;
import static vn.com.lcx.common.database.utils.EntityUtils.getTableShortenedName;
import static vn.com.lcx.common.utils.MyStringUtils.removeSuffixOfString;

@Getter
public class SelectStatementBuilder {

    // private static volatile SelectStatementBuilder INSTANCE;

    private final Class<?> entityClass;
    private final ArrayList<String> listOfColumnName;
    private final ArrayList<Field> listOfField;
    private final String tableName;
    private final String tableNameShortenedName;
    private final ArrayList<SubTableEntry> subTableStatementBuilders;

    private SelectStatementBuilder(Class<?> entityClass, boolean getSelfColumnOnly, int... order) {
        this.entityClass = entityClass;

        val tableNameAnnotation = entityClass.getAnnotation(TableName.class);

        if (tableNameAnnotation == null) {
            throw new IllegalArgumentException(String.format("%s must be annotated with @TableName", entityClass.getName()));
        }

        String tableNameValue = tableNameAnnotation.value();

        if (StringUtils.isNotBlank(tableNameAnnotation.schema())) {
            String schemaName = tableNameAnnotation.schema() + ".";
            if (tableNameValue.contains(".")) {
                val tableNameValueArray = tableNameValue.split(JavaSqlResultSetConstant.DOT);
                this.tableName = schemaName + tableNameValueArray[tableNameValueArray.length - 1];
            } else {
                this.tableName = schemaName + tableNameValue;
            }
        } else {
            this.tableName = tableNameValue;
        }

        this.tableNameShortenedName = getTableShortenedName(tableName) + (order.length > 0 ? order[0] + "" : "");

        this.listOfColumnName = new ArrayList<>();
        this.listOfField = new ArrayList<>();
        this.subTableStatementBuilders = new ArrayList<>();

        val listOfFields = new ArrayList<>(Arrays.asList(entityClass.getDeclaredFields()));

        for (Field field : listOfFields) {
            listOfField.add(field);
            boolean isColumnNameAnnotationExisted = false;
            val columnNameAnnotation = field.getAnnotation(ColumnName.class);
            if (columnNameAnnotation != null) {
                val columnNameAnnotationValue = columnNameAnnotation.name();
                this.listOfColumnName.add(
                        // String.format("%s.%s", this.tableNameShortenedName, columnNameAnnotation.name())
                        String.format(
                                "%s.%s AS %s",
                                this.tableNameShortenedName,
                                columnNameAnnotationValue,
                                String.format("%s_%s", this.tableNameShortenedName.toUpperCase(), columnNameAnnotationValue)
                        )
                );
                isColumnNameAnnotationExisted = true;
            }
            if (getSelfColumnOnly) {
                continue;
            }
            try {
                int order2 = 0;
                val subTableAnnotation = field.getAnnotation(SubTable.class);
                if (subTableAnnotation != null) {
                    final SelectStatementBuilder selectStatementBuilderOfSubTable;
                    final Class<?> clz;
                    if (field.getType().isAssignableFrom(List.class)) {
                        val genericType = (ParameterizedType) field.getGenericType();
                        val type = genericType.getActualTypeArguments()[0];
                        clz = Class.forName(type.getTypeName());
                    } else {
                        clz = field.getType();
                    }
                    if (clz.isAssignableFrom(this.entityClass)) {
                        throw new IllegalArgumentException("1 - 1 relation is currently not supported");
                        // selectStatementBuilderOfSubTable = new SelectStatementBuilder(clz, true, ++order2);
                    } else {
                        // selectStatementBuilderOfSubTable = new SelectStatementBuilder(clz, true);
                        selectStatementBuilderOfSubTable = SelectStatementBuilder.of(clz);
                    }
                    val optionalMatchedField = selectStatementBuilderOfSubTable.getListOfField().stream().filter(f -> f.getName().equals(subTableAnnotation.mapField())).findFirst();
                    if (!optionalMatchedField.isPresent()) {
                        throw new RuntimeException("Cannot find appropriate field of sub class");
                    }
                    val matchedField = optionalMatchedField.get();
                    if (!isColumnNameAnnotationExisted) {
                        this.listOfColumnName.add(
                                String.format(
                                        "%s.%s AS %s",
                                        this.tableNameShortenedName,
                                        subTableAnnotation.columnName(),
                                        String.format("%s_%s", this.tableNameShortenedName.toUpperCase(), subTableAnnotation.columnName())
                                )
                        );
                    }
                    val subTableEntry = SubTableEntry.builder()
                            .field(field)
                            .joinType(subTableAnnotation.joinType())
                            .columnName(subTableAnnotation.columnName())
                            .matchField(matchedField)
                            .selectStatementBuilder(selectStatementBuilderOfSubTable)
                            .build();
                    this.subTableStatementBuilders.add(subTableEntry);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                LogUtils.writeLog(e.getMessage(), e);
            }
        }
    }

    public static SelectStatementBuilder of(Class<?> entityClass) {
        SelectStatementBuilder builderClass = BUILDER_MAP.get(entityClass.getName());
        if (builderClass == null) {
            synchronized (BUILDER_MAP) {
                if (BUILDER_MAP.get(entityClass.getName()) == null) {
                    builderClass = new SelectStatementBuilder(entityClass, false);
                    BUILDER_MAP.put(entityClass.getName(), builderClass);
                }
            }
        }
        return builderClass;
    }

    // public static SelectStatementBuilder getInstance(Class<?> entityClass) {
    //     if (INSTANCE == null) {
    //         synchronized (SelectStatementBuilder.class) {
    //             if (INSTANCE == null) {
    //                 INSTANCE = new SelectStatementBuilder(entityClass);
    //             }
    //         }
    //     }
    //     return INSTANCE;
    // }

    public String build(String methodName, Object... parameters) {
        final String statement;
        if (methodName.startsWith("count")) {
            statement = this.generateCountStatement();
        } else {
            statement = this.generateSqlStatement();
        }
        if (StringUtils.isNotBlank(methodName)) {
            return String.format(
                    "%s\nWHERE\n    %s",
                    statement,
                    this.parseMethodNameIntoConditionStatement(methodName, new ArrayList<>(Arrays.asList(parameters)))
            );
        } else {
            return statement;
        }
    }

    public String buildFullJoin(String methodName, Object... parameters) {
        final String statement;
        if (methodName.startsWith("count")) {
            statement = this.generateCountStatement();
        } else {
            statement = this.generateSqlStatementFullJoin();
        }
        if (StringUtils.isNotBlank(methodName)) {
            return String.format(
                    "%s\nWHERE\n    %s",
                    statement,
                    this.parseMethodNameIntoConditionStatement(methodName, new ArrayList<>(Arrays.asList(parameters)))
            );
        } else {
            return statement;
        }
    }

    private String generateSqlStatement() {
        val tableNameWithShortenedName = String.format("%s %s", this.tableName, this.tableNameShortenedName);
        return String.format(
                "SELECT\n    %s\nFROM\n    %s",
                String.join(",\n    ", this.listOfColumnName),
                //String.join(", ", this.listOfColumnName),
                tableNameWithShortenedName
        );
    }

    private String generateCountStatement() {
        val tableNameWithShortenedName = String.format("%s %s", this.tableName, this.tableNameShortenedName);
        return String.format(
                "SELECT\n    COUNT(1)\nFROM\n    %s",
                tableNameWithShortenedName
        );
    }

    private String generateSqlStatementFullJoin() {
        val tableNameWithShortenedName = String.format("%s %s", this.tableName, this.tableNameShortenedName);
        val listColumn = this.listOfColumnName;
        for (SubTableEntry subTableStatementBuilder : this.subTableStatementBuilders) {
            listColumn.addAll(subTableStatementBuilder.getSelectStatementBuilder().getListOfColumnName());
        }
        return String.format(
                "SELECT\n    %s\nFROM\n    %s%s",
                String.join(",\n    ", listColumn),
                tableNameWithShortenedName,
                subTableJoinStatementBuilder()
        );
    }

    private String subTableJoinStatementBuilder() {
        final List<String> result = new ArrayList<>();
        for (SubTableEntry subTableStatementBuilder : this.subTableStatementBuilders) {
            val tableNameWithShortenedName = String.format("%s %s", subTableStatementBuilder.getSelectStatementBuilder().getTableName(), subTableStatementBuilder.getSelectStatementBuilder().getTableNameShortenedName());
            result.add(
                    String.format(
                            "%1$s %2$s ON %3$s.%4$s = %5$s.%6$s",
                            subTableStatementBuilder.getJoinType().getStatement(),
                            tableNameWithShortenedName,
                            subTableStatementBuilder.getSelectStatementBuilder().getTableNameShortenedName(),
                            subTableStatementBuilder.getMatchField().getAnnotation(ColumnName.class).name(),
                            this.tableNameShortenedName,
                            subTableStatementBuilder.getColumnName()
                    )
            );
        }
        return result.isEmpty() ? CommonConstant.EMPTY_STRING : "\n    " + String.join("\n    ", result);
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    private String parseMethodNameIntoConditionStatement(String methodName, ArrayList<Object> parameters) {
        if (
                !(methodName.startsWith("findBy")) &&
                        !(methodName.startsWith(String.format("find%sBy", this.entityClass.getSimpleName()))) &&
                        !(methodName.startsWith("countBy")) &&
                        !(methodName.startsWith(String.format("count%sBy", this.entityClass.getSimpleName())))
        ) {
            throw new IllegalArgumentException(String.format("%s must start with `findBy`", methodName));
        }
        // if (parameters.isEmpty()) {
        //     throw new IllegalArgumentException(String.format("%s must contain at least one parameter", methodName));
        // }
        final ArrayList<String> partsOfMethod;
        if (methodName.startsWith("find")) {
            partsOfMethod = new ArrayList<>(
                    Arrays.asList(
                            methodName.startsWith("findBy") ?
                                    methodName.substring(6).split("(?=And|OrderBy|Or)|(?<=And|OrderBy|Or)(?=[A-Z])") :
                                    methodName.substring(6 + this.entityClass.getSimpleName().length()).split("(?=And|OrderBy|Or)|(?<=And|OrderBy|Or)(?=[A-Z])")
                    )
            );
        } else {
            partsOfMethod = new ArrayList<>(
                    Arrays.asList(
                            methodName.startsWith("countBy") ?
                                    methodName.substring(7).split("(?=And|OrderBy|Or)|(?<=And|OrderBy|Or)(?=[A-Z])") :
                                    methodName.substring(7 + this.entityClass.getSimpleName().length()).split("(?=And|OrderBy|Or)|(?<=And|OrderBy|Or)(?=[A-Z])")
                    )
            );
        }

        val conditionSQLStatement = new ArrayList<String>();
        val listOfOrderByStatement = new ArrayList<String>();
        val handledParameterIndexThatIsAList = new HashSet<Integer>();

        boolean meetOrderByStatement = false;

        for (String part : partsOfMethod) {

            if (!meetOrderByStatement) {

                if ("or".equalsIgnoreCase(part)) {
                    conditionSQLStatement.add("\n    OR ");
                } else if ("and".equalsIgnoreCase(part)) {
                    conditionSQLStatement.add("\n    AND ");
                } else if ("orderby".equalsIgnoreCase(part)) {
                    meetOrderByStatement = true;
                } else {
                    val currentFieldParts = new ArrayList<String>(
                            Arrays.asList(
                                    part.split(
                                            "(?=In|Like|Not|Between|LessThan|GreaterThan|LessEqual|GreaterEqual|Null|Nonull)|" +
                                                    "(?<=In|Like|Not|Between|LessThan|GreaterThan|LessEqual|GreaterEqual|Null|Nonull)"
                                    )
                            )
                    );
                    final Optional<String> columnNameOptional;
                    final String columnName, tblShortName;

                    if (currentFieldParts.get(0).contains("_")) {
                        val currentFieldParts2 = new ArrayList<String>(Arrays.asList(currentFieldParts.get(0).split("_")));
                        SelectStatementBuilder subTableClass = this;
                        for (int i = 0; i < currentFieldParts2.size() - 1; i++) {
                            int finalI = i;
                            val optionalSubTableFieldDataType = subTableClass.getListOfField().stream().filter(f -> f.getName().equalsIgnoreCase(currentFieldParts2.get(finalI))).findFirst();
                            if (!optionalSubTableFieldDataType.isPresent()) {
                                throw new IllegalArgumentException("Unknown field");
                            }
                            subTableClass = SelectStatementBuilder.of(optionalSubTableFieldDataType.get().getType());
                        }
                        val optionalSubTableFieldDataType = subTableClass.getListOfField().stream().filter(f -> f.getName().equalsIgnoreCase(currentFieldParts2.get(currentFieldParts2.size() - 1))).findFirst();
                        if (!optionalSubTableFieldDataType.isPresent()) {
                            throw new IllegalArgumentException("Unknown field");
                        }
                        columnNameOptional = subTableClass.getListOfField()
                                .stream()
                                .filter(f -> f.getName().equalsIgnoreCase(currentFieldParts2.get(1)))
                                .map(field -> field.getAnnotation(ColumnName.class).name())
                                .findFirst();
                        tblShortName = subTableClass.getTableNameShortenedName();

                    } else {
                        columnNameOptional = this.listOfField
                                .stream()
                                .filter(
                                        field ->
                                                field.getName().equalsIgnoreCase(currentFieldParts.get(0)) &&
                                                        field.getAnnotation(ColumnName.class) != null
                                )
                                .findAny()
                                .map(field -> field.getAnnotation(ColumnName.class).name());
                        tblShortName = this.tableNameShortenedName;
                    }

                    if (!columnNameOptional.isPresent()) {
                        throw new RuntimeException(
                                String.format(
                                        "Method part name %s can not found the matching column",
                                        currentFieldParts.get(0)
                                )
                        );
                    }

                    columnName = String.format("%s.%s", tblShortName, columnNameOptional.get());
                    if (currentFieldParts.size() == 2) {
                        switch (currentFieldParts.get(1).toLowerCase()) {
                            case "in":
                                // val parameterWithListDataType = (List<?>) parameters.stream().filter(o -> o instanceof List<?>).findFirst().orElse(null);
                                List<?> parameterWithListDataType = null;
                                for (int i = 0; i < parameters.size(); i++) {

                                    if (handledParameterIndexThatIsAList.contains(i)) {
                                        continue;
                                    }

                                    if (parameters.get(i) instanceof List<?>) {
                                        parameterWithListDataType = (List<?>) parameters.get(i);
                                        handledParameterIndexThatIsAList.add(i);
                                    }
                                }
                                if (parameterWithListDataType == null) {
                                    throw new RuntimeException("In condition statement must contain at least 1 collection parameter");
                                }
                                conditionSQLStatement.add(
                                        String.format(
                                                "%s IN (%s)",
                                                columnName,
                                                parameterWithListDataType.stream()
                                                        .map(a -> "?")
                                                        .collect(Collectors.joining(", "))
                                        )
                                );
                                break;
                            case "like":
                                conditionSQLStatement.add(columnName + " LIKE '%' || ? || '%'");
                                break;
                            case "not":
                                conditionSQLStatement.add(String.format("%s <> ?", columnName));
                                break;
                            case "between":
                                conditionSQLStatement.add(String.format("%s BETWEEN ? AND ?", columnName));
                                break;
                            case "lessthan":
                                conditionSQLStatement.add(String.format("%s BETWEEN < ?", columnName));
                                break;
                            case "greaterthan":
                                conditionSQLStatement.add(String.format("%s BETWEEN > ?", columnName));
                                break;
                            case "lessequal":
                                conditionSQLStatement.add(String.format("%s BETWEEN <= ?", columnName));
                                break;
                            case "greaterequal":
                                conditionSQLStatement.add(String.format("%s BETWEEN >= ?", columnName));
                                break;
                            case "null":
                                conditionSQLStatement.add(String.format("%s IS NULL", columnName));
                                break;
                            case "nonull":
                                conditionSQLStatement.add(String.format("%s IS NOT NULL", columnName));
                                break;
                            default:
                                conditionSQLStatement.add(String.format("%s = ?", columnName));
                                break;
                        }
                    } else {
                        conditionSQLStatement.add(String.format("%s = ?", columnName));
                    }

                }
            } else {

                if ("or".equalsIgnoreCase(part) || "and".equalsIgnoreCase(part)) {
                    continue;
                }

                val direction = part.toLowerCase().endsWith("desc") ? Direction.DESC : Direction.ASC;
                val columnNameOptional = this.listOfField
                        .stream()
                        .filter(
                                field ->
                                        (
                                                field.getName().equalsIgnoreCase(removeSuffixOfString(part.toLowerCase(), "desc")) ||
                                                        field.getName().equalsIgnoreCase(removeSuffixOfString(part.toLowerCase(), "asc"))
                                        ) &&
                                                field.getAnnotation(ColumnName.class) != null
                        )
                        .findAny()
                        .map(field -> field.getAnnotation(ColumnName.class).name());
                if (!columnNameOptional.isPresent()) {
                    throw new RuntimeException(
                            String.format(
                                    "Method part name %s can not found the matching column",
                                    part
                            )
                    );
                }
                val columnName = String.format("%s.%s", this.tableNameShortenedName, columnNameOptional.get());
                listOfOrderByStatement.add(
                        String.format(
                                "%s %s",
                                columnName,
                                direction.name()
                        )
                );
            }
        }

        if (methodName.startsWith("count")) {
            return String.join("", conditionSQLStatement);
        }
        String format;
        if (!listOfOrderByStatement.isEmpty()) {
            format = String.format(
                    "%s\nORDER BY %s",
                    String.join("", conditionSQLStatement),
                    String.join(", ", listOfOrderByStatement)
            );
        } else {
            format = String.join("", conditionSQLStatement);
        }
        if (parameters.get(parameters.size() - 1) instanceof Pageable && !(methodName.startsWith("count"))) {
            @SuppressWarnings("PatternVariableCanBeUsed") val page = (Pageable) parameters.get(parameters.size() - 1);
            if (listOfOrderByStatement.isEmpty()) {
                page.setEntityClass(this.entityClass);
                page.fieldToColumn();
                return String.join("", conditionSQLStatement) + "\n" + page.toSql().replaceFirst(" ", "");
            } else {
                page.setColumnNameAndDirectionMap(null);
                return format + page.toSql().replaceFirst(" ", "");
            }
        }
        return format;
    }

}
