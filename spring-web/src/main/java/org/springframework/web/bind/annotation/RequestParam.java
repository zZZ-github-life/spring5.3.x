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
import java.util.Map;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到 Web 请求参数的注解。
 *
 * <p>在 Spring MVC 和 Spring WebFlux 中支持用于注解处理方法，具体如下：
 * <ul>
 * <li>在 Spring MVC 中，"请求参数"映射到查询参数、表单数据以及 multipart
 * 请求中的部件。这是因为 Servlet API 将查询参数和表单数据合并为一个名为
 * "parameters" 的 Map，其中包含对请求体的自动解析。
 * <li>在 Spring WebFlux 中，"请求参数"仅映射到查询参数。
 * 若要处理查询参数、表单数据和 multipart 数据这三种情况，
 * 可以使用数据绑定到标注了 {@link ModelAttribute} 的命令对象。
 * </ul>
 *
 * <p>如果方法参数类型为 {@link Map} 并指定了请求参数名称，则在有合适的转换策略
 * 可用时，请求参数值会被转换为 {@link Map}。
 *
 * <p>如果方法参数为 {@link java.util.Map Map&lt;String, String&gt;} 或
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * 且未指定参数名称，则 Map 参数将填充所有请求参数名称和值。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see RequestMapping
 * @see RequestHeader
 * @see CookieValue
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的请求参数名称。
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 参数是否为必需。
	 * <p>默认为 {@code true}，如果请求中缺少该参数将抛出异常。
	 * 如果希望在请求中不存在该参数时得到 {@code null} 值，请将其改为 {@code false}。
	 * <p>另外，提供 {@link #defaultValue} 会隐式地将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 当未提供请求参数或其值为空时用作回退的默认值。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
