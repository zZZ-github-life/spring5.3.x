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

package org.springframework.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 实现，用于
 * 自动装配带注解的字段、setter 方法和任意配置方法。要注入的这些成员通过注解来检测：默认情况下是
 * Spring 的 {@link Autowired @Autowired} 和 {@link Value @Value} 注解。
 *
 * <p>如果可用，还支持 JSR-330 的 {@link javax.inject.Inject @Inject} 注解，作为 Spring 自身
 * {@code @Autowired} 的直接替代方案。
 *
 * <h3>自动装配的构造器</h3>
 * <p>对于任意给定的 Bean 类，只能有一个构造器将该注解的 'required' 属性设为 {@code true}，表示作为
 * Spring Bean 使用时<i>该</i>构造器进行自动装配。此外，如果 'required' 属性设为 {@code true}，则只
 * 能有一个构造器被标注 {@code @Autowired}。如果有多个<i>非必需</i>构造器声明了该注解，它们将被视为
 * 自动装配的候选。将选择依赖数量最多且能由 Spring 容器中匹配的 Bean 满足的构造器。如果没有任何候选
 * 能够被满足，则使用主/默认构造器（如果存在）。如果一个类从一开始就只声明了一个构造器，则始终使用
 * 它，即使未标注注解。带注解的构造器不必是 public。
 *
 * <h3>自动装配的字段</h3>
 * <p>字段在 Bean 构造完成后、任何配置方法调用之前立即注入。此类配置字段不必是 public。
 *
 * <h3>自动装配的方法</h3>
 * <p>配置方法可以具有任意名称和任意数量的参数；其中每个参数都将与 Spring 容器中匹配的 Bean 进行自
 * 动装配。Bean 属性 setter 方法实际上只是此类通用配置方法的一个特例。配置方法不必是 public。
 *
 * <h3>注解配置与 XML 配置</h3>
 * <p>默认的 {@code AutowiredAnnotationBeanPostProcessor} 将由 "context:annotation-config" 和
 * "context:component-scan" XML 标签注册。如果打算指定自定义的
 * {@code AutowiredAnnotationBeanPostProcessor} Bean 定义，请移除或关闭那里的默认注解配置。
 *
 * <p><b>NOTE:</b> 注解注入将在 XML 注入<i>之前</i>执行；因此对于通过两种方式装配的属性，后者的配置
 * 将覆盖前者。
 *
 * <h3>{@literal @}Lookup 方法</h3>
 * <p>除了上面讨论的常规注入点之外，该后处理器还处理 Spring 的 {@link Lookup @Lookup} 注解，该注解
 * 标识要在运行时由容器替换的查找方法。这实质上是 {@code getBean(Class, args)} 和
 * {@code getBean(String, args)} 的类型安全版本。详情参见 {@link Lookup @Lookup's javadoc}。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 2.5
 * @see #setAutowiredAnnotationType
 * @see Autowired
 * @see Value
 */
public class AutowiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

	private String requiredParameterName = "required";

	private boolean requiredParameterValue = true;

	private int order = Ordered.LOWEST_PRECEDENCE - 2;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	private final Set<String> lookupMethodsChecked = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

	private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);


	/**
	 * 为 Spring 标准的 {@link Autowired @Autowired} 和 {@link Value @Value} 注解创建一个新的
	 * {@code AutowiredAnnotationBeanPostProcessor}。
	 * <p>如果可用，还支持 JSR-330 的 {@link javax.inject.Inject @Inject} 注解。
	 */
	@SuppressWarnings("unchecked")
	public AutowiredAnnotationBeanPostProcessor() {
		this.autowiredAnnotationTypes.add(Autowired.class);
		this.autowiredAnnotationTypes.add(Value.class);
		try {
			this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
			logger.trace("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - simply skip.
		}
	}


	/**
	 * 设置 'autowired' 注解类型，用于构造器、字段、setter 方法和任意配置方法。
	 * <p>默认的自动装配注解类型是 Spring 提供的 {@link Autowired @Autowired} 和 {@link Value @Value}
	 * 注解，以及 JSR-330 的 {@link javax.inject.Inject @Inject} 注解（如果可用）。
	 * <p>提供此 setter 属性是为了让开发者可以提供自己的（非 Spring 专用的）注解类型，以表明某个成员应
	 * 被自动装配。
	 */
	public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
		Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.add(autowiredAnnotationType);
	}

	/**
	 * 设置 'autowired' 注解类型，用于构造器、字段、setter 方法和任意配置方法。
	 * <p>默认的自动装配注解类型是 Spring 提供的 {@link Autowired @Autowired} 和 {@link Value @Value}
	 * 注解，以及 JSR-330 的 {@link javax.inject.Inject @Inject} 注解（如果可用）。
	 * <p>提供此 setter 属性是为了让开发者可以提供自己的（非 Spring 专用）注解类型，以表明某个成员应被
	 * 自动装配。
	 */
	public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
		Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
	}

	/**
	 * 设置注解中用于指定该依赖是否为必需的属性名称。
	 * @see #setRequiredParameterValue(boolean)
	 */
	public void setRequiredParameterName(String requiredParameterName) {
		this.requiredParameterName = requiredParameterName;
	}

	/**
	 * 设置将依赖标记为必需的布尔值。
	 * <p>例如，如果使用 'required=true'（默认值），该值应为 {@code true}；但如果使用 'optional=false'，
	 * 该值应为 {@code false}。
	 * @see #setRequiredParameterName(String)
	 */
	public void setRequiredParameterValue(boolean requiredParameterValue) {
		this.requiredParameterValue = requiredParameterValue;
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
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
		metadata.checkConfigMembers(beanDefinition);
	}

	@Override
	public void resetBeanDefinition(String beanName) {
		this.lookupMethodsChecked.remove(beanName);
		this.injectionMetadataCache.remove(beanName);
	}

	/*
	* 推断候选的构造方法：
	* 	1.检查所有的构造方法是否存在@Autowrited注解。
	* 	2.如果有@Autowrited注解的构造且required=true的，那么表示这是用户指定的构造方法，如果还有其他的方法加了@Autowrited注解的，直接抛出异常
	* 	3.如果没有@Autowrited注解且required=true的，那么检查是否存在@Autowrited注解required=false的，存在的话，如果还有默认的构造，有这两类则加入到候选组
	* 	4.符合上述条件的都会加入到候选组，没有则返回null
	* */
	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
			throws BeanCreationException {

		/*
		* 检查@Lookup注解。
		* 	单例bean依赖原型bean的是需要将原型bean转化为单例
		* */
		// Let's check for lookup methods here...
		if (!this.lookupMethodsChecked.contains(beanName)) {
			if (AnnotationUtils.isCandidateClass(beanClass, Lookup.class)) {
				try {
					Class<?> targetClass = beanClass;
					do {
						ReflectionUtils.doWithLocalMethods(targetClass, method -> {
							Lookup lookup = method.getAnnotation(Lookup.class);
							if (lookup != null) {
								Assert.state(this.beanFactory != null, "No BeanFactory available");
								LookupOverride override = new LookupOverride(method, lookup.value());
								try {
									RootBeanDefinition mbd = (RootBeanDefinition)
											this.beanFactory.getMergedBeanDefinition(beanName);
									mbd.getMethodOverrides().addOverride(override);
								}
								catch (NoSuchBeanDefinitionException ex) {
									throw new BeanCreationException(beanName,
											"Cannot apply @Lookup to beans without corresponding bean definition");
								}
							}
						});
						targetClass = targetClass.getSuperclass();
					}
					while (targetClass != null && targetClass != Object.class);

				}
				catch (IllegalStateException ex) {
					throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
				}
			}
			this.lookupMethodsChecked.add(beanName);
		}

		/*
		* 先检查候选缓存组，如果有，则表示之前已经推断过了，此时无需再做多余的动作，直接返回。
		* */
		// Quick check on the concurrent map first, with minimal locking.
		Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
		//没有表示没有推断过，或者之前就没推断出来
		if (candidateConstructors == null) {
			// Fully synchronized resolution now...
			synchronized (this.candidateConstructorsCache) {
				//双重检查锁，防止重复多线程重复推断
				candidateConstructors = this.candidateConstructorsCache.get(beanClass);
				if (candidateConstructors == null) {
					//存放所有的构造方法
					Constructor<?>[] rawCandidates;
					try {
						rawCandidates = beanClass.getDeclaredConstructors();
					}
					catch (Throwable ex) {
						throw new BeanCreationException(beanName,
								"Resolution of declared constructors on bean Class [" + beanClass.getName() +
								"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
					}
					//存放候选的构造方法
					List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
					//存放用户指定的构造方法：即@Autowrited注解为required=true的
					Constructor<?> requiredConstructor = null;
					//存放无参的默认构造
					Constructor<?> defaultConstructor = null;
					//获取Kotlin类，我们用不到，忽略
					Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
					//表示所有的构造方法中，不是合成的构造方法有多少个（内部类时，可能会出现合成的构造，正常情况没有）
					int nonSyntheticConstructors = 0;
					//遍历所有的构造开始推断
					for (Constructor<?> candidate : rawCandidates) {
						if (!candidate.isSynthetic()) {
							//记录正常的构造方法的数量
							nonSyntheticConstructors++;
						}
						//为空不管
						else if (primaryConstructor != null) {
							continue;
						}
						/*
						* 查找构造方法中是否添加了@Autowrited注解
						* */
						MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
						if (ann == null) {
							//如果没有加，判断是不是cglib代理的类，如果是，那么找到开发者定义的类，根据参数去匹配真实类的构造并检查是否存在@Autowrited注解
							Class<?> userClass = ClassUtils.getUserClass(beanClass);
							//表示当前是代理类，需要查找真实的类构造的注解情况
							if (userClass != beanClass) {
								try {
									Constructor<?> superCtor =
											userClass.getDeclaredConstructor(candidate.getParameterTypes());
									ann = findAutowiredAnnotation(superCtor);
								}
								catch (NoSuchMethodException ex) {
									// Simply proceed, no equivalent superclass constructor found...
								}
							}
						}
						//如果找到了@Autowrited注解
						if (ann != null) {

							/*
							* requiredConstructor != null 说明之前的遍历中已经找到了用户指定的构造函数（@Autowrited:required=true的）,
							* 	再次进入这个分支，说明开发者还配置了其他的构造函数@Autowrited注解，spring不允许这样配置，抛出异常
							* */
							if (requiredConstructor != null) {
								throw new BeanCreationException(beanName,
										"Invalid autowire-marked constructor: " + candidate +
										". Found constructor with 'required' Autowired annotation already: " +
										requiredConstructor);
							}
							boolean required = determineRequiredStatus(ann);
							if (required) {
								/*
								* required为true且!candidates.isEmpty() ，同上，抛出异常
								* */
								if (!candidates.isEmpty()) {
									throw new BeanCreationException(beanName,
											"Invalid autowire-marked constructors: " + candidates +
											". Found constructor with 'required' Autowired annotation: " +
											candidate);
								}
								requiredConstructor = candidate;
							}
							//符合候选的构造函数，加入集合
							candidates.add(candidate);
						}
						//记录默认的无参构造
						else if (candidate.getParameterCount() == 0) {
							defaultConstructor = candidate;
						}
					}

					/*
					* 找到一些候选的构造，且开发者没有指定那一个构造，那么也将无参构造加入候选组
					* */
					if (!candidates.isEmpty()) {
						// Add default constructor to list of optional constructors, as fallback.
						if (requiredConstructor == null) {
							if (defaultConstructor != null) {
								candidates.add(defaultConstructor);
							}
							else if (candidates.size() == 1 && logger.isInfoEnabled()) {
								logger.info("Inconsistent constructor declaration on bean with name '" + beanName +
										"': single autowire-marked constructor flagged as optional - " +
										"this constructor is effectively required since there is no " +
										"default constructor to fall back to: " + candidates.get(0));
							}
						}
						//封装为数组
						candidateConstructors = candidates.toArray(new Constructor<?>[0]);
					}
					//只有一个构造函数的，且不是无参构造
					else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
						candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
					}
					//Kotlin相关，不考虑
					else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
							defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
					}
					else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor};
					}
					//没有找到，返回空
					else {
						candidateConstructors = new Constructor<?>[0];
					}
					//放入缓存，后面无需再推断
					this.candidateConstructorsCache.put(beanClass, candidateConstructors);
				}
			}
		}
		return (candidateConstructors.length > 0 ? candidateConstructors : null);
	}

	/*
	* 找到所有的需要自动注入的字段，开始注入
	*
	* */
	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
		}
		return pvs;
	}

	@Deprecated
	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		return postProcessProperties(pvs, bean, beanName);
	}

	/**
	 * 用于对任意目标实例进行直接调用的“原生”处理方法，解析其所有使用某个已配置的 'autowired' 注解类型
	 * 标注的字段和方法。
	 * @param bean the target instance to process
	 * @throws BeanCreationException if autowiring failed
	 * @see #setAutowiredAnnotationTypes(Set)
	 */
	public void processInjection(Object bean) throws BeanCreationException {
		Class<?> clazz = bean.getClass();
		InjectionMetadata metadata = findAutowiringMetadata(clazz.getName(), clazz, null);
		try {
			metadata.inject(bean, null, null);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					"Injection of autowired dependencies failed for class [" + clazz + "]", ex);
		}
	}

	private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
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
					metadata = buildAutowiringMetadata(clazz);
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
		if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
			return InjectionMetadata.EMPTY;
		}

		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;

		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				MergedAnnotation<?> ann = findAutowiredAnnotation(field);
				if (ann != null) {
					if (Modifier.isStatic(field.getModifiers())) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation is not supported on static fields: " + field);
						}
						return;
					}
					boolean required = determineRequiredStatus(ann);
					currElements.add(new AutowiredFieldElement(field, required));
				}
			});

			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				MergedAnnotation<?> ann = findAutowiredAnnotation(bridgedMethod);
				if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					if (Modifier.isStatic(method.getModifiers())) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation is not supported on static methods: " + method);
						}
						return;
					}
					if (method.getParameterCount() == 0) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation should only be used on methods with parameters: " +
									method);
						}
					}
					boolean required = determineRequiredStatus(ann);
					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
					currElements.add(new AutowiredMethodElement(method, required, pd));
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return InjectionMetadata.forElements(elements, clazz);
	}

	@Nullable
	private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
		MergedAnnotations annotations = MergedAnnotations.from(ao);
		for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
			MergedAnnotation<?> annotation = annotations.get(type);
			if (annotation.isPresent()) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * 确定带注解的字段或方法是否必需其依赖。
	 * <p>“必需”依赖意味着在未找到 Bean 时自动装配应当失败。否则，在未找到 Bean 时，自动装配过程将直接
	 * 跳过该字段或方法。
	 * @param ann the Autowired annotation
	 * @return whether the annotation indicates that a dependency is required
	 */
	@SuppressWarnings("deprecation")
	protected boolean determineRequiredStatus(MergedAnnotation<?> ann) {
		return determineRequiredStatus(ann.<AnnotationAttributes> asMap(
				new Function<MergedAnnotation<?>, AnnotationAttributes>() {
					@Override
					public AnnotationAttributes apply(MergedAnnotation<?> mergedAnnotation) {
						return new AnnotationAttributes(mergedAnnotation.getType());
					}
				}));
	}

	/**
	 * 确定带注解的字段或方法是否必需其依赖。
	 * <p>“必需”依赖意味着在未找到 Bean 时自动装配应当失败。否则，在未找到 Bean 时，自动装配过程将直接
	 * 跳过该字段或方法。
	 * @param ann the Autowired annotation
	 * @return whether the annotation indicates that a dependency is required
	 * @deprecated since 5.2, in favor of {@link #determineRequiredStatus(MergedAnnotation)}
	 */
	@Deprecated
	protected boolean determineRequiredStatus(AnnotationAttributes ann) {
		return (!ann.containsKey(this.requiredParameterName) ||
				this.requiredParameterValue == ann.getBoolean(this.requiredParameterName));
	}

	/**
	 * 获取给定类型的所有 Bean 作为自动装配候选。
	 * @param type the type of the bean
	 * @return the target beans, or an empty Collection if no bean of this type is found
	 * @throws BeansException if bean retrieval failed
	 * @deprecated since 5.3.24 since it is unused in the meantime
	 */
	@Deprecated
	protected <T> Map<String, T> findAutowireCandidates(Class<T> type) throws BeansException {
		if (this.beanFactory == null) {
			throw new IllegalStateException("No BeanFactory configured - " +
					"override the getBeanOfType method or specify the 'beanFactory' property");
		}
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
	}

	/**
	 * 将指定的 Bean 注册为依赖于自动装配的 Bean。
	 */
	private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
		if (beanName != null) {
			for (String autowiredBeanName : autowiredBeanNames) {
				if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
					this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Autowiring by type from bean name '" + beanName +
							"' to bean named '" + autowiredBeanName + "'");
				}
			}
		}
	}

	/**
	 * 解析指定的缓存方法参数或字段值。
	 */
	@Nullable
	private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
		if (cachedArgument instanceof DependencyDescriptor) {
			DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
			Assert.state(this.beanFactory != null, "No BeanFactory available");
			return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
		}
		else {
			return cachedArgument;
		}
	}


	/**
	 * 表示带注解字段的注入信息的类。
	 */
	private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached;

		@Nullable
		private volatile Object cachedFieldValue;

		public AutowiredFieldElement(Field field, boolean required) {
			super(field, null);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
			Field field = (Field) this.member;
			Object value;
			if (this.cached) {
				try {
					value = resolvedCachedArgument(beanName, this.cachedFieldValue);
				}
				catch (NoSuchBeanDefinitionException ex) {
					// Unexpected removal of target bean for cached argument -> re-resolve
					value = resolveFieldValue(field, bean, beanName);
				}
			}
			else {
				value = resolveFieldValue(field, bean, beanName);
			}
			if (value != null) {
				ReflectionUtils.makeAccessible(field);
				field.set(bean, value);
			}
		}
		// 处理需要注入的字段
		@Nullable
		private Object resolveFieldValue(Field field, Object bean, @Nullable String beanName) {
			DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
			desc.setContainingClass(bean.getClass());
			Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
			Assert.state(beanFactory != null, "No BeanFactory available");
			TypeConverter typeConverter = beanFactory.getTypeConverter();
			Object value;
			try {
				value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
			}
			catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
			}
			synchronized (this) {
				if (!this.cached) {
					Object cachedFieldValue = null;
					if (value != null || this.required) {
						cachedFieldValue = desc;
						registerDependentBeans(beanName, autowiredBeanNames); //注册两个bean之间的依赖
						if (autowiredBeanNames.size() == 1) {
							String autowiredBeanName = autowiredBeanNames.iterator().next();
							if (beanFactory.containsBean(autowiredBeanName) &&
									beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
								cachedFieldValue = new ShortcutDependencyDescriptor(
										desc, autowiredBeanName, field.getType());
							}
						}
					}
					this.cachedFieldValue = cachedFieldValue;
					this.cached = true;
				}
			}
			return value;
		}
	}


	/**
	 * 表示带注解方法的注入信息的类。
	 */
	private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached;

		@Nullable
		private volatile Object[] cachedMethodArguments;

		public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
			super(method, pd);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
			if (checkPropertySkipping(pvs)) {
				return;
			}
			Method method = (Method) this.member;
			Object[] arguments;
			if (this.cached) {
				try {
					arguments = resolveCachedArguments(beanName);
				}
				catch (NoSuchBeanDefinitionException ex) {
					// Unexpected removal of target bean for cached argument -> re-resolve
					arguments = resolveMethodArguments(method, bean, beanName);
				}
			}
			else {
				arguments = resolveMethodArguments(method, bean, beanName);
			}
			if (arguments != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					method.invoke(bean, arguments);
				}
				catch (InvocationTargetException ex) {
					throw ex.getTargetException();
				}
			}
		}

		@Nullable
		private Object[] resolveCachedArguments(@Nullable String beanName) {
			Object[] cachedMethodArguments = this.cachedMethodArguments;
			if (cachedMethodArguments == null) {
				return null;
			}
			Object[] arguments = new Object[cachedMethodArguments.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = resolvedCachedArgument(beanName, cachedMethodArguments[i]);
			}
			return arguments;
		}

		@Nullable
		private Object[] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
			int argumentCount = method.getParameterCount();
			Object[] arguments = new Object[argumentCount];
			DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
			Set<String> autowiredBeans = new LinkedHashSet<>(argumentCount);
			Assert.state(beanFactory != null, "No BeanFactory available");
			TypeConverter typeConverter = beanFactory.getTypeConverter();
			for (int i = 0; i < arguments.length; i++) {
				MethodParameter methodParam = new MethodParameter(method, i);
				DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
				currDesc.setContainingClass(bean.getClass());
				descriptors[i] = currDesc;
				try {
					Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeans, typeConverter);
					if (arg == null && !this.required) {
						arguments = null;
						break;
					}
					arguments[i] = arg;
				}
				catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
				}
			}
			synchronized (this) {
				if (!this.cached) {
					if (arguments != null) {
						DependencyDescriptor[] cachedMethodArguments = Arrays.copyOf(descriptors, arguments.length);
						registerDependentBeans(beanName, autowiredBeans);
						if (autowiredBeans.size() == argumentCount) {
							Iterator<String> it = autowiredBeans.iterator();
							Class<?>[] paramTypes = method.getParameterTypes();
							for (int i = 0; i < paramTypes.length; i++) {
								String autowiredBeanName = it.next();
								if (beanFactory.containsBean(autowiredBeanName) &&
										beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
									cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
											descriptors[i], autowiredBeanName, paramTypes[i]);
								}
							}
						}
						this.cachedMethodArguments = cachedMethodArguments;
					}
					else {
						this.cachedMethodArguments = null;
					}
					this.cached = true;
				}
			}
			return arguments;
		}
	}


	/**
	 * 带有预解析目标 Bean 名称的 DependencyDescriptor 变体。
	 */
	@SuppressWarnings("serial")
	private static class ShortcutDependencyDescriptor extends DependencyDescriptor {

		private final String shortcut;

		private final Class<?> requiredType;

		public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut, Class<?> requiredType) {
			super(original);
			this.shortcut = shortcut;
			this.requiredType = requiredType;
		}

		@Override
		public Object resolveShortcut(BeanFactory beanFactory) {
			return beanFactory.getBean(this.shortcut, this.requiredType);
		}
	}

}
