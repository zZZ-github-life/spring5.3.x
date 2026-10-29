/**
 * Spring AOP 核心接口，构建于 AOP Alliance AOP 互操作接口之上。
 *
 * <p>任何 AOP Alliance MethodInterceptor 都可以在 Spring 中使用。
 *
 * <br>Spring AOP 还提供：
 * <ul>
 * <li>引入（Introduction）支持
 * <li>Pointcut 抽象，支持“静态”切点（基于类和方法的）和“动态”切点（还会考虑方法参数）。目前尚无针对切点的 AOP Alliance 接口。
 * <li>完整的通知类型，包括环绕、前置、返回后和抛出后通知。
 * <li>可扩展性，允许在不修改核心框架的情况下插入任意自定义通知类型。
 * </ul>
 *
 * <p>Spring AOP 可以通过编程方式使用，或者（更推荐）与 Spring IoC 容器集成。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
