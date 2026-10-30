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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * 参数解析器的抽象基类，通过 {@link HttpMessageReader} 读取请求正文来解析方法参数。
 *
 * <p>如果方法参数带有任何
 * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints
 * 触发校验的注解}，则应用校验。校验失败会导致 {@link ServerWebInputException}。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractMessageReaderArgumentResolver extends HandlerMethodArgumentResolverSupport {

	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);


	private final List<HttpMessageReader<?>> messageReaders;


	/**
	 * 使用 {@link HttpMessageReader} 和 {@link Validator} 的构造函数。
	 * @param readers 用于从请求体转换的读取器
	 */
	protected AbstractMessageReaderArgumentResolver(List<HttpMessageReader<?>> readers) {
		this(readers, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 还接受 {@link ReactiveAdapterRegistry} 的构造函数。
	 * @param messageReaders 用于从请求体转换的读取器
	 * @param adapterRegistry 用于从 Flux 和 Mono 适配为其他响应式类型
	 */
	protected AbstractMessageReaderArgumentResolver(
			List<HttpMessageReader<?>> messageReaders, ReactiveAdapterRegistry adapterRegistry) {

		super(adapterRegistry);
		Assert.notEmpty(messageReaders, "At least one HttpMessageReader is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.messageReaders = messageReaders;
	}


	/**
	 * 返回配置的消息转换器。
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}


	/**
	 * 使用 {@link HttpMessageReader} 从方法参数读取正文。
	 * @param bodyParameter 要读取的 {@link MethodParameter}
	 * @param isBodyRequired 如果正文是必需的，则为 true
	 * @param bindingContext 要使用的绑定上下文
	 * @param exchange 当前交换
	 * @return 正文
	 * @see #readBody(MethodParameter, MethodParameter, boolean, BindingContext, ServerWebExchange)
	 */
	protected Mono<Object> readBody(MethodParameter bodyParameter, boolean isBodyRequired,
			BindingContext bindingContext, ServerWebExchange exchange) {

		return this.readBody(bodyParameter, null, isBodyRequired, bindingContext, exchange);
	}

	/**
	 * 使用 {@link HttpMessageReader} 从方法参数读取正文。
	 * @param bodyParam 表示正文的元素类型
	 * @param actualParam 实际的方法参数类型；可能与 {@code bodyParam} 不同，
	 * 例如对于 {@code HttpEntity} 参数。
	 * @param isBodyRequired 如果正文是必需的则返回 true。
	 * @param bindingContext 要使用的绑定上下文
	 * @param exchange 当前交换
	 * @return 包含用于方法参数的值的 Mono
	 * @since 5.0.2
	 */
	protected Mono<Object> readBody(MethodParameter bodyParam, @Nullable MethodParameter actualParam,
			boolean isBodyRequired, BindingContext bindingContext, ServerWebExchange exchange) {

		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParam);
		ResolvableType actualType = (actualParam != null ? ResolvableType.forMethodParameter(actualParam) : bodyType);
		Class<?> resolvedType = bodyType.resolve();
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);
		ResolvableType elementType = (adapter != null ? bodyType.getGeneric() : bodyType);
		isBodyRequired = isBodyRequired || (adapter != null && !adapter.supportsEmpty());

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		MediaType contentType;
		HttpHeaders headers = request.getHeaders();
		try {
			contentType = headers.getContentType();
		}
		catch (InvalidMediaTypeException ex) {
			throw new UnsupportedMediaTypeStatusException(
					"Can't parse Content-Type [" + headers.getFirst("Content-Type") + "]: " + ex.getMessage());
		}

		MediaType mediaType = (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
		Object[] hints = extractValidationHints(bodyParam);

		if (mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Form data is accessed via ServerWebExchange.getFormData() in WebFlux.");
			}
			return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + (contentType != null ?
					"Content-Type:" + contentType :
					"No Content-Type, using " + MediaType.APPLICATION_OCTET_STREAM));
		}

		for (HttpMessageReader<?> reader : getMessageReaders()) {
			if (reader.canRead(elementType, mediaType)) {
				Map<String, Object> readHints = Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix());
				if (adapter != null && adapter.isMultiValue()) {
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "0..N [" + elementType + "]");
					}
					Flux<?> flux = reader.read(actualType, elementType, request, response, readHints);
					flux = flux.onErrorResume(ex -> Flux.error(handleReadError(bodyParam, ex)));
					if (isBodyRequired) {
						flux = flux.switchIfEmpty(Flux.error(() -> handleMissingBody(bodyParam)));
					}
					if (hints != null) {
						flux = flux.doOnNext(target ->
								validate(target, hints, bodyParam, bindingContext, exchange));
					}
					return Mono.just(adapter.fromPublisher(flux));
				}
				else {
					// Single-value (with or without reactive type wrapper)
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "0..1 [" + elementType + "]");
					}
					Mono<?> mono = reader.readMono(actualType, elementType, request, response, readHints);
					mono = mono.onErrorResume(ex -> Mono.error(handleReadError(bodyParam, ex)));
					if (isBodyRequired) {
						mono = mono.switchIfEmpty(Mono.error(() -> handleMissingBody(bodyParam)));
					}
					if (hints != null) {
						mono = mono.doOnNext(target ->
								validate(target, hints, bodyParam, bindingContext, exchange));
					}
					return (adapter != null ? Mono.just(adapter.fromPublisher(mono)) : Mono.from(mono));
				}
			}
		}

		// No compatible reader but body may be empty.

		HttpMethod method = request.getMethod();
		if (contentType == null && method != null && SUPPORTED_METHODS.contains(method)) {
			Flux<DataBuffer> body = request.getBody().doOnNext(buffer -> {
				DataBufferUtils.release(buffer);
				// Body not empty, back toy 415..
				throw new UnsupportedMediaTypeStatusException(
						mediaType, getSupportedMediaTypes(elementType), elementType);
			});
			if (isBodyRequired) {
				body = body.switchIfEmpty(Mono.error(() -> handleMissingBody(bodyParam)));
			}
			return (adapter != null ? Mono.just(adapter.fromPublisher(body)) : Mono.from(body));
		}

		return Mono.error(new UnsupportedMediaTypeStatusException(
				mediaType, getSupportedMediaTypes(elementType), elementType));
	}

	private Throwable handleReadError(MethodParameter parameter, Throwable ex) {
		return (ex instanceof DecodingException ?
				new ServerWebInputException("Failed to read HTTP message", parameter, ex) : ex);
	}

	private ServerWebInputException handleMissingBody(MethodParameter parameter) {
		String paramInfo = parameter.getExecutable().toGenericString();
		return new ServerWebInputException("Request body is missing: " + paramInfo, parameter);
	}

	/**
	 * 检查给定 MethodParameter 是否需要校验；如果需要，则返回包含校验提示的（可能为空的）Object[]。返回值为 {@code null} 表示不需要校验。
	 */
	@Nullable
	private Object[] extractValidationHints(MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Object[] hints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (hints != null) {
				return hints;
			}
		}
		return null;
	}

	private void validate(Object target, Object[] validationHints, MethodParameter param,
			BindingContext binding, ServerWebExchange exchange) {

		String name = Conventions.getVariableNameForParameter(param);
		WebExchangeDataBinder binder = binding.createDataBinder(exchange, target, name);
		binder.validate(validationHints);
		if (binder.getBindingResult().hasErrors()) {
			throw new WebExchangeBindException(param, binder.getBindingResult());
		}
	}

	private List<MediaType> getSupportedMediaTypes(ResolvableType elementType) {
		List<MediaType> mediaTypes = new ArrayList<>();
		for (HttpMessageReader<?> reader : this.messageReaders) {
			mediaTypes.addAll(reader.getReadableMediaTypes(elementType));
		}
		return mediaTypes;
	}

}
