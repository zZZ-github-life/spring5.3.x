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

package org.springframework.orm.jpa.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.orm.jpa.ExtendedEntityManagerCreator;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * BeanPostProcessor，用于处理 {@link javax.persistence.PersistenceUnit}
 * 和 {@link javax.persistence.PersistenceContext} 注解，注入相应的
 * JPA 资源 {@link javax.persistence.EntityManagerFactory}
 * 和 {@link javax.persistence.EntityManager}。任何 Spring 托管对象中
 * 此类带注解的字段或方法都会被自动注入。
 *
 * <p>如果带注解的字段或方法声明为 {@code EntityManagerFactory}
 * 和 {@code EntityManager} 的子接口，此后处理器将注入这些子接口。
 * 实际类型会尽早验证，但共享（"事务性"）的 {@code EntityManager}
 * 引用除外，其类型不匹配可能要到第一次实际调用时才会被发现。
 *
 * <p>注意：在当前实现中，PersistenceAnnotationBeanPostProcessor
 * 仅支持带有 "unitName" 属性的 {@code @PersistenceUnit} 和
 * {@code @PersistenceContext}，或者完全不指定属性（使用默认单元）。
 * 如果这些注解在类级别带有 "name" 属性，它们将被直接忽略，
 * 因为这些注解仅作为部署提示（依据 Java EE 规范）。
 *
 * <p>此后期处理器既可以从 Spring 应用上下文中获取已定义的
 * EntityManagerFactory bean（默认方式），也可以从 JNDI 获取
 * EntityManagerFactory 引用（"持久化单元引用"）。在 bean 方式下，
 * 持久化单元名称将与实际部署的单元进行匹配，如果未找到部署的名称，
 * 则以 bean 名称作为回退单元名称。通常使用 Spring 的
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * 来设置此类 EntityManagerFactory bean。另一种方式是通过 JNDI
 * 获取这些 bean，例如使用 {@code jee:jndi-lookup} XML 配置元素
 * （bean 名称与请求的单元名称相匹配）。在两种情况下，
 * 后处理器的定义都非常简单：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * 在 JNDI 方式下，请在此后处理器的
 * {@link #setPersistenceUnits "persistenceUnits" map} 中指定相应的 JNDI 名称，
 * 通常与 Java EE 部署描述符中匹配的 {@code persistence-unit-ref} 条目对应。
 * 默认情况下，这些名称被视为资源引用（依照 Java EE 资源引用约定），
 * 位于 "java:comp/env/" 命名空间之下。例如：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceUnits"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/unit1"/&gt;
 *       &lt;entry key="unit2" value="persistence/unit2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 在这种情况下，指定的持久化单元将始终从 JNDI 解析，
 * 而不会作为 Spring 定义的 bean 解析。整个持久化单元部署，
 * 包括持久化类的织入，都由 Java EE 服务器负责。
 * 持久化上下文（即 EntityManager 引用）将基于这些由服务器提供的
 * EntityManagerFactory 引用来构建，并使用 Spring 自身的事务同步设施
 * 进行事务性 EntityManager 处理（通常使用 Spring 的 {@code @Transactional}
 * 注解进行事务划分，并以 {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 作为后端）。
 *
 * <p>如果希望使用 Java EE 服务器自身的 EntityManager 处理方式，请在此后处理器的
 * {@link #setPersistenceContexts "persistenceContexts" map}
 * （或 {@link #setExtendedPersistenceContexts "extendedPersistenceContexts" map}）
 * 中指定条目，通常与 Java EE 部署描述符中匹配的 {@code persistence-context-ref}
 * 条目对应。例如：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceContexts"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/context1"/&gt;
 *       &lt;entry key="unit2" value="persistence/context2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 如果应用程序只需要获取 EntityManager 引用，
 * 这些就是需要指定的全部内容。如果还需要 EntityManagerFactory 引用，
 * 请同时为 "persistenceUnits" 和 "persistenceContexts" 指定条目，
 * 指向匹配的 JNDI 位置。
 *
 * <p><b>注意：通常不要将 EXTENDED EntityManager 注入到无状态 bean 中，
 * 即不要在作用域为 'singleton'（Spring 的默认作用域）的 Spring bean 中
 * 使用类型为 {@code EXTENDED} 的 {@code @PersistenceContext}。</b>
 * 扩展 EntityManager <i>不是</i>线程安全的，因此不得在并发访问的 bean 中使用
 * （而 Spring 管理的单例通常就是这种情况）。
 *
 * <p>注意："context:annotation-config" 和 "context:component-scan" XML 标签
 * 会注册一个默认的 PersistenceAnnotationBeanPostProcessor。
 * 如果要指定自定义的 PersistenceAnnotationBeanPostProcessor bean 定义，
 * 请移除或关闭其中的默认注解配置。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see javax.persistence.PersistenceUnit
 * @see javax.persistence.PersistenceContext
 */
@SuppressWarnings("serial")
public class PersistenceAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, DestructionAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, Serializable {

	@Nullable
	private Object jndiEnvironment;

	private boolean resourceRef = true;

	@Nullable
	private transient Map<String, String> persistenceUnits;

	@Nullable
	private transient Map<String, String> persistenceContexts;

	@Nullable
	private transient Map<String, String> extendedPersistenceContexts;

	private transient String defaultPersistenceUnitName = "";

	private int order = Ordered.LOWEST_PRECEDENCE - 4;

	@Nullable
	private transient ListableBeanFactory beanFactory;

	private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

	private final Map<Object, EntityManager> extendedEntityManagersToClose = new ConcurrentHashMap<>(16);


	/**
	 * 设置用于 JNDI 查找的 JNDI 模板。
	 * @see org.springframework.jndi.JndiAccessor#setJndiTemplate
	 */
	public void setJndiTemplate(Object jndiTemplate) {
		this.jndiEnvironment = jndiTemplate;
	}

	/**
	 * 设置用于 JNDI 查找的 JNDI 环境。
	 * @see org.springframework.jndi.JndiAccessor#setJndiEnvironment
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiEnvironment = jndiEnvironment;
	}

	/**
	 * 设置查找是否发生在 Java EE 容器中，即当 JNDI 名称尚未包含前缀
	 * "java:comp/env/" 时是否需要添加该前缀。
	 * PersistenceAnnotationBeanPostProcessor 的默认值为 "true"。
	 * @see org.springframework.jndi.JndiLocatorSupport#setResourceRef
	 */
	public void setResourceRef(boolean resourceRef) {
		this.resourceRef = resourceRef;
	}

	/**
	 * 指定用于 EntityManagerFactory 查找的持久化单元，
	 * 形式为从持久化单元名称到持久化单元 JNDI 名称的 Map
	 * （JNDI 名称需要解析为 EntityManagerFactory 实例）。
	 * <p>此处指定的 JNDI 名称应引用 Java EE 部署描述符中的
	 * {@code persistence-unit-ref} 条目，并与目标持久化单元相匹配。
	 * <p>如果注解中未指定单元名称，则采用
	 * {@link #setDefaultPersistenceUnitName 默认持久化单元} 所指定的值
	 * （默认为映射到空字符串的值），或者如果只有一个持久化单元，
	 * 就直接使用该单元。
	 * <p>这主要面向 Java EE 环境，所有查找都由标准 JPA 注解驱动，
	 * 所有 EntityManagerFactory 引用都从 JNDI 获取。
	 * 在这种情况下，无需单独定义 EntityManagerFactory bean。
	 * <p>如果未指定相应的 "persistenceContexts"/"extendedPersistenceContexts"，
	 * 则 {@code @PersistenceContext} 将被解析为基于此处定义的
	 * EntityManagerFactory 构建的 EntityManager。
	 * 请注意，这些将是 Spring 管理的 EntityManager，它们基于 Spring 的设施
	 * 实现事务同步。如果希望使用 Java EE 服务器自身的 EntityManager 处理方式，
	 * 请指定相应的 "persistenceContexts"/"extendedPersistenceContexts"。
	 */
	public void setPersistenceUnits(Map<String, String> persistenceUnits) {
		this.persistenceUnits = persistenceUnits;
	}

	/**
	 * 指定用于 EntityManager 查找的<i>事务性</i>持久化上下文，
	 * 形式为从持久化单元名称到持久化上下文 JNDI 名称的 Map
	 * （JNDI 名称需要解析为 EntityManager 实例）。
	 * <p>此处指定的 JNDI 名称应引用 Java EE 部署描述符中的
	 * {@code persistence-context-ref} 条目，与目标持久化单元相匹配，
	 * 并设置为持久化上下文类型 {@code Transaction}。
	 * <p>如果注解中未指定单元名称，则采用
	 * {@link #setDefaultPersistenceUnitName 默认持久化单元} 所指定的值
	 * （默认为映射到空字符串的值），或者如果只有一个持久化单元，
	 * 就直接使用该单元。
	 * <p>这主要面向 Java EE 环境，所有查找都由标准 JPA 注解驱动，
	 * 所有 EntityManager 引用都从 JNDI 获取。在这种情况下，无需单独定义
	 * EntityManagerFactory bean，所有 EntityManager 处理
	 * 都由 Java EE 服务器本身完成。
	 */
	public void setPersistenceContexts(Map<String, String> persistenceContexts) {
		this.persistenceContexts = persistenceContexts;
	}

	/**
	 * 指定用于 EntityManager 查找的<i>扩展</i>持久化上下文，
	 * 形式为从持久化单元名称到持久化上下文 JNDI 名称的 Map
	 * （JNDI 名称需要解析为 EntityManager 实例）。
	 * <p>此处指定的 JNDI 名称应引用 Java EE 部署描述符中的
	 * {@code persistence-context-ref} 条目，与目标持久化单元相匹配，
	 * 并设置为持久化上下文类型 {@code Extended}。
	 * <p>如果注解中未指定单元名称，则采用
	 * {@link #setDefaultPersistenceUnitName 默认持久化单元} 所指定的值
	 * （默认为映射到空字符串的值），或者如果只有一个持久化单元，
	 * 就直接使用该单元。
	 * <p>这主要面向 Java EE 环境，所有查找都由标准 JPA 注解驱动，
	 * 所有 EntityManager 引用都从 JNDI 获取。在这种情况下，无需单独定义
	 * EntityManagerFactory bean，所有 EntityManager 处理
	 * 都由 Java EE 服务器本身完成。
	 */
	public void setExtendedPersistenceContexts(Map<String, String> extendedPersistenceContexts) {
		this.extendedPersistenceContexts = extendedPersistenceContexts;
	}

	/**
	 * 指定默认持久化单元名称，当 {@code @PersistenceUnit} /
	 * {@code @PersistenceContext} 注解中未指定单元名称时使用。
	 * <p>这主要面向应用上下文中的查找，用于指示目标持久化单元名称
	 * （通常与 bean 名称一致），但也适用于
	 * {@link #setPersistenceUnits "persistenceUnits"} /
	 * {@link #setPersistenceContexts "persistenceContexts"} /
	 * {@link #setExtendedPersistenceContexts "extendedPersistenceContexts"} map
	 * 中的查找，从而避免为其中的空字符串重复配置映射。
	 * <p>默认行为是检查 Spring 应用上下文中是否存在单个 EntityManagerFactory bean
	 * （如果有）。如果存在多个这样的工厂，请指定此默认持久化单元名称，
	 * 或在注解中显式引用具名持久化单元。
	 */
	public void setDefaultPersistenceUnitName(@Nullable String unitName) {
		this.defaultPersistenceUnitName = (unitName != null ? unitName : "");
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		InjectionMetadata metadata = findPersistenceMetadata(beanName, beanType, null);
		metadata.checkConfigMembers(beanDefinition);
	}

	@Override
	public void resetBeanDefinition(String beanName) {
		this.injectionMetadataCache.remove(beanName);
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		InjectionMetadata metadata = findPersistenceMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of persistence dependencies failed", ex);
		}
		return pvs;
	}

	@Deprecated
	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return postProcessProperties(pvs, bean, beanName);
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) {
		EntityManager emToClose = this.extendedEntityManagersToClose.remove(bean);
		EntityManagerFactoryUtils.closeEntityManager(emToClose);
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return this.extendedEntityManagersToClose.containsKey(bean);
	}


	private InjectionMetadata findPersistenceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
		// Fall back to class name as cache key, for backwards compatibility with custom callers.
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					metadata = buildPersistenceMetadata(clazz);
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildPersistenceMetadata(Class<?> clazz) {
		if (!AnnotationUtils.isCandidateClass(clazz, Arrays.asList(PersistenceContext.class, PersistenceUnit.class))) {
			return InjectionMetadata.EMPTY;
		}

		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;

		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				if (field.isAnnotationPresent(PersistenceContext.class) ||
						field.isAnnotationPresent(PersistenceUnit.class)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("Persistence annotations are not supported on static fields");
					}
					currElements.add(new PersistenceElement(field, field, null));
				}
			});

			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				if ((bridgedMethod.isAnnotationPresent(PersistenceContext.class) ||
						bridgedMethod.isAnnotationPresent(PersistenceUnit.class)) &&
						method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					if (Modifier.isStatic(method.getModifiers())) {
						throw new IllegalStateException("Persistence annotations are not supported on static methods");
					}
					if (method.getParameterCount() != 1) {
						throw new IllegalStateException("Persistence annotation requires a single-arg method: " + method);
					}
					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
					currElements.add(new PersistenceElement(method, bridgedMethod, pd));
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return InjectionMetadata.forElements(elements, clazz);
	}

	/**
	 * 返回通过 "persistenceUnits" map 为给定单元名称指定的持久化单元。
	 * @param unitName the name of the persistence unit
	 * @return the corresponding EntityManagerFactory,
	 * or {@code null} if none found
	 * @see #setPersistenceUnits
	 */
	@Nullable
	protected EntityManagerFactory getPersistenceUnit(@Nullable String unitName) {
		if (this.persistenceUnits != null) {
			String unitNameForLookup = (unitName != null ? unitName : "");
			if (unitNameForLookup.isEmpty()) {
				unitNameForLookup = this.defaultPersistenceUnitName;
			}
			String jndiName = this.persistenceUnits.get(unitNameForLookup);
			if (jndiName == null && unitNameForLookup.isEmpty() && this.persistenceUnits.size() == 1) {
				jndiName = this.persistenceUnits.values().iterator().next();
			}
			if (jndiName != null) {
				try {
					return lookup(jndiName, EntityManagerFactory.class);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not obtain EntityManagerFactory [" + jndiName + "] from JNDI", ex);
				}
			}
		}
		return null;
	}

	/**
	 * 返回通过 "persistenceContexts"（或 "extendedPersistenceContexts"）map
	 * 为给定单元名称指定的持久化上下文。
	 * @param unitName the name of the persistence unit
	 * @param extended whether to obtain an extended persistence context
	 * @return the corresponding EntityManager, or {@code null} if none found
	 * @see #setPersistenceContexts
	 * @see #setExtendedPersistenceContexts
	 */
	@Nullable
	protected EntityManager getPersistenceContext(@Nullable String unitName, boolean extended) {
		Map<String, String> contexts = (extended ? this.extendedPersistenceContexts : this.persistenceContexts);
		if (contexts != null) {
			String unitNameForLookup = (unitName != null ? unitName : "");
			if (unitNameForLookup.isEmpty()) {
				unitNameForLookup = this.defaultPersistenceUnitName;
			}
			String jndiName = contexts.get(unitNameForLookup);
			if (jndiName == null && unitNameForLookup.isEmpty() && contexts.size() == 1) {
				jndiName = contexts.values().iterator().next();
			}
			if (jndiName != null) {
				try {
					return lookup(jndiName, EntityManager.class);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Could not obtain EntityManager [" + jndiName + "] from JNDI", ex);
				}
			}
		}
		return null;
	}

	/**
	 * 在当前 Spring 应用上下文中查找具有给定名称的 EntityManagerFactory；
	 * 如果未指定单元名称，则回退到单个默认的 EntityManagerFactory（如果有）。
	 * @param unitName the name of the persistence unit (may be {@code null} or empty)
	 * @param requestingBeanName the name of the requesting bean
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
	 */
	protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, @Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		String unitNameForLookup = (unitName != null ? unitName : "");
		if (unitNameForLookup.isEmpty()) {
			unitNameForLookup = this.defaultPersistenceUnitName;
		}
		if (!unitNameForLookup.isEmpty()) {
			return findNamedEntityManagerFactory(unitNameForLookup, requestingBeanName);
		}
		else {
			return findDefaultEntityManagerFactory(requestingBeanName);
		}
	}

	/**
	 * 在当前 Spring 应用上下文中查找具有给定名称的 EntityManagerFactory。
	 * @param unitName the name of the persistence unit (never empty)
	 * @param requestingBeanName the name of the requesting bean
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
	 */
	protected EntityManagerFactory findNamedEntityManagerFactory(String unitName, @Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		Assert.state(this.beanFactory != null, "ListableBeanFactory required for EntityManagerFactory bean lookup");

		EntityManagerFactory emf = EntityManagerFactoryUtils.findEntityManagerFactory(this.beanFactory, unitName);
		if (requestingBeanName != null && this.beanFactory instanceof ConfigurableBeanFactory) {
			((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(unitName, requestingBeanName);
		}
		return emf;
	}

	/**
	 * 在 Spring 应用上下文中查找单个默认的 EntityManagerFactory。
	 * @return the default EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException if there is no single EntityManagerFactory in the context
	 */
	protected EntityManagerFactory findDefaultEntityManagerFactory(@Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		Assert.state(this.beanFactory != null, "ListableBeanFactory required for EntityManagerFactory bean lookup");

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			// Fancy variant with dependency registration
			ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) this.beanFactory;
			NamedBeanHolder<EntityManagerFactory> emfHolder = clbf.resolveNamedBean(EntityManagerFactory.class);
			if (requestingBeanName != null) {
				clbf.registerDependentBean(emfHolder.getBeanName(), requestingBeanName);
			}
			return emfHolder.getBeanInstance();
		}
		else {
			// Plain variant: just find a default bean
			return this.beanFactory.getBean(EntityManagerFactory.class);
		}
	}

	/**
	 * 按名称对给定资源执行 JNDI 查找。
	 * <p>当为特定持久化单元映射了 JNDI 名称时，用于
	 * EntityManagerFactory 和 EntityManager 的查找。
	 * @param jndiName the JNDI name to look up
	 * @param requiredType the required type of the object
	 * @return the obtained object
	 * @throws Exception if the JNDI lookup failed
	 */
	protected <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
		return new LocatorDelegate().lookup(jndiName, requiredType);
	}


	/**
	 * 单独的内部类，用于隔离 JNDI API 依赖
	 * （以兼容 Google App Engine 的 API 白名单）。
	 */
	private class LocatorDelegate {

		public <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
			JndiLocatorDelegate locator = new JndiLocatorDelegate();
			if (jndiEnvironment instanceof JndiTemplate) {
				locator.setJndiTemplate((JndiTemplate) jndiEnvironment);
			}
			else if (jndiEnvironment instanceof Properties) {
				locator.setJndiEnvironment((Properties) jndiEnvironment);
			}
			else if (jndiEnvironment != null) {
				throw new IllegalStateException("Illegal 'jndiEnvironment' type: " + jndiEnvironment.getClass());
			}
			locator.setResourceRef(resourceRef);
			return locator.lookup(jndiName, requiredType);
		}
	}


	/**
	 * 表示带注解字段或 setter 方法注入信息的类。
	 */
	private class PersistenceElement extends InjectionMetadata.InjectedElement {

		private final String unitName;

		@Nullable
		private PersistenceContextType type;

		private boolean synchronizedWithTransaction = false;

		@Nullable
		private Properties properties;

		public PersistenceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			PersistenceContext pc = ae.getAnnotation(PersistenceContext.class);
			PersistenceUnit pu = ae.getAnnotation(PersistenceUnit.class);
			Class<?> resourceType = EntityManager.class;
			if (pc != null) {
				if (pu != null) {
					throw new IllegalStateException("Member may only be annotated with either " +
							"@PersistenceContext or @PersistenceUnit, not both: " + member);
				}
				Properties properties = null;
				PersistenceProperty[] pps = pc.properties();
				if (!ObjectUtils.isEmpty(pps)) {
					properties = new Properties();
					for (PersistenceProperty pp : pps) {
						properties.setProperty(pp.name(), pp.value());
					}
				}
				this.unitName = pc.unitName();
				this.type = pc.type();
				this.synchronizedWithTransaction = SynchronizationType.SYNCHRONIZED.equals(pc.synchronization());
				this.properties = properties;
			}
			else {
				resourceType = EntityManagerFactory.class;
				this.unitName = pu.unitName();
			}
			checkResourceType(resourceType);
		}

		/**
		 * 针对应用上下文解析对象。
		 */
		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			// Resolves to EntityManagerFactory or EntityManager.
			if (this.type != null) {
				return (this.type == PersistenceContextType.EXTENDED ?
						resolveExtendedEntityManager(target, requestingBeanName) :
						resolveEntityManager(requestingBeanName));
			}
			else {
				// OK, so we need an EntityManagerFactory...
				return resolveEntityManagerFactory(requestingBeanName);
			}
		}

		private EntityManagerFactory resolveEntityManagerFactory(@Nullable String requestingBeanName) {
			// Obtain EntityManagerFactory from JNDI?
			EntityManagerFactory emf = getPersistenceUnit(this.unitName);
			if (emf == null) {
				// Need to search for EntityManagerFactory beans.
				emf = findEntityManagerFactory(this.unitName, requestingBeanName);
			}
			return emf;
		}

		private EntityManager resolveEntityManager(@Nullable String requestingBeanName) {
			// Obtain EntityManager reference from JNDI?
			EntityManager em = getPersistenceContext(this.unitName, false);
			if (em == null) {
				// No pre-built EntityManager found -> build one based on factory.
				// Obtain EntityManagerFactory from JNDI?
				EntityManagerFactory emf = getPersistenceUnit(this.unitName);
				if (emf == null) {
					// Need to search for EntityManagerFactory beans.
					emf = findEntityManagerFactory(this.unitName, requestingBeanName);
				}
				// Inject a shared transactional EntityManager proxy.
				if (emf instanceof EntityManagerFactoryInfo &&
						((EntityManagerFactoryInfo) emf).getEntityManagerInterface() != null) {
					// Create EntityManager based on the info's vendor-specific type
					// (which might be more specific than the field's type).
					em = SharedEntityManagerCreator.createSharedEntityManager(
							emf, this.properties, this.synchronizedWithTransaction);
				}
				else {
					// Create EntityManager based on the field's type.
					em = SharedEntityManagerCreator.createSharedEntityManager(
							emf, this.properties, this.synchronizedWithTransaction, getResourceType());
				}
			}
			return em;
		}

		private EntityManager resolveExtendedEntityManager(Object target, @Nullable String requestingBeanName) {
			// Obtain EntityManager reference from JNDI?
			EntityManager em = getPersistenceContext(this.unitName, true);
			if (em == null) {
				// No pre-built EntityManager found -> build one based on factory.
				// Obtain EntityManagerFactory from JNDI?
				EntityManagerFactory emf = getPersistenceUnit(this.unitName);
				if (emf == null) {
					// Need to search for EntityManagerFactory beans.
					emf = findEntityManagerFactory(this.unitName, requestingBeanName);
				}
				// Inject a container-managed extended EntityManager.
				em = ExtendedEntityManagerCreator.createContainerManagedEntityManager(
						emf, this.properties, this.synchronizedWithTransaction);
			}
			if (em instanceof EntityManagerProxy && beanFactory != null && requestingBeanName != null &&
					beanFactory.containsBean(requestingBeanName) && !beanFactory.isPrototype(requestingBeanName)) {
				extendedEntityManagersToClose.put(target, ((EntityManagerProxy) em).getTargetEntityManager());
			}
			return em;
		}
	}

}
