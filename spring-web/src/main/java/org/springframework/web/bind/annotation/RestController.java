/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.stereotype.Controller;

/**
 * 一个便捷注解，其本身标注了 {@link Controller @Controller} 和
 * {@link ResponseBody @ResponseBody}。
 * <p>
 * 标注了此注解的类型会被视为控制器，其中的 {@link RequestMapping @RequestMapping}
 * 方法默认采用 {@link ResponseBody @ResponseBody} 语义。
 *
 * <p><b>注意：</b>只有在配置了合适的 {@code HandlerMapping}-{@code HandlerAdapter}
 * 组合时才会处理 {@code @RestController}，例如
 * {@code RequestMappingHandlerMapping}-{@code RequestMappingHandlerAdapter} 组合，
 * 它们是 MVC Java 配置和 MVC 命名空间中的默认配置。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController {

	/**
	 * 该值可用于建议一个逻辑组件名称，在组件被自动检测到时转换为 Spring bean 名称。
	 * @return 建议的组件名称（如果有），否则为空字符串
	 * @since 4.0.1
	 */
	@AliasFor(annotation = Controller.class)
	String value() default "";

}
