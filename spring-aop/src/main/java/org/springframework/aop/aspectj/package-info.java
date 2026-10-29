/**
 * AspectJ 集成包。包括针对 AspectJ 5 注解风格方法的 Spring AOP 通知实现，以及一个
 * AspectJExpressionPointcut：一种允许在 Spring AOP 运行时框架中使用 AspectJ 切点表达式语言的
 * Spring AOP Pointcut 实现。
 *
 * <p>请注意，使用此包<i>并不</i>需要使用 {@code ajc} 编译器或 AspectJ 加载时织入器。它的目的是在基
 * 于代理的 Spring AOP 框架中，以一致的语义使用 AspectJ 功能中一个有价值的子集。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.aspectj;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
