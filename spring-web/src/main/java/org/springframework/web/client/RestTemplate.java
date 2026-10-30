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

package org.springframework.web.client;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.SpringProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;
import org.springframework.web.util.UriTemplateHandler;

/**
 * 执行 HTTP 请求的同步客户端，在 JDK {@code HttpURLConnection}、
 * Apache HttpComponents 等底层 HTTP 客户端库之上提供简单、模板化的方法 API。
 * 除了支持较少见场景的通用 {@code exchange} 和 {@code execute} 方法外，
 * RestTemplate 还按 HTTP 方法为常见场景提供了模板。
 *
 * <p>RestTemplate 通常作为共享组件使用。但是，其配置不支持并发修改，
 * 因此其配置通常在启动时准备。如有必要，可以在启动时创建多个配置不同的
 * RestTemplate 实例。如果这些实例需要共享 HTTP 客户端资源，
 * 它们可以使用相同的底层 {@link ClientHttpRequestFactory}。
 *
 * <p><strong>注意：</strong>自 5.0 起，此类处于维护模式，
 * 今后只接受较小的变更请求和错误修复。请考虑使用
 * {@code org.springframework.web.reactive.client.WebClient}，
 * 它拥有更现代的 API 并支持同步、异步和流式场景。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @since 3.0
 * @see HttpMessageConverter
 * @see RequestCallback
 * @see ResponseExtractor
 * @see ResponseErrorHandler
 */
public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示 Spring
	 * 忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	private static final boolean romePresent;

	private static final boolean jaxb2Present;

	private static final boolean jackson2Present;

	private static final boolean jackson2XmlPresent;

	private static final boolean jackson2SmilePresent;

	private static final boolean jackson2CborPresent;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean kotlinSerializationJsonPresent;

	static {
		ClassLoader classLoader = RestTemplate.class.getClassLoader();
		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}


	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

	private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

	private UriTemplateHandler uriTemplateHandler;

	private final ResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();


	/**
	 * 使用默认设置创建一个新的 {@link RestTemplate} 实例。
	 * 会初始化默认的 {@link HttpMessageConverter HttpMessageConverters}。
	 */
	public RestTemplate() {
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(new StringHttpMessageConverter());
		this.messageConverters.add(new ResourceHttpMessageConverter(false));
		if (!shouldIgnoreXml) {
			try {
				this.messageConverters.add(new SourceHttpMessageConverter<>());
			}
			catch (Error err) {
				// Ignore when no TransformerFactory implementation is available
			}
		}
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		if (romePresent) {
			this.messageConverters.add(new AtomFeedHttpMessageConverter());
			this.messageConverters.add(new RssChannelHttpMessageConverter());
		}

		if (!shouldIgnoreXml) {
			if (jackson2XmlPresent) {
				this.messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
			}
			else if (jaxb2Present) {
				this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			}
		}

		if (kotlinSerializationJsonPresent) {
			this.messageConverters.add(new KotlinSerializationJsonHttpMessageConverter());
		}
		if (jackson2Present) {
			this.messageConverters.add(new MappingJackson2HttpMessageConverter());
		}
		else if (gsonPresent) {
			this.messageConverters.add(new GsonHttpMessageConverter());
		}
		else if (jsonbPresent) {
			this.messageConverters.add(new JsonbHttpMessageConverter());
		}

		if (jackson2SmilePresent) {
			this.messageConverters.add(new MappingJackson2SmileHttpMessageConverter());
		}
		if (jackson2CborPresent) {
			this.messageConverters.add(new MappingJackson2CborHttpMessageConverter());
		}

		this.uriTemplateHandler = initUriTemplateHandler();
	}

	/**
	 * 基于给定的 {@link ClientHttpRequestFactory} 创建一个新的 {@link RestTemplate} 实例。
	 * @param requestFactory 要使用的 HTTP 请求工厂
	 * @see org.springframework.http.client.SimpleClientHttpRequestFactory
	 * @see org.springframework.http.client.HttpComponentsClientHttpRequestFactory
	 */
	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		this();
		setRequestFactory(requestFactory);
	}

	/**
	 * 使用给定的 {@link HttpMessageConverter} 列表创建一个新的 {@link RestTemplate} 实例。
	 * @param messageConverters 要使用的 {@link HttpMessageConverter} 列表
	 * @since 3.2.7
	 */
	public RestTemplate(List<HttpMessageConverter<?>> messageConverters) {
		validateConverters(messageConverters);
		this.messageConverters.addAll(messageConverters);
		this.uriTemplateHandler = initUriTemplateHandler();
	}


	private static DefaultUriBuilderFactory initUriTemplateHandler() {
		DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
		uriFactory.setEncodingMode(EncodingMode.URI_COMPONENT);  // for backwards compatibility..
		return uriFactory;
	}


	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于在 HTTP 请求和响应之间进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		validateConverters(messageConverters);
		// Take getMessageConverters() List as-is when passed in here
		if (this.messageConverters != messageConverters) {
			this.messageConverters.clear();
			this.messageConverters.addAll(messageConverters);
		}
	}

	private void validateConverters(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notEmpty(messageConverters, "At least one HttpMessageConverter is required");
		Assert.noNullElements(messageConverters, "The HttpMessageConverter list must not contain null elements");
	}

	/**
	 * 返回消息体转换器列表。
	 * <p>返回的 {@link List} 是活动的，可能会被追加内容。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * 设置错误处理器。
	 * <p>默认情况下，RestTemplate 使用 {@link DefaultResponseErrorHandler}。
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回错误处理器。
	 */
	public ResponseErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * 配置默认的 URI 变量值。这是以下代码的快捷方式：
	 * <pre class="code">
	 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
	 * handler.setDefaultUriVariables(...);
	 *
	 * RestTemplate restTemplate = new RestTemplate();
	 * restTemplate.setUriTemplateHandler(handler);
	 * </pre>
	 * @param uriVars 默认 URI 变量值
	 * @since 4.3
	 */
	@SuppressWarnings("deprecation")
	public void setDefaultUriVariables(Map<String, ?> uriVars) {
		if (this.uriTemplateHandler instanceof DefaultUriBuilderFactory) {
			((DefaultUriBuilderFactory) this.uriTemplateHandler).setDefaultUriVariables(uriVars);
		}
		else if (this.uriTemplateHandler instanceof org.springframework.web.util.AbstractUriTemplateHandler) {
			((org.springframework.web.util.AbstractUriTemplateHandler) this.uriTemplateHandler)
					.setDefaultUriVariables(uriVars);
		}
		else {
			throw new IllegalArgumentException(
					"This property is not supported with the configured UriTemplateHandler.");
		}
	}

	/**
	 * 配置用于展开 URI 模板的策略。
	 * <p>默认使用 {@link DefaultUriBuilderFactory}，并且为了向后兼容，
	 * 编码模式设置为 {@link EncodingMode#URI_COMPONENT URI_COMPONENT}。
	 * 从 5.0.8 起，建议使用
	 * {@link EncodingMode#TEMPLATE_AND_VALUES TEMPLATE_AND_VALUES}。
	 * <p><strong>注意：</strong>在 5.0 中，默认实现从
	 * {@link org.springframework.web.util.DefaultUriTemplateHandler
	 * DefaultUriTemplateHandler}（在 4.3 中已弃用）切换为
	 * {@link DefaultUriBuilderFactory}，这为 {@code parsePath} 属性
	 * 引入了不同的默认值（从 false 切换为 true）。
	 * @param handler 要使用的 URI 模板处理器
	 */
	public void setUriTemplateHandler(UriTemplateHandler handler) {
		Assert.notNull(handler, "UriTemplateHandler must not be null");
		this.uriTemplateHandler = handler;
	}

	/**
	 * 返回所配置的 URI 模板处理器。
	 */
	public UriTemplateHandler getUriTemplateHandler() {
		return this.uriTemplateHandler;
	}


	// GET

	@Override
	@Nullable
	public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T getForObject(URI url, Class<T> responseType) throws RestClientException {
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
	}

	@Override
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException {
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor));
	}


	// HEAD

	@Override
	public HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException {
		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
	}

	@Override
	public HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
	}

	@Override
	public HttpHeaders headForHeaders(URI url) throws RestClientException {
		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor()));
	}


	// POST

	@Override
	@Nullable
	public URI postForLocation(String url, @Nullable Object request, Object... uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request);
		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
		return (headers != null ? headers.getLocation() : null);
	}

	@Override
	@Nullable
	public URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request);
		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
		return (headers != null ? headers.getLocation() : null);
	}

	@Override
	@Nullable
	public URI postForLocation(URI url, @Nullable Object request) throws RestClientException {
		RequestCallback requestCallback = httpEntityCallback(request);
		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor());
		return (headers != null ? headers.getLocation() : null);
	}

	@Override
	@Nullable
	public <T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
			Object... uriVariables) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T postForObject(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
	}

	@Override
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
			Class<T> responseType, Object... uriVariables) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor));
	}


	// PUT

	@Override
	public void put(String url, @Nullable Object request, Object... uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request);
		execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public void put(String url, @Nullable Object request, Map<String, ?> uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request);
		execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public void put(URI url, @Nullable Object request) throws RestClientException {
		RequestCallback requestCallback = httpEntityCallback(request);
		execute(url, HttpMethod.PUT, requestCallback, null);
	}


	// PATCH

	@Override
	@Nullable
	public <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType,
			Object... uriVariables) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor);
	}


	// DELETE

	@Override
	public void delete(String url, Object... uriVariables) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public void delete(String url, Map<String, ?> uriVariables) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public void delete(URI url) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null);
	}


	// OPTIONS

	@Override
	public Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
		return (headers != null ? headers.getAllow() : Collections.emptySet());
	}

	@Override
	public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
		return (headers != null ? headers.getAllow() : Collections.emptySet());
	}

	@Override
	public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor);
		return (headers != null ? headers.getAllow() : Collections.emptySet());
	}


	// exchange

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType) throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, method, requestCallback, responseExtractor));
	}

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {

		Type type = responseType.getType();
		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		Type type = responseType.getType();
		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType) throws RestClientException {

		Type type = responseType.getType();
		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return nonNull(execute(url, method, requestCallback, responseExtractor));
	}

	@Override
	public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, Class<T> responseType)
			throws RestClientException {

		RequestCallback requestCallback = httpEntityCallback(entity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(doExecute(resolveUrl(entity), entity.getMethod(), requestCallback, responseExtractor));
	}

	@Override
	public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, ParameterizedTypeReference<T> responseType)
			throws RestClientException {

		Type type = responseType.getType();
		RequestCallback requestCallback = httpEntityCallback(entity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return nonNull(doExecute(resolveUrl(entity), entity.getMethod(), requestCallback, responseExtractor));
	}

	private URI resolveUrl(RequestEntity<?> entity) {
		if (entity instanceof RequestEntity.UriTemplateRequestEntity) {
			RequestEntity.UriTemplateRequestEntity<?> ext = (RequestEntity.UriTemplateRequestEntity<?>) entity;
			if (ext.getVars() != null) {
				return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVars());
			}
			else if (ext.getVarsMap() != null) {
				return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVarsMap());
			}
			else {
				throw new IllegalStateException("No variables specified for URI template: " + ext.getUriTemplate());
			}
		}
		else {
			return entity.getUrl();
		}
	}


	// General execution

	/**
	 * {@inheritDoc}
	 * <p>如果只需要提供 {@code RequestCallback} 或 {@code ResponseExtractor}
	 * 之一，而不需要同时提供两者，可以考虑使用：
	 * <ul>
	 * <li>{@link #acceptHeaderRequestCallback(Class)}
	 * <li>{@link #httpEntityCallback(Object)}
	 * <li>{@link #httpEntityCallback(Object, Type)}
	 * <li>{@link #responseEntityExtractor(Type)}
	 * </ul>
	 */
	@Override
	@Nullable
	public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {

		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	/**
	 * {@inheritDoc}
	 * <p>如果只需要提供 {@code RequestCallback} 或 {@code ResponseExtractor}
	 * 之一，而不需要同时提供两者，可以考虑使用：
	 * <ul>
	 * <li>{@link #acceptHeaderRequestCallback(Class)}
	 * <li>{@link #httpEntityCallback(Object)}
	 * <li>{@link #httpEntityCallback(Object, Type)}
	 * <li>{@link #responseEntityExtractor(Type)}
	 * </ul>
	 */
	@Override
	@Nullable
	public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables)
			throws RestClientException {

		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	/**
	 * {@inheritDoc}
	 * <p>如果只需要提供 {@code RequestCallback} 或 {@code ResponseExtractor}
	 * 之一，而不需要同时提供两者，可以考虑使用：
	 * <ul>
	 * <li>{@link #acceptHeaderRequestCallback(Class)}
	 * <li>{@link #httpEntityCallback(Object)}
	 * <li>{@link #httpEntityCallback(Object, Type)}
	 * <li>{@link #responseEntityExtractor(Type)}
	 * </ul>
	 */
	@Override
	@Nullable
	public <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		return doExecute(url, method, requestCallback, responseExtractor);
	}

	/**
	 * 在给定的 URI 上执行指定的方法。
	 * <p>{@link ClientHttpRequest} 使用 {@link RequestCallback} 进行处理；
	 * 响应则使用 {@link ResponseExtractor} 进行处理。
	 * @param url 要连接的完整展开后的 URL
	 * @param method 要执行的 HTTP 方法（GET、POST 等）
	 * @param requestCallback 准备请求的对象（可以为 {@code null}）
	 * @param responseExtractor 从响应中提取返回值的对象（可以为 {@code null}）
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	@Nullable
	protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "URI is required");
		Assert.notNull(method, "HttpMethod is required");
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = createRequest(url, method);
			if (requestCallback != null) {
				requestCallback.doWithRequest(request);
			}
			response = request.execute();
			handleResponse(url, method, response);
			return (responseExtractor != null ? responseExtractor.extractData(response) : null);
		}
		catch (IOException ex) {
			String resource = url.toString();
			String query = url.getRawQuery();
			resource = (query != null ? resource.substring(0, resource.indexOf('?')) : resource);
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + resource + "\": " + ex.getMessage(), ex);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * 处理给定的响应，执行适当的日志记录，并在必要时调用
	 * {@link ResponseErrorHandler}。
	 * <p>可以在子类中重写。
	 * @param url 要连接的完整展开后的 URL
	 * @param method 要执行的 HTTP 方法（GET、POST 等）
	 * @param response 得到的 {@link ClientHttpResponse}
	 * @throws IOException 如果由 {@link ResponseErrorHandler} 传播而来
	 * @since 4.1.6
	 * @see #setErrorHandler
	 */
	protected void handleResponse(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		ResponseErrorHandler errorHandler = getErrorHandler();
		boolean hasError = errorHandler.hasError(response);
		if (logger.isDebugEnabled()) {
			try {
				int code = response.getRawStatusCode();
				HttpStatus status = HttpStatus.resolve(code);
				logger.debug("Response " + (status != null ? status : code));
			}
			catch (IOException ex) {
				logger.debug("Failed to obtain response status code", ex);
			}
		}
		if (hasError) {
			errorHandler.handleError(url, method, response);
		}
	}

	/**
	 * 返回一个 {@code RequestCallback}，它根据给定的响应类型设置请求的
	 * {@code Accept} 头，并会与配置的消息转换器进行交叉检查。
	 */
	public <T> RequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
		return new AcceptHeaderRequestCallback(responseType);
	}

	/**
	 * 返回一个 {@code RequestCallback} 实现，用于将给定对象写入请求流。
	 */
	public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody) {
		return new HttpEntityRequestCallback(requestBody);
	}

	/**
	 * 返回一个 {@code RequestCallback} 实现，它会：
	 * <ol>
	 * <li>根据给定的响应类型设置请求的 {@code Accept} 头，
	 * 并会与配置的消息转换器进行交叉检查。
	 * <li>将给定对象写入请求流。
	 * </ol>
	 */
	public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody, Type responseType) {
		return new HttpEntityRequestCallback(requestBody, responseType);
	}

	/**
	 * 返回一个用于准备 {@link ResponseEntity} 的 {@code ResponseExtractor}。
	 */
	public <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
		return new ResponseEntityResponseExtractor<>(responseType);
	}

	/**
	 * 返回用于提取 {@link HttpHeaders} 的响应提取器。
	 */
	protected ResponseExtractor<HttpHeaders> headersExtractor() {
		return this.headersExtractor;
	}

	private static <T> T nonNull(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}


	/**
	 * 用于准备请求 accept 头的请求回调实现。
	 */
	private class AcceptHeaderRequestCallback implements RequestCallback {

		@Nullable
		private final Type responseType;

		public AcceptHeaderRequestCallback(@Nullable Type responseType) {
			this.responseType = responseType;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			if (this.responseType != null) {
				List<MediaType> allSupportedMediaTypes = getMessageConverters().stream()
						.filter(converter -> canReadResponse(this.responseType, converter))
						.flatMap((HttpMessageConverter<?> converter) -> getSupportedMediaTypes(this.responseType, converter))
						.distinct()
						.sorted(MediaType.SPECIFICITY_COMPARATOR)
						.collect(Collectors.toList());
				if (logger.isDebugEnabled()) {
					logger.debug("Accept=" + allSupportedMediaTypes);
				}
				request.getHeaders().setAccept(allSupportedMediaTypes);
			}
		}

		private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
			Class<?> responseClass = (responseType instanceof Class ? (Class<?>) responseType : null);
			if (responseClass != null) {
				return converter.canRead(responseClass, null);
			}
			else if (converter instanceof GenericHttpMessageConverter) {
				GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
				return genericConverter.canRead(responseType, null, null);
			}
			return false;
		}

		private Stream<MediaType> getSupportedMediaTypes(Type type, HttpMessageConverter<?> converter) {
			Type rawType = (type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType() : type);
			Class<?> clazz = (rawType instanceof Class ? (Class<?>) rawType : null);
			return (clazz != null ? converter.getSupportedMediaTypes(clazz) : converter.getSupportedMediaTypes())
					.stream()
					.map(mediaType -> {
						if (mediaType.getCharset() != null) {
							return new MediaType(mediaType.getType(), mediaType.getSubtype());
						}
						return mediaType;
					});
		}
	}


	/**
	 * 用于将给定对象写入请求流的请求回调实现。
	 */
	private class HttpEntityRequestCallback extends AcceptHeaderRequestCallback {

		private final HttpEntity<?> requestEntity;

		public HttpEntityRequestCallback(@Nullable Object requestBody) {
			this(requestBody, null);
		}

		public HttpEntityRequestCallback(@Nullable Object requestBody, @Nullable Type responseType) {
			super(responseType);
			if (requestBody instanceof HttpEntity) {
				this.requestEntity = (HttpEntity<?>) requestBody;
			}
			else if (requestBody != null) {
				this.requestEntity = new HttpEntity<>(requestBody);
			}
			else {
				this.requestEntity = HttpEntity.EMPTY;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
			super.doWithRequest(httpRequest);
			Object requestBody = this.requestEntity.getBody();
			if (requestBody == null) {
				HttpHeaders httpHeaders = httpRequest.getHeaders();
				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
				if (!requestHeaders.isEmpty()) {
					requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
				}
				if (httpHeaders.getContentLength() < 0) {
					httpHeaders.setContentLength(0L);
				}
			}
			else {
				Class<?> requestBodyClass = requestBody.getClass();
				Type requestBodyType = (this.requestEntity instanceof RequestEntity ?
						((RequestEntity<?>)this.requestEntity).getType() : requestBodyClass);
				HttpHeaders httpHeaders = httpRequest.getHeaders();
				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
				MediaType requestContentType = requestHeaders.getContentType();
				for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
					if (messageConverter instanceof GenericHttpMessageConverter) {
						GenericHttpMessageConverter<Object> genericConverter =
								(GenericHttpMessageConverter<Object>) messageConverter;
						if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
							if (!requestHeaders.isEmpty()) {
								requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
							}
							logBody(requestBody, requestContentType, genericConverter);
							genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
							return;
						}
					}
					else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
						if (!requestHeaders.isEmpty()) {
							requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
						}
						logBody(requestBody, requestContentType, messageConverter);
						((HttpMessageConverter<Object>) messageConverter).write(
								requestBody, requestContentType, httpRequest);
						return;
					}
				}
				String message = "No HttpMessageConverter for " + requestBodyClass.getName();
				if (requestContentType != null) {
					message += " and content type \"" + requestContentType + "\"";
				}
				throw new RestClientException(message);
			}
		}

		private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
			if (logger.isDebugEnabled()) {
				if (mediaType != null) {
					logger.debug("Writing [" + body + "] as \"" + mediaType + "\"");
				}
				else {
					logger.debug("Writing [" + body + "] with " + converter.getClass().getName());
				}
			}
		}
	}


	/**
	 * 用于 {@link HttpEntity} 的响应提取器。
	 */
	private class ResponseEntityResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {

		@Nullable
		private final HttpMessageConverterExtractor<T> delegate;

		public ResponseEntityResponseExtractor(@Nullable Type responseType) {
			if (responseType != null && Void.class != responseType) {
				this.delegate = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
			}
			else {
				this.delegate = null;
			}
		}

		@Override
		public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
			if (this.delegate != null) {
				T body = this.delegate.extractData(response);
				return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).body(body);
			}
			else {
				return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).build();
			}
		}
	}


	/**
	 * 用于提取响应 {@link HttpHeaders} 的响应提取器。
	 */
	private static class HeadersExtractor implements ResponseExtractor<HttpHeaders> {

		@Override
		public HttpHeaders extractData(ClientHttpResponse response) {
			return response.getHeaders();
		}
	}

}
