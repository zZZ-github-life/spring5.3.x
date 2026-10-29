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

package org.springframework.web.multipart.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * {@link MultipartResolver} 接口的标准实现，
 * 基于 Servlet 3.0 的 {@link javax.servlet.http.Part} API。
 * 可作为 "multipartResolver" Bean 添加到 Spring DispatcherServlet 上下文中，
 * 而无需在 Bean 层面进行任何额外配置（见下文）。
 *
 * <p>此解析器变体按原样使用 Servlet 容器的 multipart 解析器，
 * 这可能会使应用暴露于容器实现差异之下。
 * 请参阅 {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}，
 * 了解另一种在应用内部使用本地 Commons FileUpload 库的实现，
 * 它可在各类 Servlet 容器之间提供最大的可移植性。
 * 另请参阅此解析器的 {@link #setStrictServletCompliance 严格遵循 Servlet 规范}
 * 配置选项，它会把 Spring 的 {@link MultipartHttpServletRequest} 的适用范围
 * 收窄到仅限表单数据。
 *
 * <p><b>注意：</b>要使用基于 Servlet 3.0 的 multipart 解析，
 * 需要在 {@code web.xml} 中为受影响的 Servlet 标记 "multipart-config" 配置节，
 * 或者在以编程方式注册 Servlet 时提供 {@link javax.servlet.MultipartConfigElement}，
 * 或者（对于自定义 Servlet 类）在 Servlet 类上使用
 * {@link javax.servlet.annotation.MultipartConfig} 注解。
 * 诸如最大大小或存储位置之类的配置设置需要在 Servlet 注册层面应用；
 * Servlet 3.0 不允许在 MultipartResolver 层面设置这些配置。
 *
 * <pre class="code">
 * public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
 *	 // ...
 *	 &#064;Override
 *	 protected void customizeRegistration(ServletRegistration.Dynamic registration) {
 *     // Optionally also set maxFileSize, maxRequestSize, fileSizeThreshold
 *     registration.setMultipartConfig(new MultipartConfigElement("/tmp"));
 *   }
 * }
 * </pre>
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setResolveLazily
 * @see #setStrictServletCompliance
 * @see HttpServletRequest#getParts()
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 */
public class StandardServletMultipartResolver implements MultipartResolver {

	private boolean resolveLazily = false;

	private boolean strictServletCompliance = false;


	/**
	 * 设置是否在访问文件或参数时才延迟解析 multipart 请求。
	 * <p>默认值为 "false"，即立即解析 multipart 元素，在调用 {@link #resolveMultipart}
	 * 时抛出相应的异常。将其设置为 "true" 可启用延迟 multipart 解析，
	 * 在应用尝试获取 multipart 文件或参数时才抛出解析异常。
	 * @since 3.2.9
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * 指定此解析器是否应严格遵循 Servlet 规范，仅对 "multipart/form-data"
	 * 请求生效。
	 * <p>默认值为 "false"，即尝试处理任何 "multipart/" 内容类型的请求
	 * （只要底层 Servlet 容器支持，这在 Tomcat 上可行，但在 Jetty 上不可行）。
	 * 为了保持一致的跨容器可移植性，特别是为了在 Spring 的 {@link MultipartResolver}
	 * 机制之外一致地自定义处理非表单的 multipart 请求类型，
	 * 可将此标志设置为 "true"：此时只有 "multipart/form-data" 请求会被
	 * {@link MultipartHttpServletRequest} 包装；其他类型的请求将保持原样，
	 * 以便在用户代码中进行自定义处理。
	 * <p>请注意，Commons FileUpload 因而
	 * {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
	 * 支持任何 "multipart/" 请求类型。不过，它将处理限制为 POST 请求，
	 * 而标准 Servlet multipart 解析器可能不会这样做。
	 * @since 5.3.9
	 */
	public void setStrictServletCompliance(boolean strictServletCompliance) {
		this.strictServletCompliance = strictServletCompliance;
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return StringUtils.startsWithIgnoreCase(request.getContentType(),
				(this.strictServletCompliance ? MediaType.MULTIPART_FORM_DATA_VALUE : "multipart/"));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			// To be on the safe side: explicitly delete the parts,
			// but only actual file parts (for Resin compatibility)
			try {
				for (Part part : request.getParts()) {
					if (request.getFile(part.getName()) != null) {
						part.delete();
					}
				}
			}
			catch (Throwable ex) {
				LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
			}
		}
	}

}
