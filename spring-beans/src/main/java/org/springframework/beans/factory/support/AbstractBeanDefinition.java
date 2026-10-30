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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 具体、完整的 {@link BeanDefinition} 类的基类，抽取了 {@link GenericBeanDefinition}、
 * {@link RootBeanDefinition} 和 {@link ChildBeanDefinition} 的公共属性。
 *
 * <p>自动装配常量与 {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory} 接
 * 口中定义的常量一致。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @see GenericBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanDefinition extends BeanMetadataAttributeAccessor
		implements BeanDefinition, Cloneable {

	/**
	 * 默认作用域名称的常量：{@code ""}，等同于单例状态，除非从父 Bean 定义覆盖（如果适用）。
	 */
	public static final String SCOPE_DEFAULT = "";

	/**
	 * 表示完全不做外部自动装配的常量。
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

	/**
	 * 表示按名称自动装配 Bean 属性的常量。
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * 表示按类型自动装配 Bean 属性的常量。
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

	/**
	 * 表示自动装配构造函数的常量。
	 * @see #setAutowireMode
	 */
	public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

	/**
	 * 表示通过内省 Bean 类来确定合适自动装配策略的常量。
	 * @see #setAutowireMode
	 * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
	 * use annotation-based autowiring for clearer demarcation of autowiring needs.
	 */
	@Deprecated
	public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;

	/**
	 * 表示完全不做依赖检查的常量。
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_NONE = 0;

	/**
	 * 表示对对象引用做依赖检查的常量。
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;

	/**
	 * 表示对“简单”属性做依赖检查的常量。
	 * @see #setDependencyCheck
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 */
	public static final int DEPENDENCY_CHECK_SIMPLE = 2;

	/**
	 * 表示对所有属性（对象引用以及“简单”属性）做依赖检查的常量。
	 * @see #setDependencyCheck
	 */
	public static final int DEPENDENCY_CHECK_ALL = 3;

	/**
	 * 表示容器应尝试为 Bean 推断 {@link #setDestroyMethodName destroy method name}，而不是显式指定方法
	 * 名称的常量。{@value} 值特意包含在方法名中原本非法的字符，从而确保不会与具有相同名称的合法方法发
	 * 生冲突。
	 * <p>目前，在销毁方法推断过程中检测到的方法名是 "close" 和 "shutdown"（如果特定 Bean 类上存在这些
	 * 方法）。
	 */
	public static final String INFER_METHOD = "(inferred)";


	@Nullable
	private volatile Object beanClass;

	@Nullable
	private String scope = SCOPE_DEFAULT;

	private boolean abstractFlag = false;

	@Nullable
	private Boolean lazyInit;

	private int autowireMode = AUTOWIRE_NO;

	private int dependencyCheck = DEPENDENCY_CHECK_NONE;

	@Nullable
	private String[] dependsOn;

	private boolean autowireCandidate = true;

	private boolean primary = false;

	private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

	@Nullable
	private Supplier<?> instanceSupplier;

	private boolean nonPublicAccessAllowed = true;

	private boolean lenientConstructorResolution = true;

	@Nullable
	private String factoryBeanName;

	@Nullable
	private String factoryMethodName;

	@Nullable
	private ConstructorArgumentValues constructorArgumentValues;

	@Nullable
	private MutablePropertyValues propertyValues;

	private MethodOverrides methodOverrides = new MethodOverrides();

	@Nullable
	private String initMethodName;

	@Nullable
	private String destroyMethodName;

	private boolean enforceInitMethod = true;

	private boolean enforceDestroyMethod = true;

	private boolean synthetic = false;

	private int role = BeanDefinition.ROLE_APPLICATION;

	@Nullable
	private String description;

	@Nullable
	private Resource resource;


	/**
	 * 使用默认设置创建一个新的 AbstractBeanDefinition。
	 */
	protected AbstractBeanDefinition() {
		this(null, null);
	}

	/**
	 * 使用给定的构造参数值和属性值创建一个新的 AbstractBeanDefinition。
	 */
	protected AbstractBeanDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable MutablePropertyValues pvs) {
		this.constructorArgumentValues = cargs;
		this.propertyValues = pvs;
	}

	/**
	 * 以给定 Bean 定义的深拷贝创建一个新的 AbstractBeanDefinition。
	 * @param original 要从中复制的原始 Bean 定义
	 */
	protected AbstractBeanDefinition(BeanDefinition original) {
		setParentName(original.getParentName());
		setBeanClassName(original.getBeanClassName());
		setScope(original.getScope());
		setAbstract(original.isAbstract());
		setFactoryBeanName(original.getFactoryBeanName());
		setFactoryMethodName(original.getFactoryMethodName());
		setRole(original.getRole());
		setSource(original.getSource());
		copyAttributesFrom(original);

		if (original instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
			if (originalAbd.hasBeanClass()) {
				setBeanClass(originalAbd.getBeanClass());
			}
			if (originalAbd.hasConstructorArgumentValues()) {
				setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			}
			if (originalAbd.hasPropertyValues()) {
				setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			}
			if (originalAbd.hasMethodOverrides()) {
				setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
			}
			Boolean lazyInit = originalAbd.getLazyInit();
			if (lazyInit != null) {
				setLazyInit(lazyInit);
			}
			setAutowireMode(originalAbd.getAutowireMode());
			setDependencyCheck(originalAbd.getDependencyCheck());
			setDependsOn(originalAbd.getDependsOn());
			setAutowireCandidate(originalAbd.isAutowireCandidate());
			setPrimary(originalAbd.isPrimary());
			copyQualifiersFrom(originalAbd);
			setInstanceSupplier(originalAbd.getInstanceSupplier());
			setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
			setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
			setInitMethodName(originalAbd.getInitMethodName());
			setEnforceInitMethod(originalAbd.isEnforceInitMethod());
			setDestroyMethodName(originalAbd.getDestroyMethodName());
			setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
			setSynthetic(originalAbd.isSynthetic());
			setResource(originalAbd.getResource());
		}
		else {
			setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
			setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
			setLazyInit(original.isLazyInit());
			setResourceDescription(original.getResourceDescription());
		}
	}


	/**
	 * 用给定的 Bean 定义（大概是子定义）覆盖此 Bean 定义（大概是父子继承关系中复制而来的父定义）中的设
	 * 置。
	 * <ul>
	 * <li>如果给定的 Bean 定义中指定了 beanClass，则覆盖 beanClass。
	 * <li>始终采用给定 Bean 定义中的 {@code abstract}、{@code scope}、{@code lazyInit}、{@code autowireMode}、{@code dependencyCheck} 和 {@code dependsOn}。
	 * <li>将给定 Bean 定义中的 {@code constructorArgumentValues}、{@code propertyValues}、{@code methodOverrides} 添加到现有的对应项中。
	 * <li>如果给定的 Bean 定义中指定了 {@code factoryBeanName}、{@code factoryMethodName}、{@code initMethodName} 和 {@code destroyMethodName}，则覆盖这些设置。
	 * </ul>
	 */
	public void overrideFrom(BeanDefinition other) {
		if (StringUtils.hasLength(other.getBeanClassName())) {
			setBeanClassName(other.getBeanClassName());
		}
		if (StringUtils.hasLength(other.getScope())) {
			setScope(other.getScope());
		}
		setAbstract(other.isAbstract());
		if (StringUtils.hasLength(other.getFactoryBeanName())) {
			setFactoryBeanName(other.getFactoryBeanName());
		}
		if (StringUtils.hasLength(other.getFactoryMethodName())) {
			setFactoryMethodName(other.getFactoryMethodName());
		}
		setRole(other.getRole());
		setSource(other.getSource());
		copyAttributesFrom(other);

		if (other instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition otherAbd = (AbstractBeanDefinition) other;
			if (otherAbd.hasBeanClass()) {
				setBeanClass(otherAbd.getBeanClass());
			}
			if (otherAbd.hasConstructorArgumentValues()) {
				getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			}
			if (otherAbd.hasPropertyValues()) {
				getPropertyValues().addPropertyValues(other.getPropertyValues());
			}
			if (otherAbd.hasMethodOverrides()) {
				getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
			}
			Boolean lazyInit = otherAbd.getLazyInit();
			if (lazyInit != null) {
				setLazyInit(lazyInit);
			}
			setAutowireMode(otherAbd.getAutowireMode());
			setDependencyCheck(otherAbd.getDependencyCheck());
			setDependsOn(otherAbd.getDependsOn());
			setAutowireCandidate(otherAbd.isAutowireCandidate());
			setPrimary(otherAbd.isPrimary());
			copyQualifiersFrom(otherAbd);
			setInstanceSupplier(otherAbd.getInstanceSupplier());
			setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
			setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
			if (otherAbd.getInitMethodName() != null) {
				setInitMethodName(otherAbd.getInitMethodName());
				setEnforceInitMethod(otherAbd.isEnforceInitMethod());
			}
			if (otherAbd.getDestroyMethodName() != null) {
				setDestroyMethodName(otherAbd.getDestroyMethodName());
				setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
			}
			setSynthetic(otherAbd.isSynthetic());
			setResource(otherAbd.getResource());
		}
		else {
			getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
			getPropertyValues().addPropertyValues(other.getPropertyValues());
			setLazyInit(other.isLazyInit());
			setResourceDescription(other.getResourceDescription());
		}
	}

	/**
	 * 将提供的默认值应用到此 Bean。
	 * @param defaults 要应用的默认设置
	 * @since 2.5
	 */
	public void applyDefaults(BeanDefinitionDefaults defaults) {
		Boolean lazyInit = defaults.getLazyInit();
		if (lazyInit != null) {
			setLazyInit(lazyInit);
		}
		setAutowireMode(defaults.getAutowireMode());
		setDependencyCheck(defaults.getDependencyCheck());
		setInitMethodName(defaults.getInitMethodName());
		setEnforceInitMethod(false);
		setDestroyMethodName(defaults.getDestroyMethodName());
		setEnforceDestroyMethod(false);
	}


	/**
	 * 指定此 Bean 定义的 Bean 类名。
	 */
	@Override
	public void setBeanClassName(@Nullable String beanClassName) {
		this.beanClass = beanClassName;
	}

	/**
	 * 返回此 Bean 定义当前的 Bean 类名。
	 */
	@Override
	@Nullable
	public String getBeanClassName() {
		Object beanClassObject = this.beanClass;
		if (beanClassObject instanceof Class) {
			return ((Class<?>) beanClassObject).getName();
		}
		else {
			return (String) beanClassObject;
		}
	}

	/**
	 * 为此 Bean 指定类。
	 * @see #setBeanClassName(String)
	 */
	public void setBeanClass(@Nullable Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * 返回 Bean 定义所指定的类（假定已经解析）。<p><b>注意：</b>这是 Bean 元数据定义中声明的初始类引用，
	 * 可能还会结合声明的工厂方法或 {@link org.springframework.beans.factory.FactoryBean}，这可能导致
	 * Bean 的运行时类型不同；在实例级工厂方法的情况下（此时改用 {@link #getFactoryBeanName()} 解析），
	 * 甚至可能根本不会设置。<b>不要将其用于对任意 Bean 定义的运行时类型内省。</b>要查明特定 Bean 实际
	 * 运行时类型的推荐方式是对指定的 Bean 名称调用
	 * {@link org.springframework.beans.factory.BeanFactory#getType}；该调用会考虑上述所有情况，并返回
	 * 对同一 Bean 名称调用 {@link org.springframework.beans.factory.BeanFactory#getBean} 时将要返回的
	 * 对象类型。
	 * @return 已解析的 Bean 类（永不为 {@code null}）
	 * @throws IllegalStateException 如果 Bean 定义未定义 Bean 类，
	 * 或指定的 Bean 类名尚未解析为实际的 Class
	 * @see #getBeanClassName()
	 * @see #hasBeanClass()
	 * @see #setBeanClass(Class)
	 * @see #resolveBeanClass(ClassLoader)
	 */
	public Class<?> getBeanClass() throws IllegalStateException {
		Object beanClassObject = this.beanClass;
		if (beanClassObject == null) {
			throw new IllegalStateException("No bean class specified on bean definition");
		}
		if (!(beanClassObject instanceof Class)) {
			throw new IllegalStateException(
					"Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
		}
		return (Class<?>) beanClassObject;
	}

	/**
	 * 返回此定义是否指定了 Bean 类。
	 * @see #getBeanClass()
	 * @see #setBeanClass(Class)
	 * @see #resolveBeanClass(ClassLoader)
	 */
	public boolean hasBeanClass() {
		return (this.beanClass instanceof Class);
	}

	/**
	 * 确定所包装 Bean 的类，必要时根据指定的类名解析它。当调用时 Bean 类已解析，也会根据类名重新加载指
	 * 定的 Class。
	 * @param classLoader 用于解析（可能的）类名的 ClassLoader
	 * @return 已解析的 Bean 类
	 * @throws ClassNotFoundException 如果无法解析该类名
	 */
	@Nullable
	public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		String className = getBeanClassName();
		if (className == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
		this.beanClass = resolvedClass;
		return resolvedClass;
	}

	/**
	 * 返回此 Bean 定义的可解析类型。<p>此实现委托给 {@link #getBeanClass()}。
	 * @since 5.2
	 */
	@Override
	public ResolvableType getResolvableType() {
		return (hasBeanClass() ? ResolvableType.forClass(getBeanClass()) : ResolvableType.NONE);
	}

	/**
	 * 设置 Bean 的目标作用域名称。<p>默认是单例状态，不过这仅在 Bean 定义在所包含的工厂中变为活动状态
	 * 后才会应用。Bean 定义最终可能会从父 Bean 定义继承其作用域。因此，默认作用域名称是空字符串（即
	 * {@code ""}），在设置解析后的作用域之前，都假定为单例状态。
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	/**
	 * 返回 Bean 的目标作用域名称。
	 */
	@Override
	@Nullable
	public String getScope() {
		return this.scope;
	}

	/**
	 * 返回此 Bean 是否为 <b>Singleton</b>，即所有调用都返回单个共享实例。
	 * @see #SCOPE_SINGLETON
	 */
	@Override
	public boolean isSingleton() {
		return SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope);
	}

	/**
	 * 返回此 Bean 是否为 <b>Prototype</b>，即每次调用都返回独立的实例。
	 * @see #SCOPE_PROTOTYPE
	 */
	@Override
	public boolean isPrototype() {
		return SCOPE_PROTOTYPE.equals(this.scope);
	}

	/**
	 * 设置此 Bean 是否为“抽象”，即其本身不应被实例化，而只是作为具体子 Bean 定义的父定义。<p>默认为
	 * "false"。指定 true 可告知 Bean 工厂在任何情况下都不要尝试实例化该 Bean。
	 */
	public void setAbstract(boolean abstractFlag) {
		this.abstractFlag = abstractFlag;
	}

	/**
	 * 返回此 Bean 是否为“抽象”，即其本身不应被实例化，而只是作为具体子 Bean 定义的父定义。
	 */
	@Override
	public boolean isAbstract() {
		return this.abstractFlag;
	}

	/**
	 * 设置此 Bean 是否应延迟初始化。<p>如果为 {@code false}，则执行单例预先初始化的 Bean 工厂会在启动
	 * 时实例化该 Bean。
	 */
	@Override
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回此 Bean 是否应延迟初始化，即不在启动时预先实例化。仅适用于单例 Bean。
	 * @return 是否应用延迟初始化语义（默认 {@code false}）
	 */
	@Override
	public boolean isLazyInit() {
		return (this.lazyInit != null && this.lazyInit.booleanValue());
	}

	/**
	 * 返回此 Bean 是否应延迟初始化，即不在启动时预先实例化。仅适用于单例 Bean。
	 * @return 显式设置时的延迟初始化标志；否则为 {@code null}
	 * @since 5.2
	 */
	@Nullable
	public Boolean getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 设置自动装配模式。这决定了是否会发生对 Bean 引用的自动检测和设置。默认是 AUTOWIRE_NO，这意味着不
	 * 会进行基于名称或类型的约定式自动装配（不过，仍然可能存在显式的注解驱动自动装配）。
	 * @param autowireMode 要设置的自动装配模式。必须是本类中定义的常量之一。
	 * @see #AUTOWIRE_NO
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_AUTODETECT
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * 返回 Bean 定义中指定的自动装配模式。
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 返回解析后的自动装配代码（将 AUTOWIRE_AUTODETECT 解析为 AUTOWIRE_CONSTRUCTOR 或 AUTOWIRE_BY_TYPE）。
	 *
	 * @see #AUTOWIRE_AUTODETECT
	 * @see #AUTOWIRE_CONSTRUCTOR
	 * @see #AUTOWIRE_BY_TYPE
	 */
	public int getResolvedAutowireMode() {
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			// Work out whether to apply setter autowiring or constructor autowiring.
			// If it has a no-arg constructor it's deemed to be setter autowiring,
			// otherwise we'll try constructor autowiring.
			Constructor<?>[] constructors = getBeanClass().getConstructors();
			for (Constructor<?> constructor : constructors) {
				if (constructor.getParameterCount() == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			return AUTOWIRE_CONSTRUCTOR;
		}
		else {
			return this.autowireMode;
		}
	}

	/**
	 * 设置依赖检查代码。
	 * @param dependencyCheck 要设置的代码。必须是本类中定义的四个常量之一。
	 * @see #DEPENDENCY_CHECK_NONE
	 * @see #DEPENDENCY_CHECK_OBJECTS
	 * @see #DEPENDENCY_CHECK_SIMPLE
	 * @see #DEPENDENCY_CHECK_ALL
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * 返回依赖检查代码。
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * 设置此 Bean 所依赖、需先初始化的 Bean 名称。Bean 工厂将保证这些 Bean 先被初始化。<p>请注意，依赖
	 * 通常通过 Bean 属性或构造参数来表达。此属性只应对于其他类型的依赖才需要，例如静态初始化（*ugh*）
	 * 或启动时的数据库准备。
	 */
	@Override
	public void setDependsOn(@Nullable String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	/**
	 * 返回此 Bean 所依赖的 Bean 名称。
	 */
	@Override
	@Nullable
	public String[] getDependsOn() {
		return this.dependsOn;
	}

	/**
	 * 设置此 Bean 是否可作为自动装配到其他 Bean 的候选者。<p>请注意，此标志旨在仅影响基于类型的自动装
	 * 配。它不影响按名称的显式引用，即使指定的 Bean 未被标记为自动装配候选者，这些引用仍会被解析。因此，
	 * 如果名称匹配，按名称自动装配仍然会注入 Bean。
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_BY_NAME
	 */
	@Override
	public void setAutowireCandidate(boolean autowireCandidate) {
		this.autowireCandidate = autowireCandidate;
	}

	/**
	 * 返回此 Bean 是否可作为自动装配到其他 Bean 的候选者。
	 */
	@Override
	public boolean isAutowireCandidate() {
		return this.autowireCandidate;
	}

	/**
	 * 设置此 Bean 是否为首选自动装配候选者。<p>如果在多个匹配的候选者中恰好有一个 Bean 的此值为
	 * {@code true}，它将起到决胜作用。
	 */
	@Override
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * 返回此 Bean 是否为首选自动装配候选者。
	 */
	@Override
	public boolean isPrimary() {
		return this.primary;
	}

	/**
	 * 注册用于自动装配候选者解析的限定符，以限定符的类型名作为键。
	 * @see AutowireCandidateQualifier#getTypeName()
	 */
	public void addQualifier(AutowireCandidateQualifier qualifier) {
		this.qualifiers.put(qualifier.getTypeName(), qualifier);
	}

	/**
	 * 返回此 Bean 是否具有指定的限定符。
	 */
	public boolean hasQualifier(String typeName) {
		return this.qualifiers.containsKey(typeName);
	}

	/**
	 * 返回映射到所提供类型名的限定符。
	 */
	@Nullable
	public AutowireCandidateQualifier getQualifier(String typeName) {
		return this.qualifiers.get(typeName);
	}

	/**
	 * 返回所有已注册的限定符。
	 * @return {@link AutowireCandidateQualifier} 对象的 Set。
	 */
	public Set<AutowireCandidateQualifier> getQualifiers() {
		return new LinkedHashSet<>(this.qualifiers.values());
	}

	/**
	 * 将所提供的 AbstractBeanDefinition 中的限定符复制到此 Bean 定义。
	 * @param source 要从中复制的 AbstractBeanDefinition
	 */
	public void copyQualifiersFrom(AbstractBeanDefinition source) {
		Assert.notNull(source, "Source must not be null");
		this.qualifiers.putAll(source.qualifiers);
	}

	/**
	 * 指定用于创建 Bean 实例的回调，作为声明式指定的工厂方法的替代方案。<p>如果设置了此类回调，它将覆
	 * 盖任何其他构造函数或工厂方法元数据。不过，Bean 属性填充和可能的注解驱动注入仍将照常应用。
	 * @since 5.0
	 * @see #setConstructorArgumentValues(ConstructorArgumentValues)
	 * @see #setPropertyValues(MutablePropertyValues)
	 */
	public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
		this.instanceSupplier = instanceSupplier;
	}

	/**
	 * 返回用于创建 Bean 实例的回调（如果有）。
	 * @since 5.0
	 */
	@Nullable
	public Supplier<?> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	/**
	 * 指定是否允许访问非 public 的构造函数和方法，用于外部化元数据指向这些成员的情况。默认值为
	 * {@code true}；如果只想访问 public 成员，请将其改为 {@code false}。<p>这适用于构造函数解析、工厂
	 * 方法解析以及 init/destroy 方法。Bean 属性访问器在任何情况下都必须是 public，不受此设置影响。<p>
	 * 请注意，注解驱动的配置仍会访问已标注注解的非 public 成员。此设置仅适用于此 Bean 定义中的外部化元
	 * 数据。
	 */
	public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
	}

	/**
	 * 返回是否允许访问非 public 的构造函数和方法。
	 */
	public boolean isNonPublicAccessAllowed() {
		return this.nonPublicAccessAllowed;
	}

	/**
	 * 指定是以宽松模式解析构造函数（{@code true}，这是默认值），还是切换到严格解析（当存在多个有歧义的
	 * 构造函数、且在参数转换后全部匹配时抛出异常，而宽松模式会选择类型匹配“最接近”的那个）。
	 */
	public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
		this.lenientConstructorResolution = lenientConstructorResolution;
	}

	/**
	 * 返回是以宽松模式还是严格模式解析构造函数。
	 */
	public boolean isLenientConstructorResolution() {
		return this.lenientConstructorResolution;
	}

	/**
	 * 指定要使用的工厂 Bean（如果有）。这是要调用指定工厂方法的 Bean 的名称。
	 * @see #setFactoryMethodName
	 */
	@Override
	public void setFactoryBeanName(@Nullable String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}

	/**
	 * 返回工厂 Bean 名称（如果有）。
	 */
	@Override
	@Nullable
	public String getFactoryBeanName() {
		return this.factoryBeanName;
	}

	/**
	 * 指定工厂方法（如果有）。调用此方法时会传入构造参数；如果未指定任何参数，则以无参方式调用。如果有
	 * 指定的工厂 Bean，则在该工厂 Bean 上调用此方法；否则作为本地 Bean 类上的静态方法调用。
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	@Override
	public void setFactoryMethodName(@Nullable String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}

	/**
	 * 返回工厂方法（如果有）。
	 */
	@Override
	@Nullable
	public String getFactoryMethodName() {
		return this.factoryMethodName;
	}

	/**
	 * 为此 Bean 指定构造参数值。
	 */
	public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
		this.constructorArgumentValues = constructorArgumentValues;
	}

	/**
	 * 返回此 Bean 的构造参数值（永不为 {@code null}）。
	 */
	@Override
	public ConstructorArgumentValues getConstructorArgumentValues() {
		if (this.constructorArgumentValues == null) {
			this.constructorArgumentValues = new ConstructorArgumentValues();
		}
		return this.constructorArgumentValues;
	}

	/**
	 * 返回此 Bean 是否定义了构造参数值。
	 */
	@Override
	public boolean hasConstructorArgumentValues() {
		return (this.constructorArgumentValues != null && !this.constructorArgumentValues.isEmpty());
	}

	/**
	 * 为此 Bean 指定属性值（如果有）。
	 */
	public void setPropertyValues(MutablePropertyValues propertyValues) {
		this.propertyValues = propertyValues;
	}

	/**
	 * 返回此 Bean 的属性值（永不为 {@code null}）。
	 */
	@Override
	public MutablePropertyValues getPropertyValues() {
		if (this.propertyValues == null) {
			this.propertyValues = new MutablePropertyValues();
		}
		return this.propertyValues;
	}

	/**
	 * 返回此 Bean 是否定义了属性值。
	 * @since 5.0.2
	 */
	@Override
	public boolean hasPropertyValues() {
		return (this.propertyValues != null && !this.propertyValues.isEmpty());
	}

	/**
	 * 为此 Bean 指定方法覆盖（如果有）。
	 */
	public void setMethodOverrides(MethodOverrides methodOverrides) {
		this.methodOverrides = methodOverrides;
	}

	/**
	 * 返回有关将由 IoC 容器覆盖的方法的信息。如果没有方法覆盖，则为空。<p>永不返回 {@code null}。
	 */
	public MethodOverrides getMethodOverrides() {
		return this.methodOverrides;
	}

	/**
	 * 返回此 Bean 是否定义了方法覆盖。
	 * @since 5.0.2
	 */
	public boolean hasMethodOverrides() {
		return !this.methodOverrides.isEmpty();
	}

	/**
	 * 设置初始化方法的名称。<p>默认值为 {@code null}，此时没有初始化方法。
	 */
	@Override
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = initMethodName;
	}

	/**
	 * 返回初始化方法的名称。
	 */
	@Override
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * 指定所配置的初始化方法是否为默认方法。<p>对于本地指定的 init 方法，默认值为 {@code true}；但对于
	 * defaults 部分中的共享设置（例如 XML 中的 {@code bean init-method} 与
	 * {@code beans default-init-method} 层级），则会切换为 {@code false}，因为该设置可能并不适用于所有
	 * 包含的 Bean 定义。
	 * @see #setInitMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceInitMethod(boolean enforceInitMethod) {
		this.enforceInitMethod = enforceInitMethod;
	}

	/**
	 * 指示所配置的初始化方法是否为默认方法。
	 * @see #getInitMethodName()
	 */
	public boolean isEnforceInitMethod() {
		return this.enforceInitMethod;
	}

	/**
	 * 设置销毁方法的名称。<p>默认值为 {@code null}，此时没有销毁方法。
	 */
	@Override
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}

	/**
	 * 返回销毁方法的名称。
	 */
	@Override
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

	/**
	 * 指定所配置的销毁方法是否为默认方法。<p>对于本地指定的 destroy 方法，默认值为 {@code true}；但对
	 * 于 defaults 部分中的共享设置（例如 XML 中的 {@code bean destroy-method} 与
	 * {@code beans default-destroy-method} 层级），则会切换为 {@code false}，因为该设置可能并不适用于
	 * 所有包含的 Bean 定义。
	 * @see #setDestroyMethodName
	 * @see #applyDefaults
	 */
	public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
		this.enforceDestroyMethod = enforceDestroyMethod;
	}

	/**
	 * 指示所配置的销毁方法是否为默认方法。
	 * @see #getDestroyMethodName()
	 */
	public boolean isEnforceDestroyMethod() {
		return this.enforceDestroyMethod;
	}

	/**
	 * 设置此 Bean 定义是否为“合成的”，即不是由应用本身定义的（例如，通过 {@code <aop:config>} 创建、用
	 * 于自动代理的辅助性基础设施 Bean）。
	 */
	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * 返回此 Bean 定义是否为“合成的”，即不是由应用本身定义的。
	 */
	public boolean isSynthetic() {
		return this.synthetic;
	}

	/**
	 * 为此 {@code BeanDefinition} 设置角色提示。
	 */
	@Override
	public void setRole(int role) {
		this.role = role;
	}

	/**
	 * 返回此 {@code BeanDefinition} 的角色提示。
	 */
	@Override
	public int getRole() {
		return this.role;
	}

	/**
	 * 设置此 Bean 定义的可读描述。
	 */
	@Override
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * 返回此 Bean 定义的可读描述。
	 */
	@Override
	@Nullable
	public String getDescription() {
		return this.description;
	}

	/**
	 * 设置此 Bean 定义来源的资源（用于在出错时显示上下文）。
	 */
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * 返回此 Bean 定义来源的资源。
	 */
	@Nullable
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 设置此 Bean 定义来源资源的描述（用于在出错时显示上下文）。
	 */
	public void setResourceDescription(@Nullable String resourceDescription) {
		this.resource = (resourceDescription != null ? new DescriptiveResource(resourceDescription) : null);
	}

	/**
	 * 返回此 Bean 定义来源资源的描述（用于在出错时显示上下文）。
	 */
	@Override
	@Nullable
	public String getResourceDescription() {
		return (this.resource != null ? this.resource.getDescription() : null);
	}

	/**
	 * 设置最初的（例如被装饰的）BeanDefinition（如果有）。
	 */
	public void setOriginatingBeanDefinition(BeanDefinition originatingBd) {
		this.resource = new BeanDefinitionResource(originatingBd);
	}

	/**
	 * 返回最初的 BeanDefinition；如果没有，则返回 {@code null}。可用于获取被装饰的 Bean 定义（如果有）。
	 * <p>请注意，此方法返回的是直接来源。要找到用户定义的原始 BeanDefinition，请遍历来源链。
	 */
	@Override
	@Nullable
	public BeanDefinition getOriginatingBeanDefinition() {
		return (this.resource instanceof BeanDefinitionResource ?
				((BeanDefinitionResource) this.resource).getBeanDefinition() : null);
	}

	/**
	 * 校验此 Bean 定义。
	 * @throws BeanDefinitionValidationException 校验失败时
	 */
	public void validate() throws BeanDefinitionValidationException {
		/*如果有方法重写（需要代理），又是工厂方法（无法代理） 那就抛出异常*/
		if (hasMethodOverrides() && getFactoryMethodName() != null) {
			throw new BeanDefinitionValidationException(
					"Cannot combine factory method with container-generated method overrides: " +
					"the factory method must create the concrete bean instance.");
		}
		if (hasBeanClass()) {
			prepareMethodOverrides();
		}
	}

	/**
	 * 校验并准备为此 Bean 定义的方法覆盖。检查是否存在具有指定名称的方法。
	 * @throws BeanDefinitionValidationException 校验失败时
	 */
	public void prepareMethodOverrides() throws BeanDefinitionValidationException {
		// Check that lookup methods exist and determine their overloaded status.
		if (hasMethodOverrides()) {
			getMethodOverrides().getOverrides().forEach(new Consumer<MethodOverride>() {
				@Override
				public void accept(MethodOverride mo) {
					AbstractBeanDefinition.this.prepareMethodOverride(mo);
				}
			});
		}
	}

	/**
	 * 校验并准备给定的方法覆盖。检查是否存在具有指定名称的方法，如果未找到，则将其标记为未重载。
	 * @param mo 要校验的 MethodOverride 对象
	 * @throws BeanDefinitionValidationException 校验失败时
	 */
	protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
		int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
		if (count == 0) {
			throw new BeanDefinitionValidationException(
					"Invalid method override: no method with name '" + mo.getMethodName() +
					"' on class [" + getBeanClassName() + "]");
		}
		else if (count == 1) {
			// Mark override as not overloaded, to avoid the overhead of arg type checking.
			mo.setOverloaded(false);
		}
	}


	/**
	 * Object 的 {@code clone()} 方法的 public 声明。委托给 {@link #cloneBeanDefinition()}。
	 * @see Object#clone()
	 */
	@Override
	public Object clone() {
		return cloneBeanDefinition();
	}

	/**
	 * 克隆此 Bean 定义。由具体子类实现。
	 * @return 克隆得到的 Bean 定义对象
	 */
	public abstract AbstractBeanDefinition cloneBeanDefinition();

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractBeanDefinition)) {
			return false;
		}
		AbstractBeanDefinition that = (AbstractBeanDefinition) other;
		return (ObjectUtils.nullSafeEquals(getBeanClassName(), that.getBeanClassName()) &&
				ObjectUtils.nullSafeEquals(this.scope, that.scope) &&
				this.abstractFlag == that.abstractFlag &&
				this.lazyInit == that.lazyInit &&
				this.autowireMode == that.autowireMode &&
				this.dependencyCheck == that.dependencyCheck &&
				Arrays.equals(this.dependsOn, that.dependsOn) &&
				this.autowireCandidate == that.autowireCandidate &&
				ObjectUtils.nullSafeEquals(this.qualifiers, that.qualifiers) &&
				this.primary == that.primary &&
				this.nonPublicAccessAllowed == that.nonPublicAccessAllowed &&
				this.lenientConstructorResolution == that.lenientConstructorResolution &&
				equalsConstructorArgumentValues(that) &&
				equalsPropertyValues(that) &&
				ObjectUtils.nullSafeEquals(this.methodOverrides, that.methodOverrides) &&
				ObjectUtils.nullSafeEquals(this.factoryBeanName, that.factoryBeanName) &&
				ObjectUtils.nullSafeEquals(this.factoryMethodName, that.factoryMethodName) &&
				ObjectUtils.nullSafeEquals(this.initMethodName, that.initMethodName) &&
				this.enforceInitMethod == that.enforceInitMethod &&
				ObjectUtils.nullSafeEquals(this.destroyMethodName, that.destroyMethodName) &&
				this.enforceDestroyMethod == that.enforceDestroyMethod &&
				this.synthetic == that.synthetic &&
				this.role == that.role &&
				super.equals(other));
	}

	private boolean equalsConstructorArgumentValues(AbstractBeanDefinition other) {
		if (!hasConstructorArgumentValues()) {
			return !other.hasConstructorArgumentValues();
		}
		return ObjectUtils.nullSafeEquals(this.constructorArgumentValues, other.constructorArgumentValues);
	}

	private boolean equalsPropertyValues(AbstractBeanDefinition other) {
		if (!hasPropertyValues()) {
			return !other.hasPropertyValues();
		}
		return ObjectUtils.nullSafeEquals(this.propertyValues, other.propertyValues);
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getBeanClassName());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
		if (hasConstructorArgumentValues()) {
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
		}
		if (hasPropertyValues()) {
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
		}
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
		hashCode = 29 * hashCode + super.hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("class [");
		sb.append(getBeanClassName()).append(']');
		sb.append("; scope=").append(this.scope);
		sb.append("; abstract=").append(this.abstractFlag);
		sb.append("; lazyInit=").append(this.lazyInit);
		sb.append("; autowireMode=").append(this.autowireMode);
		sb.append("; dependencyCheck=").append(this.dependencyCheck);
		sb.append("; autowireCandidate=").append(this.autowireCandidate);
		sb.append("; primary=").append(this.primary);
		sb.append("; factoryBeanName=").append(this.factoryBeanName);
		sb.append("; factoryMethodName=").append(this.factoryMethodName);
		sb.append("; initMethodName=").append(this.initMethodName);
		sb.append("; destroyMethodName=").append(this.destroyMethodName);
		if (this.resource != null) {
			sb.append("; defined in ").append(this.resource.getDescription());
		}
		return sb.toString();
	}

}
