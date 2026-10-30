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

package org.springframework.web.reactive.result.view;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于 {@link View} 实现的基类。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class AbstractView implements View, BeanNameAware, ApplicationContextAware {

	/** bean 工厂中 RequestDataValueProcessor 的众所周知名称。 */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private final ReactiveAdapterRegistry adapterRegistry;

	private final List<MediaType> mediaTypes = new ArrayList<>(4);

	private Charset defaultCharset = StandardCharsets.UTF_8;

	@Nullable
	private String requestContextAttribute;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;


	public AbstractView() {
		this(ReactiveAdapterRegistry.getSharedInstance());
	}

	public AbstractView(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		this.adapterRegistry = reactiveAdapterRegistry;
		this.mediaTypes.add(ViewResolverSupport.DEFAULT_CONTENT_TYPE);
	}


	/**
	 * 设置此视图支持的媒体类型。
	 * <p>默认为 {@code "text/html;charset=UTF-8"}。
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.mediaTypes.clear();
		this.mediaTypes.addAll(supportedMediaTypes);
	}

	/**
	 * 获取此视图支持的已配置媒体类型。
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * 设置此视图的默认字符集，在{@linkplain #setSupportedMediaTypes(List) 内容类型}不包含字符集时使用。
	 * <p>默认为 {@linkplain StandardCharsets#UTF_8 UTF 8}。
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "'defaultCharset' must not be null");
		this.defaultCharset = defaultCharset;
	}

	/**
	 * 获取默认字符集，在{@linkplain #setSupportedMediaTypes(List) 内容类型}不包含字符集时使用。
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}

	/**
	 * 设置此视图的 {@code RequestContext} 属性名称。
	 * <p>默认没有（{@code null}）。
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 获取此视图的 {@code RequestContext} 属性名称（如果有）。
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 设置视图名称。有助于跟踪。
	 * <p>框架代码在构造视图时必须调用此方法。
	 */
	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 获取视图名称。
	 * <p>如果视图配置正确，绝不应为 {@code null}。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 获取实际使用的 {@link ApplicationContext}。
	 * @return 该 {@code ApplicationContext}（永不为 {@code null}）
	 * @throws IllegalStateException 如果无法获取 ApplicationContext
	 * @see #getApplicationContext()
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}


	/**
	 * 准备要渲染的模型。
	 * @param model 以属性名称为键、对应模型对象为值的映射
	 * （在模型为空的情况下，该映射也可以为 {@code null}）
	 * @param contentType 选择用于渲染的内容类型，
	 * 它应匹配 {@link #getSupportedMediaTypes() supported media types} 之一
	 * @param exchange 当前交换
	 * @return 表示渲染何时成功以及是否成功的 {@code Mono}
	 */
	@Override
	public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
			ServerWebExchange exchange) {

		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + "View " + formatViewName() +
					", model " + (model != null ? model : Collections.emptyMap()));
		}

		if (contentType != null) {
			exchange.getResponse().getHeaders().setContentType(contentType);
		}

		return getModelAttributes(model, exchange).flatMap(mergedModel -> {
			// Expose RequestContext?
			if (this.requestContextAttribute != null) {
				mergedModel.put(this.requestContextAttribute, createRequestContext(exchange, mergedModel));
			}
			return renderInternal(mergedModel, contentType, exchange);
		});
	}

	/**
	 * 准备用于渲染的模型。
	 * <p>默认实现创建一个合并的输出 Map，包含模型和静态属性，其中模型优先。
	 */
	protected Mono<Map<String, Object>> getModelAttributes(
			@Nullable Map<String, ?> model, ServerWebExchange exchange) {

		Map<String, Object> attributes;
		if (model != null) {
			attributes = new ConcurrentHashMap<>(model.size());
			for (Map.Entry<String, ?> entry : model.entrySet()) {
				if (entry.getValue() != null) {
					attributes.put(entry.getKey(), entry.getValue());
				}
			}
		}
		else {
			attributes = new ConcurrentHashMap<>(0);
		}

		//noinspection deprecation
		return resolveAsyncAttributes(attributes)
				.then(resolveAsyncAttributes(attributes, exchange))
				.doOnTerminate(() -> exchange.getAttributes().remove(BINDING_CONTEXT_ATTRIBUTE))
				.thenReturn(attributes);
	}

	/**
	 * 使用配置的 {@link ReactiveAdapterRegistry} 将异步属性适配为 {@code Mono<T>} 或 {@code Mono<List<T>>}，然后等待将其解析为实际值。当返回的 {@code Mono<Void>} 完成时，模型中的异步属性将被替换为相应的已解析值。
	 * @return 模型就绪时完成的 {@code Mono}
	 * @since 5.1.8
	 */
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model, ServerWebExchange exchange) {
		List<Mono<?>> asyncAttributes = null;
		for (Map.Entry<String, ?> entry : model.entrySet()) {
			Object value =  entry.getValue();
			if (value == null) {
				continue;
			}
			ReactiveAdapter adapter = this.adapterRegistry.getAdapter(null, value);
			if (adapter != null) {
				if (asyncAttributes == null) {
					asyncAttributes = new ArrayList<>();
				}
				String name = entry.getKey();
				if (adapter.isMultiValue()) {
					asyncAttributes.add(
							Flux.from(adapter.toPublisher(value))
									.collectList()
									.doOnSuccess(result -> model.put(name, result)));
				}
				else {
					asyncAttributes.add(
							Mono.from(adapter.toPublisher(value))
									.doOnSuccess(result -> {
										if (result != null) {
											model.put(name, result);
											addBindingResult(name, result, model, exchange);
										}
										else {
											model.remove(name);
										}
									}));
				}
			}
		}
		return asyncAttributes != null ? Mono.when(asyncAttributes) : Mono.empty();
	}

	private void addBindingResult(String name, Object value, Map<String, Object> model, ServerWebExchange exchange) {
		BindingContext context = exchange.getAttribute(BINDING_CONTEXT_ATTRIBUTE);
		if (context == null || value.getClass().isArray() || value instanceof Collection ||
				value instanceof Map || BeanUtils.isSimpleValueType(value.getClass())) {
			return;
		}
		BindingResult result = context.createDataBinder(exchange, value, name).getBindingResult();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, result);
	}

	/**
	 * 使用配置的 {@link ReactiveAdapterRegistry} 将异步属性适配为 {@code Mono<T>} 或 {@code Mono<List<T>>}，然后等待将其解析为实际值。当返回的 {@code Mono<Void>} 完成时，模型中的异步属性将被替换为相应的已解析值。
	 * @return 模型就绪时完成的 {@code Mono}
	 * @deprecated as of 5.1.8 this method is still invoked but it is a no-op.
	 * Please use {@link #resolveAsyncAttributes(Map, ServerWebExchange)}
	 * instead. It is invoked after this one and does the actual work.
	 */
	@Deprecated
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model) {
		return Mono.empty();
	}

	/**
	 * 创建要在{@linkplain #setRequestContextAttribute 指定属性名称}下公开的 {@link RequestContext}。
	 * <p>默认实现为给定交换和模型创建标准的 {@code RequestContext} 实例。
	 * <p>可以在子类中重写以创建自定义实例。
	 * @param exchange 当前交换
	 * @param model 合并后的输出 Map（永不为 {@code null}），
	 * 其中动态值优先于静态属性
	 * @return 该 {@code RequestContext} 实例
	 * @see #setRequestContextAttribute
	 */
	protected RequestContext createRequestContext(ServerWebExchange exchange, Map<String, Object> model) {
		return new RequestContext(exchange, model, obtainApplicationContext(), getRequestDataValueProcessor());
	}

	/**
	 * 获取要使用的 {@link RequestDataValueProcessor}。
	 * <p>默认实现会在 {@link #getApplicationContext() ApplicationContext} 中查找名称为 {@link #REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME} 的 {@code RequestDataValueProcessor} Bean。
	 * @return 该 {@code RequestDataValueProcessor}；如果应用上下文中没有，
	 * 则为 {@code null}
	 */
	@Nullable
	protected RequestDataValueProcessor getRequestDataValueProcessor() {
		ApplicationContext context = getApplicationContext();
		if (context != null && context.containsBean(REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			return context.getBean(REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
		return null;
	}

	/**
	 * 子类必须实现此方法以实际渲染视图。
	 * @param renderAttributes 合并后的输出 Map（永不为 {@code null}），
	 * 其中动态值优先于静态属性
	 * @param contentType 选择用于渲染的内容类型，
	 * 它应匹配 {@linkplain #getSupportedMediaTypes() supported media types} 之一
	 * @param exchange 当前交换
	 * @return 表示渲染何时成功以及是否成功的 {@code Mono}
	 */
	protected abstract Mono<Void> renderInternal(Map<String, Object> renderAttributes,
			@Nullable MediaType contentType, ServerWebExchange exchange);


	@Override
	public String toString() {
		return getClass().getName() + ": " + formatViewName();
	}

	protected String formatViewName() {
		return (getBeanName() != null ?
				"name '" + getBeanName() + "'" : "[" + getClass().getSimpleName() + "]");
	}

}
