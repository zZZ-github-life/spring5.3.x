/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.filter.TypeFilter;

/**
 * 配置用于 @{@link Configuration} 类的组件扫描指令。提供与 Spring XML 的
 * {@code <context:component-scan>} 元素并行的支持。
 *
 * <p>可以指定 {@link #basePackageClasses} 或 {@link #basePackages}（或其别名 {@link #value}）来定
 * 义要扫描的特定包。如果未定义特定包，则从声明此注解的类所在的包开始扫描。
 *
 * <p>请注意，{@code <context:component-scan>} 元素具有 {@code annotation-config} 属性；但此注解没
 * 有。这是因为在几乎所有使用 {@code @ComponentScan} 的情况下，都假定使用默认的注解配置处理（例如处
 * 理 {@code @Autowired} 及其同类注解）。此外，使用 {@link AnnotationConfigApplicationContext} 时，
 * 注解配置处理器始终会被注册，这意味着在 {@code @ComponentScan} 层级禁用它们的任何尝试都会被忽略。
 *
 * <p>有关用法示例，请参阅 {@link Configuration @Configuration} 的 Javadoc。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see Configuration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(ComponentScans.class)
public @interface ComponentScan {

	/**
	 * 是 {@link #basePackages} 的别名。<p>如果不需要其他属性，可以使用更简洁的注解声明&mdash;例如，用
	 * {@code @ComponentScan("org.my.pkg")} 代替 {@code @ComponentScan(basePackages = "org.my.pkg")}。
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 要扫描以查找带注解组件的基础包。<p>{@link #value} 是此属性的别名（并且与之互斥）。<p>对于基于字
	 * 符串的包名，可使用 {@link #basePackageClasses} 作为类型安全的替代方案。
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * 用于指定要扫描以查找带注解组件的包，是 {@link #basePackages} 的类型安全替代方案。将扫描所指定每
	 * 个类所在的包。<p>考虑在每个包中创建一个特殊的空操作标记类或接口，其唯一用途就是被此属性引用。
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * 用于在 Spring 容器中为检测到的组件命名的 {@link BeanNameGenerator} 类。<p>
	 * {@link BeanNameGenerator} 接口本身的默认值表明，用于处理此 {@code @ComponentScan} 注解的扫描器应
	 * 使用其继承的 Bean 名称生成器，例如默认的 {@link AnnotationBeanNameGenerator} 或在引导时提供给应
	 * 用上下文的任何自定义实例。
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * 用于解析检测到的组件作用域的 {@link ScopeMetadataResolver}。
	 */
	Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

	/**
	 * 指示是否应为检测到的组件生成代理，在以代理风格使用作用域时这可能是必要的。<p>默认是遵循用于执行
	 * 实际扫描的组件扫描器的默认行为。<p>请注意，设置此属性会覆盖为 {@link #scopeResolver} 设置的任何
	 * 值。
	 * @see ClassPathBeanDefinitionScanner#setScopedProxyMode(ScopedProxyMode)
	 */
	ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

	/**
	 * 控制符合组件检测条件的类文件。<p>考虑使用 {@link #includeFilters} 和 {@link #excludeFilters} 以
	 * 获得更灵活的方式。
	 */
	String resourcePattern() default ClassPathScanningCandidateComponentProvider.DEFAULT_RESOURCE_PATTERN;

	/**
	 * 指示是否应启用对标注了 {@code @Component}、{@code @Repository}、{@code @Service} 或
	 * {@code @Controller} 的类的自动检测。
	 */
	boolean useDefaultFilters() default true;

	/**
	 * 指定哪些类型符合组件扫描条件。<p>进一步将候选组件集合从 {@link #basePackages} 中的所有内容缩小到
	 * 基础包中与给定过滤器匹配的所有内容。<p>请注意，如果指定了这些过滤器，它们将在默认过滤器之外额外
	 * 应用。指定基础包下匹配给定过滤器的任何类型都会被包含，即使它不匹配默认过滤器（即未标注
	 * {@code @Component}）。
	 * @see #resourcePattern()
	 * @see #useDefaultFilters()
	 */
	Filter[] includeFilters() default {};

	/**
	 * 指定哪些类型不符合组件扫描条件。
	 * @see #resourcePattern
	 */
	Filter[] excludeFilters() default {};

	/**
	 * 指定是否应为扫描到的 Bean 注册以便延迟初始化。<p>默认值为 {@code false}；需要时将其切换为
	 * {@code true}。
	 * @since 4.1
	 */
	boolean lazyInit() default false;


	/**
	 * 声明要用作{@linkplain ComponentScan#includeFilters 包含过滤器}或
	 * {@linkplain ComponentScan#excludeFilters 排除过滤器}的类型过滤器。
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {

		/**
		 * 要使用的过滤器类型。<p>默认是 {@link FilterType#ANNOTATION}。
		 * @see #classes
		 * @see #pattern
		 */
		FilterType type() default FilterType.ANNOTATION;

		/**
		 * 是 {@link #classes} 的别名。
		 * @see #classes
		 */
		@AliasFor("classes")
		Class<?>[] value() default {};

		/**
		 * 用作过滤器的类。<p>下表说明了将如何根据 {@link #type} 属性的配置值来解释这些类。
		 * <table border="1">
		 * <tr><th>{@code FilterType}</th><th>类被解释为</th></tr>
		 * <tr><td>{@link FilterType#ANNOTATION ANNOTATION}</td>
		 * <td>注解本身</td></tr>
		 * <tr><td>{@link FilterType#ASSIGNABLE_TYPE ASSIGNABLE_TYPE}</td>
		 * <td>检测到的组件应可赋值给的类型</td></tr>
		 * <tr><td>{@link FilterType#CUSTOM CUSTOM}</td>
		 * <td>{@link TypeFilter} 的实现</td></tr>
		 * </table>
		 * <p>当指定多个类时，会应用<em>或</em>逻辑&mdash;例如，“包含标注了 {@code @Foo} 或 {@code @Bar} 的
		 * 类型”。<p>自定义的 {@link TypeFilter TypeFilters} 可以选择实现以下任何
		 * {@link org.springframework.beans.factory.Aware Aware} 接口，其各自的方法将在
		 * {@link TypeFilter#match match} 之前被调用：
		 * <ul>
		 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
		 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
		 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
		 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
		 * </ul>
		 * <p>允许指定零个类，但不会对组件扫描产生任何影响。
		 * @since 4.2
		 * @see #value
		 * @see #type
		 */
		@AliasFor("value")
		Class<?>[] classes() default {};

		/**
		 * 用于过滤器的模式，作为指定 Class {@link #value} 的替代方案。<p>如果 {@link #type} 设置为
		 * {@link FilterType#ASPECTJ ASPECTJ}，则这是一个 AspectJ 类型模式表达式。如果 {@link #type} 设置为
		 * {@link FilterType#REGEX REGEX}，则这是一个用于匹配全限定类名的正则表达式模式。
		 * @see #type
		 * @see #classes
		 */
		String[] pattern() default {};

	}

}
