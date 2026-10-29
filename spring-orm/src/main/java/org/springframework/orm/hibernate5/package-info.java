/**
 * 提供将 <a href="https://hibernate.org/">Hibernate 5.x</a>
 * 与 Spring 概念集成的包。
 *
 * <p>包含用于本地 Hibernate 事务的 Spring 事务 SPI 实现。
 * 此包有意保持相当精简，不提供模板类之类的内容，
 * 以便尽可能遵循 Hibernate 的建议。我们建议
 * 使用 Hibernate 原生的 <code>sessionFactory.getCurrentSession()</code> 风格。
 *
 * <p><b>此包仅支持 Hibernate 5.x。</b>
 */
@NonNullApi
@NonNullFields
package org.springframework.orm.hibernate5;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
