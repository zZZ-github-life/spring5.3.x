/**
 * 此包中的类使 JDBC 更易于使用，并
 * 降低常见错误发生的可能性。具体而言，它们：
 * <ul>
 * <li>简化错误处理，避免在应用程序代码中需要 try/catch/finally
 * 块。
 * <li>以通用的非受检异常层次结构向应用程序代码呈现异常，
 * 使应用程序能够捕获数据访问异常而不必依赖 JDBC，
 * 并且可以忽略那些捕获也没有意义的致命异常。
 * <li>允许修改错误处理的实现，以针对不同的 RDBMS，
 * 而无需在应用程序代码中引入专有依赖。
 * </ul>
 *
 * <p>此包及相关包在 Rod Johnson 所著
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * （Wrox，2002 年）一书的第 9 章中进行了讨论。
 */
@NonNullApi
@NonNullFields
package org.springframework.jdbc;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
