/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.TypeUtils;

/**
 * 基于 Jackson 且与内容类型无关的 {@link HttpMessageConverter} 实现的
 * 抽象基类。
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author Arjen Poutsma
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.1
 * @see MappingJackson2HttpMessageConverter
 */
public abstract class AbstractJackson2HttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	private static final Map<String, JsonEncoding> ENCODINGS;

	static {
		ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);
		for (JsonEncoding encoding : JsonEncoding.values()) {
			ENCODINGS.put(encoding.getJavaName(), encoding);
		}
		ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
	}


	/**
	 * 转换器使用的默认字符集。
	 */
	@Nullable
	@Deprecated
	public static final Charset DEFAULT_CHARSET = null;


	protected ObjectMapper defaultObjectMapper;

	@Nullable
	private Map<Class<?>, Map<MediaType, ObjectMapper>> objectMapperRegistrations;

	@Nullable
	private Boolean prettyPrint;

	@Nullable
	private PrettyPrinter ssePrettyPrinter;


	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper) {
		this.defaultObjectMapper = objectMapper;
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
		this.ssePrettyPrinter = prettyPrinter;
	}

	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType supportedMediaType) {
		this(objectMapper);
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType... supportedMediaTypes) {
		this(objectMapper);
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}


	/**
	 * 配置用于对象转换的主 {@code ObjectMapper}。
	 * 如果未设置，将创建默认的 {@link ObjectMapper} 实例。
	 * <p>设置自定义配置的 {@code ObjectMapper} 是进一步控制 JSON 序列化
	 * 过程的一种方式。例如，可以配置扩展的
	 * {@link com.fasterxml.jackson.databind.ser.SerializerFactory}，
	 * 为特定类型提供自定义序列化器。另一种改进序列化过程的方式是
	 * 在待序列化的类型上使用 Jackson 提供的注解，在这种情况下
	 * 无需自定义配置的 ObjectMapper。
	 * @see #registerObjectMappersForType(Class, Consumer)
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * 返回正在使用的主 {@code ObjectMapper}。
	 */
	public ObjectMapper getObjectMapper() {
		return this.defaultObjectMapper;
	}

	/**
	 * 为给定的 {@link Class} 配置要使用的 {@link ObjectMapper} 实例。
	 * 当希望偏离{@link #getObjectMapper() 默认}的 ObjectMapper，
	 * 或希望 {@code ObjectMapper} 随 {@code MediaType} 变化时，这很有用。
	 * <p><strong>注意：</strong>使用此方法会实际关闭对给定类的默认
	 * {@link #getObjectMapper() ObjectMapper} 和
	 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} 的使用。
	 * 因此，此处配置的映射必须{@link MediaType#includes(MediaType) 包含}
	 * 给定类必须支持的每一种 MediaType，这一点很重要。
	 * @param clazz 要为其注册 ObjectMapper 实例的对象类型
	 * @param registrar 用于填充或以其他方式更新给定 Class 的
	 * MediaType 到 ObjectMapper 关联的消费者
	 * @since 5.3.4
	 */
	public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MediaType, ObjectMapper>> registrar) {
		if (this.objectMapperRegistrations == null) {
			this.objectMapperRegistrations = new LinkedHashMap<>();
		}
		Map<MediaType, ObjectMapper> registrations =
				this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
		registrar.accept(registrations);
	}

	/**
	 * 返回给定类的 ObjectMapper 注册信息（如果有）。
	 * @param clazz 要查找其注册项的类
	 * @return 已注册的 MediaType 到 ObjectMapper 关联的映射，
	 * 如果给定类没有任何注册项则为空映射。
	 * @since 5.3.4
	 */
	@Nullable
	public Map<MediaType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				return entry.getValue();
			}
		}
		return Collections.emptyMap();
	}

	@Override
	public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		List<MediaType> result = null;
		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
				result.addAll(entry.getValue().keySet());
			}
		}
		return (CollectionUtils.isEmpty(result) ? getSupportedMediaTypes() : result);
	}

	private Map<Class<?>, Map<MediaType, ObjectMapper>> getObjectMapperRegistrations() {
		return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
	}

	/**
	 * 写入 JSON 时是否使用 {@link DefaultPrettyPrinter}。
	 * 这是按如下方式设置 {@code ObjectMapper} 的快捷方式：
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * converter.setObjectMapper(mapper);
	 * </pre>
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.defaultObjectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return canRead(clazz, null, mediaType);
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		if (!canRead(mediaType)) {
			return false;
		}
		JavaType javaType = getJavaType(type, contextClass);
		ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), mediaType);
		if (objectMapper == null) {
			return false;
		}
		AtomicReference<Throwable> causeRef = new AtomicReference<>();
		if (objectMapper.canDeserialize(javaType, causeRef)) {
			return true;
		}
		logWarningIfNecessary(javaType, causeRef.get());
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		if (!canWrite(mediaType)) {
			return false;
		}
		if (mediaType != null && mediaType.getCharset() != null) {
			Charset charset = mediaType.getCharset();
			if (!ENCODINGS.containsKey(charset.name())) {
				return false;
			}
		}
		ObjectMapper objectMapper = selectObjectMapper(clazz, mediaType);
		if (objectMapper == null) {
			return false;
		}
		AtomicReference<Throwable> causeRef = new AtomicReference<>();
		if (objectMapper.canSerialize(clazz, causeRef)) {
			return true;
		}
		logWarningIfNecessary(clazz, causeRef.get());
		return false;
	}

	/**
	 * 选择要使用的 ObjectMapper，可以是主 ObjectMapper，或者在通过
	 * {@link #registerObjectMappersForType(Class, Consumer)} 自定义了给定类的
	 * 处理方式时使用另一个 ObjectMapper。
	 */
	@Nullable
	private ObjectMapper selectObjectMapper(Class<?> targetType, @Nullable MediaType targetMediaType) {
		if (targetMediaType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
			return this.defaultObjectMapper;
		}
		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
			if (typeEntry.getKey().isAssignableFrom(targetType)) {
				for (Map.Entry<MediaType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
					if (objectMapperEntry.getKey().includes(targetMediaType)) {
						return objectMapperEntry.getValue();
					}
				}
				// No matching registrations
				return null;
			}
		}
		// No registrations
		return this.defaultObjectMapper;
	}

	/**
	 * 确定是否记录来自
	 * {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} 检查的给定异常。
	 * @param type Jackson 测试过（反）序列化能力的类
	 * @param cause 要评估的由 Jackson 抛出的异常
	 * （通常是 {@link JsonMappingException}）
	 * @since 4.3
	 */
	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
		if (cause == null) {
			return;
		}

		// Do not log warning for serializer not found (note: different message wording on Jackson 2.9)
		boolean debugLevel = (cause instanceof JsonMappingException && cause.getMessage().startsWith("Cannot find"));

		if (debugLevel ? logger.isDebugEnabled() : logger.isWarnEnabled()) {
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			if (debugLevel) {
				logger.debug(msg, cause);
			}
			else if (logger.isDebugEnabled()) {
				logger.warn(msg, cause);
			}
			else {
				logger.warn(msg + ": " + cause);
			}
		}
	}

	@Override
	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		JavaType javaType = getJavaType(type, contextClass);
		return readJavaType(javaType, inputMessage);
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		JavaType javaType = getJavaType(clazz, null);
		return readJavaType(javaType, inputMessage);
	}

	private Object readJavaType(JavaType javaType, HttpInputMessage inputMessage) throws IOException {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = getCharset(contentType);

		ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), contentType);
		Assert.state(objectMapper != null, () -> "No ObjectMapper for " + javaType);

		boolean isUnicode = ENCODINGS.containsKey(charset.name()) ||
				"UTF-16".equals(charset.name()) ||
				"UTF-32".equals(charset.name());
		try {
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			if (inputMessage instanceof MappingJacksonInputMessage) {
				Class<?> deserializationView = ((MappingJacksonInputMessage) inputMessage).getDeserializationView();
				if (deserializationView != null) {
					ObjectReader objectReader = objectMapper.readerWithView(deserializationView).forType(javaType);
					if (isUnicode) {
						return objectReader.readValue(inputStream);
					}
					else {
						Reader reader = new InputStreamReader(inputStream, charset);
						return objectReader.readValue(reader);
					}
				}
			}
			if (isUnicode) {
				return objectMapper.readValue(inputStream, javaType);
			}
			else {
				Reader reader = new InputStreamReader(inputStream, charset);
				return objectMapper.readValue(reader, javaType);
			}
		}
		catch (InvalidDefinitionException ex) {
			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
		}
		catch (JsonProcessingException ex) {
			throw new HttpMessageNotReadableException("JSON parse error: " + ex.getOriginalMessage(), ex, inputMessage);
		}
	}

	/**
	 * 确定用于 JSON 输入的字符集。
	 * <p>默认情况下，使用输入 {@code MediaType} 中的字符集，
	 * 否则回退到 {@code UTF-8}。可在子类中重写。
	 * @param contentType HTTP 输入消息的内容类型
	 * @return 要使用的字符集
	 * @since 5.1.18
	 */
	protected Charset getCharset(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		else {
			return StandardCharsets.UTF_8;
		}
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		JsonEncoding encoding = getJsonEncoding(contentType);

		Class<?> clazz = (object instanceof MappingJacksonValue ?
				((MappingJacksonValue) object).getValue().getClass() : object.getClass());
		ObjectMapper objectMapper = selectObjectMapper(clazz, contentType);
		Assert.state(objectMapper != null, () -> "No ObjectMapper for " + clazz.getName());

		OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());
		try (JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, encoding)) {
			writePrefix(generator, object);

			Object value = object;
			Class<?> serializationView = null;
			FilterProvider filters = null;
			JavaType javaType = null;

			if (object instanceof MappingJacksonValue) {
				MappingJacksonValue container = (MappingJacksonValue) object;
				value = container.getValue();
				serializationView = container.getSerializationView();
				filters = container.getFilters();
			}
			if (type != null && TypeUtils.isAssignable(type, value.getClass())) {
				javaType = getJavaType(type, null);
			}

			ObjectWriter objectWriter = (serializationView != null ?
					objectMapper.writerWithView(serializationView) : objectMapper.writer());
			if (filters != null) {
				objectWriter = objectWriter.with(filters);
			}
			if (javaType != null && javaType.isContainerType()) {
				objectWriter = objectWriter.forType(javaType);
			}
			SerializationConfig config = objectWriter.getConfig();
			if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) &&
					config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
				objectWriter = objectWriter.with(this.ssePrettyPrinter);
			}
			objectWriter.writeValue(generator, value);

			writeSuffix(generator, object);
			generator.flush();
		}
		catch (InvalidDefinitionException ex) {
			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
		}
		catch (JsonProcessingException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
		}
	}

	/**
	 * 在主内容之前写入前缀。
	 * @param generator 用于写入内容的生成器。
	 * @param object 要写入输出消息的对象。
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 在主内容之后写入后缀。
	 * @param generator 用于写入内容的生成器。
	 * @param object 要写入输出消息的对象。
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 返回指定类型和上下文类的 Jackson {@link JavaType}。
	 * @param type 要返回其 Jackson JavaType 的泛型类型
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中时
	 * 所在的类（可以为 {@code null}）
	 * @return Jackson 的 JavaType
	 */
	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	/**
	 * 确定用于给定内容类型的 JSON 编码。
	 * @param contentType 调用方请求的媒体类型
	 * @return 要使用的 JSON 编码（永不为 {@code null}）
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			Charset charset = contentType.getCharset();
			JsonEncoding encoding = ENCODINGS.get(charset.name());
			if (encoding != null) {
				return encoding;
			}
		}
		return JsonEncoding.UTF8;
	}

	@Override
	@Nullable
	protected MediaType getDefaultContentType(Object object) throws IOException {
		if (object instanceof MappingJacksonValue) {
			object = ((MappingJacksonValue) object).getValue();
		}
		return super.getDefaultContentType(object);
	}

	@Override
	protected Long getContentLength(Object object, @Nullable MediaType contentType) throws IOException {
		if (object instanceof MappingJacksonValue) {
			object = ((MappingJacksonValue) object).getValue();
		}
		return super.getContentLength(object, contentType);
	}

}
