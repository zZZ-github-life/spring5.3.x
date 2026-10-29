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

package org.springframework.web.socket.server.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.handler.BeanCreatingHandlerProvider;

/**
 * 用于基于 Spring 的应用程序的 {@link javax.websocket.server.ServerEndpointConfig}
 * 实现。{@link ServerEndpointRegistration} Bean 会被
 * {@link ServerEndpointExporter} 检测到，并在启动时注册到 Java WebSocket 运行时。
 *
 * <p>类构造函数接受单例 {@link javax.websocket.Endpoint} 实例
 * 或由 {@link Class} 类型指定的 Endpoint。当通过类型指定时，
 * 端点将在每个客户端 WebSocket 连接之前通过 Spring ApplicationContext
 * 进行实例化和初始化。
 *
 * <p>此类还扩展了
 * {@link javax.websocket.server.ServerEndpointConfig.Configurator}，
 * 以便更容易地覆盖方法来定制握手过程。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 * @see ServerEndpointExporter
 */
public class ServerEndpointRegistration extends ServerEndpointConfig.Configurator
		implements ServerEndpointConfig, BeanFactoryAware {

	private final String path;

	@Nullable
	private final Endpoint endpoint;

	@Nullable
	private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

	private List<String> subprotocols = new ArrayList<>(0);

	private List<Extension> extensions = new ArrayList<>(0);

	private List<Class<? extends Encoder>> encoders = new ArrayList<>(0);

	private List<Class<? extends Decoder>> decoders = new ArrayList<>(0);

	private final Map<String, Object> userProperties = new HashMap<>(4);


	/**
	 * 从 {@code javax.websocket.Endpoint} 实例创建一个新的
	 * {@link ServerEndpointRegistration} 实例。
	 * @param path the endpoint path
	 * @param endpoint the endpoint instance
	 */
	public ServerEndpointRegistration(String path, Endpoint endpoint) {
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.path = path;
		this.endpoint = endpoint;
		this.endpointProvider = null;
	}

	/**
	 * 从 {@code javax.websocket.Endpoint} 类创建一个新的
	 * {@link ServerEndpointRegistration} 实例。
	 * @param path the endpoint path
	 * @param endpointClass the endpoint class
	 */
	public ServerEndpointRegistration(String path, Class<? extends Endpoint> endpointClass) {
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(endpointClass, "Endpoint Class must not be null");
		this.path = path;
		this.endpoint = null;
		this.endpointProvider = new BeanCreatingHandlerProvider<>(endpointClass);
	}


	// ServerEndpointConfig implementation

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public Class<? extends Endpoint> getEndpointClass() {
		if (this.endpoint != null) {
			return this.endpoint.getClass();
		}
		else {
			Assert.state(this.endpointProvider != null, "No endpoint set");
			return this.endpointProvider.getHandlerType();
		}
	}

	public Endpoint getEndpoint() {
		if (this.endpoint != null) {
			return this.endpoint;
		}
		else {
			Assert.state(this.endpointProvider != null, "No endpoint set");
			return this.endpointProvider.getHandler();
		}
	}

	public void setSubprotocols(List<String> subprotocols) {
		this.subprotocols = subprotocols;
	}

	@Override
	public List<String> getSubprotocols() {
		return this.subprotocols;
	}

	public void setExtensions(List<Extension> extensions) {
		this.extensions = extensions;
	}

	@Override
	public List<Extension> getExtensions() {
		return this.extensions;
	}

	public void setEncoders(List<Class<? extends Encoder>> encoders) {
		this.encoders = encoders;
	}

	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return this.encoders;
	}

	public void setDecoders(List<Class<? extends Decoder>> decoders) {
		this.decoders = decoders;
	}

	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return this.decoders;
	}

	public void setUserProperties(Map<String, Object> userProperties) {
		this.userProperties.clear();
		this.userProperties.putAll(userProperties);
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	@Override
	public Configurator getConfigurator() {
		return this;
	}


	// ServerEndpointConfig.Configurator implementation

	@SuppressWarnings("unchecked")
	@Override
	public final <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		return (T) getEndpoint();
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		super.modifyHandshake(this, request, response);
	}


	// Remaining methods

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.endpointProvider != null) {
			this.endpointProvider.setBeanFactory(beanFactory);
		}
	}

	@Override
	public String toString() {
		return "ServerEndpointRegistration for path '" + getPath() + "': " + getEndpointClass();
	}

}
