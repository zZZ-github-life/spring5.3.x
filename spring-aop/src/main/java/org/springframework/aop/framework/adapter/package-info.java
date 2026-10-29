/**
 * SPI 包，允许 Spring AOP 框架处理任意 advice 类型。
 *
 * <p>只想<i>使用</i> Spring AOP 框架而不想扩展其功能的用户无需关注此包。
 *
 * <p>您可能希望使用这些适配器将 Spring 特有的 advice（如 MethodBeforeAdvice）包装到
 * MethodInterceptor 中，以便在另一个支持 AOP Alliance 接口的 AOP 框架中使用它们。
 *
 * <p>这些适配器不依赖任何其他 Spring 框架类，即可实现此类用法。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework.adapter;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
