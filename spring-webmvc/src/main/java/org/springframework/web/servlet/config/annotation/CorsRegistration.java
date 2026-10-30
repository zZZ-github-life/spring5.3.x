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

package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.cors.CorsConfiguration;

/**
 * 辅助为给定的 URL 路径模式创建 {@link CorsConfiguration} 实例。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.2
 * @see CorsConfiguration
 * @see CorsRegistry
 */
public class CorsRegistration {

	private final String pathPattern;

	private CorsConfiguration config;


	public CorsRegistration(String pathPattern) {
		this.pathPattern = pathPattern;
		// Same implicit default values as the @CrossOrigin annotation + allows simple methods
		this.config = new CorsConfiguration().applyPermitDefaultValues();
	}


	/**
	 * 设置允许浏览器发起跨域请求的来源。关于格式细节及其他注意事项，
	 * 请参阅 {@link CorsConfiguration#setAllowedOrigins(List)}。
	 *
	 * <p>默认允许所有来源，但如果同时设置了
	 * {@link #allowedOriginPatterns(String...) allowedOriginPatterns}，
	 * 则以该设置为准。
	 * @see #allowedOriginPatterns(String...)
	 */
	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(Arrays.asList(origins));
		return this;
	}

	/**
	 * {@link #allowedOrigins(String...)} 的替代方案，支持更灵活的模式来指定
	 * 允许浏览器发起跨域请求的来源。关于格式细节及其他注意事项，
	 * 请参阅 {@link CorsConfiguration#setAllowedOriginPatterns(List)}。
	 * <p>默认未设置。
	 * @since 5.3
	 */
	public CorsRegistration allowedOriginPatterns(String... patterns) {
		this.config.setAllowedOriginPatterns(Arrays.asList(patterns));
		return this;
	}

	/**
	 * 设置允许的 HTTP 方法，例如 {@code "GET"}、{@code "POST"} 等。
	 * <p>特殊值 {@code "*"} 允许所有方法。
	 * <p>默认允许"简单"方法：{@code GET}、{@code HEAD} 和 {@code POST}。
	 */
	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(Arrays.asList(methods));
		return this;
	}

	/**
	 * 设置预检请求可以列为在实际请求期间允许使用的标头列表。
	 * <p>可以使用特殊值 {@code "*"} 允许所有标头。
	 * <p>根据 CORS 规范，如果标头是以下之一，则无需列出：
	 * {@code Cache-Control}、{@code Content-Language}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma}。
	 * <p>默认允许所有标头。
	 */
	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 设置除"简单"标头（即 {@code Cache-Control}、{@code Content-Language}、
	 * {@code Content-Type}、{@code Expires}、{@code Last-Modified} 或 {@code Pragma}）之外，
	 * 实际响应可能具有且可以暴露的响应标头列表。
	 * <p>特殊值 {@code "*"} 允许为非凭证请求暴露所有标头。
	 * <p>默认未设置。
	 */
	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 浏览器是否应随跨域请求一起向注解端点发送凭证（例如 cookie）。配置的值会设置到
	 * 预检请求的 {@code Access-Control-Allow-Credentials} 响应标头上。
	 * <p><strong>注意：</strong>请注意，此选项会与配置的域建立高度信任关系，
	 * 并且通过暴露 cookie 和 CSRF 令牌等敏感的用户特定信息，
	 * 还会扩大 Web 应用受攻击面。
	 * <p>默认未设置，此时也不会设置
	 * {@code Access-Control-Allow-Credentials} 标头，
	 * 因此不允许凭证。
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	/**
	 * 配置客户端可以缓存预检请求响应的时长（以秒为单位）。
	 * <p>默认设置为 1800 秒（30 分钟）。
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * 通过 {@link CorsConfiguration#combine(CorsConfiguration)} 将给定的
	 * {@code CorsConfiguration} 应用到当前正在配置的配置上，而后者已通过
	 * {@link CorsConfiguration#applyPermitDefaultValues()} 初始化。
	 * @param other 要应用的配置
	 * @since 5.3
	 */
	public CorsRegistration combine(CorsConfiguration other) {
		this.config = this.config.combine(other);
		return this;
	}

	protected String getPathPattern() {
		return this.pathPattern;
	}

	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}

}
