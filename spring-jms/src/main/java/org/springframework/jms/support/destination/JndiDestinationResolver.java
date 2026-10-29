/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.support.destination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.NamingException;

import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver} 实现，将目的地名称解释为 JNDI 位置
 * （并带有可配置的后备策略）。
 *
 * <p>如有必要，允许自定义 JNDI 环境，例如指定适当的 JNDI 环境属性。
 *
 * <p>动态队列和主题会按目的地名称进行缓存。因此，你需要确保队列和主题中的
 * 目的地名称都是唯一的。可以通过 {@link #setCache "cache"} 标志关闭缓存。
 *
 * <p>请注意，回退到动态目的地解析默认是<i>关闭</i>的。
 * 打开 {@link #setFallbackToDynamicDestination "fallbackToDynamicDestination"}
 * 标志即可启用此功能。
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setCache
 * @see #setFallbackToDynamicDestination
 */
public class JndiDestinationResolver extends JndiLocatorSupport implements CachingDestinationResolver {

	private boolean cache = true;

	private boolean fallbackToDynamicDestination = false;

	private DestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver();

	private final Map<String, Destination> destinationCache = new ConcurrentHashMap<>(16);


	/**
	 * 设置是否缓存已解析的目的地。默认值为 "true"。
	 * <p>可以关闭此标志，使每次操作都重新查找目的地，
	 * 从而支持目的地的热重启。这主要用于开发期间。
	 * <p>请注意，动态队列和主题会按目的地名称进行缓存。
	 * 因此，你需要确保队列和主题中的目的地名称都是唯一的。
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * 设置此解析器在 JNDI 中找不到目的地名称时是否应创建动态目的地。
	 * 默认值为 "false"。
	 * <p>打开此标志可启用对动态目的地的透明回退。
	 * @see #setDynamicDestinationResolver
	 */
	public void setFallbackToDynamicDestination(boolean fallbackToDynamicDestination) {
		this.fallbackToDynamicDestination = fallbackToDynamicDestination;
	}

	/**
	 * 设置回退到动态目的地时要使用的 {@link DestinationResolver}。
	 * <p>默认是 Spring 标准的 {@link DynamicDestinationResolver}。
	 * @see #setFallbackToDynamicDestination
	 * @see DynamicDestinationResolver
	 */
	public void setDynamicDestinationResolver(DestinationResolver dynamicDestinationResolver) {
		this.dynamicDestinationResolver = dynamicDestinationResolver;
	}


	@Override
	public Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.notNull(destinationName, "Destination name must not be null");
		Destination dest = this.destinationCache.get(destinationName);
		if (dest != null) {
			validateDestination(dest, destinationName, pubSubDomain);
		}
		else {
			try {
				dest = lookup(destinationName, Destination.class);
				validateDestination(dest, destinationName, pubSubDomain);
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Destination [" + destinationName + "] not found in JNDI", ex);
				}
				if (this.fallbackToDynamicDestination) {
					dest = this.dynamicDestinationResolver.resolveDestinationName(session, destinationName, pubSubDomain);
				}
				else {
					throw new DestinationResolutionException(
							"Destination [" + destinationName + "] not found in JNDI", ex);
				}
			}
			if (this.cache) {
				this.destinationCache.put(destinationName, dest);
			}
		}
		return dest;
	}

	/**
	 * 校验给定的 Destination 对象，检查其是否与预期的类型匹配。
	 * @param destination the Destination object to validate
	 * @param destinationName the name of the destination
	 * @param pubSubDomain {@code true} if a Topic is expected,
	 * {@code false} in case of a Queue
	 */
	protected void validateDestination(Destination destination, String destinationName, boolean pubSubDomain) {
		Class<?> targetClass = Queue.class;
		if (pubSubDomain) {
			targetClass = Topic.class;
		}
		if (!targetClass.isInstance(destination)) {
			throw new DestinationResolutionException(
					"Destination [" + destinationName + "] is not of expected type [" + targetClass.getName() + "]");
		}
	}


	@Override
	public void removeFromCache(String destinationName) {
		this.destinationCache.remove(destinationName);
	}

	@Override
	public void clearCache() {
		this.destinationCache.clear();
	}

}
