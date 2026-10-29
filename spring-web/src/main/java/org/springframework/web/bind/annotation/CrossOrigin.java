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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.cors.CorsConfiguration;

/**
 * 用于允许特定处理器类和/或处理器方法上的跨域请求的注解。
 * 如果配置了合适的 {@code HandlerMapping}，该注解会被处理。
 *
 * <p>Spring Web MVC 和 Spring WebFlux 都通过各自模块中的
 * {@code RequestMappingHandlerMapping} 支持此注解。每个类型级别和方法级别的
 * 注解对中的值会被添加到 {@link CorsConfiguration} 中，然后通过
 * {@link CorsConfiguration#applyPermitDefaultValues()} 应用默认值。
 *
 * <p>合并全局配置和局部配置的规则通常是叠加的 —— 例如所有全局来源和所有
 * 局部来源。对于只能接受单个值的属性（如 {@code allowCredentials} 和
 * {@code maxAge}），局部值会覆盖全局值。
 * 更多详情请参见 {@link CorsConfiguration#combine(CorsConfiguration)}。
 *
 * @author Russell Allen
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Ruslan Akhundov
 * @since 4.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	String[] DEFAULT_ORIGINS = {"*"};

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	String[] DEFAULT_ALLOWED_HEADERS = {"*"};

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	boolean DEFAULT_ALLOW_CREDENTIALS = false;

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	long DEFAULT_MAX_AGE = 1800;


	/**
	 * {@link #origins} 的别名。
	 */
	@AliasFor("origins")
	String[] value() default {};

	/**
	 * 允许进行跨域请求的来源列表。详情请参见
	 * {@link CorsConfiguration#setAllowedOrigins(List)}。
	 * <p>默认允许所有来源，除非同时设置了 {@link #originPatterns}，
	 * 此时将改用 {@code originPatterns}。
	 */
	@AliasFor("value")
	String[] origins() default {};

	/**
	 * {@link #origins} 的替代方案，支持更灵活的来源模式。详情请参见
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)}。
	 * <p>默认不设置。
	 * @since 5.3
	 */
	String[] originPatterns() default {};

	/**
	 * 实际请求中允许的请求头列表，可以是 {@code "*"} 以允许所有头。
	 * <p>允许的头会在预检请求的 {@code Access-Control-Allow-Headers}
	 * 响应头中列出。
	 * <p>根据 CORS 规范，如果头是以下之一，则无需列出：{@code Cache-Control}、
	 * {@code Content-Language}、{@code Expires}、{@code Last-Modified} 或
	 * {@code Pragma}。
	 * <p>默认允许所有请求的头。
	 */
	String[] allowedHeaders() default {};

	/**
	 * 用户代理将允许客户端在实际响应中访问的响应头列表（"简单"头除外，即
	 * {@code Cache-Control}、{@code Content-Language}、{@code Content-Type}、
	 * {@code Expires}、{@code Last-Modified} 或 {@code Pragma}）。
	 * <p>公开的头会在实际 CORS 请求的 {@code Access-Control-Expose-Headers}
	 * 响应头中列出。
	 * <p>特殊值 {@code "*"} 允许在非凭证请求中公开所有头。
	 * <p>默认不列出任何公开的头。
	 */
	String[] exposedHeaders() default {};

	/**
	 * 支持的 HTTP 请求方法列表。
	 * <p>默认情况下，支持的方法与控制器方法所映射的方法相同。
	 */
	RequestMethod[] methods() default {};

	/**
	 * 浏览器是否应随跨域请求向所注解的端点发送凭证（例如 cookie）。
	 * 配置的值会设置在预检请求的 {@code Access-Control-Allow-Credentials}
	 * 响应头中。
	 * <p><strong>注意：</strong>请注意，此选项会与所配置的域建立高度的信任关系，
	 * 并通过暴露敏感的用户特定信息（如 cookie 和 CSRF 令牌）而增大 Web 应用的
	 * 攻击面。
	 * <p>默认不设置，此时也不会设置 {@code Access-Control-Allow-Credentials} 头，
	 * 因此不允许凭证。
	 */
	String allowCredentials() default "";

	/**
	 * 预检响应缓存时长的最大年龄（以秒为单位）。
	 * <p>此属性控制预检请求的 {@code Access-Control-Max-Age} 响应头的值。
	 * <p>将其设置为合理的值可以减少浏览器所需的预检请求/响应交互次数。
	 * 负值表示<em>未定义</em>。
	 * <p>默认设置为 {@code 1800} 秒（30 分钟）。
	 */
	long maxAge() default -1;

}
