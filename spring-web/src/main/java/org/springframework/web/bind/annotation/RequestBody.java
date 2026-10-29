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

import org.springframework.http.converter.HttpMessageConverter;

/**
 * 指示方法参数应绑定到 Web 请求体的注解。
 * 请求体会通过 {@link HttpMessageConverter} 传递，以根据请求的内容类型
 * 解析方法参数。此外，可以通过为参数添加 {@code @Valid} 注解来应用自动校验。
 *
 * <p>支持用于注解处理方法。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see RequestHeader
 * @see ResponseBody
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {

	/**
	 * 请求体内容是否为必需。
	 * <p>默认为 {@code true}，如果请求体内容为空则抛出异常。
	 * 如果希望在请求体内容为 {@code null} 时传入 {@code null}，
	 * 请将其改为 {@code false}。
	 * @since 3.2
	 */
	boolean required() default true;

}
