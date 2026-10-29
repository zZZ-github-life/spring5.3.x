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

package org.springframework.context.annotation;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} 实现，开箱即用地支持常见的
 * Java 注解，特别是 {@code javax.annotation} 包中的 JSR-250 注解。这些常见 Java 注解在许多 Java EE
 * 5 技术（例如 JSF 1.2）以及 Java 6 的 JAX-WS 中都受支持。
 *
 * <p>此后处理器支持 {@link javax.annotation.PostConstruct} 和 {@link javax.annotation.PreDestroy}
 * 注解——分别作为初始化注解和销毁注解——其方式是通过继承
 * {@link InitDestroyAnnotationBeanPostProcessor} 并预配置注解类型。
 *
 * <p>核心元素是 {@link javax.annotation.Resource} 注解，用于注解驱动的具名 Bean 注入，默认从所在的
 * Spring BeanFactory 注入，只有 {@code mappedName} 引用在 JNDI 中解析。
 * {@link #setAlwaysUseJndiLookup "alwaysUseJndiLookup" 标志}会强制执行等同于标准 Java EE 5 资源注
 * 入的 JNDI 查找，对 {@code name} 引用和默认名称亦如此。目标 Bean 可以是简单的 POJO，除了类型必须
 * 匹配之外没有特殊要求。
 *
 * <p>JAX-WS 的 {@link javax.xml.ws.WebServiceRef} 注解也受支持，其方式类似于
 * {@link javax.annotation.Resource}，但能够创建特定的 JAX-WS 服务端点。它可以按名称指向显式定义的
 * 资源，也可以操作本地指定的 JAX-WS 服务类。最后，此后处理器还支持 EJB 3 的 {@link javax.ejb.EJB}
 * 注解，同样类似于 {@link javax.annotation.Resource}，并且能够同时指定本地 Bean 名称和用于回退检索
 * 的全局 JNDI 名称。在这种情况下，目标 Bean 既可以是普通 POJO，也可以是 EJB 3 会话 Bean。
 *
 * <p>此后处理器支持的常见注解在 Java 6（JDK 1.6）以及 Java EE 5/6 中可用（Java EE 5/6 也为其常见注
 * 解提供了独立的 jar，从而允许在任何基于它的应用中使用）。
 *
 * <p>对于默认用法，即将资源名称解析为 Spring Bean 名称，只需在应用上下文中定义以下内容：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * 对于直接 JNDI 访问，即在 Java EE 应用的 “java:comp/env/” 命名空间内将资源名称解析为 JNDI 资源引
 * 用，请使用以下内容：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="alwaysUseJndiLookup" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * {@code mappedName} 引用将始终在 JNDI 中解析，因此也支持全局 JNDI 名称（包括 “java:” 前缀）。
 * “alwaysUseJndiLookup” 标志仅影响 {@code name} 引用和默认名称（从字段名/属性名推断）。
 *
 * <p><b>注意：</b>默认的 CommonAnnotationBeanPostProcessor 将由 “context:annotation-config” 和
 * “context:component-scan” XML 标签注册。如果你打算指定自定义的 CommonAnnotationBeanPostProcessor
 * Bean 定义，请在那里移除或关闭默认的注解配置！<p><b>注意：</b>注解注入将在 XML 注入<i>之前</i>执
 * 行；因此对于通过两种方式都装配的属性，后者的配置将覆盖前者。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see #setAlwaysUseJndiLookup
 * @see #setResourceFactory
 * @see org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 */
@SuppressWarnings("serial")
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {

	// Defensive reference to JNDI API for JDK 9+ (optional java.naming module)
	private static final boolean jndiPresent = ClassUtils.isPresent(
			"javax.naming.InitialContext", CommonAnnotationBeanPostProcessor.class.getClassLoader());

	private static final Set<Class<? extends Annotation>> resourceAnnotationTypes = new LinkedHashSet<>(4);

	@Nullable
	private static final Class<? extends Annotation> webServiceRefClass;

	@Nullable
	private static final Class<? extends Annotation> ejbClass;

	static {
		resourceAnnotationTypes.add(Resource.class);

		webServiceRefClass = loadAnnotationType("javax.xml.ws.WebServiceRef");
		if (webServiceRefClass != null) {
			resourceAnnotationTypes.add(webServiceRefClass);
		}

		ejbClass = loadAnnotationType("javax.ejb.EJB");
		if (ejbClass != null) {
			resourceAnnotationTypes.add(ejbClass);
		}
	}


	private final Set<String> ignoredResourceTypes = new HashSet<>(1);

	private boolean fallbackToDefaultTypeMatch = true;

	private boolean alwaysUseJndiLookup = false;

	@Nullable
	private transient BeanFactory jndiFactory;

	@Nullable
	private transient BeanFactory resourceFactory;

	@Nullable
	private transient BeanFactory beanFactory;

	@Nullable
	private transient StringValueResolver embeddedValueResolver;

	private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);


	/**
	 * 创建一个新的 CommonAnnotationBeanPostProcessor，并将初始化和销毁注解类型分别设置为
	 * {@link javax.annotation.PostConstruct} 和 {@link javax.annotation.PreDestroy}。
	 */
	public CommonAnnotationBeanPostProcessor() {
		setOrder(Ordered.LOWEST_PRECEDENCE - 3);
		setInitAnnotationType(PostConstruct.class);
		setDestroyAnnotationType(PreDestroy.class);
		ignoreResourceType("javax.xml.ws.WebServiceContext");

		// java.naming module present on JDK 9+?
		if (jndiPresent) {
			this.jndiFactory = new SimpleJndiBeanFactory();
		}
	}


	/**
	 * 在解析 {@code @Resource} 注解时忽略给定的资源类型。<p>默认情况下，
	 * {@code javax.xml.ws.WebServiceContext} 接口会被忽略，因为它将由 JAX-WS 运行时解析。
	 * @param resourceType the resource type to ignore
	 */
	public void ignoreResourceType(String resourceType) {
		Assert.notNull(resourceType, "Ignored resource type must not be null");
		this.ignoredResourceTypes.add(resourceType);
	}

	/**
	 * 设置在没有指定显式名称时是否允许回退到类型匹配。默认名称（即字段名或 Bean 属性名）仍会首先检查；
	 * 如果存在该名称的 Bean，则会使用它。但是，如果不存在该名称的 Bean，并且此标志为 “true”，则会尝试
	 * 按类型解析依赖。<p>默认值为 “true”。将此标志切换为 “false” 可在所有情况下强制执行按名称查找，在
	 * 没有名称匹配时抛出异常。
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#resolveDependency
	 */
	public void setFallbackToDefaultTypeMatch(boolean fallbackToDefaultTypeMatch) {
		this.fallbackToDefaultTypeMatch = fallbackToDefaultTypeMatch;
	}

	/**
	 * 设置是否始终使用等同于标准 Java EE 5 资源注入的 JNDI 查找，<b>即使对于 {@code name} 属性和默认名
	 * 称也是如此</b>。<p>默认值为 “false”：资源名称用于在所在的 BeanFactory 中查找 Spring Bean；只有
	 * {@code mappedName} 属性直接指向 JNDI。将此标志切换为 “true” 可在任何情况下强制执行 Java EE 风格
	 * 的 JNDI 查找，即使对于 {@code name} 属性和默认名称也是如此。
	 * @see #setJndiFactory
	 * @see #setResourceFactory
	 */
	public void setAlwaysUseJndiLookup(boolean alwaysUseJndiLookup) {
		this.alwaysUseJndiLookup = alwaysUseJndiLookup;
	}

	/**
	 * 指定用于注入到标注了 {@code @Resource} / {@code @WebServiceRef} / {@code @EJB} 的字段和 setter
	 * 方法中的对象的工厂，<b>适用于直接指向 JNDI 的 {@code mappedName} 属性</b>。如果
	 * “alwaysUseJndiLookup” 设置为 “true” 以强制对 {@code name} 属性和默认名称也执行 JNDI 查找，则也会
	 * 使用此工厂。<p>默认是一个 {@link org.springframework.jndi.support.SimpleJndiBeanFactory}，其
	 * JNDI 查找行为等同于标准 Java EE 5 资源注入。
	 * @see #setResourceFactory
	 * @see #setAlwaysUseJndiLookup
	 */
	public void setJndiFactory(BeanFactory jndiFactory) {
		Assert.notNull(jndiFactory, "BeanFactory must not be null");
		this.jndiFactory = jndiFactory;
	}

	/**
	 * 指定用于注入到标注了 {@code @Resource} / {@code @WebServiceRef} / {@code @EJB} 的字段和 setter
	 * 方法中的对象的工厂，<b>适用于 {@code name} 属性和默认名称</b>。<p>默认是定义此后处理器的
	 * BeanFactory（如果有），它将资源名称作为 Spring Bean 名称进行查找。对于此后处理器的编程式使用，请
	 * 显式指定资源工厂。<p>指定 Spring 的
	 * {@link org.springframework.jndi.support.SimpleJndiBeanFactory} 会带来等同于标准 Java EE 5 资源注
	 * 入的 JNDI 查找行为，即使对于 {@code name} 属性和默认名称也是如此。这与 “alwaysUseJndiLookup” 标
	 * 志所启用的行为相同。
	 * @see #setAlwaysUseJndiLookup
	 */
	public void setResourceFactory(BeanFactory resourceFactory) {
		Assert.notNull(resourceFactory, "BeanFactory must not be null");
		this.resourceFactory = resourceFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		if (this.resourceFactory == null) {
			this.resourceFactory = beanFactory;
		}
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
		InjectionMetadata metadata = findResourceMetadata(beanName, beanType, null);
		metadata.checkConfigMembers(beanDefinition);
	}

	@Override
	public void resetBeanDefinition(String beanName) {
		this.injectionMetadataCache.remove(beanName);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
		return true;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
		}
		return pvs;
	}

	@Deprecated
	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return postProcessProperties(pvs, bean, beanName);
	}


	private InjectionMetadata findResourceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
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
					metadata = buildResourceMetadata(clazz);
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildResourceMetadata(Class<?> clazz) {
		if (!AnnotationUtils.isCandidateClass(clazz, resourceAnnotationTypes)) {
			return InjectionMetadata.EMPTY;
		}

		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;

		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				if (webServiceRefClass != null && field.isAnnotationPresent(webServiceRefClass)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("@WebServiceRef annotation is not supported on static fields");
					}
					currElements.add(new WebServiceRefElement(field, field, null));
				}
				else if (ejbClass != null && field.isAnnotationPresent(ejbClass)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("@EJB annotation is not supported on static fields");
					}
					currElements.add(new EjbRefElement(field, field, null));
				}
				else if (field.isAnnotationPresent(Resource.class)) {
					if (Modifier.isStatic(field.getModifiers())) {
						throw new IllegalStateException("@Resource annotation is not supported on static fields");
					}
					if (!this.ignoredResourceTypes.contains(field.getType().getName())) {
						currElements.add(new ResourceElement(field, field, null));
					}
				}
			});

			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				if (method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					if (webServiceRefClass != null && bridgedMethod.isAnnotationPresent(webServiceRefClass)) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("@WebServiceRef annotation is not supported on static methods");
						}
						if (method.getParameterCount() != 1) {
							throw new IllegalStateException("@WebServiceRef annotation requires a single-arg method: " + method);
						}
						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
						currElements.add(new WebServiceRefElement(method, bridgedMethod, pd));
					}
					else if (ejbClass != null && bridgedMethod.isAnnotationPresent(ejbClass)) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("@EJB annotation is not supported on static methods");
						}
						if (method.getParameterCount() != 1) {
							throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
						}
						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
						currElements.add(new EjbRefElement(method, bridgedMethod, pd));
					}
					else if (bridgedMethod.isAnnotationPresent(Resource.class)) {
						if (Modifier.isStatic(method.getModifiers())) {
							throw new IllegalStateException("@Resource annotation is not supported on static methods");
						}
						Class<?>[] paramTypes = method.getParameterTypes();
						if (paramTypes.length != 1) {
							throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
						}
						if (!this.ignoredResourceTypes.contains(paramTypes[0].getName())) {
							PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
							currElements.add(new ResourceElement(method, bridgedMethod, pd));
						}
					}
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return InjectionMetadata.forElements(elements, clazz);
	}

	/**
	 * 为给定的名称和类型获取一个延迟解析的资源代理，在方法调用到来时按需委托给 {@link #getResource}。
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never {@code null})
	 * @since 4.2
	 * @see #getResource
	 * @see Lazy
	 */
	protected Object buildLazyResourceProxy(final LookupElement element, final @Nullable String requestingBeanName) {
		TargetSource ts = new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return element.lookupType;
			}
			@Override
			public boolean isStatic() {
				return false;
			}
			@Override
			public Object getTarget() {
				return getResource(element, requestingBeanName);
			}
			@Override
			public void releaseTarget(Object target) {
			}
		};

		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		if (element.lookupType.isInterface()) {
			pf.addInterface(element.lookupType);
		}
		ClassLoader classLoader = (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() : null);
		return pf.getProxy(classLoader);
	}

	/**
	 * 获取给定名称和类型的资源对象。
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never {@code null})
	 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
	 */
	protected Object getResource(LookupElement element, @Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		// JNDI lookup to perform?
		String jndiName = null;
		if (StringUtils.hasLength(element.mappedName)) {
			jndiName = element.mappedName;
		}
		else if (this.alwaysUseJndiLookup) {
			jndiName = element.name;
		}
		if (jndiName != null) {
			if (this.jndiFactory == null) {
				throw new NoSuchBeanDefinitionException(element.lookupType,
						"No JNDI factory configured - specify the 'jndiFactory' property");
			}
			return this.jndiFactory.getBean(jndiName, element.lookupType);
		}

		// Regular resource autowiring
		if (this.resourceFactory == null) {
			throw new NoSuchBeanDefinitionException(element.lookupType,
					"No resource factory configured - specify the 'resourceFactory' property");
		}
		return autowireResource(this.resourceFactory, element, requestingBeanName);
	}

	/**
	 * 基于给定的工厂通过自动装配获取给定名称和类型的资源对象。
	 * @param factory the factory to autowire against
	 * @param element the descriptor for the annotated field/method
	 * @param requestingBeanName the name of the requesting bean
	 * @return the resource object (never {@code null})
	 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
	 */
	protected Object autowireResource(BeanFactory factory, LookupElement element, @Nullable String requestingBeanName)
			throws NoSuchBeanDefinitionException {

		Object resource;
		Set<String> autowiredBeanNames;
		String name = element.name;

		if (factory instanceof AutowireCapableBeanFactory) {
			AutowireCapableBeanFactory beanFactory = (AutowireCapableBeanFactory) factory;
			DependencyDescriptor descriptor = element.getDependencyDescriptor();
			if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {
				autowiredBeanNames = new LinkedHashSet<>();
				resource = beanFactory.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, null);
				if (resource == null) {
					throw new NoSuchBeanDefinitionException(element.getLookupType(), "No resolvable resource object");
				}
			}
			else {
				resource = beanFactory.resolveBeanByName(name, descriptor);
				autowiredBeanNames = Collections.singleton(name);
			}
		}
		else {
			resource = factory.getBean(name, element.lookupType);
			autowiredBeanNames = Collections.singleton(name);
		}

		if (factory instanceof ConfigurableBeanFactory) {
			ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
			for (String autowiredBeanName : autowiredBeanNames) {
				if (requestingBeanName != null && beanFactory.containsBean(autowiredBeanName)) {
					beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
				}
			}
		}

		return resource;
	}


	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Annotation> loadAnnotationType(String name) {
		try {
			return (Class<? extends Annotation>)
					ClassUtils.forName(name, CommonAnnotationBeanPostProcessor.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}


	/**
	 * 表示关于标注字段或 setter 方法的通用注入信息的类，支持 @Resource 及相关注解。
	 */
	protected abstract static class LookupElement extends InjectionMetadata.InjectedElement {

		protected String name = "";

		protected boolean isDefaultName = false;

		protected Class<?> lookupType = Object.class;

		@Nullable
		protected String mappedName;

		public LookupElement(Member member, @Nullable PropertyDescriptor pd) {
			super(member, pd);
		}

		/**
		 * 返回用于查找的资源名称。
		 */
		public final String getName() {
			return this.name;
		}

		/**
		 * 返回用于查找的所需类型。
		 */
		public final Class<?> getLookupType() {
			return this.lookupType;
		}

		/**
		 * 为底层字段/方法构建一个 DependencyDescriptor。
		 */
		public final DependencyDescriptor getDependencyDescriptor() {
			if (this.isField) {
				return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
			}
			else {
				return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
			}
		}
	}


	/**
	 * 表示关于标注字段或 setter 方法的注入信息的类，支持 @Resource 注解。
	 */
	private class ResourceElement extends LookupElement {

		private final boolean lazyLookup;

		public ResourceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			Resource resource = ae.getAnnotation(Resource.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			else if (embeddedValueResolver != null) {
				resourceName = embeddedValueResolver.resolveStringValue(resourceName);
			}
			if (Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = (resourceName != null ? resourceName : "");
			this.lookupType = resourceType;
			String lookupValue = resource.lookup();
			this.mappedName = (StringUtils.hasLength(lookupValue) ? lookupValue : resource.mappedName());
			Lazy lazy = ae.getAnnotation(Lazy.class);
			this.lazyLookup = (lazy != null && lazy.value());
		}

		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) :
					getResource(this, requestingBeanName));
		}
	}


	/**
	 * 表示关于标注字段或 setter 方法的注入信息的类，支持 @WebServiceRef 注解。
	 */
	private class WebServiceRefElement extends LookupElement {

		private final Class<?> elementType;

		private final String wsdlLocation;

		public WebServiceRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			WebServiceRef resource = ae.getAnnotation(WebServiceRef.class);
			String resourceName = resource.name();
			Class<?> resourceType = resource.type();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			if (Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.name = resourceName;
			this.elementType = resourceType;
			if (Service.class.isAssignableFrom(resourceType)) {
				this.lookupType = resourceType;
			}
			else {
				this.lookupType = resource.value();
			}
			this.mappedName = resource.mappedName();
			this.wsdlLocation = resource.wsdlLocation();
		}

		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			Service service;
			try {
				service = (Service) getResource(this, requestingBeanName);
			}
			catch (NoSuchBeanDefinitionException notFound) {
				// Service to be created through generated class.
				if (Service.class == this.lookupType) {
					throw new IllegalStateException("No resource with name '" + this.name + "' found in context, " +
							"and no specific JAX-WS Service subclass specified. The typical solution is to either specify " +
							"a LocalJaxWsServiceFactoryBean with the given name or to specify the (generated) Service " +
							"subclass as @WebServiceRef(...) value.");
				}
				if (StringUtils.hasLength(this.wsdlLocation)) {
					try {
						Constructor<?> ctor = this.lookupType.getConstructor(URL.class, QName.class);
						WebServiceClient clientAnn = this.lookupType.getAnnotation(WebServiceClient.class);
						if (clientAnn == null) {
							throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
									"] does not carry a WebServiceClient annotation");
						}
						service = (Service) BeanUtils.instantiateClass(ctor,
								new URL(this.wsdlLocation), new QName(clientAnn.targetNamespace(), clientAnn.name()));
					}
					catch (NoSuchMethodException ex) {
						throw new IllegalStateException("JAX-WS Service class [" + this.lookupType.getName() +
								"] does not have a (URL, QName) constructor. Cannot apply specified WSDL location [" +
								this.wsdlLocation + "].");
					}
					catch (MalformedURLException ex) {
						throw new IllegalArgumentException(
								"Specified WSDL location [" + this.wsdlLocation + "] isn't a valid URL");
					}
				}
				else {
					service = (Service) BeanUtils.instantiateClass(this.lookupType);
				}
			}
			return service.getPort(this.elementType);
		}
	}


	/**
	 * 表示关于标注字段或 setter 方法的注入信息的类，支持 @EJB 注解。
	 */
	private class EjbRefElement extends LookupElement {

		private final String beanName;

		public EjbRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
			super(member, pd);
			EJB resource = ae.getAnnotation(EJB.class);
			String resourceBeanName = resource.beanName();
			String resourceName = resource.name();
			this.isDefaultName = !StringUtils.hasLength(resourceName);
			if (this.isDefaultName) {
				resourceName = this.member.getName();
				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
					resourceName = Introspector.decapitalize(resourceName.substring(3));
				}
			}
			Class<?> resourceType = resource.beanInterface();
			if (Object.class != resourceType) {
				checkResourceType(resourceType);
			}
			else {
				// No resource type specified... check field/method.
				resourceType = getResourceType();
			}
			this.beanName = resourceBeanName;
			this.name = resourceName;
			this.lookupType = resourceType;
			this.mappedName = resource.mappedName();
		}

		@Override
		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
			if (StringUtils.hasLength(this.beanName)) {
				if (beanFactory != null && beanFactory.containsBean(this.beanName)) {
					// Local match found for explicitly specified local bean name.
					Object bean = beanFactory.getBean(this.beanName, this.lookupType);
					if (requestingBeanName != null && beanFactory instanceof ConfigurableBeanFactory) {
						((ConfigurableBeanFactory) beanFactory).registerDependentBean(this.beanName, requestingBeanName);
					}
					return bean;
				}
				else if (this.isDefaultName && !StringUtils.hasLength(this.mappedName)) {
					throw new NoSuchBeanDefinitionException(this.beanName,
							"Cannot resolve 'beanName' in local BeanFactory. Consider specifying a general 'name' value instead.");
				}
			}
			// JNDI name lookup - may still go to a local BeanFactory.
			return getResource(this, requestingBeanName);
		}
	}


	/**
	 * DependencyDescriptor 类的扩展，用指定的资源类型覆盖依赖类型。
	 */
	private static class LookupDependencyDescriptor extends DependencyDescriptor {

		private final Class<?> lookupType;

		public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
			super(field, true);
			this.lookupType = lookupType;
		}

		public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
			super(new MethodParameter(method, 0), true);
			this.lookupType = lookupType;
		}

		@Override
		public Class<?> getDependencyType() {
			return this.lookupType;
		}
	}

}
