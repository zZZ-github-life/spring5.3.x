/**
 * 声明式 JSR-107 缓存配置的支持包。
 * 当 Spring 的常规缓存配置检测到 JSR-107 API 和 Spring 的 JCache 实现时使用。
 *
 * <p>提供 {@code CachingConfigurer} 的扩展，暴露所使用的异常缓存解析器，
 * 参见 {@code JCacheConfigurer}。
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.jcache.config;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
