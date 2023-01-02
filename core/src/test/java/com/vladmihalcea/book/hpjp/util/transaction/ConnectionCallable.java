package com.vladmihalcea.book.hpjp.util.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
@FunctionalInterface
public interface ConnectionCallable<T> {
	T execute(Connection connection) throws SQLException;
}
