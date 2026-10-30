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

package org.springframework.web.bind.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;

/**
 * 特殊的 {@link org.springframework.validation.DataBinder}，用于将 Web 请求参数
 * 绑定到 JavaBean，包括对多部分文件的支持。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露不应被外部客户端访问或修改的
 * 对象图部分，从而带来安全问题。因此，数据绑定的设计和使用应仔细考虑安全性。
 * 更多详情请参阅参考手册中关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * <p>有关自定义选项（包括指定允许/必需字段以及注册自定义属性编辑器），
 * 请参见 DataBinder/WebDataBinder 父类。
 *
 * <p>也可用于构建在 Spring 的
 * {@link org.springframework.web.context.request.WebRequest} 抽象之上的自定义
 * Web 控制器或拦截器中的手动数据绑定：例如在
 * {@link org.springframework.web.context.request.WebRequestInterceptor} 实现中。
 * 只需为每个绑定过程实例化一个 WebRequestDataBinder，并以当前 WebRequest
 * 作为参数调用 {@code bind}：
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * WebRequestDataBinder binder = new WebRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 2.5.2
 * @see #bind(org.springframework.web.context.request.WebRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class WebRequestDataBinder extends WebDataBinder {

	/**
	 * 创建一个新的 WebRequestDataBinder 实例，使用默认对象名称。
	 * @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的 WebRequestDataBinder 实例。
	 * @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @param objectName 目标对象的名称
	 */
	public WebRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标对象，如果是多部分请求，
	 * 还会绑定多部分文件。
	 * <p>此调用可能会创建字段错误，表示基本绑定错误，例如必填字段
	 * （代码 "required"）或值与 bean 属性之间的类型不匹配（代码 "typeMismatch"）。
	 * <p>多部分文件通过其参数名称进行绑定，就像普通 HTTP 参数一样：
	 * 即将 "uploadedFile" 绑定到 "uploadedFile" bean 属性，
	 * 调用 "setUploadedFile" setter 方法。
	 * <p>多部分文件的目标属性类型可以是 MultipartFile、byte[] 或 String。
	 * 当请求尚未通过 MultipartResolver 解析为 MultipartRequest 时，
	 * 也支持 Servlet Part 绑定。
	 * @param request 带有要绑定参数的请求（可能为 multipart）
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see javax.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(WebRequest request) {
		MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());
		if (request instanceof NativeWebRequest) {
			NativeWebRequest nativeRequest = (NativeWebRequest) request;
			MultipartRequest multipartRequest = nativeRequest.getNativeRequest(MultipartRequest.class);
			if (multipartRequest != null) {
				bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
			}
			else if (StringUtils.startsWithIgnoreCase(
					request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
				HttpServletRequest servletRequest = nativeRequest.getNativeRequest(HttpServletRequest.class);
				if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
					StandardServletPartUtils.bindParts(servletRequest, mpvs, isBindEmptyMultipartFiles());
				}
			}
		}
		doBind(mpvs);
	}

	/**
	 * 将错误视为致命错误。
	 * <p>仅在输入无效即构成错误时使用此方法。
	 * 例如，如果所有输入都来自下拉列表，这可能适用。
	 * @throws BindException 如果遇到绑定错误
	 */
	public void closeNoCatch() throws BindException {
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
	}

}
