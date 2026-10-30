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

package org.springframework.messaging.rsocket;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.loadbalance.LoadbalanceStrategy;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Decoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.MimeType;

/**
 * 对发送方 {@link RSocket} 的轻量封装，提供流畅的 API，接受并返回更高层次的
 * 输入输出对象，同时提供用于准备路由和其他元数据的方法。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.2
 */
public interface RSocketRequester extends Disposable {

	/**
	 * 返回用于发出请求的底层 {@link RSocketClient}。
	 * @since 5.3
	 */
	RSocketClient rsocketClient();

	/**
	 * 如果请求者是通过 {@link #wrap(RSocket, MimeType, MimeType, RSocketStrategies)}
	 * 或通过 {@code RSocketRequester} 构建器上的（已弃用的）连接方法之一使用
	 * "live" RSocket 创建的，则返回底层 {@link RSocket}，否则返回 {@code null}。
	 */
	@Nullable
	RSocket rsocket();

	/**
	 * 返回连接时为底层 RSocket 选择的数据 {@code MimeType}。在客户端，这通过
	 * {@link RSocketRequester.Builder#dataMimeType(MimeType)} 配置；在服务器端，
	 * 则从 {@link ConnectionSetupPayload} 获取。
	 */
	MimeType dataMimeType();

	/**
	 * 返回连接时为底层 RSocket 选择的元数据 {@code MimeType}。在客户端，这通过
	 * {@link RSocketRequester.Builder#metadataMimeType(MimeType)} 配置；在服务器端，
	 * 则从 {@link ConnectionSetupPayload} 获取。
	 */
	MimeType metadataMimeType();

	/**
	 * 开始指定一个带有给定路由的新请求，指向远程处理器。
	 * <p>路由可以是带占位符的模板，例如 {@code "flight.{code}"}，此时提供的
	 * 路由变量会通过 {@code toString()} 格式化并展开到模板中。
	 * 如果格式化后的变量包含 "."，会被替换为转义序列 "%2E"，以免被响应方
	 * 当作分隔符处理。
	 * <p>如果连接设置为使用复合元数据，则路由编码为
	 * {@code "message/x.rsocket.routing.v0"}。否则，路由按照连接的
	 * mime 类型进行编码。
	 * @param route 表示远程处理器映射的路由
	 * @param routeVars 要展开到路由模板中的变量
	 * @return 用于进一步定义和执行该请求的 spec
	 */
	RequestSpec route(String route, Object... routeVars);

	/**
	 * 开始指定一个带有给定元数据值的新请求，该值可以是具体值，也可以是任何
	 * 可通过 {@link ReactiveAdapterRegistry} 适配为 {@link Publisher} 的单值生产者。
	 * @param metadata 要编码的元数据值
	 * @param mimeType 描述该元数据的 MIME 类型；
	 * 使用复合元数据（composite metadata）的连接必须提供该参数。
	 * 否则，值将按连接的 MIME 类型编码，
	 * 该参数可保留为 {@code null}。
	 */
	RequestSpec metadata(Object metadata, @Nullable MimeType mimeType);

	/**
	 * 快捷方法，委托给底层 {@link #rsocketClient()} 上的同名方法，
	 * 以便关闭底层传输的连接并通知订阅者。
	 * @since 5.3.7
	 */
	@Override
	default void dispose() {
		rsocketClient().dispose();
	}

	/**
	 * 快捷方法，委托给底层 {@link #rsocketClient()} 上的同名方法。
	 * @since 5.3.7
	 */
	@Override
	default boolean isDisposed() {
		return rsocketClient().isDisposed();
	}

	/**
	 * 获取一个构建器，通过连接到 RSocket 服务器来创建客户端 {@link RSocketRequester}。
	 */
	static RSocketRequester.Builder builder() {
		return new DefaultRSocketRequesterBuilder();
	}

	/**
	 * 包装现有的 {@link RSocket}。通常供框架内部使用，用于在客户端或服务器
	 * 响应方中包装远程 {@code RSocket}，但也可以用于包装任何 {@link RSocket}。
	 */
	static RSocketRequester wrap(
			RSocket rsocket, MimeType dataMimeType, MimeType metadataMimeType,
			RSocketStrategies strategies) {

		return new DefaultRSocketRequester(null, rsocket, dataMimeType, metadataMimeType, strategies);
	}


	/**
	 * 用于通过连接到服务器来创建请求者的构建器。
	 */
	interface Builder {

		/**
		 * 配置要在应用于整个连接的 {@code SETUP} 帧中指定的负载数据 MimeType。
		 * <p>如果未设置，将初始化为第一个
		 * {@link RSocketStrategies.Builder#decoder(Decoder[]) 非默认}
		 * {@code Decoder} 的 MimeType，否则为第一个解码器的 MimeType。
		 */
		RSocketRequester.Builder dataMimeType(@Nullable MimeType mimeType);

		/**
		 * 配置要在应用于整个连接的 {@code SETUP} 帧中指定的负载元数据 MimeType。
		 * <p>默认设置为 {@code "message/x.rsocket.composite-metadata.v0"}，
		 * 此时如果提供了路由，则将其编码为
		 * {@code "message/x.rsocket.routing.v0"} 复合元数据条目。
		 * 如果将其设置为任何其他 MimeType，则假定该 MimeType 适用于路由
		 * （如果提供了路由）。
		 */
		RSocketRequester.Builder metadataMimeType(MimeType mimeType);

		/**
		 * 设置设置负载的数据。数据将根据配置的 {@link #dataMimeType(MimeType)}
		 * 进行编码。数据可以是具体值，也可以是任何可通过
		 * {@link ReactiveAdapterRegistry} 适配为 {@link Publisher} 的单值生产者。
		 * <p>默认情况下不设置此项。
		 */
		RSocketRequester.Builder setupData(Object data);

		/**
		 * 设置设置负载的路由。路由的格式化和编码规则与请求路由相同，
		 * 如 {@link #route(String, Object...)} 中所述。
		 * <p>默认情况下不设置此项。
		 */
		RSocketRequester.Builder setupRoute(String route, Object... routeVars);

		/**
		 * 向设置负载添加元数据条目。如果多次调用，或者在
		 * {@link #setupRoute(String, Object...)} 之外额外调用，则必须使用复合元数据。
		 * 元数据值可以是具体值，也可以是任何可通过
		 * {@link ReactiveAdapterRegistry} 适配为 {@link Publisher} 的单值生产者。
		 */
		RSocketRequester.Builder setupMetadata(Object value, @Nullable MimeType mimeType);

		/**
		 * 提供要使用的 {@link RSocketStrategies}。
		 * <p>这有助于更改默认设置，同时仍允许通过
		 * {@link #rsocketStrategies(Consumer)} 进行进一步自定义。
		 * 如果未设置，则从 {@link RSocketStrategies#builder()} 获取默认值。
		 * @param strategies 要使用的策略
		 */
		RSocketRequester.Builder rsocketStrategies(@Nullable RSocketStrategies strategies);

		/**
		 * 自定义 {@link RSocketStrategies}。
		 * <p>允许对 {@link RSocketStrategies} 进行进一步自定义：如果它们已通过
		 * {@link #rsocketStrategies(RSocketStrategies) set} 设置，则就地修改；
		 * 否则从 {@link RSocketStrategies#builder()} 默认值开始。
		 */
		RSocketRequester.Builder rsocketStrategies(Consumer<RSocketStrategies.Builder> configurer);

		/**
		 * 直接配置 {@code RSocketConnector} 的回调。
		 * <ul>
		 * <li>数据 MimeType 和元数据 MimeType 无法直接在
		 * {@code RSocketConnector} 上设置，并且会被覆盖。请改为使用本构建器上的
		 * 快捷方法 {@link #dataMimeType(MimeType)} 和
		 * {@link #metadataMimeType(MimeType)}。
		 * <li>帧解码器也无法直接设置，而是会设置为与配置的
		 * {@code DataBufferFactory} 匹配。
		 * <li>对于
		 * {@link io.rsocket.core.RSocketConnector#setupPayload(Payload)
		 * setupPayload}，请考虑使用本构建器上的方法来以 Object 值形式指定
		 * 路由、其他元数据和数据，由框架进行编码。
		 * <li>要配置客户端响应，请参阅
		 * {@link RSocketMessageHandler#responder(RSocketStrategies, Object...)}。
		 * </ul>
		 * @since 5.2.6
		 */
		RSocketRequester.Builder rsocketConnector(RSocketConnectorConfigurer configurer);

		/**
		 * 通过 {@code Consumer} 配置此构建器。这使 Spring Security 等库能够
		 * 提供应用一组相关自定义配置的快捷方式。
		 * @param configurer 要应用的配置器
		 */
		RSocketRequester.Builder apply(Consumer<RSocketRequester.Builder> configurer);

		/**
		 * 使用通过 TCP 连接到给定主机和端口的
		 * {@link io.rsocket.core.RSocketClient} 构建一个 {@link RSocketRequester}。
		 * 该请求者可用于并发发出请求。请求通过共享连接发出，并且在后续请求时
		 * 会按需重新建立该连接。
		 * @param host 要连接的主机
		 * @param port 要连接的端口
		 * @return 创建的 {@code RSocketRequester}
		 * @since 5.3
		 */
		RSocketRequester tcp(String host, int port);

		/**
		 * 使用通过 WebSocket 连接到给定 URL 的
		 * {@link io.rsocket.core.RSocketClient} 构建一个 {@link RSocketRequester}。
		 * 该请求者可用于并发发出请求。请求通过共享连接发出，并且在后续请求时
		 * 会按需重新建立该连接。
		 * @param uri 要连接的 URL
		 * @return 创建的 {@code RSocketRequester}
		 * @since 5.3
		 */
		RSocketRequester websocket(URI uri);

		/**
		 * {@link #tcp(String, int)} 和 {@link #websocket(URI)} 的变体，
		 * 使用已初始化的 {@link ClientTransport}。
		 * @param transport 用于连接的传输层
		 * @return 创建的 {@code RSocketRequester}
		 * @since 5.3
		 */
		RSocketRequester transport(ClientTransport transport);

		/**
		 * 使用 {@link io.rsocket.loadbalance.LoadbalanceRSocketClient} 构建一个
		 * {@link RSocketRequester}，该客户端会连接到通过给定
		 * {@link io.rsocket.loadbalance.LoadbalanceRSocketClient} 选出的多个目标之一。
		 * @param targetPublisher 提供一组用于负载均衡的目标传输层的 {@code Publisher}；
		 * 给定的列表可能由该 {@code Publisher} 定期更新。
		 * @param loadbalanceStrategy 从负载均衡目标列表中进行选择所使用的策略。
		 * @return 创建的 {@code RSocketRequester}
		 * @since 5.3
		 */
		RSocketRequester transports(
				Publisher<List<LoadbalanceTarget>> targetPublisher,
				LoadbalanceStrategy loadbalanceStrategy);

		/**
		 * 通过 TCP 连接到服务器。
		 * @param host 服务器主机
		 * @param port 服务器端口
		 * @return 用于该连接的 {@code RSocketRequester}
		 * @see TcpClientTransport
		 * @deprecated as of 5.3 in favor of {@link #tcp(String, int)}
		 */
		@Deprecated
		Mono<RSocketRequester> connectTcp(String host, int port);

		/**
		 * 通过 WebSocket 连接到服务器。
		 * @param uri RSocket 服务器端点 URI
		 * @return 用于该连接的 {@code RSocketRequester}
		 * @see WebsocketClientTransport
		 * @deprecated as of 5.3 in favor of {@link #websocket(URI)}
		 */
		@Deprecated
		Mono<RSocketRequester> connectWebSocket(URI uri);

		/**
		 * 使用给定的 {@code ClientTransport} 连接到服务器。
		 * @param transport 要使用的客户端传输层
		 * @return 用于该连接的 {@code RSocketRequester}
		 * @deprecated as of 5.3 in favor of {@link #transport(ClientTransport)}
		 */
		@Deprecated
		Mono<RSocketRequester> connect(ClientTransport transport);

	}

	/**
	 * 用于声明 RSocket 请求输入的规范。
	 */
	interface RequestSpec extends MetadataSpec<RequestSpec>, RetrieveSpec {

		/**
		 * 通过 {@code Consumer} 追加额外的元数据条目。
		 * 这使 Spring Security 等库能够提供应用一组自定义配置的快捷方式。
		 * @param configurer 要应用的配置器
		 * @throws IllegalArgumentException 如果未使用复合元数据。
		 */
		RequestSpec metadata(Consumer<MetadataSpec<?>> configurer);

		/**
		 * 执行 {@link RSocket#metadataPush(Payload) metadataPush}。
		 * @since 5.3
		 */
		Mono<Void> sendMetadata();

		/**
		 * 为请求提供负载数据。可以是以下之一：
		 * <ul>
		 * <li>具体值
		 * <li>值的 {@link Publisher}
		 * <li>任何其他可通过 {@link ReactiveAdapterRegistry} 适配为
		 * {@link Publisher} 的值生产者
		 * </ul>
		 * @param data 负载数据的 Object 值
		 * @return 用于声明预期响应的 spec
		 */
		RetrieveSpec data(Object data);

		/**
		 * {@link #data(Object)} 的变体，还接受关于将要产生的值类型的提示。
		 * 该类型提示用于一次性预先查找兼容的 {@code Encoder}，而不是为每个值查找。
		 * @param producer 负载数据值的来源。必须是 {@link Publisher}，
		 * 或可通过 {@link ReactiveAdapterRegistry} 适配为 {@code Publisher} 的其他生产者
		 * @param elementClass 要产生的值的类型
		 * @return 用于声明预期响应的 spec
		 */
		RetrieveSpec data(Object producer, Class<?> elementClass);

		/**
		 * {@link #data(Object, Class)} 的变体，适用于类型提示必须包含泛型的情况。
		 * 请参阅 {@link ParameterizedTypeReference}。
		 * @param producer 负载数据值的来源。必须是 {@link Publisher}，
		 * 或可通过 {@link ReactiveAdapterRegistry} 适配为 {@code Publisher} 的其他生产者
		 * @param elementTypeRef 要产生的值的类型
		 * @return 用于声明预期响应的 spec
		 */
		RetrieveSpec data(Object producer, ParameterizedTypeReference<?> elementTypeRef);
	}


	/**
	 * 用于提供额外复合元数据条目的规范。
	 *
	 * @param <S> 对 spec 类型的自引用
	 */
	interface MetadataSpec<S extends MetadataSpec<S>> {

		/**
		 * 使用复合元数据时，用于追加额外的元数据条目。
		 * 如果在未使用复合元数据时使用此方法，将抛出
		 * {@link IllegalArgumentException}。
		 * 元数据值可以是具体值，也可以是任何可通过
		 * {@link ReactiveAdapterRegistry} 适配为 {@link Publisher} 的单值生产者。
		 * @param metadata 要用合适的 {@link org.springframework.core.codec.Encoder Encoder} 编码的 Object，
		 * 或一个 {@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
		 * @param mimeType 描述该元数据的 MIME 类型
		 */
		S metadata(Object metadata, MimeType mimeType);
	}


	/**
	 * 用于声明 RSocket 请求预期输出的规范。
	 * @since 5.2.2
	 */
	interface RetrieveSpec {

		/**
		 * 执行 {@link RSocket#fireAndForget fireAndForget}，发送提供的数据和元数据。
		 * @return 指示负载是否成功发送的完成信号。但请注意，这是单向发送，
		 * 无法得知对端是否处理了该事件，也无法得知处理方式。
		 */
		Mono<Void> send();

		/**
		 * 执行 {@link RSocket#requestResponse requestResponse} 交换。
		 * <p>如果返回类型是 {@code Mono<Void>}，则 {@code Mono} 将在所有数据
		 * 被消费后完成。
		 * <p><strong>注意：</strong>如果请求负载是多值 {@link Publisher}，
		 * 此方法会抛出错误，因为 RSocket 没有多对一的交互方式。
		 * @param dataType 响应的预期数据类型
		 * @param <T> 预期数据类型的类型参数
		 * @return 解码后的响应
		 */
		<T> Mono<T> retrieveMono(Class<T> dataType);

		/**
		 * {@link #retrieveMono(Class)} 的变体，适用于 dataType 必须包含泛型的情况。
		 * 请参阅 {@link ParameterizedTypeReference}。
		 */
		<T> Mono<T> retrieveMono(ParameterizedTypeReference<T> dataTypeRef);

		/**
		 * 根据请求输入是单负载还是多负载，执行
		 * {@link RSocket#requestStream requestStream} 或
		 * {@link RSocket#requestChannel requestChannel} 交换。
		 * <p>如果返回类型是 {@code Flux<Void>}，则 {@code Flux} 将在所有数据
		 * 被消费后完成。
		 * @param dataType 响应中值的预期类型
		 * @param <T> 将值的预期类型参数化
		 * @return 解码后的响应
		 */
		<T> Flux<T> retrieveFlux(Class<T> dataType);

		/**
		 * {@link #retrieveFlux(Class)} 的变体，适用于 dataType 必须包含泛型的情况。
		 * 请参阅 {@link ParameterizedTypeReference}。
		 */
		<T> Flux<T> retrieveFlux(ParameterizedTypeReference<T> dataTypeRef);
	}

}
