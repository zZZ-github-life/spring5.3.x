/*
 * Copyright 2002-2022 the original author or authors.
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

/**
 * 标识用于初始化 {@link org.springframework.web.bind.WebDataBinder} 的方法的注解，
 * 该绑定器将用于填充注解处理方法的命令对象和表单对象参数。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露不应被外部客户端访问或修改的
 * 对象图部分，从而带来安全问题。因此，应仔细考虑数据绑定的设计与使用对安全性的影响。
 * 更多详情请参阅参考手册中关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和 <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * <p>{@code @InitBinder} 方法支持 {@link RequestMapping @RequestMapping} 方法
 * 所支持的所有参数，但命令/表单对象及相应的校验结果对象除外。
 * {@code @InitBinder} 方法不能有返回值；通常声明为 {@code void}。
 *
 * <p>典型参数是 {@link org.springframework.web.bind.WebDataBinder} 结合
 * {@link org.springframework.web.context.request.WebRequest} 或
 * {@link java.util.Locale}，从而可以注册特定于上下文的编辑器。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see ControllerAdvice
 * @see org.springframework.web.bind.WebDataBinder
 * @see org.springframework.web.context.request.WebRequest
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InitBinder {

	/**
	 * 此初始化绑定方法应应用的命令/表单属性名称和/或请求参数名称。
	 * <p>默认应用于注解处理类所处理的所有命令/表单属性及所有请求参数。
	 * 在此指定模型属性名称或请求参数名称会将初始化绑定方法限制为那些特定的
	 * 属性/参数，不同的初始化绑定方法通常应用于不同的属性组或参数组。
	 */
	String[] value() default {};

}
