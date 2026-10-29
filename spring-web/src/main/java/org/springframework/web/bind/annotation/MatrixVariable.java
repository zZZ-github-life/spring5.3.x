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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到路径段中某个名称-值对的注解。
 * 支持用于 {@link RequestMapping} 注解的处理器方法。
 *
 * <p>如果方法参数类型为 {@link java.util.Map} 并指定了矩阵变量名称，
 * 则在有合适的转换策略可用时，矩阵变量值会被转换为 {@link java.util.Map}。
 *
 * <p>如果方法参数为 {@link java.util.Map Map&lt;String, String&gt;} 或
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * 且未指定变量名称，则 Map 将填充所有矩阵变量名称和值。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MatrixVariable {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 矩阵变量的名称。
	 * @since 4.2
	 * @see #value
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 矩阵变量所在的 URI 路径变量名称，在需要消歧时使用
	 * （例如多个路径段中存在同名矩阵变量）。
	 */
	String pathVar() default ValueConstants.DEFAULT_NONE;

	/**
	 * 矩阵变量是否为必需。
	 * <p>默认为 {@code true}，如果请求中缺少该变量将抛出异常。
	 * 如果希望在变量缺失时得到 {@code null}，请将其改为 {@code false}。
	 * <p>另外，提供 {@link #defaultValue} 会隐式地将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 用作回退的默认值。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
