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

package org.springframework.test.web.servlet.setup;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.MockMvcBuilderSupport;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.ConfigurableSmartRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link MockMvcBuilder} 的抽象实现，提供用于配置过滤器、
 * 默认请求属性、全局预期和全局结果操作的通用方法。
 *
 * <p>子类可以使用不同的策略来准备将传递给
 * {@code DispatcherServlet} 的 Spring {@code WebApplicationContext}。
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.0
 * @param <B> a self reference to the builder type
 */
public abstract class AbstractMockMvcBuilder<B extends AbstractMockMvcBuilder<B>>
		extends MockMvcBuilderSupport implements ConfigurableMockMvcBuilder<B> {

	private List<Filter> filters = new ArrayList<>();

	@Nullable
	private RequestBuilder defaultRequestBuilder;

	@Nullable
	private Charset defaultResponseCharacterEncoding;

	private final List<ResultMatcher> globalResultMatchers = new ArrayList<>();

	private final List<ResultHandler> globalResultHandlers = new ArrayList<>();

	private final List<DispatcherServletCustomizer> dispatcherServletCustomizers = new ArrayList<>();

	private final List<MockMvcConfigurer> configurers = new ArrayList<>(4);


	@Override
	public final <T extends B> T addFilters(Filter... filters) {
		Assert.notNull(filters, "filters cannot be null");
		for (Filter f : filters) {
			Assert.notNull(f, "filters cannot contain null values");
			this.filters.add(f);
		}
		return self();
	}

	@Override
	public final <T extends B> T addFilter(Filter filter, String... urlPatterns) {
		Assert.notNull(filter, "filter cannot be null");
		Assert.notNull(urlPatterns, "urlPatterns cannot be null");
		if (urlPatterns.length > 0) {
			filter = new PatternMappingFilterProxy(filter, urlPatterns);
		}
		this.filters.add(filter);
		return self();
	}

	@Override
	public final <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
		return self();
	}

	/**
	 * 定义应用于每个响应的默认字符编码。
	 * @param defaultResponseCharacterEncoding the default response character encoding
	 * @since 5.3.10
	 */
	@Override
	public final <T extends B> T defaultResponseCharacterEncoding(Charset defaultResponseCharacterEncoding) {
		this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
		return self();
	}

	@Override
	public final <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
		this.globalResultMatchers.add(resultMatcher);
		return self();
	}

	@Override
	public final <T extends B> T alwaysDo(ResultHandler resultHandler) {
		this.globalResultHandlers.add(resultHandler);
		return self();
	}

	@Override
	public final <T extends B> T addDispatcherServletCustomizer(DispatcherServletCustomizer customizer) {
		this.dispatcherServletCustomizers.add(customizer);
		return self();
	}

	@Override
	public final <T extends B> T dispatchOptions(boolean dispatchOptions) {
		return addDispatcherServletCustomizer(
				dispatcherServlet -> dispatcherServlet.setDispatchOptionsRequest(dispatchOptions));
	}

	@Override
	public final <T extends B> T apply(MockMvcConfigurer configurer) {
		configurer.afterConfigurerAdded(this);
		this.configurers.add(configurer);
		return self();
	}

	@SuppressWarnings("unchecked")
	protected <T extends B> T self() {
		return (T) this;
	}


	/**
	 * 构建 {@link org.springframework.test.web.servlet.MockMvc} 实例。
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public final MockMvc build() {
		WebApplicationContext wac = initWebAppContext();
		ServletContext servletContext = wac.getServletContext();
		MockServletConfig mockServletConfig = new MockServletConfig(servletContext);

		for (MockMvcConfigurer configurer : this.configurers) {
			RequestPostProcessor processor = configurer.beforeMockMvcCreated(this, wac);
			if (processor != null) {
				if (this.defaultRequestBuilder == null) {
					this.defaultRequestBuilder = MockMvcRequestBuilders.get("/");
				}
				if (this.defaultRequestBuilder instanceof ConfigurableSmartRequestBuilder) {
					((ConfigurableSmartRequestBuilder) this.defaultRequestBuilder).with(processor);
				}
			}
		}

		Filter[] filterArray = this.filters.toArray(new Filter[0]);

		return super.createMockMvc(filterArray, mockServletConfig, wac, this.defaultRequestBuilder,
				this.defaultResponseCharacterEncoding, this.globalResultMatchers, this.globalResultHandlers,
				this.dispatcherServletCustomizers);
	}

	/**
	 * 用于获取将传递给 {@code DispatcherServlet} 的
	 * {@code WebApplicationContext} 的方法。在创建 {@link MockMvc}
	 * 实例之前由 {@link #build()} 调用。
	 */
	protected abstract WebApplicationContext initWebAppContext();

}
