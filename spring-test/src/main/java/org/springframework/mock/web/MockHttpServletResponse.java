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

package org.springframework.mock.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.http.HttpServletResponse} 接口的 mock 实现。
 *
 * <p>从 Spring 5.0 开始，这组 mock 基于 Servlet 4.0 基线设计。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 1.0.2
 */
public class MockHttpServletResponse implements HttpServletResponse {

	private static final String CHARSET_PREFIX = "charset=";

	private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");


	//---------------------------------------------------------------------
	// ServletResponse properties
	//---------------------------------------------------------------------

	private boolean outputStreamAccessAllowed = true;

	private boolean writerAccessAllowed = true;

	private String defaultCharacterEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

	private String characterEncoding = this.defaultCharacterEncoding;

	/**
	 * 如果已通过 {@link HttpServletResponse} 方法或
	 * {@code Content-Type} 上的 {@code charset} 参数
	 * 显式设置字符编码，则为 {@code true}。
	 */
	private boolean characterEncodingSet = false;

	private final ByteArrayOutputStream content = new ByteArrayOutputStream(1024);

	private final ServletOutputStream outputStream = new ResponseServletOutputStream(this.content);

	@Nullable
	private PrintWriter writer;

	private long contentLength = 0;

	@Nullable
	private String contentType;

	private int bufferSize = 4096;

	private boolean committed;

	private Locale locale = Locale.getDefault();


	//---------------------------------------------------------------------
	// HttpServletResponse properties
	//---------------------------------------------------------------------

	private final List<Cookie> cookies = new ArrayList<>();

	private final Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<>();

	private int status = HttpServletResponse.SC_OK;

	@Nullable
	private String errorMessage;


	//---------------------------------------------------------------------
	// Properties for MockRequestDispatcher
	//---------------------------------------------------------------------

	@Nullable
	private String forwardedUrl;

	private final List<String> includedUrls = new ArrayList<>();


	//---------------------------------------------------------------------
	// ServletResponse interface
	//---------------------------------------------------------------------

	/**
	 * 设置是否允许访问 {@link #getOutputStream()}。
	 * <p>默认值为 {@code true}。
	 */
	public void setOutputStreamAccessAllowed(boolean outputStreamAccessAllowed) {
		this.outputStreamAccessAllowed = outputStreamAccessAllowed;
	}

	/**
	 * 返回是否允许访问 {@link #getOutputStream()}。
	 */
	public boolean isOutputStreamAccessAllowed() {
		return this.outputStreamAccessAllowed;
	}

	/**
	 * 设置是否允许访问 {@link #getWriter()}。
	 * <p>默认值为 {@code true}。
	 */
	public void setWriterAccessAllowed(boolean writerAccessAllowed) {
		this.writerAccessAllowed = writerAccessAllowed;
	}

	/**
	 * 返回是否允许访问 {@link #getOutputStream()}。
	 */
	public boolean isWriterAccessAllowed() {
		return this.writerAccessAllowed;
	}

	/**
	 * 设置响应的<em>默认</em>字符编码。
	 * <p>如果未调用此方法，将使用 {@code ISO-8859-1} 作为
	 * 默认字符编码。
	 * <p>如果尚未通过 {@link #setCharacterEncoding(String)}
	 * 或 {@link #setContentType(String)} 显式设置响应的
	 * {@linkplain #getCharacterEncoding() 字符编码}，
	 * 则响应的字符编码将设置为提供的默认字符编码。
	 * @param characterEncoding 默认字符编码
	 * @since 5.3.10
	 * @see #setCharacterEncoding(String)
	 * @see #setContentType(String)
	 */
	public void setDefaultCharacterEncoding(String characterEncoding) {
		Assert.notNull(characterEncoding, "'characterEncoding' must not be null");
		this.defaultCharacterEncoding = characterEncoding;
		if (!this.characterEncodingSet) {
			this.characterEncoding = characterEncoding;
		}
	}

	/**
	 * 确定字符编码是否已通过
	 * {@link HttpServletResponse} 方法或 {@code Content-Type} 上的
	 * {@code charset} 参数显式设置。
	 * <p>如果为 {@code false}，{@link #getCharacterEncoding()} 将返回
	 * {@linkplain #setDefaultCharacterEncoding(String) 默认字符编码}。
	 */
	public boolean isCharset() {
		return this.characterEncodingSet;
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		setExplicitCharacterEncoding(characterEncoding);
		updateContentTypePropertyAndHeader();
	}

	private void setExplicitCharacterEncoding(String characterEncoding) {
		Assert.notNull(characterEncoding, "'characterEncoding' must not be null");
		this.characterEncoding = characterEncoding;
		this.characterEncodingSet = true;
	}

	private void updateContentTypePropertyAndHeader() {
		if (this.contentType != null) {
			String value = this.contentType;
			if (this.characterEncodingSet && !value.toLowerCase().contains(CHARSET_PREFIX)) {
				value += ';' + CHARSET_PREFIX + getCharacterEncoding();
				this.contentType = value;
			}
			doAddHeaderValue(HttpHeaders.CONTENT_TYPE, value, true);
		}
	}

	@Override
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public ServletOutputStream getOutputStream() {
		Assert.state(this.outputStreamAccessAllowed, "OutputStream access not allowed");
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws UnsupportedEncodingException {
		Assert.state(this.writerAccessAllowed, "Writer access not allowed");
		if (this.writer == null) {
			Writer targetWriter = new OutputStreamWriter(this.content, getCharacterEncoding());
			this.writer = new ResponsePrintWriter(targetWriter);
		}
		return this.writer;
	}

	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	/**
	 * 以 {@code String} 形式获取响应正文内容，使用应用程序为响应
	 * 指定的字符集（通过
	 * {@link HttpServletResponse} 方法或 {@code Content-Type} 上的
	 * charset 参数）。如果未显式定义字符集，将使用
	 * {@linkplain #setDefaultCharacterEncoding(String) 默认字符编码}。
	 * @return 内容，类型为 {@code String}
	 * @throws UnsupportedEncodingException 如果不支持该字符编码
	 * @see #getContentAsString(Charset)
	 * @see #setCharacterEncoding(String)
	 * @see #setContentType(String)
	 */
	public String getContentAsString() throws UnsupportedEncodingException {
		return this.content.toString(getCharacterEncoding());
	}

	/**
	 * 以 {@code String} 形式获取响应正文内容；如果未显式定义字符集，
	 * 则使用提供的 {@code fallbackCharset}，否则
	 * 使用应用程序为响应指定的字符集（通过
	 * {@link HttpServletResponse} 方法或 {@code Content-Type} 上的 charset 参数）。
	 * @return 内容，类型为 {@code String}
	 * @throws UnsupportedEncodingException 如果不支持该字符编码
	 * @since 5.2
	 * @see #getContentAsString()
	 * @see #setCharacterEncoding(String)
	 * @see #setContentType(String)
	 */
	public String getContentAsString(Charset fallbackCharset) throws UnsupportedEncodingException {
		String charsetName = (this.characterEncodingSet ? getCharacterEncoding() : fallbackCharset.name());
		return this.content.toString(charsetName);
	}

	@Override
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
		doAddHeaderValue(HttpHeaders.CONTENT_LENGTH, contentLength, true);
	}

	public int getContentLength() {
		return (int) this.contentLength;
	}

	@Override
	public void setContentLengthLong(long contentLength) {
		this.contentLength = contentLength;
		doAddHeaderValue(HttpHeaders.CONTENT_LENGTH, contentLength, true);
	}

	public long getContentLengthLong() {
		return this.contentLength;
	}

	@Override
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				if (mediaType.getCharset() != null) {
					setExplicitCharacterEncoding(mediaType.getCharset().name());
				}
			}
			catch (Exception ex) {
				// Try to get charset value anyway
				int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
				if (charsetIndex != -1) {
					setExplicitCharacterEncoding(contentType.substring(charsetIndex + CHARSET_PREFIX.length()));
				}
			}
			updateContentTypePropertyAndHeader();
		}
	}

	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public int getBufferSize() {
		return this.bufferSize;
	}

	@Override
	public void flushBuffer() {
		setCommitted(true);
	}

	@Override
	public void resetBuffer() {
		Assert.state(!isCommitted(), "Cannot reset buffer - response is already committed");
		this.content.reset();
	}

	private void setCommittedIfBufferSizeExceeded() {
		int bufSize = getBufferSize();
		if (bufSize > 0 && this.content.size() > bufSize) {
			setCommitted(true);
		}
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	@Override
	public boolean isCommitted() {
		return this.committed;
	}

	@Override
	public void reset() {
		resetBuffer();
		this.characterEncoding = this.defaultCharacterEncoding;
		this.characterEncodingSet = false;
		this.contentLength = 0;
		this.contentType = null;
		this.locale = Locale.getDefault();
		this.cookies.clear();
		this.headers.clear();
		this.status = HttpServletResponse.SC_OK;
		this.errorMessage = null;
	}

	@Override
	public void setLocale(@Nullable Locale locale) {
		// Although the Javadoc for javax.servlet.ServletResponse.setLocale(Locale) does not
		// state how a null value for the supplied Locale should be handled, both Tomcat and
		// Jetty simply ignore a null value. So we do the same here.
		if (locale == null) {
			return;
		}
		this.locale = locale;
		doAddHeaderValue(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag(), true);
	}

	@Override
	public Locale getLocale() {
		return this.locale;
	}


	//---------------------------------------------------------------------
	// HttpServletResponse interface
	//---------------------------------------------------------------------

	@Override
	public void addCookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie);
		doAddHeaderValue(HttpHeaders.SET_COOKIE, getCookieHeader(cookie), false);
	}

	private String getCookieHeader(Cookie cookie) {
		StringBuilder buf = new StringBuilder();
		buf.append(cookie.getName()).append('=').append(cookie.getValue() == null ? "" : cookie.getValue());
		if (StringUtils.hasText(cookie.getPath())) {
			buf.append("; Path=").append(cookie.getPath());
		}
		if (StringUtils.hasText(cookie.getDomain())) {
			buf.append("; Domain=").append(cookie.getDomain());
		}
		int maxAge = cookie.getMaxAge();
		ZonedDateTime expires = (cookie instanceof MockCookie ? ((MockCookie) cookie).getExpires() : null);
		if (maxAge >= 0) {
			buf.append("; Max-Age=").append(maxAge);
			buf.append("; Expires=");
			if (expires != null) {
				buf.append(expires.format(DateTimeFormatter.RFC_1123_DATE_TIME));
			}
			else {
				HttpHeaders headers = new HttpHeaders();
				headers.setExpires(maxAge > 0 ? System.currentTimeMillis() + 1000L * maxAge : 0);
				buf.append(headers.getFirst(HttpHeaders.EXPIRES));
			}
		}
		else if (expires != null) {
			buf.append("; Expires=");
			buf.append(expires.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		}

		if (cookie.getSecure()) {
			buf.append("; Secure");
		}
		if (cookie.isHttpOnly()) {
			buf.append("; HttpOnly");
		}
		if (cookie instanceof MockCookie) {
			MockCookie mockCookie = (MockCookie) cookie;
			if (StringUtils.hasText(mockCookie.getSameSite())) {
				buf.append("; SameSite=").append(mockCookie.getSameSite());
			}
		}
		if (StringUtils.hasText(cookie.getComment())) {
			buf.append("; Comment=").append(cookie.getComment());
		}
		return buf.toString();
	}

	public Cookie[] getCookies() {
		return this.cookies.toArray(new Cookie[0]);
	}

	@Nullable
	public Cookie getCookie(String name) {
		Assert.notNull(name, "Cookie name must not be null");
		for (Cookie cookie : this.cookies) {
			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	@Override
	public boolean containsHeader(String name) {
		return this.headers.containsKey(name);
	}

	/**
	 * 以字符串 Set 形式返回所有已指定头的名称。
	 * <p>从 Servlet 3.0 开始，此方法也在 {@link HttpServletResponse} 中定义。
	 * @return 头名称 {@code String} 的 {@code Set}，
	 * 如果没有则为空 {@code Set}
	 */
	@Override
	public Collection<String> getHeaderNames() {
		return this.headers.keySet();
	}

	/**
	 * 以 String 形式返回给定头的主值（如果有）。
	 * 若有多个值，将返回第一个值。
	 * <p>从 Servlet 3.0 开始，此方法也在 {@link HttpServletResponse} 中定义。
	 * 从 Spring 3.1 开始，为兼容 Servlet 3.0，它返回字符串化后的值。
	 * 如需原始 Object 访问，请考虑使用 {@link #getHeaderValue(String)}。
	 * @param name 头的名称
	 * @return 关联的头值，如果没有则为 {@code null}
	 */
	@Override
	@Nullable
	public String getHeader(String name) {
		HeaderValueHolder header = this.headers.get(name);
		return (header != null ? header.getStringValue() : null);
	}

	/**
	 * 以字符串 List 形式返回给定头的所有值。
	 * <p>从 Servlet 3.0 开始，此方法也在 {@link HttpServletResponse} 中定义。
	 * 从 Spring 3.1 开始，为兼容 Servlet 3.0，它返回字符串化值的 List。
	 * 如需原始 Object 访问，请考虑使用 {@link #getHeaderValues(String)}。
	 * @param name 头的名称
	 * @return 关联的头值，如果没有则为空 List
	 */
	@Override
	public List<String> getHeaders(String name) {
		HeaderValueHolder header = this.headers.get(name);
		if (header != null) {
			return header.getStringValues();
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * 返回给定头的主值（如果有）。
	 * <p>若有多个值，将返回第一个值。
	 * @param name 头的名称
	 * @return 关联的头值，如果没有则为 {@code null}
	 */
	@Nullable
	public Object getHeaderValue(String name) {
		HeaderValueHolder header = this.headers.get(name);
		return (header != null ? header.getValue() : null);
	}

	/**
	 * 以值对象 List 形式返回给定头的所有值。
	 * @param name 头的名称
	 * @return 关联的头值，如果没有则为空 List
	 */
	public List<Object> getHeaderValues(String name) {
		HeaderValueHolder header = this.headers.get(name);
		if (header != null) {
			return header.getValues();
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * 默认实现按原样返回给定的 URL 字符串。
	 * <p>可以在子类中覆写，以附加会话 id 等。
	 */
	@Override
	public String encodeURL(String url) {
		return url;
	}

	/**
	 * 默认实现委托给 {@link #encodeURL}，
	 * 按原样返回给定的 URL 字符串。
	 * <p>可以在子类中覆写，以重定向特有的方式附加会话 id 等。
	 * 对于通用 URL 编码规则，请改为覆写公共的 {@link #encodeURL} 方法，
	 * 该方法同时适用于重定向 URL 和一般 URL。
	 */
	@Override
	public String encodeRedirectURL(String url) {
		return encodeURL(url);
	}

	@Override
	@Deprecated
	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	@Override
	@Deprecated
	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	@Override
	public void sendError(int status, String errorMessage) throws IOException {
		Assert.state(!isCommitted(), "Cannot set error status - response is already committed");
		this.status = status;
		this.errorMessage = errorMessage;
		setCommitted(true);
	}

	@Override
	public void sendError(int status) throws IOException {
		Assert.state(!isCommitted(), "Cannot set error status - response is already committed");
		this.status = status;
		setCommitted(true);
	}

	@Override
	public void sendRedirect(String url) throws IOException {
		Assert.state(!isCommitted(), "Cannot send redirect - response is already committed");
		Assert.notNull(url, "Redirect URL must not be null");
		setHeader(HttpHeaders.LOCATION, url);
		setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
		setCommitted(true);
	}

	@Nullable
	public String getRedirectedUrl() {
		return getHeader(HttpHeaders.LOCATION);
	}

	@Override
	public void setDateHeader(String name, long value) {
		setHeaderValue(name, formatDate(value));
	}

	@Override
	public void addDateHeader(String name, long value) {
		addHeaderValue(name, formatDate(value));
	}

	public long getDateHeader(String name) {
		String headerValue = getHeader(name);
		if (headerValue == null) {
			return -1;
		}
		try {
			return newDateFormat().parse(getHeader(name)).getTime();
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException(
					"Value for header '" + name + "' is not a valid Date: " + headerValue);
		}
	}

	private String formatDate(long date) {
		return newDateFormat().format(new Date(date));
	}

	private DateFormat newDateFormat() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
		dateFormat.setTimeZone(GMT);
		return dateFormat;
	}

	@Override
	public void setHeader(String name, @Nullable String value) {
		setHeaderValue(name, value);
	}

	@Override
	public void addHeader(String name, @Nullable String value) {
		addHeaderValue(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeaderValue(name, value);
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeaderValue(name, value);
	}

	private void setHeaderValue(String name, @Nullable Object value) {
		if (value == null) {
			return;
		}
		boolean replaceHeader = true;
		if (setSpecialHeader(name, value, replaceHeader)) {
			return;
		}
		doAddHeaderValue(name, value, replaceHeader);
	}

	private void addHeaderValue(String name, @Nullable Object value) {
		if (value == null) {
			return;
		}
		boolean replaceHeader = false;
		if (setSpecialHeader(name, value, replaceHeader)) {
			return;
		}
		doAddHeaderValue(name, value, replaceHeader);
	}

	private boolean setSpecialHeader(String name, Object value, boolean replaceHeader) {
		if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
			setContentType(value.toString());
			return true;
		}
		else if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
			setContentLength(value instanceof Number ? ((Number) value).intValue() :
					Integer.parseInt(value.toString()));
			return true;
		}
		else if (HttpHeaders.CONTENT_LANGUAGE.equalsIgnoreCase(name)) {
			String contentLanguages = value.toString();
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_LANGUAGE, contentLanguages);
			Locale language = headers.getContentLanguage();
			setLocale(language != null ? language : Locale.getDefault());
			// Since setLocale() sets the Content-Language header to the given
			// single Locale, we have to explicitly set the Content-Language header
			// to the user-provided value.
			doAddHeaderValue(HttpHeaders.CONTENT_LANGUAGE, contentLanguages, true);
			return true;
		}
		else if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(name)) {
			MockCookie cookie = MockCookie.parse(value.toString());
			if (replaceHeader) {
				setCookie(cookie);
			}
			else {
				addCookie(cookie);
			}
			return true;
		}
		else {
			return false;
		}
	}

	private void doAddHeaderValue(String name, Object value, boolean replace) {
		Assert.notNull(value, "Header value must not be null");
		HeaderValueHolder header = this.headers.computeIfAbsent(name, key -> new HeaderValueHolder());
		if (replace) {
			header.setValue(value);
		}
		else {
			header.addValue(value);
		}
	}

	/**
	 * 将 {@code Set-Cookie} 头设置为提供的 {@link Cookie}，
	 * 覆盖之前的任何 Cookie。
	 * @param cookie 要设置的 {@code Cookie}
	 * @since 5.1.10
	 * @see #addCookie(Cookie)
	 */
	private void setCookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.clear();
		this.cookies.add(cookie);
		doAddHeaderValue(HttpHeaders.SET_COOKIE, getCookieHeader(cookie), true);
	}

	@Override
	public void setStatus(int status) {
		if (!this.isCommitted()) {
			this.status = status;
		}
	}

	@Override
	@Deprecated
	public void setStatus(int status, String errorMessage) {
		if (!this.isCommitted()) {
			this.status = status;
			this.errorMessage = errorMessage;
		}
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Nullable
	public String getErrorMessage() {
		return this.errorMessage;
	}


	//---------------------------------------------------------------------
	// Methods for MockRequestDispatcher
	//---------------------------------------------------------------------

	public void setForwardedUrl(@Nullable String forwardedUrl) {
		this.forwardedUrl = forwardedUrl;
	}

	@Nullable
	public String getForwardedUrl() {
		return this.forwardedUrl;
	}

	public void setIncludedUrl(@Nullable String includedUrl) {
		this.includedUrls.clear();
		if (includedUrl != null) {
			this.includedUrls.add(includedUrl);
		}
	}

	@Nullable
	public String getIncludedUrl() {
		int count = this.includedUrls.size();
		Assert.state(count <= 1,
				() -> "More than 1 URL included - check getIncludedUrls instead: " + this.includedUrls);
		return (count == 1 ? this.includedUrls.get(0) : null);
	}

	public void addIncludedUrl(String includedUrl) {
		Assert.notNull(includedUrl, "Included URL must not be null");
		this.includedUrls.add(includedUrl);
	}

	public List<String> getIncludedUrls() {
		return this.includedUrls;
	}


	/**
	 * 内部类，适配 ServletOutputStream，在超出缓冲区
	 * 大小时将响应标记为已提交。
	 */
	private class ResponseServletOutputStream extends DelegatingServletOutputStream {

		public ResponseServletOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			super.write(b);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void flush() throws IOException {
			super.flush();
			setCommitted(true);
		}
	}


	/**
	 * 内部类，适配 PrintWriter，在超出缓冲区
	 * 大小时将响应标记为已提交。
	 */
	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(Writer out) {
			super(out, true);
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
			setCommittedIfBufferSizeExceeded();
		}

		@Override
		public void flush() {
			super.flush();
			setCommitted(true);
		}

		@Override
		public void close() {
			super.flush();
			super.close();
			setCommitted(true);
		}
	}

}
