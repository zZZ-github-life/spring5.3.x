/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * 为 Java 类缓存 JavaBeans {@link java.beans.PropertyDescriptor} 信息的内部类。不适用于应用程序代
 * 码直接使用。
 *
 * <p>对于 Spring 自身在应用程序 {@link ClassLoader} 内缓存 Bean 描述符是必要的，而不是依赖 JDK 系
 * 统范围的 {@link BeanInfo} 缓存（以避免在共享 JVM 中单个应用程序关闭时发生泄漏）。
 *
 * <p>信息以静态方式缓存，因此无需为每个操作的 JavaBean 创建此类的新对象。因此，此类实现了工厂设计
 * 模式，使用私有构造方法和静态的 {@link #forClass(Class)} 工厂方法来获取实例。
 *
 * <p>请注意，要使缓存有效工作，需要满足一些前提条件：最好安排 Spring jar 与应用程序类位于同一个
 * ClassLoader 中，这样在任何情况下都能随应用程序的生命周期进行干净的缓存。对于 Web 应用程序，如果
 * 是多 ClassLoader 布局，可考虑在 {@code web.xml} 中声明一个本地
 * {@link org.springframework.web.util.IntrospectorCleanupListener}，这也将允许有效缓存。
 *
 * <p>如果 ClassLoader 布局不干净且未设置清理监听器，此类将回退到基于弱引用的缓存模型，每次垃圾回收
 * 器移除条目时都会重新创建被大量请求的条目。在这种情况下，可考虑使用
 * {@link #IGNORE_BEANINFO_PROPERTY_NAME} 系统属性。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 05 May 2001
 * @see #acceptClassLoader(ClassLoader)
 * @see #clearClassLoader(ClassLoader)
 * @see #forClass(Class)
 */
public final class CachedIntrospectionResults {

	/**
	 * 系统属性，指示 Spring 在调用 JavaBeans {@link Introspector} 时使用
	 * {@link Introspector#IGNORE_ALL_BEANINFO} 模式："spring.beaninfo.ignore"，值为 "true" 时会跳过对
	 * {@code BeanInfo} 类的搜索（通常适用于应用程序中一开始就未为此类 Bean 定义任何此类类的场景）。<p>
	 * 默认值为 "false"，会考虑所有 {@code BeanInfo} 元数据类，与标准的
	 * {@link Introspector#getBeanInfo(Class)} 调用相同。如果你遇到对不存在的 {@code BeanInfo} 类反复进
	 * 行 ClassLoader 访问，且此类访问在启动或延迟加载时开销很大，可考虑将此标志切换为 "true"。<p>请注
	 * 意，这种影响也可能表明缓存未有效工作的场景：最好安排 Spring jar 与应用程序类位于同一个
	 * ClassLoader 中，这样在任何情况下都能随应用程序的生命周期进行干净的缓存。对于 Web 应用程序，如果
	 * 是多 ClassLoader 布局，可考虑在 {@code web.xml} 中声明一个本地
	 * {@link org.springframework.web.util.IntrospectorCleanupListener}，这也将允许有效缓存。
	 * @see Introspector#getBeanInfo(Class, int)
	 */
	public static final String IGNORE_BEANINFO_PROPERTY_NAME = "spring.beaninfo.ignore";


	private static final boolean shouldIntrospectorIgnoreBeaninfoClasses =
			SpringProperties.getFlag(IGNORE_BEANINFO_PROPERTY_NAME);

	/** 存储 BeanInfoFactory 实例。 */
	private static final List<BeanInfoFactory> beanInfoFactories = SpringFactoriesLoader.loadFactories(
			BeanInfoFactory.class, CachedIntrospectionResults.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

	/**
	 * 此 CachedIntrospectionResults 类始终接受其类的 ClassLoader 集合，即使这些类不符合缓存安全条件。
	 */
	static final Set<ClassLoader> acceptedClassLoaders =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * 以 Class 为键、包含 CachedIntrospectionResults 的 Map，采用强引用。此变体用于缓存安全的 Bean 类。
	 *
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache =
			new ConcurrentHashMap<>(64);

	/**
	 * 以 Class 为键、包含 CachedIntrospectionResults 的 Map，采用软引用。此变体用于非缓存安全的 Bean
	 * 类。
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache =
			new ConcurrentReferenceHashMap<>(64);


	/**
	 * 将给定 ClassLoader 接受为缓存安全，即使其类在此 CachedIntrospectionResults 类中不符合缓存安全条
	 * 件。<p>此配置方法仅在 Spring 类驻留于 'common' ClassLoader（例如系统 ClassLoader）且其生命周期不
	 * 与应用程序耦合的场景中才有意义。在这种场景下，CachedIntrospectionResults 默认不会缓存应用程序的
	 * 任何类，因为它们会在 common ClassLoader 中造成泄漏。<p>应用程序启动时的任何
	 * {@code acceptClassLoader} 调用都应与应用程序关闭时的 {@link #clearClassLoader} 调用配对使用。
	 * @param classLoader the ClassLoader to accept
	 */
	public static void acceptClassLoader(@Nullable ClassLoader classLoader) {
		if (classLoader != null) {
			acceptedClassLoaders.add(classLoader);
		}
	}

	/**
	 * 清除给定 ClassLoader 的内省缓存，移除该 ClassLoader 下所有类的内省结果，并将该 ClassLoader（及其
	 * 子 ClassLoader）从接受列表中移除。
	 * @param classLoader the ClassLoader to clear the cache for
	 */
	public static void clearClassLoader(@Nullable ClassLoader classLoader) {
		acceptedClassLoaders.removeIf(new Predicate<ClassLoader>() {
			@Override
			public boolean test(ClassLoader registeredLoader) {
				return isUnderneathClassLoader(registeredLoader, classLoader);
			}
		});
		strongClassCache.keySet().removeIf(beanClass ->
				isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
		softClassCache.keySet().removeIf(beanClass ->
				isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
	}

	/**
	 * 为给定 Bean 类创建 CachedIntrospectionResults。
	 * @param beanClass the bean class to analyze
	 * @return the corresponding CachedIntrospectionResults
	 * @throws BeansException in case of introspection failure
	 */
	static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
		CachedIntrospectionResults results = strongClassCache.get(beanClass);
		if (results != null) {
			return results;
		}
		results = softClassCache.get(beanClass);
		if (results != null) {
			return results;
		}

		results = new CachedIntrospectionResults(beanClass);
		ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse;

		if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader()) ||
				isClassLoaderAccepted(beanClass.getClassLoader())) {
			classCacheToUse = strongClassCache;
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
			}
			classCacheToUse = softClassCache;
		}

		CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
		return (existing != null ? existing : results);
	}

	/**
	 * 检查此 CachedIntrospectionResults 类是否配置为接受给定 ClassLoader。
	 * @param classLoader the ClassLoader to check
	 * @return whether the given ClassLoader is accepted
	 * @see #acceptClassLoader
	 */
	private static boolean isClassLoaderAccepted(ClassLoader classLoader) {
		for (ClassLoader acceptedLoader : acceptedClassLoaders) {
			if (isUnderneathClassLoader(classLoader, acceptedLoader)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定 ClassLoader 是否位于给定父级之下，即父级是否在候选者的层级结构中。
	 * @param candidate the candidate ClassLoader to check
	 * @param parent the parent ClassLoader to check for
	 */
	private static boolean isUnderneathClassLoader(@Nullable ClassLoader candidate, @Nullable ClassLoader parent) {
		if (candidate == parent) {
			return true;
		}
		if (candidate == null) {
			return false;
		}
		ClassLoader classLoaderToCheck = candidate;
		while (classLoaderToCheck != null) {
			classLoaderToCheck = classLoaderToCheck.getParent();
			if (classLoaderToCheck == parent) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取给定目标类的 {@link BeanInfo} 描述符。
	 * @param beanClass the target class to introspect
	 * @return the resulting {@code BeanInfo} descriptor (never {@code null})
	 * @throws IntrospectionException from the underlying {@link Introspector}
	 */
	private static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		for (BeanInfoFactory beanInfoFactory : beanInfoFactories) {
			BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanClass);
			if (beanInfo != null) {
				return beanInfo;
			}
		}
		return (shouldIntrospectorIgnoreBeaninfoClasses ?
				Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO) :
				Introspector.getBeanInfo(beanClass));
	}


	/** 被内省 bean 类的 BeanInfo 对象。 */
	private final BeanInfo beanInfo;

	/** 以属性名 String 为键的 PropertyDescriptor 对象。 */
	private final Map<String, PropertyDescriptor> propertyDescriptors;

	/** 以 PropertyDescriptor 为键的 TypeDescriptor 对象。 */
	private final ConcurrentMap<PropertyDescriptor, TypeDescriptor> typeDescriptorCache;


	/**
	 * 为给定类创建一个新的 CachedIntrospectionResults 实例。
	 * @param beanClass the bean class to analyze
	 * @throws BeansException in case of introspection failure
	 */
	private CachedIntrospectionResults(Class<?> beanClass) throws BeansException {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
			}
			this.beanInfo = getBeanInfo(beanClass);

			if (logger.isTraceEnabled()) {
				logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
			}
			this.propertyDescriptors = new LinkedHashMap<>();

			Set<String> readMethodNames = new HashSet<>();

			// This call is slow so we do it once.
			PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (Class.class == beanClass && !("name".equals(pd.getName()) ||
						(pd.getName().endsWith("Name") && String.class == pd.getPropertyType()))) {
					// Only allow all name variants of Class properties
					continue;
				}
				if (URL.class == beanClass && "content".equals(pd.getName())) {
					// Only allow URL attribute introspection, not content resolution
					continue;
				}
				if (pd.getWriteMethod() == null && isInvalidReadOnlyPropertyType(pd.getPropertyType(), beanClass)) {
					// Ignore read-only properties such as ClassLoader - no need to bind to those
					continue;
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Found bean property '" + pd.getName() + "'" +
							(pd.getPropertyType() != null ? " of type [" + pd.getPropertyType().getName() + "]" : "") +
							(pd.getPropertyEditorClass() != null ?
									"; editor [" + pd.getPropertyEditorClass().getName() + "]" : ""));
				}
				pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
				this.propertyDescriptors.put(pd.getName(), pd);
				Method readMethod = pd.getReadMethod();
				if (readMethod != null) {
					readMethodNames.add(readMethod.getName());
				}
			}

			// Explicitly check implemented interfaces for setter/getter methods as well,
			// in particular for Java 8 default methods...
			Class<?> currClass = beanClass;
			while (currClass != null && currClass != Object.class) {
				introspectInterfaces(beanClass, currClass, readMethodNames);
				currClass = currClass.getSuperclass();
			}

			// Check for record-style accessors without prefix: e.g. "lastName()"
			// - accessor method directly referring to instance field of same name
			// - same convention for component accessors of Java 15 record classes
			introspectPlainAccessors(beanClass, readMethodNames);

			this.typeDescriptorCache = new ConcurrentReferenceHashMap<>();
		}
		catch (IntrospectionException ex) {
			throw new FatalBeanException("Failed to obtain BeanInfo for class [" + beanClass.getName() + "]", ex);
		}
	}

	private void introspectInterfaces(Class<?> beanClass, Class<?> currClass, Set<String> readMethodNames)
			throws IntrospectionException {

		for (Class<?> ifc : currClass.getInterfaces()) {
			if (!ClassUtils.isJavaLanguageInterface(ifc)) {
				for (PropertyDescriptor pd : getBeanInfo(ifc).getPropertyDescriptors()) {
					PropertyDescriptor existingPd = this.propertyDescriptors.get(pd.getName());
					if (existingPd == null ||
							(existingPd.getReadMethod() == null && pd.getReadMethod() != null)) {
						// GenericTypeAwarePropertyDescriptor leniently resolves a set* write method
						// against a declared read method, so we prefer read method descriptors here.
						pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
						if (pd.getWriteMethod() == null &&
								isInvalidReadOnlyPropertyType(pd.getPropertyType(), beanClass)) {
							// Ignore read-only properties such as ClassLoader - no need to bind to those
							continue;
						}
						this.propertyDescriptors.put(pd.getName(), pd);
						Method readMethod = pd.getReadMethod();
						if (readMethod != null) {
							readMethodNames.add(readMethod.getName());
						}
					}
				}
				introspectInterfaces(ifc, ifc, readMethodNames);
			}
		}
	}

	private void introspectPlainAccessors(Class<?> beanClass, Set<String> readMethodNames)
			throws IntrospectionException {

		for (Method method : beanClass.getMethods()) {
			if (!this.propertyDescriptors.containsKey(method.getName()) &&
					!readMethodNames.contains(method.getName()) && isPlainAccessor(method)) {
				this.propertyDescriptors.put(method.getName(),
						new GenericTypeAwarePropertyDescriptor(beanClass, method.getName(), method, null, null));
				readMethodNames.add(method.getName());
			}
		}
	}

	private boolean isPlainAccessor(Method method) {
		if (Modifier.isStatic(method.getModifiers()) ||
				method.getDeclaringClass() == Object.class || method.getDeclaringClass() == Class.class ||
				method.getParameterCount() > 0 || method.getReturnType() == void.class ||
				isInvalidReadOnlyPropertyType(method.getReturnType(), method.getDeclaringClass())) {
			return false;
		}
		try {
			// Accessor method referring to instance field of same name?
			method.getDeclaringClass().getDeclaredField(method.getName());
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	private boolean isInvalidReadOnlyPropertyType(@Nullable Class<?> returnType, Class<?> beanClass) {
		return (returnType != null && (ClassLoader.class.isAssignableFrom(returnType) ||
				ProtectionDomain.class.isAssignableFrom(returnType) ||
				(AutoCloseable.class.isAssignableFrom(returnType) &&
						!AutoCloseable.class.isAssignableFrom(beanClass))));
	}


	BeanInfo getBeanInfo() {
		return this.beanInfo;
	}

	Class<?> getBeanClass() {
		return this.beanInfo.getBeanDescriptor().getBeanClass();
	}

	@Nullable
	PropertyDescriptor getPropertyDescriptor(String name) {
		PropertyDescriptor pd = this.propertyDescriptors.get(name);
		if (pd == null && StringUtils.hasLength(name)) {
			// Same lenient fallback checking as in Property...
			pd = this.propertyDescriptors.get(StringUtils.uncapitalize(name));
			if (pd == null) {
				pd = this.propertyDescriptors.get(StringUtils.capitalize(name));
			}
		}
		return pd;
	}

	PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors.values().toArray(PropertyDescriptorUtils.EMPTY_PROPERTY_DESCRIPTOR_ARRAY);
	}

	private PropertyDescriptor buildGenericTypeAwarePropertyDescriptor(Class<?> beanClass, PropertyDescriptor pd) {
		try {
			return new GenericTypeAwarePropertyDescriptor(beanClass, pd.getName(), pd.getReadMethod(),
					pd.getWriteMethod(), pd.getPropertyEditorClass());
		}
		catch (IntrospectionException ex) {
			throw new FatalBeanException("Failed to re-introspect class [" + beanClass.getName() + "]", ex);
		}
	}

	TypeDescriptor addTypeDescriptor(PropertyDescriptor pd, TypeDescriptor td) {
		TypeDescriptor existing = this.typeDescriptorCache.putIfAbsent(pd, td);
		return (existing != null ? existing : td);
	}

	@Nullable
	TypeDescriptor getTypeDescriptor(PropertyDescriptor pd) {
		return this.typeDescriptorCache.get(pd);
	}

}
