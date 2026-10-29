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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;

/**
 * 创建 {@code ContentNegotiationManager} 并使用一个或多个
 * {@link ContentNegotiationStrategy} 实例对其进行配置。
 *
 * <p>此工厂提供了若干属性，用于配置底层的策略。下表列出了属性名称、
 * 它们的默认设置以及它们帮助配置的策略：
 *
 * <table>
 * <tr>
 * <th>属性设置方法</th>
 * <th>默认值</th>
 * <th>底层策略</th>
 * <th>是否启用</th>
 * </tr>
 * <tr>
 * <td>{@link #favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #favorPathExtension}</td>
 * <td>false（自 5.3 起）</td>
 * <td>{@link org.springframework.web.accept.PathExtensionContentNegotiationStrategy
 * PathExtensionContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>启用</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * </table>
 *
 * <p>自 5.0 起，可以通过 {@link #strategies(List)} 设置要使用的确切策略。
 *
 * <p><strong>注意：</strong>如果必须使用基于 URL 的内容类型解析，
 * 使用查询参数比使用路径扩展更简单也更可取，因为后者可能会对 URI 变量、
 * 路径参数和 URI 解码造成问题。考虑将 {@link #favorPathExtension}
 * 设置为 {@literal false}，或者通过 {@link #strategies(List)} 显式设置要使用的策略。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentNegotiationConfigurer {

	private final ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();

	private final Map<String, MediaType> mediaTypes = new HashMap<>();


	/**
	 * 带有 {@link javax.servlet.ServletContext} 的类构造函数。
	 */
	public ContentNegotiationConfigurer(@Nullable ServletContext servletContext) {
		if (servletContext != null) {
			this.factory.setServletContext(servletContext);
		}
	}


	/**
	 * 设置要使用的确切策略列表。
	 * <p><strong>注意：</strong>使用此方法与使用此类中所有其他用于自定义
	 * 默认固定策略集的设置方法互斥。更多详情请参见类级别文档。
	 * @param strategies the strategies to use
	 * @since 5.0
	 */
	public void strategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.factory.setStrategies(strategies);
	}

	/**
	 * 是否应使用请求参数（默认为 "format"）来确定请求的媒体类型。
	 * 要使此选项生效，必须注册
	 * {@link #mediaType(String, MediaType) 媒体类型映射}。
	 * <p>默认设置为 {@code false}。
	 * @see #parameterName(String)
	 */
	public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
		this.factory.setFavorParameter(favorParameter);
		return this;
	}

	/**
	 * 设置 {@link #favorParameter} 开启时要使用的查询参数名称。
	 * <p>默认参数名称为 {@code "format"}。
	 */
	public ContentNegotiationConfigurer parameterName(String parameterName) {
		this.factory.setParameterName(parameterName);
		return this;
	}

	/**
	 * 是否应使用 URL 路径中的路径扩展来确定请求的媒体类型。
	 * <p>默认设置为 {@code false}，此时路径扩展对内容协商没有影响。
	 * @deprecated 自 5.2.4 起。请参见
	 * {@link ContentNegotiationManagerFactoryBean#setFavorPathExtension(boolean)} 上的弃用说明。
	 */
	@Deprecated
	public ContentNegotiationConfigurer favorPathExtension(boolean favorPathExtension) {
		this.factory.setFavorPathExtension(favorPathExtension);
		return this;
	}

	/**
	 * 添加从键（从路径扩展或查询参数中提取）到 MediaType 的映射。
	 * 这是参数策略正常工作所必需的。此处显式注册的任何扩展还会被视为
	 * 可安全用于反射文件下载（Reflected File Download）攻击检测
	 * （有关 RFD 攻击防护的更多详情，请参见 Spring Framework 参考文档）。
	 * <p>路径扩展策略还会尝试使用
	 * {@link ServletContext#getMimeType} 和 {@link MediaTypeFactory} 来解析路径扩展。
	 * 要更改此行为，请参见 {@link #useRegisteredExtensionsOnly} 属性。
	 * @param extension the key to look up
	 * @param mediaType the media type
	 * @see #mediaTypes(Map)
	 * @see #replaceMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
		this.mediaTypes.put(extension, mediaType);
		return this;
	}

	/**
	 * {@link #mediaType} 的替代方案。
	 * @see #mediaType(String, MediaType)
	 * @see #replaceMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer mediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.mediaTypes.putAll(mediaTypes);
		}
		return this;
	}

	/**
	 * 与 {@link #mediaType} 类似，但用于替换现有的映射。
	 * @see #mediaType(String, MediaType)
	 * @see #mediaTypes(Map)
	 */
	public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
		this.mediaTypes.clear();
		mediaTypes(mediaTypes);
		return this;
	}

	/**
	 * 是否忽略路径扩展无法解析为任何媒体类型的请求。
	 * 将此设置为 {@code false} 时，如果没有匹配项，将导致
	 * {@code HttpMediaTypeNotAcceptableException}。
	 * <p>默认设置为 {@code true}。
	 * @deprecated 自 5.2.4 起。请参见
	 * {@link ContentNegotiationManagerFactoryBean#setIgnoreUnknownPathExtensions(boolean)} 上的弃用说明。
	 */
	@Deprecated
	public ContentNegotiationConfigurer ignoreUnknownPathExtensions(boolean ignore) {
		this.factory.setIgnoreUnknownPathExtensions(ignore);
		return this;
	}

	/**
	 * 当设置了 {@link #favorPathExtension} 时，此属性决定是否允许使用
	 * JAF（Java Activation Framework）将路径扩展解析为特定的 MediaType。
	 * @deprecated 自 5.0 起，推荐使用 {@link #useRegisteredExtensionsOnly(boolean)}，
	 * 其行为相反
	 */
	@Deprecated
	public ContentNegotiationConfigurer useJaf(boolean useJaf) {
		return this.useRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * 当设置了 {@link #favorPathExtension favorPathExtension} 时，
	 * 此属性决定是否仅使用已注册的 {@code MediaType} 映射
	 * 将路径扩展解析为特定的 MediaType。
	 * <p>默认未设置，此时
	 * {@code PathExtensionContentNegotiationStrategy} 将在可用的情况下使用默认值。
	 */
	public ContentNegotiationConfigurer useRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.factory.setUseRegisteredExtensionsOnly(useRegisteredExtensionsOnly);
		return this;
	}

	/**
	 * 是否禁用对 'Accept' 请求标头的检查。
	 * <p>默认设置为 {@code false}。
	 */
	public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.factory.setIgnoreAcceptHeader(ignoreAcceptHeader);
		return this;
	}

	/**
	 * 按优先级顺序设置未请求内容类型时要使用的默认内容类型。
	 * <p>如果存在不支持任何给定媒体类型的目标，考虑在末尾追加 {@link MediaType#ALL}。
	 * <p>默认未设置。
	 * @see #defaultContentTypeStrategy
	 */
	public ContentNegotiationConfigurer defaultContentType(MediaType... defaultContentTypes) {
		this.factory.setDefaultContentTypes(Arrays.asList(defaultContentTypes));
		return this;
	}

	/**
	 * 设置自定义的 {@link ContentNegotiationStrategy}，用于在未请求内容类型时
	 * 确定要使用的内容类型。
	 * <p>默认未设置。
	 * @since 4.1.2
	 * @see #defaultContentType
	 */
	public ContentNegotiationConfigurer defaultContentTypeStrategy(ContentNegotiationStrategy defaultStrategy) {
		this.factory.setDefaultContentTypeStrategy(defaultStrategy);
		return this;
	}


	/**
	 * 根据此配置器的设置构建 {@link ContentNegotiationManager}。
	 * @since 4.3.12
	 * @see ContentNegotiationManagerFactoryBean#getObject()
	 */
	protected ContentNegotiationManager buildContentNegotiationManager() {
		this.factory.addMediaTypes(this.mediaTypes);
		return this.factory.build();
	}

}
