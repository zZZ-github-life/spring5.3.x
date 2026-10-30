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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 使用 <a href="https://netty.io/">Netty 4</a> 创建请求的
 * {@link org.springframework.http.client.ClientHttpRequestFactory} 实现。
 *
 * <p>允许使用预配置的 {@link EventLoopGroup} 实例：便于
 * 跨多个客户端共享。
 *
 * <p>请注意，此实现会在每次请求时始终关闭
 * HTTP 连接。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Mark Paluch
 * @since 4.1.2
 * @deprecated as of Spring 5.0, in favor of
 * {@link org.springframework.http.client.reactive.ReactorClientHttpConnector}
 */
@Deprecated
public class Netty4ClientHttpRequestFactory implements ClientHttpRequestFactory,
		AsyncClientHttpRequestFactory, InitializingBean, DisposableBean {

	/**
	 * 默认的最大响应大小。
	 * @see #setMaxResponseSize(int)
	 */
	public static final int DEFAULT_MAX_RESPONSE_SIZE = 1024 * 1024 * 10;


	private final EventLoopGroup eventLoopGroup;

	private final boolean defaultEventLoopGroup;

	private int maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;

	@Nullable
	private SslContext sslContext;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	@Nullable
	private volatile Bootstrap bootstrap;


	/**
	 * 使用默认的 {@link NioEventLoopGroup}
	 * 创建新的 {@code Netty4ClientHttpRequestFactory}。
	 */
	public Netty4ClientHttpRequestFactory() {
		int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
		this.eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
		this.defaultEventLoopGroup = true;
	}

	/**
	 * 使用给定的 {@link EventLoopGroup}
	 * 创建新的 {@code Netty4ClientHttpRequestFactory}。
	 * <p><b>注意：</b>此工厂<strong>不会</strong> {@linkplain EventLoopGroup#shutdownGracefully() 关闭}
	 * 给定的 group；关闭操作由调用者负责。
	 */
	public Netty4ClientHttpRequestFactory(EventLoopGroup eventLoopGroup) {
		Assert.notNull(eventLoopGroup, "EventLoopGroup must not be null");
		this.eventLoopGroup = eventLoopGroup;
		this.defaultEventLoopGroup = false;
	}


	/**
	 * 设置默认的最大响应大小。
	 * <p>默认设置为 {@link #DEFAULT_MAX_RESPONSE_SIZE}。
	 * @since 4.1.5
	 * @see HttpObjectAggregator#HttpObjectAggregator(int)
	 */
	public void setMaxResponseSize(int maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	/**
	 * 设置 SSL 上下文。配置后，将使用它在通道管道中
	 * 创建并插入 {@link io.netty.handler.ssl.SslHandler}。
	 * <p>如果未提供，则配置默认的客户端 SslContext。
	 */
	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * 设置底层连接超时（以毫秒为单位）。
	 * 超时值 0 表示无限超时。
	 * @see ChannelConfig#setConnectTimeoutMillis(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层 URLConnection 的读取超时（以毫秒为单位）。
	 * 超时值 0 表示无限超时。
	 * @see ReadTimeoutHandler
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.sslContext == null) {
			this.sslContext = getDefaultClientSslContext();
		}
	}

	private SslContext getDefaultClientSslContext() {
		try {
			return SslContextBuilder.forClient().build();
		}
		catch (SSLException ex) {
			throw new IllegalStateException("Could not create default client SslContext", ex);
		}
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	private Netty4ClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new Netty4ClientHttpRequest(getBootstrap(uri), uri, httpMethod);
	}

	private Bootstrap getBootstrap(URI uri) {
		boolean isSecure = (uri.getPort() == 443 || "https".equalsIgnoreCase(uri.getScheme()));
		if (isSecure) {
			return buildBootstrap(uri, true);
		}
		else {
			Bootstrap bootstrap = this.bootstrap;
			if (bootstrap == null) {
				bootstrap = buildBootstrap(uri, false);
				this.bootstrap = bootstrap;
			}
			return bootstrap;
		}
	}

	private Bootstrap buildBootstrap(URI uri, boolean isSecure) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						configureChannel(channel.config());
						ChannelPipeline pipeline = channel.pipeline();
						if (isSecure) {
							Assert.notNull(sslContext, "sslContext should not be null");
							pipeline.addLast(sslContext.newHandler(channel.alloc(), uri.getHost(), uri.getPort()));
						}
						pipeline.addLast(new HttpClientCodec());
						pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
						if (readTimeout > 0) {
							pipeline.addLast(new ReadTimeoutHandler(readTimeout,
									TimeUnit.MILLISECONDS));
						}
					}
				});
		return bootstrap;
	}

	/**
	 * 用于更改给定 {@link SocketChannelConfig} 属性的模板方法。
	 * <p>默认实现根据所设置的属性设置连接超时。
	 * @param config 通道配置
	 */
	protected void configureChannel(SocketChannelConfig config) {
		if (this.connectTimeout >= 0) {
			config.setConnectTimeoutMillis(this.connectTimeout);
		}
	}


	@Override
	public void destroy() throws InterruptedException {
		if (this.defaultEventLoopGroup) {
			// Clean up the EventLoopGroup if we created it in the constructor
			this.eventLoopGroup.shutdownGracefully().sync();
		}
	}

}
