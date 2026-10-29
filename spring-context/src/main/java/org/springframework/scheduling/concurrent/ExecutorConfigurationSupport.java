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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 用于设置 {@link java.util.concurrent.ExecutorService}（通常是
 * {@link java.util.concurrent.ThreadPoolExecutor} 或
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}）的基类。定义了通用配置设置和通用生命周
 * 期处理。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
		implements BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThreadFactory threadFactory = this;

	private boolean threadNamePrefixSet = false;

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean waitForTasksToCompleteOnShutdown = false;

	private long awaitTerminationMillis = 0;

	@Nullable
	private String beanName;

	@Nullable
	private ExecutorService executor;


	/**
	 * 设置用于 ExecutorService 线程池的 ThreadFactory。默认值为底层 ExecutorService 的默认线程工厂。
	 * <p>在 Java EE 7 或其他支持 JSR-236 的受管环境中，可考虑指定位于 JNDI 中的 ManagedThreadFactory：
	 * 默认情况下可在 "java:comp/DefaultManagedThreadFactory" 找到。可使用 XML 中的 "jee:jndi-lookup"
	 * 命名空间元素，或以编程方式使用 {@link org.springframework.jndi.JndiLocatorDelegate} 进行便捷查找。
	 * 另外，也可考虑使用 Spring 的 {@link DefaultManagedAwareThreadFactory}，在找不到托管线程工厂时
	 * 它会回退到本地线程。
	 * @see java.util.concurrent.Executors#defaultThreadFactory()
	 * @see javax.enterprise.concurrent.ManagedThreadFactory
	 * @see DefaultManagedAwareThreadFactory
	 */
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
	}

	@Override
	public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	/**
	 * 设置用于 ExecutorService 的 RejectedExecutionHandler。默认值为 ExecutorService 的默认中止策略。
	 * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
	 */
	public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * 设置关闭时是否等待已调度任务完成，即不中断正在运行的任务，并执行队列中的所有任务。
	 * <p>默认值为 "false"，即通过中断正在执行的任务并清空队列来立即关闭。如果你更希望任务全部完成、并
	 * 愿意接受更长的关闭阶段，请将此标志切换为 "true"。
	 * <p>请注意，在正在执行的任务完成期间，Spring 容器的关闭过程会继续进行。如果你希望此执行器阻塞等待
	 * 任务终止，然后容器其余部分才继续关闭（例如为了维持任务可能需要的其他资源），请改为设置或在设置此
	 * 属性的同时设置 {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} 属性。
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	/**
	 * 设置此执行器在关闭时最多应阻塞多少秒，以等待剩余任务完成执行，然后容器其余部分才继续关闭。如果剩
	 * 余任务可能需要访问同样由容器管理的其他资源，这一点尤其有用。
	 * <p>默认情况下，此执行器根本不会等待任务终止。它要么立即关闭，中断正在执行的任务并清空剩余任务队
	 * 列；要么，如果 {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
	 * 标志已设为 {@code true}，它会在容器其余部分关闭的同时，继续完整执行所有正在执行的任务以及队列中
	 * 的所有剩余任务。
	 * <p>无论哪种情况，如果你通过此属性指定了等待终止的期限，此执行器将最多等待给定的时间，以等待任务
	 * 终止。经验法则是：如果你同时将 "waitForTasksToCompleteOnShutdown" 设为 {@code true}，请在此处指
	 * 定明显更长的超时时间，因为队列中的所有剩余任务仍会被执行——这与默认的关闭行为不同，后者只是等待当
	 * 前正在执行、且不响应线程中断的任务。
	 * @see #setAwaitTerminationMillis
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#awaitTermination
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationMillis = awaitTerminationSeconds * 1000L;
	}

	/**
	 * {@link #setAwaitTerminationSeconds} 的变体，提供毫秒精度。
	 * @since 5.2.4
	 * @see #setAwaitTerminationSeconds
	 */
	public void setAwaitTerminationMillis(long awaitTerminationMillis) {
		this.awaitTerminationMillis = awaitTerminationMillis;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * 在容器应用所有属性值之后调用 {@code initialize()}。
	 * @see #initialize()
	 */
	@Override
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * 设置 ExecutorService。
	 */
	public void initialize() {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
	}

	/**
	 * 创建目标 {@link java.util.concurrent.ExecutorService} 实例。由 {@code afterPropertiesSet} 调用。
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ExecutorService instance
	 * @see #afterPropertiesSet()
	 */
	protected abstract ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);


	/**
	 * 当 BeanFactory 销毁任务执行器实例时调用 {@code shutdown}。
	 * @see #shutdown()
	 */
	@Override
	public void destroy() {
		shutdown();
	}

	/**
	 * 对底层 ExecutorService 执行关闭。
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	public void shutdown() {
		if (logger.isDebugEnabled()) {
			logger.debug("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.executor != null) {
			if (this.waitForTasksToCompleteOnShutdown) {
				this.executor.shutdown();
			}
			else {
				for (Runnable remainingTask : this.executor.shutdownNow()) {
					cancelRemainingTask(remainingTask);
				}
			}
			awaitTerminationIfNecessary(this.executor);
		}
	}

	/**
	 * 取消给定的、从未开始执行的剩余任务，该任务由 {@link ExecutorService#shutdownNow()} 返回。
	 * @param task the task to cancel (typically a {@link RunnableFuture})
	 * @since 5.0.5
	 * @see #shutdown()
	 * @see RunnableFuture#cancel(boolean)
	 */
	protected void cancelRemainingTask(Runnable task) {
		if (task instanceof Future) {
			((Future<?>) task).cancel(true);
		}
	}

	/**
	 * 根据 {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} 属性的值等待执行器终止。
	 */
	private void awaitTerminationIfNecessary(ExecutorService executor) {
		if (this.awaitTerminationMillis > 0) {
			try {
				if (!executor.awaitTermination(this.awaitTerminationMillis, TimeUnit.MILLISECONDS)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Timed out while waiting for executor" +
								(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
					}
				}
			}
			catch (InterruptedException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Interrupted while waiting for executor" +
							(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
				}
				Thread.currentThread().interrupt();
			}
		}
	}

}
