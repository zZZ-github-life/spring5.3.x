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

package org.springframework.beans.factory.support;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 根 Bean 定义表示在运行时为 Spring BeanFactory 中特定 Bean 提供支持的合并后 Bean 定义。它可能由多
 * 个相互继承的原始 Bean 定义创建而来，这些定义通常注册为
 * {@link GenericBeanDefinition GenericBeanDefinitions}。根 Bean 定义本质上就是运行时的“统一”Bean
 * 定义视图。
 *
 * <p>根 Bean 定义也可以用于在配置阶段注册单独的 Bean 定义。不过，自 Spring 2.5 起，以编程方式注册
 * Bean 定义的首选方式是 {@link GenericBeanDefinition} 类。GenericBeanDefinition 的优势在于它允许动
 * 态定义父依赖，而不必将角色“硬编码”为根 Bean 定义。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see GenericBeanDefinition
 * @see ChildBeanDefinition
 */
@SuppressWarnings("serial")
public class RootBeanDefinition extends AbstractBeanDefinition {

	@Nullable
	private BeanDefinitionHolder decoratedDefinition;

	@Nullable
	private AnnotatedElement qualifiedElement;

	/** 确定该定义是否需要重新合并。 */
	volatile boolean stale;

	boolean allowCaching = true;

	boolean isFactoryMethodUnique;

	@Nullable
	volatile ResolvableType targetType;

	/** 包级可见字段，用于缓存给定 bean 定义所确定的 Class。 */
	@Nullable
	volatile Class<?> resolvedTargetType;

	/** 包级可见字段，用于缓存该 bean 是否为工厂 bean。 */
	@Nullable
	volatile Boolean isFactoryBean;

	/** 包级可见字段，用于缓存泛型工厂方法的返回类型。 */
	@Nullable
	volatile ResolvableType factoryMethodReturnType;

	/** 包级可见字段，用于缓存供内省使用的唯一工厂方法候选。 */
	@Nullable
	volatile Method factoryMethodToIntrospect;

	/** 包级可见字段，用于缓存解析后的销毁方法名（也用于推断出的方法名）。 */
	@Nullable
	volatile String resolvedDestroyMethodName;

	/** 下面四个构造器字段的公共锁。 */
	final Object constructorArgumentLock = new Object();

	/** 包级可见字段，用于缓存解析后的构造方法或工厂方法。 */
	@Nullable
	Executable resolvedConstructorOrFactoryMethod;

	/** 包级可见字段，用于标记构造方法参数已解析。 */
	boolean constructorArgumentsResolved = false;

	/** 包级可见字段，用于缓存完全解析后的构造方法参数。 */
	@Nullable
	Object[] resolvedConstructorArguments;

	/** 包级可见字段，用于缓存部分准备好的构造方法参数。 */
	@Nullable
	Object[] preparedConstructorArguments;

	/** 下面两个后处理字段的公共锁。 */
	final Object postProcessingLock = new Object();

	/** 包级可见字段，指示已应用 MergedBeanDefinitionPostProcessor。 */
	boolean postProcessed = false;

	/** 包级可见字段，指示实例化之前的后处理器已介入。 */
	@Nullable
	volatile Boolean beforeInstantiationResolved;

	@Nullable
	private Set<Member> externallyManagedConfigMembers;

	@Nullable
	private Set<String> externallyManagedInitMethods;

	@Nullable
	private Set<String> externallyManagedDestroyMethods;


	/**
	 * 创建一个新的 RootBeanDefinition，通过其 Bean 属性和配置方法进行配置。
	 * @see #setBeanClass
	 * @see #setScope
	 * @see #setConstructorArgumentValues
	 * @see #setPropertyValues
	 */
	public RootBeanDefinition() {
		super();
	}

	/**
	 * 为单例创建一个新的 RootBeanDefinition。
	 * @param beanClass the class of the bean to instantiate
	 * @see #setBeanClass
	 */
	public RootBeanDefinition(@Nullable Class<?> beanClass) {
		super();
		setBeanClass(beanClass);
	}

	/**
	 * 为单例 Bean 创建一个新的 RootBeanDefinition，通过调用给定的 supplier 构造每个实例（可以是 lambda
	 * 或方法引用）。
	 * @param beanClass the class of the bean to instantiate
	 * @param instanceSupplier the supplier to construct a bean instance,
	 * as an alternative to a declaratively specified factory method
	 * @since 5.0
	 * @see #setInstanceSupplier
	 */
	public <T> RootBeanDefinition(@Nullable Class<T> beanClass, @Nullable Supplier<T> instanceSupplier) {
		super();
		setBeanClass(beanClass);
		setInstanceSupplier(instanceSupplier);
	}

	/**
	 * 为作用域 Bean 创建一个新的 RootBeanDefinition，通过调用给定的 supplier 构造每个实例（可以是
	 * lambda 或方法引用）。
	 * @param beanClass the class of the bean to instantiate
	 * @param scope the name of the corresponding scope
	 * @param instanceSupplier the supplier to construct a bean instance,
	 * as an alternative to a declaratively specified factory method
	 * @since 5.0
	 * @see #setInstanceSupplier
	 */
	public <T> RootBeanDefinition(@Nullable Class<T> beanClass, String scope, @Nullable Supplier<T> instanceSupplier) {
		super();
		setBeanClass(beanClass);
		setScope(scope);
		setInstanceSupplier(instanceSupplier);
	}

	/**
	 * 使用给定的自动装配模式为单例创建一个新的 RootBeanDefinition。
	 * @param beanClass the class of the bean to instantiate
	 * @param autowireMode by name or type, using the constants in this interface
	 * @param dependencyCheck whether to perform a dependency check for objects
	 * (not applicable to autowiring a constructor, thus ignored there)
	 */
	public RootBeanDefinition(@Nullable Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
		super();
		setBeanClass(beanClass);
		setAutowireMode(autowireMode);
		if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
			setDependencyCheck(DEPENDENCY_CHECK_OBJECTS);
		}
	}

	/**
	 * 为单例创建一个新的 RootBeanDefinition，并提供构造器参数和属性值。
	 * @param beanClass the class of the bean to instantiate
	 * @param cargs the constructor argument values to apply
	 * @param pvs the property values to apply
	 */
	public RootBeanDefinition(@Nullable Class<?> beanClass, @Nullable ConstructorArgumentValues cargs,
			@Nullable MutablePropertyValues pvs) {

		super(cargs, pvs);
		setBeanClass(beanClass);
	}

	/**
	 * 为单例创建一个新的 RootBeanDefinition，并提供构造器参数和属性值。<p>接收 Bean 类名，以避免急切加
	 * 载 Bean 类。
	 * @param beanClassName the name of the class to instantiate
	 */
	public RootBeanDefinition(String beanClassName) {
		setBeanClassName(beanClassName);
	}

	/**
	 * 为单例创建一个新的 RootBeanDefinition，并提供构造器参数和属性值。<p>接收 Bean 类名，以避免急切加
	 * 载 Bean 类。
	 * @param beanClassName the name of the class to instantiate
	 * @param cargs the constructor argument values to apply
	 * @param pvs the property values to apply
	 */
	public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(cargs, pvs);
		setBeanClassName(beanClassName);
	}

	/**
	 * 创建给定 Bean 定义的深拷贝，作为新的 RootBeanDefinition。
	 * @param original the original bean definition to copy from
	 */
	public RootBeanDefinition(RootBeanDefinition original) {
		super(original);
		this.decoratedDefinition = original.decoratedDefinition;
		this.qualifiedElement = original.qualifiedElement;
		this.allowCaching = original.allowCaching;
		this.isFactoryMethodUnique = original.isFactoryMethodUnique;
		this.targetType = original.targetType;
		this.factoryMethodToIntrospect = original.factoryMethodToIntrospect;
	}

	/**
	 * 创建给定 Bean 定义的深拷贝，作为新的 RootBeanDefinition。
	 * @param original the original bean definition to copy from
	 */
	RootBeanDefinition(BeanDefinition original) {
		super(original);
	}


	@Override
	public String getParentName() {
		return null;
	}

	@Override
	public void setParentName(@Nullable String parentName) {
		if (parentName != null) {
			throw new IllegalArgumentException("Root bean cannot be changed into a child bean with parent reference");
		}
	}

	/**
	 * 注册一个由该 Bean 定义装饰的目标定义。
	 */
	public void setDecoratedDefinition(@Nullable BeanDefinitionHolder decoratedDefinition) {
		this.decoratedDefinition = decoratedDefinition;
	}

	/**
	 * 返回由该 Bean 定义装饰的目标定义（如果有）。
	 */
	@Nullable
	public BeanDefinitionHolder getDecoratedDefinition() {
		return this.decoratedDefinition;
	}

	/**
	 * 指定定义限定符的 {@link AnnotatedElement}，用以替代目标类或工厂方法。
	 * @since 4.3.3
	 * @see #setTargetType(ResolvableType)
	 * @see #getResolvedFactoryMethod()
	 */
	public void setQualifiedElement(@Nullable AnnotatedElement qualifiedElement) {
		this.qualifiedElement = qualifiedElement;
	}

	/**
	 * 返回定义限定符的 {@link AnnotatedElement}（如果有）。否则将检查工厂方法和目标类。
	 * @since 4.3.3
	 */
	@Nullable
	public AnnotatedElement getQualifiedElement() {
		return this.qualifiedElement;
	}

	/**
	 * 如果事先已知，指定该 Bean 定义的目标类型（可包含泛型）。
	 * @since 4.3.3
	 */
	public void setTargetType(ResolvableType targetType) {
		this.targetType = targetType;
	}

	/**
	 * 如果事先已知，指定该 Bean 定义的目标类型。
	 * @since 3.2.2
	 */
	public void setTargetType(@Nullable Class<?> targetType) {
		this.targetType = (targetType != null ? ResolvableType.forClass(targetType) : null);
	}

	/**
	 * 返回该 Bean 定义的目标类型（如果已知，即事先指定或在首次实例化时解析）。
	 * @since 3.2.2
	 */
	@Nullable
	public Class<?> getTargetType() {
		if (this.resolvedTargetType != null) {
			return this.resolvedTargetType;
		}
		ResolvableType targetType = this.targetType;
		return (targetType != null ? targetType.resolve() : null);
	}

	/**
	 * 返回该 Bean 定义的 {@link ResolvableType}，其来源可以是运行时缓存的类型信息，也可以是配置时通过
	 * {@link #setTargetType(ResolvableType)} 或 {@link #setBeanClass(Class)} 指定的信息，同时还会考虑
	 * 已解析的工厂方法定义。
	 * @since 5.1
	 * @see #setTargetType(ResolvableType)
	 * @see #setBeanClass(Class)
	 * @see #setResolvedFactoryMethod(Method)
	 */
	@Override
	public ResolvableType getResolvableType() {
		ResolvableType targetType = this.targetType;
		if (targetType != null) {
			return targetType;
		}
		ResolvableType returnType = this.factoryMethodReturnType;
		if (returnType != null) {
			return returnType;
		}
		Method factoryMethod = this.factoryMethodToIntrospect;
		if (factoryMethod != null) {
			return ResolvableType.forMethodReturnType(factoryMethod);
		}
		return super.getResolvableType();
	}

	/**
	 * 确定用于默认构造的首选构造器（如果有）。如有必要，构造器参数将被自动装配。
	 * @return one or more preferred constructors, or {@code null} if none
	 * (in which case the regular no-arg default constructor will be called)
	 * @since 5.1
	 */
	@Nullable
	public Constructor<?>[] getPreferredConstructors() {
		return null;
	}

	/**
	 * 指定一个指向非重载方法的工厂方法名。
	 */
	public void setUniqueFactoryMethodName(String name) {
		Assert.hasText(name, "Factory method name must not be empty");
		setFactoryMethodName(name);
		this.isFactoryMethodUnique = true;
	}

	/**
	 * 指定一个指向重载方法的工厂方法名。
	 * @since 5.2
	 */
	public void setNonUniqueFactoryMethodName(String name) {
		Assert.hasText(name, "Factory method name must not be empty");
		setFactoryMethodName(name);
		this.isFactoryMethodUnique = false;
	}

	/**
	 * 检查给定的候选方法是否符合工厂方法的要求。
	 */
	public boolean isFactoryMethod(Method candidate) {
		return candidate.getName().equals(getFactoryMethodName());
	}

	/**
	 * 为该 Bean 定义上的工厂方法设置已解析的 Java Method。
	 * @param method the resolved factory method, or {@code null} to reset it
	 * @since 5.2
	 */
	public void setResolvedFactoryMethod(@Nullable Method method) {
		this.factoryMethodToIntrospect = method;
	}

	/**
	 * 以 Java Method 对象形式返回已解析的工厂方法（如果可用）。
	 * @return the factory method, or {@code null} if not found or not resolved yet
	 */
	@Nullable
	public Method getResolvedFactoryMethod() {
		return this.factoryMethodToIntrospect;
	}

	/**
	 * 注册一个外部管理的配置方法或字段。
	 */
	public void registerExternallyManagedConfigMember(Member configMember) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedConfigMembers == null) {
				this.externallyManagedConfigMembers = new LinkedHashSet<>(1);
			}
			this.externallyManagedConfigMembers.add(configMember);
		}
	}

	/**
	 * 判断给定的方法或字段是否为外部管理的配置成员。
	 */
	public boolean isExternallyManagedConfigMember(Member configMember) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedConfigMembers != null &&
					this.externallyManagedConfigMembers.contains(configMember));
		}
	}

	/**
	 * 获取所有外部管理的配置方法和字段（作为不可变的 Set）。
	 * @since 5.3.11
	 */
	public Set<Member> getExternallyManagedConfigMembers() {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedConfigMembers != null ?
					Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedConfigMembers)) :
					Collections.emptySet());
		}
	}

	/**
	 * 注册一个外部管理的配置初始化方法 &mdash; 例如，使用 JSR-250 的
	 * {@link javax.annotation.PostConstruct} 注解标注的方法。<p>提供的 {@code initMethod} 可以是用于非
	 * 私有方法的{@linkplain Method#getName() 简单方法名}，也可以是用于 {@code private} 方法的
	 * {@linkplain org.springframework.util.ClassUtils#getQualifiedMethodName(Method) 限定方法名}。对于
	 * {@code private} 方法，必须使用限定名，以便在类层次结构中区分多个同名私有方法。
	 */
	public void registerExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedInitMethods == null) {
				this.externallyManagedInitMethods = new LinkedHashSet<>(1);
			}
			this.externallyManagedInitMethods.add(initMethod);
		}
	}

	/**
	 * 判断给定的方法名是否表示外部管理的初始化方法。<p>有关提供的 {@code initMethod} 格式的详细信息，
	 * 请参见 {@link #registerExternallyManagedInitMethod}。
	 */
	public boolean isExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedInitMethods != null &&
					this.externallyManagedInitMethods.contains(initMethod));
		}
	}

	/**
	 * 判断给定的方法名是否表示外部管理的初始化方法，而不考虑方法可见性。<p>与
	 * {@link #isExternallyManagedInitMethod(String)} 不同，如果存在使用限定方法名（而非简单方法名）
	 * {@linkplain #registerExternallyManagedInitMethod(String) 注册}的 {@code private} 外部管理初始化
	 * 方法，此方法也会返回 {@code true}。
	 * @since 5.3.17
	 */
	boolean hasAnyExternallyManagedInitMethod(String initMethod) {
		synchronized (this.postProcessingLock) {
			if (isExternallyManagedInitMethod(initMethod)) {
				return true;
			}
			if (this.externallyManagedInitMethods != null) {
				for (String candidate : this.externallyManagedInitMethods) {
					int indexOfDot = candidate.lastIndexOf('.');
					if (indexOfDot >= 0) {
						String methodName = candidate.substring(indexOfDot + 1);
						if (methodName.equals(initMethod)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * 返回所有外部管理的初始化方法（作为不可变的 Set）。<p>有关返回集合中初始化方法格式的详细信息，请
	 * 参见 {@link #registerExternallyManagedInitMethod}。
	 * @since 5.3.11
	 */
	public Set<String> getExternallyManagedInitMethods() {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedInitMethods != null ?
					Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedInitMethods)) :
					Collections.emptySet());
		}
	}

	/**
	 * 注册一个外部管理的配置销毁方法 &mdash; 例如，使用 JSR-250 的 {@link javax.annotation.PreDestroy}
	 * 注解标注的方法。<p>提供的 {@code destroyMethod} 可以是用于非私有方法的
	 * {@linkplain Method#getName() 简单方法名}，也可以是用于 {@code private} 方法的
	 * {@linkplain org.springframework.util.ClassUtils#getQualifiedMethodName(Method) 限定方法名}。对于
	 * {@code private} 方法，必须使用限定名，以便在类层次结构中区分多个同名私有方法。
	 */
	public void registerExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			if (this.externallyManagedDestroyMethods == null) {
				this.externallyManagedDestroyMethods = new LinkedHashSet<>(1);
			}
			this.externallyManagedDestroyMethods.add(destroyMethod);
		}
	}

	/**
	 * 判断给定的方法名是否表示外部管理的销毁方法。<p>有关提供的 {@code destroyMethod} 格式的详细信息，
	 * 请参见 {@link #registerExternallyManagedDestroyMethod}。
	 */
	public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedDestroyMethods != null &&
					this.externallyManagedDestroyMethods.contains(destroyMethod));
		}
	}

	/**
	 * 判断给定的方法名是否表示外部管理的销毁方法，而不考虑方法可见性。<p>与
	 * {@link #isExternallyManagedDestroyMethod(String)} 不同，如果存在使用限定方法名（而非简单方法名）
	 * {@linkplain #registerExternallyManagedDestroyMethod(String) 注册}的 {@code private} 外部管理销毁
	 * 方法，此方法也会返回 {@code true}。
	 * @since 5.3.17
	 */
	boolean hasAnyExternallyManagedDestroyMethod(String destroyMethod) {
		synchronized (this.postProcessingLock) {
			if (isExternallyManagedDestroyMethod(destroyMethod)) {
				return true;
			}
			if (this.externallyManagedDestroyMethods != null) {
				for (String candidate : this.externallyManagedDestroyMethods) {
					int indexOfDot = candidate.lastIndexOf('.');
					if (indexOfDot >= 0) {
						String methodName = candidate.substring(indexOfDot + 1);
						if (methodName.equals(destroyMethod)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * 获取所有外部管理的销毁方法（作为不可变的 Set）。<p>有关返回集合中销毁方法格式的详细信息，请参见
	 * {@link #registerExternallyManagedDestroyMethod}。
	 * @since 5.3.11
	 */
	public Set<String> getExternallyManagedDestroyMethods() {
		synchronized (this.postProcessingLock) {
			return (this.externallyManagedDestroyMethods != null ?
					Collections.unmodifiableSet(new LinkedHashSet<>(this.externallyManagedDestroyMethods)) :
					Collections.emptySet());
		}
	}


	@Override
	public RootBeanDefinition cloneBeanDefinition() {
		return new RootBeanDefinition(this);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof RootBeanDefinition && super.equals(other)));
	}

	@Override
	public String toString() {
		return "Root bean: " + super.toString();
	}

}
