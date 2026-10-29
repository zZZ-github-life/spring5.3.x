/**
 * JDBC 框架的支持类，供 jdbc.core 和 jdbc.object 包中的类使用。
 * 提供从 SQLException 到 Spring 通用 DataAccessException 的转换器。
 *
 * <p>可以独立使用，例如在自定义的 JDBC 访问代码中，或在基于 JDBC 的
 * O/R 映射层中。
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.support;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
