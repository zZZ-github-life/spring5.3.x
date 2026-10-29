/**
 * 本包中的类将 RDBMS 查询、更新和存储过程表示为线程安全、可重用的对象。
 * 这种方式借鉴了 JDO 的模型，当然查询返回的对象是与数据库“断开的”。
 *
 * <p>这种更高层级的 JDBC 抽象依赖于 {@code org.springframework.jdbc.core}
 * 包中的较低层级抽象。抛出的异常与 {@code org.springframework.dao} 包中的
 * 一致，这意味着使用本包的代码无需实现 JDBC 或 RDBMS 特定的错误处理。
 *
 * <p>本包及相关包在 Rod Johnson 所著《Expert One-On-One J2EE Design and Development》
 * （Wrox，2002 年）第 9 章中有所讨论：
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>。
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc.object;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
