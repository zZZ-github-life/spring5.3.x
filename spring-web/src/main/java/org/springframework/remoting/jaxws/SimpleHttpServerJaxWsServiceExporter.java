/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.remoting.jaxws;

import java.net.InetSocketAddress;
import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceProvider;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JAX-WS 服务的简单导出器，自动检测带注解的服务 bean
 * （通过 JAX-WS 的 {@link javax.jws.WebService} 注解），并通过
 * Sun JDK 1.6 内置的 HTTP 服务器导出它们。每个服务的完整地址
 * 由服务器的基地址加上服务名称组成
 * （例如 "http://localhost:8080/OrderService"）。
 *
 * <p>请注意，此导出器仅能在 Sun JDK 1.6 或更高版本上运行，
 * 以及在包含 Sun JDK 完整类库的 JDK 上运行。
 * 如需可移植的 JAX-WS 导出器，请参阅 {@link SimpleJaxWsServiceExporter}。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see javax.jws.WebService
 * @see javax.xml.ws.Endpoint#publish(Object)
 * @see SimpleJaxWsServiceExporter
 * @deprecated as of Spring Framework 5.1, in favor of {@link SimpleJaxWsServiceExporter}
 */
@Deprecated
@org.springframework.lang.UsesSunHttpServer
public class SimpleHttpServerJaxWsServiceExporter extends AbstractJaxWsServiceExporter {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private HttpServer server;

	private int port = 8080;

	@Nullable
	private String hostname;

	private int backlog = -1;

	private int shutdownDelay = 0;

	private String basePath = "/";

	@Nullable
	private List<Filter> filters;

	@Nullable
	private Authenticator authenticator;

	private boolean localServer = false;


	/**
	 * 指定用于注册 Web 服务上下文的现有 HTTP 服务器。
	 * 这通常是由通用 Spring
	 * {@link org.springframework.remoting.support.SimpleHttpServerFactoryBean} 管理的服务器。
	 * <p>或者，通过
	 * {@link #setPort "port"}、{@link #setHostname "hostname"} 和
	 * {@link #setBacklog "backlog"} 属性配置本地 HTTP 服务器（或使用其中的默认值）。
	 */
	public void setServer(HttpServer server) {
		this.server = server;
	}

	/**
	 * 指定 HTTP 服务器的端口。默认为 8080。
	 * <p>仅适用于本地配置的 HTTP 服务器。
	 * 如果已指定 {@link #setServer "server"} 属性，则忽略此项。
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 指定要绑定的 HTTP 服务器主机名。默认为 localhost；
	 * 可以覆盖为要绑定的特定网络地址。
	 * <p>仅适用于本地配置的 HTTP 服务器。
	 * 如果已指定 {@link #setServer "server"} 属性，则忽略此项。
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * 指定 HTTP 服务器的 TCP backlog。默认为 -1，
	 * 表示使用系统的默认值。
	 * <p>仅适用于本地配置的 HTTP 服务器。
	 * 如果已指定 {@link #setServer "server"} 属性，则忽略此项。
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * 指定关闭 HTTP 服务器时等待 HTTP 交换完成的秒数。
	 * 默认为 0。
	 * <p>仅适用于本地配置的 HTTP 服务器。
	 * 如果已指定 {@link #setServer "server"} 属性，则忽略此项。
	 */
	public void setShutdownDelay(int shutdownDelay) {
		this.shutdownDelay = shutdownDelay;
	}

	/**
	 * 设置用于发布上下文的基路径。默认为 "/"。
	 * <p>对于每个上下文发布路径，服务名称将
	 * 追加到此基地址之后。例如服务名称 "OrderService"
	 * &rarr; "/OrderService"。
	 * @see javax.xml.ws.Endpoint#publish(Object)
	 * @see javax.jws.WebService#serviceName()
	 */
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	/**
	 * 注册通用的 {@link com.sun.net.httpserver.Filter Filter}，以应用于
	 * 所有检测到的标注 {@link javax.jws.WebService} 的 bean。
	 */
	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	/**
	 * 注册通用的 {@link com.sun.net.httpserver.Authenticator}，以应用于
	 * 所有检测到的标注 {@link javax.jws.WebService} 的 bean。
	 */
	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.server == null) {
			InetSocketAddress address = (this.hostname != null ?
					new InetSocketAddress(this.hostname, this.port) : new InetSocketAddress(this.port));
			HttpServer server = HttpServer.create(address, this.backlog);
			if (logger.isInfoEnabled()) {
				logger.info("Starting HttpServer at address " + address);
			}
			server.start();
			this.server = server;
			this.localServer = true;
		}
		super.afterPropertiesSet();
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebService annotation) {
		endpoint.publish(buildHttpContext(endpoint, annotation.serviceName()));
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation) {
		endpoint.publish(buildHttpContext(endpoint, annotation.serviceName()));
	}

	/**
	 * 为给定的端点构建 HttpContext。
	 * @param endpoint JAX-WS 的 Provider Endpoint 对象
	 * @param serviceName 给定的服务名称
	 * @return 填充完整的 HttpContext
	 */
	protected HttpContext buildHttpContext(Endpoint endpoint, String serviceName) {
		Assert.state(this.server != null, "No HttpServer available");
		String fullPath = calculateEndpointPath(endpoint, serviceName);
		HttpContext httpContext = this.server.createContext(fullPath);
		if (this.filters != null) {
			httpContext.getFilters().addAll(this.filters);
		}
		if (this.authenticator != null) {
			httpContext.setAuthenticator(this.authenticator);
		}
		return httpContext;
	}

	/**
	 * 计算给定端点的完整端点路径。
	 * @param endpoint JAX-WS 的 Provider Endpoint 对象
	 * @param serviceName 给定的服务名称
	 * @return 完整的端点路径
	 */
	protected String calculateEndpointPath(Endpoint endpoint, String serviceName) {
		return this.basePath + serviceName;
	}


	@Override
	public void destroy() {
		super.destroy();
		if (this.server != null && this.localServer) {
			logger.info("Stopping HttpServer");
			this.server.stop(this.shutdownDelay);
		}
	}

}
