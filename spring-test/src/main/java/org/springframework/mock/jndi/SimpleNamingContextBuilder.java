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

package org.springframework.mock.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * JNDI 命名上下文构建器的简单实现。
 *
 * <p>主要面向测试环境，其中每个测试用例都可以
 * 适当地配置 JNDI，以便 {@code new InitialContext()}
 * 暴露所需的对象。也可用于独立应用程序，
 * 例如将 JDBC DataSource 绑定到众所周知的 JNDI 位置，
 * 从而能够在 Java EE 容器之外使用传统的 Java EE
 * 数据访问代码。
 *
 * <p>DataSource 实现有多种选择：
 * <ul>
 * <li>{@code SingleConnectionDataSource}（所有 getConnection 调用使用同一个 Connection）
 * <li>{@code DriverManagerDataSource}（每次 getConnection 调用创建一个新 Connection）
 * <li>Apache 的 Commons DBCP 提供 {@code org.apache.commons.dbcp.BasicDataSource}（真正的连接池）
 * </ul>
 *
 * <p>引导代码中的典型用法：
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
 * DataSource ds = new DriverManagerDataSource(...);
 * builder.bind("java:comp/env/jdbc/myds", ds);
 * builder.activate();</pre>
 *
 * 请注意，由于 JNDI 的限制，无法在同一个 JVM 中激活多个构建器。
 * 因此，要反复配置新的构建器，请使用以下代码获取已激活构建器
 * 或新激活构建器的引用：
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
 * DataSource ds = new DriverManagerDataSource(...);
 * builder.bind("java:comp/env/jdbc/myds", ds);</pre>
 *
 * 请注意，<i>不应</i>对此工厂方法返回的构建器调用
 * {@code activate()}，因为在任何情况下都已经有一个已激活的构建器。
 *
 * <p>此类实例仅在设置阶段需要。
 * 激活后，应用程序无需保留对它的引用。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see #emptyActivatedContextBuilder()
 * @see #bind(String, Object)
 * @see #activate()
 * @see SimpleNamingContext
 * @see org.springframework.jdbc.datasource.SingleConnectionDataSource
 * @see org.springframework.jdbc.datasource.DriverManagerDataSource
 * @deprecated Deprecated as of Spring Framework 5.2 in favor of complete solutions from
 * third parties such as <a href="https://github.com/h-thurow/Simple-JNDI">Simple-JNDI</a>
 */
@Deprecated
public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder {

	/** 绑定到 JNDI 的此类实例。 */
	@Nullable
	private static volatile SimpleNamingContextBuilder activated;

	private static boolean initialized = false;

	private static final Object initializationLock = new Object();


	/**
	 * 检查是否有 SimpleNamingContextBuilder 处于活动状态。
	 * @return the current SimpleNamingContextBuilder instance,
	 * or {@code null} if none
	 */
	@Nullable
	public static SimpleNamingContextBuilder getCurrentContextBuilder() {
		return activated;
	}

	/**
	 * 如果尚没有 SimpleNamingContextBuilder 在配置 JNDI，
	 * 则创建并激活一个。否则，获取现有的已激活
	 * SimpleNamingContextBuilder，清除它并返回。
	 * <p>这主要面向希望反复从头
	 * 重新初始化 JNDI 绑定的测试套件。
	 * @return an empty SimpleNamingContextBuilder that can be used
	 * to control JNDI bindings
	 */
	public static SimpleNamingContextBuilder emptyActivatedContextBuilder() throws NamingException {
		SimpleNamingContextBuilder builder = activated;
		if (builder != null) {
			// Clear already activated context builder.
			builder.clear();
		}
		else {
			// Create and activate new context builder.
			builder = new SimpleNamingContextBuilder();
			// The activate() call will cause an assignment to the activated variable.
			builder.activate();
		}
		return builder;
	}


	private final Log logger = LogFactory.getLog(getClass());

	private final Hashtable<String,Object> boundObjects = new Hashtable<>();


	/**
	 * 通过向 JNDI NamingManager 注册来注册此上下文构建器。
	 * 请注意，一旦完成此操作，{@code new InitialContext()} 将始终
	 * 返回来自此工厂的上下文。可使用 {@code emptyActivatedContextBuilder()}
	 * 静态方法获取空上下文（例如在测试方法中）。
	 * @throws IllegalStateException if there's already a naming context builder
	 * registered with the JNDI NamingManager
	 */
	public void activate() throws IllegalStateException, NamingException {
		logger.info("Activating simple JNDI environment");
		synchronized (initializationLock) {
			if (!initialized) {
				Assert.state(!NamingManager.hasInitialContextFactoryBuilder(),
							"Cannot activate SimpleNamingContextBuilder: there is already a JNDI provider registered. " +
							"Note that JNDI is a JVM-wide service, shared at the JVM system class loader level, " +
							"with no reset option. As a consequence, a JNDI provider must only be registered once per JVM.");
				NamingManager.setInitialContextFactoryBuilder(this);
				initialized = true;
			}
		}
		activated = this;
	}

	/**
	 * 临时停用此上下文构建器。它仍将注册到
	 * JNDI NamingManager，但会委托给标准 JNDI InitialContextFactory
	 * （如果已配置），而不是暴露其自身绑定的对象。
	 * <p>再次调用 {@code activate()} 即可重新暴露此上下文构建器自身
	 * 绑定的对象。此类激活/停用序列可以应用任意多次
	 * （例如在同一个 VM 中运行的大型集成测试套件内）。
	 * @see #activate()
	 */
	public void deactivate() {
		logger.info("Deactivating simple JNDI environment");
		activated = null;
	}

	/**
	 * 清除此上下文构建器中的所有绑定，同时保持其处于活动状态。
	 */
	public void clear() {
		this.boundObjects.clear();
	}

	/**
	 * 针对此上下文构建器将生成的所有命名上下文，
	 * 将给定对象绑定到给定名称下。
	 * @param name the JNDI name of the object (e.g. "java:comp/env/jdbc/myds")
	 * @param obj the object to bind (e.g. a DataSource implementation)
	 */
	public void bind(String name, Object obj) {
		if (logger.isInfoEnabled()) {
			logger.info("Static JNDI binding: [" + name + "] = [" + obj + "]");
		}
		this.boundObjects.put(name, obj);
	}


	/**
	 * 简单的 InitialContextFactoryBuilder 实现，
	 * 创建一个新的 SimpleNamingContext 实例。
	 * @see SimpleNamingContext
	 */
	@Override
	@SuppressWarnings("unchecked")
	public InitialContextFactory createInitialContextFactory(@Nullable Hashtable<?,?> environment) {
		if (activated == null && environment != null) {
			Object icf = environment.get(Context.INITIAL_CONTEXT_FACTORY);
			if (icf != null) {
				Class<?> icfClass;
				if (icf instanceof Class) {
					icfClass = (Class<?>) icf;
				}
				else if (icf instanceof String) {
					icfClass = ClassUtils.resolveClassName((String) icf, getClass().getClassLoader());
				}
				else {
					throw new IllegalArgumentException("Invalid value type for environment key [" +
							Context.INITIAL_CONTEXT_FACTORY + "]: " + icf.getClass().getName());
				}
				if (!InitialContextFactory.class.isAssignableFrom(icfClass)) {
					throw new IllegalArgumentException(
							"Specified class does not implement [" + InitialContextFactory.class.getName() + "]: " + icf);
				}
				try {
					return (InitialContextFactory) ReflectionUtils.accessibleConstructor(icfClass).newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to instantiate specified InitialContextFactory: " + icf, ex);
				}
			}
		}

		// Default case...
		return env -> new SimpleNamingContext("", this.boundObjects, (Hashtable<String, Object>) env);
	}

}
