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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * 属性资源配置器的抽象基类，用于解析 Bean 定义属性值中的占位符。实现会从属性文件或其他
 * {@linkplain org.springframework.core.env.PropertySource property source} 中<em>拉取</em>值并放入
 * Bean 定义。
 *
 * <p>默认的占位符语法遵循 Ant / Log4J / JSP EL 风格：
 *
 * <pre class="code">${...}</pre>
 *
 * 示例 XML Bean 定义：
 *
 * <pre class="code">
 * &lt;bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"&gt;
 *   &lt;property name="driverClassName" value="${driver}" /&gt;
 *   &lt;property name="url" value="jdbc:${dbname}" /&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * 示例属性文件：
 *
 * <pre class="code">
 * driver=com.mysql.jdbc.Driver
 * dbname=mysql:mydb</pre>
 *
 * 注解式 Bean 定义可以利用属性替换，使用
 * {@link org.springframework.beans.factory.annotation.Value @Value} 注解：
 *
 * <pre class="code">@Value("${person.age}")</pre>
 *
 * 实现会检查简单属性值、列表、映射、props 以及 Bean 引用中的 Bean 名称。此外，占位符值还可以交叉引
 * 用其他占位符，例如：
 *
 * <pre class="code">
 * rootPath=myrootdir
 * subPath=${rootPath}/subdir</pre>
 *
 * 与 {@link PropertyOverrideConfigurer} 不同，此类型的子类允许在 Bean 定义中填充显式占位符。
 *
 * <p>如果配置器无法解析某个占位符，将抛出 {@link BeanDefinitionStoreException}。如果希望针对多个属
 * 性文件进行检查，可以通过 {@link #setLocations locations} 属性指定多个资源。也可以定义多个配置器，
 * 每个配置器使用其<em>自己的</em>占位符语法。使用 {@link #ignoreUnresolvablePlaceholders} 可以有
 * 意抑制在无法解析占位符时抛出异常。
 *
 * <p>可以通过 {@link #setProperties properties} 属性为每个配置器实例全局定义默认属性值，也可以按属
 * 性逐个定义，使用值分隔符（默认为 {@code ":"}，可通过 {@link #setValueSeparator(String)} 定制）。
 *
 * <p>带默认值的 XML 属性示例：
 *
 * <pre class="code">
 *   &lt;property name="url" value="jdbc:${dbname:defaultdb}" /&gt;
 * </pre>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertyPlaceholderConfigurer
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 */
public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer
		implements BeanNameAware, BeanFactoryAware {

	/** 默认占位符前缀：{@value}。 */
	public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

	/** 默认占位符后缀：{@value}。 */
	public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

	/** 默认值分隔符：{@value}。 */
	public static final String DEFAULT_VALUE_SEPARATOR = ":";


	/** 默认为 {@value #DEFAULT_PLACEHOLDER_PREFIX}。 */
	protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

	/** 默认为 {@value #DEFAULT_PLACEHOLDER_SUFFIX}。 */
	protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

	/** 默认为 {@value #DEFAULT_VALUE_SEPARATOR}。 */
	@Nullable
	protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

	protected boolean trimValues = false;

	@Nullable
	protected String nullValue;

	protected boolean ignoreUnresolvablePlaceholders = false;

	@Nullable
	private String beanName;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 设置占位符字符串的起始前缀。默认为 {@value #DEFAULT_PLACEHOLDER_PREFIX}。
	 */
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * 设置占位符字符串的结束后缀。默认为 {@value #DEFAULT_PLACEHOLDER_SUFFIX}。
	 */
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * 指定占位符变量与其关联默认值之间的分隔字符；如果不应将任何此类特殊字符作为值分隔符处理，则为
	 * {@code null}。默认为 {@value #DEFAULT_VALUE_SEPARATOR}。
	 */
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * 指定在应用解析后的值之前是否对其进行修剪，去除开头和结尾的多余空白。
	 * <p>默认为 {@code false}。
	 * @since 4.3
	 */
	public void setTrimValues(boolean trimValues) {
		this.trimValues = trimValues;
	}

	/**
	 * 设置一个值，当它被解析为占位符值时应被视为 {@code null}：例如 ""（空 String）或 "null"。
	 * <p>请注意，这只适用于完整的属性值，而不适用于拼接值的各个部分。
	 * <p>默认情况下未定义此类 null 值。这意味着，除非在此处显式映射一个对应的值，否则无法将
	 * {@code null} 表示为属性值。
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	/**
	 * 设置是否忽略无法解析的占位符。
	 * <p>默认为 "false"：如果占位符解析失败，将抛出异常。将此标志切换为 "true"，以便在这种情况下原样保
	 * 留占位符 String，交由其他占位符配置器来解析。
	 */
	public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}

	/**
	 * 仅用于检查我们不是在解析自己的 Bean 定义，以避免因属性文件位置中不可解析的占位符而失败。后一种情
	 * 况可能发生在资源位置中使用系统属性占位符时。
	 * @see #setLocations
	 * @see org.springframework.core.io.ResourceEditor
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 仅用于检查我们不是在解析自己的 Bean 定义，以避免因属性文件位置中不可解析的占位符而失败。后一种情
	 * 况可能发生在资源位置中使用系统属性占位符时。
	 * @see #setLocations
	 * @see org.springframework.core.io.ResourceEditor
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			StringValueResolver valueResolver) {

		BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (String curName : beanNames) {
			// Check that we're not parsing our own bean definition,
			// to avoid failing on unresolvable placeholders in properties file locations.
			if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
				try {
					visitor.visitBeanDefinition(bd);
				}
				catch (Exception ex) {
					throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
				}
			}
		}

		// New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
		beanFactoryToProcess.resolveAliases(valueResolver);

		// New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
		beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
	}

}
