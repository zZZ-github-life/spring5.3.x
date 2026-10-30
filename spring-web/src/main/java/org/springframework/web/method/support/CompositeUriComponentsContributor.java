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

package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 一个 {@link UriComponentsContributor}，包含一个其他贡献器列表以便委托调用，
 * 同时封装一个特定的 {@link ConversionService}，
 * 用于将方法参数值格式化为 String。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class CompositeUriComponentsContributor implements UriComponentsContributor {

	private final List<Object> contributors;

	private final ConversionService conversionService;


	/**
	 * 从一组 {@link UriComponentsContributor UriComponentsContributors} 或
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 创建实例。
	 * 由于两者通常由同一个类实现，最便捷的做法是获取
	 * {@code RequestMappingHandlerAdapter} 中配置的
	 * {@code HandlerMethodArgumentResolvers}，并将其提供给此构造函数。
	 * @param contributors {@link UriComponentsContributor} 或
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 的集合
	 */
	public CompositeUriComponentsContributor(UriComponentsContributor... contributors) {
		this.contributors = Arrays.asList((Object[]) contributors);
		this.conversionService = new DefaultFormattingConversionService();
	}

	/**
	 * 从一组 {@link UriComponentsContributor UriComponentsContributors} 或
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 创建实例。
	 * 由于两者通常由同一个类实现，最便捷的做法是获取
	 * {@code RequestMappingHandlerAdapter} 中配置的
	 * {@code HandlerMethodArgumentResolvers}，并将其提供给此构造函数。
	 * @param contributors {@link UriComponentsContributor} 或
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 的集合
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors) {
		this(contributors, null);
	}

	/**
	 * 从一组 {@link UriComponentsContributor UriComponentsContributors} 或
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 创建实例。
	 * 由于两者通常由同一个类实现，最便捷的做法是获取
	 * {@code RequestMappingHandlerAdapter} 中配置的
	 * {@code HandlerMethodArgumentResolvers}，并将其提供给此构造函数。
	 * <p>如果 {@link ConversionService} 参数为 {@code null}，
	 * 则默认使用 {@link org.springframework.format.support.DefaultFormattingConversionService}。
	 * @param contributors {@link UriComponentsContributor} 或
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 的集合
	 * @param cs 当方法参数值在添加到 URI 之前需要格式化为
	 * String 时使用的 ConversionService
	 */
	public CompositeUriComponentsContributor(@Nullable Collection<?> contributors, @Nullable ConversionService cs) {
		this.contributors = (contributors != null ? new ArrayList<>(contributors) : Collections.emptyList());
		this.conversionService = (cs != null ? cs : new DefaultFormattingConversionService());
	}

	/**
	 * 判断此 {@code CompositeUriComponentsContributor} 是否有任何贡献器。
	 * @return 如果此 {@code CompositeUriComponentsContributor} 创建时带有
	 * 可委托的贡献者则返回 {@code true}
	 */
	public boolean hasContributors() {
		return !this.contributors.isEmpty();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		for (Object contributor : this.contributors) {
			if (contributor instanceof UriComponentsContributor) {
				if (((UriComponentsContributor) contributor).supportsParameter(parameter)) {
					return true;
				}
			}
			else if (contributor instanceof HandlerMethodArgumentResolver) {
				if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		for (Object contributor : this.contributors) {
			if (contributor instanceof UriComponentsContributor) {
				UriComponentsContributor ucc = (UriComponentsContributor) contributor;
				if (ucc.supportsParameter(parameter)) {
					ucc.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
					break;
				}
			}
			else if (contributor instanceof HandlerMethodArgumentResolver) {
				if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
					break;
				}
			}
		}
	}

	/**
	 * 重载方法，使用构造时创建的 ConversionService。
	 */
	public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables) {

		contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
	}

}
