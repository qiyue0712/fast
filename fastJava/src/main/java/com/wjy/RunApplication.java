package com.wjy;

import com.wjy.bean.TableInfo;
import com.wjy.builder.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class RunApplication {
    public static void main(String[] args) throws SQLException, IOException {

       List<TableInfo> tableInfoList = BuildTable.getTables();

        BuildBase.execute();

       for (TableInfo tableInfo : tableInfoList) {
           BuildPo.execute(tableInfo);

           BuildQuery.execute(tableInfo);

           BuildMapper.execute(tableInfo);

           BuildMapperXml.execute(tableInfo);

           BuildService.execute(tableInfo);

           BuildServiceImpl.execute(tableInfo);

           BuildController.execute(tableInfo);
       }
    }
}