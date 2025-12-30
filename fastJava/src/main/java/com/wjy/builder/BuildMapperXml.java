package com.wjy.builder;

import com.wjy.bean.Constants;
import com.wjy.bean.FieldInfo;
import com.wjy.bean.TableInfo;
import com.wjy.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildMapperXml {

    private static final Logger logger = LoggerFactory.getLogger(BuildMapperXml.class);

    private static final String BASE_COLUMN_LIST = "base_column_list";

    private static final String BASE_QUERY_CONDITION = "base_query_condition";

    private static final String BASE_QUERY_CONDITION_EXTEND = "base_query_condition_extend";

    private static final String QUERY_CONDITION = "query_condition";

    public static void execute(TableInfo tableInfo) {
        File folder = new File(Constants.PATH_MAPPERS_XMLS);  // 改为PATH_MAPPERS_XMLS（资源目录）
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String className = tableInfo.getBeanName() + Constants.SUFFIX_MAPPERS;

        File poFile = new File(folder,  className + ".xml");

        OutputStream out = null;
        OutputStreamWriter outw = null;
        BufferedWriter bw = null;
        try {
            out = new FileOutputStream(poFile);
            outw = new OutputStreamWriter(out, "utf8");
            bw = new BufferedWriter(outw);
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            bw.newLine();
            bw.write("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"");
            bw.newLine();
            bw.write( "\t\t\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">");
            bw.newLine();
            bw.write("<mapper namespace=\"" + Constants.PACKAGE_MAPPERS + "." + className + "\">");
            bw.newLine();

            bw.write("\t<!--实体映射-->");
            bw.newLine();
            String poClass = Constants.PACKAGE_PO + "." + tableInfo.getBeanName();
            bw.write("\t<resultMap id=\"base_result_map\" type=\"" + poClass + "\">");

            FieldInfo idField = null;
            Map<String, List<FieldInfo>> keyIndexMap = tableInfo.getKeyIndexMap();
            for (Map.Entry<String, List<FieldInfo>> entry : keyIndexMap.entrySet()) {
                if ("PRIMARY".equals(entry.getKey())) {
                    List<FieldInfo> fieldInfoList = entry.getValue();
                    if (fieldInfoList.size() == 1) {
                        idField = fieldInfoList.get(0);
                        break;
                    }
                }
            }

            for (FieldInfo fieldInfo : tableInfo.getFieldList()) {
                bw.write("\t\t<!-- " + fieldInfo.getComment() + "-->");
                bw.newLine();
                String key = "";
                if (idField != null && fieldInfo.getPropertyName().equals(idField.getPropertyName())) {
                    key = "id";
                } else {
                    key = "result";
                }
                bw.write("\t\t<" + key + " column=\"" + fieldInfo.getFieldName() +"\" property=\"" + fieldInfo.getPropertyName() + "\"/>");
                bw.newLine();
            }
            bw.write("\t</resultMap>");
            bw.newLine();

            // 通用查询列
            bw.write("\t<!--通用查询列-->");
            bw.newLine();

            bw.write("\t<sql id=\"" + BASE_COLUMN_LIST + "\">");
            bw.newLine();
            StringBuilder columnBuilder = new StringBuilder();
            for (FieldInfo fieldInfo : tableInfo.getFieldList()) {
                columnBuilder.append(fieldInfo.getFieldName()).append(",");
            }
            String columnBuilderStr = columnBuilder.substring(0, columnBuilder.lastIndexOf(","));
            bw.write("\t\t" + columnBuilderStr);
            bw.newLine();
            bw.write("\t</sql>");
            bw.newLine();

            // 基础查询条件
            bw.newLine();
            bw.write("\t<!--基础查询条件-->");
            bw.newLine();
            bw.write("\t<sql id=\"" + BASE_QUERY_CONDITION + "\">");
            bw.newLine();
            for (FieldInfo fieldInfo : tableInfo.getFieldList()) {
                String stringQuery = "";
                if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, fieldInfo.getSqlType())) {
                    stringQuery = " and query." + fieldInfo.getPropertyName() + "!=''";
                }

                bw.write("\t\t<if test=\"query." + fieldInfo.getPropertyName() +  " != null" + stringQuery + "\">");
                bw.newLine();
                bw.write("\t\t\tand " + fieldInfo.getFieldName() + " = #{query." + fieldInfo.getPropertyName() + "}");
                bw.newLine();
                bw.write("\t\t</if>");
                bw.newLine();
            }
            bw.write("\t</sql>");
            bw.newLine();

            // 扩展的查询条件
            bw.newLine();
            bw.write("\t<!--扩展的查询条件-->");
            bw.newLine();
            bw.write("\t<sql id=\"" + BASE_QUERY_CONDITION_EXTEND + "\">");
            bw.newLine();
            for (FieldInfo fieldInfo : tableInfo.getFieldExtendList()) {
                String andWhere = "";
                if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, fieldInfo.getSqlType())) {
                    andWhere = "and " + fieldInfo.getFieldName() + " like concat('%', #{query." + fieldInfo.getPropertyName() + "}, '%')";
                } else if (ArrayUtils.contains(Constants.SQL_DATE_TIME_TYPES, fieldInfo.getSqlType()) || ArrayUtils.contains(Constants.SQL_DATE_TYPES, fieldInfo.getSqlType())) {
                    if (fieldInfo.getPropertyName().endsWith(Constants.SUFFIX_BEAN_QUERY_TIME_START)) {
                        andWhere = "<![CDATA[ and " + fieldInfo.getFieldName() + " >= str_to_date(#{query." + fieldInfo.getPropertyName() + "}, '%Y-%m-%d %H:%i:%s') ]]>";
                    } else if (fieldInfo.getPropertyName().endsWith(Constants.SUFFIX_BEAN_QUERY_TIME_END)) {
                        andWhere = "<![CDATA[ and " + fieldInfo.getFieldName() + " < date_sub(str_to_date(#{query." + fieldInfo.getPropertyName() + "},'%Y-%m-%d %H:%i:%s'),interval -1 day) ]]>";
                    }
                }

                bw.write("\t\t<if test=\"query." + fieldInfo.getPropertyName() +  " != null and query." + fieldInfo.getPropertyName() + " != '' \">");
                bw.newLine();
                bw.write("\t\t\t" + andWhere);
                bw.newLine();
                bw.write("\t\t</if>");
                bw.newLine();
            }
            bw.write("\t</sql>");
            bw.newLine();
            // 通用查询条件
            bw.newLine();
            bw.write("\t<!--扩展的查询条件-->");
            bw.newLine();
            bw.write("\t<sql id=\"" + QUERY_CONDITION + "\">");
            bw.newLine();
            bw.write("\t\t<where>");
            bw.newLine();
            bw.write("\t\t\t<include refid=\"" + BASE_QUERY_CONDITION + "\"/>");
            bw.newLine();
            bw.write("\t\t\t<include refid=\"" + BASE_QUERY_CONDITION_EXTEND + "\"/>");
            bw.newLine();
            bw.write("\t\t</where>");
            bw.newLine();
            bw.write("\t</sql>");
            bw.newLine();

            // 查询列表
            bw.newLine();
            bw.write("\t<!--查询列表-->");
            bw.newLine();

            bw.write("\t<select id=\"selectList\" resultMap=\"base_result_map\">");
            bw.newLine();
            bw.write("\t\tSELECT <include refid=\"" + BASE_COLUMN_LIST + "\"/> FROM " + tableInfo.getTableName() + " <include refid=\"" + QUERY_CONDITION + "\"/>");
            bw.newLine();
            bw.write("\t\t<if test=\"query.orderBy!=null\">order by ${query.orderBy}</if>");
            bw.newLine();
            bw.write("\t\t<if test=\"query.simplePage!=null\">limit #{query.simplePage.start},#{query.simplePage.end}</if>");
            bw.newLine();
            bw.write("\t</select>");
            bw.newLine();


            // 查询数量
            bw.newLine();
            bw.write("\t<!--查询数量-->");
            bw.newLine();

            bw.write("\t<select id=\"selectCount\" resultType=\"java.lang.Long\">");
            bw.newLine();
            bw.write("\t\tSELECT count(1) FROM " + tableInfo.getTableName() + " <include refid=\"" + QUERY_CONDITION + "\"/>");
            bw.newLine();
            bw.write("\t</select>");
            bw.newLine();

            // ========== 构建索引映射（所有插入方法共用） ==========
            // 构建所有索引字段映射（包括主键和唯一索引）
            Map<String, String> indexFieldMap = new HashMap<>();
            for (Map.Entry<String, List<FieldInfo>> entry : keyIndexMap.entrySet()) {
                for (FieldInfo item : entry.getValue()) {
                    indexFieldMap.put(item.getFieldName(), item.getFieldName());
                }
            }

            // 构建主键映射表（只包含PRIMARY KEY）
            Map<String, String> primaryKeyMap = new HashMap<>();
            List<FieldInfo> primaryKeyFields = keyIndexMap.get("PRIMARY");
            if (primaryKeyFields != null) {
                for (FieldInfo item : primaryKeyFields) {
                    primaryKeyMap.put(item.getFieldName(), item.getFieldName());
                }
            }

            // 构建唯一索引映射（不包括主键）
            Map<String, String> uniqueIndexMap = new HashMap<>();
            for (Map.Entry<String, List<FieldInfo>> entry : keyIndexMap.entrySet()) {
                if (!"PRIMARY".equals(entry.getKey())) {  // 排除主键
                    for (FieldInfo item : entry.getValue()) {
                        uniqueIndexMap.put(item.getFieldName(), item.getFieldName());
                    }
                }
            }

            // 单条插入
            bw.newLine();
            bw.write("\t<!--插入 (匹配有值的字段)-->");
            bw.newLine();
            bw.write("\t<insert id=\"insert\" parameterType=\"" + poClass + "\">");
            bw.newLine();

            FieldInfo autoIncrementField = null;
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    autoIncrementField = fieldInfo;
                    break;
                }
            }

            if (autoIncrementField != null) {
                bw.write("\t\t<selectKey keyProperty=\"bean." + autoIncrementField.getPropertyName() + "\" resultType=\"" + autoIncrementField.getJavaType() + "\" order=\"AFTER\">");
                bw.newLine();
                bw.write("\t\t\tSELECT LAST_INSERT_ID()");
                bw.newLine();
                bw.write("\t\t</selectKey>");
            }

            bw.newLine();
            bw.write("\t\tINSERT INTO " + tableInfo.getTableName());
            bw.newLine();
            bw.write("\t\t<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                bw.newLine();
                // 跳过自增字段
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    continue;
                }
                // 对于主键和唯一索引字段，必须包含（不能为空）
                if (indexFieldMap.get(fieldInfo.getFieldName()) != null) {
bw.write("\t\t\t" + fieldInfo.getFieldName() + ",");
                }
                // 对于 NOT NULL 的非索引字段，总是包含
                else if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                    bw.write("\t\t\t" + fieldInfo.getFieldName() + ",");
                }
                // 其他可空字段，只在非 null 时包含
                else {
                    bw.write("\t\t\t<if test=\"bean." + fieldInfo.getPropertyName() +  " != null\">");
                    bw.newLine();
                    bw.write("\t\t\t\t" + fieldInfo.getFieldName() + ",");
                    bw.newLine();
                    bw.write("\t\t\t</if>");
                }
            }
            bw.newLine();
            bw.write("\t\t</trim>");
            bw.newLine();
            bw.write("\t\t<trim prefix=\"VALUES (\" suffix=\")\" suffixOverrides=\",\">");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                bw.newLine();
                // 跳过自增字段
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    continue;
                }
                // 对于主键和唯一索引字段，直接使用值（不能为空，不提供默认值）
                if (indexFieldMap.get(fieldInfo.getFieldName()) != null) {
                    bw.write("\t\t\t#{bean." + fieldInfo.getPropertyName() + "},");
                }
                // 对于 NOT NULL 的非索引字段，使用 IFNULL 提供默认值
                else if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                    String defaultValue = "";
                    if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "''";  // 字符串默认为空字符串
                    } else if (ArrayUtils.contains(Constants.SQL_INTEGER_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_LONG_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "0";   // 数字默认为0
                    } else if (ArrayUtils.contains(Constants.SQL_DATE_TIME_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_DATE_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "NOW()";  // 日期默认为当前时间
                    } else {
                        defaultValue = "''";  // 其他类型默认为空字符串
                    }
                    bw.write("\t\t\tIFNULL(#{bean." + fieldInfo.getPropertyName() + "}, " + defaultValue + "),");
                }
                // 其他可空字段，只在非 null 时包含
                else {
                    bw.write("\t\t\t<if test=\"bean." + fieldInfo.getPropertyName() + " != null\">");
                    bw.newLine();
                    bw.write("\t\t\t\t#{bean." + fieldInfo.getPropertyName() + "},");
                    bw.newLine();
                    bw.write("\t\t\t</if>");
                }
            }
            bw.newLine();
            bw.write("\t\t</trim>");
            bw.newLine();
            bw.write("\t</insert>");
            bw.newLine();
            bw.newLine();

            // 插入或者更新
            bw.write("\t<!--插入或者更新 (匹配有值的字段)-->");
            bw.newLine();
            bw.write("\t<insert id=\"insertOrUpdate\" parameterType=\"" + poClass + "\">");
            bw.newLine();

            bw.write("\t\tINSERT INTO " + tableInfo.getTableName());
            bw.newLine();
            bw.write("\t\t<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                bw.newLine();
                // 对于主键字段，必须包含（不能为空）
                if (primaryKeyMap.get(fieldInfo.getFieldName()) != null) {
                    bw.write("\t\t\t" + fieldInfo.getFieldName() + ",");
                }
                // 对于唯一索引字段
                else if (uniqueIndexMap.get(fieldInfo.getFieldName()) != null) {
                    // 如果唯一索引是 NOT NULL，总是包含（提供默认值）
                    if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                        bw.write("\t\t\t" + fieldInfo.getFieldName() + ",");
                    }
                    // 如果唯一索引可以为 NULL，只在非null时包含
                    else {
                        bw.write("\t\t\t<if test=\"bean." + fieldInfo.getPropertyName() +  " != null\">");
                        bw.newLine();
                        bw.write("\t\t\t\t" + fieldInfo.getFieldName() + ",");
                        bw.newLine();
                        bw.write("\t\t\t</if>");
                    }
                }
                // 对于NOT NULL的非主键、非唯一索引字段，总是包含
                else if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                    bw.write("\t\t\t" + fieldInfo.getFieldName() + ",");
                }
                // 可空字段，只在非null时包含
                else {
                    bw.write("\t\t\t<if test=\"bean." + fieldInfo.getPropertyName() +  " != null\">");
                    bw.newLine();
                    bw.write("\t\t\t\t" + fieldInfo.getFieldName() + ",");
                    bw.newLine();
                    bw.write("\t\t\t</if>");
                }
            }
            bw.newLine();
            bw.write("\t\t</trim>");
            bw.newLine();
            bw.write("\t\t<trim prefix=\"VALUES (\" suffix=\")\" suffixOverrides=\",\">");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                bw.newLine();
                // 对于主键字段，直接使用值（不能为空，不提供默认值）
                if (primaryKeyMap.get(fieldInfo.getFieldName()) != null) {
                    bw.write("\t\t\t#{bean." + fieldInfo.getPropertyName() + "},");
                }
                // 对于唯一索引字段，不提供默认值，要求调用方必须显式设置
                // 避免多条记录的唯一字段都使用相同的默认值导致冲突
                else if (uniqueIndexMap.get(fieldInfo.getFieldName()) != null) {
                    bw.write("\t\t\t#{bean." + fieldInfo.getPropertyName() + "},");
                }
                // 对于NOT NULL的非主键、非唯一索引字段，使用IFNULL提供默认值
                else if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                    String defaultValue = "";
                    if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "''";  // 字符串默认为空字符串
                    } else if (ArrayUtils.contains(Constants.SQL_INTEGER_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_LONG_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "0";   // 数字默认为0
                    } else if (ArrayUtils.contains(Constants.SQL_DATE_TIME_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_DATE_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "NOW()";  // 日期默认为当前时间
                    } else {
                        defaultValue = "''";  // 其他类型默认为空字符串
                    }
                    bw.write("\t\t\tIFNULL(#{bean." + fieldInfo.getPropertyName() + "}, " + defaultValue + "),");
                }
                // 可空字段，只在非null时包含
                else {
                    bw.write("\t\t\t<if test=\"bean." + fieldInfo.getPropertyName() + " != null\">");
                    bw.newLine();
                    bw.write("\t\t\t\t#{bean." + fieldInfo.getPropertyName() + "},");
                    bw.newLine();
                    bw.write("\t\t\t</if>");
                }
            }
            bw.newLine();
            bw.write("\t\t</trim>");
            bw.newLine();
            bw.write("\t\ton DUPLICATE key update");
            bw.newLine();

            bw.write("\t\t<trim suffixOverrides=\",\">");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                if (primaryKeyMap.get(fieldInfo.getFieldName()) != null) {
                    continue;
                }
                bw.newLine();
                bw.write("\t\t\t<if test=\"bean." + fieldInfo.getPropertyName() + " != null\">");
                bw.newLine();
                bw.write("\t\t\t\t" + fieldInfo.getFieldName() + " = #{bean." + fieldInfo.getPropertyName() + "},");
                bw.newLine();
                bw.write("\t\t\t</if>");
            }
            bw.newLine();
            bw.write("\t\t</trim>");
            bw.newLine();
            bw.write("\t</insert>");
            bw.newLine();
            bw.newLine();

            // 批量插入
            bw.write("\t<!-- 添加 (批量插入)-->");
            bw.newLine();
            bw.write("\t<insert id=\"insertBatch\" parameterType=\"" + poClass + "\">");
            bw.newLine();
            StringBuffer insertFieldBuffer = new StringBuffer();
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    continue;
                }
                insertFieldBuffer.append(fieldInfo.getFieldName()).append(",");
            }
            String insertFieldBufferStr = insertFieldBuffer.substring(0, insertFieldBuffer.lastIndexOf(","));
            bw.write("\t\tINSERT INTO " + tableInfo.getTableName() + "(" + insertFieldBufferStr + ") VALUES");
            bw.newLine();
            bw.write("\t\t<foreach collection=\"list\" item=\"item\" separator=\",\">");
            bw.newLine();

            StringBuilder valuesBuilder = new StringBuilder();
            valuesBuilder.append("\t\t\t(");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    continue;
                }
                // 对于主键和唯一索引字段，直接使用值（不能为空，不提供默认值）
                if (indexFieldMap.get(fieldInfo.getFieldName()) != null) {
                    valuesBuilder.append("#{item." + fieldInfo.getPropertyName() + "},");
                }
                // 对于 NOT NULL 非索引字段提供默认值
                else if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                    String defaultValue = "";
                    if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "''";  // 字符串默认为空字符串
                    } else if (ArrayUtils.contains(Constants.SQL_INTEGER_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_LONG_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "0";   // 数字默认为0
                    } else if (ArrayUtils.contains(Constants.SQL_DATE_TIME_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_DATE_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "NOW()";  // 日期默认为当前时间
                    } else {
                        defaultValue = "''";  // 其他类型默认为空字符串
                    }
                    valuesBuilder.append("IFNULL(#{item." + fieldInfo.getPropertyName() + "}, " + defaultValue + "),");
                }
                // 其他可空字段，直接使用值
                else {
                    valuesBuilder.append("#{item." + fieldInfo.getPropertyName() + "},");
                }
            }
            String valuesStr = valuesBuilder.substring(0, valuesBuilder.lastIndexOf(","));
            bw.write(valuesStr + ")");
            bw.newLine();
            bw.write("\t\t</foreach>");
            bw.newLine();
            bw.write("\t</insert>");
            bw.newLine();
            bw.newLine();


            // 批量插入或更新
            bw.write("\t<!-- 批量插入或更新 (批量插入或更新)-->");
            bw.newLine();
            bw.write("\t<insert id=\"insertOrUpdateBatch\" parameterType=\"" + poClass + "\">");
            bw.newLine();
            bw.write("\t\tINSERT INTO " + tableInfo.getTableName() + "(" + insertFieldBufferStr + ") VALUES");
            bw.newLine();
            bw.write("\t\t<foreach collection=\"list\" item=\"item\" separator=\",\">");
            bw.newLine();

            StringBuilder batchValuesBuilder = new StringBuilder();
            batchValuesBuilder.append("\t\t\t(");
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    continue;
                }
                // 对于主键字段，直接使用值（不能为空，不提供默认值）
                if (primaryKeyMap.get(fieldInfo.getFieldName()) != null) {
                    batchValuesBuilder.append("#{item." + fieldInfo.getPropertyName() + "},");
                }
                // 对于唯一索引字段，不提供默认值，要求调用方必须显式设置
                // 这样可以避免多条记录的唯一字段都使用相同的默认值导致冲突
                else if (uniqueIndexMap.get(fieldInfo.getFieldName()) != null) {
                    batchValuesBuilder.append("#{item." + fieldInfo.getPropertyName() + "},");
                }
                // 对于NOT NULL的非主键、非唯一索引字段，使用IFNULL提供默认值
                else if (fieldInfo.getIsNullable() != null && !fieldInfo.getIsNullable()) {
                    String defaultValue = "";
                    if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "''";  // 字符串默认为空字符串
                    } else if (ArrayUtils.contains(Constants.SQL_INTEGER_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_LONG_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "0";   // 数字默认为0
                    } else if (ArrayUtils.contains(Constants.SQL_DATE_TIME_TYPES, fieldInfo.getSqlType()) ||
                            ArrayUtils.contains(Constants.SQL_DATE_TYPES, fieldInfo.getSqlType())) {
                        defaultValue = "NOW()";  // 日期默认为当前时间
                    } else {
                        defaultValue = "''";  // 其他类型默认为空字符串
                    }
                    batchValuesBuilder.append("IFNULL(#{item." + fieldInfo.getPropertyName() + "}, " + defaultValue + "),");
                }
                // 可空字段，直接使用值
                else {
                    batchValuesBuilder.append("#{item." + fieldInfo.getPropertyName() + "},");
                }
            }
            String batchValuesStr = batchValuesBuilder.substring(0, batchValuesBuilder.lastIndexOf(","));
            bw.write(batchValuesStr + ")");
            bw.newLine();
            bw.write("\t\t</foreach>");
            bw.newLine();

            bw.write("\t\ton DUPLICATE key update");
            StringBuffer insertBatchUpdateBuffer = new StringBuffer();
            for (FieldInfo fieldInfo:tableInfo.getFieldList()) {
                if (fieldInfo.getAutoIncrement() != null && fieldInfo.getAutoIncrement()) {
                    continue;
                }
                insertBatchUpdateBuffer.append(fieldInfo.getFieldName() + " = VALUES(" + fieldInfo.getFieldName() + "),");
            }
            String insertBatchUpdateBufferStr = insertBatchUpdateBuffer.substring(0, insertBatchUpdateBuffer.lastIndexOf(","));
            bw.write(" "+insertBatchUpdateBufferStr);
            bw.newLine();
            bw.write("\t</insert>");
            bw.newLine();
            bw.newLine();

            // 根据主键更新
            for (Map.Entry<String, List<FieldInfo>> entry : keyIndexMap.entrySet()) {
                List<FieldInfo> keyFieldInfoList = entry.getValue();

                Integer index = 0;
                StringBuilder methodName = new StringBuilder();

                StringBuilder paramsNames = new StringBuilder();
                for (FieldInfo fieldInfo : keyFieldInfoList) {
                    index++;
                    methodName.append(StringUtils.upperCaseFirstLetter(fieldInfo.getPropertyName()));
                    paramsNames.append(fieldInfo.getFieldName() + "=#{" + fieldInfo.getPropertyName() + "}");
                    if (index < keyFieldInfoList.size()) {
                        methodName.append("And");
                        paramsNames.append(" and ");
                    }
                }
                bw.newLine();
                bw.write("\t<!-- 根据" + methodName + "查询-->");
                bw.newLine();
                bw.write("\t<select id=\"selectBy" + methodName + "\" resultMap=\"base_result_map\">");
                bw.newLine();
                bw.write("\t\tselect <include refid=\"" + BASE_COLUMN_LIST + "\"/> from " + tableInfo.getTableName() + " where " + paramsNames);
                bw.newLine();
                bw.write("\t</select>");
                bw.newLine();
                bw.newLine();

                bw.write("\t<!-- 根据" + methodName + "更新-->");
                bw.newLine();
                bw.write("\t<update id=\"updateBy" + methodName + "\" parameterType=\"" + poClass + "\">");
                bw.newLine();
                bw.write("\t\tupdate " + tableInfo.getTableName());
                bw.newLine();
                bw.write("\t\t<set>");
                bw.newLine();

                for (FieldInfo fieldInfo : tableInfo.getFieldList()) {
                    bw.write("\t\t\t<if test=\"bean." + fieldInfo.getFieldName() + " != null\">");
                    bw.newLine();
                    bw.write("\t\t\t\t"+fieldInfo.getFieldName() + " = #{bean." + fieldInfo.getPropertyName() + "},");
                    bw.newLine();
                    bw.write("\t\t\t</if>");
                    bw.newLine();
                }
                bw.write("\t\t</set>");
                bw.newLine();
                bw.write("\t\twhere " + paramsNames);
                bw.newLine();
                bw.write("\t</update>");
                bw.newLine();
                bw.newLine();

                bw.write("\t<!-- 根据" + methodName + "删除-->");
                bw.newLine();
                bw.write("\t<delete id=\"deleteBy" + methodName + "\">");
                bw.newLine();
                bw.write("\t\tdelete from " + tableInfo.getTableName() + " where " + paramsNames);
                bw.newLine();
                bw.write("\t</delete>");
                bw.newLine();
                bw.newLine();
            }

            bw.newLine();

            bw.write("</mapper>");
            bw.flush();

        } catch (Exception e) {
            logger.error("创建mapper XML失败", e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (outw != null) {
                try {
                    outw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}