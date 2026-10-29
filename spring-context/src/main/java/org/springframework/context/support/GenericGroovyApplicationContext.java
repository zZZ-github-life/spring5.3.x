/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 一个 {@link org.springframework.context.ApplicationContext} 实现，它继承
 * {@link GenericApplicationContext} 并实现 {@link GroovyObject}，从而可以使用点解引用语法而不是
 * {@link #getBean} 来获取 Bean。
 *
 * <p>可以将其视为 Groovy Bean 定义场景下 {@link GenericXmlApplicationContext} 的等效实现，甚至可以
 * 说是其升级版，因为它同样能够无缝理解 XML Bean 定义文件。主要区别在于，在 Groovy 脚本中，上下文可
 * 以与内联 Bean 定义闭包一起使用，如下所示：
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def context = new GenericGroovyApplicationContext()
 * context.reader.beans {
 *     dataSource(BasicDataSource) {                  // &lt;--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // &lt;-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // &lt;-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;         // &lt;-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * context.refresh()
 * </pre>
 *
 * <p>或者，从外部资源（例如 "applicationContext.groovy" 文件）加载如下所示的 Groovy Bean 定义脚本：
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;
 *             dataSource = dataSource
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>使用以下 Java 代码创建 {@code GenericGroovyApplicationContext}（可能使用 Ant 风格的 '*'/'**'
 * 位置模式）：
 *
 * <pre class="code">
 * GenericGroovyApplicationContext context = new GenericGroovyApplicationContext();
 * context.load("org/myapp/applicationContext.groovy");
 * context.refresh();
 * </pre>
 *
 * <p>或者更简洁，前提是无需额外配置：
 *
 * <pre class="code">
 * ApplicationContext context = new GenericGroovyApplicationContext("org/myapp/applicationContext.groovy");
 * </pre>
 *
 * <p><b>此应用上下文也能理解 XML Bean 定义文件，从而可以与 Groovy Bean 定义文件无缝混用和搭配。
 * </b> ".xml" 文件将作为 XML 内容解析；所有其他类型的资源将作为 Groovy 脚本解析。
 *
 * @author Juergen Hoeller
 * @author Jeff Brown
 * @since 4.0
 * @see org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
 */
public class GenericGroovyApplicationContext extends GenericApplicationContext implements GroovyObject {

	private final GroovyBeanDefinitionReader reader = new GroovyBeanDefinitionReader(this);

	private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);

	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());


	/**
	 * 创建一个新的 GenericGroovyApplicationContext，需要先{@link #load 加载}，然后手动
	 * {@link #refresh 刷新}。
	 */
	public GenericGroovyApplicationContext() {
	}

	/**
	 * 创建一个新的 GenericGroovyApplicationContext，从给定的资源加载 Bean 定义并自动刷新上下文。
	 * @param resources the resources to load from
	 */
	public GenericGroovyApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * 创建一个新的 GenericGroovyApplicationContext，从给定的资源位置加载 Bean 定义并自动刷新上下文。
	 * @param resourceLocations the resources to load from
	 */
	public GenericGroovyApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * 创建一个新的 GenericGroovyApplicationContext，从给定的资源位置加载 Bean 定义并自动刷新上下文。
	 * @param relativeClass class whose package will be used as a prefix when
	 * loading each specified resource name
	 * @param resourceNames relatively-qualified names of resources to load
	 */
	public GenericGroovyApplicationContext(Class<?> relativeClass, String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}


	/**
	 * 公开底层的 {@link GroovyBeanDefinitionReader}，以便便捷地访问其上的 {@code loadBeanDefinition}
	 * 方法，并能够指定内联 Groovy Bean 定义闭包。
	 * @see GroovyBeanDefinitionReader#loadBeanDefinitions(org.springframework.core.io.Resource...)
	 * @see GroovyBeanDefinitionReader#loadBeanDefinitions(String...)
	 */
	public final GroovyBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 将给定的环境委托给底层的 {@link GroovyBeanDefinitionReader}。应在调用 {@code #load} 之前调用。
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(getEnvironment());
	}

	/**
	 * 从给定的 Groovy 脚本或 XML 文件加载 Bean 定义。<p>请注意，".xml" 文件将作为 XML 内容解析；所有其
	 * 他类型的资源将作为 Groovy 脚本解析。
	 * @param resources one or more resources to load from
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * 从给定的 Groovy 脚本或 XML 文件加载 Bean 定义。<p>请注意，".xml" 文件将作为 XML 内容解析；所有其
	 * 他类型的资源将作为 Groovy 脚本解析。
	 * @param resourceLocations one or more resource locations to load from
	 */
	public void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * 从给定的 Groovy 脚本或 XML 文件加载 Bean 定义。<p>请注意，".xml" 文件将作为 XML 内容解析；所有其
	 * 他类型的资源将作为 Groovy 脚本解析。
	 * @param relativeClass class whose package will be used as a prefix when
	 * loading each specified resource name
	 * @param resourceNames relatively-qualified names of resources to load
	 */
	public void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		load(resources);
	}


	// Implementation of the GroovyObject interface

	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	@Override
	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	@Override
	public Object invokeMethod(String name, Object args) {
		return this.metaClass.invokeMethod(this, name, args);
	}

	@Override
	public void setProperty(String property, Object newValue) {
		if (newValue instanceof BeanDefinition) {
			registerBeanDefinition(property, (BeanDefinition) newValue);
		}
		else {
			this.metaClass.setProperty(this, property, newValue);
		}
	}

	@Override
	@Nullable
	public Object getProperty(String property) {
		if (containsBean(property)) {
			return getBean(property);
		}
		else if (this.contextWrapper.isReadableProperty(property)) {
			return this.contextWrapper.getPropertyValue(property);
		}
		throw new NoSuchBeanDefinitionException(property);
	}

}
