package vn.com.lcx.common.database.utils;

import lombok.val;
import lombok.var;
import org.apache.commons.lang3.StringUtils;
import vn.com.lcx.common.annotation.ColumnName;
import vn.com.lcx.common.annotation.IdColumn;
import vn.com.lcx.common.annotation.TableName;
import vn.com.lcx.common.constant.Constant;
import vn.com.lcx.common.constant.JavaSqlResultSetConstant;
import vn.com.lcx.common.scanner.PackageScanner;
import vn.com.lcx.common.utils.DateTimeUtils;
import vn.com.lcx.common.utils.FileUtils;
import vn.com.lcx.common.utils.MyStringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static vn.com.lcx.common.database.utils.DBEntityAnalysis.CREATE_TABLE_TEMPLATE_NO_PRIMARY_KEY;
import static vn.com.lcx.common.database.utils.DBEntityAnalysis.MSSQL_DATATYPE_MAP;
import static vn.com.lcx.common.database.utils.DBEntityAnalysis.MYSQL_DATATYPE_MAP;
import static vn.com.lcx.common.database.utils.DBEntityAnalysis.ORACLE_DATATYPE_MAP;
import static vn.com.lcx.common.database.utils.DBEntityAnalysis.POSTGRESQL_DATATYPE_MAP;
import static vn.com.lcx.common.utils.FileUtils.createFolderIfNotExists;
import static vn.com.lcx.common.utils.FileUtils.writeContentToFile;
import static vn.com.lcx.common.utils.WordCaseUtils.convertCamelToConstant;

public final class EntityUtils {

    private EntityUtils() {
    }

    public static String getColumnNameFromFieldName(String fieldName, Class<?> entityClass) {
        TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
        if (tableNameAnnotation == null) {
            throw new IllegalArgumentException(entityClass.getName() + " must has @TableName annotation");
        }
        List<Field> entityClassFields = new ArrayList<>(Arrays.asList(entityClass.getDeclaredFields()));
        for (Field field : entityClassFields) {
            if (field.getName().equals(fieldName)) {
                ColumnName columnNameAnnotation = field.getAnnotation(ColumnName.class);
                if (columnNameAnnotation != null && StringUtils.isNotBlank(columnNameAnnotation.name())) {
                    return columnNameAnnotation.name();
                } else {
                    return convertCamelToConstant(field.getName());
                }
            }
        }
        return null;
    }

    public static String getTableShortenedName(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("table name is empty");
        }
        if (tableName.contains(" ")) {
            throw new IllegalArgumentException("table name contains spaces");
        }
        val finalTableName = (tableName.contains(".") ? tableName.substring(tableName.lastIndexOf(".") + 1) : tableName).toLowerCase();
        val firstCharArr = new ArrayList<String>();

        val tableNameArr = Arrays.asList(finalTableName.split("_"));
        tableNameArr.forEach(word -> firstCharArr.add(String.valueOf(word.charAt(0))));
        return String.join("", firstCharArr);
    }

    public static String getTableShortenedName(Class<?> entityClass) {
        val tableNameAnnotation = entityClass.getAnnotation(TableName.class);

        if (tableNameAnnotation == null) {
            throw new IllegalArgumentException(String.format("%s must be annotated with @TableName", entityClass.getName()));
        }
        return getTableShortenedName(tableNameAnnotation.value());
    }

    public static void entityAnalysis(final String packageName, final String databaseType) {
        try {
            String folderPath = FileUtils.pathJoining(
                    Constant.ROOT_DIRECTORY_PROJECT_PATH,
                    "data",
                    "sql",
                    DateTimeUtils.generateCurrentLocalDateDefault().format(DateTimeFormatter.ofPattern(Constant.DEFAULT_LOCAL_DATE_STRING_PATTERN))
            );
            FileUtils.deleteFolder(new File(folderPath));
            createFolderIfNotExists(folderPath);
            List<Class<?>> listOfClassInPackage = PackageScanner.findClasses(packageName);
            for (Class<?> aClass : listOfClassInPackage) {
                if (aClass.isInterface()) {
                    continue;
                } else {
                    TableName tableNameAnnotation = aClass.getAnnotation(TableName.class);
                    if (tableNameAnnotation == null) {
                        continue;
                    }
                    List<Field> entityClassFieldList = new ArrayList<>(Arrays.asList(aClass.getDeclaredFields()));
                    entityClassFieldList.sort(Comparator.comparing(Field::getName));
                    Field idField = entityClassFieldList.stream()
                            .filter(c -> c.getAnnotation(IdColumn.class) != null)
                            .findFirst()
                            .orElse(null);
                    if (idField == null || idField.getAnnotation(ColumnName.class) == null) {
                        continue;
                    }

                    final String finalTableName;
                    String tableNameValue = tableNameAnnotation.value();

                    if (StringUtils.isNotBlank(tableNameAnnotation.schema())) {
                        String schemaName = tableNameAnnotation.schema() + ".";
                        if (tableNameValue.contains(".")) {
                            val tableNameValueArray = tableNameValue.split(JavaSqlResultSetConstant.DOT);
                            finalTableName = schemaName + tableNameValueArray[tableNameValueArray.length - 1];
                        } else {
                            finalTableName = schemaName + tableNameValue;
                        }
                    } else {
                        finalTableName = tableNameValue;
                    }

                    String createSequenceStatement = Constant.EMPTY_STRING;
                    List<List<String>> columnDefinitionLines = new ArrayList<>();

                    List<String> alterAddColumnList = new ArrayList<>();
                    List<String> alterDropColumnList = new ArrayList<>();
                    List<String> alterModifyColumnList = new ArrayList<>();
                    List<String> createIndexList = new ArrayList<>();
                    List<String> dropIndexList = new ArrayList<>();

                    for (Field field : entityClassFieldList) {
                        if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                            ColumnName columnNameAnnotation = field.getAnnotation(ColumnName.class);
                            if (columnNameAnnotation != null && StringUtils.isNotBlank(columnNameAnnotation.name())) {
                                var fieldColumnName = columnNameAnnotation.name();
                                Class<?> fieldDataTypeClass = field.getType();
                                if (fieldDataTypeClass.isPrimitive()) {
                                    continue;
                                }
                                String fieldDataTypeName = fieldDataTypeClass.getName();
                                String sqlDataType = "";

                                Map<String, String> databaseDatatypeMap;

                                switch (databaseType) {
                                    case "postgresql":
                                        databaseDatatypeMap = POSTGRESQL_DATATYPE_MAP;
                                        break;
                                    case "mysql":
                                        databaseDatatypeMap = MYSQL_DATATYPE_MAP;
                                        break;
                                    case "mssql":
                                        databaseDatatypeMap = MSSQL_DATATYPE_MAP;
                                        break;
                                    default:
                                        databaseDatatypeMap = ORACLE_DATATYPE_MAP;
                                        break;
                                }
                                if (StringUtils.isBlank(columnNameAnnotation.columnDataTypeDefinition())) {
                                    for (Map.Entry<String, String> entry : databaseDatatypeMap.entrySet()) {
                                        String k = entry.getKey();
                                        String v = entry.getValue();
                                        if (fieldDataTypeName.contains(k)) {
                                            sqlDataType = v;
                                            break;
                                        }
                                    }
                                } else {
                                    sqlDataType = columnNameAnnotation.columnDataTypeDefinition();
                                }

                                String alterAddColumn = "";
                                String alterDropColumn = "";
                                String alterModifyColumn = "";

                                if (
                                        idField.getName().equals(field.getName())
                                ) {
                                    if ((fieldDataTypeName.contains("Long") || fieldDataTypeName.contains("BigDecimal"))) {
                                        switch (databaseType) {
                                            case "postgresql":
                                                columnDefinitionLines.add(new ArrayList<>(Arrays.asList(fieldColumnName, "SERIAL", "PRIMARY KEY")));
                                                break;
                                            case "mysql":
                                                columnDefinitionLines.add(new ArrayList<>(Arrays.asList(fieldColumnName, "INT", "AUTO_INCREMENT", "PRIMARY KEY")));
                                                break;
                                            case "mssql":
                                                columnDefinitionLines.add(new ArrayList<>(Arrays.asList(fieldColumnName, "INT", "IDENTITY(1,1)", "PRIMARY KEY")));
                                                break;
                                            default:
                                                createSequenceStatement = String.format(
                                                        "CREATE SEQUENCE %1$s_SEQ START WITH 1 INCREMENT BY 1 CACHE 20;\n" +
                                                                "-- SELECT %1$s_SEQ.NEXTVAL FROM dual;\n" +
                                                                "-- SELECT %1$s_SEQ.CURRVAL FROM dual;\n",
                                                        finalTableName
                                                );
                                                columnDefinitionLines.add(
                                                        new ArrayList<>(
                                                                Arrays.asList(
                                                                        fieldColumnName, "NUMBER(18)", String.format("DEFAULT %s_SEQ.nextval NOT NULL PRIMARY KEY", finalTableName)
                                                                )
                                                        )
                                                );
                                                break;
                                        }
                                    } else {
                                        columnDefinitionLines.add(new ArrayList<>(Arrays.asList(fieldColumnName, sqlDataType, "PRIMARY KEY")));
                                    }
                                } else {
                                    List<String> columnDefinitionList = new ArrayList<>();
                                    columnDefinitionList.add(fieldColumnName);
                                    columnDefinitionList.add(sqlDataType);

                                    List<String> alterTableConstraint = new ArrayList<>();

                                    if (columnNameAnnotation.nullable()) {
                                        columnDefinitionList.add("NULL");
                                        alterTableConstraint.add("NULL");
                                    } else {
                                        if (StringUtils.isBlank(columnNameAnnotation.defaultValue())) {
                                            columnDefinitionList.add("NOT NULL");
                                            alterTableConstraint.add("NOT NULL");
                                        } else {
                                            columnDefinitionList.add("DEFAULT " + columnNameAnnotation.defaultValue());
                                            columnDefinitionList.add("NOT NULL");
                                            alterTableConstraint.add("DEFAULT " + columnNameAnnotation.defaultValue());
                                            alterTableConstraint.add("NOT NULL");
                                        }
                                        if (columnNameAnnotation.unique()) {
                                            columnDefinitionList.add("UNIQUE");
                                            alterTableConstraint.add("UNIQUE");
                                        }
                                    }
                                    columnDefinitionLines.add(columnDefinitionList);

                                    if (columnNameAnnotation.index()) {
                                        String createIndex = "", dropIndex = "";
                                        switch (databaseType) {
                                            case "postgresql":
                                            case "mysql":
                                            case "oracle":
                                                if (
                                                        (
                                                                databaseType.equals("postgresql") ||
                                                                        databaseType.equals("oracle")
                                                        ) &&
                                                                columnNameAnnotation.unique()
                                                ) {
                                                    createIndex = String.format(
                                                            "CREATE UNIQUE INDEX %s_INDEX\nON %s (%s);\n",
                                                            fieldColumnName,
                                                            finalTableName,
                                                            fieldColumnName
                                                    );
                                                } else {
                                                    createIndex = String.format(
                                                            "CREATE INDEX %s_INDEX\nON %s (%s);\n",
                                                            fieldColumnName,
                                                            finalTableName,
                                                            fieldColumnName
                                                    );
                                                }
                                                if (databaseType.equals("postgresql") || databaseType.equals("oracle")) {
                                                    dropIndex = String.format(
                                                            "DROP INDEX %s_INDEX;\n",
                                                            fieldColumnName
                                                    );
                                                } else {
                                                    dropIndex = String.format(
                                                            "DROP INDEX %s_INDEX ON %s;\n",
                                                            fieldColumnName,
                                                            finalTableName
                                                    );
                                                }
                                                break;
                                            case "mssql":
                                                createIndex = String.format(
                                                        "CREATE INDEX %s_INDEX\nON %s (%s);\n",
                                                        fieldColumnName,
                                                        finalTableName,
                                                        fieldColumnName
                                                );
                                                dropIndex = String.format(
                                                        "DROP INDEX %s_INDEX ON %s;\n",
                                                        fieldColumnName,
                                                        finalTableName
                                                );
                                                break;
                                            default:
                                                break;
                                        }
                                        if (StringUtils.isNotBlank(createIndex)) {
                                            createIndexList.add(createIndex);
                                        }
                                        if (StringUtils.isNotBlank(dropIndex)) {
                                            dropIndexList.add(dropIndex);
                                        }
                                    }

                                    switch (databaseType) {
                                        case "postgresql": {
                                            alterAddColumn = String.format(
                                                    "ALTER TABLE %s\n  ADD COLUMN %s %s %s;\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            alterDropColumn = String.format(
                                                    "ALTER TABLE %s\n  DROP COLUMN %s;\n",
                                                    finalTableName,
                                                    fieldColumnName
                                            );
                                            alterModifyColumn = String.format(
                                                    "ALTER TABLE %s\n  ALTER COLUMN %s TYPE %s, ALTER COLUMN %2$s SET %s;\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            break;
                                        }
                                        case "mysql": {
                                            alterAddColumn = String.format(
                                                    "ALTER TABLE %s\n  ADD %s %s %s;\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            alterDropColumn = String.format(
                                                    "ALTER TABLE %s\n  DROP COLUMN %s;\n",
                                                    finalTableName,
                                                    fieldColumnName
                                            );
                                            alterModifyColumn = String.format(
                                                    "ALTER TABLE %s\n  MODIFY %s %s %s;\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            break;
                                        }
                                        case "mssql": {
                                            alterAddColumn = String.format(
                                                    "ALTER TABLE %s\n  ADD %s %s %s;\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            alterDropColumn = String.format(
                                                    "ALTER TABLE %s\n  DROP COLUMN %s;\n",
                                                    finalTableName,
                                                    fieldColumnName
                                            );
                                            break;
                                        }
                                        default: {
                                            alterAddColumn = String.format(
                                                    "ALTER TABLE %s\n  ADD (%s %s %s);\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            alterDropColumn = String.format(
                                                    "ALTER TABLE %s\n  DROP COLUMN %s;\n",
                                                    finalTableName,
                                                    fieldColumnName
                                            );
                                            alterModifyColumn = String.format(
                                                    "ALTER TABLE %s\n  MODIFY (%s %s %s);\n",
                                                    finalTableName,
                                                    fieldColumnName,
                                                    sqlDataType,
                                                    String.join(" ", alterTableConstraint)
                                            );
                                            break;
                                        }
                                    }
                                    alterAddColumnList.add(alterAddColumn);
                                    alterDropColumnList.add(alterDropColumn);
                                    alterModifyColumnList.add(alterModifyColumn);
                                }

                            }
                        }
                    }
                    createIndexList = createIndexList.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                    dropIndexList = dropIndexList.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                    alterAddColumnList = alterAddColumnList.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                    alterDropColumnList = alterDropColumnList.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                    alterModifyColumnList = alterModifyColumnList.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                    val generatedTime = DateTimeUtils.generateCurrentTimeDefault().format(DateTimeFormatter.ofPattern(Constant.DEFAULT_LOCAL_DATE_TIME_STRING_PATTERN));
                    String createTableStatement = String.format(
                            String.format("-- GENERATED AT %s BY LOCVT\n\n", generatedTime) +
                                    "-- ################# CREATE INDEX ####################### --\n" +
                                    "\n" +
                                    "%s\n" +
                                    "-- ################# DROP INDEX ####################### --\n" +
                                    "\n" +
                                    "%s\n" +
                                    "-- ################# ADD COLUMN ####################### --\n" +
                                    "\n" +
                                    "%s\n" +
                                    "-- ################# DROP COLUMN ####################### --\n" +
                                    "\n" +
                                    "%s\n" +
                                    "-- ################# MODIFY COLUMN ####################### --\n" +
                                    "\n" +
                                    "%s\n" +
                                    "-- ################# CREATE TABLE ####################### --\n" +
                                    "%s\n" +
                                    "%s",
                            String.join(System.lineSeparator(), createIndexList),
                            String.join(System.lineSeparator(), dropIndexList),
                            String.join(System.lineSeparator(), alterAddColumnList),
                            String.join(System.lineSeparator(), alterDropColumnList),
                            String.join(System.lineSeparator(), alterModifyColumnList),
                            (StringUtils.isBlank(createSequenceStatement) ? Constant.EMPTY_STRING : "\n" + createSequenceStatement + "\n"),
                            String.format(
                                    CREATE_TABLE_TEMPLATE_NO_PRIMARY_KEY,
                                    finalTableName,
                                    MyStringUtils.formatStringSpace2(columnDefinitionLines, ",\n    ")
                            )
                    );
                    writeContentToFile(
                            FileUtils.pathJoining(folderPath, tableNameAnnotation.value() + ".sql"),
                            createTableStatement
                    );
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
