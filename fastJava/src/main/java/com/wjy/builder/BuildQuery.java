package com.wjy.builder;

import com.wjy.bean.Constants;
import com.wjy.bean.FieldInfo;
import com.wjy.bean.TableInfo;
import com.wjy.utils.DateUtils;
import com.wjy.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BuildQuery {
    private static final Logger logger = LoggerFactory.getLogger(BuildQuery.class);
    public static void execute(TableInfo tableInfo) throws IOException {
        File folder = new File(Constants.PATH_QUERY);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String className = tableInfo.getBeanName() +Constants.SUFFIX_BEAN_QUERY;

        File poFile = new File(folder, className + ".java");

        OutputStream out = null;
        OutputStreamWriter outw = null;
        BufferedWriter bw = null;
        try {
            out = new FileOutputStream(poFile);
            outw = new OutputStreamWriter(out, "utf8");
            bw = new BufferedWriter(outw);

            bw.write("package " + Constants.PACKAGE_QUERY + ";");
            bw.newLine();
            bw.newLine();

            if (tableInfo.getHaveBigDecimal()) {
                bw.write("import java.math.BigDecimal;");
                bw.newLine();
            }

            if (tableInfo.getHaveDate() || tableInfo.getHaveDateTime()) {
                bw.write("import java.util.Date;");
                bw.newLine();
            }


            bw.newLine();
            bw.newLine();
            // 构建类注解
            BuildComment.createClassComment(bw, tableInfo.getComment() + "查询对象");
            bw.write("public class " + className + " extends BaseQuery {");
            bw.newLine();

            for (FieldInfo field:tableInfo.getFieldList()) {
                BuildComment.createFieldComment(bw, field.getComment());
                bw.write("\tprivate " + field.getJavaType() + " " + field.getPropertyName() + ";");
                bw.newLine();
                bw.newLine();

                // String类型的参数
                if (ArrayUtils.contains(Constants.SQL_STRING_TYPES, field.getSqlType())) {
                    String propertyName = field.getPropertyName() + Constants.SUFFIX_BEAN_QUERY_FUZZY;
                    bw.write("\tprivate String " + propertyName + ";");  // 改为String类型
                    bw.newLine();
                    bw.newLine();
                }

                if (ArrayUtils.contains(Constants.SQL_DATE_TYPES, field.getSqlType()) || ArrayUtils.contains(Constants.SQL_DATE_TIME_TYPES, field.getSqlType())) {
                    bw.write("\tprivate String " + field.getPropertyName() + Constants.SUFFIX_BEAN_QUERY_TIME_START + ";");
                    bw.newLine();
                    bw.newLine();

                    bw.write("\tprivate String " + field.getPropertyName() + Constants.SUFFIX_BEAN_QUERY_TIME_END + ";");
                    bw.newLine();
                    bw.newLine();
                }
            }

            buildGetSet(bw, tableInfo.getFieldList());
            buildGetSet(bw, tableInfo.getFieldExtendList());

            bw.write("}");
            bw.flush();

        } catch (Exception e) {
            logger.error("创建po失败", e);
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

    private static void buildGetSet(BufferedWriter bw, List<FieldInfo> fieldInfoList) throws Exception {
        for (FieldInfo field: fieldInfoList) {
            String tempField = StringUtils.upperCaseFirstLetter(field.getPropertyName());
            
            // 检查是否是扩展字段（模糊查询或时间范围字段）
            boolean isExtendField = field.getPropertyName().endsWith(Constants.SUFFIX_BEAN_QUERY_FUZZY) || 
                                   field.getPropertyName().endsWith(Constants.SUFFIX_BEAN_QUERY_TIME_START) || 
                                   field.getPropertyName().endsWith(Constants.SUFFIX_BEAN_QUERY_TIME_END);
            
            // 对于扩展字段，使用String类型；对于普通字段，使用字段的Java类型
            String javaType = isExtendField ? "String" : field.getJavaType();
            
            bw.write("\tpublic void set" + tempField + "(" + javaType + " " + field.getPropertyName() + ") {");
            bw.newLine();
            bw.write("\t\tthis." + field.getPropertyName() + " = " + field.getPropertyName() + ";");
            bw.newLine();
            bw.write("\t}");
            bw.newLine();
            bw.newLine();
    
            bw.write("\tpublic "+ javaType +" get" + tempField + "() {");
            bw.newLine();
            bw.write("\t\treturn this." + field.getPropertyName() + ";");
            bw.newLine();
            bw.write("\t}");
            bw.newLine();
            bw.newLine();
        }
    }
}