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

package org.springframework.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import kotlinx.coroutines.CompletableDeferredKt;
import kotlinx.coroutines.Deferred;
import org.reactivestreams.Publisher;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.RxReactiveStreams;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * 适配器注册表，用于在各种异步/响应式类型（如 {@code CompletableFuture}、RxJava {@code Flowable}
 * 等）与 Reactive Streams {@link Publisher} 之间进行适配。
 *
 * <p>默认情况下，根据类路径的可用性，会为 Reactor、RxJava 3、{@link CompletableFuture}、
 * {@code Flow.Publisher} 以及 Kotlin Coroutines 的 {@code Deferred} 和 {@code Flow} 注册适配器。
 *
 * <p><strong>注意：</strong>从 Spring Framework 5.3.11 起，对 RxJava 1.x 和 2.x 的支持已弃用，建议
 * 改用 RxJava 3。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ReactiveAdapterRegistry {

	@Nullable
	private static volatile ReactiveAdapterRegistry sharedInstance;

	private static final boolean reactorPresent;

	private static final boolean rxjava1Present;

	private static final boolean rxjava2Present;

	private static final boolean rxjava3Present;

	private static final boolean flowPublisherPresent;

	private static final boolean kotlinCoroutinesPresent;

	private static final boolean mutinyPresent;

	static {
		ClassLoader classLoader = ReactiveAdapterRegistry.class.getClassLoader();
		reactorPresent = ClassUtils.isPresent("reactor.core.publisher.Flux", classLoader);
		flowPublisherPresent = ClassUtils.isPresent("java.util.concurrent.Flow.Publisher", classLoader);
		rxjava1Present = ClassUtils.isPresent("rx.Observable", classLoader) &&
				ClassUtils.isPresent("rx.RxReactiveStreams", classLoader);
		rxjava2Present = ClassUtils.isPresent("io.reactivex.Flowable", classLoader);
		rxjava3Present = ClassUtils.isPresent("io.reactivex.rxjava3.core.Flowable", classLoader);
		kotlinCoroutinesPresent = ClassUtils.isPresent("kotlinx.coroutines.reactor.MonoKt", classLoader);
		mutinyPresent = ClassUtils.isPresent("io.smallrye.mutiny.Multi", classLoader);
	}

	private final List<ReactiveAdapter> adapters = new ArrayList<>();


	/**
	 * 创建一个注册表并自动注册默认适配器。
	 * @see #getSharedInstance()
	 */
	public ReactiveAdapterRegistry() {
		// Reactor
		if (reactorPresent) {
			new ReactorRegistrar().registerAdapters(this);
			if (flowPublisherPresent) {
				// Java 9+ Flow.Publisher
				new ReactorJdkFlowAdapterRegistrar().registerAdapter(this);
			}
		}

		// RxJava
		if (rxjava1Present) {
			new RxJava1Registrar().registerAdapters(this);
		}
		if (rxjava2Present) {
			new RxJava2Registrar().registerAdapters(this);
		}
		if (rxjava3Present) {
			new RxJava3Registrar().registerAdapters(this);
		}

		// Kotlin Coroutines
		if (reactorPresent && kotlinCoroutinesPresent) {
			new CoroutinesRegistrar().registerAdapters(this);
		}

		// SmallRye Mutiny
		if (mutinyPresent) {
			new MutinyRegistrar().registerAdapters(this);
		}
	}


	/**
	 * 该注册表是否有任何适配器。
	 */
	public boolean hasAdapters() {
		return !this.adapters.isEmpty();
	}

	/**
	 * 注册一个响应式类型，以及用于与 Reactive Streams {@link Publisher} 相互适配的函数。这些函数的参数
	 * 假定其输入既不是 {@code null}，也不是 {@link Optional}。
	 */
	public void registerReactiveType(ReactiveTypeDescriptor descriptor,
			Function<Object, Publisher<?>> toAdapter, Function<Publisher<?>, Object> fromAdapter) {

		if (reactorPresent) {
			this.adapters.add(new ReactorAdapter(descriptor, toAdapter, fromAdapter));
		}
		else {
			this.adapters.add(new ReactiveAdapter(descriptor, toAdapter, fromAdapter));
		}
	}

	/**
	 * 获取给定响应式类型的适配器。
	 * @return 对应的适配器，如果没有可用的则为 {@code null}
	 */
	@Nullable
	public ReactiveAdapter getAdapter(Class<?> reactiveType) {
		return getAdapter(reactiveType, null);
	}

	/**
	 * 获取给定响应式类型的适配器。或者，如果提供了“源”对象，则改用其实际类型。
	 * @param reactiveType 响应式类型（如果给出了具体的源对象则可以为 {@code null}）
	 * @param source 响应式类型的实例（即要从中适配；如果指定了响应式类型则可以为 {@code null}）
	 * @return 对应的适配器，如果没有可用的则为 {@code null}
	 */
	@Nullable
	public ReactiveAdapter getAdapter(@Nullable Class<?> reactiveType, @Nullable Object source) {
		if (this.adapters.isEmpty()) {
			return null;
		}

		Object sourceToUse = (source instanceof Optional ? ((Optional<?>) source).orElse(null) : source);
		Class<?> clazz = (sourceToUse != null ? sourceToUse.getClass() : reactiveType);
		if (clazz == null) {
			return null;
		}
		for (ReactiveAdapter adapter : this.adapters) {
			if (adapter.getReactiveType() == clazz) {
				return adapter;
			}
		}
		for (ReactiveAdapter adapter : this.adapters) {
			if (adapter.getReactiveType().isAssignableFrom(clazz)) {
				return adapter;
			}
		}
		return null;
	}


	/**
	 * 返回一个共享的默认 {@code ReactiveAdapterRegistry} 实例，在需要时延迟构建。<p><b>注意：</b>为了
	 * 自定义目的，我们强烈建议传入一个长期存在且预先配置好的 {@code ReactiveAdapterRegistry} 实例。此
	 * 访问器仅作为回退方案，供那些希望在未提供实例时回退到默认实例的代码路径使用。
	 * @return 共享的 {@code ReactiveAdapterRegistry} 实例
	 * @since 5.0.2
	 */
	public static ReactiveAdapterRegistry getSharedInstance() {
		ReactiveAdapterRegistry registry = sharedInstance;
		if (registry == null) {
			synchronized (ReactiveAdapterRegistry.class) {
				registry = sharedInstance;
				if (registry == null) {
					registry = new ReactiveAdapterRegistry();
					sharedInstance = registry;
				}
			}
		}
		return registry;
	}


	/**
	 * ReactiveAdapter 的变体，会根据 {@link ReactiveTypeDescriptor#isMultiValue()} 将适配后的
	 * Publisher 包装为 {@link Flux} 或 {@link Mono}。在仅能获得流及流元素类型信息的地方（如编码器和解
	 * 码器）这一点很重要。
	 */
	private static class ReactorAdapter extends ReactiveAdapter {

		ReactorAdapter(ReactiveTypeDescriptor descriptor,
				Function<Object, Publisher<?>> toPublisherFunction,
				Function<Publisher<?>, Object> fromPublisherFunction) {

			super(descriptor, toPublisherFunction, fromPublisherFunction);
		}

		@Override
		public <T> Publisher<T> toPublisher(@Nullable Object source) {
			Publisher<T> publisher = super.toPublisher(source);
			return (isMultiValue() ? Flux.from(publisher) : Mono.from(publisher));
		}
	}


	private static class ReactorRegistrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			// Register Flux and Mono before Publisher...

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Mono.class, Mono::empty),
					source -> (Mono<?>) source,
					Mono::from);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Flux.class, Flux::empty),
					source -> (Flux<?>) source,
					Flux::from);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Publisher.class, Flux::empty),
					source -> (Publisher<?>) source,
					source -> source);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.nonDeferredAsyncValue(CompletionStage.class, EmptyCompletableFuture::new),
					source -> Mono.fromCompletionStage((CompletionStage<?>) source),
					source -> Mono.from(source).toFuture());
		}
	}


	private static class EmptyCompletableFuture<T> extends CompletableFuture<T> {

		EmptyCompletableFuture() {
			complete(null);
		}
	}


	private static class ReactorJdkFlowAdapterRegistrar {

		void registerAdapter(ReactiveAdapterRegistry registry) {
			// Reflectively access optional JDK 9+ API (for runtime compatibility with JDK 8)

			try {
				String publisherName = "java.util.concurrent.Flow.Publisher";
				Class<?> publisherClass = ClassUtils.forName(publisherName, getClass().getClassLoader());

				String adapterName = "reactor.adapter.JdkFlowAdapter";
				Class<?> flowAdapterClass = ClassUtils.forName(adapterName,  getClass().getClassLoader());

				Method toFluxMethod = flowAdapterClass.getMethod("flowPublisherToFlux", publisherClass);
				Method toFlowMethod = flowAdapterClass.getMethod("publisherToFlowPublisher", Publisher.class);
				Object emptyFlow = ReflectionUtils.invokeMethod(toFlowMethod, null, Flux.empty());

				registry.registerReactiveType(
						ReactiveTypeDescriptor.multiValue(publisherClass, () -> emptyFlow),
						source -> (Publisher<?>) ReflectionUtils.invokeMethod(toFluxMethod, null, source),
						publisher -> ReflectionUtils.invokeMethod(toFlowMethod, null, publisher));
			}
			catch (Throwable ex) {
				// Ignore
			}
		}
	}


	private static class RxJava1Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(rx.Observable.class, rx.Observable::empty),
					source -> RxReactiveStreams.toPublisher((rx.Observable<?>) source),
					RxReactiveStreams::toObservable);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(rx.Single.class),
					source -> RxReactiveStreams.toPublisher((rx.Single<?>) source),
					RxReactiveStreams::toSingle);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(rx.Completable.class, rx.Completable::complete),
					source -> RxReactiveStreams.toPublisher((rx.Completable) source),
					RxReactiveStreams::toCompletable);
		}
	}


	private static class RxJava2Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(io.reactivex.Flowable.class, io.reactivex.Flowable::empty),
					source -> (io.reactivex.Flowable<?>) source,
					io.reactivex.Flowable::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(io.reactivex.Observable.class, io.reactivex.Observable::empty),
					source -> ((io.reactivex.Observable<?>) source).toFlowable(io.reactivex.BackpressureStrategy.BUFFER),
					io.reactivex.Observable::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.Single.class),
					source -> ((io.reactivex.Single<?>) source).toFlowable(),
					io.reactivex.Single::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(io.reactivex.Maybe.class, io.reactivex.Maybe::empty),
					source -> ((io.reactivex.Maybe<?>) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source)
							.toObservable().singleElement());

			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(io.reactivex.Completable.class, io.reactivex.Completable::complete),
					source -> ((io.reactivex.Completable) source).toFlowable(),
					io.reactivex.Completable::fromPublisher);
		}
	}


	private static class RxJava3Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(
							io.reactivex.rxjava3.core.Flowable.class,
							io.reactivex.rxjava3.core.Flowable::empty),
					source -> (io.reactivex.rxjava3.core.Flowable<?>) source,
					io.reactivex.rxjava3.core.Flowable::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(
							io.reactivex.rxjava3.core.Observable.class,
							io.reactivex.rxjava3.core.Observable::empty),
					source -> ((io.reactivex.rxjava3.core.Observable<?>) source).toFlowable(
							io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER),
					io.reactivex.rxjava3.core.Observable::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.rxjava3.core.Single.class),
					source -> ((io.reactivex.rxjava3.core.Single<?>) source).toFlowable(),
					io.reactivex.rxjava3.core.Single::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(
							io.reactivex.rxjava3.core.Maybe.class,
							io.reactivex.rxjava3.core.Maybe::empty),
					source -> ((io.reactivex.rxjava3.core.Maybe<?>) source).toFlowable(),
					io.reactivex.rxjava3.core.Maybe::fromPublisher);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(
							io.reactivex.rxjava3.core.Completable.class,
							io.reactivex.rxjava3.core.Completable::complete),
					source -> ((io.reactivex.rxjava3.core.Completable) source).toFlowable(),
					io.reactivex.rxjava3.core.Completable::fromPublisher);
		}
	}


	private static class CoroutinesRegistrar {

		@SuppressWarnings("KotlinInternalInJava")
		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Deferred.class,
							() -> CompletableDeferredKt.CompletableDeferred(null)),
					source -> CoroutinesUtils.deferredToMono((Deferred<?>) source),
					source -> CoroutinesUtils.monoToDeferred(Mono.from(source)));

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(kotlinx.coroutines.flow.Flow.class, kotlinx.coroutines.flow.FlowKt::emptyFlow),
					source -> kotlinx.coroutines.reactor.ReactorFlowKt.asFlux((kotlinx.coroutines.flow.Flow<?>) source),
					kotlinx.coroutines.reactive.ReactiveFlowKt::asFlow);
		}
	}


	private static class MutinyRegistrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(
							io.smallrye.mutiny.Uni.class,
							() -> io.smallrye.mutiny.Uni.createFrom().nothing()),
					uni -> ((io.smallrye.mutiny.Uni<?>) uni).convert().toPublisher(),
					publisher -> io.smallrye.mutiny.Uni.createFrom().publisher(publisher));

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(
							io.smallrye.mutiny.Multi.class,
							() -> io.smallrye.mutiny.Multi.createFrom().empty()),
					multi -> (io.smallrye.mutiny.Multi<?>) multi,
					publisher -> io.smallrye.mutiny.Multi.createFrom().publisher(publisher));
		}
	}


	/**
	 * spring-core 类的 {@code BlockHoundIntegration}。<p>显式允许以下操作：
	 * <ul>
	 * <li>通过 {@link LocalVariableTableParameterNameDiscoverer} 读取类信息。
	 * <li>在 {@link ConcurrentReferenceHashMap} 内部加锁。
	 * </ul>
	 * @since 5.2.4
	 */
	public static class SpringCoreBlockHoundIntegration implements BlockHoundIntegration {

		@Override
		public void applyTo(BlockHound.Builder builder) {
			// Avoid hard references potentially anywhere in spring-core (no need for structural dependency)

			builder.allowBlockingCallsInside(
					"org.springframework.core.LocalVariableTableParameterNameDiscoverer", "inspectClass");

			String className = "org.springframework.util.ConcurrentReferenceHashMap$Segment";
			builder.allowBlockingCallsInside(className, "doTask");
			builder.allowBlockingCallsInside(className, "clear");
			builder.allowBlockingCallsInside(className, "restructure");
		}
	}

}
