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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link HandlerMapping} 实现的抽象基类，用于定义请求与 {@link HandlerMethod} 之间的映射。
 *
 * <p>对于每个已注册的处理器方法，都会维护一个唯一的映射，由子类定义映射类型 {@code <T>} 的细节。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.0
 * @param <T> the mapping for a {@link HandlerMethod} containing the conditions
 * needed to match the handler method to an incoming request.
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 作用域代理背后目标 Bean 的 Bean 名称前缀。用于将这些目标从处理器方法检测中排除，转而使用相应的代理。
	 * <p>这里不检查 autowire-candidate 状态，该状态是在自动装配层面处理代理目标过滤问题的方式，因为 autowire-candidate 可能因其他原因被设为 {@code false}，而仍然期望该 Bean 有资格作为处理器方法。
	 * <p>最初定义于 {@link org.springframework.aop.scope.ScopedProxyUtils}，但在此处复制以避免对 spring-aop 模块的硬依赖。
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	/**
	 * 当请求映射比访问控制头更精细时，在预检请求匹配时返回的 HandlerMethod。
	 */
	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new PreFlightAmbiguousMatchHandler(),
					ClassUtils.getMethod(PreFlightAmbiguousMatchHandler.class, "handle"));

	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOriginPattern("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}


	private final MappingRegistry mappingRegistry = new MappingRegistry();


	// TODO: handlerMethodMappingNamingStrategy

	/**
	 * 返回包含所有映射和 HandlerMethod 的（只读）映射表。
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		this.mappingRegistry.acquireReadLock();
		try {
			return Collections.unmodifiableMap(
					this.mappingRegistry.getRegistrations().entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().handlerMethod)));
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 返回内部映射注册表。供测试使用。
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * 注册给定的映射。
	 * <p>此方法可以在初始化完成后于运行时调用。
	 * @param mapping the mapping for the handler method
	 * @param handler the handler
	 * @param method the method
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		if (logger.isTraceEnabled()) {
			logger.trace("Register \"" + mapping + "\" to " + method.toGenericString());
		}
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 注销给定的映射。
	 * <p>此方法可以在初始化完成后于运行时调用。
	 * @param mapping the mapping to unregister
	 */
	public void unregisterMapping(T mapping) {
		if (logger.isTraceEnabled()) {
			logger.trace("Unregister mapping \"" + mapping);
		}
		this.mappingRegistry.unregister(mapping);
	}


	// Handler method detection

	/**
	 * 在初始化时检测处理器方法。
	 */
	@Override
	public void afterPropertiesSet() {
		initHandlerMethods();

		// Total includes detected mappings + explicit registrations via registerMapping
		int total = getHandlerMethods().size();
		if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0) ) {
			logger.debug(total + " mappings in " + formatMappingName());
		}
	}

	/**
	 * 扫描 ApplicationContext 中的 Bean，检测并注册处理器方法。
	 * @see #isHandler(Class)
	 * @see #getMappingForMethod(Method, Class)
	 * @see #handlerMethodsInitialized(Map)
	 */
	protected void initHandlerMethods() {
		String[] beanNames = obtainApplicationContext().getBeanNamesForType(Object.class);

		for (String beanName : beanNames) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = obtainApplicationContext().getType(beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isTraceEnabled()) {
						logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * 在处理器中查找处理器方法。
	 * @param handler the bean name of a handler or a handler instance
	 */
	protected void detectHandlerMethods(final Object handler) {
		Class<?> handlerType = (handler instanceof String ?
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		if (handlerType != null) {
			final Class<?> userType = ClassUtils.getUserClass(handlerType);
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> getMappingForMethod(method, userType));
			if (logger.isTraceEnabled()) {
				logger.trace(formatMappings(userType, methods));
			}
			else if (mappingsLogger.isDebugEnabled()) {
				mappingsLogger.debug(formatMappings(userType, methods));
			}
			methods.forEach((method, mapping) -> {
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
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
	 * 注册处理器方法及其唯一映射。在启动时为每个检测到的处理器方法调用。
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already registered
	 * under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 创建 HandlerMethod 实例。
	 * @param handler either a bean name or an actual handler instance
	 * @param method the target method
	 * @return the created HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		if (handler instanceof String) {
			return new HandlerMethod((String) handler,
					obtainApplicationContext().getAutowireCapableBeanFactory(),
					obtainApplicationContext(),
					method);
		}
		return new HandlerMethod(handler, method);
	}

	/**
	 * 提取并返回该映射的 CORS 配置。
	 */
	@Nullable
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * 在所有处理器方法检测完成后调用。
	 * @param handlerMethods a read-only map with handler methods and mappings.
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}


	// Handler method lookup

	/**
	 * 为给定请求查找处理器方法。
	 * @param exchange the current exchange
	 */
	@Override
	public Mono<HandlerMethod> getHandlerInternal(ServerWebExchange exchange) {
		this.mappingRegistry.acquireReadLock();
		try {
			HandlerMethod handlerMethod;
			try {
				handlerMethod = lookupHandlerMethod(exchange);
			}
			catch (Exception ex) {
				return Mono.error(ex);
			}
			if (handlerMethod != null) {
				handlerMethod = handlerMethod.createWithResolvedBean();
			}
			return Mono.justOrEmpty(handlerMethod);
		}
		finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 为当前请求查找最佳匹配的处理器方法。如果找到多个匹配，则选择最佳匹配。
	 * @param exchange the current exchange
	 * @return the best-matching handler method, or {@code null} if no match
	 * @see #handleMatch
	 * @see #handleNoMatch
	 */
	@Nullable
	protected HandlerMethod lookupHandlerMethod(ServerWebExchange exchange) throws Exception {
		List<Match> matches = new ArrayList<>();
		List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(exchange);
		if (directPathMatches != null) {
			addMatchingMappings(directPathMatches, matches, exchange);
		}
		if (matches.isEmpty()) {
			addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, exchange);
		}
		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(exchange));
			matches.sort(comparator);
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				if (logger.isTraceEnabled()) {
					logger.trace(exchange.getLogPrefix() + matches.size() + " matching mappings: " + matches);
				}
				if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
					for (Match match : matches) {
						if (match.hasCorsConfig()) {
							return PREFLIGHT_AMBIGUOUS_MATCH;
						}
					}
				}
				else {
					Match secondBestMatch = matches.get(1);
					if (comparator.compare(bestMatch, secondBestMatch) == 0) {
						Method m1 = bestMatch.getHandlerMethod().getMethod();
						Method m2 = secondBestMatch.getHandlerMethod().getMethod();
						RequestPath path = exchange.getRequest().getPath();
						throw new IllegalStateException(
								"Ambiguous handler methods mapped for '" + path + "': {" + m1 + ", " + m2 + "}");
					}
				}
			}
			handleMatch(bestMatch.mapping, bestMatch.getHandlerMethod(), exchange);
			return bestMatch.getHandlerMethod();
		}
		else {
			return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), exchange);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, ServerWebExchange exchange) {
		for (T mapping : mappings) {
			T match = getMatchingMapping(mapping, exchange);
			if (match != null) {
				matches.add(new Match(match, this.mappingRegistry.getRegistrations().get(mapping)));
			}
		}
	}

	/**
	 * 在找到匹配的映射时调用。
	 * @param mapping the matching mapping
	 * @param handlerMethod the matching method
	 * @param exchange the current exchange
	 */
	protected void handleMatch(T mapping, HandlerMethod handlerMethod, ServerWebExchange exchange) {
		String lookupPath = exchange.getRequest().getPath().pathWithinApplication().value();
		exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * 在未找到匹配的映射时调用。
	 * @param mappings all registered mappings
	 * @param exchange the current exchange
	 * @return an alternative HandlerMethod or {@code null}
	 * @throws Exception provides details that can be translated into an error status code
	 */
	@Nullable
	protected HandlerMethod handleNoMatch(Set<T> mappings, ServerWebExchange exchange) throws Exception {
		return null;
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		return super.hasCorsConfigurationSource(handler) ||
				(handler instanceof HandlerMethod && this.mappingRegistry.getCorsConfiguration((HandlerMethod) handler) != null);
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, exchange);
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return ALLOW_CORS_CONFIG;
			}
			CorsConfiguration methodConfig = this.mappingRegistry.getCorsConfiguration(handlerMethod);
			corsConfig = (corsConfig != null ? corsConfig.combine(methodConfig) : methodConfig);
		}
		return corsConfig;
	}


	// Abstract template methods

	/**
	 * 给定类型是否是带有处理器方法的处理器。
	 * @param beanType the type of the bean being checked
	 * @return "true" if this a handler type, "false" otherwise.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * 为处理器方法提供映射。无法提供映射的方法不是处理器方法。
	 * @param method the method to provide a mapping for
	 * @param handlerType the handler type, possibly a subtype of the method's
	 * declaring class
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 返回不是模式的请求映射路径。
	 * @since 5.3
	 */
	protected Set<String> getDirectPaths(T mapping) {
		return Collections.emptySet();
	}

	/**
	 * 检查映射是否匹配当前请求，并返回一个（可能是新的）映射，其中包含与当前请求相关的条件。
	 * @param mapping the mapping to get a match for
	 * @param exchange the current exchange
	 * @return the match, or {@code null} if the mapping doesn't match
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, ServerWebExchange exchange);

	/**
	 * 返回用于对匹配映射排序的比较器。返回的比较器应将“更好”的匹配排在前面。
	 * @param exchange the current exchange
	 * @return the comparator (never {@code null})
	 */
	protected abstract Comparator<T> getMappingComparator(ServerWebExchange exchange);


	/**
	 * 维护所有映射到处理器方法的注册表，公开用于执行查找的方法并提供并发访问。
	 *
	 * <p>包级私有，供测试使用。
	 */
	class MappingRegistry {

		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

		private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();

		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		/**
		 * 返回所有注册项。
		 * @since 5.3
		 */
		public Map<T, MappingRegistration<T>> getRegistrations() {
			return this.registry;
		}

		/**
		 * 返回给定 URL 路径的匹配项。非线程安全。
		 * @since 5.3
		 * @see #acquireReadLock()
		 */
		@Nullable
		public List<T> getMappingsByDirectPath(ServerWebExchange exchange) {
			String path = exchange.getRequest().getPath().pathWithinApplication().value();
			return this.pathLookup.get(path);
		}

		/**
		 * 返回 CORS 配置。可安全并发使用。
		 */
		@Nullable
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		/**
		 * 在使用 getMappings 和 getMappingsByUrl 时获取读锁。
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
		 * 在使用 getMappings 和 getMappingsByUrl 之后释放读锁。
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}

		public void register(T mapping, Object handler, Method method) {
			this.readWriteLock.writeLock().lock();
			try {
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				validateMethodMapping(handlerMethod, mapping);

				Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
				for (String path : directPaths) {
					this.pathLookup.add(path, mapping);
				}

				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					corsConfig.validateAllowCredentials();
					this.corsLookup.put(handlerMethod, corsConfig);
				}

				this.registry.put(mapping,
						new MappingRegistration<>(mapping, handlerMethod, directPaths, corsConfig != null));
			}
			finally {
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
			MappingRegistration<T> registration = this.registry.get(mapping);
			HandlerMethod existingHandlerMethod = (registration != null ? registration.getHandlerMethod() : null);
			if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
						handlerMethod + "\nto " + mapping + ": There is already '" +
						existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
			}
		}

		public void unregister(T mapping) {
			this.readWriteLock.writeLock().lock();
			try {
				MappingRegistration<T> registration = this.registry.remove(mapping);
				if (registration == null) {
					return;
				}

				for (String path : registration.getDirectPaths()) {
					List<T> mappings = this.pathLookup.get(path);
					if (mappings != null) {
						mappings.remove(registration.getMapping());
						if (mappings.isEmpty()) {
							this.pathLookup.remove(path);
						}
					}
				}

				this.corsLookup.remove(registration.getHandlerMethod());
			}
			finally {
				this.readWriteLock.writeLock().unlock();
			}
		}
	}


	static class MappingRegistration<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		private final Set<String> directPaths;

		private final boolean corsConfig;

		public MappingRegistration(
				T mapping, HandlerMethod handlerMethod, @Nullable Set<String> directPaths, boolean corsConfig) {

			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.directPaths = (directPaths != null ? directPaths : Collections.emptySet());
			this.corsConfig = corsConfig;
		}

		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public Set<String> getDirectPaths() {
			return this.directPaths;
		}

		public boolean hasCorsConfig() {
			return this.corsConfig;
		}
	}


	/**
	 * 匹配的 HandlerMethod 及其映射的轻量包装，用于在当前请求的上下文中使用比较器比较最佳匹配。
	 */
	private class Match {

		private final T mapping;

		private final MappingRegistration<T> registration;

		public Match(T mapping, MappingRegistration<T> registration) {
			this.mapping = mapping;
			this.registration = registration;
		}

		public HandlerMethod getHandlerMethod() {
			return this.registration.getHandlerMethod();
		}

		public boolean hasCorsConfig() {
			return this.registration.hasCorsConfig();
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


	private static class PreFlightAmbiguousMatchHandler {

		@SuppressWarnings("unused")
		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
