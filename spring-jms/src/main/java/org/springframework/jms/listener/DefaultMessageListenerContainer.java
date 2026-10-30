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

package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.core.Constants;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.CachingDestinationResolver;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

/**
 * 消息监听器容器的变体，使用原生 JMS 客户端 API，具体来说是基于
 * {@code MessageConsumer.receive()} 调用的循环，同时也支持消息的事务性接收
 * （将消息注册到 XA 事务中）。它既适用于原生 JMS 环境，也适用于 Java EE 环境，
 * 配置上的差异极小。
 *
 * <p>这是一种简单但功能依然强大的消息监听器容器。
 * 在启动时，它会获取固定数量的 JMS Session 来调用监听器，
 * 并且可选地允许在运行时进行动态调整（最多达到某个最大数量）。
 * 与 {@link SimpleMessageListenerContainer} 一样，它的主要优点是运行时复杂度低，
 * 特别是对 JMS 提供者的要求极低：甚至不需要 JMS {@code ServerSessionPool} 设施。
 * 除此之外，在代理（broker）暂时不可用时它能够完全自我恢复，并且支持
 * 停止/重启以及对配置进行运行时修改。
 *
 * <p>实际的 {@code MessageListener} 执行发生在异步工作单元中，这些工作单元
 * 通过 Spring 的 {@link org.springframework.core.task.TaskExecutor TaskExecutor}
 * 抽象创建。默认情况下，启动时会根据 {@link #setConcurrentConsumers "concurrentConsumers"}
 * 设置创建指定数量的调用者任务。可以指定其他的 {@code TaskExecutor} 以与现有的
 * 线程池设施（例如 Java EE 服务器的线程池）集成，例如使用
 * {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor CommonJ WorkManager}。
 * 在原生 JMS 环境中，这些监听器线程中的每一个都会使用缓存的 JMS {@code Session}
 * 和 {@code MessageConsumer}（仅在发生故障时刷新），从而尽可能高效地利用
 * JMS 提供者的资源。
 *
 * <p>通过向 {@link #setTransactionManager "transactionManager"} 属性传入 Spring 的
 * {@link org.springframework.transaction.PlatformTransactionManager}，
 * 消息接收和监听器执行可以自动被事务包装。在 Java EE 环境中，这通常是
 * {@link org.springframework.transaction.jta.JtaTransactionManager}，并与从 JNDI
 * 获取的 JTA 感知的 JMS {@code ConnectionFactory} 配合使用（请查阅 Java EE 服务器文档）。
 * 请注意，如果指定了外部事务管理器，此监听器容器会为每个事务自动重新获取所有
 * JMS 句柄，以兼容所有 Java EE 服务器（特别是 JBoss）。这种非缓存行为可以通过
 * {@link #setCacheLevel "cacheLevel"} / {@link #setCacheLevelName "cacheLevelName"}
 * 属性覆盖，即使涉及外部事务管理器，也强制缓存 {@code Connection}
 * （或同时缓存 {@code Session} 和 {@code MessageConsumer}）。
 *
 * <p>可以通过指定大于 {@link #setConcurrentConsumers "concurrentConsumers"} 值的
 * {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"} 值来启用并发调用者数量的
 * 动态伸缩。由于后者的默认值为 1，你也可以简单地指定 "maxConcurrentConsumers" 为
 * 例如 5，这样在消息负载增加时会动态扩容到 5 个并发消费者，而在负载下降后
 * 又会动态收缩回标准消费者数量。可以考虑调整
 * {@link #setIdleTaskExecutionLimit "idleTaskExecutionLimit"} 设置来控制每个新任务的
 * 生命周期，以避免频繁地扩容和缩容，特别是当 {@code ConnectionFactory} 不池化
 * JMS {@code Session} 和/或 {@code TaskExecutor} 不池化线程时（请检查你的配置！）。
 * 请注意，动态伸缩首先只对队列真正有意义；对于主题（topic），你通常会坚持使用
 * 默认的 1 个消费者，否则你会在同一节点上多次收到同一条消息。
 *
 * <p><b>注意：不要将 Spring 的 {@link org.springframework.jms.connection.CachingConnectionFactory}
 * 与动态伸缩结合使用。</b>理想情况下，根本不要将其与消息监听器容器一起使用，
 * 因为通常更可取的做法是让监听器容器自身在其生命周期内处理适当的缓存。
 * 此外，停止和重启监听器容器只能与独立的、本地缓存的 Connection 一起工作，
 * 而不能与外部缓存的 Connection 一起工作。
 *
 * <p><b>强烈建议要么将 {@link #setSessionTransacted "sessionTransacted"} 设置为 "true"，
 * 要么指定外部 {@link #setTransactionManager "transactionManager"}。</b>
 * 有关确认模式和原生事务选项的详细信息，请参阅 {@link AbstractMessageListenerContainer}
 * 的 javadoc；有关配置外部事务管理器的详细信息，请参阅
 * {@link AbstractPollingMessageListenerContainer} 的 javadoc。请注意，对于默认的
 * "AUTO_ACKNOWLEDGE" 模式，此容器会在监听器执行之前应用自动消息确认，
 * 发生异常时不会重新投递。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setTransactionManager
 * @see #setCacheLevel
 * @see javax.jms.MessageConsumer#receive(long)
 * @see SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 */
public class DefaultMessageListenerContainer extends AbstractPollingMessageListenerContainer {

	/**
	 * 默认的线程名称前缀：“DefaultMessageListenerContainer-”。
	 */
	public static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(DefaultMessageListenerContainer.class) + "-";

	/**
	 * 默认的恢复间隔：5000 毫秒，即 5 秒。
	 */
	public static final long DEFAULT_RECOVERY_INTERVAL = 5000;


	/**
	 * 常量，表示完全不缓存任何 JMS 资源。
	 * @see #setCacheLevel
	 */
	public static final int CACHE_NONE = 0;

	/**
	 * 常量，表示针对每个监听器线程缓存一个共享的 JMS {@code Connection}。
	 * @see #setCacheLevel
	 */
	public static final int CACHE_CONNECTION = 1;

	/**
	 * 常量，表示针对每个监听器线程缓存一个共享的 JMS {@code Connection}
	 * 和一个 JMS {@code Session}。
	 * @see #setCacheLevel
	 */
	public static final int CACHE_SESSION = 2;

	/**
	 * 常量，表示针对每个监听器线程缓存一个共享的 JMS {@code Connection}、
	 * 一个 JMS {@code Session} 以及一个 JMS MessageConsumer。
	 * @see #setCacheLevel
	 */
	public static final int CACHE_CONSUMER = 3;

	/**
	 * 常量，表示自动选择合适的缓存级别（取决于事务管理策略）。
	 * @see #setCacheLevel
	 */
	public static final int CACHE_AUTO = 4;


	private static final Constants constants = new Constants(DefaultMessageListenerContainer.class);


	@Nullable
	private Executor taskExecutor;

	private BackOff backOff = new FixedBackOff(DEFAULT_RECOVERY_INTERVAL, Long.MAX_VALUE);

	private int cacheLevel = CACHE_AUTO;

	private int concurrentConsumers = 1;

	private int maxConcurrentConsumers = 1;

	private int maxMessagesPerTask = Integer.MIN_VALUE;

	private int idleConsumerLimit = 1;

	private int idleTaskExecutionLimit = 1;

	private int idleReceivesPerTaskLimit = Integer.MIN_VALUE;

	private final Set<AsyncMessageListenerInvoker> scheduledInvokers = new HashSet<>();

	private int activeInvokerCount = 0;

	private int registeredWithDestination = 0;

	private volatile boolean recovering;

	private volatile boolean interrupted;

	@Nullable
	private Runnable stopCallback;

	private Object currentRecoveryMarker = new Object();

	private final Object recoveryMonitor = new Object();


	/**
	 * 设置用于运行监听器线程的 Spring {@code TaskExecutor}。
	 * <p>默认是一个 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}，
	 * 它会根据指定的并发消费者数量启动若干新线程。
	 * <p>可以指定其他 {@code TaskExecutor} 以与现有线程池集成。请注意，只有当线程
	 * 以特定方式管理时（例如在 Java EE 环境中）这才真正有价值。普通的线程池
	 * 并没有太大价值，因为此监听器容器会在其整个生命周期内占用若干线程。
	 * @see #setConcurrentConsumers
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 指定用于计算恢复尝试之间间隔的 {@link BackOff} 实例。如果 {@link BackOffExecution}
	 * 实现返回 {@link BackOffExecution#STOP}，此监听器容器将不再尝试恢复。
	 * <p>设置此属性后，{@link #setRecoveryInterval(long) recovery interval} 将被忽略。
	 * @since 4.1
	 */
	public void setBackOff(BackOff backOff) {
		this.backOff = backOff;
	}

	/**
	 * 指定恢复尝试之间的间隔，单位为<b>毫秒</b>。
	 * 默认值为 5000 毫秒，即 5 秒。这是一个便捷方法，
	 * 用于创建具有指定间隔的 {@link FixedBackOff}。
	 * <p>如需更多恢复选项，请考虑改为指定 {@link BackOff} 实例。
	 * @see #setBackOff(BackOff)
	 * @see #handleListenerSetupFailure
	 */
	public void setRecoveryInterval(long recoveryInterval) {
		this.backOff = new FixedBackOff(recoveryInterval, Long.MAX_VALUE);
	}

	/**
	 * 以对应常量名称的形式（例如 "CACHE_CONNECTION"）指定此监听器容器允许
	 * 应用的缓存级别。
	 * @see #setCacheLevel
	 */
	public void setCacheLevelName(String constantName) throws IllegalArgumentException {
		if (!constantName.startsWith("CACHE_")) {
			throw new IllegalArgumentException("Only cache constants allowed");
		}
		setCacheLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * 指定此监听器容器允许应用的缓存级别。
	 * <p>如果指定了外部事务管理器，默认值为 {@link #CACHE_NONE}
	 * （在外部事务范围内重新获取所有资源），否则默认值为 {@link #CACHE_CONSUMER}
	 * （使用本地 JMS 资源）。
	 * <p>某些 Java EE 服务器只有在 JMS {@code Connection} 和 {@code Session} 是新获取的
	 * 情况下，才会将其 JMS 资源注册到正在进行中的 XA 事务，因此此监听器容器默认
	 * 不缓存其中任何一个。然而，取决于你的服务器在事务资源缓存方面的规则，
	 * 可以考虑即使配合外部事务管理器也将此设置切换为至少
	 * {@link #CACHE_CONNECTION} 或 {@link #CACHE_SESSION}。
	 * @see #CACHE_NONE
	 * @see #CACHE_CONNECTION
	 * @see #CACHE_SESSION
	 * @see #CACHE_CONSUMER
	 * @see #setCacheLevelName
	 * @see #setTransactionManager
	 */
	public void setCacheLevel(int cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	/**
	 * 返回此监听器容器允许应用的缓存级别。
	 */
	public int getCacheLevel() {
		return this.cacheLevel;
	}


	/**
	 * 通过 "lower-upper" 形式的字符串（例如 "5-10"）或简单的上限字符串
	 * （例如 "10"，此时下限为 1）指定并发限制。
	 * <p>此监听器容器将始终保有最小数量的消费者（{@link #setConcurrentConsumers}），
	 * 并在负载增加时逐步扩容到最大消费者数量 {@link #setMaxConcurrentConsumers}。
	 */
	@Override
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setConcurrentConsumers(Integer.parseInt(concurrency.substring(0, separatorIndex)));
				setMaxConcurrentConsumers(Integer.parseInt(concurrency.substring(separatorIndex + 1)));
			}
			else {
				setConcurrentConsumers(1);
				setMaxConcurrentConsumers(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported.");
		}
	}

	/**
	 * 指定要创建的并发消费者数量。默认值为 1。
	 * <p>为此设置指定更高的值会提高运行时计划的并发消费者的标准数量：
	 * 这实际上是任意给定时刻将要调度的最小并发消费者数量。这是一个静态设置；
	 * 如需动态伸缩，请考虑改为指定 "maxConcurrentConsumers" 设置。
	 * <p>为了扩展对来自队列的消息的消费能力，建议提高并发消费者数量。
	 * 但请注意，一旦注册了多个消费者，任何顺序保证都会丢失。
	 * 一般来说，对于低吞吐量的队列，请坚持使用 1 个消费者。
	 * <p><b>不要为主题（topic）提高并发消费者数量，
	 * 除非特定于供应商的配置措施明确允许这样做。</b>
	 * 在常规配置下，这会导致对同一条消息的并发消费，而这几乎从不可取。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @see #setMaxConcurrentConsumers
	 */
	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
		synchronized (this.lifecycleMonitor) {
			this.concurrentConsumers = concurrentConsumers;
			if (this.maxConcurrentConsumers < concurrentConsumers) {
				this.maxConcurrentConsumers = concurrentConsumers;
			}
		}
	}

	/**
	 * 返回 "concurrentConsumer" 设置。
	 * <p>这里返回的是当前配置的 "concurrentConsumers" 值；
	 * 当前已调度/活跃的消费者数量可能与之不同。
	 * @see #getScheduledConsumerCount()
	 * @see #getActiveConsumerCount()
	 */
	public final int getConcurrentConsumers() {
		synchronized (this.lifecycleMonitor) {
			return this.concurrentConsumers;
		}
	}

	/**
	 * 指定要创建的并发消费者的最大数量。默认值为 1。
	 * <p>如果此设置高于 "concurrentConsumers"，则监听器容器会在运行时动态调度
	 * 新的消费者，前提是遇到足够多的传入消息。一旦负载再次下降，
	 * 消费者数量将再次减少到标准级别（"concurrentConsumers"）。
	 * <p>为了扩展对来自队列的消息的消费能力，建议提高并发消费者数量。
	 * 但请注意，一旦注册了多个消费者，任何顺序保证都会丢失。
	 * 一般来说，对于低吞吐量的队列，请坚持使用 1 个消费者。
	 * <p><b>不要为主题（topic）提高并发消费者数量，
	 * 除非特定于供应商的配置措施明确允许这样做。</b>
	 * 在常规配置下，这会导致对同一条消息的并发消费，而这几乎从不可取。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @see #setConcurrentConsumers
	 */
	public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		Assert.isTrue(maxConcurrentConsumers > 0, "'maxConcurrentConsumers' value must be at least 1 (one)");
		synchronized (this.lifecycleMonitor) {
			this.maxConcurrentConsumers = Math.max(maxConcurrentConsumers, this.concurrentConsumers);
		}
	}

	/**
	 * 返回 "maxConcurrentConsumer" 设置。
	 * <p>这里返回的是当前配置的 "maxConcurrentConsumers" 值；
	 * 当前已调度/活跃的消费者数量可能与之不同。
	 * @see #getScheduledConsumerCount()
	 * @see #getActiveConsumerCount()
	 */
	public final int getMaxConcurrentConsumers() {
		synchronized (this.lifecycleMonitor) {
			return this.maxConcurrentConsumers;
		}
	}

	/**
	 * 指定在一个任务中处理的最大消息数量。
	 * 更具体地说，这限制每个任务的消息接收尝试次数，其中包括在达到超时之前
	 * 并未真正取到消息的接收迭代（参见 {@link #setReceiveTimeout "receiveTimeout"} 属性）。
	 * <p>在使用标准 TaskExecutor 的情况下，默认值为无限制（-1），
	 * 即复用原始的调用者线程直到关闭（代价是动态调度能力受限）。
	 * <p>如果 SchedulingTaskExecutor 表示偏好短生命周期任务，则默认值为 10。
	 * 可以在此处指定 10 到 100 之间的消息数，以在较长生命周期任务与
	 * 较短生命周期任务之间取得平衡。
	 * <p>长生命周期任务通过始终使用同一线程来避免频繁的线程上下文切换，
	 * 而短生命周期任务则允许线程池控制调度。因此，线程池通常更偏好短生命周期任务。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @see #setTaskExecutor
	 * @see #setReceiveTimeout
	 * @see org.springframework.scheduling.SchedulingTaskExecutor#prefersShortLivedTasks()
	 */
	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(maxMessagesPerTask != 0, "'maxMessagesPerTask' must not be 0");
		synchronized (this.lifecycleMonitor) {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}
	}

	/**
	 * 返回在一个任务中处理的最大消息数量。
	 */
	public final int getMaxMessagesPerTask() {
		synchronized (this.lifecycleMonitor) {
			return this.maxMessagesPerTask;
		}
	}

	/**
	 * 指定在任意给定时刻允许处于空闲状态的消费者数量上限。
	 * <p>该上限由 {@link #scheduleNewInvokerIfAppropriate} 方法使用，
	 * 以确定是否应创建新的调用者。提高该上限会使调用者被更积极地创建。
	 * 这对于更快地增加调用者数量可能很有用。
	 * <p>默认值为 1，即只有在当前没有任何现有调用者处于空闲状态时，
	 * 才调度一个新的调用者（它最初很可能是空闲的）。
	 */
	public void setIdleConsumerLimit(int idleConsumerLimit) {
		Assert.isTrue(idleConsumerLimit > 0, "'idleConsumerLimit' must be 1 or higher");
		synchronized (this.lifecycleMonitor) {
			this.idleConsumerLimit = idleConsumerLimit;
		}
	}

	/**
	 * 返回空闲消费者数量的上限。
	 */
	public final int getIdleConsumerLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleConsumerLimit;
		}
	}

	/**
	 * 指定消费者任务在其执行期间未收到任何消息时允许的空闲执行次数上限。
	 * 如果达到该上限，任务将关闭，并将接收工作留给其他正在执行的任务。
	 * <p>默认值为 1，即一旦某个任务没有收到消息就尽早关闭空闲资源。这仅适用于
	 * 动态调度；请参见 {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"} 设置。
	 * 最小消费者数量（参见 {@link #setConcurrentConsumers "concurrentConsumers"}）
	 * 无论如何都会保留到关闭为止。
	 * <p>在每次任务执行中，若干次消息接收尝试（取决于 "maxMessagesPerTask" 设置）
	 * 都会各自等待传入的消息（取决于 "receiveTimeout" 设置）。如果给定任务中的
	 * 所有这些接收尝试都未收到消息而返回，则该任务在接收消息方面被视为空闲。
	 * 这样的任务仍可能被重新调度；然而，一旦它达到指定的 "idleTaskExecutionLimit"，
	 * 它就会关闭（在动态伸缩的情况下）。
	 * <p>如果遇到过于频繁的扩容和缩容，请提高此上限。
	 * 该上限越高，空闲消费者保留的时间就越长，从而在出现新的消息负载时
	 * 避免重启消费者。或者，指定更高的 "maxMessagesPerTask" 和/或 "receiveTimeout"
	 * 值，这也会使空闲消费者保留更长时间（同时也会增加每个已调度任务的平均执行时间）。
	 * <p><b>此设置可以在运行时修改，例如通过 JMX。</b>
	 * @see #setMaxMessagesPerTask
	 * @see #setReceiveTimeout
	 */
	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		Assert.isTrue(idleTaskExecutionLimit > 0, "'idleTaskExecutionLimit' must be 1 or higher");
		synchronized (this.lifecycleMonitor) {
			this.idleTaskExecutionLimit = idleTaskExecutionLimit;
		}
	}

	/**
	 * 返回消费者任务空闲执行的次数上限。
	 */
	public final int getIdleTaskExecutionLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleTaskExecutionLimit;
		}
	}

	/**
	 * 在达到指定数量的空闲接收次数后，将消费者标记为“空闲”。空闲接收从接收器
	 * 在潜在的 {@link #setReceiveTimeout} 超时之后返回空消息的那一刻开始计数。
	 * 这样就有机会检查空闲任务计数是否超过 {@link #setIdleTaskExecutionLimit}，
	 * 并据此决定是否需要重新调度该任务，从而节省原本会被占用的资源。
	 * <p>此设置不同于 {@link #setMaxMessagesPerTask}，后者是在达到该上限后
	 * 无论收到的消息是空还是非空，都会释放并重新调度任务。如果想为每个任务
	 * 设置足够大的批次，但又需要在没有更多消息可处理时尽快释放任务，
	 * 仅靠该设置可能不够灵活。
	 * <p>此设置也不同于 {@link #setIdleTaskExecutionLimit}，后者决定的是
	 * 在被标记为空闲多少次迭代之后释放任务。
	 * <p>例如：如果将 {@link #setMaxMessagesPerTask} 设置为 '500'，
	 * 将 {@code #setIdleReceivesPerTaskLimit} 设置为 '60'，将 {@link #setReceiveTimeout}
	 * 设置为 '1000'，并将 {@link #setIdleTaskExecutionLimit} 设置为 '1'，
	 * 那么每个任务将处理 500 条消息，除非随后连续收到 60 条空闲消息，
	 * 此时任务将被标记为空闲并被释放。这也意味着在最后一条消息处理完毕后，
	 * 只要没有新消息出现，任务将在 60 秒后被释放。
	 * @since 5.3.5
	 * @see #setMaxMessagesPerTask
	 * @see #setReceiveTimeout
	 */
	public void setIdleReceivesPerTaskLimit(int idleReceivesPerTaskLimit) {
		Assert.isTrue(idleReceivesPerTaskLimit != 0, "'idleReceivesPerTaskLimit' must not be 0)");
		synchronized (this.lifecycleMonitor) {
			this.idleReceivesPerTaskLimit = idleReceivesPerTaskLimit;
		}
	}

	/**
	 * 返回在将消费者标记为“空闲”之前，单个任务中可接收的连续空消息的最大数量。
	 * @since 5.3.5
	 */
	public int getIdleReceivesPerTaskLimit() {
		synchronized (this.lifecycleMonitor) {
			return this.idleReceivesPerTaskLimit;
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of AbstractMessageListenerContainer's template methods
	//-------------------------------------------------------------------------

	@Override
	public void initialize() {
		// Adapt default cache level.
		if (this.cacheLevel == CACHE_AUTO) {
			this.cacheLevel = (getTransactionManager() != null ? CACHE_NONE : CACHE_CONSUMER);
		}

		// Prepare taskExecutor and maxMessagesPerTask.
		synchronized (this.lifecycleMonitor) {
			if (this.taskExecutor == null) {
				this.taskExecutor = createDefaultTaskExecutor();
			}
			else if (this.taskExecutor instanceof SchedulingTaskExecutor &&
					((SchedulingTaskExecutor) this.taskExecutor).prefersShortLivedTasks() &&
					this.maxMessagesPerTask == Integer.MIN_VALUE) {
				// TaskExecutor indicated a preference for short-lived tasks. According to
				// setMaxMessagesPerTask javadoc, we'll use 10 message per task in this case
				// unless the user specified a custom value.
				this.maxMessagesPerTask = 10;
			}
		}

		// Proceed with actual listener initialization.
		super.initialize();
	}

	/**
	 * 以 JMS Session 及关联的 MessageConsumer 的形式创建指定数量的并发消费者，
	 * 它们在单独的线程中运行。
	 * @see #scheduleNewInvoker
	 * @see #setTaskExecutor
	 */
	@Override
	protected void doInitialize() throws JMSException {
		synchronized (this.lifecycleMonitor) {
			for (int i = 0; i < this.concurrentConsumers; i++) {
				scheduleNewInvoker();
			}
		}
	}

	/**
	 * 销毁已注册的 JMS Session 及关联的 MessageConsumer。
	 */
	@Override
	protected void doShutdown() throws JMSException {
		logger.debug("Waiting for shutdown of message listener invokers");
		try {
			synchronized (this.lifecycleMonitor) {
				long receiveTimeout = getReceiveTimeout();
				long waitStartTime = System.currentTimeMillis();
				int waitCount = 0;
				while (this.activeInvokerCount > 0) {
					if (waitCount > 0 && !isAcceptMessagesWhileStopping() &&
							System.currentTimeMillis() - waitStartTime >= receiveTimeout) {
						// Unexpectedly some invokers are still active after the receive timeout period
						// -> interrupt remaining receive attempts since we'd reject the messages anyway
						for (AsyncMessageListenerInvoker scheduledInvoker : this.scheduledInvokers) {
							scheduledInvoker.interruptIfNecessary();
						}
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Still waiting for shutdown of " + this.activeInvokerCount +
								" message listener invokers (iteration " + waitCount + ")");
					}
					// Wait for AsyncMessageListenerInvokers to deactivate themselves...
					if (receiveTimeout > 0) {
						this.lifecycleMonitor.wait(receiveTimeout);
					}
					else {
						this.lifecycleMonitor.wait();
					}
					waitCount++;
				}
				// Clear remaining scheduled invokers, possibly left over as paused tasks
				for (AsyncMessageListenerInvoker scheduledInvoker : this.scheduledInvokers) {
					scheduledInvoker.clearResources();
				}
				this.scheduledInvokers.clear();
			}
		}
		catch (InterruptedException ex) {
			// Re-interrupt current thread, to allow other threads to react.
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 重写以重置停止回调（如果存在）。
	 */
	@Override
	public void start() throws JmsException {
		synchronized (this.lifecycleMonitor) {
			this.stopCallback = null;
		}
		super.start();
	}

	/**
	 * 停止此监听器容器，并在所有监听器处理实际停止后调用指定的回调。
	 * <p>注意：进一步的 {@code stop(runnable)} 调用（在处理实际停止之前）
	 * 将覆盖指定的回调。只有最新指定的回调会被调用。
	 * <p>如果后续的 {@link #start()} 调用在监听器容器完全停止之前将其重启，
	 * 则该回调根本不会被调用。
	 * @param callback 监听器处理完全停止后要调用的回调
	 * @throws JmsException 停止失败时
	 * @see #stop()
	 */
	@Override
	public void stop(Runnable callback) throws JmsException {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning() || this.stopCallback != null) {
				// Not started, already stopped, or previous stop attempt in progress
				// -> return immediately, no stop process to control anymore.
				callback.run();
				return;
			}
			this.stopCallback = callback;
		}
		stop();
	}

	/**
	 * 返回当前已调度的消费者数量。
	 * <p>该数量始终介于 "concurrentConsumers" 和 "maxConcurrentConsumers" 之间，
	 * 但可能高于 "activeConsumerCount"（如果某些消费者已调度但当前未执行）。
	 * @see #getConcurrentConsumers()
	 * @see #getMaxConcurrentConsumers()
	 * @see #getActiveConsumerCount()
	 */
	public final int getScheduledConsumerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.scheduledInvokers.size();
		}
	}

	/**
	 * 返回当前活跃的消费者数量。
	 * <p>该数量始终介于 "concurrentConsumers" 和 "maxConcurrentConsumers" 之间，
	 * 但可能低于 "scheduledConsumerCount"（如果某些消费者已调度但当前未执行）。
	 * @see #getConcurrentConsumers()
	 * @see #getMaxConcurrentConsumers()
	 * @see #getActiveConsumerCount()
	 */
	public final int getActiveConsumerCount() {
		synchronized (this.lifecycleMonitor) {
			return this.activeInvokerCount;
		}
	}

	/**
	 * 返回是否至少有一个消费者已与目标目的地建立了固定注册。
	 * 这在发布-订阅场景中特别有意义，因为在这些场景中，拥有一个已注册的实际消费者
	 * 可能很重要，它可以保证不会错过任何即将发布的消息。
	 * <p>可以在调用 {@link #start()} 之后轮询此方法，直到消费者的异步注册完成，
	 * 此时该方法将开始返回 {@code true}——前提是监听器容器确实建立过固定注册。
	 * 之后它将一直返回 {@code true} 直到关闭，因为此后容器会至少保留一个消费者注册。
	 * <p>请注意，监听器容器并不一定具有固定注册。它也可能在每次调用者执行时
	 * 不断重新创建消费者。这主要取决于 {@link #setCacheLevel cache level} 设置：
	 * 只有 {@link #CACHE_CONSUMER} 才会导致固定注册。
	 */
	public boolean isRegisteredWithDestination() {
		synchronized (this.lifecycleMonitor) {
			return (this.registeredWithDestination > 0);
		}
	}


	/**
	 * 创建默认的 TaskExecutor。在未指定显式 TaskExecutor 时调用。
	 * <p>默认实现构建一个 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}，
	 * 并使用指定的 bean 名称（如果没有指定 bean 名称则使用类名）作为线程名称前缀。
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	/**
	 * 调度一个新的调用者，增加此监听器容器已调度的调用者总数。
	 */
	private void scheduleNewInvoker() {
		AsyncMessageListenerInvoker invoker = new AsyncMessageListenerInvoker();
		if (rescheduleTaskIfNecessary(invoker)) {
			// This should always be true, since we're only calling this when active.
			this.scheduledInvokers.add(invoker);
		}
	}

	/**
	 * 根据 "cacheLevel" 设置决定是否使用共享的 JMS Connection。
	 * @see #setCacheLevel
	 * @see #CACHE_CONNECTION
	 */
	@Override
	protected final boolean sharedConnectionEnabled() {
		return (getCacheLevel() >= CACHE_CONNECTION);
	}

	/**
	 * 通过此监听器容器的 TaskExecutor 重新执行给定的任务。
	 * @see #setTaskExecutor
	 */
	@Override
	protected void doRescheduleTask(Object task) {
		Assert.state(this.taskExecutor != null, "No TaskExecutor available");
		this.taskExecutor.execute((Runnable) task);
	}

	/**
	 * 尝试调度一个新的调用者，因为我们知道消息正在流入……
	 * @see #scheduleNewInvokerIfAppropriate()
	 */
	@Override
	protected void messageReceived(Object invoker, Session session) {
		((AsyncMessageListenerInvoker) invoker).setIdle(false);
		scheduleNewInvokerIfAppropriate();
	}

	/**
	 * 将受影响的调用者标记为空闲。
	 */
	@Override
	protected void noMessageReceived(Object invoker, Session session) {
		((AsyncMessageListenerInvoker) invoker).setIdle(true);
	}

	/**
	 * 调度一个新的调用者，增加此监听器容器已调度的调用者总数，但仅在尚未达到
	 * 指定的 "maxConcurrentConsumers" 上限，且尚未达到指定的 "idleConsumerLimit"
	 * 上限时才这样做。
	 * <p>在收到消息后调用，以便在最初接收该消息的调用者处理该消息的同时进行扩容。
	 * @see #setTaskExecutor
	 * @see #getMaxConcurrentConsumers()
	 * @see #getIdleConsumerLimit()
	 */
	protected void scheduleNewInvokerIfAppropriate() {
		if (isRunning()) {
			resumePausedTasks();
			synchronized (this.lifecycleMonitor) {
				if (this.scheduledInvokers.size() < this.maxConcurrentConsumers &&
						getIdleInvokerCount() < this.idleConsumerLimit) {
					scheduleNewInvoker();
					if (logger.isDebugEnabled()) {
						logger.debug("Raised scheduled invoker count: " + this.scheduledInvokers.size());
					}
				}
			}
		}
	}

	/**
	 * 判断当前调用者是否应重新调度，考虑到它可能已有一段时间没有收到消息。
	 * @param idleTaskExecutionCount 该调用器任务已经（连续）累积的空闲执行次数
	 */
	private boolean shouldRescheduleInvoker(int idleTaskExecutionCount) {
		boolean superfluous =
				(idleTaskExecutionCount >= this.idleTaskExecutionLimit && getIdleInvokerCount() > 1);
		return (this.scheduledInvokers.size() <=
				(superfluous ? this.concurrentConsumers : this.maxConcurrentConsumers));
	}

	/**
	 * 判断此监听器容器的已调度调用者中当前是否有多个处于空闲状态。
	 */
	private int getIdleInvokerCount() {
		int count = 0;
		for (AsyncMessageListenerInvoker invoker : this.scheduledInvokers) {
			if (invoker.isIdle()) {
				count++;
			}
		}
		return count;
	}


	/**
	 * 重写以接受初始设置中的失败——将建立共享 Connection 的工作留给
	 * 异步调用者在首次访问时完成。
	 * @see #refreshConnectionUntilSuccessful()
	 */
	@Override
	protected void establishSharedConnection() {
		try {
			super.establishSharedConnection();
		}
		catch (Exception ex) {
			if (ex instanceof JMSException) {
				invokeExceptionListener((JMSException) ex);
			}
			logger.debug("Could not establish shared JMS Connection - " +
					"leaving it up to asynchronous invokers to establish a Connection as soon as possible", ex);
		}
	}

	/**
	 * 此实现在 {@code Connection.start()} 抛出异常后仍会继续执行，
	 * 依靠监听器执行适当的恢复。
	 */
	@Override
	protected void startSharedConnection() {
		try {
			super.startSharedConnection();
		}
		catch (Exception ex) {
			logger.debug("Connection start failed - relying on listeners to perform recovery", ex);
		}
	}

	/**
	 * 此实现在 {@code Connection.stop()} 抛出异常后仍会继续执行，
	 * 依靠监听器在重启后执行适当的恢复。
	 */
	@Override
	protected void stopSharedConnection() {
		try {
			super.stopSharedConnection();
		}
		catch (Exception ex) {
			logger.debug("Connection stop failed - relying on listeners to perform recovery after restart", ex);
		}
	}

	/**
	 * 处理监听器设置期间出现的给定异常。
	 * 在每个并发监听器中，每个此类异常都会调用此方法。
	 * <p>默认实现会在尚未恢复时以 warn 级别记录该异常，如果已经恢复则以 debug 级别记录。
	 * 可以在子类中重写。
	 * @param ex 要处理的异常
	 * @param alreadyRecovered 此前正在执行的监听器是否已从当前的监听器设置失败中恢复
	 * （这通常表示可忽略的后续失败，仅用于调试日志目的）
	 * @see #recoverAfterListenerSetupFailure()
	 */
	protected void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
		if (ex instanceof JMSException) {
			invokeExceptionListener((JMSException) ex);
		}
		if (ex instanceof SharedConnectionNotInitializedException) {
			if (!alreadyRecovered) {
				logger.debug("JMS message listener invoker needs to establish shared Connection");
			}
		}
		else {
			// Recovery during active operation..
			if (alreadyRecovered) {
				logger.debug("Setup of JMS message listener invoker failed - already recovered by other invoker", ex);
			}
			else {
				StringBuilder msg = new StringBuilder();
				msg.append("Setup of JMS message listener invoker failed for destination '");
				msg.append(getDestinationDescription()).append("' - trying to recover. Cause: ");
				msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage((JMSException) ex) : ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				}
				else {
					logger.warn(msg);
				}
			}
		}
	}

	/**
	 * 在监听器设置自身失败后恢复此监听器容器，例如重新建立底层的 Connection。
	 * <p>默认实现委托给 DefaultMessageListenerContainer 中具备恢复能力的
	 * {@link #refreshConnectionUntilSuccessful()} 方法，对于共享 Connection 和
	 * 非共享 Connection 两种情况，它都会尝试重新建立与 JMS 提供者的 Connection。
	 * @see #refreshConnectionUntilSuccessful()
	 * @see #refreshDestination()
	 */
	protected void recoverAfterListenerSetupFailure() {
		this.recovering = true;
		try {
			refreshConnectionUntilSuccessful();
			refreshDestination();
		}
		finally {
			this.recovering = false;
			this.interrupted = false;
		}
	}

	/**
	 * 刷新底层的 Connection，在尝试成功之前不会返回。无论是否使用共享 Connection
	 * 都会调用此方法，因此它需要要么操作共享 Connection，要么操作仅为验证目的
	 * 而临时建立的 Connection。
	 * <p>默认实现会持续重试，直到成功建立 Connection，只要此消息监听器容器仍在运行。
	 * 在重试之间应用指定的恢复间隔。
	 * @see #setRecoveryInterval
	 * @see #start()
	 * @see #stop()
	 */
	protected void refreshConnectionUntilSuccessful() {
		BackOffExecution execution = this.backOff.start();
		while (isRunning()) {
			try {
				if (sharedConnectionEnabled()) {
					refreshSharedConnection();
				}
				else {
					Connection con = createConnection();
					JmsUtils.closeConnection(con);
				}
				logger.debug("Successfully refreshed JMS Connection");
				break;
			}
			catch (Exception ex) {
				if (ex instanceof JMSException) {
					invokeExceptionListener((JMSException) ex);
				}
				StringBuilder msg = new StringBuilder();
				msg.append("Could not refresh JMS Connection for destination '");
				msg.append(getDestinationDescription()).append("' - retrying using ");
				msg.append(execution).append(". Cause: ");
				msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage((JMSException) ex) : ex.getMessage());
				if (logger.isDebugEnabled()) {
					logger.error(msg, ex);
				}
				else {
					logger.error(msg);
				}
			}
			if (!applyBackOffTime(execution)) {
				logger.error("Stopping container for destination '" + getDestinationDescription() +
						"': back-off policy does not allow for further attempts.");
				stop();
			}
		}
	}

	/**
	 * 刷新此监听器容器所操作的 JMS 目的地。
	 * <p>在监听器设置失败后调用，假定缓存的 Destination 对象可能已失效
	 * （这是 WebLogic JMS 上的典型情况）。
	 * <p>如果是 CachingDestinationResolver，默认实现会从 DestinationResolver 的缓存中
	 * 移除该目的地。
	 * @see #setDestinationName
	 * @see org.springframework.jms.support.destination.CachingDestinationResolver
	 */
	protected void refreshDestination() {
		String destName = getDestinationName();
		if (destName != null) {
			DestinationResolver destResolver = getDestinationResolver();
			if (destResolver instanceof CachingDestinationResolver) {
				((CachingDestinationResolver) destResolver).removeFromCache(destName);
			}
		}
	}

	/**
	 * 使用指定的 {@link BackOffExecution} 应用下一次退避时间。
	 * <p>如果已应用退避时间段并且应进行新的恢复尝试，则返回 {@code true}；
	 * 如果不应再进行尝试，则返回 {@code false}。
	 * @since 4.1
	 */
	protected boolean applyBackOffTime(BackOffExecution execution) {
		if (this.recovering && this.interrupted) {
			// Interrupted right before and still failing... give up.
			return false;
		}
		long interval = execution.nextBackOff();
		if (interval == BackOffExecution.STOP) {
			return false;
		}
		else {
			try {
				synchronized (this.lifecycleMonitor) {
					this.lifecycleMonitor.wait(interval);
				}
			}
			catch (InterruptedException interEx) {
				// Re-interrupt current thread, to allow other threads to react.
				Thread.currentThread().interrupt();
				if (this.recovering) {
					this.interrupted = true;
				}
			}
			return true;
		}
	}

	/**
	 * 返回此监听器容器当前是否处于恢复尝试中。
	 * <p>可用于检测恢复阶段，也可用于检测恢复阶段的结束——在之前发现
	 * {@code isRecovering()} 返回 {@code true} 之后，它会切换为 {@code false}。
	 * @see #recoverAfterListenerSetupFailure()
	 */
	public final boolean isRecovering() {
		return this.recovering;
	}


	//-------------------------------------------------------------------------
	// Inner classes used as internal adapters
	//-------------------------------------------------------------------------

	/**
	 * 执行循环 {@code MessageConsumer.receive()} 调用的 Runnable。
	 */
	private class AsyncMessageListenerInvoker implements SchedulingAwareRunnable {

		@Nullable
		private Session session;

		@Nullable
		private MessageConsumer consumer;

		@Nullable
		private Object lastRecoveryMarker;

		private boolean lastMessageSucceeded;

		private int idleTaskExecutionCount = 0;

		private volatile boolean idle = true;

		@Nullable
		private volatile Thread currentReceiveThread;

		@Override
		public void run() {
			synchronized (lifecycleMonitor) {
				activeInvokerCount++;
				lifecycleMonitor.notifyAll();
			}
			boolean messageReceived = false;
			try {
				int messageLimit = maxMessagesPerTask;
				int idleLimit = idleReceivesPerTaskLimit;
				if (messageLimit < 0 && idleLimit < 0) {
					messageReceived = executeOngoingLoop();
				}
				else {
					int messageCount = 0;
					int idleCount = 0;
					while (isRunning() && (messageLimit < 0 || messageCount < messageLimit) &&
							(idleLimit < 0 || idleCount < idleLimit)) {
						boolean currentReceived = invokeListener();
						messageReceived |= currentReceived;
						messageCount++;
						idleCount = (currentReceived ? 0 : idleCount + 1);
					}
				}
			}
			catch (Throwable ex) {
				clearResources();
				if (!this.lastMessageSucceeded) {
					// We failed more than once in a row or on startup -
					// wait before first recovery attempt.
					waitBeforeRecoveryAttempt();
				}
				this.lastMessageSucceeded = false;
				boolean alreadyRecovered = false;
				synchronized (recoveryMonitor) {
					if (this.lastRecoveryMarker == currentRecoveryMarker) {
						handleListenerSetupFailure(ex, false);
						recoverAfterListenerSetupFailure();
						currentRecoveryMarker = new Object();
					}
					else {
						alreadyRecovered = true;
					}
				}
				if (alreadyRecovered) {
					handleListenerSetupFailure(ex, true);
				}
			}
			finally {
				synchronized (lifecycleMonitor) {
					decreaseActiveInvokerCount();
					lifecycleMonitor.notifyAll();
				}
				if (!messageReceived) {
					this.idleTaskExecutionCount++;
				}
				else {
					this.idleTaskExecutionCount = 0;
				}
				synchronized (lifecycleMonitor) {
					if (!shouldRescheduleInvoker(this.idleTaskExecutionCount) || !rescheduleTaskIfNecessary(this)) {
						// We're shutting down completely.
						scheduledInvokers.remove(this);
						if (logger.isDebugEnabled()) {
							logger.debug("Lowered scheduled invoker count: " + scheduledInvokers.size());
						}
						lifecycleMonitor.notifyAll();
						clearResources();
					}
					else if (isRunning()) {
						int nonPausedConsumers = getScheduledConsumerCount() - getPausedTaskCount();
						if (nonPausedConsumers < 1) {
							logger.error("All scheduled consumers have been paused, probably due to tasks having been rejected. " +
									"Check your thread pool configuration! Manual recovery necessary through a start() call.");
						}
						else if (nonPausedConsumers < getConcurrentConsumers()) {
							logger.warn("Number of scheduled consumers has dropped below concurrentConsumers limit, probably " +
									"due to tasks having been rejected. Check your thread pool configuration! Automatic recovery " +
									"to be triggered by remaining consumers.");
						}
					}
				}
			}
		}

		private boolean executeOngoingLoop() throws JMSException {
			boolean messageReceived = false;
			boolean active = true;
			while (active) {
				synchronized (lifecycleMonitor) {
					boolean interrupted = false;
					boolean wasWaiting = false;
					while ((active = isActive()) && !isRunning()) {
						if (interrupted) {
							throw new IllegalStateException("Thread was interrupted while waiting for " +
									"a restart of the listener container, but container is still stopped");
						}
						if (!wasWaiting) {
							decreaseActiveInvokerCount();
						}
						wasWaiting = true;
						try {
							lifecycleMonitor.wait();
						}
						catch (InterruptedException ex) {
							// Re-interrupt current thread, to allow other threads to react.
							Thread.currentThread().interrupt();
							interrupted = true;
						}
					}
					if (wasWaiting) {
						activeInvokerCount++;
					}
					if (scheduledInvokers.size() > maxConcurrentConsumers) {
						active = false;
					}
				}
				if (active) {
					messageReceived = (invokeListener() || messageReceived);
				}
			}
			return messageReceived;
		}

		private boolean invokeListener() throws JMSException {
			this.currentReceiveThread = Thread.currentThread();
			try {
				initResourcesIfNecessary();
				boolean messageReceived = receiveAndExecute(this, this.session, this.consumer);
				this.lastMessageSucceeded = true;
				return messageReceived;
			}
			finally {
				this.currentReceiveThread = null;
			}
		}

		private void decreaseActiveInvokerCount() {
			activeInvokerCount--;
			if (stopCallback != null && activeInvokerCount == 0) {
				stopCallback.run();
				stopCallback = null;
			}
		}

		private void initResourcesIfNecessary() throws JMSException {
			if (getCacheLevel() <= CACHE_CONNECTION) {
				updateRecoveryMarker();
			}
			else {
				if (this.session == null && getCacheLevel() >= CACHE_SESSION) {
					updateRecoveryMarker();
					this.session = createSession(getSharedConnection());
				}
				if (this.consumer == null && getCacheLevel() >= CACHE_CONSUMER) {
					this.consumer = createListenerConsumer(this.session);
					synchronized (lifecycleMonitor) {
						registeredWithDestination++;
					}
				}
			}
		}

		private void updateRecoveryMarker() {
			synchronized (recoveryMonitor) {
				this.lastRecoveryMarker = currentRecoveryMarker;
			}
		}

		private void interruptIfNecessary() {
			Thread currentReceiveThread = this.currentReceiveThread;
			if (currentReceiveThread != null && !currentReceiveThread.isInterrupted()) {
				currentReceiveThread.interrupt();
			}
		}

		private void clearResources() {
			if (sharedConnectionEnabled()) {
				synchronized (sharedConnectionMonitor) {
					JmsUtils.closeMessageConsumer(this.consumer);
					JmsUtils.closeSession(this.session);
				}
			}
			else {
				JmsUtils.closeMessageConsumer(this.consumer);
				JmsUtils.closeSession(this.session);
			}
			if (this.consumer != null) {
				synchronized (lifecycleMonitor) {
					registeredWithDestination--;
				}
			}
			this.consumer = null;
			this.session = null;
		}

		/**
		 * 应用一次退避时间。在常规场景中，只有当我们未能从代理（broker）恢复时
		 * 才会应用退避。这段额外的等待时间可以避免在代理实际上正常运行、
		 * 但其他某些方面（即监听器特定方面）失败时出现突发的重试。
		 */
		private void waitBeforeRecoveryAttempt() {
			BackOffExecution execution = DefaultMessageListenerContainer.this.backOff.start();
			applyBackOffTime(execution);
		}

		@Override
		public boolean isLongLived() {
			return (maxMessagesPerTask < 0);
		}

		public void setIdle(boolean idle) {
			this.idle = idle;
		}

		public boolean isIdle() {
			return this.idle;
		}
	}

}
