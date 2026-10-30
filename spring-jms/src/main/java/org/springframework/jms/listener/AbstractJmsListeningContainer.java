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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 所有需要基于 JMS Connection（共享的，或每次尝试时新获取的）实现监听功能的
 * 容器的通用基类。它从 {@link org.springframework.jms.support.JmsAccessor} 基类
 * 继承基本的 Connection 和 Session 配置处理能力。
 *
 * <p>此类提供基本的生命周期管理，特别是共享 JMS Connection 的管理。
 * 子类应当接入此生命周期，实现 {@link #sharedConnectionEnabled()} 以及
 * {@link #doInitialize()} 和 {@link #doShutdown()} 模板方法。
 *
 * <p>此基类不假定任何特定的监听器编程模型或监听器调用者机制。它只是提供
 * 任何基于 JMS Connection/Session 的监听机制所需的通用运行时生命周期管理。
 *
 * <p>有关具体的监听器编程模型，请查看 {@link AbstractMessageListenerContainer} 子类。
 * 有关具体的监听器调用者机制，请查看 {@link DefaultMessageListenerContainer} 类。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #sharedConnectionEnabled()
 * @see #doInitialize()
 * @see #doShutdown()
 */
public abstract class AbstractJmsListeningContainer extends JmsDestinationAccessor
		implements BeanNameAware, DisposableBean, SmartLifecycle {

	@Nullable
	private String clientId;

	private boolean autoStartup = true;

	private int phase = DEFAULT_PHASE;

	@Nullable
	private String beanName;

	@Nullable
	private Connection sharedConnection;

	private boolean sharedConnectionStarted = false;

	protected final Object sharedConnectionMonitor = new Object();

	private boolean active = false;

	private volatile boolean running;

	private final List<Object> pausedTasks = new ArrayList<>();

	protected final Object lifecycleMonitor = new Object();


	/**
	 * 指定此容器创建和使用的共享 Connection 的 JMS 客户端 ID。
	 * <p>请注意，客户端 ID 在底层 JMS 提供者的所有活动 Connection 中必须唯一。
	 * 此外，只有原始 ConnectionFactory 尚未分配客户端 ID 时才能分配客户端 ID。
	 * @see javax.jms.Connection#setClientID
	 * @see #setConnectionFactory
	 */
	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	/**
	 * 返回此容器创建和使用的共享 Connection 的 JMS 客户端 ID（如果有）。
	 */
	@Nullable
	public String getClientId() {
		return this.clientId;
	}

	/**
	 * 设置初始化后是否自动启动容器。
	 * <p>默认值为 "true"；将其设置为 "false" 可以通过 {@link #start()} 方法手动启动。
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定此容器应启动和停止的阶段。启动顺序从低到高，关闭顺序则相反。
	 * 默认值为 Integer.MAX_VALUE，意味着此容器尽可能晚启动、尽可能早停止。
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此容器将启动和停止的阶段。
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 返回此监听器容器在其所属 bean 工厂中分配到的 bean 名称（如果有）。
	 */
	@Nullable
	protected final String getBeanName() {
		return this.beanName;
	}


	/**
	 * 委托给 {@link #validateConfiguration()} 和 {@link #initialize()}。
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		validateConfiguration();
		initialize();
	}

	/**
	 * 校验此容器的配置。
	 * <p>默认实现为空。可在子类中重写。
	 */
	protected void validateConfiguration() {
	}

	/**
	 * 在 BeanFactory 销毁容器实例时调用 {@link #shutdown()}。
	 * @see #shutdown()
	 */
	@Override
	public void destroy() {
		shutdown();
	}


	//-------------------------------------------------------------------------
	// Lifecycle methods for starting and stopping the container
	//-------------------------------------------------------------------------

	/**
	 * 初始化此容器。
	 * <p>创建一个 JMS Connection，启动 {@link javax.jms.Connection}
	 * （如果 {@link #setAutoStartup(boolean) "autoStartup"} 未被关闭），
	 * 并调用 {@link #doInitialize()}。
	 * @throws org.springframework.jms.JmsException 如果启动失败
	 */
	public void initialize() throws JmsException {
		try {
			synchronized (this.lifecycleMonitor) {
				this.active = true;
				this.lifecycleMonitor.notifyAll();
			}
			doInitialize();
		}
		catch (JMSException ex) {
			synchronized (this.sharedConnectionMonitor) {
				ConnectionFactoryUtils.releaseConnection(this.sharedConnection, getConnectionFactory(), this.autoStartup);
				this.sharedConnection = null;
			}
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * 停止共享 Connection，调用 {@link #doShutdown()}，
	 * 并关闭此容器。
	 * @throws JmsException 如果关闭失败
	 */
	public void shutdown() throws JmsException {
		logger.debug("Shutting down JMS listener container");
		boolean wasRunning;
		synchronized (this.lifecycleMonitor) {
			wasRunning = this.running;
			this.running = false;
			this.active = false;
			this.pausedTasks.clear();
			this.lifecycleMonitor.notifyAll();
		}

		// Stop shared Connection early, if necessary.
		if (wasRunning && sharedConnectionEnabled()) {
			try {
				stopSharedConnection();
			}
			catch (Throwable ex) {
				logger.debug("Could not stop JMS Connection on shutdown", ex);
			}
		}

		// Shut down the invokers.
		try {
			doShutdown();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			if (sharedConnectionEnabled()) {
				synchronized (this.sharedConnectionMonitor) {
					ConnectionFactoryUtils.releaseConnection(this.sharedConnection, getConnectionFactory(), false);
					this.sharedConnection = null;
				}
			}
		}
	}

	/**
	 * 返回此容器当前是否处于活动状态，即是否已设置但尚未关闭。
	 */
	public final boolean isActive() {
		synchronized (this.lifecycleMonitor) {
			return this.active;
		}
	}

	/**
	 * 启动此容器。
	 * @throws JmsException 如果启动失败
	 * @see #doStart
	 */
	@Override
	public void start() throws JmsException {
		try {
			doStart();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * 启动共享 Connection（如果有），并通知所有调用者任务。
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @see #startSharedConnection
	 */
	protected void doStart() throws JMSException {
		// Lazily establish a shared Connection, if necessary.
		if (sharedConnectionEnabled()) {
			establishSharedConnection();
		}

		// Reschedule paused tasks, if any.
		synchronized (this.lifecycleMonitor) {
			this.running = true;
			this.lifecycleMonitor.notifyAll();
			resumePausedTasks();
		}

		// Start the shared Connection, if any.
		if (sharedConnectionEnabled()) {
			startSharedConnection();
		}
	}

	/**
	 * 停止此容器。
	 * @throws JmsException 如果停止失败
	 * @see #doStop
	 */
	@Override
	public void stop() throws JmsException {
		try {
			doStop();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * 通知所有调用者任务，并停止共享 Connection（如果有）。
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @see #stopSharedConnection
	 */
	protected void doStop() throws JMSException {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.lifecycleMonitor.notifyAll();
		}

		if (sharedConnectionEnabled()) {
			stopSharedConnection();
		}
	}

	/**
	 * 判断此容器当前是否正在运行，即是否已启动且尚未停止。
	 * @see #start()
	 * @see #stop()
	 * @see #runningAllowed()
	 */
	@Override
	public final boolean isRunning() {
		return (this.running && runningAllowed());
	}

	/**
	 * 检查此容器的监听器通常是否被允许运行。
	 * <p>此实现始终返回 {@code true}；默认的“运行中”状态完全由
	 * {@link #start()} / {@link #stop()} 决定。
	 * <p>子类可以重写此方法，以检查阻止监听器实际运行的临时条件。换句话说，
	 * 它们可以对“运行中”状态施加进一步的限制，如果此类限制阻止监听器运行，
	 * 则返回 {@code false}。
	 */
	protected boolean runningAllowed() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Management of a shared JMS Connection
	//-------------------------------------------------------------------------

	/**
	 * 为此容器建立共享 Connection。
	 * <p>默认实现委托给 {@link #createSharedConnection()}，它只尝试一次，
	 * 如果失败则抛出异常。可以通过重写来加入恢复流程，持续重试
	 * 直到成功建立 Connection。
	 * @throws JMSException 如果由 JMS API 方法抛出
	 */
	protected void establishSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			if (this.sharedConnection == null) {
				this.sharedConnection = createSharedConnection();
				logger.debug("Established shared JMS Connection");
			}
		}
	}

	/**
	 * 刷新此容器持有的共享 Connection。
	 * <p>在启动时以及在调用者设置和/或执行期间发生基础设施异常后调用。
	 * @throws JMSException 如果由 JMS API 方法抛出
	 */
	protected final void refreshSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			ConnectionFactoryUtils.releaseConnection(
					this.sharedConnection, getConnectionFactory(), this.sharedConnectionStarted);
			this.sharedConnection = null;
			this.sharedConnection = createSharedConnection();
			if (this.sharedConnectionStarted) {
				this.sharedConnection.start();
			}
		}
	}

	/**
	 * 为此容器创建一个共享 Connection。
	 * <p>默认实现创建一个标准 Connection，并通过 {@link #prepareSharedConnection} 进行准备。
	 * @return 准备好的 Connection
	 * @throws JMSException 如果创建失败
	 */
	protected Connection createSharedConnection() throws JMSException {
		Connection con = createConnection();
		try {
			prepareSharedConnection(con);
			return con;
		}
		catch (JMSException ex) {
			JmsUtils.closeConnection(con);
			throw ex;
		}
	}

	/**
	 * 准备给定的 Connection，它将作为此容器的共享 Connection 被注册。
	 * <p>默认实现会设置指定的客户端 ID（如果有）。
	 * 子类可以重写此方法以应用更多设置。
	 * @param connection 要准备的 Connection
	 * @throws JMSException 如果准备工作失败
	 * @see #getClientId()
	 */
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		String clientId = getClientId();
		if (clientId != null) {
			connection.setClientID(clientId);
		}
	}

	/**
	 * 启动共享 Connection。
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @see javax.jms.Connection#start()
	 */
	protected void startSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			this.sharedConnectionStarted = true;
			if (this.sharedConnection != null) {
				try {
					this.sharedConnection.start();
				}
				catch (javax.jms.IllegalStateException ex) {
					logger.debug("Ignoring Connection start exception - assuming already started: " + ex);
				}
			}
		}
	}

	/**
	 * 停止共享 Connection。
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @see javax.jms.Connection#start()
	 */
	protected void stopSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			this.sharedConnectionStarted = false;
			if (this.sharedConnection != null) {
				try {
					this.sharedConnection.stop();
				}
				catch (javax.jms.IllegalStateException ex) {
					logger.debug("Ignoring Connection stop exception - assuming already stopped: " + ex);
				}
			}
		}
	}

	/**
	 * 返回此容器维护的共享 JMS Connection。初始化之后可用。
	 * @return 共享的 Connection（永不为 {@code null}）
	 * @throws IllegalStateException 如果此容器未维护共享 Connection，
	 * 或该 Connection 尚未初始化
	 * @see #sharedConnectionEnabled()
	 */
	protected final Connection getSharedConnection() {
		if (!sharedConnectionEnabled()) {
			throw new IllegalStateException(
					"This listener container does not maintain a shared Connection");
		}
		synchronized (this.sharedConnectionMonitor) {
			if (this.sharedConnection == null) {
				throw new SharedConnectionNotInitializedException(
						"This listener container's shared Connection has not been initialized yet");
			}
			return this.sharedConnection;
		}
	}


	//-------------------------------------------------------------------------
	// Management of paused tasks
	//-------------------------------------------------------------------------

	/**
	 * 接收给定的任务对象并重新调度它：如果此容器当前正在运行，则立即调度；
	 * 否则等到此容器重新启动后再调度。
	 * <p>如果此容器已经关闭，则该任务根本不会被重新调度。
	 * @param task 要重新调度的任务对象
	 * @return 任务是否已重新调度
	 * （立即调度或为此容器的重启而调度）
	 * @see #doRescheduleTask
	 */
	protected final boolean rescheduleTaskIfNecessary(Object task) {
		if (this.running) {
			try {
				doRescheduleTask(task);
			}
			catch (RuntimeException ex) {
				logRejectedTask(task, ex);
				this.pausedTasks.add(task);
			}
			return true;
		}
		else if (this.active) {
			this.pausedTasks.add(task);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 尝试恢复所有已暂停的任务。
	 * 重新调度失败的任务只会保持暂停状态。
	 */
	protected void resumePausedTasks() {
		synchronized (this.lifecycleMonitor) {
			if (!this.pausedTasks.isEmpty()) {
				for (Iterator<?> it = this.pausedTasks.iterator(); it.hasNext();) {
					Object task = it.next();
					try {
						doRescheduleTask(task);
						it.remove();
						if (logger.isDebugEnabled()) {
							logger.debug("Resumed paused task: " + task);
						}
					}
					catch (RuntimeException ex) {
						logRejectedTask(task, ex);
						// Keep the task in paused mode...
					}
				}
			}
		}
	}

	/**
	 * 确定当前已暂停的任务数量（如果有）。
	 */
	public int getPausedTaskCount() {
		synchronized (this.lifecycleMonitor) {
			return this.pausedTasks.size();
		}
	}

	/**
	 * 立即重新调度给定的任务对象。
	 * <p>如果子类调用了 {@code rescheduleTaskIfNecessary}，则需要实现此方法。
	 * 此实现会抛出 UnsupportedOperationException。
	 * @param task 要重新调度的任务对象
	 * @see #rescheduleTaskIfNecessary
	 */
	protected void doRescheduleTask(Object task) {
		throw new UnsupportedOperationException(
				ClassUtils.getShortName(getClass()) + " does not support rescheduling of tasks");
	}

	/**
	 * 记录被 {@link #doRescheduleTask} 拒绝的任务。
	 * <p>默认实现只是以 warn 级别记录一条相应消息。
	 * @param task 被拒绝的任务对象
	 * @param ex 从 {@link #doRescheduleTask} 抛出的异常
	 */
	protected void logRejectedTask(Object task, RuntimeException ex) {
		if (logger.isWarnEnabled()) {
			logger.warn("Listener container task [" + task + "] has been rejected and paused: " + ex);
		}
	}


	//-------------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	//-------------------------------------------------------------------------

	/**
	 * 返回此容器基类是否应维护共享的 JMS Connection。
	 * @see #getSharedConnection()
	 */
	protected abstract boolean sharedConnectionEnabled();

	/**
	 * 在此容器内注册所有调用者。
	 * <p>子类需要针对其特定的调用者管理流程实现此方法。
	 * <p>共享的 JMS Connection（如果有）此时已经启动。
	 * @throws JMSException 如果注册失败
	 * @see #getSharedConnection()
	 */
	protected abstract void doInitialize() throws JMSException;

	/**
	 * 关闭已注册的调用者。
	 * <p>子类需要针对其特定的调用者管理流程实现此方法。
	 * <p>共享的 JMS Connection（如果有）将在此<i>之后</i>自动关闭。
	 * @throws JMSException 如果关闭失败
	 * @see #shutdown()
	 */
	protected abstract void doShutdown() throws JMSException;


	/**
	 * 表示此容器的共享 JMS Connection 初始设置失败的异常。
	 * 它向调用者表明，调用者需要在首次访问时自行建立共享 Connection。
	 */
	@SuppressWarnings("serial")
	public static class SharedConnectionNotInitializedException extends RuntimeException {

		/**
		 * 创建一个新的 SharedConnectionNotInitializedException。
		 * @param msg 详细消息
		 */
		protected SharedConnectionNotInitializedException(String msg) {
			super(msg);
		}
	}

}
