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

package org.springframework.http.converter.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import com.googlecode.protobuf.format.FormatFactory;
import com.googlecode.protobuf.format.ProtobufFormatter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * {@code HttpMessageConverter}，使用
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * 读写 {@link com.google.protobuf.Message com.google.protobuf.Message}。
 *
 * <p>要生成 {@code Message} Java 类，需要安装 {@code protoc} 二进制文件。
 *
 * <p>使用官方的 {@code "com.google.protobuf:protobuf-java"} 库时，
 * 此转换器默认支持 {@code "application/x-protobuf"} 和 {@code "text/plain"}。
 * 在类路径上提供以下任一附加库即可支持其他格式：
 * <ul>
 * <li>使用第三方的 {@code "com.googlecode.protobuf-java-format:protobuf-java-format"} 库
 * 支持 {@code "application/json"}、{@code "application/xml"} 和
 * {@code "text/html"}（只写）
 * <li>对于 Protobuf 3，使用官方的 {@code "com.google.protobuf:protobuf-java-util"}
 * 支持 {@code "application/json"}（可配置的变体请参阅
 * {@link ProtobufJsonFormatHttpMessageConverter}）
 * </ul>
 *
 * <p>要求 Protobuf 2.6 或更高版本（格式化功能要求 Protobuf Java Format 1.4 或更高版本）。
 * 如果不存在基于 Protobuf 2 的 {@code protobuf-java-format}，此转换器将自动适配
 * Protobuf 3 及其默认的 {@code protobuf-java-util} JSON 格式；不过，
 * 如果需要在 Protobuf 3 上进行更明确的 JSON 配置，请考虑使用
 * {@link ProtobufJsonFormatHttpMessageConverter}。
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.1
 * @see FormatFactory
 * @see JsonFormat
 * @see ProtobufJsonFormatHttpMessageConverter
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * protobuf 的媒体类型 {@code application/x-protobuf}。
	 */
	public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

	/**
	 * 包含 protobuf schema 的 HTTP 头。
	 */
	public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	/**
	 * 包含 protobuf 消息的 HTTP 头。
	 */
	public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";


	private static final Map<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

	final ExtensionRegistry extensionRegistry;

	@Nullable
	private final ProtobufFormatSupport protobufFormatSupport;


	/**
	 * 构造一个新的 {@code ProtobufHttpMessageConverter}。
	 */
	public ProtobufHttpMessageConverter() {
		this(null, null);
	}

	/**
	 * 构造一个新的 {@code ProtobufHttpMessageConverter}，
	 * 并提供一个允许注册消息扩展的初始化器。
	 * @param registryInitializer 消息扩展的初始化器
	 * @deprecated as of Spring Framework 5.1, use {@link #ProtobufHttpMessageConverter(ExtensionRegistry)} instead
	 */
	@Deprecated
	public ProtobufHttpMessageConverter(@Nullable ExtensionRegistryInitializer registryInitializer) {
		this(null, null);
		if (registryInitializer != null) {
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
	}

	/**
	 * 构造一个新的 {@code ProtobufHttpMessageConverter}，
	 * 并提供一个指定 Protocol Message 扩展的注册表。
	 * @param extensionRegistry 要填充的注册表
	 */
	public ProtobufHttpMessageConverter(ExtensionRegistry extensionRegistry) {
		this(null, extensionRegistry);
	}

	ProtobufHttpMessageConverter(@Nullable ProtobufFormatSupport formatSupport,
			@Nullable ExtensionRegistry extensionRegistry) {

		if (formatSupport != null) {
			this.protobufFormatSupport = formatSupport;
		}
		else if (ClassUtils.isPresent("com.googlecode.protobuf.format.FormatFactory", getClass().getClassLoader())) {
			this.protobufFormatSupport = new ProtobufJavaFormatSupport();
		}
		else if (ClassUtils.isPresent("com.google.protobuf.util.JsonFormat", getClass().getClassLoader())) {
			this.protobufFormatSupport = new ProtobufJavaUtilSupport(null, null);
		}
		else {
			this.protobufFormatSupport = null;
		}

		setSupportedMediaTypes(Arrays.asList(this.protobufFormatSupport != null ?
				this.protobufFormatSupport.supportedMediaTypes() : new MediaType[] {PROTOBUF, TEXT_PLAIN}));

		this.extensionRegistry = (extensionRegistry == null ? ExtensionRegistry.newInstance() : extensionRegistry);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Message.class.isAssignableFrom(clazz);
	}

	@Override
	protected MediaType getDefaultContentType(Message message) {
		return PROTOBUF;
	}

	@Override
	protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = PROTOBUF;
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		Message.Builder builder = getMessageBuilder(clazz);
		if (PROTOBUF.isCompatibleWith(contentType)) {
			builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
		}
		else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
			InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
			TextFormat.merge(reader, this.extensionRegistry, builder);
		}
		else if (this.protobufFormatSupport != null) {
			this.protobufFormatSupport.merge(
					inputMessage.getBody(), charset, contentType, this.extensionRegistry, builder);
		}
		return builder.build();
	}

	/**
	 * 为给定的类创建新的 {@code Message.Builder} 实例。
	 * <p>此方法使用 ConcurrentReferenceHashMap 缓存方法查找。
	 */
	private Message.Builder getMessageBuilder(Class<? extends Message> clazz) {
		try {
			Method method = methodCache.get(clazz);
			if (method == null) {
				method = clazz.getMethod("newBuilder");
				methodCache.put(clazz, method);
			}
			return (Message.Builder) method.invoke(clazz);
		}
		catch (Exception ex) {
			throw new HttpMessageConversionException(
					"Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
		}
	}


	@Override
	protected boolean canWrite(@Nullable MediaType mediaType) {
		return (super.canWrite(mediaType) ||
				(this.protobufFormatSupport != null && this.protobufFormatSupport.supportsWriteOnly(mediaType)));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void writeInternal(Message message, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = getDefaultContentType(message);
			Assert.state(contentType != null, "No content type");
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		if (PROTOBUF.isCompatibleWith(contentType)) {
			setProtoHeader(outputMessage, message);
			CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputMessage.getBody());
			message.writeTo(codedOutputStream);
			codedOutputStream.flush();
		}
		else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
			TextFormat.print(message, outputStreamWriter);  // deprecated on Protobuf 3.9
			outputStreamWriter.flush();
			outputMessage.getBody().flush();
		}
		else if (this.protobufFormatSupport != null) {
			this.protobufFormatSupport.print(message, outputMessage.getBody(), contentType, charset);
			outputMessage.getBody().flush();
		}
	}

	/**
	 * 在以 "application/x-protobuf" 内容类型响应消息时设置
	 * "X-Protobuf-*" HTTP 头
	 * <p><b>注意：</b>之前不应调用 <code>outputMessage.getBody()</code>，
	 * 因为它会写入 HTTP 头（使其变为只读）。</p>
	 */
	private void setProtoHeader(HttpOutputMessage response, Message message) {
		response.getHeaders().set(X_PROTOBUF_SCHEMA_HEADER, message.getDescriptorForType().getFile().getName());
		response.getHeaders().set(X_PROTOBUF_MESSAGE_HEADER, message.getDescriptorForType().getFullName());
	}


	/**
	 * Protobuf 格式支持。
	 */
	interface ProtobufFormatSupport {

		MediaType[] supportedMediaTypes();

		boolean supportsWriteOnly(@Nullable MediaType mediaType);

		void merge(InputStream input, Charset charset, MediaType contentType,
				ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, HttpMessageConversionException;

		void print(Message message, OutputStream output, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException;
	}


	/**
	 * 当 {@code com.googlecode.protobuf.format.FormatFactory} 可用时使用的
	 * {@link ProtobufFormatSupport} 实现。
	 */
	static class ProtobufJavaFormatSupport implements ProtobufFormatSupport {

		private final ProtobufFormatter jsonFormatter;

		private final ProtobufFormatter xmlFormatter;

		private final ProtobufFormatter htmlFormatter;

		public ProtobufJavaFormatSupport() {
			FormatFactory formatFactory = new FormatFactory();
			this.jsonFormatter = formatFactory.createFormatter(FormatFactory.Formatter.JSON);
			this.xmlFormatter = formatFactory.createFormatter(FormatFactory.Formatter.XML);
			this.htmlFormatter = formatFactory.createFormatter(FormatFactory.Formatter.HTML);
		}

		@Override
		public MediaType[] supportedMediaTypes() {
			return new MediaType[] {PROTOBUF, TEXT_PLAIN, APPLICATION_XML, APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
			return TEXT_HTML.isCompatibleWith(mediaType);
		}

		@Override
		public void merge(InputStream input, Charset charset, MediaType contentType,
				ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				this.jsonFormatter.merge(input, charset, extensionRegistry, builder);
			}
			else if (contentType.isCompatibleWith(APPLICATION_XML)) {
				this.xmlFormatter.merge(input, charset, extensionRegistry, builder);
			}
			else {
				throw new HttpMessageConversionException(
						"protobuf-java-format does not support parsing " + contentType);
			}
		}

		@Override
		public void print(Message message, OutputStream output, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				this.jsonFormatter.print(message, output, charset);
			}
			else if (contentType.isCompatibleWith(APPLICATION_XML)) {
				this.xmlFormatter.print(message, output, charset);
			}
			else if (contentType.isCompatibleWith(TEXT_HTML)) {
				this.htmlFormatter.print(message, output, charset);
			}
			else {
				throw new HttpMessageConversionException(
						"protobuf-java-format does not support printing " + contentType);
			}
		}
	}


	/**
	 * 当 {@code com.google.protobuf.util.JsonFormat} 可用时使用的
	 * {@link ProtobufFormatSupport} 实现。
	 */
	static class ProtobufJavaUtilSupport implements ProtobufFormatSupport {

		private final JsonFormat.Parser parser;

		private final JsonFormat.Printer printer;

		public ProtobufJavaUtilSupport(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {
			this.parser = (parser != null ? parser : JsonFormat.parser());
			this.printer = (printer != null ? printer : JsonFormat.printer());
		}

		@Override
		public MediaType[] supportedMediaTypes() {
			return new MediaType[] {PROTOBUF, TEXT_PLAIN, APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
			return false;
		}

		@Override
		public void merge(InputStream input, Charset charset, MediaType contentType,
				ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				InputStreamReader reader = new InputStreamReader(input, charset);
				this.parser.merge(reader, builder);
			}
			else {
				throw new HttpMessageConversionException(
						"protobuf-java-util does not support parsing " + contentType);
			}
		}

		@Override
		public void print(Message message, OutputStream output, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				OutputStreamWriter writer = new OutputStreamWriter(output, charset);
				this.printer.appendTo(message, writer);
				writer.flush();
			}
			else {
				throw new HttpMessageConversionException(
						"protobuf-java-util does not support printing " + contentType);
			}
		}
	}

}
