/**
 * 用于 ApplicationContext 的 Bean 后处理器，通过自动创建 AOP 代理来简化 AOP 的使用，无需使用
 * ProxyFactoryBean。
 *
 * <p>此包中的各种后处理器只需添加到 ApplicationContext 中（通常是在 XML Bean 定义文档中），即可自
 * 动代理选定的 Bean。
 *
 * <p><b>NB</b>：BeanFactory 实现不支持自动自动代理，因为后处理器 Bean 只会在应用上下文中被自动检测
 * 到。相反，可以在 ConfigurableBeanFactory 上显式注册后处理器。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework.autoproxy;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
