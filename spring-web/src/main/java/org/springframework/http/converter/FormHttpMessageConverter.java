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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeUtility;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link HttpMessageConverter} 的实现，用于读写"普通"HTML 表单，
 * 也用于写入（但不读取）multipart 数据（例如文件上传）。
 *
 * <p>换言之，此转换器可以将
 * {@code "application/x-www-form-urlencoded"} 媒体类型读写为
 * {@link MultiValueMap MultiValueMap&lt;String, String&gt;}，也可以将
 * {@code "multipart/form-data"} 和 {@code "multipart/mixed"} 媒体类型
 * 写入（但不读取）为
 * {@link MultiValueMap MultiValueMap&lt;String, Object&gt;}。
 *
 * <h3>Multipart 数据</h3>
 *
 * <p>默认情况下，{@linkplain #write 写入} multipart 数据时使用
 * {@code "multipart/form-data"} 作为内容类型。从 Spring Framework 5.2 起，
 * 也可以使用其他 multipart 子类型（如 {@code "multipart/mixed"} 和
 * {@code "multipart/related"}）写入 multipart 数据，前提是该 multipart
 * 子类型已注册为{@linkplain #getSupportedMediaTypes 受支持的媒体类型}，
 * <em>并且</em>在{@linkplain #write 写入} multipart 数据时将要使用的
 * multipart 子类型指定为内容类型。请注意，{@code "multipart/mixed"}
 * 默认已注册为受支持的媒体类型。
 *
 * <p>写入 multipart 数据时，此转换器使用其他
 * {@link HttpMessageConverter HttpMessageConverter} 来写入各个 MIME 部分。
 * 默认注册了用于字节数组、{@code String} 和 {@code Resource} 的基本转换器。
 * 可以通过 {@link #setPartConverters} 覆盖，或通过
 * {@link #addPartConverter} 扩充。
 *
 * <h3>示例</h3>
 *
 * <p>以下代码片段展示了如何使用 {@code "multipart/form-data"}
 * 内容类型提交 HTML 表单。
 *
 * <pre class="code">
 * RestTemplate restTemplate = new RestTemplate();
 * // AllEncompassingFormHttpMessageConverter is configured by default
 *
 * MultiValueMap&lt;String, Object&gt; form = new LinkedMultiValueMap&lt;&gt;();
 * form.add("field 1", "value 1");
 * form.add("field 2", "value 2");
 * form.add("field 2", "value 3");
 * form.add("field 3", 4);  // non-String form values supported as of 5.1.4
 *
 * restTemplate.postForLocation("https://example.com/myForm", form);</pre>
 *
 * <p>以下代码片段展示了如何使用 {@code "multipart/form-data"}
 * 内容类型执行文件上传。
 *
 * <pre class="code">
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * restTemplate.postForLocation("https://example.com/myFileUpload", parts);</pre>
 *
 * <p>以下代码片段展示了如何使用 {@code "multipart/mixed"}
 * 内容类型执行文件上传。
 *
 * <pre class="code">
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * HttpHeaders requestHeaders = new HttpHeaders();
 * requestHeaders.setContentType(MediaType.MULTIPART_MIXED);
 *
 * restTemplate.postForLocation("https://example.com/myFileUpload",
 *     new HttpEntity&lt;&gt;(parts, requestHeaders));</pre>
 *
 * <p>以下代码片段展示了如何使用 {@code "multipart/related"}
 * 内容类型执行文件上传。
 *
 * <pre class="code">
 * MediaType multipartRelated = new MediaType("multipart", "related");
 *
 * restTemplate.getMessageConverters().stream()
 *     .filter(FormHttpMessageConverter.class::isInstance)
 *     .map(FormHttpMessageConverter.class::cast)
 *     .findFirst()
 *     .orElseThrow(() -&gt; new IllegalStateException("Failed to find FormHttpMessageConverter"))
 *     .addSupportedMediaTypes(multipartRelated);
 *
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * HttpHeaders requestHeaders = new HttpHeaders();
 * requestHeaders.setContentType(multipartRelated);
 *
 * restTemplate.postForLocation("https://example.com/myFileUpload",
 *     new HttpEntity&lt;&gt;(parts, requestHeaders));</pre>
 *
 * <h3>其他</h3>
 *
 * <p>此类中的一些方法受
 * {@code org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity} 启发。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter
 * @see org.springframework.util.MultiValueMap
 */
public class FormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final MediaType DEFAULT_FORM_DATA_MEDIA_TYPE =
			new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET);


	private List<MediaType> supportedMediaTypes = new ArrayList<>();

	private List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

	private Charset charset = DEFAULT_CHARSET;

	@Nullable
	private Charset multipartCharset;


	public FormHttpMessageConverter() {
		this.supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
		this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
		this.supportedMediaTypes.add(MediaType.MULTIPART_MIXED);

		this.partConverters.add(new ByteArrayHttpMessageConverter());
		this.partConverters.add(new StringHttpMessageConverter());
		this.partConverters.add(new ResourceHttpMessageConverter());

		applyDefaultCharset();
	}


	/**
	 * 设置此转换器支持的 {@link MediaType} 对象列表。
	 * @see #addSupportedMediaTypes(MediaType...)
	 * @see #getSupportedMediaTypes()
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notNull(supportedMediaTypes, "'supportedMediaTypes' must not be null");
		// Ensure internal list is mutable.
		this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
	}

	/**
	 * 添加此转换器要支持的 {@link MediaType} 对象。
	 * <p>所提供的 {@code MediaType} 对象将被追加到
	 * {@linkplain #getSupportedMediaTypes() 受支持的 MediaType 对象}列表中。
	 * @param supportedMediaTypes a var-args list of {@code MediaType} objects to add
	 * @since 5.2
	 * @see #setSupportedMediaTypes(List)
	 */
	public void addSupportedMediaTypes(MediaType... supportedMediaTypes) {
		Assert.notNull(supportedMediaTypes, "'supportedMediaTypes' must not be null");
		Assert.noNullElements(supportedMediaTypes, "'supportedMediaTypes' must not contain null elements");
		Collections.addAll(this.supportedMediaTypes, supportedMediaTypes);
	}

	/**
	 * {@inheritDoc}
	 * @see #setSupportedMediaTypes(List)
	 * @see #addSupportedMediaTypes(MediaType...)
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * 设置要使用的消息体转换器。这些转换器用于
	 * 将对象转换为 MIME 部分。
	 */
	public void setPartConverters(List<HttpMessageConverter<?>> partConverters) {
		Assert.notEmpty(partConverters, "'partConverters' must not be empty");
		this.partConverters = partConverters;
	}

	/**
	 * 返回为 MIME 部分{@linkplain #setPartConverters 配置的转换器}。
	 * @since 5.3
	 */
	public List<HttpMessageConverter<?>> getPartConverters() {
		return Collections.unmodifiableList(this.partConverters);
	}

	/**
	 * 添加一个消息体转换器。此类转换器用于将对象
	 * 转换为 MIME 部分。
	 */
	public void addPartConverter(HttpMessageConverter<?> partConverter) {
		Assert.notNull(partConverter, "'partConverter' must not be null");
		this.partConverters.add(partConverter);
	}

	/**
	 * 设置当请求或响应的 {@code Content-Type} 头未明确指定时，
	 * 读写表单数据所使用的默认字符集。
	 * <p>从 4.3 起，它还用作 multipart 请求中文本正文转换的
	 * 默认字符集。
	 * <p>从 5.0 起，它还用于部件头，包括
	 * {@code Content-Disposition}（及其 filename 参数），
	 * 除非同时设置了（互斥的）{@link #setMultipartCharset multipartCharset}，
	 * 在这种情况下部件头将以 ASCII 编码，而 <i>filename</i> 使用
	 * RFC 2047 中的 {@code encoded-word} 语法编码。
	 * <p>默认设置为 "UTF-8"。
	 */
	public void setCharset(@Nullable Charset charset) {
		if (charset != this.charset) {
			this.charset = (charset != null ? charset : DEFAULT_CHARSET);
			applyDefaultCharset();
		}
	}

	/**
	 * 将配置的字符集作为默认值应用于已注册的部件转换器。
	 */
	private void applyDefaultCharset() {
		for (HttpMessageConverter<?> candidate : this.partConverters) {
			if (candidate instanceof AbstractHttpMessageConverter) {
				AbstractHttpMessageConverter<?> converter = (AbstractHttpMessageConverter<?>) candidate;
				// Only override default charset if the converter operates with a charset to begin with...
				if (converter.getDefaultCharset() != null) {
					converter.setDefaultCharset(this.charset);
				}
			}
		}
	}

	/**
	 * 设置写入 multipart 数据时用于编码文件名的字符集。
	 * 编码基于 RFC 2047 中定义的 {@code encoded-word} 语法，
	 * 并依赖 {@code javax.mail} 中的 {@code MimeUtility}。
	 * <p>从 5.0 起，默认情况下部件头（包括 {@code Content-Disposition}
	 * 及其 filename 参数）将根据 {@link #setCharset(Charset)} 的设置进行编码，
	 * 默认使用 {@code UTF-8}。
	 * @since 4.1.1
	 * @see <a href="https://en.wikipedia.org/wiki/MIME#Encoded-Word">Encoded-Word</a>
	 */
	public void setMultipartCharset(Charset charset) {
		this.multipartCharset = charset;
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.getType().equalsIgnoreCase("multipart")) {
				// We can't read multipart, so skip this supported media type.
				continue;
			}
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public MultiValueMap<String, String> read(@Nullable Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : this.charset);
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
		for (String pair : pairs) {
			int idx = pair.indexOf('=');
			if (idx == -1) {
				result.add(URLDecoder.decode(pair, charset.name()), null);
			}
			else {
				String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
				String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
				result.add(name, value);
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(MultiValueMap<String, ?> map, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		if (isMultipart(map, contentType)) {
			writeMultipart((MultiValueMap<String, Object>) map, contentType, outputMessage);
		}
		else {
			writeForm((MultiValueMap<String, Object>) map, contentType, outputMessage);
		}
	}


	private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
		if (contentType != null) {
			return contentType.getType().equalsIgnoreCase("multipart");
		}
		for (List<?> values : map.values()) {
			for (Object value : values) {
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

	private void writeForm(MultiValueMap<String, Object> formData, @Nullable MediaType contentType,
			HttpOutputMessage outputMessage) throws IOException {

		contentType = getFormContentType(contentType);
		outputMessage.getHeaders().setContentType(contentType);

		Charset charset = contentType.getCharset();
		Assert.notNull(charset, "No charset"); // should never occur

		byte[] bytes = serializeForm(formData, charset).getBytes(charset);
		outputMessage.getHeaders().setContentLength(bytes.length);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(bytes, outputStream));
		}
		else {
			StreamUtils.copy(bytes, outputMessage.getBody());
		}
	}

	/**
	 * 根据首选的内容类型返回用于写入表单的内容类型。
	 * 默认情况下，此方法返回给定的内容类型，但会添加
	 * {@linkplain #setCharset(Charset) 字符集}（如果其中没有）。
	 * 如果 {@code contentType} 为 {@code null}，
	 * 则返回 {@code application/x-www-form-urlencoded; charset=UTF-8}。
	 * <p>子类可以重写此方法以改变此行为。
	 * @param contentType the preferred content type (can be {@code null})
	 * @return the content type to be used
	 * @since 5.2.2
	 */
	protected MediaType getFormContentType(@Nullable MediaType contentType) {
		if (contentType == null) {
			return DEFAULT_FORM_DATA_MEDIA_TYPE;
		}
		else if (contentType.getCharset() == null) {
			return new MediaType(contentType, this.charset);
		}
		else {
			return contentType;
		}
	}

	protected String serializeForm(MultiValueMap<String, Object> formData, Charset charset) {
		StringBuilder builder = new StringBuilder();
		formData.forEach((name, values) -> {
				if (name == null) {
					Assert.isTrue(CollectionUtils.isEmpty(values), () -> "Null name in form data: " + formData);
					return;
				}
				values.forEach(value -> {
					try {
						if (builder.length() != 0) {
							builder.append('&');
						}
						builder.append(URLEncoder.encode(name, charset.name()));
						if (value != null) {
							builder.append('=');
							builder.append(URLEncoder.encode(String.valueOf(value), charset.name()));
						}
					}
					catch (UnsupportedEncodingException ex) {
						throw new IllegalStateException(ex);
					}
				});
		});

		return builder.toString();
	}

	private void writeMultipart(
			MultiValueMap<String, Object> parts, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException {

		// If the supplied content type is null, fall back to multipart/form-data.
		// Otherwise rely on the fact that isMultipart() already verified the
		// supplied content type is multipart.
		if (contentType == null) {
			contentType = MediaType.MULTIPART_FORM_DATA;
		}

		Map<String, String> parameters = new LinkedHashMap<>(contentType.getParameters().size() + 2);
		parameters.putAll(contentType.getParameters());

		byte[] boundary = generateMultipartBoundary();
		if (!isFilenameCharsetSet()) {
			if (!this.charset.equals(StandardCharsets.UTF_8) &&
					!this.charset.equals(StandardCharsets.US_ASCII)) {
				parameters.put("charset", this.charset.name());
			}
		}
		parameters.put("boundary", new String(boundary, StandardCharsets.US_ASCII));

		// Add parameters to output content type
		contentType = new MediaType(contentType, parameters);
		outputMessage.getHeaders().setContentType(contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(outputStream -> {
				writeParts(outputStream, parts, boundary);
				writeEnd(outputStream, boundary);
			});
		}
		else {
			writeParts(outputMessage.getBody(), parts, boundary);
			writeEnd(outputMessage.getBody(), boundary);
		}
	}

	/**
	 * 当配置了 {@link #setMultipartCharset(Charset)}（即 RFC 2047
	 * 的 {@code encoded-word} 语法）时，部件头需要使用 ASCII 编码，
	 * 否则直接使用配置的 {@link #setCharset(Charset)} 进行编码。
	 */
	private boolean isFilenameCharsetSet() {
		return (this.multipartCharset != null);
	}

	private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
		for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
			String name = entry.getKey();
			for (Object part : entry.getValue()) {
				if (part != null) {
					writeBoundary(os, boundary);
					writePart(name, getHttpEntity(part), os);
					writeNewLine(os);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
		Object partBody = partEntity.getBody();
		if (partBody == null) {
			throw new IllegalStateException("Empty body for part '" + name + "': " + partEntity);
		}
		Class<?> partType = partBody.getClass();
		HttpHeaders partHeaders = partEntity.getHeaders();
		MediaType partContentType = partHeaders.getContentType();
		for (HttpMessageConverter<?> messageConverter : this.partConverters) {
			if (messageConverter.canWrite(partType, partContentType)) {
				Charset charset = isFilenameCharsetSet() ? StandardCharsets.US_ASCII : this.charset;
				HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os, charset);
				multipartMessage.getHeaders().setContentDispositionFormData(name, getFilename(partBody));
				if (!partHeaders.isEmpty()) {
					multipartMessage.getHeaders().putAll(partHeaders);
				}
				((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartMessage);
				return;
			}
		}
		throw new HttpMessageNotWritableException("Could not write request: no suitable HttpMessageConverter " +
				"found for request type [" + partType.getName() + "]");
	}

	/**
	 * 生成 multipart 边界。
	 * <p>此实现委托给
	 * {@link MimeTypeUtils#generateMultipartBoundary()}。
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * 返回给定部件对象的 {@link HttpEntity}。
	 * @param part the part to return an {@link HttpEntity} for
	 * @return the part Object itself it is an {@link HttpEntity},
	 * or a newly built {@link HttpEntity} wrapper for that part
	 */
	protected HttpEntity<?> getHttpEntity(Object part) {
		return (part instanceof HttpEntity ? (HttpEntity<?>) part : new HttpEntity<>(part));
	}

	/**
	 * 返回给定 multipart 部件的文件名。此值将用于
	 * {@code Content-Disposition} 头。
	 * <p>默认实现会在部件为 {@code Resource} 时返回
	 * {@link Resource#getFilename()}，其他情况下返回 {@code null}。
	 * 可在子类中重写。
	 * @param part the part to determine the file name for
	 * @return the filename, or {@code null} if not known
	 */
	@Nullable
	protected String getFilename(Object part) {
		if (part instanceof Resource) {
			Resource resource = (Resource) part;
			String filename = resource.getFilename();
			if (filename != null && this.multipartCharset != null) {
				filename = MimeDelegate.encode(filename, this.multipartCharset.name());
			}
			return filename;
		}
		else {
			return null;
		}
	}


	private void writeBoundary(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	private static void writeEnd(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		os.write('-');
		os.write('-');
		writeNewLine(os);
	}

	private static void writeNewLine(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}


	/**
	 * 用于写入 MIME multipart 的 {@link org.springframework.http.HttpOutputMessage}
	 * 实现。
	 */
	private static class MultipartHttpOutputMessage implements HttpOutputMessage {

		private final OutputStream outputStream;

		private final Charset charset;

		private final HttpHeaders headers = new HttpHeaders();

		private boolean headersWritten = false;

		public MultipartHttpOutputMessage(OutputStream outputStream, Charset charset) {
			this.outputStream = outputStream;
			this.charset = charset;
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public OutputStream getBody() throws IOException {
			writeHeaders();
			return this.outputStream;
		}

		private void writeHeaders() throws IOException {
			if (!this.headersWritten) {
				for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
					byte[] headerName = getBytes(entry.getKey());
					for (String headerValueString : entry.getValue()) {
						byte[] headerValue = getBytes(headerValueString);
						this.outputStream.write(headerName);
						this.outputStream.write(':');
						this.outputStream.write(' ');
						this.outputStream.write(headerValue);
						writeNewLine(this.outputStream);
					}
				}
				writeNewLine(this.outputStream);
				this.headersWritten = true;
			}
		}

		private byte[] getBytes(String name) {
			return name.getBytes(this.charset);
		}
	}


	/**
	 * 内部类，用于避免对 JavaMail API 的硬依赖。
	 */
	private static class MimeDelegate {

		public static String encode(String value, String charset) {
			try {
				return MimeUtility.encodeText(value, charset, null);
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
