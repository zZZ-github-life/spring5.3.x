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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于确定请求所请求的{@linkplain MediaType 媒体类型}的核心类。
 * 它通过委托给一组已配置的 {@code ContentNegotiationStrategy} 实例来完成此工作。
 *
 * <p>还提供了查找媒体类型对应文件扩展名的方法。它通过委托给
 * 一组已配置的 {@code MediaTypeFileExtensionResolver} 实例来完成此工作。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

	private final List<ContentNegotiationStrategy> strategies = new ArrayList<>();

	private final Set<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<>();


	/**
	 * 使用给定的 {@code ContentNegotiationStrategy} 策略列表创建实例，
	 * 其中每个策略也可以是 {@code MediaTypeFileExtensionResolver} 的实例。
	 * @param strategies 要使用的策略
	 */
	public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
		this(Arrays.asList(strategies));
	}

	/**
	 * 基于集合的替代方式，用于替代
	 * {@link #ContentNegotiationManager(ContentNegotiationStrategy...)}。
	 * @param strategies 要使用的策略
	 * @since 3.2.2
	 */
	public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
		Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
		this.strategies.addAll(strategies);
		for (ContentNegotiationStrategy strategy : this.strategies) {
			if (strategy instanceof MediaTypeFileExtensionResolver) {
				this.resolvers.add((MediaTypeFileExtensionResolver) strategy);
			}
		}
	}

	/**
	 * 使用 {@link HeaderContentNegotiationStrategy} 创建一个默认实例。
	 */
	public ContentNegotiationManager() {
		this(new HeaderContentNegotiationStrategy());
	}


	/**
	 * 返回已配置的内容协商策略。
	 * @since 3.2.16
	 */
	public List<ContentNegotiationStrategy> getStrategies() {
		return this.strategies;
	}

	/**
	 * 查找给定类型的 {@code ContentNegotiationStrategy}。
	 * @param strategyType 策略类型
	 * @return 第一个匹配的策略，如果没有则为 {@code null}
	 * @since 4.3
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends ContentNegotiationStrategy> T getStrategy(Class<T> strategyType) {
		for (ContentNegotiationStrategy strategy : getStrategies()) {
			if (strategyType.isInstance(strategy)) {
				return (T) strategy;
			}
		}
		return null;
	}

	/**
	 * 在构造时检测到的解析器之外，再注册更多
	 * {@code MediaTypeFileExtensionResolver} 实例。
	 * @param resolvers 要添加的解析器
	 */
	public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
		Collections.addAll(this.resolvers, resolvers);
	}

	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
		for (ContentNegotiationStrategy strategy : this.strategies) {
			List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
			if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
				continue;
			}
			return mediaTypes;
		}
		return MEDIA_TYPE_ALL_LIST;
	}

	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		return doResolveExtensions(resolver -> resolver.resolveFileExtensions(mediaType));
	}

	/**
	 * {@inheritDoc}
	 * <p>在启动时，此方法返回显式注册到
	 * {@link PathExtensionContentNegotiationStrategy} 或
	 * {@link ParameterContentNegotiationStrategy} 的扩展名。在运行时，如果存在
	 * “路径扩展名”策略且其
	 * {@link PathExtensionContentNegotiationStrategy#setUseRegisteredExtensionsOnly(boolean)
	 * useRegisteredExtensionsOnly} 属性设置为 "false"，那么随着通过
	 * {@link org.springframework.http.MediaTypeFactory} 解析并缓存文件扩展名，
	 * 扩展名列表可能会增加。
	 */
	@Override
	public List<String> getAllFileExtensions() {
		return doResolveExtensions(MediaTypeFileExtensionResolver::getAllFileExtensions);
	}

	private List<String> doResolveExtensions(Function<MediaTypeFileExtensionResolver, List<String>> extractor) {
		List<String> result = null;
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			List<String> extensions = extractor.apply(resolver);
			if (CollectionUtils.isEmpty(extensions)) {
				continue;
			}
			result = (result != null ? result : new ArrayList<>(4));
			for (String extension : extensions) {
				if (!result.contains(extension)) {
					result.add(extension);
				}
			}
		}
		return (result != null ? result : Collections.emptyList());
	}

	/**
	 * 通过遍历 {@link MediaTypeFileExtensionResolver}，返回所有已注册的
	 * 查找键到媒体类型的映射。
	 * @since 5.2.4
	 */
	public Map<String, MediaType> getMediaTypeMappings() {
		Map<String, MediaType> result = null;
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			if (resolver instanceof MappingMediaTypeFileExtensionResolver) {
				Map<String, MediaType> map = ((MappingMediaTypeFileExtensionResolver) resolver).getMediaTypes();
				if (CollectionUtils.isEmpty(map)) {
					continue;
				}
				result = (result != null ? result : new HashMap<>(4));
				result.putAll(map);
			}
		}
		return (result != null ? result : Collections.emptyMap());
	}

}
