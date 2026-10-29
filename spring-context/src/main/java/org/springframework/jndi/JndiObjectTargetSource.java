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

package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * AOP {@link org.springframework.aop.TargetSource}，为 {@code getTarget()} 调用提供可配置的 JNDI
 * 查找。
 *
 * <p>可以作为 {@link JndiObjectFactoryBean} 的替代方案使用，以允许延迟地或针对每次操作重新定位
 * JNDI 对象（参见 "lookupOnStartup" 和 "cache" 属性）。这在开发期间特别有用，因为它允许热重启 JNDI
 * 服务器（例如远程 JMS 服务器）。
 *
 * <p>示例：
 *
 * <pre class="code">
 * &lt;bean id="queueConnectionFactoryTarget" class="org.springframework.jndi.JndiObjectTargetSource"&gt;
 *   &lt;property name="jndiName" value="JmsQueueConnectionFactory"/&gt;
 *   &lt;property name="lookupOnStartup" value="false"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="queueConnectionFactory" class="org.springframework.aop.framework.ProxyFactoryBean"&gt;
 *   &lt;property name="proxyInterfaces" value="javax.jms.QueueConnectionFactory"/&gt;
 *   &lt;property name="targetSource" ref="queueConnectionFactoryTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 在 "queueConnectionFactory" 代理上调用 {@code createQueueConnection} 会触发对
 * "JmsQueueConnectionFactory" 的延迟 JNDI 查找，随后委托调用所获取的 QueueConnectionFactory 的
 * {@code createQueueConnection}。
 *
 * <p><b>另外，也可以使用带 "proxyInterface" 的 {@link JndiObjectFactoryBean}。</b> 之后可以在
 * JndiObjectFactoryBean 上指定 "lookupOnStartup" 和 "cache"，它会在底层创建 JndiObjectTargetSource
 * （而不是分别定义 ProxyFactoryBean 和 JndiObjectTargetSource 这两个 Bean）。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setLookupOnStartup
 * @see #setCache
 * @see org.springframework.aop.framework.ProxyFactoryBean#setTargetSource
 * @see JndiObjectFactoryBean#setProxyInterface
 */
public class JndiObjectTargetSource extends JndiObjectLocator implements TargetSource {

	private boolean lookupOnStartup = true;

	private boolean cache = true;

	@Nullable
	private Object cachedObject;

	@Nullable
	private Class<?> targetClass;


	/**
	 * 设置是否在启动时查找 JNDI 对象。默认值为 "true"。
	 * <p>可以关闭该选项，以允许 JNDI 对象稍后才可用。在这种情况下，JNDI 对象将在首次访问时获取。
	 * @see #setCache
	 */
	public void setLookupOnStartup(boolean lookupOnStartup) {
		this.lookupOnStartup = lookupOnStartup;
	}

	/**
	 * 设置 JNDI 对象被定位后是否对其进行缓存。默认值为 "true"。
	 * <p>可以关闭该选项，以允许 JNDI 对象热重新部署。在这种情况下，每次调用都会获取 JNDI 对象。
	 * @see #setLookupOnStartup
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.lookupOnStartup) {
			Object object = lookup();
			if (this.cache) {
				this.cachedObject = object;
			}
			else {
				this.targetClass = object.getClass();
			}
		}
	}


	@Override
	@Nullable
	public Class<?> getTargetClass() {
		if (this.cachedObject != null) {
			return this.cachedObject.getClass();
		}
		else if (this.targetClass != null) {
			return this.targetClass;
		}
		else {
			return getExpectedType();
		}
	}

	@Override
	public boolean isStatic() {
		return (this.cachedObject != null);
	}

	@Override
	@Nullable
	public Object getTarget() {
		try {
			if (this.lookupOnStartup || !this.cache) {
				return (this.cachedObject != null ? this.cachedObject : lookup());
			}
			else {
				synchronized (this) {
					if (this.cachedObject == null) {
						this.cachedObject = lookup();
					}
					return this.cachedObject;
				}
			}
		}
		catch (NamingException ex) {
			throw new JndiLookupFailureException("JndiObjectTargetSource failed to obtain new target object", ex);
		}
	}

	@Override
	public void releaseTarget(Object target) {
	}

}
