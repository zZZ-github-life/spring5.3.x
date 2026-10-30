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

package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.util.Assert;

/**
 * {@code WebConnectionHtmlUnitDriver} 支持为 {@link HtmlUnitDriver} 实例
 * 配置 {@link WebConnection}。
 *
 * <p>这很有用，因为它允许注入
 * {@link org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection
 * MockMvcWebConnection}。
 * @author Rob Winch
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.2
 * @see MockMvcHtmlUnitDriverBuilder
 */
public class WebConnectionHtmlUnitDriver extends HtmlUnitDriver {

	public WebConnectionHtmlUnitDriver() {
	}

	public WebConnectionHtmlUnitDriver(BrowserVersion browserVersion) {
		super(browserVersion);
	}

	public WebConnectionHtmlUnitDriver(boolean enableJavascript) {
		super(enableJavascript);
	}

	public WebConnectionHtmlUnitDriver(Capabilities capabilities) {
		super(capabilities);
	}


	/**
	 * 修改提供的 {@link WebClient} 并保留对其的引用，以便其
	 * {@link WebConnection} 可以在以后通过 {@linkplain #getWebConnection 访问}。
	 * <p>默认行为委托给 {@link HtmlUnitDriver#modifyWebClient}，
	 * 进一步的自定义则委托给 {@link #modifyWebClientInternal}。
	 * @param webClient 要修改的客户端
	 * @return 修改后的客户端
	 * @see HtmlUnitDriver#modifyWebClient(WebClient)
	 * @see #modifyWebClientInternal(WebClient)
	 */
	@Override
	protected final WebClient modifyWebClient(WebClient webClient) {
		return modifyWebClientInternal(super.modifyWebClient(webClient));
	}

	/**
	 * 修改提供的 {@link WebClient}。
	 * <p>默认实现仅原样返回提供的客户端。
	 * <p>子类可以覆盖此方法以自定义 {@link HtmlUnitDriver}
	 * 使用的 {@code WebClient}。
	 * @param webClient 要修改的客户端
	 * @return 修改后的客户端
	 */
	protected WebClient modifyWebClientInternal(WebClient webClient) {
		return webClient;
	}

	/**
	 * 以公共方式返回当前的 {@link WebClient}。
	 * @since 4.3
	 */
	@Override
	public WebClient getWebClient() {
		return super.getWebClient();
	}

	/**
	 * 设置要与 {@link WebClient} 一起使用的 {@link WebConnection}。
	 * @param webConnection 要使用的 {@code WebConnection}
	 */
	public void setWebConnection(WebConnection webConnection) {
		Assert.notNull(webConnection, "WebConnection must not be null");
		getWebClient().setWebConnection(webConnection);
	}

	/**
	 * 访问 {@link WebClient} 当前的 {@link WebConnection}。
	 * @return 当前的 {@code WebConnection}
	 */
	public WebConnection getWebConnection() {
		return getWebClient().getWebConnection();
	}

}
