/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * 用于组合式 {@link RequestedContentTypeResolver} 的构建器，它将请求委托给其他解析器，每个解析器实现不同的策略来确定所请求的内容类型，例如 Accept 头、查询参数或其他。
 *
 * <p>使用构建器方法按所需顺序添加解析器。对于给定请求，将使用第一个返回非空且不仅包含 {@link MediaType#ALL} 的列表的解析器。
 *
 * <p>默认情况下，如果未显式配置任何解析器，构建器将添加 {@link HeaderContentTypeResolver}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestedContentTypeResolverBuilder {

	private final List<Supplier<RequestedContentTypeResolver>> candidates = new ArrayList<>();


	/**
	 * 添加从查询参数获取所请求内容类型的解析器。默认情况下查询参数名为 {@code "format"}。
	 */
	public ParameterResolverConfigurer parameterResolver() {
		ParameterResolverConfigurer parameterBuilder = new ParameterResolverConfigurer();
		this.candidates.add(parameterBuilder::createResolver);
		return parameterBuilder;
	}

	/**
	 * 添加从 {@literal "Accept"} 头获取所请求内容类型的解析器。
	 */
	public void headerResolver() {
		this.candidates.add(HeaderContentTypeResolver::new);
	}

	/**
	 * 添加返回固定媒体类型集合的解析器。
	 * @param mediaTypes the media types to use
	 */
	public void fixedResolver(MediaType... mediaTypes) {
		this.candidates.add(() -> new FixedContentTypeResolver(Arrays.asList(mediaTypes)));
	}

	/**
	 * 添加自定义解析器。
	 * @param resolver the resolver to add
	 */
	public void resolver(RequestedContentTypeResolver resolver) {
		this.candidates.add(() -> resolver);
	}

	/**
	 * 构建一个 {@link RequestedContentTypeResolver}，将请求委托给通过此构建器配置的解析器列表。
	 */
	public RequestedContentTypeResolver build() {
		List<RequestedContentTypeResolver> resolvers = (!this.candidates.isEmpty() ?
				this.candidates.stream().map(Supplier::get).collect(Collectors.toList()) :
				Collections.singletonList(new HeaderContentTypeResolver()));

		return exchange -> {
			for (RequestedContentTypeResolver resolver : resolvers) {
				List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
				if (isMediaTypeAll(mediaTypes)) {
					continue;
				}
				return mediaTypes;
			}
			return RequestedContentTypeResolver.MEDIA_TYPE_ALL_LIST;
		};
	}

	private boolean isMediaTypeAll(List<MediaType> mediaTypes) {
		return mediaTypes.size() == 1
			&& mediaTypes.get(0).removeQualityValue().equals(MediaType.ALL);
	}


	/**
	 * 用于创建和配置 {@link ParameterContentTypeResolver} 的辅助方法。
	 */
	public static class ParameterResolverConfigurer {

		private final Map<String, MediaType> mediaTypes = new HashMap<>();

		@Nullable
		private String parameterName;

		/**
		 * 配置查找键（从查询参数值中提取）与对应 {@code MediaType} 之间的映射。
		 * @param key the lookup key
		 * @param mediaType the MediaType for that key
		 */
		public ParameterResolverConfigurer mediaType(String key, MediaType mediaType) {
			this.mediaTypes.put(key, mediaType);
			return this;
		}

		/**
		 * {@link #mediaType(String, MediaType)} 的基于 Map 的变体。
		 * @param mediaTypes the mappings to copy
		 */
		public ParameterResolverConfigurer mediaType(Map<String, MediaType> mediaTypes) {
			this.mediaTypes.putAll(mediaTypes);
			return this;
		}

		/**
		 * 设置用于确定所请求媒体类型的参数名称。
		 * <p>默认设置为 {@literal "format"}。
		 */
		public ParameterResolverConfigurer parameterName(String parameterName) {
			this.parameterName = parameterName;
			return this;
		}

		/**
		 * 用于创建解析器的私有工厂方法。
		 */
		private RequestedContentTypeResolver createResolver() {
			ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(this.mediaTypes);
			if (this.parameterName != null) {
				resolver.setParameterName(this.parameterName);
			}
			return resolver;
		}
	}

}
