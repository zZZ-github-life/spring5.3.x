/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.commonj;

import javax.naming.NamingException;

import commonj.timers.TimerManager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 访问 CommonJ {@link commonj.timers.TimerManager} 的基类。
 * 定义了通用的配置设置和通用的生命周期处理。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see commonj.timers.TimerManager
 * @deprecated as of 5.1, in favor of EE 7's
 * {@link org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler}
 */
@Deprecated
public abstract class TimerManagerAccessor extends JndiLocatorSupport
		implements InitializingBean, DisposableBean, Lifecycle {

	@Nullable
	private TimerManager timerManager;

	@Nullable
	private String timerManagerName;

	private boolean shared = false;


	/**
	 * 指定要委托的 CommonJ TimerManager。
	 * <p>请注意，给定 TimerManager 的生命周期将由
	 * 此 FactoryBean 管理。
	 * <p>另外（通常的做法），您可以指定目标
	 * TimerManager 的 JNDI 名称。
	 * @see #setTimerManagerName
	 */
	public void setTimerManager(TimerManager timerManager) {
		this.timerManager = timerManager;
	}

	/**
	 * 设置 CommonJ TimerManager 的 JNDI 名称。
	 * <p>这可以是完全限定的 JNDI 名称，也可以是相对于
	 * 当前环境命名上下文的 JNDI 名称（如果 "resourceRef" 设置为 "true"）。
	 * @see #setTimerManager
	 * @see #setResourceRef
	 */
	public void setTimerManagerName(String timerManagerName) {
		this.timerManagerName = timerManagerName;
	}

	/**
	 * 指定此 FactoryBean 获取的 TimerManager
	 * 是共享实例（"true"）还是独立实例（"false"）。
	 * 前者的生命周期应由应用服务器
	 * 管理，后者的生命周期由应用程序自行管理。
	 * <p>默认为 "false"，即管理一个独立的 TimerManager 实例。
	 * 这也是 CommonJ 规范建议应用服务器
	 * 通过 JNDI 查找提供的方式，通常在
	 * {@code web.xml} 中声明为类型为 {@code commonj.timers.TimerManager}
	 * 的 {@code resource-ref}，并将 {@code res-sharing-scope} 设置为 'Unshareable'。
	 * <p>如果您获取的是共享 TimerManager，请将此标志切换为 "true"，
	 * 通常通过指定已明确声明为 'Shareable' 的
	 * TimerManager 的 JNDI 位置。请注意，WebLogic 的
	 * 集群感知作业调度器也是共享的 TimerManager。
	 * <p>此 FactoryBean 处于共享模式或非共享模式的
	 * 唯一区别在于，只有在独立（非共享）实例的情况下，
	 * 它才会尝试暂停/恢复/停止底层的 TimerManager。
	 * 这只影响 {@link org.springframework.context.Lifecycle} 的支持
	 * 以及应用上下文的关闭。
	 * @see #stop()
	 * @see #start()
	 * @see #destroy()
	 * @see commonj.timers.TimerManager
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}


	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.timerManager == null) {
			if (this.timerManagerName == null) {
				throw new IllegalArgumentException("Either 'timerManager' or 'timerManagerName' must be specified");
			}
			this.timerManager = lookup(this.timerManagerName, TimerManager.class);
		}
	}

	/**
	 * 返回已配置的 TimerManager（如果有）。
	 * @return TimerManager；如果不可用，则为 {@code null}
	 */
	@Nullable
	protected final TimerManager getTimerManager() {
		return this.timerManager;
	}

	/**
	 * 获取实际使用的 TimerManager。
	 * @return TimerManager（永不为 {@code null}）
	 * @throws IllegalStateException 未设置 TimerManager 时
	 * @since 5.0
	 */
	protected TimerManager obtainTimerManager() {
		Assert.notNull(this.timerManager, "No TimerManager set");
		return this.timerManager;
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	/**
	 * 恢复底层的 TimerManager（如果不是共享的）。
	 * @see commonj.timers.TimerManager#resume()
	 */
	@Override
	public void start() {
		if (!this.shared) {
			obtainTimerManager().resume();
		}
	}

	/**
	 * 暂停底层的 TimerManager（如果不是共享的）。
	 * @see commonj.timers.TimerManager#suspend()
	 */
	@Override
	public void stop() {
		if (!this.shared) {
			obtainTimerManager().suspend();
		}
	}

	/**
	 * 如果底层的 TimerManager 既没有在暂停也没有在停止，
	 * 则认为其正在运行。
	 * @see commonj.timers.TimerManager#isSuspending()
	 * @see commonj.timers.TimerManager#isStopping()
	 */
	@Override
	public boolean isRunning() {
		TimerManager tm = obtainTimerManager();
		return (!tm.isSuspending() && !tm.isStopping());
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * 停止底层的 TimerManager（如果不是共享的）。
	 * @see commonj.timers.TimerManager#stop()
	 */
	@Override
	public void destroy() {
		// Stop the entire TimerManager, if necessary.
		if (this.timerManager != null && !this.shared) {
			// May return early, but at least we already cancelled all known Timers.
			this.timerManager.stop();
		}
	}

}
