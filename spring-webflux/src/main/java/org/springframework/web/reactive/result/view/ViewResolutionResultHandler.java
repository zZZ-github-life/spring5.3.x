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

package org.springframework.web.reactive.result.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.HandlerResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code HandlerResultHandler}，封装视图解析算法，支持以下返回类型：
 * <ul>
 * <li>{@link Void} 或无值 —— 默认视图名称</li>
 * <li>{@link String} —— 视图名称，除非带有 {@code @ModelAttribute} 注解
 * <li>{@link View} —— 用于渲染的 View
 * <li>{@link Model} —— 要添加到模型的属性
 * <li>{@link Map} —— 要添加到模型的属性
 * <li>{@link Rendering} —— 用例驱动的视图解析 API</li>
 * <li>{@link ModelAttribute @ModelAttribute} —— 模型的属性
 * <li>非简单值 —— 模型的属性
 * </ul>
 *
 * <p>基于 String 的视图名称通过配置的 {@link ViewResolver} 实例解析为用于渲染的 {@link View}。如果未指定视图（例如返回 {@code null} 或与模型相关的返回值），则选择默认视图名称。
 *
 * <p>默认情况下，此解析器在 {@link Ordered#LOWEST_PRECEDENCE} 排序，通常需要较晚执行，因为它将任何 String 返回值解释为视图名称，或将任何非简单值类型解释为模型属性，而其他结果处理器可能根据注解的存在对相同值做出不同解释，例如 {@code @ResponseBody}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ViewResolutionResultHandler extends HandlerResultHandlerSupport implements HandlerResultHandler, Ordered {

	private static final Object NO_VALUE = new Object();

	private static final Mono<Object> NO_VALUE_MONO = Mono.just(NO_VALUE);


	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final List<View> defaultViews = new ArrayList<>(4);


	/**
	 * 使用默认 {@link ReactiveAdapterRegistry} 的基本构造函数。
	 * @param viewResolvers 要使用的解析器
	 * @param contentTypeResolver 用于确定请求的内容类型
	 */
	public ViewResolutionResultHandler(List<ViewResolver> viewResolvers,
			RequestedContentTypeResolver contentTypeResolver) {

		this(viewResolvers, contentTypeResolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 使用 {@link ReactiveAdapterRegistry} 实例的构造函数。
	 * @param viewResolvers 要使用的视图解析器
	 * @param contentTypeResolver 用于确定请求的内容类型
	 * @param registry 用于适配响应式类型
	 */
	public ViewResolutionResultHandler(List<ViewResolver> viewResolvers,
			RequestedContentTypeResolver contentTypeResolver, ReactiveAdapterRegistry registry) {

		super(contentTypeResolver, registry);
		this.viewResolvers.addAll(viewResolvers);
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
	}


	/**
	 * 返回视图解析器的只读列表。
	 */
	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(this.viewResolvers);
	}

	/**
	 * 设置解析视图名称并尝试满足最佳匹配内容类型时始终考虑的默认视图。
	 */
	public void setDefaultViews(@Nullable List<View> defaultViews) {
		this.defaultViews.clear();
		if (defaultViews != null) {
			this.defaultViews.addAll(defaultViews);
		}
	}

	/**
	 * 返回配置的默认 {@code View}。
	 */
	public List<View> getDefaultViews() {
		return this.defaultViews;
	}


	@Override
	public boolean supports(HandlerResult result) {
		if (hasModelAnnotation(result.getReturnTypeSource())) {
			return true;
		}

		Class<?> type = result.getReturnType().toClass();
		ReactiveAdapter adapter = getAdapter(result);
		if (adapter != null) {
			if (adapter.isNoValue()) {
				return true;
			}
			type = result.getReturnType().getGeneric().toClass();
		}

		return (CharSequence.class.isAssignableFrom(type) ||
				Rendering.class.isAssignableFrom(type) ||
				Model.class.isAssignableFrom(type) ||
				Map.class.isAssignableFrom(type) ||
				View.class.isAssignableFrom(type) ||
				!BeanUtils.isSimpleProperty(type));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Mono<Object> valueMono;
		ResolvableType valueType;
		ReactiveAdapter adapter = getAdapter(result);

		if (adapter != null) {
			if (adapter.isMultiValue()) {
				throw new IllegalArgumentException(
						"Multi-value reactive types not supported in view resolution: " + result.getReturnType());
			}

			valueMono = (result.getReturnValue() != null ?
					Mono.from(adapter.toPublisher(result.getReturnValue())) : Mono.empty());

			valueType = (adapter.isNoValue() ? ResolvableType.forClass(Void.class) :
					result.getReturnType().getGeneric());
		}
		else {
			valueMono = Mono.justOrEmpty(result.getReturnValue());
			valueType = result.getReturnType();
		}

		return valueMono
				.switchIfEmpty(exchange.isNotModified() ? Mono.empty() : NO_VALUE_MONO)
				.flatMap(returnValue -> {

					Mono<List<View>> viewsMono;
					Model model = result.getModel();
					MethodParameter parameter = result.getReturnTypeSource();
					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());

					Class<?> clazz = valueType.toClass();
					if (clazz == Object.class) {
						clazz = returnValue.getClass();
					}

					if (returnValue == NO_VALUE || clazz == void.class || clazz == Void.class) {
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (CharSequence.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						viewsMono = resolveViews(returnValue.toString(), locale);
					}
					else if (Rendering.class.isAssignableFrom(clazz)) {
						Rendering render = (Rendering) returnValue;
						HttpStatus status = render.status();
						if (status != null) {
							exchange.getResponse().setStatusCode(status);
						}
						exchange.getResponse().getHeaders().putAll(render.headers());
						model.addAllAttributes(render.modelAttributes());
						Object view = render.view();
						if (view == null) {
							view = getDefaultViewName(exchange);
						}
						viewsMono = (view instanceof String ? resolveViews((String) view, locale) :
								Mono.just(Collections.singletonList((View) view)));
					}
					else if (Model.class.isAssignableFrom(clazz)) {
						model.addAllAttributes(((Model) returnValue).asMap());
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (Map.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						model.addAllAttributes((Map<String, ?>) returnValue);
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					else if (View.class.isAssignableFrom(clazz)) {
						viewsMono = Mono.just(Collections.singletonList((View) returnValue));
					}
					else {
						String name = getNameForReturnValue(parameter);
						model.addAttribute(name, returnValue);
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					BindingContext bindingContext = result.getBindingContext();
					updateBindingResult(bindingContext, exchange);
					return viewsMono.flatMap(views -> render(views, model.asMap(), bindingContext, exchange));
				});
	}


	private boolean hasModelAnnotation(MethodParameter parameter) {
		return parameter.hasMethodAnnotation(ModelAttribute.class);
	}

	/**
	 * 当控制器未指定视图时选择默认视图名称。使用去掉前导和尾部斜杠的请求路径。
	 */
	private String getDefaultViewName(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return StringUtils.stripFilenameExtension(path);
	}

	private Mono<List<View>> resolveViews(String viewName, Locale locale) {
		return Flux.fromIterable(getViewResolvers())
				.concatMap(resolver -> resolver.resolveViewName(viewName, locale))
				.collectList()
				.map(views -> {
					if (views.isEmpty()) {
						throw new IllegalStateException(
								"Could not resolve view with name '" + viewName + "'.");
					}
					views.addAll(getDefaultViews());
					return views;
				});
	}

	private String getNameForReturnValue(MethodParameter returnType) {
		return Optional.ofNullable(returnType.getMethodAnnotation(ModelAttribute.class))
				.filter(ann -> StringUtils.hasText(ann.value()))
				.map(ModelAttribute::value)
				.orElseGet(() -> Conventions.getVariableNameForParameter(returnType));
	}

	private void updateBindingResult(BindingContext context, ServerWebExchange exchange) {
		Map<String, Object> model = context.getModel().asMap();
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (isBindingCandidate(name, value)) {
				if (!model.containsKey(BindingResult.MODEL_KEY_PREFIX + name)) {
					WebExchangeDataBinder binder = context.createDataBinder(exchange, value, name);
					model.put(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
				}
			}
		}
	}

	private boolean isBindingCandidate(String name, @Nullable Object value) {
		return (!name.startsWith(BindingResult.MODEL_KEY_PREFIX) && value != null &&
				!value.getClass().isArray() && !(value instanceof Collection) && !(value instanceof Map) &&
				getAdapterRegistry().getAdapter(null, value) == null &&
				!BeanUtils.isSimpleValueType(value.getClass()));
	}

	private Mono<? extends Void> render(List<View> views, Map<String, Object> model,
			BindingContext bindingContext, ServerWebExchange exchange) {

		for (View view : views) {
			if (view.isRedirectView()) {
				return renderWith(view, model, null, exchange, bindingContext);
			}
		}
		List<MediaType> mediaTypes = getMediaTypes(views);
		MediaType bestMediaType;
		try {
			bestMediaType = selectMediaType(exchange, () -> mediaTypes);
		}
		catch (NotAcceptableStatusException ex) {
			HttpStatus statusCode = exchange.getResponse().getStatusCode();
			if (statusCode != null && statusCode.isError()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring error response content (if any). " + ex.getReason());
				}
				return Mono.empty();
			}
			throw ex;
		}
		if (bestMediaType != null) {
			for (View view : views) {
				for (MediaType mediaType : view.getSupportedMediaTypes()) {
					if (mediaType.isCompatibleWith(bestMediaType)) {
						return renderWith(view, model, mediaType, exchange, bindingContext);
					}
				}
			}
		}
		throw new NotAcceptableStatusException(mediaTypes);
	}

	private Mono<? extends Void> renderWith(View view, Map<String, Object> model,
			@Nullable MediaType mediaType, ServerWebExchange exchange, BindingContext bindingContext) {

		exchange.getAttributes().put(View.BINDING_CONTEXT_ATTRIBUTE, bindingContext);
		return view.render(model, mediaType, exchange)
				.doOnTerminate(() -> exchange.getAttributes().remove(View.BINDING_CONTEXT_ATTRIBUTE));
	}

	private List<MediaType> getMediaTypes(List<View> views) {
		return views.stream()
				.flatMap(view -> view.getSupportedMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
