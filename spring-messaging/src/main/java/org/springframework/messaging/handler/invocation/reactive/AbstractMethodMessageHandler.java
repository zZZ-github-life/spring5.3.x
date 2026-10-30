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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.StringUtils;

/**
 * 基于响应式 HandlerMethod 的消息处理的抽象基类。提供启动时发现处理器方法、
 * 运行时为给定消息查找匹配的处理器方法并调用它所需的大部分逻辑。
 *
 * <p>还支持发现并调用异常处理方法，以处理消息处理期间引发的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @param <T> 包含映射信息的对象类型
 */
public abstract class AbstractMethodMessageHandler<T>
		implements ReactiveMessageHandler, ApplicationContextAware, InitializingBean, BeanNameAware {

	/**
	 * 作用域代理背后目标 Bean 的 Bean 名称前缀。用于在处理器方法检测中排除这些目标，
	 * 转而使用相应的代理。
	 * <p>此处不检查 autowire-candidate 状态——自动装配层面正是以该状态处理代理目标
	 * 过滤问题的；因为 autowire-candidate 可能因其他原因被设为 {@code false}，
	 * 但 Bean 仍应被纳入处理器方法的候选范围。
	 * <p>最初定义于 {@link org.springframework.aop.scope.ScopedProxyUtils}，
	 * 但此处重复定义以避免对 spring-aop 模块的硬依赖。
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";


	protected final Log logger = LogFactory.getLog(getClass());


	@Nullable
	private Predicate<Class<?>> handlerPredicate;

	@Nullable
	List<Object> handlers;

	private ArgumentResolverConfigurer argumentResolverConfigurer = new ArgumentResolverConfigurer();

	private ReturnValueHandlerConfigurer returnValueHandlerConfigurer = new ReturnValueHandlerConfigurer();

	private final InvocableHelper invocableHelper = new InvocableHelper(this::createExceptionMethodResolverFor);

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private String beanName;

	private final Map<T, HandlerMethod> handlerMethods = new ConcurrentHashMap<>(64);

	private final Map<String, List<T>> destinationLookup = new ConcurrentHashMap<>(48);


	/**
	 * 配置用于选择要检查是否存在消息处理方法的 Spring Bean 的谓词。
	 * <p>默认不设置。不过子类可以将其初始化为某种默认策略
	 * （例如 {@code @Controller} 类）。
	 * @see #setHandlers(List)
	 */
	public void setHandlerPredicate(@Nullable Predicate<Class<?>> handlerPredicate) {
		this.handlerPredicate = handlerPredicate;
	}

	/**
	 * 返回{@link #setHandlerPredicate 已配置}的处理器谓词。
	 */
	@Nullable
	public Predicate<Class<?>> getHandlerPredicate() {
		return this.handlerPredicate;
	}

	/**
	 * 手动配置要检查是否存在消息处理方法的处理器，这也会禁用通过
	 * {@link #setHandlerPredicate(Predicate) handlerPredicate} 进行的自动检测。
	 * 如果你不想禁用自动检测，请先调用此方法，然后再设置处理器谓词。
	 * @param handlers 要检查的处理器
	 */
	public void setHandlers(List<Object> handlers) {
		this.handlers = handlers;
		this.handlerPredicate = null;
	}

	/**
	 * 为处理器方法参数配置自定义解析器。
	 */
	public void setArgumentResolverConfigurer(ArgumentResolverConfigurer configurer) {
		Assert.notNull(configurer, "HandlerMethodArgumentResolver is required");
		this.argumentResolverConfigurer = configurer;
	}

	/**
	 * 返回已配置的处理器方法参数自定义解析器。
	 */
	public ArgumentResolverConfigurer getArgumentResolverConfigurer() {
		return this.argumentResolverConfigurer;
	}

	/**
	 * 为处理器方法配置自定义返回值处理器。
	 */
	public void setReturnValueHandlerConfigurer(ReturnValueHandlerConfigurer configurer) {
		Assert.notNull(configurer, "ReturnValueHandlerConfigurer is required");
		this.returnValueHandlerConfigurer = configurer;
	}

	/**
	 * 返回已配置的返回值处理器。
	 */
	public ReturnValueHandlerConfigurer getReturnValueHandlerConfigurer() {
		return this.returnValueHandlerConfigurer;
	}

	/**
	 * 配置用于适配各种响应式类型的注册表。
	 * <p>默认情况下，这是使用默认设置的 {@link ReactiveAdapterRegistry} 实例。
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.invocableHelper.setReactiveAdapterRegistry(registry);
	}

	/**
	 * 返回已配置的用于适配响应式类型的注册表。
	 */
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return this.invocableHelper.getReactiveAdapterRegistry();
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public String getBeanName() {
		return (this.beanName != null ? this.beanName :
				getClass().getSimpleName() + "@" + ObjectUtils.getIdentityHexString(this));
	}

	/**
	 * 子类可以调用此方法来填充 MessagingAdviceBean 缓存
	 * （例如以支持“全局” {@code @MessageExceptionHandler}）。
	 */
	protected void registerExceptionHandlerAdvice(
			MessagingAdviceBean bean, AbstractExceptionHandlerMethodResolver resolver) {

		this.invocableHelper.registerExceptionHandlerAdvice(bean, resolver);
	}

	/**
	 * 返回包含所有处理器方法及其映射的只读映射表。
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return Collections.unmodifiableMap(this.handlerMethods);
	}

	/**
	 * 返回一个只读多值映射，用于直接查找映射
	 * （例如非模式目标）。
	 */
	public MultiValueMap<String, T> getDestinationLookup() {
		return CollectionUtils.unmodifiableMultiValueMap(CollectionUtils.toMultiValueMap(this.destinationLookup));
	}

	/**
	 * 返回在 {@link #afterPropertiesSet()} 期间初始化的参数解析器。
	 * 主要供子类内部使用。
	 * @since 5.2.2
	 */
	protected HandlerMethodArgumentResolverComposite getArgumentResolvers() {
		return this.invocableHelper.getArgumentResolvers();
	}


	@Override
	public void afterPropertiesSet() {

		List<? extends HandlerMethodArgumentResolver> resolvers = initArgumentResolvers();
		if (resolvers.isEmpty()) {
			resolvers = new ArrayList<>(this.argumentResolverConfigurer.getCustomResolvers());
		}
		this.invocableHelper.addArgumentResolvers(resolvers);

		List<? extends HandlerMethodReturnValueHandler> handlers = initReturnValueHandlers();
		if (handlers.isEmpty()) {
			handlers = new ArrayList<>(this.returnValueHandlerConfigurer.getCustomHandlers());
		}
		this.invocableHelper.addReturnValueHandlers(handlers);

		initHandlerMethods();
	}

	/**
	 * 返回要使用的参数解析器列表。
	 * <p>子类还应考虑通过 {@link #setArgumentResolverConfigurer}
	 * 配置的自定义参数类型。
	 */
	protected abstract List<? extends HandlerMethodArgumentResolver> initArgumentResolvers();

	/**
	 * 返回要使用的返回值处理器列表。
	 * <p>子类还应考虑通过 {@link #setReturnValueHandlerConfigurer}
	 * 配置的自定义返回值类型。
	 */
	protected abstract List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers();


	private void initHandlerMethods() {
		if (this.handlers != null) {
			for (Object handler : this.handlers) {
				detectHandlerMethods(handler);
			}
		}
		Predicate<Class<?>> predicate = this.handlerPredicate;
		if (predicate == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("[" + getBeanName() + "] Skip auto-detection of message handling methods");
			}
			return;
		}
		if (this.applicationContext == null) {
			logger.warn("No ApplicationContext for auto-detection of beans with message handling methods.");
			return;
		}
		for (String beanName : this.applicationContext.getBeanNamesForType(Object.class)) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = this.applicationContext.getType(beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && predicate.test(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
	}

	/**
	 * 检测给定处理器是否有任何可以处理消息的方法，如果有，
	 * 则使用提取的映射信息注册它。
	 * <p><strong>注意：</strong>此方法为 protected，可由子类调用，
	 * 但按照 {@link #registerHandlerMethod} 的说明，只应在启动时调用。
	 * @param handler 要检查的处理器，可以是实例或 Spring Bean 名称
	 */
	protected final void detectHandlerMethods(Object handler) {
		Class<?> handlerType;
		if (handler instanceof String) {
			ApplicationContext context = getApplicationContext();
			Assert.state(context != null, "ApplicationContext is required for resolving handler bean names");
			handlerType = context.getType((String) handler);
		}
		else {
			handlerType = handler.getClass();
		}
		if (handlerType != null) {
			final Class<?> userType = ClassUtils.getUserClass(handlerType);
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> getMappingForMethod(method, userType));
			if (logger.isDebugEnabled()) {
				logger.debug(formatMappings(userType, methods));
			}
			methods.forEach((key, value) -> registerHandlerMethod(handler, key, value));
		}
	}

	private String formatMappings(Class<?> userType, Map<Method, T> methods) {
		String packageName = ClassUtils.getPackageName(userType);
		String formattedType = (StringUtils.hasText(packageName) ?
				Arrays.stream(packageName.split("\\."))
						.map(packageSegment -> packageSegment.substring(0, 1))
						.collect(Collectors.joining(".", "", "." + userType.getSimpleName())) :
				userType.getSimpleName());
		Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
				.map(Class::getSimpleName)
				.collect(Collectors.joining(",", "(", ")"));
		return methods.entrySet().stream()
				.map(e -> {
					Method method = e.getKey();
					return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
				})
				.collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
	}

	/**
	 * 获取给定方法的映射（如果有）。
	 * @param method 要检查的方法
	 * @param handlerType 处理器类型，可能是该方法声明类的子类型
	 * @return 映射；如果该方法未被映射则返回 {@code null}
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 注册处理器方法及其唯一映射。
	 * <p><strong>注意：</strong>从 5.3 开始，此方法为 public（而不是 protected），
	 * 既可以在启动时使用，也可以在运行时使用。
	 * @param handler 处理器的 Bean 名称或处理器实例
	 * @param method 要注册的方法
	 * @param mapping 与处理器方法关联的映射条件
	 * @throws IllegalStateException 如果已在同一映射下注册了其他方法
	 */
	public final void registerHandlerMethod(Object handler, Method method, T mapping) {
		Assert.notNull(mapping, "Mapping must not be null");
		HandlerMethod newHandlerMethod = createHandlerMethod(handler, method);
		HandlerMethod oldHandlerMethod = this.handlerMethods.get(mapping);

		if (oldHandlerMethod != null && !oldHandlerMethod.equals(newHandlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + newHandlerMethod.getBean() +
					"' bean method \n" + newHandlerMethod + "\nto " + mapping + ": There is already '" +
					oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
		}

		mapping = extendMapping(mapping, newHandlerMethod);
		this.handlerMethods.put(mapping, newHandlerMethod);

		for (String pattern : getDirectLookupMappings(mapping)) {
			List<T> values = this.destinationLookup.computeIfAbsent(pattern, p -> new CopyOnWriteArrayList<>());
			values.add(mapping);
		}
	}

	/**
	 * 从 Object 处理器（处理器实例或基于 String 的 Bean 名称）
	 * 创建 HandlerMethod 实例。
	 */
	private HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			ApplicationContext context = getApplicationContext();
			Assert.state(context != null, "ApplicationContext is required for resolving handler bean names");
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName, context.getAutowireCapableBeanFactory(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * 在添加映射之前调用此方法。它允许子类结合 {@link HandlerMethod}
	 * 更新映射。当使用方法签名来细化映射时（例如根据输入和输出的基数），
	 * 这可能很有用。
	 * <p>默认情况下，此方法返回传入的映射。
	 * @param mapping 要添加的映射
	 * @param handlerMethod 该映射的目标处理器
	 * @return 新的映射，或原映射
	 * @since 5.2.2
	 */
	protected T extendMapping(T mapping, HandlerMethod handlerMethod) {
		return mapping;
	}

	/**
	 * 返回给定映射中基于 String 的目标（如果有），
	 * 这些目标可用于直接查找匹配（即非模式）。
	 * <p><strong>注意：</strong>这完全是可选的。子类的映射元数据
	 * 可能既不支持直接查找，也不支持基于 String 的目标。
	 */
	protected abstract Set<String> getDirectLookupMappings(T mapping);


	@Override
	public Mono<Void> handleMessage(Message<?> message) throws MessagingException {
		Match<T> match = null;
		try {
			match = getHandlerMethod(message);
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}
		if (match == null) {
			// handleNoMatch would have been invoked already
			return Mono.empty();
		}
		return handleMatch(match.mapping, match.handlerMethod, message);
	}

	protected Mono<Void> handleMatch(T mapping, HandlerMethod handlerMethod, Message<?> message) {
		handlerMethod = handlerMethod.createWithResolvedBean();
		return this.invocableHelper.handleMessage(handlerMethod, message);
	}

	@Nullable
	private Match<T> getHandlerMethod(Message<?> message) {
		List<Match<T>> matches = new ArrayList<>();

		RouteMatcher.Route destination = getDestination(message);
		List<T> mappingsByUrl = (destination != null ? this.destinationLookup.get(destination.value()) : null);
		if (mappingsByUrl != null) {
			addMatchesToCollection(mappingsByUrl, message, matches);
		}
		if (matches.isEmpty()) {
			// No direct hits, go through all mappings
			Set<T> allMappings = this.handlerMethods.keySet();
			addMatchesToCollection(allMappings, message, matches);
		}
		if (matches.isEmpty()) {
			handleNoMatch(destination, message);
			return null;
		}
		Comparator<Match<T>> comparator = new MatchComparator(getMappingComparator(message));
		matches.sort(comparator);
		if (logger.isTraceEnabled()) {
			logger.trace("Found " + matches.size() + " handler methods: " + matches);
		}
		Match<T> bestMatch = matches.get(0);
		if (matches.size() > 1) {
			Match<T> secondBestMatch = matches.get(1);
			if (comparator.compare(bestMatch, secondBestMatch) == 0) {
				HandlerMethod m1 = bestMatch.handlerMethod;
				HandlerMethod m2 = secondBestMatch.handlerMethod;
				throw new IllegalStateException("Ambiguous handler methods mapped for destination '" +
						(destination != null ? destination.value() : "") + "': {" +
						m1.getShortLogMessage() + ", " + m2.getShortLogMessage() + "}");
			}
		}
		return bestMatch;
	}

	/**
	 * 从给定消息中提取目标。
	 * @see #getDirectLookupMappings(Object)
	 */
	@Nullable
	protected abstract RouteMatcher.Route getDestination(Message<?> message);

	private void addMatchesToCollection(
			Collection<T> mappingsToCheck, Message<?> message, List<Match<T>> matches) {

		for (T mapping : mappingsToCheck) {
			T match = getMatchingMapping(mapping, message);
			if (match != null) {
				matches.add(new Match<T>(match, this.handlerMethods.get(mapping)));
			}
		}
	}

	/**
	 * 检查映射是否匹配当前消息，并返回一个可能包含与当前请求相关条件的新映射。
	 * @param mapping 要为其获取匹配项的映射
	 * @param message 正在处理的消息
	 * @return 匹配项；如果没有匹配则返回 {@code null}
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, Message<?> message);

	/**
	 * 返回用于对匹配映射排序的比较器。
	 * 返回的比较器应将“更好”的匹配排在前面。
	 * @param message 当前消息
	 * @return 比较器，永远不会为 {@code null}
	 */
	protected abstract Comparator<T> getMappingComparator(Message<?> message);

	/**
	 * 未找到匹配的处理器时调用。
	 * @param destination 目的地
	 * @param message 消息
	 */
	protected void handleNoMatch(@Nullable RouteMatcher.Route destination, Message<?> message) {
		logger.debug("No handlers for destination '" +
				(destination != null ? destination.value() : "") + "'");
	}

	/**
	 * 创建 {@link AbstractExceptionHandlerMethodResolver} 的具体实例，
	 * 它基于某种条件（例如是否存在 {@code @MessageExceptionHandler}）
	 * 查找异常处理方法。
	 * @param beanType 处理期间发生异常的类
	 * @return 要使用的解析器
	 */
	protected abstract AbstractExceptionHandlerMethodResolver createExceptionMethodResolverFor(Class<?> beanType);


	/**
	 * 匹配的映射和 HandlerMethod 的容器。用于最佳匹配比较和访问映射信息。
	 */
	private static class Match<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match<T>> {

		private final Comparator<T> comparator;

		MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match<T> match1, Match<T> match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}

}
