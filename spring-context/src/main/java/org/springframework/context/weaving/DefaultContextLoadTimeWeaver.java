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

package org.springframework.context.weaving;

import java.lang.instrument.ClassFileTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver;
import org.springframework.instrument.classloading.glassfish.GlassFishLoadTimeWeaver;
import org.springframework.instrument.classloading.jboss.JBossLoadTimeWeaver;
import org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver;
import org.springframework.instrument.classloading.weblogic.WebLogicLoadTimeWeaver;
import org.springframework.instrument.classloading.websphere.WebSphereLoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于应用上下文的默认 {@link LoadTimeWeaver} Bean，它装饰一个自动检测到的内部
 * {@code LoadTimeWeaver}。
 *
 * <p>通常以默认 Bean 名称 "{@code loadTimeWeaver}" 注册；最方便的实现方式是使用 Spring 的
 * {@code <context:load-time-weaver>} XML 标签，或在 {@code @Configuration} 类上使用
 * {@code @EnableLoadTimeWeaving}。
 *
 * <p>此类执行运行时环境检查，以获取合适的织入器实现。自 Spring Framework 5.0 起，它可以检测 Oracle
 * WebLogic 10+、GlassFish 4+、Tomcat 8+、WildFly 8+、IBM WebSphere 8.5+、
 * {@link InstrumentationSavingAgent Spring's VM agent}，以及 Spring 的
 * {@link ReflectiveLoadTimeWeaver} 所支持的任何 {@link ClassLoader}（例如 Liberty 的）。
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Costin Leau
 * @since 2.5
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public class DefaultContextLoadTimeWeaver implements LoadTimeWeaver, BeanClassLoaderAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private LoadTimeWeaver loadTimeWeaver;


	public DefaultContextLoadTimeWeaver() {
	}

	public DefaultContextLoadTimeWeaver(ClassLoader beanClassLoader) {
		setBeanClassLoader(beanClassLoader);
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		LoadTimeWeaver serverSpecificLoadTimeWeaver = createServerSpecificLoadTimeWeaver(classLoader);
		if (serverSpecificLoadTimeWeaver != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Determined server-specific load-time weaver: " +
						serverSpecificLoadTimeWeaver.getClass().getName());
			}
			this.loadTimeWeaver = serverSpecificLoadTimeWeaver;
		}
		else if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			logger.debug("Found Spring's JVM agent for instrumentation");
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(classLoader);
		}
		else {
			try {
				this.loadTimeWeaver = new ReflectiveLoadTimeWeaver(classLoader);
				if (logger.isDebugEnabled()) {
					logger.debug("Using reflective load-time weaver for class loader: " +
							this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
				}
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException(ex.getMessage() + " Specify a custom LoadTimeWeaver or start your " +
						"Java virtual machine with Spring's agent: -javaagent:spring-instrument-{version}.jar");
			}
		}
	}

	/*
	 * This method never fails, allowing to try other possible ways to use a
	 * server-agnostic weaver. This non-failure logic is required since
	 * determining a load-time weaver based on the ClassLoader name alone may
	 * legitimately fail due to other mismatches.
	 */
	@Nullable
	protected LoadTimeWeaver createServerSpecificLoadTimeWeaver(ClassLoader classLoader) {
		String name = classLoader.getClass().getName();
		try {
			if (name.startsWith("org.apache.catalina")) {
				return new TomcatLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.glassfish")) {
				return new GlassFishLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.jboss.modules")) {
				return new JBossLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("com.ibm.ws.classloader")) {
				return new WebSphereLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("weblogic")) {
				return new WebLogicLoadTimeWeaver(classLoader);
			}
		}
		catch (Exception ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Could not obtain server-specific LoadTimeWeaver: " + ex.getMessage());
			}
		}
		return null;
	}

	@Override
	public void destroy() {
		if (this.loadTimeWeaver instanceof InstrumentationLoadTimeWeaver) {
			if (logger.isDebugEnabled()) {
				logger.debug("Removing all registered transformers for class loader: " +
						this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
			}
			((InstrumentationLoadTimeWeaver) this.loadTimeWeaver).removeTransformers();
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.state(this.loadTimeWeaver != null, "Not initialized");
		this.loadTimeWeaver.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		Assert.state(this.loadTimeWeaver != null, "Not initialized");
		return this.loadTimeWeaver.getInstrumentableClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		Assert.state(this.loadTimeWeaver != null, "Not initialized");
		return this.loadTimeWeaver.getThrowawayClassLoader();
	}

}
