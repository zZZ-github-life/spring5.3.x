/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.Version;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.RequestContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * 使用 FreeMarker 模板引擎的 {@code View} 实现。
 *
 * <p>依赖应用程序上下文中的单个 {@link FreeMarkerConfig} 对象（例如
 * {@link FreeMarkerConfigurer}）可访问。也可以通过 {@link #setConfiguration}
 * 将 FreeMarker {@link Configuration} 直接设置到此类上。
 *
 * <p>{@link #setUrl(String) url} 属性指定 FreeMarker 模板相对于
 * FreeMarkerConfigurer 的
 * {@link FreeMarkerConfigurer#setTemplateLoaderPath templateLoaderPath} 的位置。
 *
 * <p>注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class FreeMarkerView extends AbstractUrlBasedView {

	/**
	 * 模板模型中 {@link RequestContext} 实例的属性名称，
	 * 可供 Spring 的宏使用 &mdash; 例如，用于创建
	 * {@link org.springframework.web.reactive.result.view.BindStatus BindStatus}
	 * 对象。
	 * @since 5.2
	 * @see #setExposeSpringMacroHelpers(boolean)
	 */
	public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";


	@Nullable
	private Configuration configuration;

	@Nullable
	private String encoding;

	private boolean exposeSpringMacroHelpers = true;


	/**
	 * 设置此视图要使用的 FreeMarker {@link Configuration}。
	 * <p>通常不会直接设置此属性。而是期望在 Spring 应用程序上下文中存在一个
	 * {@link FreeMarkerConfig}，用于获取 FreeMarker 配置。
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 获取此视图使用的 FreeMarker {@link Configuration}。
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * 获取用于实际使用的 FreeMarker {@link Configuration}。
	 * @return FreeMarker 配置（永不为 {@code null}）
	 * @throws IllegalStateException 在未设置 {@code Configuration} 对象的情况下
	 * @see #getConfiguration()
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "No Configuration set");
		return configuration;
	}

	/**
	 * 设置 FreeMarker 模板文件的编码。
	 * <p>默认情况下，{@link FreeMarkerConfigurer} 会将 FreeMarker 配置中的
	 * 默认编码设置为 "UTF-8"。如果你的所有模板共用一种编码，建议在 FreeMarker
	 * {@link Configuration} 中指定编码，而不是按模板单独指定。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 获取 FreeMarker 模板的编码。
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置是否在名称 {@value #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE} 下暴露
	 * 一个供 Spring 宏库使用的 {@link RequestContext}。
	 * <p>默认值为 {@code true}。
	 * <p>Spring 的 FreeMarker 默认宏需要此项。请注意，使用 HTML 表单的模板
	 * <i>并不</i> 需要此项，<i>除非</i> 你希望利用 Spring 的辅助宏。
	 * @since 5.2
	 * @see #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (getConfiguration() == null) {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
		}
	}

	/**
	 * 自动检测 {@code ApplicationContext} 中的 {@link FreeMarkerConfig} 对象。
	 * @return 此视图要使用的 {@code FreeMarkerConfig} 实例
	 * @throws BeansException 如果找不到 {@code FreeMarkerConfig} 实例
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this application context " +
							"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
							"This bean may be given any name.", ex);
		}
	}


	/**
	 * 检查此视图使用的 FreeMarker 模板是否存在且有效。
	 * <p>可以重写此方法以自定义行为，例如需要将多个模板渲染到单个视图中的情况。
	 */
	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			// Allow for ViewResolver chaining...
			return false;
		}
		catch (ParseException ex) {
			throw new ApplicationContextException(
					"Failed to parse FreeMarker template for URL [" +  getUrl() + "]", ex);
		}
		catch (IOException ex) {
			throw new ApplicationContextException(
					"Could not load FreeMarker template for URL [" + getUrl() + "]", ex);
		}
	}

	/**
	 * 准备用于渲染的模型，可能会暴露一个 {@link RequestContext} 供 Spring FreeMarker
	 * 宏使用，然后委托给此方法的继承实现。
	 * @since 5.2
	 * @see #setExposeSpringMacroHelpers(boolean)
	 * @see org.springframework.web.reactive.result.view.AbstractView#getModelAttributes(Map, ServerWebExchange)
	 */
	@Override
	protected Mono<Map<String, Object>> getModelAttributes(
			@Nullable Map<String, ?> model, ServerWebExchange exchange) {

		if (this.exposeSpringMacroHelpers) {
			if (model != null && model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
				throw new IllegalStateException(
						"Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE +
						"' because of an existing model object of the same name");
			}
			// Make a defensive copy of the model.
			Map<String, Object> attributes = (model != null ? new HashMap<>(model) : new HashMap<>());
			// Expose RequestContext instance for Spring macros.
			attributes.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, new RequestContext(
					exchange, attributes, obtainApplicationContext(), getRequestDataValueProcessor()));
			return super.getModelAttributes(attributes, exchange);
		}
		return super.getModelAttributes(model, exchange);
	}

	@Override
	protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
			@Nullable MediaType contentType, ServerWebExchange exchange) {

		return exchange.getResponse().writeWith(Mono
				.fromCallable(() -> {
					// Expose all standard FreeMarker hash models.
					SimpleHash freeMarkerModel = getTemplateModel(renderAttributes, exchange);

					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "Rendering [" + getUrl() + "]");
					}

					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());
					DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
					try {
						Charset charset = getCharset(contentType);
						Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset);
						getTemplate(locale).process(freeMarkerModel, writer);
						return dataBuffer;
					}
					catch (IOException ex) {
						DataBufferUtils.release(dataBuffer);
						String message = "Could not load FreeMarker template for URL [" + getUrl() + "]";
						throw new IllegalStateException(message, ex);
					}
					catch (Throwable ex) {
						DataBufferUtils.release(dataBuffer);
						throw ex;
					}
				})
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
	}

	private Charset getCharset(@Nullable MediaType mediaType) {
		return Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(getDefaultCharset());
	}

	/**
	 * 为给定的模型 Map 构建 FreeMarker 模板模型。
	 * <p>默认实现构建一个 {@link SimpleHash}。
	 * @param model 要用于渲染的模型
	 * @param exchange 当前交换
	 * @return FreeMarker 模板模型，为 {@link SimpleHash} 或其子类
	 */
	protected SimpleHash getTemplateModel(Map<String, Object> model, ServerWebExchange exchange) {
		SimpleHash fmModel = new SimpleHash(getObjectWrapper());
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * 获取配置的 FreeMarker {@link ObjectWrapper}，如果未指定则获取
	 * {@linkplain ObjectWrapper#DEFAULT_WRAPPER 默认包装器}。
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
		Version version = Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
		return (ow != null ? ow : new DefaultObjectWrapperBuilder(version).build());
	}

	/**
	 * 获取给定区域设置下、由此视图渲染的 FreeMarker 模板。
	 * <p>默认情况下，将获取由 "url" bean 属性指定的模板。
	 * @param locale 当前 Locale
	 * @return 要渲染的 FreeMarker 模板
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(getUrl(), locale, getEncoding()) :
				obtainConfiguration().getTemplate(getUrl(), locale));
	}

}
