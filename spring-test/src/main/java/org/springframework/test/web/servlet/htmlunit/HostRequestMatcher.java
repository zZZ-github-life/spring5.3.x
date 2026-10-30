/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * 一个 {@link WebRequestMatcher}，允许根据 {@code WebRequest#getUrl()} 的主机以及可选的端口进行匹配。
 *
 * <p>例如，以下配置将匹配发送到主机
 * {@code "code.jquery.com"} 的任何请求，而不考虑端口。
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com");</pre>
 *
 * <p>也可以传入多个主机。例如，以下配置将
 * 匹配发送到主机 {@code "code.jquery.com"} 或主机
 * {@code "cdn.com"} 的任何请求，而不考虑端口。
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com", "cdn.com");</pre>
 *
 * <p>或者，也可以指定端口。例如，以下配置将匹配
 * 发送到主机 {@code "code.jquery.com"} 且端口为 {@code 80} 的任何请求。
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.example:80");</pre>
 *
 * <p>上述 {@code cdnMatcher} 将匹配 {@code "http://code.jquery.example/jquery.js"}
 * （其默认端口为 {@code 80}）以及 {@code "http://code.jquery.example:80/jquery.js"}。
 * 但是，它不会匹配 {@code "https://code.jquery.example/jquery.js"}，
 * 因为该地址的默认端口为 {@code 443}。
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see UrlRegexRequestMatcher
 * @see org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection
 */
public final class HostRequestMatcher implements WebRequestMatcher {

	private final Set<String> hosts = new HashSet<>();


	/**
	 * 为给定主机创建新的 {@code HostRequestMatcher} &mdash;
	 * 例如：{@code "localhost"}、{@code "example.com:443"} 等。
	 * @param hosts 要匹配的主机
	 */
	public HostRequestMatcher(String... hosts) {
		Collections.addAll(this.hosts, hosts);
	}


	@Override
	public boolean matches(WebRequest request) {
		URL url = request.getUrl();
		String host = url.getHost();

		if (this.hosts.contains(host)) {
			return true;
		}

		int port = url.getPort();
		if (port == -1) {
			port = url.getDefaultPort();
		}
		String hostAndPort = host + ":" + port;

		return this.hosts.contains(hostAndPort);
	}

}
