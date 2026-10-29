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

package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnectionBuilderSupport;
import org.springframework.test.web.servlet.htmlunit.WebRequestMatcher;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code MockMvcHtmlUnitDriverBuilder} 简化了构建委托给 {@link MockMvc} 并可针对特定请求
 * 委托给实际连接的 {@link HtmlUnitDriver} 的过程。
 *
 * <p>默认情况下，驱动程序将委托给 {@code MockMvc} 处理
 * 对 {@code localhost} 的请求，并委托给 {@link WebClient} 处理任何
 * 其他 URL（即执行实际的 HTTP 请求）。
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see #mockMvcSetup(MockMvc)
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #webAppContextSetup(WebApplicationContext, MockMvcConfigurer)
 * @see #javascriptEnabled(boolean)
 * @see #withDelegate(WebConnectionHtmlUnitDriver)
 * @see #build()
 */
public class MockMvcHtmlUnitDriverBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcHtmlUnitDriverBuilder> {

	@Nullable
	private HtmlUnitDriver driver;

	private boolean javascriptEnabled = true;


	protected MockMvcHtmlUnitDriverBuilder(MockMvc mockMvc) {
		super(mockMvc);
	}

	protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context) {
		super(context);
	}

	protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
		super(context, configurer);
	}


	/**
	 * 基于提供的 {@link MockMvc} 实例创建新的 {@code MockMvcHtmlUnitDriverBuilder}。
	 * @param mockMvc the {@code MockMvc} instance to use (never {@code null})
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder mockMvcSetup(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		return new MockMvcHtmlUnitDriverBuilder(mockMvc);
	}

	/**
	 * 基于提供的 {@link WebApplicationContext} 创建新的 {@code MockMvcHtmlUnitDriverBuilder}。
	 * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
	 * instance from (never {@code null})
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context) {
		Assert.notNull(context, "WebApplicationContext must not be null");
		return new MockMvcHtmlUnitDriverBuilder(context);
	}

	/**
	 * 基于提供的 {@link WebApplicationContext} 和 {@link MockMvcConfigurer} 创建新的
	 * {@code MockMvcHtmlUnitDriverBuilder}。
	 * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
	 * instance from (never {@code null})
	 * @param configurer the {@code MockMvcConfigurer} to apply (never {@code null})
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context,
			MockMvcConfigurer configurer) {

		Assert.notNull(context, "WebApplicationContext must not be null");
		Assert.notNull(configurer, "MockMvcConfigurer must not be null");
		return new MockMvcHtmlUnitDriverBuilder(context, configurer);
	}

	/**
	 * 指定是否应启用 JavaScript。
	 * <p>默认为 {@code true}。
	 * @param javascriptEnabled {@code true} if JavaScript should be enabled
	 * @return this builder for further customizations
	 * @see #build()
	 */
	public MockMvcHtmlUnitDriverBuilder javascriptEnabled(boolean javascriptEnabled) {
		this.javascriptEnabled = javascriptEnabled;
		return this;
	}

	/**
	 * 提供此构建器 {@linkplain #build 构建} 的驱动程序在处理
	 * 非{@linkplain WebRequestMatcher 匹配}请求时应委托给
	 * 的 {@code WebConnectionHtmlUnitDriver}。
	 * @param driver the {@code WebConnectionHtmlUnitDriver} to delegate to
	 * for requests that do not match (never {@code null})
	 * @return this builder for further customizations
	 * @see #build()
	 */
	public MockMvcHtmlUnitDriverBuilder withDelegate(WebConnectionHtmlUnitDriver driver) {
		Assert.notNull(driver, "HtmlUnitDriver must not be null");
		driver.setJavascriptEnabled(this.javascriptEnabled);
		driver.setWebConnection(createConnection(driver.getWebClient()));
		this.driver = driver;
		return this;
	}

	/**
	 * 构建通过此构建器配置的 {@link HtmlUnitDriver}。
	 * <p>返回的驱动程序将使用配置的 {@link MockMvc} 实例
	 * 处理任何 {@linkplain WebRequestMatcher 匹配}请求，
	 * 并对所有其他请求使用委托的 {@code HtmlUnitDriver}。
	 * <p>如果已显式配置{@linkplain #withDelegate 委托}，
	 * 则使用该委托；否则，将配置一个将 {@link BrowserVersion} 设置为
	 * {@link BrowserVersion#CHROME CHROME} 的默认 {@code WebConnectionHtmlUnitDriver}
	 * 作为委托。
	 * @return the {@code HtmlUnitDriver} to use
	 * @see #withDelegate(WebConnectionHtmlUnitDriver)
	 */
	public HtmlUnitDriver build() {
		return (this.driver != null ? this.driver :
				withDelegate(new WebConnectionHtmlUnitDriver(BrowserVersion.CHROME)).build());
	}

}
