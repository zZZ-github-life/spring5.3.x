/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 适配器，实现 {@link DisposableBean} 和 {@link Runnable} 接口，对给定 Bean 实例执行各种销毁步骤：
 * <ul>
 * <li>DestructionAwareBeanPostProcessors；
 * <li>实现 DisposableBean 的 Bean 本身；
 * <li>在 Bean 定义上指定的自定义销毁方法。
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 2.0
 * @see AbstractBeanFactory
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
 * @see AbstractBeanDefinition#getDestroyMethodName()
 */
@SuppressWarnings("serial")
class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

	private static final String DESTROY_METHOD_NAME = "destroy";

	private static final String CLOSE_METHOD_NAME = "close";

	private static final String SHUTDOWN_METHOD_NAME = "shutdown";

	private static final Log logger = LogFactory.getLog(DisposableBeanAdapter.class);


	private final Object bean;

	private final String beanName;

	private final boolean nonPublicAccessAllowed;

	private final boolean invokeDisposableBean;

	private boolean invokeAutoCloseable;

	@Nullable
	private String destroyMethodName;

	@Nullable
	private transient Method destroyMethod;

	@Nullable
	private final List<DestructionAwareBeanPostProcessor> beanPostProcessors;

	@Nullable
	private final AccessControlContext acc;


	/**
	 * 为给定 Bean 创建一个新的 DisposableBeanAdapter。
	 * @param bean Bean 实例（永不为 {@code null}）
	 * @param beanName Bean 的名称
	 * @param beanDefinition 合并后的 Bean 定义
	 * @param postProcessors BeanPostProcessor（可能是 DestructionAwareBeanPostProcessor）的 List（如果有的话）
	 */
	public DisposableBeanAdapter(Object bean, String beanName, RootBeanDefinition beanDefinition,
			List<DestructionAwareBeanPostProcessor> postProcessors, @Nullable AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = beanName;
		this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
		this.invokeDisposableBean = (bean instanceof DisposableBean &&
				!beanDefinition.hasAnyExternallyManagedDestroyMethod(DESTROY_METHOD_NAME));

		String destroyMethodName = inferDestroyMethodIfNecessary(bean, beanDefinition);
		if (destroyMethodName != null &&
				!(this.invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodName)) &&
				!beanDefinition.hasAnyExternallyManagedDestroyMethod(destroyMethodName)) {

			this.invokeAutoCloseable = (bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodName));
			if (!this.invokeAutoCloseable) {
				this.destroyMethodName = destroyMethodName;
				Method destroyMethod = determineDestroyMethod(destroyMethodName);
				if (destroyMethod == null) {
					if (beanDefinition.isEnforceDestroyMethod()) {
						throw new BeanDefinitionValidationException("Could not find a destroy method named '" +
								destroyMethodName + "' on bean with name '" + beanName + "'");
					}
				}
				else {
					if (destroyMethod.getParameterCount() > 0) {
						Class<?>[] paramTypes = destroyMethod.getParameterTypes();
						if (paramTypes.length > 1) {
							throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
									beanName + "' has more than one parameter - not supported as destroy method");
						}
						else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
							throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
									beanName + "' has a non-boolean parameter - not supported as destroy method");
						}
					}
					destroyMethod = ClassUtils.getInterfaceMethodIfPossible(destroyMethod, bean.getClass());
				}
				this.destroyMethod = destroyMethod;
			}
		}

		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
		this.acc = acc;
	}

	/**
	 * 为给定 Bean 创建一个新的 DisposableBeanAdapter。
	 * @param bean Bean 实例（永不为 {@code null}）
	 * @param postProcessors BeanPostProcessor（可能是 DestructionAwareBeanPostProcessor）的 List（如果有的话）
	 */
	public DisposableBeanAdapter(
			Object bean, List<DestructionAwareBeanPostProcessor> postProcessors, AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = bean.getClass().getName();
		this.nonPublicAccessAllowed = true;
		this.invokeDisposableBean = (this.bean instanceof DisposableBean);
		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
		this.acc = acc;
	}

	/**
	 * 为给定 Bean 创建一个新的 DisposableBeanAdapter。
	 */
	private DisposableBeanAdapter(Object bean, String beanName, boolean nonPublicAccessAllowed,
			boolean invokeDisposableBean, boolean invokeAutoCloseable, @Nullable String destroyMethodName,
			@Nullable List<DestructionAwareBeanPostProcessor> postProcessors) {

		this.bean = bean;
		this.beanName = beanName;
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
		this.invokeDisposableBean = invokeDisposableBean;
		this.invokeAutoCloseable = invokeAutoCloseable;
		this.destroyMethodName = destroyMethodName;
		this.beanPostProcessors = postProcessors;
		this.acc = null;
	}


	@Override
	public void run() {
		destroy();
	}

	@Override
	public void destroy() {
		if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
				processor.postProcessBeforeDestruction(this.bean, this.beanName);
			}
		}

		if (this.invokeDisposableBean) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking destroy() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((DisposableBean) this.bean).destroy();
						return null;
					}, this.acc);
				}
				else {
					((DisposableBean) this.bean).destroy();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
					if (logger.isDebugEnabled()) {
						// Log at warn level like below but add the exception stacktrace only with debug level
						logger.warn(msg, ex);
					}
					else {
						logger.warn(msg + ": " + ex);
					}
				}
			}
		}

		if (this.invokeAutoCloseable) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking close() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						((AutoCloseable) this.bean).close();
						return null;
					}, this.acc);
				}
				else {
					((AutoCloseable) this.bean).close();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					String msg = "Invocation of close method failed on bean with name '" + this.beanName + "'";
					if (logger.isDebugEnabled()) {
						// Log at warn level like below but add the exception stacktrace only with debug level
						logger.warn(msg, ex);
					}
					else {
						logger.warn(msg + ": " + ex);
					}
				}
			}
		}
		else if (this.destroyMethod != null) {
			invokeCustomDestroyMethod(this.destroyMethod);
		}
		else if (this.destroyMethodName != null) {
			Method destroyMethod = determineDestroyMethod(this.destroyMethodName);
			if (destroyMethod != null) {
				invokeCustomDestroyMethod(ClassUtils.getInterfaceMethodIfPossible(destroyMethod, this.bean.getClass()));
			}
		}
	}


	@Nullable
	private Method determineDestroyMethod(String name) {
		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Method>) () -> findDestroyMethod(name));
			}
			else {
				return findDestroyMethod(name);
			}
		}
		catch (IllegalArgumentException ex) {
			throw new BeanDefinitionValidationException("Could not find unique destroy method on bean with name '" +
					this.beanName + ": " + ex.getMessage());
		}
	}

	@Nullable
	private Method findDestroyMethod(String name) {
		return (this.nonPublicAccessAllowed ?
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), name) :
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass().getMethods(), name));
	}

	/**
	 * 在给定 Bean 上调用指定的自定义销毁方法。
	 * <p>如果找到无参方法，此实现会调用该方法；否则检查带单个 boolean 参数的方法（传入 "true"，假定存
	 * 在 "force" 参数）；再否则记录错误日志。
	 */
	private void invokeCustomDestroyMethod(final Method destroyMethod) {
		int paramCount = destroyMethod.getParameterCount();
		final Object[] args = new Object[paramCount];
		if (paramCount == 1) {
			args[0] = Boolean.TRUE;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking custom destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'");
		}
		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(destroyMethod);
					return null;
				});
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
						destroyMethod.invoke(this.bean, args), this.acc);
				}
				catch (PrivilegedActionException pax) {
					throw (InvocationTargetException) pax.getException();
				}
			}
			else {
				ReflectionUtils.makeAccessible(destroyMethod);
				destroyMethod.invoke(this.bean, args);
			}
		}
		catch (InvocationTargetException ex) {
			if (logger.isWarnEnabled()) {
				String msg = "Custom destroy method '" + this.destroyMethodName + "' on bean with name '" +
						this.beanName + "' threw an exception";
				if (logger.isDebugEnabled()) {
					// Log at warn level like below but add the exception stacktrace only with debug level
					logger.warn(msg, ex.getTargetException());
				}
				else {
					logger.warn(msg + ": " + ex.getTargetException());
				}
			}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to invoke custom destroy method '" + this.destroyMethodName +
						"' on bean with name '" + this.beanName + "'", ex);
			}
		}
	}


	/**
	 * 序列化此类状态的副本，并过滤掉不可序列化的 BeanPostProcessor。
	 */
	protected Object writeReplace() {
		List<DestructionAwareBeanPostProcessor> serializablePostProcessors = null;
		if (this.beanPostProcessors != null) {
			serializablePostProcessors = new ArrayList<>();
			for (DestructionAwareBeanPostProcessor postProcessor : this.beanPostProcessors) {
				if (postProcessor instanceof Serializable) {
					serializablePostProcessors.add(postProcessor);
				}
			}
		}
		return new DisposableBeanAdapter(
				this.bean, this.beanName, this.nonPublicAccessAllowed, this.invokeDisposableBean,
				this.invokeAutoCloseable, this.destroyMethodName, serializablePostProcessors);
	}


	/**
	 * 检查给定 Bean 是否有任何类型的销毁方法可供调用。
	 * @param bean Bean 实例
	 * @param beanDefinition 对应的 Bean 定义
	 */
	public static boolean hasDestroyMethod(Object bean, RootBeanDefinition beanDefinition) {
		return (bean instanceof DisposableBean || inferDestroyMethodIfNecessary(bean, beanDefinition) != null);
	}


	/**
	 * 如果给定 beanDefinition 的 "destroyMethodName" 属性当前值为
	 * {@link AbstractBeanDefinition#INFER_METHOD}，则尝试推断销毁方法。候选方法目前限于名为 "close" 或
	 * "shutdown" 的 public 无参方法（无论是本地声明还是继承而来）。如果找不到此类方法，给定
	 * BeanDefinition 的 "destroyMethodName" 会被更新为 null，否则被设置为推断出的方法名。此常量是
	 * {@code @Bean#destroyMethod} 属性的默认值，该常量的值也可用于 XML 中的
	 * {@code <bean destroy-method="">} 或 {@code <beans default-destroy-method="">} 属性。
	 * <p>此外还会处理 {@link java.io.Closeable} 和 {@link java.lang.AutoCloseable} 接口，对实现这些接
	 * 口的 Bean 也以反射方式调用 "close" 方法。
	 */
	@Nullable
	private static String inferDestroyMethodIfNecessary(Object bean, RootBeanDefinition beanDefinition) {
		String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
		if (destroyMethodName == null) {
			destroyMethodName = beanDefinition.getDestroyMethodName();
			boolean autoCloseable = (bean instanceof AutoCloseable);
			if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
					(destroyMethodName == null && autoCloseable)) {
				// Only perform destroy method inference in case of the bean
				// not explicitly implementing the DisposableBean interface
				destroyMethodName = null;
				if (!(bean instanceof DisposableBean)) {
					if (autoCloseable) {
						destroyMethodName = CLOSE_METHOD_NAME;
					}
					else {
						try {
							destroyMethodName = bean.getClass().getMethod(CLOSE_METHOD_NAME).getName();
						}
						catch (NoSuchMethodException ex) {
							try {
								destroyMethodName = bean.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
							}
							catch (NoSuchMethodException ex2) {
								// no candidate destroy method found
							}
						}
					}
				}
			}
			beanDefinition.resolvedDestroyMethodName = (destroyMethodName != null ? destroyMethodName : "");
		}
		return (StringUtils.hasLength(destroyMethodName) ? destroyMethodName : null);
	}

	/**
	 * 检查给定 Bean 是否有适用于它的、感知销毁的后处理器。
	 * @param bean Bean 实例
	 * @param postProcessors 后处理器候选
	 */
	public static boolean hasApplicableProcessors(Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
		if (!CollectionUtils.isEmpty(postProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : postProcessors) {
				if (processor.requiresDestruction(bean)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 在 List 中查找所有 DestructionAwareBeanPostProcessor。
	 * @param processors 要搜索的 List
	 * @return 过滤后的 DestructionAwareBeanPostProcessor 的 List
	 */
	@Nullable
	private static List<DestructionAwareBeanPostProcessor> filterPostProcessors(
			List<DestructionAwareBeanPostProcessor> processors, Object bean) {

		List<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
		if (!CollectionUtils.isEmpty(processors)) {
			filteredPostProcessors = new ArrayList<>(processors.size());
			for (DestructionAwareBeanPostProcessor processor : processors) {
				if (processor.requiresDestruction(bean)) {
					filteredPostProcessors.add(processor);
				}
			}
		}
		return filteredPostProcessors;
	}

}
