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

package org.springframework.web.socket.server.standard;

import javax.servlet.ServletContext;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

/**
 * 用于配置 {@link javax.websocket.server.ServerContainer} 的 {@link FactoryBean}。
 * 由于通常只能通过一个众所周知的 {@code javax.servlet.ServletContext} 属性
 * 访问一个 {@code ServerContainer} 实例，因此只需声明此 FactoryBean
 * 并使用其 setter，就可以通过 Spring 配置来配置该 {@code ServerContainer}。
 *
 * <p>即使 {@code ServerContainer} 没有被注入到 Spring 应用程序上下文中的
 * 任何其他 Bean 中，这仍然很有用。例如，应用程序可以配置
 * {@link org.springframework.web.socket.server.support.DefaultHandshakeHandler}、
 * {@link org.springframework.web.socket.sockjs.SockJsService} 或
 * {@link ServerEndpointExporter}，并单独声明此 FactoryBean，
 * 以自定义这个（唯一的）{@code ServerContainer} 实例的属性。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class ServletServerContainerFactoryBean
		implements FactoryBean<WebSocketContainer>, ServletContextAware, InitializingBean {

	@Nullable
	private Long asyncSendTimeout;

	@Nullable
	private Long maxSessionIdleTimeout;

	@Nullable
	private Integer maxTextMessageBufferSize;

	@Nullable
	private Integer maxBinaryMessageBufferSize;

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private ServerContainer serverContainer;


	public void setAsyncSendTimeout(Long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	@Nullable
	public Long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}

	public void setMaxSessionIdleTimeout(Long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	@Nullable
	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	public void setMaxTextMessageBufferSize(Integer bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	@Nullable
	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	@Nullable
	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(this.servletContext != null,
				"A ServletContext is required to access the javax.websocket.server.ServerContainer instance");
		this.serverContainer = (ServerContainer) this.servletContext.getAttribute(
				"javax.websocket.server.ServerContainer");
		Assert.state(this.serverContainer != null,
				"Attribute 'javax.websocket.server.ServerContainer' not found in ServletContext");

		if (this.asyncSendTimeout != null) {
			this.serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
		}
		if (this.maxSessionIdleTimeout != null) {
			this.serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		if (this.maxTextMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		if (this.maxBinaryMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}
	}


	@Override
	@Nullable
	public ServerContainer getObject() {
		return this.serverContainer;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.serverContainer != null ? this.serverContainer.getClass() : ServerContainer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
