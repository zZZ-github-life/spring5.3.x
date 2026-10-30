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

package org.springframework.messaging.handler.annotation.reactive;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.reactive.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;
import org.springframework.util.StringValueResolver;
import org.springframework.validation.Validator;

/**
 * {@link AbstractMethodMessageHandler} 的扩展，用于通过
 * {@link MessageMapping @MessageMapping} 方法以响应式、非阻塞方式处理消息。
 * 默认情况下，会在 {@code @Controller} Spring Bean 中检测此类方法，
 * 但可以通过 {@link #setHandlerPredicate(Predicate)} 更改。
 *
 * <p>传入消息的载荷通过配置的 {@link #setDecoders(List)} 解码器，
 * 借助 {@link PayloadMethodArgumentResolver} 进行解码。
 *
 * <p>返回值没有默认处理方式，但可以使用
 * {@link #setReturnValueHandlerConfigurer} 配置自定义返回值处理器。
 * 子类也可以重写 {@link #initReturnValueHandlers()} 来设置默认返回值处理器。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see AbstractEncoderMethodReturnValueHandler
 */
public class MessageMappingMessageHandler extends AbstractMethodMessageHandler<CompositeMessageCondition>
		implements EmbeddedValueResolverAware {

	private final List<Decoder<?>> decoders = new ArrayList<>();

	@Nullable
	private Validator validator;

	@Nullable
	private RouteMatcher routeMatcher;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	@Nullable
	private StringValueResolver valueResolver;


	public MessageMappingMessageHandler() {
		setHandlerPredicate(type -> AnnotatedElementUtils.hasAnnotation(type, Controller.class));
	}


	/**
	 * 配置用于传入载荷的解码器。
	 */
	public void setDecoders(List<? extends Decoder<?>> decoders) {
		this.decoders.clear();
		this.decoders.addAll(decoders);
	}

	/**
	 * 返回已配置的解码器。
	 */
	public List<? extends Decoder<?>> getDecoders() {
		return this.decoders;
	}

	/**
	 * 设置用于校验 {@code @Payload} 参数的 Validator 实例。
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadMethodArgumentResolver
	 */
	public void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	/**
	 * 返回已配置的 Validator 实例。
	 */
	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * 设置 {@code RouteMatcher}，用于根据消息配置的路由模式将消息映射到处理器。
	 * <p>默认使用 {@link SimpleRouteMatcher}，其底层为以 "." 作为分隔符的
	 * {@link AntPathMatcher}。为了提高效率，可以考虑改用
	 * {@code spring-web} 中的 {@code PathPatternRouteMatcher}。
	 */
	public void setRouteMatcher(@Nullable RouteMatcher routeMatcher) {
		this.routeMatcher = routeMatcher;
	}

	/**
	 * 返回用于将消息映射到处理器的 {@code RouteMatcher}。
	 * 在组件初始化之前可能为 {@code null}。
	 */
	@Nullable
	public RouteMatcher getRouteMatcher() {
		return this.routeMatcher;
	}

	/**
	 * 获取实际使用的 {@code RouteMatcher}。
	 * @return RouteMatcher（永远不会为 {@code null}）
	 * @throws IllegalStateException 如果未设置 RouteMatcher
	 * @since 5.0
	 */
	protected RouteMatcher obtainRouteMatcher() {
		RouteMatcher routeMatcher = getRouteMatcher();
		Assert.state(routeMatcher != null, "No RouteMatcher set");
		return routeMatcher;
	}

	/**
	 * 配置用于对基于 String 的值（例如目标变量或头中的值）进行类型转换的
	 * {@link ConversionService}。
	 * <p>默认使用 {@link DefaultFormattingConversionService}。
	 * @param conversionService 要使用的转换服务
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回已配置的 ConversionService。
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.valueResolver = resolver;
	}

	/**
	 * 使用此方法注册 {@link MessagingAdviceBean}，其中可能包含全局适用的
	 * {@link org.springframework.messaging.handler.annotation.MessageExceptionHandler @MessageExceptionHandler}
	 * 方法。
	 * <p>注意：spring-messaging 不依赖 spring-web，因此无法显式支持注册
	 * {@code @ControllerAdvice} Bean。你可以使用以下适配器代码在此注册
	 * {@code @ControllerAdvice} Bean：
	 * <pre>
	 * ControllerAdviceBean.findAnnotatedBeans(context).forEach(bean -&gt;
	 *         messageHandler.registerMessagingAdvice(new ControllerAdviceWrapper(bean));
	 *
	 * public class ControllerAdviceWrapper implements MessagingAdviceBean {
	 *     private final ControllerAdviceBean delegate;
	 *     // delegate all methods
	 * }
	 * </pre>
	 * @param bean 要检查 {@code @MessageExceptionHandler} 方法的 Bean
	 * @since 5.3.5
	 */
	public void registerMessagingAdvice(MessagingAdviceBean bean) {
		Class<?> type = bean.getBeanType();
		if (type != null) {
			AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(type);
			if (resolver.hasExceptionMappings()) {
				registerExceptionHandlerAdvice(bean, resolver);
				if (logger.isTraceEnabled()) {
					logger.trace("Detected @MessageExceptionHandler methods in " + bean);
				}
			}
		}
	}

	@Override
	public void afterPropertiesSet() {

		// Initialize RouteMatcher before parent initializes handler mappings
		if (this.routeMatcher == null) {
			AntPathMatcher pathMatcher = new AntPathMatcher();
			pathMatcher.setPathSeparator(".");
			this.routeMatcher = new SimpleRouteMatcher(pathMatcher);
		}

		super.afterPropertiesSet();
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		ApplicationContext context = getApplicationContext();
		ConfigurableBeanFactory beanFactory = (context instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) context).getBeanFactory() : null);

		// Annotation-based resolvers
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());
		resolvers.add(new DestinationVariableMethodArgumentResolver(this.conversionService));

		// Type-based...
		if (KotlinDetector.isKotlinPresent()) {
			resolvers.add(new ContinuationHandlerMethodArgumentResolver());
		}

		// Custom resolvers
		resolvers.addAll(getArgumentResolverConfigurer().getCustomResolvers());

		// Catch-all
		resolvers.add(new PayloadMethodArgumentResolver(
				getDecoders(), this.validator, getReactiveAdapterRegistry(), true));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		return Collections.emptyList();
	}


	@Override
	protected CompositeMessageCondition getMappingForMethod(Method method, Class<?> handlerType) {
		CompositeMessageCondition methodCondition = getCondition(method);
		if (methodCondition != null) {
			CompositeMessageCondition typeCondition = getCondition(handlerType);
			if (typeCondition != null) {
				return typeCondition.combine(methodCondition);
			}
		}
		return methodCondition;
	}

	/**
	 * 确定给定注解元素的映射条件。
	 * @param element 要检查的元素
	 * @return 条件，或 {@code null}
	 */
	@Nullable
	protected CompositeMessageCondition getCondition(AnnotatedElement element) {
		MessageMapping ann = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
		if (ann == null || ann.value().length == 0) {
			return null;
		}
		String[] patterns = processDestinations(ann.value());
		return new CompositeMessageCondition(
				new DestinationPatternsMessageCondition(patterns, obtainRouteMatcher()));
	}

	/**
	 * 解析给定目标中的占位符。
	 * @param destinations 目的地
	 * @return 包含处理后目的地的新数组，或原数组
	 */
	protected String[] processDestinations(String[] destinations) {
		if (this.valueResolver != null) {
			destinations = Arrays.stream(destinations)
					.map(s -> this.valueResolver.resolveStringValue(s))
					.toArray(String[]::new);
		}
		return destinations;
	}

	@Override
	protected Set<String> getDirectLookupMappings(CompositeMessageCondition mapping) {
		Set<String> result = new LinkedHashSet<>();
		for (String pattern : mapping.getCondition(DestinationPatternsMessageCondition.class).getPatterns()) {
			if (!obtainRouteMatcher().isPattern(pattern)) {
				result.add(pattern);
			}
		}
		return result;
	}

	@Override
	protected RouteMatcher.Route getDestination(Message<?> message) {
		return (RouteMatcher.Route) message.getHeaders()
				.get(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER);
	}

	@Override
	protected CompositeMessageCondition getMatchingMapping(CompositeMessageCondition mapping, Message<?> message) {
		return mapping.getMatchingCondition(message);
	}

	@Override
	protected Comparator<CompositeMessageCondition> getMappingComparator(Message<?> message) {
		return (info1, info2) -> info1.compareTo(info2, message);
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

	@Override
	protected Mono<Void> handleMatch(
			CompositeMessageCondition mapping, HandlerMethod handlerMethod, Message<?> message) {

		Set<String> patterns = mapping.getCondition(DestinationPatternsMessageCondition.class).getPatterns();
		if (!CollectionUtils.isEmpty(patterns)) {
			String pattern = patterns.iterator().next();
			RouteMatcher.Route destination = getDestination(message);
			Assert.state(destination != null, "Missing destination header");
			Map<String, String> vars = obtainRouteMatcher().matchAndExtract(pattern, destination);
			if (!CollectionUtils.isEmpty(vars)) {
				MessageHeaderAccessor mha = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
				Assert.state(mha != null && mha.isMutable(), "Mutable MessageHeaderAccessor required");
				mha.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
			}
		}
		return super.handleMatch(mapping, handlerMethod, message);
	}

}
