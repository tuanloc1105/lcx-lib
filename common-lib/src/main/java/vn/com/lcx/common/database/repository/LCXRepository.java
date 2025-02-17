package vn.com.lcx.common.database.repository;

import vn.com.lcx.common.database.pool.entry.ConnectionEntry;

import java.util.List;
import java.util.Map;

public interface LCXRepository<T> {

    int save(ConnectionEntry connection, T entity);

    void save2(ConnectionEntry connection, T entity);

    int update(ConnectionEntry connection, T entity);

    int delete(ConnectionEntry connection, T entity);

    Map<String, Integer> save(ConnectionEntry connection, List<T> entities);

    Map<String, Integer> update(ConnectionEntry connection, List<T> entities);

    Map<String, Integer> delete(ConnectionEntry connection, List<T> entities);

}
