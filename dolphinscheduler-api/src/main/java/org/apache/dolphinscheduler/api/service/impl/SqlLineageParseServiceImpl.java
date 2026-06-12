/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api.service.impl;

import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineage;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineageColumn;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineageEdge;
import org.apache.dolphinscheduler.api.dto.datagovernance.DataGovernanceDtos.SqlLineageTable;
import org.apache.dolphinscheduler.api.service.SqlLineageParseService;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

@Service
public class SqlLineageParseServiceImpl implements SqlLineageParseService {

    private static final String LINEAGE_TYPE_TABLE = "TABLE";
    private static final String LINEAGE_TYPE_FIELD = "FIELD";
    private static final String DEFAULT_COLUMN_TYPE = "UNKNOWN";
    private static final String QUERY_RESULT_TABLE = "query_result";
    private static final int MAX_SQL_LENGTH = 20_000;

    @Override
    public SqlLineage parse(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        if (sql.length() > MAX_SQL_LENGTH) {
            throw new IllegalArgumentException("SQL 超过 20000 字符，当前 MVP 暂不支持解析超长 SQL");
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Insert) {
                return parseInsert((Insert) statement);
            }
            if (statement instanceof Select) {
                return parseSelect((Select) statement);
            }
            throw new IllegalArgumentException("第一版 SQL 血缘解析仅支持 SELECT 或 INSERT INTO ... SELECT ...");
        } catch (JSQLParserException ex) {
            throw new IllegalArgumentException("SQL 解析失败：" + ex.getMessage(), ex);
        }
    }

    private SqlLineage parseInsert(Insert insert) {
        SqlLineage lineage = new SqlLineage();
        Table target = insert.getTable();
        if (target == null || insert.getSelect() == null) {
            throw new IllegalArgumentException("INSERT 语句需要包含目标表和 SELECT 查询");
        }
        SqlLineageTable targetTable = addTable(lineage, target);
        SelectBody selectBody = insert.getSelect().getSelectBody();
        if (selectBody instanceof SetOperationList) {
            parseInsertSetOperation((SetOperationList) selectBody, lineage, targetTable, insert.getColumns());
            return lineage;
        }
        ParsedSelect parsedSelect = parseSelectBody(selectBody, lineage);
        if (!isEmpty(insert.getWithItemsList())) {
            lineage.getWarnings().add("当前 MVP 已识别 WITH 语句，但 CTE 字段级血缘仅按主 SELECT 近似解析。");
        }
        parsedSelect.sourceTables.values().forEach(sourceTable -> addTableEdge(lineage, sourceTable.getId(), targetTable.getId()));

        List<Column> targetColumns = insert.getColumns();
        List<SelectItem> selectItems = parsedSelect.selectItems;
        for (int index = 0; index < selectItems.size(); index++) {
            String targetColumn = resolveTargetColumn(targetColumns, selectItems.get(index), index);
            addColumn(targetTable, targetColumn);
            addFieldEdges(lineage, parsedSelect, selectItems.get(index), targetTable.getId(), targetColumn);
        }
        return lineage;
    }

    private void parseInsertSetOperation(SqlLineage lineage,
                                         SqlLineageTable targetTable,
                                         List<Column> targetColumns,
                                         List<SelectBody> selectBodies) {
        if (isEmpty(selectBodies)) {
            return;
        }
        lineage.getWarnings().add("已按 UNION/INTERSECT 等集合查询的每个 SELECT 分支生成近似血缘。");
        for (SelectBody branch : selectBodies) {
            ParsedSelect parsedSelect = parseSelectBody(branch, lineage);
            parsedSelect.sourceTables.values()
                    .forEach(sourceTable -> addTableEdge(lineage, sourceTable.getId(), targetTable.getId()));
            List<SelectItem> selectItems = parsedSelect.selectItems;
            for (int index = 0; index < selectItems.size(); index++) {
                String targetColumn = resolveTargetColumn(targetColumns, selectItems.get(index), index);
                addColumn(targetTable, targetColumn);
                addFieldEdges(lineage, parsedSelect, selectItems.get(index), targetTable.getId(), targetColumn);
            }
        }
    }

    private void parseInsertSetOperation(SetOperationList setOperationList,
                                         SqlLineage lineage,
                                         SqlLineageTable targetTable,
                                         List<Column> targetColumns) {
        parseInsertSetOperation(lineage, targetTable, targetColumns, setOperationList.getSelects());
    }

    private SqlLineage parseSelect(Select select) {
        SqlLineage lineage = new SqlLineage();
        SqlLineageTable resultTable = addVirtualResultTable(lineage);
        if (select.getSelectBody() instanceof SetOperationList) {
            parseInsertSetOperation(lineage, resultTable, new ArrayList<>(),
                    ((SetOperationList) select.getSelectBody()).getSelects());
            return lineage;
        }
        ParsedSelect parsedSelect = parseSelectBody(select.getSelectBody(), lineage);
        if (!isEmpty(select.getWithItemsList())) {
            lineage.getWarnings().add("当前 MVP 已识别 WITH 语句，但 CTE 字段级血缘仅按主 SELECT 近似解析。");
        }
        parsedSelect.sourceTables.values().forEach(sourceTable -> addTableEdge(lineage, sourceTable.getId(), resultTable.getId()));

        for (int index = 0; index < parsedSelect.selectItems.size(); index++) {
            SelectItem item = parsedSelect.selectItems.get(index);
            String targetColumn = resolveSelectAlias(item, index);
            addColumn(resultTable, targetColumn);
            addFieldEdges(lineage, parsedSelect, item, resultTable.getId(), targetColumn);
        }
        return lineage;
    }

    private ParsedSelect parseSelectBody(SelectBody selectBody, SqlLineage lineage) {
        if (selectBody instanceof PlainSelect) {
            return parsePlainSelect((PlainSelect) selectBody, lineage);
        }
        if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            lineage.getWarnings().add("当前上下文对 UNION/INTERSECT 等集合查询仅返回第一个 SELECT 分支。");
            if (!isEmpty(setOperationList.getSelects())) {
                return parseSelectBody(setOperationList.getSelects().get(0), lineage);
            }
        }
        if (selectBody instanceof WithItem) {
            WithItem withItem = (WithItem) selectBody;
            lineage.getWarnings().add("当前 MVP 对 CTE 仅展开 WITH 子查询的第一个 SELECT。");
            if (withItem.getSubSelect() != null) {
                return parseSelectBody(withItem.getSubSelect().getSelectBody(), lineage);
            }
        }
        throw new IllegalArgumentException("暂不支持该 SELECT 结构，建议后续接入 Calcite 增强。");
    }

    private ParsedSelect parsePlainSelect(PlainSelect plainSelect, SqlLineage lineage) {
        ParsedSelect parsed = new ParsedSelect();
        collectFromItem(plainSelect.getFromItem(), parsed, lineage);
        if (!isEmpty(plainSelect.getJoins())) {
            for (Join join : plainSelect.getJoins()) {
                collectFromItem(join.getRightItem(), parsed, lineage);
            }
        }
        parsed.selectItems.addAll(plainSelect.getSelectItems());
        return parsed;
    }

    private void collectFromItem(FromItem fromItem, ParsedSelect parsed, SqlLineage lineage) {
        if (fromItem instanceof Table) {
            SqlLineageTable table = addTable(lineage, (Table) fromItem);
            parsed.sourceTables.put(table.getId(), table);
            Alias alias = fromItem.getAlias();
            if (alias != null && StringUtils.isNotBlank(alias.getName())) {
                parsed.aliasToTableId.put(normalize(alias.getName()), table.getId());
            }
            parsed.aliasToTableId.put(normalize(table.getName()), table.getId());
            parsed.aliasToTableId.put(normalize(table.getId()), table.getId());
            return;
        }
        if (fromItem instanceof SubSelect) {
            lineage.getWarnings().add("当前 MVP 对 FROM 子查询仅解析子查询内部来源表，子查询别名字段暂按近似血缘处理。");
            ParsedSelect subParsed = parseSelectBody(((SubSelect) fromItem).getSelectBody(), lineage);
            parsed.sourceTables.putAll(subParsed.sourceTables);
            parsed.aliasToTableId.putAll(subParsed.aliasToTableId);
        }
    }

    private void addFieldEdges(
            SqlLineage lineage,
            ParsedSelect parsedSelect,
            SelectItem selectItem,
            String targetTable,
            String targetColumn) {
        if (selectItem instanceof AllColumns) {
            lineage.getWarnings().add("SELECT * 暂无法展开真实字段，已生成星号字段级血缘。");
            parsedSelect.sourceTables.values().forEach(sourceTable -> {
                addColumn(sourceTable, "*");
                addFieldEdge(lineage, sourceTable.getId(), "*", targetTable, targetColumn);
            });
            return;
        }
        if (selectItem instanceof AllTableColumns) {
            AllTableColumns allTableColumns = (AllTableColumns) selectItem;
            String sourceTableId = resolveSourceTableId(parsedSelect, allTableColumns.getTable());
            addFieldEdge(lineage, sourceTableId, "*", targetTable, targetColumn);
            addColumn(lineage.getTables().stream().filter(table -> table.getId().equals(sourceTableId)).findFirst().orElse(null), "*");
            return;
        }
        if (!(selectItem instanceof SelectExpressionItem)) {
            lineage.getWarnings().add("存在暂不支持的 SELECT 字段项：" + selectItem);
            return;
        }
        SelectExpressionItem expressionItem = (SelectExpressionItem) selectItem;
        List<Column> sourceColumns = extractColumns(expressionItem.getExpression());
        if (sourceColumns.isEmpty()) {
            lineage.getWarnings().add("字段 " + targetColumn + " 来源于常量或暂不支持表达式，未生成字段边。");
            return;
        }
        for (Column column : sourceColumns) {
            String sourceTableId = resolveSourceTableId(parsedSelect, column.getTable());
            String sourceColumn = column.getColumnName();
            SqlLineageTable sourceTable = parsedSelect.sourceTables.get(sourceTableId);
            addColumn(sourceTable, sourceColumn);
            addFieldEdge(lineage, sourceTableId, sourceColumn, targetTable, targetColumn);
            if (column.getTable() == null || StringUtils.isBlank(column.getTable().getName())) {
                lineage.getWarnings().add("字段 " + sourceColumn + " 未带表别名，已按第一个来源表近似归属。");
            }
        }
    }

    private String resolveSourceTableId(ParsedSelect parsedSelect, Table table) {
        if (table != null && StringUtils.isNotBlank(table.getName())) {
            String resolved = parsedSelect.aliasToTableId.get(normalize(table.getName()));
            if (resolved != null) {
                return resolved;
            }
            String tableId = tableId(table);
            if (parsedSelect.sourceTables.containsKey(tableId)) {
                return tableId;
            }
        }
        if (!parsedSelect.sourceTables.isEmpty()) {
            return parsedSelect.sourceTables.values().iterator().next().getId();
        }
        return "unknown_source";
    }

    private List<Column> extractColumns(Expression expression) {
        List<Column> columns = new ArrayList<>();
        if (expression == null) {
            return columns;
        }
        expression.accept(new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column column) {
                columns.add(column);
            }
        });
        return columns;
    }

    private SqlLineageTable addTable(SqlLineage lineage, Table table) {
        String id = tableId(table);
        SqlLineageTable existing = findTable(lineage, id);
        if (existing != null) {
            return existing;
        }
        SqlLineageTable dto = new SqlLineageTable();
        dto.setId(id);
        dto.setName(stripQuote(table.getName()));
        dto.setSchema(stripQuote(table.getSchemaName()));
        lineage.getTables().add(dto);
        return dto;
    }

    private SqlLineageTable addVirtualResultTable(SqlLineage lineage) {
        SqlLineageTable dto = new SqlLineageTable();
        dto.setId(QUERY_RESULT_TABLE);
        dto.setName(QUERY_RESULT_TABLE);
        dto.setSchema("");
        lineage.getTables().add(dto);
        return dto;
    }

    private SqlLineageTable findTable(SqlLineage lineage, String id) {
        return lineage.getTables().stream()
                .filter(table -> table.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void addColumn(SqlLineageTable table, String columnName) {
        if (table == null || StringUtils.isBlank(columnName)) {
            return;
        }
        boolean exists = table.getColumns().stream().anyMatch(column -> column.getName().equals(columnName));
        if (exists) {
            return;
        }
        SqlLineageColumn column = new SqlLineageColumn();
        column.setName(stripQuote(columnName));
        column.setType(DEFAULT_COLUMN_TYPE);
        table.getColumns().add(column);
    }

    private void addTableEdge(SqlLineage lineage, String sourceTable, String targetTable) {
        addEdge(lineage, sourceTable, null, targetTable, null, LINEAGE_TYPE_TABLE);
    }

    private void addFieldEdge(
            SqlLineage lineage,
            String sourceTable,
            String sourceColumn,
            String targetTable,
            String targetColumn) {
        addEdge(lineage, sourceTable, stripQuote(sourceColumn), targetTable, stripQuote(targetColumn), LINEAGE_TYPE_FIELD);
    }

    private void addEdge(
            SqlLineage lineage,
            String sourceTable,
            String sourceColumn,
            String targetTable,
            String targetColumn,
            String lineageType) {
        boolean exists = lineage.getEdges().stream().anyMatch(edge ->
                StringUtils.equals(edge.getSourceTable(), sourceTable)
                        && StringUtils.equals(edge.getSourceColumn(), sourceColumn)
                        && StringUtils.equals(edge.getTargetTable(), targetTable)
                        && StringUtils.equals(edge.getTargetColumn(), targetColumn)
                        && StringUtils.equals(edge.getLineageType(), lineageType));
        if (exists) {
            return;
        }
        SqlLineageEdge edge = new SqlLineageEdge();
        edge.setSourceTable(sourceTable);
        edge.setSourceColumn(sourceColumn);
        edge.setTargetTable(targetTable);
        edge.setTargetColumn(targetColumn);
        edge.setLineageType(lineageType);
        lineage.getEdges().add(edge);
    }

    private String resolveTargetColumn(List<Column> targetColumns, SelectItem selectItem, int index) {
        if (!isEmpty(targetColumns) && index < targetColumns.size()) {
            return targetColumns.get(index).getColumnName();
        }
        if (selectItem instanceof AllColumns || selectItem instanceof AllTableColumns) {
            return "*";
        }
        return resolveSelectAlias(selectItem, index);
    }

    private String resolveSelectAlias(SelectItem selectItem, int index) {
        if (selectItem instanceof SelectExpressionItem) {
            SelectExpressionItem item = (SelectExpressionItem) selectItem;
            if (item.getAlias() != null && StringUtils.isNotBlank(item.getAlias().getName())) {
                return item.getAlias().getName();
            }
            if (item.getExpression() instanceof Column) {
                return ((Column) item.getExpression()).getColumnName();
            }
        }
        return "expr_" + (index + 1);
    }

    private String tableId(Table table) {
        String schema = stripQuote(table.getSchemaName());
        String name = stripQuote(table.getName());
        return StringUtils.isBlank(schema) ? name : schema + "." + name;
    }

    private String normalize(String value) {
        return stripQuote(value).toLowerCase(Locale.ROOT);
    }

    private String stripQuote(String value) {
        return value == null ? "" : value.replace("`", "").replace("\"", "").trim();
    }

    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private static class ParsedSelect {

        private final Map<String, SqlLineageTable> sourceTables = new LinkedHashMap<>();
        private final Map<String, String> aliasToTableId = new LinkedHashMap<>();
        private final List<SelectItem> selectItems = new ArrayList<>();
    }
}
