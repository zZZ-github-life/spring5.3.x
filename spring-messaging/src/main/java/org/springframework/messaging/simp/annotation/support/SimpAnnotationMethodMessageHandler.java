/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.CompletableFutureReturnValueHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandlerComposite;
import org.springframework.messaging.handler.invocation.ListenableFutureReturnValueHandler;
import org.springframework.messaging.handler.invocation.ReactiveReturnValueHandler;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageMappingInfo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageTypeMessageCondition;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringValueResolver;
import org.springframework.validation.Validator;

/**
 * 将消息委托给使用 {@link MessageMapping @MessageMapping}
 * 和 {@link SubscribeMapping @SubscribeMapping} 注解的方法的处理器。
 *
 * <p>支持带模板变量的 Ant 风格路径模式。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpAnnotationMethodMessageHandler extends AbstractMethodMessageHandler<SimpMessageMappingInfo>
		implements EmbeddedValueResolverAware, SmartLifecycle {

	private static final boolean reactorPresent = ClassUtils.isPresent(
			"reactor.core.publisher.Flux", SimpAnnotationMethodMessageHandler.class.getClassLoader());


	private final SubscribableChannel clientInboundChannel;

	private final SimpMessageSendingOperations clientMessagingTemplate;

	private final SimpMessageSendingOperations brokerTemplate;

	private MessageConverter messageConverter;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private boolean slashPathSeparator = true;

	@Nullable
	private Validator validator;

	@Nullable
	private StringValueResolver valueResolver;

	@Nullable
	private MessageHeaderInitializer headerInitializer;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * 使用给定消息通道和代理消息模板创建 SimpAnnotationMethodMessageHandler 实例。
	 * @param clientInboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param clientOutboundChannel the channel for messages to clients (e.g. WebSocket clients)
	 * @param brokerTemplate a messaging template to send application messages to the broker
	 */
	public SimpAnnotationMethodMessageHandler(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, SimpMessageSendingOperations brokerTemplate) {

		Assert.notNull(clientInboundChannel, "clientInboundChannel must not be null");
		Assert.notNull(clientOutboundChannel, "clientOutboundChannel must not be null");
		Assert.notNull(brokerTemplate, "brokerTemplate must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.clientMessagingTemplate = new SimpMessagingTemplate(clientOutboundChannel);
		this.brokerTemplate = brokerTemplate;

		Collection<MessageConverter> converters = new ArrayList<>();
		converters.add(new StringMessageConverter());
		converters.add(new ByteArrayMessageConverter());
		this.messageConverter = new CompositeMessageConverter(converters);
	}


	/**
	 * {@inheritDoc}
	 * <p>目标前缀应为以斜杠分隔的 String，因此在缺失斜杠时会自动追加斜杠，
	 * 以确保基于前缀的正确匹配（即匹配完整的段）。
	 * <p>不过请注意，前缀之后的目标剩余部分可能会使用不同的分隔符
	 * （例如消息传递中常用的 "."），具体取决于配置的 {@code PathMatcher}。
	 */
	@Override
	public void setDestinationPrefixes(@Nullable Collection<String> prefixes) {
		super.setDestinationPrefixes(appendSlashes(prefixes));
	}

	@Nullable
	private static Collection<String> appendSlashes(@Nullable Collection<String> prefixes) {
		if (CollectionUtils.isEmpty(prefixes)) {
			return prefixes;
		}
		Collection<String> result = new ArrayList<>(prefixes.size());
		for (String prefix : prefixes) {
			if (!prefix.endsWith("/")) {
				prefix = prefix + "/";
			}
			result.add(prefix);
		}
		return result;
	}

	/**
	 * 配置 {@link MessageConverter}，用于将具有特定 MIME 类型的序列化形式的消息载荷
	 * 转换为与目标方法参数匹配的 Object。在向消息代理发送消息时也会使用该转换器。
	 * @see CompositeMessageConverter
	 */
	public void setMessageConverter(MessageConverter converter) {
		this.messageConverter = converter;
		((AbstractMessageSendingTemplate<?>) this.clientMessagingTemplate).setMessageConverter(converter);
	}

	/**
	 * 返回已配置的 {@link MessageConverter}。
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 配置在解析方法参数（例如消息头值）时使用的 {@link ConversionService}。
	 * <p>默认使用 {@link DefaultFormattingConversionService}。
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回已配置的 {@link ConversionService}。
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 设置用于将目标与已配置目标模式进行匹配的 PathMatcher 实现。
	 * <p>默认使用 {@link AntPathMatcher}。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		this.slashPathSeparator = this.pathMatcher.combine("a", "a").equals("a/a");
	}

	/**
	 * 返回用于匹配目标的 PathMatcher 实现。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 返回已配置的 Validator 实例。
	 */
	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * 设置用于校验 {@code @Payload} 参数的 Validator 实例。
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadMethodArgumentResolver
	 */
	public void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.valueResolver = resolver;
	}

	/**
	 * 配置 {@link MessageHeaderInitializer}，并传递给由控制器返回值发送消息的
	 * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}。
	 * <p>默认情况下，不设置此属性。
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回已配置的头初始化器。
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.clientInboundChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientInboundChannel.unsubscribe(this);
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		return this.running;
	}


	@Override
	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		ApplicationContext context = getApplicationContext();
		ConfigurableBeanFactory beanFactory = (context instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) context).getBeanFactory() : null);

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());
		resolvers.add(new DestinationVariableMethodArgumentResolver(this.conversionService));

		// Type-based argument resolution
		resolvers.add(new PrincipalMethodArgumentResolver());
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

		resolvers.addAll(getCustomArgumentResolvers());
		resolvers.add(new PayloadMethodArgumentResolver(this.messageConverter, this.validator));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();

		// Single-purpose return value types

		handlers.add(new ListenableFutureReturnValueHandler());
		handlers.add(new CompletableFutureReturnValueHandler());
		if (reactorPresent) {
			handlers.add(new ReactiveReturnValueHandler());
		}

		// Annotation-based return value types

		SendToMethodReturnValueHandler sendToHandler =
				new SendToMethodReturnValueHandler(this.brokerTemplate, true);
		sendToHandler.setHeaderInitializer(this.headerInitializer);
		handlers.add(sendToHandler);

		SubscriptionMethodReturnValueHandler subscriptionHandler =
				new SubscriptionMethodReturnValueHandler(this.clientMessagingTemplate);
		subscriptionHandler.setHeaderInitializer(this.headerInitializer);
		handlers.add(subscriptionHandler);

		// Custom return value types

		handlers.addAll(getCustomReturnValueHandlers());

		// Catch-all

		sendToHandler = new SendToMethodReturnValueHandler(this.brokerTemplate, false);
		sendToHandler.setHeaderInitializer(this.headerInitializer);
		handlers.add(sendToHandler);

		return handlers;
	}

	@Override
	protected Log getReturnValueHandlerLogger() {
		return SimpLogging.forLog(HandlerMethodReturnValueHandlerComposite.defaultLogger);
	}

	@Override
	protected Log getHandlerMethodLogger() {
		return SimpLogging.forLog(HandlerMethod.defaultLogger);
	}


	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
	}

	@Override
	@Nullable
	protected SimpMessageMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		MessageMapping messageAnn = AnnotatedElementUtils.findMergedAnnotation(method, MessageMapping.class);
		if (messageAnn != null) {
			MessageMapping typeAnn = AnnotatedElementUtils.findMergedAnnotation(handlerType, MessageMapping.class);
			// Only actually register it if there are destinations specified;
			// otherwise @MessageMapping is just being used as a (meta-annotation) marker.
			if (messageAnn.value().length > 0 || (typeAnn != null && typeAnn.value().length > 0)) {
				SimpMessageMappingInfo result = createMessageMappingCondition(messageAnn.value());
				if (typeAnn != null) {
					result = createMessageMappingCondition(typeAnn.value()).combine(result);
				}
				return result;
			}
		}

		SubscribeMapping subscribeAnn = AnnotatedElementUtils.findMergedAnnotation(method, SubscribeMapping.class);
		if (subscribeAnn != null) {
			MessageMapping typeAnn = AnnotatedElementUtils.findMergedAnnotation(handlerType, MessageMapping.class);
			// Only actually register it if there are destinations specified;
			// otherwise @SubscribeMapping is just being used as a (meta-annotation) marker.
			if (subscribeAnn.value().length > 0 || (typeAnn != null && typeAnn.value().length > 0)) {
				SimpMessageMappingInfo result = createSubscribeMappingCondition(subscribeAnn.value());
				if (typeAnn != null) {
					result = createMessageMappingCondition(typeAnn.value()).combine(result);
				}
				return result;
			}
		}

		return null;
	}

	private SimpMessageMappingInfo createMessageMappingCondition(String[] destinations) {
		String[] resolvedDestinations = resolveEmbeddedValuesInDestinations(destinations);
		return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.MESSAGE,
				new DestinationPatternsMessageCondition(resolvedDestinations, this.pathMatcher));
	}

	private SimpMessageMappingInfo createSubscribeMappingCondition(String[] destinations) {
		String[] resolvedDestinations = resolveEmbeddedValuesInDestinations(destinations);
		return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.SUBSCRIBE,
				new DestinationPatternsMessageCondition(resolvedDestinations, this.pathMatcher));
	}

	/**
	 * 解析给定目标数组中的占位符值。
	 * @return a new array with updated destinations
	 * @since 4.2
	 */
	protected String[] resolveEmbeddedValuesInDestinations(String[] destinations) {
		if (this.valueResolver == null) {
			return destinations;
		}
		String[] result = new String[destinations.length];
		for (int i = 0; i < destinations.length; i++) {
			result[i] = this.valueResolver.resolveStringValue(destinations[i]);
		}
		return result;
	}

	@Override
	protected Set<String> getDirectLookupDestinations(SimpMessageMappingInfo mapping) {
		Set<String> result = new LinkedHashSet<>();
		for (String pattern : mapping.getDestinationConditions().getPatterns()) {
			if (!this.pathMatcher.isPattern(pattern)) {
				result.add(pattern);
			}
		}
		return result;
	}

	@Override
	@Nullable
	protected String getDestination(Message<?> message) {
		return SimpMessageHeaderAccessor.getDestination(message.getHeaders());
	}

	@Override
	protected String getLookupDestination(@Nullable String destination) {
		if (destination == null) {
			return null;
		}
		if (CollectionUtils.isEmpty(getDestinationPrefixes())) {
			return destination;
		}
		for (String prefix : getDestinationPrefixes()) {
			if (destination.startsWith(prefix)) {
				if (this.slashPathSeparator) {
					return destination.substring(prefix.length() - 1);
				}
				else {
					return destination.substring(prefix.length());
				}
			}
		}
		return null;
	}

	@Override
	@Nullable
	protected SimpMessageMappingInfo getMatchingMapping(SimpMessageMappingInfo mapping, Message<?> message) {
		return mapping.getMatchingCondition(message);

	}

	@Override
	protected Comparator<SimpMessageMappingInfo> getMappingComparator(final Message<?> message) {
		return (info1, info2) -> info1.compareTo(info2, message);
	}

	@Override
	protected void handleMatch(SimpMessageMappingInfo mapping, HandlerMethod handlerMethod,
			String lookupDestination, Message<?> message) {

		Set<String> patterns = mapping.getDestinationConditions().getPatterns();
		if (!CollectionUtils.isEmpty(patterns)) {
			String pattern = patterns.iterator().next();
			Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(pattern, lookupDestination);
			if (!CollectionUtils.isEmpty(vars)) {
				MessageHeaderAccessor mha = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
				Assert.state(mha != null && mha.isMutable(), "Mutable MessageHeaderAccessor required");
				mha.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
			}
		}

		try {
			SimpAttributesContextHolder.setAttributesFromMessage(message);
			super.handleMatch(mapping, handlerMethod, lookupDestination, message);
		}
		finally {
			SimpAttributesContextHolder.resetAttributes();
		}
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

}
