package com.example.lcx.config;

import vn.com.lcx.common.annotation.Instance;
import vn.com.lcx.common.annotation.InstanceClass;
import vn.com.lcx.common.database.pool.LCXDataSource;

@InstanceClass
public class DatabaseConfig {

    @Instance
    public LCXDataSource datasource() {

    }

}
