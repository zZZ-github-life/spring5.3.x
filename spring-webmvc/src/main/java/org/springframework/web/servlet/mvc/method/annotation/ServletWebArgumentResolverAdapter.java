/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter;

/**
 * Servlet 专用的
 * {@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter}，
 * 它从 {@link ServletRequestAttributes} 创建 {@link NativeWebRequest}。
 *
 * <p><strong>注意：</strong>提供此类是为了向后兼容。
 * 不过，建议将 {@code WebArgumentResolver} 重写为
 * {@code HandlerMethodArgumentResolver}。更多详情请参见
 * {@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter} 的 javadoc。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletWebArgumentResolverAdapter extends AbstractWebArgumentResolverAdapter {

	public ServletWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		super(adaptee);
	}

	@Override
	protected NativeWebRequest getWebRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Assert.state(requestAttributes instanceof ServletRequestAttributes, "No ServletRequestAttributes");
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
		return new ServletWebRequest(servletRequestAttributes.getRequest());
	}
}
