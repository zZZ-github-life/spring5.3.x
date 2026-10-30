/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.logging;

/**
 * Apache Commons Logging 的 {@code LogFactory} API 的一个最小化实现，仅提供常用的 {@link Log} 查找
 * 方法。它受 JCL-over-SLF4J 桥接器启发，应在源码和二进制层面都与 Commons Logging API 的所有常见用
 * 法兼容（尤其是 {@code LogFactory.getLog(Class/String)} 字段初始化器）。
 *
 * <p>此实现不支持 Commons Logging 原有的提供者检测机制。它只检查 Spring Framework 的类路径中是否存
 * 在 Log4j 2.x API 和 SLF4J 1.7 API，若两者都不可用则回退到 {@code java.util.logging}。从这个意义
 * 上说，它可同时替代 Log4j 2 Commons Logging 桥接器和 JCL-over-SLF4J 桥接器；因此在基于 Spring 的
 * 配置中，两者都不再需要（也无需再手动排除标准的 Commons Logging API jar）。此外，对于没有外部日志
 * 提供者的简单配置，Spring 不再要求类路径中额外引入 jar，因为在这种场景下内嵌的日志工厂会自动委托
 * 给 {@code java.util.logging}。
 *
 * <p><b>请注意，此 Commons Logging 变体仅用于核心框架及扩展中的基础设施日志记录目的。</b>它也为使
 * 用 Commons Logging API 的第三方库（例如 Apache HttpClient 和 HtmlUnit）充当通用桥接器，使它们无
 * 需额外的桥接 jar 就能纳入同一套一致的配置。
 *
 * <p><b>对于应用代码中的日志需求，建议直接使用 Log4j 2.x、SLF4J 或 {@code java.util.logging}。</b>
 * 只需将 Log4j 2.x 或 Logback（或其他 SLF4J 提供者）放入类路径，无需任何额外的桥接器，框架会自动适
 * 配你的选择。
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 */
public abstract class LogFactory {

	/**
	 * 便捷方法：返回指定名称的日志记录器。
	 * @param clazz 包含用于派生日志名称的 Class
	 */
	public static Log getLog(Class<?> clazz) {
		return getLog(clazz.getName());
	}

	/**
	 * 便捷方法：返回指定名称的日志记录器。
	 * @param name 要返回的 <code>Log</code> 实例的逻辑名称
	 */
	public static Log getLog(String name) {
		return LogAdapter.createLog(name);
	}


	/**
	 * 此方法仅为兼容 Commons Logging API 的非常规用法而存在，例如
	 * {@code LogFactory.getFactory().getInstance(Class/String)}。
	 * @see #getInstance(Class)
	 * @see #getInstance(String)
	 * @deprecated in favor of {@link #getLog(Class)}/{@link #getLog(String)}
	 */
	@Deprecated
	public static LogFactory getFactory() {
		return new LogFactory() {};
	}

	/**
	 * 便捷方法：返回指定名称的日志记录器。<p>此变体直接分派给 {@link #getLog(Class)}。
	 * @param clazz 包含用于派生日志名称的 Class
	 * @deprecated in favor of {@link #getLog(Class)}
	 */
	@Deprecated
	public Log getInstance(Class<?> clazz) {
		return getLog(clazz);
	}

	/**
	 * 便捷方法：返回指定名称的日志记录器。<p>此变体直接分派给 {@link #getLog(String)}。
	 * @param name 要返回的 <code>Log</code> 实例的逻辑名称
	 * @deprecated in favor of {@link #getLog(String)}
	 */
	@Deprecated
	public Log getInstance(String name) {
		return getLog(name);
	}

}
