/**
 * 表插入和存储过程调用的简化层。
 *
 * <p>{@code SimpleJdbcInsert} 和 {@code SimpleJdbcCall} 利用 JDBC 驱动提供的
 * 数据库元数据来简化应用程序代码。大部分参数说明变得不再必要，
 * 因为它们可以从元数据中查找获得。
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.core.simple;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
