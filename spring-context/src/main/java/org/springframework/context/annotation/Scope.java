/*
 * Copyright 2002-2018 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AliasFor;

/**
 * 当与 {@link org.springframework.stereotype.Component @Component} 一起用作类型级注解时，
 * {@code @Scope} 指示要用于该注解类型实例的作用域名称。
 *
 * <p>当与 {@link Bean @Bean} 一起用作方法级注解时，{@code @Scope} 指示要用于该方法所返回实例的作用
 * 域名称。
 *
 * <p><b>注意：</b> {@code @Scope} 注解只会在具体 Bean 类（对于注解组件）或工厂方法（对于
 * {@code @Bean} 方法）上进行内省。与 XML Bean 定义不同，这里没有 Bean 定义继承的概念，类级别的继承
 * 层次结构对元数据而言无关紧要。
 *
 * <p>在此上下文中，<em>作用域</em>指实例的生命周期，例如 {@code singleton}、{@code prototype} 等。
 * Spring 开箱即用的作用域可以通过 {@link ConfigurableBeanFactory} 和 {@code WebApplicationContext}
 * 接口中提供的 {@code SCOPE_*} 常量来引用。
 *
 * <p>要注册额外的自定义作用域，请参见
 * {@link org.springframework.beans.factory.config.CustomScopeConfigurer CustomScopeConfigurer}。
 *
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.stereotype.Component
 * @see org.springframework.context.annotation.Bean
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

	/**
	 * {@link #scopeName} 的别名。
	 * @see #scopeName
	 */
	@AliasFor("scopeName")
	String value() default "";

	/**
	 * 指定用于所注解组件/Bean 的作用域名称。
	 *
	 * <p>默认值为空字符串（{@code ""}），表示
	 * {@link ConfigurableBeanFactory#SCOPE_SINGLETON SCOPE_SINGLETON}。
	 * @since 4.2
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 * @see org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST
	 * @see org.springframework.web.context.WebApplicationContext#SCOPE_SESSION
	 * @see #value
	 */
	@AliasFor("value")
	String scopeName() default "";

	/**
	 * 指定是否应将组件配置为作用域代理，如果是，则指定该代理是基于接口的还是基于子类的。
	 *
	 * <p>默认值为 {@link ScopedProxyMode#DEFAULT}，通常表示不会创建作用域代理，除非在组件扫描指令级别
	 * 配置了不同的默认值。
	 *
	 * <p>类似于 Spring XML 中的 {@code <aop:scoped-proxy/>} 支持。
	 * @see ScopedProxyMode
	 */
	ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

}
