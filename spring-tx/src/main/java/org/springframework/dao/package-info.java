/**
 * 异常层次结构，支持独立于所使用的数据访问方式的复杂错误处理。
 * 例如，当 DAO 和数据访问框架使用本包中的异常（以及自定义子类）时，
 * 调用代码可以检测和处理常见问题（如死锁），而无需绑定到特定的数据访问策略
 * （如 JDBC）。
 *
 * <p>所有这些异常都是非受检异常，意味着调用代码可以不捕获它们，
 * 将所有数据访问异常视为致命错误。
 *
 * <p>本包中的类在 Rod Johnson 所著的
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>
 * （Wrox, 2002）第 9 章中有详细讨论。
 */
@NonNullApi
@NonNullFields
package org.springframework.dao;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
