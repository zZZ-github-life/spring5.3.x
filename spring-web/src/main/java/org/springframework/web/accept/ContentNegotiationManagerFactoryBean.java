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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * 用于创建 {@code ContentNegotiationManager} 并使用
 * {@link ContentNegotiationStrategy} 实例对其进行配置的工厂。
 *
 * <p>此工厂提供了若干属性，这些属性会进而配置底层的策略。下表显示了属性名称、
 * 其默认设置，以及它们帮助配置的策略：
 *
 * <table>
 * <tr>
 * <th>属性设置方法</th>
 * <th>默认值</th>
 * <th>底层策略</th>
 * <th>是否启用</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension favorPathExtension}</td>
 * <td>false（自 5.3 起）</td>
 * <td>{@link PathExtensionContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>启用</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * </table>
 *
 * <p>或者，你可以避免使用上述便捷构建器方法，而是通过
 * {@link #setStrategies(List)} 设置要使用的确切策略。
 *
 * <p><strong>弃用说明：</strong>自 5.2.4 起，
 * {@link #setFavorPathExtension(boolean) favorPathExtension} 和
 * {@link #setIgnoreUnknownPathExtensions(boolean) ignoreUnknownPathExtensions}
 * 已被弃用，以劝阻将路径扩展名用于内容协商和请求映射，
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 * RequestMappingHandlerMapping} 中也有类似的弃用。有关更多背景，请阅读 issue
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ContentNegotiationManagerFactoryBean
		implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

	@Nullable
	private List<ContentNegotiationStrategy> strategies;


	private boolean favorParameter = false;

	private String parameterName = "format";

	private boolean favorPathExtension = false;

	private Map<String, MediaType> mediaTypes = new HashMap<>();

	private boolean ignoreUnknownPathExtensions = true;

	@Nullable
	private Boolean useRegisteredExtensionsOnly;

	private boolean ignoreAcceptHeader = false;

	@Nullable
	private ContentNegotiationStrategy defaultNegotiationStrategy;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private ServletContext servletContext;


	/**
	 * 设置要使用的确切策略列表。
	 * <p><strong>注意：</strong>使用此方法与本类中所有其他用于定制默认固定策略集的
	 * setter 方法互斥。详见类级别文档。
	 * @param strategies 要使用的策略
	 * @since 5.0
	 */
	public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
	}

	/**
	 * 是否应使用请求参数（默认为 "format"）来确定所请求的媒体类型。
	 * 为使此选项生效，必须注册 {@link #setMediaTypes 媒体类型映射}。
	 * <p>默认情况下设置为 {@code false}。
	 * @see #setParameterName
	 */
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * 设置当 {@link #setFavorParameter} 开启时要使用的查询参数名称。
	 * <p>默认参数名为 {@code "format"}。
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * 是否应使用 URL 路径中的路径扩展名来确定所请求的媒体类型。
	 * <p>默认情况下设置为 {@code false}，此时路径扩展名对内容协商没有影响。
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options. As there is no replacement for this method,
	 * in 5.2.x it is necessary to set it to {@code false}. In 5.3 the default
	 * changes to {@code false} and use of this property becomes unnecessary.
	 */
	@Deprecated
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * 添加从键到 MediaType 的映射，其中键会被规范化为小写，
	 * 并且可能已从路径扩展名、文件名扩展名中提取，或作为查询参数传入。
	 * <p>{@link #setFavorParameter(boolean) 参数策略}需要此类映射才能工作，
	 * 而 {@link #setFavorPathExtension(boolean) 路径扩展名策略}可以回退到
	 * 通过 {@link ServletContext#getMimeType} 和
	 * {@link org.springframework.http.MediaTypeFactory} 进行查找。
	 * <p><strong>注意：</strong>在此注册的映射可通过
	 * {@link ContentNegotiationManager#getMediaTypeMappings()} 访问，
	 * 并且可能不仅用于参数策略和路径扩展名策略。例如，在 Spring MVC 配置中，
	 * 例如 {@code @EnableWebMvc} 或 {@code <mvc:annotation-driven>}，
	 * 媒体类型映射也会插入到以下场景：
	 * <ul>
	 * <li>确定通过 {@code ResourceHttpRequestHandler} 提供的静态资源的媒体类型。
	 * <li>确定通过 {@code ContentNegotiatingViewResolver} 渲染的视图的媒体类型。
	 * <li>列出用于 RFD 攻击检测的安全扩展名（详情请参阅 Spring Framework 参考文档）。
	 * </ul>
	 * @param mediaTypes 媒体类型映射
	 * @see #addMediaType(String, MediaType)
	 * @see #addMediaTypes(Map)
	 */
	public void setMediaTypes(Properties mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			mediaTypes.forEach((key, value) ->
					addMediaType((String) key, MediaType.valueOf((String) value)));
		}
	}

	/**
	 * 用于编程式注册的替代方式，替代 {@link #setMediaTypes}。
	 */
	public void addMediaType(String key, MediaType mediaType) {
		this.mediaTypes.put(key.toLowerCase(Locale.ENGLISH), mediaType);
	}

	/**
	 * 用于编程式注册的替代方式，替代 {@link #setMediaTypes}。
	 */
	public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			mediaTypes.forEach(this::addMediaType);
		}
	}

	/**
	 * 是否忽略路径扩展名无法解析为任何媒体类型的请求。将其设置为
	 * {@code false} 时，如果没有匹配项，将导致
	 * {@code HttpMediaTypeNotAcceptableException}。
	 * <p>默认情况下设置为 {@code true}。
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options.
	 */
	@Deprecated
	public void setIgnoreUnknownPathExtensions(boolean ignore) {
		this.ignoreUnknownPathExtensions = ignore;
	}

	/**
	 * 指示是否使用 Java Activation 框架作为从文件扩展名映射到媒体类型的回退选项。
	 * @deprecated as of 5.0, in favor of {@link #setUseRegisteredExtensionsOnly(boolean)},
	 * which has reverse behavior.
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * 当设置了 {@link #setFavorPathExtension favorPathExtension} 或
	 * {@link #setFavorParameter(boolean)} 时，此属性决定是仅使用已注册的
	 * {@code MediaType} 映射，还是允许动态解析（例如通过 {@link MediaTypeFactory}）。
	 * <p>默认情况下不设置此属性，此时动态解析处于开启状态。
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	private boolean useRegisteredExtensionsOnly() {
		return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
	}

	/**
	 * 是否禁用对 'Accept' 请求头的检查。
	 * <p>默认情况下该值设置为 {@code false}。
	 */
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * 设置未请求任何内容类型时要使用的默认内容类型。
	 * <p>默认情况下不设置。
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * 设置未请求任何内容类型时要使用的默认内容类型列表。
	 * <p>默认情况下不设置。
	 * @since 5.0
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentTypes(List<MediaType> contentTypes) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
	}

	/**
	 * 设置一个自定义 {@link ContentNegotiationStrategy}，用于在未请求任何内容类型时
	 * 确定要使用的内容类型。
	 * <p>默认情况下不设置。
	 * @since 4.1.2
	 * @see #setDefaultContentType
	 */
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}

	/**
	 * 由 Spring 调用以注入 ServletContext。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		build();
	}

	/**
	 * 创建并初始化一个 {@link ContentNegotiationManager} 实例。
	 * @since 5.0
	 */
	@SuppressWarnings("deprecation")
	public ContentNegotiationManager build() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<>();

		if (this.strategies != null) {
			strategies.addAll(this.strategies);
		}
		else {
			if (this.favorPathExtension) {
				PathExtensionContentNegotiationStrategy strategy;
				if (this.servletContext != null && !useRegisteredExtensionsOnly()) {
					strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
				}
				else {
					strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
				}
				strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				strategies.add(strategy);
			}
			if (this.favorParameter) {
				ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
				strategy.setParameterName(this.parameterName);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				else {
					strategy.setUseRegisteredExtensionsOnly(true);  // backwards compatibility
				}
				strategies.add(strategy);
			}
			if (!this.ignoreAcceptHeader) {
				strategies.add(new HeaderContentNegotiationStrategy());
			}
			if (this.defaultNegotiationStrategy != null) {
				strategies.add(this.defaultNegotiationStrategy);
			}
		}

		this.contentNegotiationManager = new ContentNegotiationManager(strategies);

		// Ensure media type mappings are available via ContentNegotiationManager#getMediaTypeMappings()
		// independent of path extension or parameter strategies.

		if (!CollectionUtils.isEmpty(this.mediaTypes) && !this.favorPathExtension && !this.favorParameter) {
			this.contentNegotiationManager.addFileExtensionResolvers(
					new MappingMediaTypeFileExtensionResolver(this.mediaTypes));
		}

		return this.contentNegotiationManager;
	}


	@Override
	@Nullable
	public ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
