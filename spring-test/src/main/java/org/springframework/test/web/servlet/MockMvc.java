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

package org.springframework.test.web.servlet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * <strong>服务器端 Spring MVC 测试支持的主要入口点。</strong>
 *
 * <h3>示例</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 * .andExpectAll(
 * status().isOk(),
 * content().contentType("text/html"),
 * forwardedUrl("/WEB-INF/layouts/main.jsp")
 * );
 * </pre>
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 3.2
 */
public final class MockMvc {

	static final String MVC_RESULT_ATTRIBUTE = MockMvc.class.getName().concat(".MVC_RESULT_ATTRIBUTE");

	private final TestDispatcherServlet servlet;

	private final Filter[] filters;

	private final ServletContext servletContext;

	@Nullable
	private RequestBuilder defaultRequestBuilder;

	@Nullable
	private Charset defaultResponseCharacterEncoding;

	private List<ResultMatcher> defaultResultMatchers = new ArrayList<>();

	private List<ResultHandler> defaultResultHandlers = new ArrayList<>();


	/**
	 * 私有构造函数，不用于直接实例化。
	 * @see org.springframework.test.web.servlet.setup.MockMvcBuilders
	 */
	MockMvc(TestDispatcherServlet servlet, Filter... filters) {
		Assert.notNull(servlet, "DispatcherServlet is required");
		Assert.notNull(filters, "Filters cannot be null");
		Assert.noNullElements(filters, "Filters cannot contain null values");

		this.servlet = servlet;
		this.filters = filters;
		this.servletContext = servlet.getServletContext();
	}


	/**
	 * 合并到每个已执行请求中的默认请求构建器。
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#defaultRequest(RequestBuilder)
	 */
	void setDefaultRequest(@Nullable RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
	}

	/**
	 * 应用于每个响应的默认字符编码。
	 * @see org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder#defaultResponseCharacterEncoding(Charset)
	 */
	void setDefaultResponseCharacterEncoding(@Nullable Charset defaultResponseCharacterEncoding) {
		this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
	}

	/**
	 * 每次执行请求后要断言的预期。
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysExpect(ResultMatcher)
	 */
	void setGlobalResultMatchers(List<ResultMatcher> resultMatchers) {
		Assert.notNull(resultMatchers, "ResultMatcher List is required");
		this.defaultResultMatchers = resultMatchers;
	}

	/**
	 * 每次执行请求后要应用的通用操作。
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysDo(ResultHandler)
	 */
	void setGlobalResultHandlers(List<ResultHandler> resultHandlers) {
		Assert.notNull(resultHandlers, "ResultHandler List is required");
		this.defaultResultHandlers = resultHandlers;
	}

	/**
	 * 返回初始化此 {@code MockMvc} 时使用的底层 {@link DispatcherServlet} 实例。
	 * <p>这适用于自定义请求处理场景：请求处理组件在运行时恰好委托给 {@code DispatcherServlet}，
	 * 因此需要注入该实例。
	 * <p>对于大多数处理场景，只需使用 {@link MockMvc#perform}，
	 * 或者如果需要配置 {@code DispatcherServlet}，请向
	 * {@code MockMvcBuilder} 提供 {@link DispatcherServletCustomizer}。
	 * @since 5.1
	 */
	public DispatcherServlet getDispatcherServlet() {
		return this.servlet;
	}


	/**
	 * 执行请求并返回一个类型，以便在结果上链接进一步操作，
	 * 例如断言预期。
	 * @param requestBuilder used to prepare the request to execute;
	 * see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 * @return an instance of {@link ResultActions} (never {@code null})
	 * @see org.springframework.test.web.servlet.request.MockMvcRequestBuilders
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers
	 */
	public ResultActions perform(RequestBuilder requestBuilder) throws Exception {
		if (this.defaultRequestBuilder != null && requestBuilder instanceof Mergeable) {
			requestBuilder = (RequestBuilder) ((Mergeable) requestBuilder).merge(this.defaultRequestBuilder);
		}

		MockHttpServletRequest request = requestBuilder.buildRequest(this.servletContext);

		AsyncContext asyncContext = request.getAsyncContext();
		MockHttpServletResponse mockResponse;
		HttpServletResponse servletResponse;
		if (asyncContext != null) {
			servletResponse = (HttpServletResponse) asyncContext.getResponse();
			mockResponse = unwrapResponseIfNecessary(servletResponse);
		}
		else {
			mockResponse = new MockHttpServletResponse();
			servletResponse = mockResponse;
		}

		if (this.defaultResponseCharacterEncoding != null) {
			mockResponse.setDefaultCharacterEncoding(this.defaultResponseCharacterEncoding.name());
		}

		if (requestBuilder instanceof SmartRequestBuilder) {
			request = ((SmartRequestBuilder) requestBuilder).postProcessRequest(request);
		}

		MvcResult mvcResult = new DefaultMvcResult(request, mockResponse);
		request.setAttribute(MVC_RESULT_ATTRIBUTE, mvcResult);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, servletResponse));

		MockFilterChain filterChain = new MockFilterChain(this.servlet, this.filters);
		filterChain.doFilter(request, servletResponse);

		if (DispatcherType.ASYNC.equals(request.getDispatcherType()) &&
				asyncContext != null && !request.isAsyncStarted()) {
			asyncContext.complete();
		}

		applyDefaultResultActions(mvcResult);
		RequestContextHolder.setRequestAttributes(previousAttributes);

		return new ResultActions() {
			@Override
			public ResultActions andExpect(ResultMatcher matcher) throws Exception {
				matcher.match(mvcResult);
				return this;
			}
			@Override
			public ResultActions andDo(ResultHandler handler) throws Exception {
				handler.handle(mvcResult);
				return this;
			}
			@Override
			public MvcResult andReturn() {
				return mvcResult;
			}
		};
	}

	private MockHttpServletResponse unwrapResponseIfNecessary(ServletResponse servletResponse) {
		while (servletResponse instanceof HttpServletResponseWrapper) {
			servletResponse = ((HttpServletResponseWrapper) servletResponse).getResponse();
		}
		Assert.isInstanceOf(MockHttpServletResponse.class, servletResponse);
		return (MockHttpServletResponse) servletResponse;
	}

	private void applyDefaultResultActions(MvcResult mvcResult) throws Exception {
		for (ResultHandler handler : this.defaultResultHandlers) {
			handler.handle(mvcResult);
		}
		for (ResultMatcher matcher : this.defaultResultMatchers) {
			matcher.match(mvcResult);
		}
	}

}
