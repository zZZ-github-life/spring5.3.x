/**
 * 基于 AOP 的声明式缓存划分解决方案，使用 JSR-107 注解。<p>强烈依赖于 org.springframework.cache.interceptor 中处理 Spring 缓存注解的基础设施。<p>构建于 org.springframework.aop.framework 中的 AOP 基础设施之上。任何 POJO 都可以通过 Spring 获得缓存通知。
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.jcache.interceptor;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
