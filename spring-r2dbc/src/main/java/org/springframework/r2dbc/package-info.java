/**
 * 此包中的类使 R2DBC 更易于使用，并减少常见错误的发生。
 * 具体而言，它们：
 * <ul>
 * <li>简化错误处理，避免应用程序代码中需要资源管理代码块。
 * <li>以通用的非受检异常层次结构向应用程序代码呈现异常，使应用能够捕获数据访问
 * 异常而不依赖于 R2DBC，并能忽略那些捕获也没有意义的致命异常。
 * <li>允许修改错误处理的实现以针对不同的关系数据库，而无需在应用程序代码中
 * 引入专有依赖。
 * </ul>
 */
@NonNullApi
@NonNullFields
package org.springframework.r2dbc;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
