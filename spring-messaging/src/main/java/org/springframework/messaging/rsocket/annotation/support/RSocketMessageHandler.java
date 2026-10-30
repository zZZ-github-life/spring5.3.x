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

package org.springframework.messaging.rsocket.annotation.support;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.frame.FrameType;
import io.rsocket.metadata.WellKnownMimeType;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.reactive.MessageMappingMessageHandler;
import org.springframework.messaging.handler.annotation.reactive.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link MessageMappingMessageHandler} 的扩展，用于通过
 * {@link ConnectMapping @ConnectMapping} 和
 * {@link MessageMapping @MessageMapping} 方法处理 RSocket 请求。
 *
 * <p>在服务器场景中，此类可以在 Spring 配置中声明为 bean，这样会检测
 * {@code @Controller} bean 中的 {@code @MessageMapping} 方法。
 * 可以通过 {@link #setHandlerPredicate(Predicate) handlerPredicate}
 * 更改检查哪些 bean。给定此类的实例后，可以使用 {@link #responder()}
 * 获取 {@link SocketAcceptor} 适配器，并注册到
 * {@link io.rsocket.core.RSocketServer}。
 *
 * <p>对于客户端（可能与服务器在同一进程中），可以考虑使用静态工厂方法
 * {@link #responder(RSocketStrategies, Object...)} 获取客户端响应方，
 * 并通过
 * {@link org.springframework.messaging.rsocket.RSocketRequester.Builder#rsocketConnector
 * RSocketRequester.Builder} 注册。
 *
 * <p>对于 {@code @MessageMapping} 方法，此类会根据方法的输入和输出基数
 * 自动确定 RSocket 交互类型。详情请参阅 Spring Framework 参考文档中的
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#rsocket-annot-responders">
 * "Annotated Responders"</a> 一节。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketMessageHandler extends MessageMappingMessageHandler {

	private final List<Encoder<?>> encoders = new ArrayList<>();

	private RSocketStrategies strategies = RSocketStrategies.create();

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType = MimeTypeUtils.parseMimeType(
			WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());


	public RSocketMessageHandler() {
		setRSocketStrategies(this.strategies);
	}


	/**
	 * 配置用于编码处理器方法返回值的编码器。
	 * <p>当设置了 {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * 时，此属性会使用其中的编码器重新初始化；同样，设置此属性时，
	 * {@code RSocketStrategies} 也会被修改以更改其中的编码器。
	 * <p>默认情况下，此项设置为 {@code RSocketStrategies} 中的
	 * {@linkplain org.springframework.messaging.rsocket.RSocketStrategies.Builder#encoder(Encoder[]) defaults}。
	 */
	public void setEncoders(List<? extends Encoder<?>> encoders) {
		this.encoders.clear();
		this.encoders.addAll(encoders);
		this.strategies = this.strategies.mutate()
				.encoders(list -> {
					list.clear();
					list.addAll(encoders);
				})
				.build();
	}

	/**
	 * 返回配置的 {@link #setEncoders(List) encoders}。
	 */
	public List<? extends Encoder<?>> getEncoders() {
		return this.encoders;
	}

	/**
	 * {@inheritDoc}
	 * <p>当设置了 {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * 时，此属性会使用其中的解码器重新初始化；同样，设置此属性时，
	 * {@code RSocketStrategies} 也会被修改以更改其中的解码器。
	 * <p>默认情况下，此项设置为 {@code RSocketStrategies} 中的
	 * {@linkplain org.springframework.messaging.rsocket.RSocketStrategies.Builder#decoder(Decoder[]) defaults}。
	 */
	@Override
	public void setDecoders(List<? extends Decoder<?>> decoders) {
		super.setDecoders(decoders);
		this.strategies = this.strategies.mutate()
				.decoders(list -> {
					list.clear();
					list.addAll(decoders);
				})
				.build();
	}

	/**
	 * {@inheritDoc}
	 * <p>当设置了 {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * 时，此属性会使用其中的路由匹配器重新初始化；同样，设置此属性时，
	 * {@code RSocketStrategies} 也会被修改以更改其中的匹配器。
	 * <p>默认情况下，此项设置为 {@code RSocketStrategies} 中的
	 * {@linkplain org.springframework.messaging.rsocket.RSocketStrategies.Builder#routeMatcher(RouteMatcher) defaults}。
	 */
	@Override
	public void setRouteMatcher(@Nullable RouteMatcher routeMatcher) {
		super.setRouteMatcher(routeMatcher);
		this.strategies = this.strategies.mutate().routeMatcher(routeMatcher).build();
	}

	/**
	 * 配置用于适配各种响应式类型的注册表。
	 * <p>当设置了 {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * 时，此属性会使用其中的注册表重新初始化；同样，设置此属性时，
	 * {@code RSocketStrategies} 也会被修改以更改其中的注册表。
	 * <p>默认情况下，此项设置为 {@code RSocketStrategies} 中的
	 * {@link org.springframework.messaging.rsocket.RSocketStrategies.Builder#reactiveAdapterStrategy(ReactiveAdapterRegistry) defaults}。
	 */
	@Override
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		super.setReactiveAdapterRegistry(registry);
		this.strategies = this.strategies.mutate().reactiveAdapterStrategy(registry).build();
	}

	/**
	 * 配置 {@link MetadataExtractor}，以提取路由以及其他元数据。
	 * <p>当设置了 {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * 时，此属性会使用其中的提取器重新初始化；同样，设置此属性时，
	 * {@code RSocketStrategies} 也会被修改以更改其中的提取器。
	 * <p>默认情况下，此项设置为 {@code RSocketStrategies} 中的
	 * {@link org.springframework.messaging.rsocket.RSocketStrategies.Builder#metadataExtractor(MetadataExtractor) defaults}。
	 * @param extractor 要使用的提取器
	 */
	public void setMetadataExtractor(MetadataExtractor extractor) {
		this.strategies = this.strategies.mutate().metadataExtractor(extractor).build();
	}

	/**
	 * 返回配置的 {@link #setMetadataExtractor MetadataExtractor}。
	 */
	public MetadataExtractor getMetadataExtractor() {
		return this.strategies.metadataExtractor();
	}

	/**
	 * 通过 {@link RSocketStrategies} 实例配置此处理器，该实例可复用于初始化
	 * 客户端 {@link RSocketRequester}。
	 * <p>设置此属性时，会依次设置以下内容：
	 * <ul>
	 * <li>{@link #setDecoders(List)}
	 * <li>{@link #setEncoders(List)}
	 * <li>{@link #setRouteMatcher(RouteMatcher)}
	 * <li>{@link #setMetadataExtractor(MetadataExtractor)}
	 * <li>{@link #setReactiveAdapterRegistry(ReactiveAdapterRegistry)}
	 * </ul>
	 * <p>默认情况下设置为 {@link RSocketStrategies#create()}，它会依次为所有
	 * 相关属性设置默认值。
	 */
	public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
		this.strategies = rsocketStrategies;
		this.encoders.clear();
		this.encoders.addAll(this.strategies.encoders());
		super.setDecoders(this.strategies.decoders());
		super.setRouteMatcher(this.strategies.routeMatcher());
		super.setReactiveAdapterRegistry(this.strategies.reactiveAdapterRegistry());
	}

	/**
	 * 返回 {@link #setRSocketStrategies configured} 的 {@code RSocketStrategies}。
	 */
	public RSocketStrategies getRSocketStrategies() {
		return this.strategies;
	}

	/**
	 * 配置当 {@code SETUP} 帧未指定数据 MimeType 时用于数据负载的默认内容类型。
	 * <p>默认情况下不设置此项。
	 * @param mimeType 要使用的 MimeType
	 */
	public void setDefaultDataMimeType(@Nullable MimeType mimeType) {
		this.defaultDataMimeType = mimeType;
	}

	/**
	 * 返回配置的
	 * {@link #setDefaultDataMimeType defaultDataMimeType}，或 {@code null}。
	 */
	@Nullable
	public MimeType getDefaultDataMimeType() {
		return this.defaultDataMimeType;
	}

	/**
	 * 配置当 {@code SETUP} 帧未指定时用于负载数据的默认 {@code MimeType}。
	 * <p>默认情况下此项设置为 {@code "message/x.rsocket.composite-metadata.v0"}
	 * @param mimeType 要使用的 MimeType
	 */
	public void setDefaultMetadataMimeType(MimeType mimeType) {
		Assert.notNull(mimeType, "'metadataMimeType' is required");
		this.defaultMetadataMimeType = mimeType;
	}

	/**
	 * 返回配置的
	 * {@link #setDefaultMetadataMimeType defaultMetadataMimeType}。
	 */
	public MimeType getDefaultMetadataMimeType() {
		return this.defaultMetadataMimeType;
	}


	@Override
	public void afterPropertiesSet() {

		// Add argument resolver before parent initializes argument resolution
		getArgumentResolverConfigurer().addCustomResolver(new RSocketRequesterMethodArgumentResolver());

		super.afterPropertiesSet();

		getHandlerMethods().forEach((composite, handler) -> {
			if (composite.getMessageConditions().contains(RSocketFrameTypeMessageCondition.CONNECT_CONDITION)) {
				MethodParameter returnType = handler.getReturnType();
				if (getCardinality(returnType) > 0) {
					throw new IllegalStateException(
							"Invalid @ConnectMapping method. " +
									"Return type must be void or a void async type: " + handler);
				}
			}
		});
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
		handlers.add(new RSocketPayloadReturnValueHandler(this.encoders, getReactiveAdapterRegistry()));
		handlers.addAll(getReturnValueHandlerConfigurer().getCustomHandlers());
		return handlers;
	}


	@Override
	@Nullable
	protected CompositeMessageCondition getCondition(AnnotatedElement element) {
		MessageMapping ann1 = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
		if (ann1 != null && ann1.value().length > 0) {
			return new CompositeMessageCondition(
					RSocketFrameTypeMessageCondition.EMPTY_CONDITION,
					new DestinationPatternsMessageCondition(processDestinations(ann1.value()), obtainRouteMatcher()));
		}
		ConnectMapping ann2 = AnnotatedElementUtils.findMergedAnnotation(element, ConnectMapping.class);
		if (ann2 != null) {
			String[] patterns = processDestinations(ann2.value());
			return new CompositeMessageCondition(
					RSocketFrameTypeMessageCondition.CONNECT_CONDITION,
					new DestinationPatternsMessageCondition(patterns, obtainRouteMatcher()));
		}
		return null;
	}

	@Override
	protected CompositeMessageCondition extendMapping(CompositeMessageCondition composite, HandlerMethod handler) {

		List<MessageCondition<?>> conditions = composite.getMessageConditions();
		Assert.isTrue(conditions.size() == 2 &&
						conditions.get(0) instanceof RSocketFrameTypeMessageCondition &&
						conditions.get(1) instanceof DestinationPatternsMessageCondition,
				"Unexpected message condition types");

		if (conditions.get(0) != RSocketFrameTypeMessageCondition.EMPTY_CONDITION) {
			return composite;
		}

		int responseCardinality = getCardinality(handler.getReturnType());
		int requestCardinality = 0;
		for (MethodParameter parameter : handler.getMethodParameters()) {
			if (getArgumentResolvers().getArgumentResolver(parameter) instanceof PayloadMethodArgumentResolver) {
				requestCardinality = getCardinality(parameter);
			}
		}

		return new CompositeMessageCondition(
				RSocketFrameTypeMessageCondition.getCondition(requestCardinality, responseCardinality),
				conditions.get(1));
	}

	private int getCardinality(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		ReactiveAdapter adapter = getReactiveAdapterRegistry().getAdapter(clazz);
		if (adapter == null) {
			return clazz.equals(void.class) ? 0 : 1;
		}
		else if (parameter.nested().getNestedParameterType().equals(Void.class)) {
			return 0;
		}
		else {
			return adapter.isMultiValue() ? 2 : 1;
		}
	}

	@Override
	protected void handleNoMatch(@Nullable RouteMatcher.Route destination, Message<?> message) {
		FrameType frameType = RSocketFrameTypeMessageCondition.getFrameType(message);
		if (frameType == FrameType.SETUP || frameType == FrameType.METADATA_PUSH) {
			return;  // optional handling
		}
		if (frameType == FrameType.REQUEST_FNF) {
			// Can't propagate error to client, so just log
			logger.warn("No handler for fireAndForget to '" + destination + "'");
			return;
		}

		Set<FrameType> frameTypes = getHandlerMethods().keySet().stream()
				.map(CompositeMessageCondition::getMessageConditions)
				.filter(conditions -> conditions.get(1).getMatchingCondition(message) != null)
				.map(conditions -> (RSocketFrameTypeMessageCondition) conditions.get(0))
				.flatMap(condition -> condition.getFrameTypes().stream())
				.collect(Collectors.toSet());

		throw new MessageDeliveryException(frameTypes.isEmpty() ?
				"No handler for destination '" + destination + "'" :
				"Destination '" + destination + "' does not support " + frameType + ". " +
						"Supported interaction(s): " + frameTypes);
	}

	/**
	 * 返回由本 {@code RSocketMessageHandler} 实例支持的 RSocket {@link SocketAcceptor}，
	 * 它可以作为
	 * {@link io.rsocket.core.RSocketConnector#acceptor(SocketAcceptor) 客户端}或
	 * {@link io.rsocket.core.RSocketServer#acceptor(SocketAcceptor) 服务器}
	 * RSocket 响应方接入。
	 * <p>初始 {@link ConnectionSetupPayload} 通过
	 * {@link ConnectMapping @ConnectionMapping} 方法处理，这些方法可以是异步的，
	 * 并返回 {@code Mono<Void>}，通过错误信号可阻止连接建立。此类方法也可以
	 * 向客户端发起请求，但必须与处理过程解耦，并且不能在当前线程上执行。
	 * <p>连接上后续的请求可以通过
	 * {@link MessageMapping MessageMapping} 方法处理。
	 */
	public SocketAcceptor responder() {
		return (setupPayload, sendingRSocket) -> {
			MessagingRSocket responder;
			try {
				responder = createResponder(setupPayload, sendingRSocket);
			}
			catch (Throwable ex) {
				return Mono.error(ex);
			}
			return responder.handleConnectionSetupPayload(setupPayload).then(Mono.just(responder));
		};
	}

	private MessagingRSocket createResponder(ConnectionSetupPayload setupPayload, RSocket rsocket) {
		String str = setupPayload.dataMimeType();
		MimeType dataMimeType = StringUtils.hasText(str) ? MimeTypeUtils.parseMimeType(str) : this.defaultDataMimeType;
		Assert.notNull(dataMimeType, "No `dataMimeType` in ConnectionSetupPayload and no default value");
		Assert.isTrue(isDataMimeTypeSupported(dataMimeType), () -> "Data MimeType '" + dataMimeType + "' not supported");

		str = setupPayload.metadataMimeType();
		MimeType metaMimeType = StringUtils.hasText(str) ? MimeTypeUtils.parseMimeType(str) : this.defaultMetadataMimeType;
		Assert.notNull(metaMimeType, "No `metadataMimeType` in ConnectionSetupPayload and no default value");

		RSocketRequester requester = RSocketRequester.wrap(rsocket, dataMimeType, metaMimeType, this.strategies);
		return new MessagingRSocket(dataMimeType, metaMimeType, getMetadataExtractor(),
				requester, this, obtainRouteMatcher(), this.strategies);
	}

	private boolean isDataMimeTypeSupported(MimeType dataMimeType) {
		for (Encoder<?> encoder : getEncoders()) {
			for (MimeType encodable : encoder.getEncodableMimeTypes()) {
				if (encodable.isCompatibleWith(dataMimeType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 静态工厂方法，用于创建由带注解方法的处理器支持的 RSocket {@link SocketAcceptor}。
	 * 实际上是以下代码的快捷方式：
	 * <pre class="code">
	 * RSocketMessageHandler handler = new RSocketMessageHandler();
	 * handler.setHandlers(handlers);
	 * handler.setRSocketStrategies(strategies);
	 * handler.afterPropertiesSet();
	 *
	 * SocketAcceptor acceptor = handler.responder();
	 * </pre>
	 * <p>此方法旨在以编程方式创建和注册客户端响应方。例如：
	 * <pre class="code">
	 * SocketAcceptor responder =
	 *         RSocketMessageHandler.responder(strategies, new ClientHandler());
	 *
	 * RSocketRequester.builder()
	 *         .rsocketConnector(connector -&gt; connector.acceptor(responder))
	 *         .connectTcp("localhost", server.address().getPort());
	 * </pre>
	 *
	 * <p>请注意，给定的处理器不需要具有 {@code @Controller} 等构造型注解，
	 * 这有助于避免与同一应用程序中可能使用的服务器端处理器发生重叠。
	 * 不过，对于更高级的场景（例如通过自定义构造型注解发现处理器），
	 * 请考虑将 {@code RSocketMessageHandler} 声明为 bean，然后从中获取响应方。
	 * @param strategies 要设置在创建的 {@code RSocketMessageHandler} 上的策略
	 * @param candidateHandlers 包含带注解处理器方法的 Object 和/或 Class 列表；
	 * 会在创建的 {@code RSocketMessageHandler} 上用它调用 {@link #setHandlers(List)}
	 * @return 可传入 {@link org.springframework.messaging.rsocket.RSocketRequester.Builder#rsocketConnector} 的配置器
	 * @since 5.2.6
	 */
	public static SocketAcceptor responder(RSocketStrategies strategies, Object... candidateHandlers) {
		Assert.notEmpty(candidateHandlers, "No handlers");
		List<Object> handlers = new ArrayList<>(candidateHandlers.length);
		for (Object obj : candidateHandlers) {
			handlers.add(obj instanceof Class ? BeanUtils.instantiateClass((Class<?>) obj) : obj);
		}
		RSocketMessageHandler handler = new RSocketMessageHandler();
		handler.setHandlers(handlers);
		handler.setRSocketStrategies(strategies);
		handler.afterPropertiesSet();
		return handler.responder();
	}

}
