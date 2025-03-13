package com.example.lcx.config;

import vn.com.lcx.common.annotation.Instance;
import vn.com.lcx.common.annotation.InstanceClass;
import vn.com.lcx.common.database.pool.LCXDataSource;
import vn.com.lcx.common.database.type.DBTypeEnum;

import static vn.com.lcx.common.constant.CommonConstant.applicationConfig;

@InstanceClass
public class DatabaseConfig {

    @Instance
    public LCXDataSource datasource() {
        return LCXDataSource.init(
                applicationConfig.getProperty("database.my_database.host"),
                Integer.parseInt(applicationConfig.getProperty("database.my_database.port")),
                applicationConfig.getProperty("database.my_database.username"),
                applicationConfig.getProperty("database.my_database.password"),
                applicationConfig.getProperty("database.my_database.name"),
                applicationConfig.getProperty("database.my_database.driver_class_name"),
                Integer.parseInt(applicationConfig.getProperty("database.initial_pool_size")),
                Integer.parseInt(applicationConfig.getProperty("database.max_pool_size")),
                Integer.parseInt(applicationConfig.getProperty("database.max_timeout")),
                DBTypeEnum.POSTGRESQL
        );
    }

}
