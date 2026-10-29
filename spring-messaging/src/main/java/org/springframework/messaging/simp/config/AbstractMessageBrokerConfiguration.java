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

package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.converter.JsonbMessageConverter;
import org.springframework.messaging.converter.KotlinSerializationJsonMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * 为处理基于简单消息协议（如 STOMP）的消息提供基本配置。
 *
 * <p>{@link #clientInboundChannel} 和 {@link #clientOutboundChannel} 负责在远程客户端
 * 与多个消息处理器之间传递消息，这些处理器如下所示。
 * <ul>
 * <li>{@link #simpAnnotationMethodMessageHandler}</li>
 * <li>{@link #simpleBrokerMessageHandler}</li>
 * <li>{@link #stompBrokerRelayMessageHandler}</li>
 * <li>{@link #userDestinationMessageHandler}</li>
 * </ul>
 *
 * <p>{@link #brokerChannel} 在应用程序内部向相应的消息处理器传递消息。
 * {@link #brokerMessagingTemplate} 可以注入到任何应用程序组件中用于发送消息。
 *
 * <p>子类负责配置中向客户端入站/出站通道提供消息以及从这些通道接收消息的部分
 * （例如基于 WebSocket 的 STOMP）。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class AbstractMessageBrokerConfiguration implements ApplicationContextAware {

	private static final String MVC_VALIDATOR_NAME = "mvcValidator";

	private static final boolean jackson2Present;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean kotlinSerializationJsonPresent;


	static {
		ClassLoader classLoader = AbstractMessageBrokerConfiguration.class.getClassLoader();
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}


	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ChannelRegistration clientInboundChannelRegistration;

	@Nullable
	private ChannelRegistration clientOutboundChannelRegistration;

	@Nullable
	private MessageBrokerRegistry brokerRegistry;


	/**
	 * 受保护的构造函数。
	 */
	protected AbstractMessageBrokerConfiguration() {
	}


	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Bean
	public AbstractSubscribableChannel clientInboundChannel(
			@Qualifier("clientInboundChannelExecutor") TaskExecutor executor) {

		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(executor);
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		ChannelRegistration reg = getClientInboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public TaskExecutor clientInboundChannelExecutor() {
		TaskExecutorRegistration reg = getClientInboundChannelRegistration().taskExecutor();
		ThreadPoolTaskExecutor executor = reg.getTaskExecutor();
		executor.setThreadNamePrefix("clientInboundChannel-");
		return executor;
	}

	protected final ChannelRegistration getClientInboundChannelRegistration() {
		if (this.clientInboundChannelRegistration == null) {
			ChannelRegistration registration = new ChannelRegistration();
			configureClientInboundChannel(registration);
			registration.interceptors(new ImmutableMessageChannelInterceptor());
			this.clientInboundChannelRegistration = registration;
		}
		return this.clientInboundChannelRegistration;
	}

	/**
	 * 供子类自定义 WebSocket 客户端入站消息所用消息通道的钩子。
	 */
	protected void configureClientInboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel clientOutboundChannel(
			@Qualifier("clientOutboundChannelExecutor") TaskExecutor executor) {

		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(executor);
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		ChannelRegistration reg = getClientOutboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public TaskExecutor clientOutboundChannelExecutor() {
		TaskExecutorRegistration reg = getClientOutboundChannelRegistration().taskExecutor();
		ThreadPoolTaskExecutor executor = reg.getTaskExecutor();
		executor.setThreadNamePrefix("clientOutboundChannel-");
		return executor;
	}

	protected final ChannelRegistration getClientOutboundChannelRegistration() {
		if (this.clientOutboundChannelRegistration == null) {
			ChannelRegistration registration = new ChannelRegistration();
			configureClientOutboundChannel(registration);
			registration.interceptors(new ImmutableMessageChannelInterceptor());
			this.clientOutboundChannelRegistration = registration;
		}
		return this.clientOutboundChannelRegistration;
	}

	/**
	 * 供子类自定义从应用程序或消息代理发往 WebSocket 客户端的消息所用消息通道的钩子。
	 */
	protected void configureClientOutboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel brokerChannel(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			@Qualifier("brokerChannelExecutor") TaskExecutor executor) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		ChannelRegistration registration = registry.getBrokerChannelRegistration();
		ExecutorSubscribableChannel channel = (registration.hasTaskExecutor() ?
				new ExecutorSubscribableChannel(executor) : new ExecutorSubscribableChannel());
		registration.interceptors(new ImmutableMessageChannelInterceptor());
		channel.setLogger(SimpLogging.forLog(channel.getLogger()));
		channel.setInterceptors(registration.getInterceptors());
		return channel;
	}

	@Bean
	public TaskExecutor brokerChannelExecutor(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		ChannelRegistration registration = registry.getBrokerChannelRegistration();
		ThreadPoolTaskExecutor executor;
		if (registration.hasTaskExecutor()) {
			executor = registration.taskExecutor().getTaskExecutor();
		}
		else {
			// Should never be used
			executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(0);
			executor.setMaxPoolSize(1);
			executor.setQueueCapacity(0);
		}
		executor.setThreadNamePrefix("brokerChannel-");
		return executor;
	}

	/**
	 * 用于访问 {@link MessageBrokerRegistry} 的访问器，确保通过
	 * {@link #configureMessageBroker(MessageBrokerRegistry)} 一次性创建并初始化它。
	 */
	protected final MessageBrokerRegistry getBrokerRegistry(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		if (this.brokerRegistry == null) {
			MessageBrokerRegistry registry = new MessageBrokerRegistry(clientInboundChannel, clientOutboundChannel);
			configureMessageBroker(registry);
			this.brokerRegistry = registry;
		}
		return this.brokerRegistry;
	}

	/**
	 * 供子类通过提供的 {@link MessageBrokerRegistry} 实例自定义消息代理配置的钩子。
	 */
	protected void configureMessageBroker(MessageBrokerRegistry registry) {
	}

	/**
	 * 提供对已配置的 PathMatcher 的访问，以便其他配置类使用。
	 */
	@Nullable
	public final PathMatcher getPathMatcher(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		return getBrokerRegistry(clientInboundChannel, clientOutboundChannel).getPathMatcher();
	}

	@Bean
	public SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpMessagingTemplate brokerMessagingTemplate, CompositeMessageConverter brokerMessageConverter) {

		SimpAnnotationMethodMessageHandler handler = createAnnotationMethodMessageHandler(
						clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate);

		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		handler.setDestinationPrefixes(brokerRegistry.getApplicationDestinationPrefixes());
		handler.setMessageConverter(brokerMessageConverter);
		handler.setValidator(simpValidator());

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
		addArgumentResolvers(argumentResolvers);
		handler.setCustomArgumentResolvers(argumentResolvers);

		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();
		addReturnValueHandlers(returnValueHandlers);
		handler.setCustomReturnValueHandlers(returnValueHandlers);

		PathMatcher pathMatcher = brokerRegistry.getPathMatcher();
		if (pathMatcher != null) {
			handler.setPathMatcher(pathMatcher);
		}
		return handler;
	}

	/**
	 * 用于插入自定义
	 * {@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
	 * SimpAnnotationMethodMessageHandler} 子类的受保护方法。
	 * @since 5.3.2
	 */
	protected SimpAnnotationMethodMessageHandler createAnnotationMethodMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpMessagingTemplate brokerMessagingTemplate) {

		return new SimpAnnotationMethodMessageHandler(
				clientInboundChannel, clientOutboundChannel, brokerMessagingTemplate);
	}

	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	@Bean
	@Nullable
	public AbstractBrokerMessageHandler simpleBrokerMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			AbstractSubscribableChannel brokerChannel, UserDestinationResolver userDestinationResolver) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		SimpleBrokerMessageHandler handler = registry.getSimpleBroker(brokerChannel);
		if (handler == null) {
			return null;
		}
		updateUserDestinationResolver(handler, userDestinationResolver, registry.getUserDestinationPrefix());
		return handler;
	}

	private void updateUserDestinationResolver(
			AbstractBrokerMessageHandler handler, UserDestinationResolver userDestinationResolver,
			@Nullable String userDestinationPrefix) {

		Collection<String> prefixes = handler.getDestinationPrefixes();
		if (!prefixes.isEmpty() && !prefixes.iterator().next().startsWith("/")) {
			((DefaultUserDestinationResolver) userDestinationResolver).setRemoveLeadingSlash(true);
		}
		if (StringUtils.hasText(userDestinationPrefix)) {
			handler.setUserDestinationPredicate(destination -> destination.startsWith(userDestinationPrefix));
		}
	}

	@Bean
	@Nullable
	public AbstractBrokerMessageHandler stompBrokerRelayMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			AbstractSubscribableChannel brokerChannel, UserDestinationMessageHandler userDestinationMessageHandler,
			@Nullable MessageHandler userRegistryMessageHandler, UserDestinationResolver userDestinationResolver) {

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		StompBrokerRelayMessageHandler handler = registry.getStompBrokerRelay(brokerChannel);
		if (handler == null) {
			return null;
		}
		Map<String, MessageHandler> subscriptions = new HashMap<>(4);
		String destination = registry.getUserDestinationBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userDestinationMessageHandler);
		}
		destination = registry.getUserRegistryBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userRegistryMessageHandler);
		}
		handler.setSystemSubscriptions(subscriptions);
		updateUserDestinationResolver(handler, userDestinationResolver, registry.getUserDestinationPrefix());
		return handler;
	}

	@Bean
	public UserDestinationMessageHandler userDestinationMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			AbstractSubscribableChannel brokerChannel, UserDestinationResolver userDestinationResolver) {

		UserDestinationMessageHandler handler =
				new UserDestinationMessageHandler(clientInboundChannel, brokerChannel, userDestinationResolver);

		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		String destination = registry.getUserDestinationBroadcast();
		if (destination != null) {
			handler.setBroadcastDestination(destination);
		}
		return handler;
	}

	@Bean
	@Nullable
	public MessageHandler userRegistryMessageHandler(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel,
			SimpUserRegistry userRegistry, SimpMessagingTemplate brokerMessagingTemplate,
			@Qualifier("messageBrokerTaskScheduler") TaskScheduler scheduler) {

		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		if (brokerRegistry.getUserRegistryBroadcast() == null) {
			return null;
		}
		Assert.isInstanceOf(MultiServerUserRegistry.class, userRegistry, "MultiServerUserRegistry required");
		return new UserRegistryMessageHandler((MultiServerUserRegistry) userRegistry,
				brokerMessagingTemplate, brokerRegistry.getUserRegistryBroadcast(),
				scheduler);
	}

	// Expose alias for 4.1 compatibility
	@Bean(name = {"messageBrokerTaskScheduler", "messageBrokerSockJsTaskScheduler"})
	public TaskScheduler messageBrokerTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("MessageBroker-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	@Bean
	public SimpMessagingTemplate brokerMessagingTemplate(
			AbstractSubscribableChannel brokerChannel, AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel, CompositeMessageConverter brokerMessageConverter) {

		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel);
		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		String prefix = registry.getUserDestinationPrefix();
		if (prefix != null) {
			template.setUserDestinationPrefix(prefix);
		}
		template.setMessageConverter(brokerMessageConverter);
		return template;
	}

	@Bean
	public CompositeMessageConverter brokerMessageConverter() {
		List<MessageConverter> converters = new ArrayList<>();
		boolean registerDefaults = configureMessageConverters(converters);
		if (registerDefaults) {
			converters.add(new StringMessageConverter());
			converters.add(new ByteArrayMessageConverter());
			if (kotlinSerializationJsonPresent) {
				converters.add(new KotlinSerializationJsonMessageConverter());
			}
			if (jackson2Present) {
				converters.add(createJacksonConverter());
			}
			else if (gsonPresent) {
				converters.add(new GsonMessageConverter());
			}
			else if (jsonbPresent) {
				converters.add(new JsonbMessageConverter());
			}
		}
		return new CompositeMessageConverter(converters);
	}

	protected MappingJackson2MessageConverter createJacksonConverter() {
		DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
		resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setContentTypeResolver(resolver);
		return converter;
	}

	/**
	 * 重写此方法以添加自定义消息转换器。
	 * @param messageConverters the list to add converters to, initially empty
	 * @return {@code true} if default message converters should be added to list,
	 * {@code false} if no more converters should be added
	 */
	protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		return true;
	}

	@Bean
	public UserDestinationResolver userDestinationResolver(
			SimpUserRegistry userRegistry, AbstractSubscribableChannel clientInboundChannel,
			AbstractSubscribableChannel clientOutboundChannel) {

		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(userRegistry);
		MessageBrokerRegistry registry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		String prefix = registry.getUserDestinationPrefix();
		if (prefix != null) {
			resolver.setUserDestinationPrefix(prefix);
		}
		return resolver;
	}

	@Bean
	@SuppressWarnings("deprecation")
	public SimpUserRegistry userRegistry(
			AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {

		SimpUserRegistry userRegistry = createLocalUserRegistry();
		MessageBrokerRegistry brokerRegistry = getBrokerRegistry(clientInboundChannel, clientOutboundChannel);
		if (userRegistry == null) {
			userRegistry = createLocalUserRegistry(brokerRegistry.getUserRegistryOrder());
		}
		boolean broadcast = brokerRegistry.getUserRegistryBroadcast() != null;
		return (broadcast ? new MultiServerUserRegistry(userRegistry) : userRegistry);
	}

	/**
	 * 创建提供本地用户访问的用户注册表。
	 * @deprecated as of 5.1 in favor of {@link #createLocalUserRegistry(Integer)}
	 */
	@Deprecated
	@Nullable
	protected SimpUserRegistry createLocalUserRegistry() {
		return null;
	}

	/**
	 * 创建提供本地用户访问的用户注册表。
	 * @param order the order to use as a {@link SmartApplicationListener}.
	 * @since 5.1
	 */
	protected abstract SimpUserRegistry createLocalUserRegistry(@Nullable Integer order);

	/**
	 * 返回用于校验 {@code @Payload} 方法参数的
	 * {@link org.springframework.validation.Validator} 实例。
	 * <p>此方法按以下顺序尝试获取 Validator 实例：
	 * <ul>
	 * <li>首先委托给 getValidator()</li>
	 * <li>如果没有返回，则获取名称为 "mvcValidator" 的现有实例，
	 * 该实例由 MVC 配置创建</li>
	 * <li>如果没有返回，则在创建 {@code OptionalValidatorFactoryBean} 之前
	 * 检查类路径中是否存在 JSR-303 实现</li>
	 * <li>返回一个空操作的 Validator 实例</li>
	 * </ul>
	 */
	protected Validator simpValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (this.applicationContext != null && this.applicationContext.containsBean(MVC_VALIDATOR_NAME)) {
				validator = this.applicationContext.getBean(MVC_VALIDATOR_NAME, Validator.class);
			}
			else if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, AbstractMessageBrokerConfiguration.class.getClassLoader());
				}
				catch (Throwable ex) {
					throw new BeanInitializationException("Could not find default validator class", ex);
				}
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			}
			else {
				validator = new Validator() {
					@Override
					public boolean supports(Class<?> clazz) {
						return false;
					}
					@Override
					public void validate(@Nullable Object target, Errors errors) {
					}
				};
			}
		}
		return validator;
	}

	/**
	 * 重写此方法以提供自定义的 {@link Validator}。
	 * @since 4.0.1
	 */
	@Nullable
	public Validator getValidator() {
		return null;
	}

}
