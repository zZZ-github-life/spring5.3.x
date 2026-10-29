/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Servlet 过滤器，允许为请求指定字符编码。这很有用，因为当前浏览器通常
 * 不会设置字符编码，即使已在 HTML 页面或表单中指定。
 *
 * <p>此过滤器既可以在请求尚未指定编码时应用其编码，也可以在任何情况下
 * 强制使用此过滤器的编码（"forceEncoding"="true"）。在后一种情况下，
 * 该编码还会作为默认响应编码应用（不过这通常会被视图中设置的完整内容类型覆盖）。
 *
 * @author Juergen Hoeller
 * @since 15.03.2004
 * @see #setEncoding
 * @see #setForceEncoding
 * @see javax.servlet.http.HttpServletRequest#setCharacterEncoding
 * @see javax.servlet.http.HttpServletResponse#setCharacterEncoding
 */
public class CharacterEncodingFilter extends OncePerRequestFilter {

	@Nullable
	private String encoding;

	private boolean forceRequestEncoding = false;

	private boolean forceResponseEncoding = false;


	/**
	 * 创建一个默认的 {@code CharacterEncodingFilter}，
	 * 其编码通过 {@link #setEncoding} 设置。
	 * @see #setEncoding
	 */
	public CharacterEncodingFilter() {
	}

	/**
	 * 为给定的编码创建一个 {@code CharacterEncodingFilter}。
	 * @param encoding the encoding to apply
	 * @since 4.2.3
	 * @see #setEncoding
	 */
	public CharacterEncodingFilter(String encoding) {
		this(encoding, false);
	}

	/**
	 * 为给定的编码创建一个 {@code CharacterEncodingFilter}。
	 * @param encoding the encoding to apply
	 * @param forceEncoding whether the specified encoding is supposed to
	 * override existing request and response encodings
	 * @since 4.2.3
	 * @see #setEncoding
	 * @see #setForceEncoding
	 */
	public CharacterEncodingFilter(String encoding, boolean forceEncoding) {
		this(encoding, forceEncoding, forceEncoding);
	}

	/**
	 * 为给定的编码创建一个 {@code CharacterEncodingFilter}。
	 * @param encoding the encoding to apply
	 * @param forceRequestEncoding whether the specified encoding is supposed to
	 * override existing request encodings
	 * @param forceResponseEncoding whether the specified encoding is supposed to
	 * override existing response encodings
	 * @since 4.3
	 * @see #setEncoding
	 * @see #setForceRequestEncoding(boolean)
	 * @see #setForceResponseEncoding(boolean)
	 */
	public CharacterEncodingFilter(String encoding, boolean forceRequestEncoding, boolean forceResponseEncoding) {
		Assert.hasLength(encoding, "Encoding must not be empty");
		this.encoding = encoding;
		this.forceRequestEncoding = forceRequestEncoding;
		this.forceResponseEncoding = forceResponseEncoding;
	}


	/**
	 * 设置用于请求的编码。该编码将传入
	 * {@link javax.servlet.http.HttpServletRequest#setCharacterEncoding} 调用。
	 * <p>该编码是否会覆盖已有的请求编码（以及是否也会作为默认响应编码应用）
	 * 取决于 {@link #setForceEncoding "forceEncoding"} 标志。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 返回为请求和/或响应配置的编码。
	 * @since 4.3
	 */
	@Nullable
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置此过滤器配置的 {@link #setEncoding encoding} 是否应覆盖已有的
	 * 请求编码和响应编码。
	 * <p>默认值为 "false"，即如果
	 * {@link javax.servlet.http.HttpServletRequest#getCharacterEncoding()}
	 * 返回非 null 值，则不修改编码。将其设置为 "true" 可在任何情况下强制使用
	 * 指定的编码，并同时将其作为默认响应编码应用。
	 * <p>这等同于同时设置 {@link #setForceRequestEncoding(boolean)} 和
	 * {@link #setForceResponseEncoding(boolean)}。
	 * @see #setForceRequestEncoding(boolean)
	 * @see #setForceResponseEncoding(boolean)
	 */
	public void setForceEncoding(boolean forceEncoding) {
		this.forceRequestEncoding = forceEncoding;
		this.forceResponseEncoding = forceEncoding;
	}

	/**
	 * 设置此过滤器配置的 {@link #setEncoding encoding} 是否应覆盖已有的请求编码。
	 * <p>默认值为 "false"，即如果
	 * {@link javax.servlet.http.HttpServletRequest#getCharacterEncoding()}
	 * 返回非 null 值，则不修改编码。将其设置为 "true" 可在任何情况下强制使用
	 * 指定的编码。
	 * @since 4.3
	 */
	public void setForceRequestEncoding(boolean forceRequestEncoding) {
		this.forceRequestEncoding = forceRequestEncoding;
	}

	/**
	 * 返回是否应对请求强制使用该编码。
	 * @since 4.3
	 */
	public boolean isForceRequestEncoding() {
		return this.forceRequestEncoding;
	}

	/**
	 * 设置此过滤器配置的 {@link #setEncoding encoding} 是否应覆盖已有的响应编码。
	 * <p>默认值为 "false"，即不修改编码。
	 * 将其设置为 "true" 可在任何情况下强制对响应使用指定的编码。
	 * @since 4.3
	 */
	public void setForceResponseEncoding(boolean forceResponseEncoding) {
		this.forceResponseEncoding = forceResponseEncoding;
	}

	/**
	 * 返回是否应对响应强制使用该编码。
	 * @since 4.3
	 */
	public boolean isForceResponseEncoding() {
		return this.forceResponseEncoding;
	}


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String encoding = getEncoding();
		if (encoding != null) {
			if (isForceRequestEncoding() || request.getCharacterEncoding() == null) {
				request.setCharacterEncoding(encoding);
			}
			if (isForceResponseEncoding()) {
				response.setCharacterEncoding(encoding);
			}
		}
		filterChain.doFilter(request, response);
	}

}
