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

package org.springframework.web.method.support;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 用于参与构建 {@link UriComponents} 的策略：通过检查方法参数和参数值，
 * 决定应更新目标 URL 的哪个部分。
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface UriComponentsContributor {

	/**
	 * 此贡献器是否支持给定的方法参数。
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 处理方法参数，并更新 {@link UriComponentsBuilder}，
	 * 或向 URI 变量映射中添加内容，以便在所有参数处理完毕后展开 URI。
	 * @param parameter 控制器方法参数（永不为 {@code null}）
	 * @param value 参数值（可能为 {@code null}）
	 * @param builder 要更新的构建器（永不为 {@code null}）
	 * @param uriVariables 用于添加 URI 变量的 map（永不为 {@code null}）
	 * @param conversionService 用于将值格式化为 String 的 ConversionService
	 */
	void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables, ConversionService conversionService);

}
