/**
 * 自 Spring Framework 5.2 起<strong>已废弃</strong>，请改用第三方完整解决方案，
 * 例如 <a href="https://github.com/h-thurow/Simple-JNDI">Simple-JNDI</a>。
 *
 * <p>可能可行的最简 JNDI SPI 实现。
 *
 * <p>可用于为测试套件或独立应用程序设置简单的 JNDI 环境。
 * 例如，如果将 JDBC DataSource 绑定到与 Java EE 容器中相同的
 * JNDI 名称，则应用程序代码和配置都可以原样复用，无需更改。
 */
@NonNullApi
@NonNullFields
package org.springframework.mock.jndi;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
