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

package org.springframework.web.bind;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;
import org.springframework.web.util.WebUtils;

/**
 * 专用的 {@link org.springframework.validation.DataBinder}，用于从 Servlet 请求参数
 * 向 JavaBean 进行数据绑定，包括对 multipart 文件的支持。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露不应被外部客户端访问或修改的
 * 对象图部分，从而带来安全问题。因此，应仔细考虑数据绑定的设计与使用对安全性的影响。
 * 更多详情请参阅参考手册中关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和 <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * <p>有关定制选项（包括指定允许/必需的字段以及注册自定义属性编辑器），
 * 请参阅 DataBinder/WebDataBinder 父类。
 *
 * <p>也可用于自定义 Web 控制器中的手动数据绑定：例如，在普通的 Controller 实现中
 * 或 MultiActionController 处理器方法中。只需为每次绑定过程实例化一个
 * ServletRequestDataBinder，并以当前 ServletRequest 作为参数调用 {@code bind}：
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * ServletRequestDataBinder binder = new ServletRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #bind(javax.servlet.ServletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class ServletRequestDataBinder extends WebDataBinder {

	/**
	 * 创建一个新的 ServletRequestDataBinder 实例，使用默认对象名称。
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ServletRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的 ServletRequestDataBinder 实例。
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public ServletRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标对象，对于 multipart 请求
	 * 还会绑定 multipart 文件。
	 * <p>此调用可能创建字段错误，表示基本的绑定错误，例如必需字段缺失
	 * （代码为 "required"），或值与 Bean 属性类型不匹配（代码为 "typeMismatch"）。
	 * <p>multipart 文件通过其参数名进行绑定，就像普通的 HTTP 参数一样：
	 * 即 "uploadedFile" 绑定到 "uploadedFile" Bean 属性，调用 "setUploadedFile"
	 * setter 方法。
	 * <p>multipart 文件目标属性的类型可以是 MultipartFile、byte[] 或 String。
	 * 当请求尚未通过 MultipartResolver 解析为 MultipartRequest 时，
	 * 也支持 Servlet Part 绑定。
	 * @param request the request with parameters to bind (can be multipart)
	 * @see org.springframework.web.multipart.MultipartHttpServletRequest
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see jakarta.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(ServletRequest request) {
		MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
		MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
		if (multipartRequest != null) {
			bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
		}
		else if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.MULTIPART_FORM_DATA_VALUE)) {
			HttpServletRequest httpServletRequest = WebUtils.getNativeRequest(request, HttpServletRequest.class);
			if (httpServletRequest != null && HttpMethod.POST.matches(httpServletRequest.getMethod())) {
				StandardServletPartUtils.bindParts(httpServletRequest, mpvs, isBindEmptyMultipartFiles());
			}
		}
		addBindValues(mpvs, request);
		doBind(mpvs);
	}

	/**
	 * 扩展点，子类可用它为请求添加额外的绑定值。在
	 * {@link #doBind(MutablePropertyValues)} 之前调用。默认实现为空。
	 * @param mpvs the property values that will be used for data binding
	 * @param request the current request
	 */
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
	}

	/**
	 * 将错误视为致命错误。
	 * <p>仅在输入无效属于错误的情况下使用此方法。例如，如果所有输入都来自下拉列表，
	 * 这可能适用。
	 * @throws ServletRequestBindingException subclass of ServletException on any binding problem
	 */
	public void closeNoCatch() throws ServletRequestBindingException {
		if (getBindingResult().hasErrors()) {
			throw new ServletRequestBindingException(
					"Errors binding onto object '" + getBindingResult().getObjectName() + "'",
					new BindException(getBindingResult()));
		}
	}

}
