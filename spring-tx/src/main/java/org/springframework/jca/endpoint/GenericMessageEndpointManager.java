/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jca.endpoint;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 在 Spring 应用程序上下文中管理 JCA 1.7 消息端点的通用 bean，
 * 作为应用程序上下文生命周期的一部分激活和停用端点。
 *
 * <p>此类完全是通用的，可以与任何 ResourceAdapter、任何 MessageEndpointFactory
 * 和任何 ActivationSpec 一起使用。可以通过标准 bean 风格进行配置，
 * 例如通过 Spring 的 XML bean 定义格式，如下所示：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 * 	 &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 * 	 &lt;property name="messageEndpointFactory"&gt;
 *     &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *       &lt;property name="messageListener" ref="messageListener"/&gt;
 *     &lt;/bean&gt;
 * 	 &lt;/property&gt;
 * 	 &lt;property name="activationSpec"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *       &lt;property name="destination" value="myQueue"/&gt;
 *       &lt;property name="destinationType" value="javax.jms.Queue"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 在此示例中，使用 Spring 自己的 {@link GenericMessageEndpointFactory} 指向
 * 标准消息监听器对象，该对象恰好被指定的目标 ResourceAdapter 支持：
 * 在这种情况下，JMS {@link javax.jms.MessageListener} 对象由 ActiveMQ 消息代理支持，
 * 定义为 Spring bean：
 *
 * <pre class="code">
 * &lt;bean id="messageListener" class="com.myorg.messaging.myMessageListener"&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 *
 * 目标 ResourceAdapter 可以配置为本地 Spring bean（典型情况）或从 JNDI 获取（例如在 WebLogic 上）。
 * 对于上面的示例，可以如下定义本地 ResourceAdapter bean（匹配上面的 "resourceAdapter" bean 引用）：
 *
 * <pre class="code">
 * &lt;bean id="resourceAdapter" class="org.springframework.jca.support.ResourceAdapterFactoryBean"&gt;
 *   &lt;property name="resourceAdapter"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQResourceAdapter"&gt;
 *       &lt;property name="serverUrl" value="tcp://localhost:61616"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="workManager"&gt;
 *     &lt;bean class="org.springframework.jca.work.SimpleTaskWorkManager"/&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 对于不同的目标资源，配置只需指向不同的 ResourceAdapter 和不同的 ActivationSpec 对象
 * （两者都特定于资源提供者），以及可能不同的消息监听器
 * （例如，基于 JCA 公共客户端接口的资源适配器的 CCI {@link javax.resource.cci.MessageListener}）。
 *
 * <p>异步执行策略可以通过 ResourceAdapterFactoryBean 的 "workManager" 属性进行自定义
 * （如上所示）。查看 {@link org.springframework.jca.work.SimpleTaskWorkManager} 的 javadoc
 * 了解其配置选项；或者，可以使用任何其他符合 JCA 的 WorkManager（例如 Geronimo 的）。
 *
 * <p>事务执行是由具体消息端点负责的职责，由指定的 MessageEndpointFactory 构建。
 * {@link GenericMessageEndpointFactory} 通过其 "transactionManager" 属性支持 XA 事务参与，
 * 通常指定 Spring {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 或普通的 {@link javax.transaction.TransactionManager} 实现。
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 * 	 &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 * 	 &lt;property name="messageEndpointFactory"&gt;
 *     &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *       &lt;property name="messageListener" ref="messageListener"/&gt;
 *       &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *     &lt;/bean&gt;
 * 	 &lt;/property&gt;
 * 	 &lt;property name="activationSpec"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *       &lt;property name="destination" value="myQueue"/&gt;
 *       &lt;property name="destinationType" value="javax.jms.Queue"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"/&gt;</pre>
 *
 * 或者，查看你的资源提供者的 ActivationSpec 对象，它应该通过提供者特定的配置标志支持本地事务，
 * 例如 ActiveMQActivationSpec 的 "useRAManagedTransaction" bean 属性。
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointManager"&gt;
 * 	 &lt;property name="resourceAdapter" ref="resourceAdapter"/&gt;
 * 	 &lt;property name="messageEndpointFactory"&gt;
 *     &lt;bean class="org.springframework.jca.endpoint.GenericMessageEndpointFactory"&gt;
 *       &lt;property name="messageListener" ref="messageListener"/&gt;
 *     &lt;/bean&gt;
 * 	 &lt;/property&gt;
 * 	 &lt;property name="activationSpec"&gt;
 *     &lt;bean class="org.apache.activemq.ra.ActiveMQActivationSpec"&gt;
 *       &lt;property name="destination" value="myQueue"/&gt;
 *       &lt;property name="destinationType" value="javax.jms.Queue"/&gt;
 *       &lt;property name="useRAManagedTransaction" value="true"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.resource.spi.ResourceAdapter#endpointActivation
 * @see javax.resource.spi.ResourceAdapter#endpointDeactivation
 * @see javax.resource.spi.endpoint.MessageEndpointFactory
 * @see javax.resource.spi.ActivationSpec
 */
public class GenericMessageEndpointManager implements SmartLifecycle, InitializingBean, DisposableBean {

	@Nullable
	private ResourceAdapter resourceAdapter;

	@Nullable
	private MessageEndpointFactory messageEndpointFactory;

	@Nullable
	private ActivationSpec activationSpec;

	private boolean autoStartup = true;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * 设置要管理端点的 JCA ResourceAdapter。
	 */
	public void setResourceAdapter(@Nullable ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * 返回要管理端点的 JCA ResourceAdapter。
	 */
	@Nullable
	public ResourceAdapter getResourceAdapter() {
		return this.resourceAdapter;
	}

	/**
	 * 设置要激活的 JCA MessageEndpointFactory，指向端点将委托给的 MessageListener 对象。
	 * <p>MessageEndpointFactory 实例可以在多个端点（即多个 GenericMessageEndpointManager 实例）之间共享，
	 * 应用不同的 {@link #setActivationSpec ActivationSpec} 对象。
	 * @see GenericMessageEndpointFactory#setMessageListener
	 */
	public void setMessageEndpointFactory(@Nullable MessageEndpointFactory messageEndpointFactory) {
		this.messageEndpointFactory = messageEndpointFactory;
	}

	/**
	 * 返回要激活的 JCA MessageEndpointFactory。
	 */
	@Nullable
	public MessageEndpointFactory getMessageEndpointFactory() {
		return this.messageEndpointFactory;
	}

	/**
	 * 设置用于激活端点的 JCA ActivationSpec。
	 * <p>注意，此 ActivationSpec 实例不应在多个 ResourceAdapter 实例之间共享。
	 */
	public void setActivationSpec(@Nullable ActivationSpec activationSpec) {
		this.activationSpec = activationSpec;
	}

	/**
	 * 返回用于激活端点的 JCA ActivationSpec。
	 */
	@Nullable
	public ActivationSpec getActivationSpec() {
		return this.activationSpec;
	}

	/**
	 * 设置此端点管理器初始化并刷新上下文后是否自动启动端点激活。
	 * <p>默认为 "true"。关闭此标志以将端点激活延迟到显式调用 {@link #start()}。
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回 'autoStartup' 属性的值。如果为 "true"，此端点管理器将在 ContextRefreshedEvent 时启动。
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定此端点管理器应启动和停止的阶段。启动顺序从最低到最高，
	 * 关闭顺序则相反。默认值为 {@code Integer.MAX_VALUE}，
	 * 表示此端点管理器尽可能晚地启动并尽可能早地停止。
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此端点管理器将启动和停止的阶段。
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * 准备消息端点，如果 "autoStartup" 标志设置为 "true" 则自动激活。
	 */
	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (getResourceAdapter() == null) {
			throw new IllegalArgumentException("Property 'resourceAdapter' is required");
		}
		if (getMessageEndpointFactory() == null) {
			throw new IllegalArgumentException("Property 'messageEndpointFactory' is required");
		}
		ActivationSpec activationSpec = getActivationSpec();
		if (activationSpec == null) {
			throw new IllegalArgumentException("Property 'activationSpec' is required");
		}

		if (activationSpec.getResourceAdapter() == null) {
			activationSpec.setResourceAdapter(getResourceAdapter());
		}
		else if (activationSpec.getResourceAdapter() != getResourceAdapter()) {
			throw new IllegalArgumentException("ActivationSpec [" + activationSpec +
					"] is associated with a different ResourceAdapter: " + activationSpec.getResourceAdapter());
		}
	}

	/**
	 * 激活配置的消息端点。
	 */
	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				ResourceAdapter resourceAdapter = getResourceAdapter();
				Assert.state(resourceAdapter != null, "No ResourceAdapter set");
				try {
					resourceAdapter.endpointActivation(getMessageEndpointFactory(), getActivationSpec());
				}
				catch (ResourceException ex) {
					throw new IllegalStateException("Could not activate message endpoint", ex);
				}
				this.running = true;
			}
		}
	}

	/**
	 * 停用配置的消息端点。
	 */
	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				ResourceAdapter resourceAdapter = getResourceAdapter();
				Assert.state(resourceAdapter != null, "No ResourceAdapter set");
				resourceAdapter.endpointDeactivation(getMessageEndpointFactory(), getActivationSpec());
				this.running = false;
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	/**
	 * 返回配置的消息端点当前是否处于活动状态。
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 停用消息端点，为关闭做准备。
	 */
	@Override
	public void destroy() {
		stop();
	}

}
