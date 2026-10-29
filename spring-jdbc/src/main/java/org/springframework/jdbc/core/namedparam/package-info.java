/**
 * 支持命名参数的 JdbcTemplate 变体。
 *
 * <p>NamedParameterJdbcTemplate 是 JdbcTemplate 的包装器，增加了对命名参数
 * 解析的支持。它没有实现 JdbcOperations 接口，也没有继承 JdbcTemplate，
 * 而是实现了专用的 NamedParameterJdbcOperations 接口。
 *
 * <P>如果你需要 Spring JDBC 的全部能力来处理不太常见的操作，可以使用
 * NamedParameterJdbcTemplate 的 {@code getJdbcOperations()} 方法，
 * 并基于返回的经典模板进行操作，或者直接使用 JdbcTemplate 实例。
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.core.namedparam;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
