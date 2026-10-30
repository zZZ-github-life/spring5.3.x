/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jndi.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.jndi.TypeMismatchNamingException;
import org.springframework.lang.Nullable;

/**
 * Spring {@link org.springframework.beans.factory.BeanFactory} 接口的简单 JNDI 实现。不支持枚举
 * Bean 定义，因此未实现 {@link org.springframework.beans.factory.ListableBeanFactory} 接口。
 *
 * <p>该工厂会将给定的 Bean 名称解析为 Java EE 应用 "java:comp/env/" 命名空间中的 JNDI 名称。它会缓
 * 存所有已获取对象的解析结果类型，并可选地缓存可共享对象（如果它们被显式标记为
 * {@link #addShareableResource shareable resource}）。
 *
 * <p>该工厂的主要用途是与 Spring 的
 * {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor} 结合使用，将其
 * 配置为 "resourceFactory"，以便在没有中间 Bean 定义的情况下将 {@code @Resource} 注解解析为 JNDI
 * 对象。当然，它也可以用于类似的查找场景，特别是在需要 BeanFactory 风格的类型检查时。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
 */
public class SimpleJndiBeanFactory extends JndiLocatorSupport implements BeanFactory {

	/** 已知可共享（即可以缓存）的资源的 JNDI 名称 */
	private final Set<String> shareableResources = new HashSet<>();

	/** 可共享单例对象的缓存：bean 名称到 bean 实例。 */
	private final Map<String, Object> singletonObjects = new HashMap<>();

	/** 不可共享资源类型的缓存：bean 名称到 bean 类型。 */
	private final Map<String, Class<?>> resourceTypes = new HashMap<>();


	public SimpleJndiBeanFactory() {
		setResourceRef(true);
	}


	/**
	 * 添加可共享 JNDI 资源的名称，该工厂在获取该资源后可以对其进行缓存。
	 * @param shareableResource JNDI 名称（通常位于 "java:comp/env/" 命名空间内）
	 */
	public void addShareableResource(String shareableResource) {
		this.shareableResources.add(shareableResource);
	}

	/**
	 * 设置可共享 JNDI 资源名称的列表，该工厂在获取这些资源后可以对其进行缓存。
	 * @param shareableResources JNDI 名称（通常位于 "java:comp/env/" 命名空间内）
	 */
	public void setShareableResources(String... shareableResources) {
		Collections.addAll(this.shareableResources, shareableResources);
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------


	@Override
	public Object getBean(String name) throws BeansException {
		return getBean(name, Object.class);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		try {
			if (isSingleton(name)) {
				return doGetSingleton(name, requiredType);
			}
			else {
				return lookup(name, requiredType);
			}
		}
		catch (NameNotFoundException ex) {
			throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
		}
		catch (TypeMismatchNamingException ex) {
			throw new BeanNotOfRequiredTypeException(name, ex.getRequiredType(), ex.getActualType());
		}
		catch (NamingException ex) {
			throw new BeanDefinitionStoreException("JNDI environment", name, "JNDI lookup failed", ex);
		}
	}

	@Override
	public Object getBean(String name, @Nullable Object... args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"SimpleJndiBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType.getSimpleName(), requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"SimpleJndiBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		return new ObjectProvider<T>() {
			@Override
			public T getObject() throws BeansException {
				return getBean(requiredType);
			}
			@Override
			public T getObject(Object... args) throws BeansException {
				return getBean(requiredType, args);
			}
			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				try {
					return getBean(requiredType);
				}
				catch (NoUniqueBeanDefinitionException ex) {
					throw ex;
				}
				catch (NoSuchBeanDefinitionException ex) {
					return null;
				}
			}
			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				try {
					return getBean(requiredType);
				}
				catch (NoSuchBeanDefinitionException ex) {
					return null;
				}
			}
		};
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		throw new UnsupportedOperationException(
				"SimpleJndiBeanFactory does not support resolution by ResolvableType");
	}

	@Override
	public boolean containsBean(String name) {
		if (this.singletonObjects.containsKey(name) || this.resourceTypes.containsKey(name)) {
			return true;
		}
		try {
			doGetType(name);
			return true;
		}
		catch (NamingException ex) {
			return false;
		}
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return this.shareableResources.contains(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return !this.shareableResources.contains(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (type != null && typeToMatch.isAssignableFrom(type));
	}

	@Override
	public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		try {
			return doGetType(name);
		}
		catch (NameNotFoundException ex) {
			throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
		}
		catch (NamingException ex) {
			return null;
		}
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	@SuppressWarnings("unchecked")
	private <T> T doGetSingleton(String name, @Nullable Class<T> requiredType) throws NamingException {
		synchronized (this.singletonObjects) {
			Object singleton = this.singletonObjects.get(name);
			if (singleton != null) {
				if (requiredType != null && !requiredType.isInstance(singleton)) {
					throw new TypeMismatchNamingException(convertJndiName(name), requiredType, singleton.getClass());
				}
				return (T) singleton;
			}
			T jndiObject = lookup(name, requiredType);
			this.singletonObjects.put(name, jndiObject);
			return jndiObject;
		}
	}

	private Class<?> doGetType(String name) throws NamingException {
		if (isSingleton(name)) {
			return doGetSingleton(name, null).getClass();
		}
		else {
			synchronized (this.resourceTypes) {
				Class<?> type = this.resourceTypes.get(name);
				if (type == null) {
					type = lookup(name, null).getClass();
					this.resourceTypes.put(name, type);
				}
				return type;
			}
		}
	}

}
