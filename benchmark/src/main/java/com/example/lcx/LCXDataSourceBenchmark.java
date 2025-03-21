package com.example.lcx;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import vn.com.lcx.common.database.pool.LCXDataSource;
import vn.com.lcx.common.database.pool.entry.ConnectionEntry;
import vn.com.lcx.common.database.type.DBTypeEnum;

import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class LCXDataSourceBenchmark {

    private LCXDataSource dataSource;

    @Setup(Level.Trial)
    public void setup() {
        this.dataSource = LCXDataSource.init(
                "localhost",
                5432,
                "postgres",
                "postgres",
                "my_database",
                "org.postgresql.Driver",
                10,
                20,
                5,
                DBTypeEnum.POSTGRESQL
        );
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (dataSource != null) {
            // dataSource.close();
            System.out.println("closed");
        }
    }

    @Benchmark
    public void testBorrowAndReturnConnection() throws Exception {
        try (ConnectionEntry conn = dataSource.getConnection()) {
            // Giả lập thao tác ngắn (không thực hiện truy vấn)
            System.out.println("borrowed connection");
        }
    }

}
