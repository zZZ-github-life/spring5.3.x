/**
 * 为开源缓存 <a href="https://www.ehcache.org/">EhCache 2.x</a> 提供支持类，
 * 允许在 Spring 上下文中将 EhCache CacheManager 和 Cache 设置为 bean。
 *
 * <p>注意：EhCache 3.x 位于不同的包命名空间中，
 * 不在此处的传统支持类范围内。
 * 相反，请考虑通过 JCache (JSR-107) 使用它，
 * 使用 Spring 在 {@code org.springframework.cache.jcache} 中的支持。
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.ehcache;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
