/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jmx.support;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 为向 {@link javax.management.MBeanServer} 注册 MBean 提供支撑基础设施。在给定 {@link ObjectName}
 * 处遇到已有 MBean 时的行为完全可配置，从而实现灵活的注册设置。
 *
 * <p>所有已注册的 MBean 都会被跟踪，并可通过调用 #{@link #unregisterBeans()} 方法注销。
 *
 * <p>子类可以通过分别重写 {@link #onRegister(ObjectName)} 和 {@link #onUnregister(ObjectName)} 方
 * 法，在 MBean 注册或注销时接收通知。
 *
 * <p>默认情况下，如果尝试使用已被使用的 {@link javax.management.ObjectName} 注册 MBean，注册过程将
 * 失败。
 *
 * <p>通过将 {@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy} 属性设置为
 * {@link RegistrationPolicy#IGNORE_EXISTING}，注册过程将直接忽略已存在的 MBean，使其保持注册状态。
 * 在多个应用希望在共享的 {@link MBeanServer} 中共享同一个公共 MBean 的场景中，这很有用。
 *
 * <p>将 {@link #setRegistrationPolicy(RegistrationPolicy) registrationPolicy} 属性设置为
 * {@link RegistrationPolicy#REPLACE_EXISTING}，将导致在注册时如有必要替换已存在的 MBean。在无法保
 * 证 {@link MBeanServer} 状态的情况下，这很有用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.0
 * @see #setServer
 * @see #setRegistrationPolicy
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class MBeanRegistrationSupport {

	/**
	 * 此类的 {@code Log} 实例。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 用于注册 Bean 的 {@code MBeanServer} 实例。
	 */
	@Nullable
	protected MBeanServer server;

	/**
	 * 由此导出器注册的 Bean。
	 */
	private final Set<ObjectName> registeredBeans = new LinkedHashSet<>();

	/**
	 * 注册 MBean 时发现其已存在所采用的策略。默认会抛出异常。
	 */
	private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;


	/**
	 * 指定所有 Bean 都应注册到的 {@code MBeanServer} 实例。如果未提供，{@code MBeanExporter} 将尝试定
	 * 位已有的 {@code MBeanServer}。
	 */
	public void setServer(@Nullable MBeanServer server) {
		this.server = server;
	}

	/**
	 * 返回 Bean 将注册到的 {@code MBeanServer}。
	 */
	@Nullable
	public final MBeanServer getServer() {
		return this.server;
	}

	/**
	 * 尝试在已存在的 {@link javax.management.ObjectName} 下注册 MBean 时使用的策略。
	 * @param registrationPolicy the policy to use
	 * @since 3.2
	 */
	public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
		Assert.notNull(registrationPolicy, "RegistrationPolicy must not be null");
		this.registrationPolicy = registrationPolicy;
	}


	/**
	 * 实际将 MBean 注册到服务器。遇到已有 MBean 时的行为可以通过 {@link #setRegistrationPolicy} 配置。
	 * @param mbean the MBean instance
	 * @param objectName the suggested ObjectName for the MBean
	 * @throws JMException if the registration failed
	 */
	protected void doRegister(Object mbean, ObjectName objectName) throws JMException {
		Assert.state(this.server != null, "No MBeanServer set");
		ObjectName actualObjectName;

		synchronized (this.registeredBeans) {
			ObjectInstance registeredBean = null;
			try {
				registeredBean = this.server.registerMBean(mbean, objectName);
			}
			catch (InstanceAlreadyExistsException ex) {
				if (this.registrationPolicy == RegistrationPolicy.IGNORE_EXISTING) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring existing MBean at [" + objectName + "]");
					}
				}
				else if (this.registrationPolicy == RegistrationPolicy.REPLACE_EXISTING) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Replacing existing MBean at [" + objectName + "]");
						}
						this.server.unregisterMBean(objectName);
						registeredBean = this.server.registerMBean(mbean, objectName);
					}
					catch (InstanceNotFoundException ex2) {
						if (logger.isInfoEnabled()) {
							logger.info("Unable to replace existing MBean at [" + objectName + "]", ex2);
						}
						throw ex;
					}
				}
				else {
					throw ex;
				}
			}

			// Track registration and notify listeners.
			actualObjectName = (registeredBean != null ? registeredBean.getObjectName() : null);
			if (actualObjectName == null) {
				actualObjectName = objectName;
			}
			this.registeredBeans.add(actualObjectName);
		}

		onRegister(actualObjectName, mbean);
	}

	/**
	 * 注销由此类实例注册的所有 Bean。
	 */
	protected void unregisterBeans() {
		Set<ObjectName> snapshot;
		synchronized (this.registeredBeans) {
			snapshot = new LinkedHashSet<>(this.registeredBeans);
		}
		if (!snapshot.isEmpty()) {
			logger.debug("Unregistering JMX-exposed beans");
			for (ObjectName objectName : snapshot) {
				doUnregister(objectName);
			}
		}
	}

	/**
	 * 实际从服务器注销指定的 MBean。
	 * @param objectName the suggested ObjectName for the MBean
	 */
	protected void doUnregister(ObjectName objectName) {
		Assert.state(this.server != null, "No MBeanServer set");
		boolean actuallyUnregistered = false;

		synchronized (this.registeredBeans) {
			if (this.registeredBeans.remove(objectName)) {
				try {
					// MBean might already have been unregistered by an external process
					if (this.server.isRegistered(objectName)) {
						this.server.unregisterMBean(objectName);
						actuallyUnregistered = true;
					}
					else {
						if (logger.isInfoEnabled()) {
							logger.info("Could not unregister MBean [" + objectName + "] as said MBean " +
									"is not registered (perhaps already unregistered by an external process)");
						}
					}
				}
				catch (JMException ex) {
					if (logger.isInfoEnabled()) {
						logger.info("Could not unregister MBean [" + objectName + "]", ex);
					}
				}
			}
		}

		if (actuallyUnregistered) {
			onUnregister(objectName);
		}
	}

	/**
	 * 返回所有已注册 Bean 的 {@link ObjectName ObjectNames}。
	 */
	protected final ObjectName[] getRegisteredObjectNames() {
		synchronized (this.registeredBeans) {
			return this.registeredBeans.toArray(new ObjectName[0]);
		}
	}


	/**
	 * 当 MBean 在给定 {@link ObjectName} 下注册时调用。允许子类在 MBean 注册时执行额外处理。
	 * <p>默认实现委托给 {@link #onRegister(ObjectName)}。
	 * @param objectName the actual {@link ObjectName} that the MBean was registered with
	 * @param mbean the registered MBean instance
	 */
	protected void onRegister(ObjectName objectName, Object mbean) {
		onRegister(objectName);
	}

	/**
	 * 当 MBean 在给定 {@link ObjectName} 下注册时调用。允许子类在 MBean 注册时执行额外处理。
	 * <p>默认实现为空。可在子类中重写。
	 * @param objectName the actual {@link ObjectName} that the MBean was registered with
	 */
	protected void onRegister(ObjectName objectName) {
	}

	/**
	 * 当 MBean 在给定 {@link ObjectName} 下注销时调用。允许子类在 MBean 注销时执行额外处理。
	 * <p>默认实现为空。可在子类中重写。
	 * @param objectName the {@link ObjectName} that the MBean was registered with
	 */
	protected void onUnregister(ObjectName objectName) {
	}

}
