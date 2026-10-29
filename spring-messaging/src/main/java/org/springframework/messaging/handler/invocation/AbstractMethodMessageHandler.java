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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * 基于 HandlerMethod 的消息处理的抽象基类。提供启动时发现处理器方法、
 * 运行时为给定消息查找匹配的处理器方法并调用它所需的大部分逻辑。
 *
 * <p>还支持发现并调用异常处理方法，以处理消息处理期间引发的异常。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 * @param <T> the type of the Object that contains information mapping a
 * {@link org.springframework.messaging.handler.HandlerMethod} to incoming messages
 */
public abstract class AbstractMethodMessageHandler<T>
		implements MessageHandler, ApplicationContextAware, InitializingBean {

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
	private Log handlerMethodLogger;


	private final List<String> destinationPrefixes = new ArrayList<>();

	private final List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>(4);

	private final List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<>(4);

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final HandlerMethodReturnValueHandlerComposite returnValueHandlers =
			new HandlerMethodReturnValueHandlerComposite();

	@Nullable
	private ApplicationContext applicationContext;

	private final Map<T, HandlerMethod> handlerMethods = new LinkedHashMap<>(64);

	private final MultiValueMap<String, T> destinationLookup = new LinkedMultiValueMap<>(48);

	private final Map<Class<?>, AbstractExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	private final Map<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);


	/**
	 * 配置此属性后，只有发送到与某个已配置前缀匹配的目标的消息才有资格被处理。
	 * 匹配时会移除该前缀，仅使用目标的剩余部分进行方法映射。
	 * <p>默认不配置任何前缀，此时所有消息都有资格被处理。
	 */
	public void setDestinationPrefixes(@Nullable Collection<String> prefixes) {
		this.destinationPrefixes.clear();
		if (prefixes != null) {
			for (String prefix : prefixes) {
				prefix = prefix.trim();
				this.destinationPrefixes.add(prefix);
			}
		}
	}

	/**
	 * 返回已配置的目标前缀（如果有）。
	 */
	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	/**
	 * 设置自定义 {@code HandlerMethodArgumentResolver} 列表，
	 * 这些解析器将在受支持参数类型的解析器之后使用。
	 */
	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		this.customArgumentResolvers.clear();
		if (customArgumentResolvers != null) {
			this.customArgumentResolvers.addAll(customArgumentResolvers);
		}
	}

	/**
	 * 返回已配置的自定义参数解析器（如果有）。
	 */
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * 设置自定义 {@code HandlerMethodReturnValueHandler} 列表，
	 * 这些处理器将在已知类型的返回值处理器之后使用。
	 */
	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> customReturnValueHandlers) {
		this.customReturnValueHandlers.clear();
		if (customReturnValueHandlers != null) {
			this.customReturnValueHandlers.addAll(customReturnValueHandlers);
		}
	}

	/**
	 * 返回已配置的自定义返回值处理器（如果有）。
	 */
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * 配置受支持参数类型的完整列表，实际上会覆盖默认配置的列表。
	 * 这是一个高级选项；对于大多数用例，使用
	 * {@link #setCustomArgumentResolvers} 就足够了。
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers.clear();
			return;
		}
		this.argumentResolvers.addResolvers(argumentResolvers);
	}

	/**
	 * 返回参数解析器的完整列表。
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers.getResolvers();
	}

	/**
	 * 配置受支持返回值类型的完整列表，实际上会覆盖默认配置的列表。
	 * 这是一个高级选项；对于大多数用例，使用
	 * {@link #setCustomReturnValueHandlers} 就足够了。
	 */
	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			this.returnValueHandlers.clear();
			return;
		}
		this.returnValueHandlers.addHandlers(returnValueHandlers);
	}

	/**
	 * 返回返回值处理器的完整列表。
	 */
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return this.returnValueHandlers.getReturnValueHandlers();
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
	public void afterPropertiesSet() {
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}

		if (this.returnValueHandlers.getReturnValueHandlers().isEmpty()) {
			this.returnValueHandlers.addHandlers(initReturnValueHandlers());
		}
		Log returnValueLogger = getReturnValueHandlerLogger();
		if (returnValueLogger != null) {
			this.returnValueHandlers.setLogger(returnValueLogger);
		}

		this.handlerMethodLogger = getHandlerMethodLogger();

		ApplicationContext context = getApplicationContext();
		if (context == null) {
			return;
		}
		for (String beanName : context.getBeanNamesForType(Object.class)) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = context.getType(beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
	}

	/**
	 * 返回要使用的参数解析器列表。仅当尚未通过
	 * {@link #setArgumentResolvers} 设置解析器时才会调用。
	 * <p>子类还应考虑通过 {@link #setCustomArgumentResolvers}
	 * 配置的自定义参数类型。
	 */
	protected abstract List<? extends HandlerMethodArgumentResolver> initArgumentResolvers();

	/**
	 * 返回要使用的返回值处理器列表。仅当尚未通过
	 * {@link #setReturnValueHandlers} 设置返回值处理器时才会调用。
	 * <p>子类还应考虑通过 {@link #setCustomReturnValueHandlers}
	 * 配置的自定义返回值类型。
	 */
	protected abstract List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers();


	/**
	 * 是否应内省给定 Bean 类型以查找消息处理方法。
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * 检测给定处理器是否有任何可以处理消息的方法，如果有，
	 * 则使用提取的映射信息注册它。
	 * @param handler the handler to check, either an instance of a Spring bean name
	 */
	protected final void detectHandlerMethods(final Object handler) {
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
	 * 提供处理器方法的映射。
	 * @param method the method to provide a mapping for
	 * @param handlerType the handler type, possibly a subtype of the method's declaring class
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 注册处理器方法及其唯一映射。
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already registered
	 * under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		Assert.notNull(mapping, "Mapping must not be null");
		HandlerMethod newHandlerMethod = createHandlerMethod(handler, method);
		HandlerMethod oldHandlerMethod = this.handlerMethods.get(mapping);

		if (oldHandlerMethod != null && !oldHandlerMethod.equals(newHandlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + newHandlerMethod.getBean() +
					"' bean method \n" + newHandlerMethod + "\nto " + mapping + ": There is already '" +
					oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
		}

		this.handlerMethods.put(mapping, newHandlerMethod);

		for (String pattern : getDirectLookupDestinations(mapping)) {
			this.destinationLookup.add(pattern, mapping);
		}
	}

	/**
	 * 从 Object 处理器（处理器实例或基于 String 的 Bean 名称）
	 * 创建 HandlerMethod 实例。
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
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
	 * 返回映射中包含的目标，这些目标不是模式，
	 * 因此适合直接查找。
	 */
	protected abstract Set<String> getDirectLookupDestinations(T mapping);

	/**
	 * 返回要设置在 {@link HandlerMethodReturnValueHandlerComposite} 上的日志记录器。
	 * @since 5.1
	 */
	@Nullable
	protected Log getReturnValueHandlerLogger() {
		return null;
	}

	/**
	 * 返回要设置在 {@link InvocableHandlerMethod} 上的日志记录器。
	 * @since 5.1
	 */
	@Nullable
	protected Log getHandlerMethodLogger() {
		return null;
	}

	/**
	 * 子类可以调用此方法来填充 MessagingAdviceBean 缓存
	 * （例如以支持“全局” {@code @MessageExceptionHandler}）。
	 * @since 4.2
	 */
	protected void registerExceptionHandlerAdvice(
			MessagingAdviceBean bean, AbstractExceptionHandlerMethodResolver resolver) {

		this.exceptionHandlerAdviceCache.put(bean, resolver);
	}

	/**
	 * 返回包含所有处理器方法及其映射的映射表。
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return Collections.unmodifiableMap(this.handlerMethods);
	}


	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		String destination = getDestination(message);
		if (destination == null) {
			return;
		}
		String lookupDestination = getLookupDestination(destination);
		if (lookupDestination == null) {
			return;
		}

		MessageHeaderAccessor headerAccessor = MessageHeaderAccessor.getMutableAccessor(message);
		headerAccessor.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, lookupDestination);
		headerAccessor.setLeaveMutable(true);
		message = MessageBuilder.createMessage(message.getPayload(), headerAccessor.getMessageHeaders());

		if (logger.isDebugEnabled()) {
			logger.debug("Searching methods to handle " +
					headerAccessor.getShortLogMessage(message.getPayload()) +
					", lookupDestination='" + lookupDestination + "'");
		}

		handleMessageInternal(message, lookupDestination);
		headerAccessor.setImmutable();
	}

	@Nullable
	protected abstract String getDestination(Message<?> message);

	/**
	 * 检查给定目标（传入消息的目标）是否与某个已配置的目标前缀匹配，
	 * 如果匹配，则返回匹配前缀之后的目标剩余部分。
	 * <p>如果没有匹配的前缀，则返回 {@code null}。
	 * <p>如果没有目标前缀，则按原样返回目标。
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Nullable
	protected String getLookupDestination(@Nullable String destination) {
		if (destination == null) {
			return null;
		}
		if (CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return destination;
		}
		for (int i = 0; i < this.destinationPrefixes.size(); i++) {
			String prefix = this.destinationPrefixes.get(i);
			if (destination.startsWith(prefix)) {
				return destination.substring(prefix.length());
			}
		}
		return null;
	}

	protected void handleMessageInternal(Message<?> message, String lookupDestination) {
		List<Match> matches = new ArrayList<>();

		List<T> mappingsByUrl = this.destinationLookup.get(lookupDestination);
		if (mappingsByUrl != null) {
			addMatchesToCollection(mappingsByUrl, message, matches);
		}
		if (matches.isEmpty()) {
			// No direct hits, go through all mappings
			Set<T> allMappings = this.handlerMethods.keySet();
			addMatchesToCollection(allMappings, message, matches);
		}
		if (matches.isEmpty()) {
			handleNoMatch(this.handlerMethods.keySet(), lookupDestination, message);
			return;
		}

		Comparator<Match> comparator = new MatchComparator(getMappingComparator(message));
		matches.sort(comparator);
		if (logger.isTraceEnabled()) {
			logger.trace("Found " + matches.size() + " handler methods: " + matches);
		}

		Match bestMatch = matches.get(0);
		if (matches.size() > 1) {
			Match secondBestMatch = matches.get(1);
			if (comparator.compare(bestMatch, secondBestMatch) == 0) {
				Method m1 = bestMatch.handlerMethod.getMethod();
				Method m2 = secondBestMatch.handlerMethod.getMethod();
				throw new IllegalStateException("Ambiguous handler methods mapped for destination '" +
						lookupDestination + "': {" + m1 + ", " + m2 + "}");
			}
		}

		handleMatch(bestMatch.mapping, bestMatch.handlerMethod, lookupDestination, message);
	}

	private void addMatchesToCollection(Collection<T> mappingsToCheck, Message<?> message, List<Match> matches) {
		for (T mapping : mappingsToCheck) {
			T match = getMatchingMapping(mapping, message);
			if (match != null) {
				matches.add(new Match(match, this.handlerMethods.get(mapping)));
			}
		}
	}

	/**
	 * 检查映射是否匹配当前消息，并返回一个可能包含与当前请求相关条件的新映射。
	 * @param mapping the mapping to get a match for
	 * @param message the message being handled
	 * @return the match or {@code null} if there is no match
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, Message<?> message);

	protected void handleNoMatch(Set<T> ts, String lookupDestination, Message<?> message) {
		logger.debug("No matching message handler methods.");
	}

	/**
	 * 返回用于对匹配映射排序的比较器。
	 * 返回的比较器应将“更好”的匹配排在前面。
	 * @param message the current Message
	 * @return the comparator, never {@code null}
	 */
	protected abstract Comparator<T> getMappingComparator(Message<?> message);

	protected void handleMatch(T mapping, HandlerMethod handlerMethod, String lookupDestination, Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + handlerMethod.getShortLogMessage());
		}
		handlerMethod = handlerMethod.createWithResolvedBean();
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		if (this.handlerMethodLogger != null) {
			invocable.setLogger(this.handlerMethodLogger);
		}
		invocable.setMessageMethodArgumentResolvers(this.argumentResolvers);
		try {
			Object returnValue = invocable.invoke(message);
			MethodParameter returnType = handlerMethod.getReturnType();
			if (void.class == returnType.getParameterType()) {
				return;
			}
			if (returnValue != null && this.returnValueHandlers.isAsyncReturnValue(returnValue, returnType)) {
				ListenableFuture<?> future = this.returnValueHandlers.toListenableFuture(returnValue, returnType);
				if (future != null) {
					future.addCallback(new ReturnValueListenableFutureCallback(invocable, message));
				}
			}
			else {
				this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
			}
		}
		catch (Exception ex) {
			processHandlerMethodException(handlerMethod, ex, message);
		}
		catch (Throwable ex) {
			Exception handlingException =
					new MessageHandlingException(message, "Unexpected handler method invocation error", ex);
			processHandlerMethodException(handlerMethod, handlingException, message);
		}
	}

	protected void processHandlerMethodException(HandlerMethod handlerMethod, Exception exception, Message<?> message) {
		InvocableHandlerMethod invocable = getExceptionHandlerMethod(handlerMethod, exception);
		if (invocable == null) {
			logger.error("Unhandled exception from message handler method", exception);
			return;
		}
		invocable.setMessageMethodArgumentResolvers(this.argumentResolvers);
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + invocable.getShortLogMessage());
		}
		try {
			Throwable cause = exception.getCause();
			Object returnValue = (cause != null ?
					invocable.invoke(message, exception, cause, handlerMethod) :
					invocable.invoke(message, exception, handlerMethod));
			MethodParameter returnType = invocable.getReturnType();
			if (void.class == returnType.getParameterType()) {
				return;
			}
			this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
		}
		catch (Throwable ex2) {
			logger.error("Error while processing handler method exception", ex2);
		}
	}

	/**
	 * 为给定异常查找 {@code @MessageExceptionHandler} 方法。
	 * 默认实现首先在 HandlerMethod 的类层次结构中搜索方法；如果未找到，
	 * 则继续在已配置的
	 * {@linkplain org.springframework.messaging.handler.MessagingAdviceBean
	 * MessagingAdviceBean}（如果有）中搜索其他 {@code @MessageExceptionHandler} 方法。
	 * @param handlerMethod the method where the exception was raised
	 * @param exception the raised exception
	 * @return a method to handle the exception, or {@code null}
	 * @since 4.2
	 */
	@Nullable
	protected InvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching methods to handle " + exception.getClass().getSimpleName());
		}
		Class<?> beanType = handlerMethod.getBeanType();
		AbstractExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(beanType);
		if (resolver == null) {
			resolver = createExceptionHandlerMethodResolverFor(beanType);
			this.exceptionHandlerCache.put(beanType, resolver);
		}
		Method method = resolver.resolveMethod(exception);
		if (method != null) {
			return new InvocableHandlerMethod(handlerMethod.getBean(), method);
		}
		for (Map.Entry<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
			MessagingAdviceBean advice = entry.getKey();
			if (advice.isApplicableToBeanType(beanType)) {
				resolver = entry.getValue();
				method = resolver.resolveMethod(exception);
				if (method != null) {
					return new InvocableHandlerMethod(advice.resolveBean(), method);
				}
			}
		}
		return null;
	}

	protected abstract AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(
			Class<?> beanType);


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[prefixes=" + getDestinationPrefixes() + "]";
	}


	/**
	 * 对匹配的 HandlerMethod 及其匹配映射的轻量封装，
	 * 用于在消息上下文中通过比较器比较最佳匹配。
	 */
	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private class ReturnValueListenableFutureCallback implements ListenableFutureCallback<Object> {

		private final InvocableHandlerMethod handlerMethod;

		private final Message<?> message;

		public ReturnValueListenableFutureCallback(InvocableHandlerMethod handlerMethod, Message<?> message) {
			this.handlerMethod = handlerMethod;
			this.message = message;
		}

		@Override
		public void onSuccess(@Nullable Object result) {
			try {
				MethodParameter returnType = this.handlerMethod.getAsyncReturnValueType(result);
				returnValueHandlers.handleReturnValue(result, returnType, this.message);
			}
			catch (Throwable ex) {
				handleFailure(ex);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			handleFailure(ex);
		}

		private void handleFailure(Throwable ex) {
			Exception cause = (ex instanceof Exception ? (Exception) ex : new IllegalStateException(ex));
			processHandlerMethodException(this.handlerMethod, cause, this.message);
		}
	}

}
