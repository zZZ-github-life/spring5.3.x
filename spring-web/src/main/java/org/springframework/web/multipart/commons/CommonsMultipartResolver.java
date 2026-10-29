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

package org.springframework.web.multipart.commons;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * 基于 Servlet 的 {@link MultipartResolver} 实现，底层使用
 * <a href="https://commons.apache.org/proper/commons-fileupload">Apache Commons FileUpload</a>
 * 1.2 或更高版本。此解析器变体委托给应用本地的 FileUpload 库，
 * 从而在各类 Servlet 容器之间提供最大的可移植性。
 *
 * <p>Commons FileUpload 传统上会解析任何 "multipart/" 类型的 POST 请求。
 * 可以通过 {@link #setSupportedMethods} 自定义受支持的 HTTP 方法。
 *
 * <p>以 Bean 属性的形式提供 "maxUploadSize"、"maxInMemorySize" 和 "defaultEncoding"
 * 设置（继承自 {@link CommonsFileUploadSupport}）。有关默认值和可接受值的详细信息，
 * 请参阅对应的 ServletFileUpload / DiskFileItemFactory 属性
 * （"sizeMax"、"sizeThreshold"、"headerEncoding"）。
 *
 * <p>将临时文件保存到 Servlet 容器的临时目录。
 * 需要通过应用上下文<i>或</i>通过接受 ServletContext 的构造函数
 * （用于独立使用）进行初始化。
 *
 * <p>注意：常见的替代方案是
 * {@link org.springframework.web.multipart.support.StandardServletMultipartResolver}，
 * 它委托给 Servlet 容器自身的 multipart 解析器，配置在容器层面进行，
 * 并可能存在容器特定的限制。
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @since 29.09.2003
 * @see #CommonsMultipartResolver(ServletContext)
 * @see #setResolveLazily
 * @see #setSupportedMethods
 * @see org.apache.commons.fileupload.servlet.ServletFileUpload
 * @see org.apache.commons.fileupload.disk.DiskFileItemFactory
 * @see org.springframework.web.multipart.support.StandardServletMultipartResolver
 */
public class CommonsMultipartResolver extends CommonsFileUploadSupport
		implements MultipartResolver, ServletContextAware {

	private boolean resolveLazily = false;

	@Nullable
	private Set<String> supportedMethods;


	/**
	 * 供作为 Bean 使用时调用的构造函数。通过 ServletContextAware 接口
	 * 传入的 ServletContext（通常由 WebApplicationContext 提供）
	 * 确定 Servlet 容器的临时目录。
	 * @see #setServletContext
	 * @see org.springframework.web.context.ServletContextAware
	 * @see org.springframework.web.context.WebApplicationContext
	 */
	public CommonsMultipartResolver() {
		super();
	}

	/**
	 * 供独立使用时调用的构造函数。通过给定的 ServletContext
	 * 确定 Servlet 容器的临时目录。
	 * @param servletContext the ServletContext to use
	 */
	public CommonsMultipartResolver(ServletContext servletContext) {
		this();
		setServletContext(servletContext);
	}


	/**
	 * 设置是否在访问文件或参数时才延迟解析 multipart 请求。
	 * <p>默认值为 "false"，即立即解析 multipart 元素，在调用 {@link #resolveMultipart}
	 * 时抛出相应的异常。将其设置为 "true" 可启用延迟 multipart 解析，
	 * 在应用尝试获取 multipart 文件或参数时才抛出解析异常。
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * 以 HTTP 方法名称数组的形式指定受支持的方法。
	 * Commons FileUpload 的传统默认值仅为 "POST"。
	 * <p>当作为 Spring 属性值配置时，可以使用逗号分隔的字符串，例如 "POST,PUT"。
	 * @since 5.3.9
	 */
	public void setSupportedMethods(String... supportedMethods) {
		this.supportedMethods = new HashSet<>(Arrays.asList(supportedMethods));
	}

	/**
	 * 初始化底层的 {@code org.apache.commons.fileupload.servlet.ServletFileUpload}
	 * 实例。可以重写以使用自定义子类，例如用于测试目的。
	 * @param fileItemFactory the Commons FileItemFactory to use
	 * @return the new ServletFileUpload instance
	 */
	@Override
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new ServletFileUpload(fileItemFactory);
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (!isUploadTempDirSpecified()) {
			getFileItemFactory().setRepository(WebUtils.getTempDir(servletContext));
		}
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return (this.supportedMethods != null ?
				this.supportedMethods.contains(request.getMethod()) &&
						FileUploadBase.isMultipartContent(new ServletRequestContext(request)) :
				ServletFileUpload.isMultipartContent(request));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
		Assert.notNull(request, "Request must not be null");
		if (this.resolveLazily) {
			return new DefaultMultipartHttpServletRequest(request) {
				@Override
				protected void initializeMultipart() {
					MultipartParsingResult parsingResult = parseRequest(request);
					setMultipartFiles(parsingResult.getMultipartFiles());
					setMultipartParameters(parsingResult.getMultipartParameters());
					setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());
				}
			};
		}
		else {
			MultipartParsingResult parsingResult = parseRequest(request);
			return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),
					parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
		}
	}

	/**
	 * 解析给定的 Servlet 请求，解析其中的 multipart 元素。
	 * @param request the request to parse
	 * @return the parsing result
	 * @throws MultipartException if multipart resolution failed.
	 */
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		String encoding = determineEncoding(request);
		FileUpload fileUpload = prepareFileUpload(encoding);
		try {
			List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
			return parseFileItems(fileItems, encoding);
		}
		catch (FileUploadBase.SizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
		}
		catch (FileUploadBase.FileSizeLimitExceededException ex) {
			throw new MaxUploadSizeExceededException(fileUpload.getFileSizeMax(), ex);
		}
		catch (FileUploadException ex) {
			throw new MultipartException("Failed to parse multipart servlet request", ex);
		}
	}

	/**
	 * 确定给定请求的编码。可以在子类中重写。
	 * <p>默认实现检查请求的编码，
	 * 若不存在则回退到为此解析器指定的默认编码。
	 * @param request current HTTP request
	 * @return the encoding for the request (never {@code null})
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see #setDefaultEncoding
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String encoding = request.getCharacterEncoding();
		if (encoding == null) {
			encoding = getDefaultEncoding();
		}
		return encoding;
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			try {
				cleanupFileItems(request.getMultiFileMap());
			}
			catch (Throwable ex) {
				logger.warn("Failed to perform multipart cleanup for servlet request", ex);
			}
		}
	}

}
