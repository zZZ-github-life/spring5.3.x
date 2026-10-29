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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * 协助注册全局的、基于 URL 模式的 {@link CorsConfiguration} 映射。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see CorsRegistration
 */
public class CorsRegistry {

	private final List<CorsRegistration> registrations = new ArrayList<>();


	/**
	 * 为指定的路径模式启用跨源请求处理。
	 * <p>既支持精确的路径映射 URI（例如 {@code "/admin"}），
	 * 也支持 Ant 风格的路径模式（例如 {@code "/admin/**"}）。
	 * <p>默认情况下，此映射的 {@code CorsConfiguration} 会使用
	 * {@link CorsConfiguration#applyPermitDefaultValues()} 中描述的默认值进行初始化。
	 */
	public CorsRegistration addMapping(String pathPattern) {
		CorsRegistration registration = new CorsRegistration(pathPattern);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 返回已注册的 {@link CorsConfiguration} 对象，
	 * 以路径模式为键。
	 */
	protected Map<String, CorsConfiguration> getCorsConfigurations() {
		Map<String, CorsConfiguration> configs = CollectionUtils.newLinkedHashMap(this.registrations.size());
		for (CorsRegistration registration : this.registrations) {
			configs.put(registration.getPathPattern(), registration.getCorsConfiguration());
		}
		return configs;
	}

}
