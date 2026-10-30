/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scripting.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.asm.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.proxy.InterfaceMaker;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}，用于处理
 * {@link org.springframework.scripting.ScriptFactory} 定义，将每个工厂替换为由其生成的实际脚本化
 * Java 对象。
 *
 * <p>这与 {@link org.springframework.beans.factory.FactoryBean} 机制类似，但它专为脚本量身定制，并
 * 非内置于 Spring 核心容器本身，而是作为扩展实现。
 *
 * <p><b>NOTE:</b> 该后处理器最重要的特点是：构造器参数应用于
 * {@link org.springframework.scripting.ScriptFactory} 实例，而 Bean 属性值应用于生成的脚本化对象。
 * 通常，构造器参数包括脚本源定位符以及可能的脚本接口，而 Bean 属性值包括要注入到脚本化对象本身的引
 * 用和配置值。
 *
 * <p>以下 {@link ScriptFactoryPostProcessor} 将自动应用于下面两个
 * {@link org.springframework.scripting.ScriptFactory} 定义。在运行时，将为 "bshMessenger" 和
 * "groovyMessenger" 暴露实际的脚本化对象，而不是
 * {@link org.springframework.scripting.ScriptFactory} 实例。这两者都应能转换为示例中的
 * {@code Messenger} 接口。
 *
 * <pre class="code">&lt;bean class="org.springframework.scripting.support.ScriptFactoryPostProcessor"/&gt;
 *
 * &lt;bean id="bshMessenger" class="org.springframework.scripting.bsh.BshScriptFactory"&gt;
 *   &lt;constructor-arg value="classpath:mypackage/Messenger.bsh"/&gt;
 *   &lt;constructor-arg value="mypackage.Messenger"/&gt;
 *   &lt;property name="message" value="Hello World!"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="groovyMessenger" class="org.springframework.scripting.groovy.GroovyScriptFactory"&gt;
 *   &lt;constructor-arg value="classpath:mypackage/Messenger.groovy"/&gt;
 *   &lt;property name="message" value="Hello World!"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p><b>NOTE:</b> 请注意，上面这段摘自 Spring XML Bean 定义文件的内容只使用了 &lt;bean/&gt; 风格的
 * 语法（目的是演示使用 {@link ScriptFactoryPostProcessor} 本身）。实际上，你永远不会为
 * {@link ScriptFactoryPostProcessor} 显式创建 &lt;bean/&gt; 定义；相反，你会从 {@code 'lang'} 命名
 * 空间导入标签，并直接使用该命名空间中的标签创建脚本化 Bean……在此过程中，会为你隐式创建一个
 * {@link ScriptFactoryPostProcessor}。
 *
 * <p>Spring 参考文档中包含大量使用 {@code 'lang'} 命名空间中标签的示例；作为示例，下面给出一个使用
 * {@code 'lang:groovy'} 标签定义的 Groovy 支持的 Bean。
 *
 * <pre class="code">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;beans xmlns="http://www.springframework.org/schema/beans"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *     xmlns:lang="http://www.springframework.org/schema/lang"&gt;
 *
 *   &lt;!-- this is the bean definition for the Groovy-backed Messenger implementation --&gt;
 *   &lt;lang:groovy id="messenger" script-source="classpath:Messenger.groovy"&gt;
 *     &lt;lang:property name="message" value="I Can Do The Frug" /&gt;
 *   &lt;/lang:groovy&gt;
 *
 *   &lt;!-- an otherwise normal bean that will be injected by the Groovy-backed Messenger --&gt;
 *   &lt;bean id="bookingService" class="x.y.DefaultBookingService"&gt;
 *     &lt;property name="messenger" ref="messenger" /&gt;
 *   &lt;/bean&gt;
 *
 * &lt;/beans&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.0
 */
public class ScriptFactoryPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
		BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware, DisposableBean, Ordered {

	/**
	 * {@link org.springframework.core.io.Resource} 风格的、用于表示内联脚本的前缀。
	 *
	 * <p>内联脚本是直接定义在（通常是 XML）配置中的脚本，而不是定义在外部文件中的脚本。
	 */
	public static final String INLINE_SCRIPT_PREFIX = "inline:";

	/**
	 * {@code refreshCheckDelay} 属性。
	 */
	public static final String REFRESH_CHECK_DELAY_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ScriptFactoryPostProcessor.class, "refreshCheckDelay");

	/**
	 * {@code proxyTargetClass} 属性。
	 */
	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ScriptFactoryPostProcessor.class, "proxyTargetClass");

	/**
	 * {@code language} 属性。
	 */
	public static final String LANGUAGE_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ScriptFactoryPostProcessor.class, "language");

	private static final String SCRIPT_FACTORY_NAME_PREFIX = "scriptFactory.";

	private static final String SCRIPTED_OBJECT_NAME_PREFIX = "scriptedObject.";


	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private long defaultRefreshCheckDelay = -1;

	private boolean defaultProxyTargetClass = false;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private ConfigurableBeanFactory beanFactory;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	final DefaultListableBeanFactory scriptBeanFactory = new DefaultListableBeanFactory();

	/** 从 bean 名称 String 到 ScriptSource 对象的 Map。 */
	private final Map<String, ScriptSource> scriptSourceCache = new ConcurrentHashMap<>();


	/**
	 * 设置刷新检查之间的延迟，以毫秒为单位。默认值为 -1，表示完全不进行刷新检查。
	 *
	 * <p>请注意，只有 {@link org.springframework.scripting.ScriptSource} 指示其已被修改时，才会实际执
	 * 行刷新。
	 * @see org.springframework.scripting.ScriptSource#isModified()
	 */
	public void setDefaultRefreshCheckDelay(long defaultRefreshCheckDelay) {
		this.defaultRefreshCheckDelay = defaultRefreshCheckDelay;
	}

	/**
	 * 用于表示应创建可刷新代理、以代理目标类而非其接口的标志。
	 * @param defaultProxyTargetClass 要设置的标志值
	 */
	public void setDefaultProxyTargetClass(boolean defaultProxyTargetClass) {
		this.defaultProxyTargetClass = defaultProxyTargetClass;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("ScriptFactoryPostProcessor doesn't work with " +
					"non-ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;

		// Required so that references (up container hierarchies) are correctly resolved.
		this.scriptBeanFactory.setParentBeanFactory(this.beanFactory);

		// Required so that all BeanPostProcessors, Scopes, etc become available.
		this.scriptBeanFactory.copyConfigurationFrom(this.beanFactory);

		// Filter out BeanPostProcessors that are part of the AOP infrastructure,
		// since those are only meant to apply to beans defined in the original factory.
		this.scriptBeanFactory.getBeanPostProcessors().removeIf(beanPostProcessor ->
				beanPostProcessor instanceof AopInfrastructureBean);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public int getOrder() {
		return Integer.MIN_VALUE;
	}


	@Override
	@Nullable
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		// We only apply special treatment to ScriptFactory implementations here.
		if (!ScriptFactory.class.isAssignableFrom(beanClass)) {
			return null;
		}

		Assert.state(this.beanFactory != null, "No BeanFactory set");
		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);

		try {
			String scriptFactoryBeanName = SCRIPT_FACTORY_NAME_PREFIX + beanName;
			String scriptedObjectBeanName = SCRIPTED_OBJECT_NAME_PREFIX + beanName;
			prepareScriptBeans(bd, scriptFactoryBeanName, scriptedObjectBeanName);

			ScriptFactory scriptFactory = this.scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
			ScriptSource scriptSource = getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
			Class<?>[] interfaces = scriptFactory.getScriptInterfaces();

			Class<?> scriptedType = scriptFactory.getScriptedObjectType(scriptSource);
			if (scriptedType != null) {
				return scriptedType;
			}
			else if (!ObjectUtils.isEmpty(interfaces)) {
				return (interfaces.length == 1 ? interfaces[0] : createCompositeInterface(interfaces));
			}
			else {
				if (bd.isSingleton()) {
					return this.scriptBeanFactory.getBean(scriptedObjectBeanName).getClass();
				}
			}
		}
		catch (Exception ex) {
			if (ex instanceof BeanCreationException &&
					((BeanCreationException) ex).getMostSpecificCause() instanceof BeanCurrentlyInCreationException) {
				if (logger.isTraceEnabled()) {
					logger.trace("Could not determine scripted object type for bean '" + beanName + "': " +
							ex.getMessage());
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not determine scripted object type for bean '" + beanName + "'", ex);
				}
			}
		}

		return null;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		// We only apply special treatment to ScriptFactory implementations here.
		if (!ScriptFactory.class.isAssignableFrom(beanClass)) {
			return null;
		}

		Assert.state(this.beanFactory != null, "No BeanFactory set");
		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);
		String scriptFactoryBeanName = SCRIPT_FACTORY_NAME_PREFIX + beanName;
		String scriptedObjectBeanName = SCRIPTED_OBJECT_NAME_PREFIX + beanName;
		prepareScriptBeans(bd, scriptFactoryBeanName, scriptedObjectBeanName);

		ScriptFactory scriptFactory = this.scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
		ScriptSource scriptSource = getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
		boolean isFactoryBean = false;
		try {
			Class<?> scriptedObjectType = scriptFactory.getScriptedObjectType(scriptSource);
			// Returned type may be null if the factory is unable to determine the type.
			if (scriptedObjectType != null) {
				isFactoryBean = FactoryBean.class.isAssignableFrom(scriptedObjectType);
			}
		}
		catch (Exception ex) {
			throw new BeanCreationException(beanName,
					"Could not determine scripted object type for " + scriptFactory, ex);
		}

		long refreshCheckDelay = resolveRefreshCheckDelay(bd);
		if (refreshCheckDelay >= 0) {
			Class<?>[] interfaces = scriptFactory.getScriptInterfaces();
			RefreshableScriptTargetSource ts = new RefreshableScriptTargetSource(this.scriptBeanFactory,
					scriptedObjectBeanName, scriptFactory, scriptSource, isFactoryBean);
			boolean proxyTargetClass = resolveProxyTargetClass(bd);
			String language = (String) bd.getAttribute(LANGUAGE_ATTRIBUTE);
			if (proxyTargetClass && (language == null || !language.equals("groovy"))) {
				throw new BeanDefinitionValidationException(
						"Cannot use proxyTargetClass=true with script beans where language is not 'groovy': '" +
						language + "'");
			}
			ts.setRefreshCheckDelay(refreshCheckDelay);
			return createRefreshableProxy(ts, interfaces, proxyTargetClass);
		}

		if (isFactoryBean) {
			scriptedObjectBeanName = BeanFactory.FACTORY_BEAN_PREFIX + scriptedObjectBeanName;
		}
		return this.scriptBeanFactory.getBean(scriptedObjectBeanName);
	}

	/**
	 * 在此后处理器所使用的内部 BeanFactory 中准备脚本 Bean。每个原始 Bean 定义都将被拆分为一个
	 * ScriptFactory 定义和一个脚本化对象定义。
	 * @param bd 主 BeanFactory 中的原始 Bean 定义
	 * @param scriptFactoryBeanName 内部 ScriptFactory Bean 的名称
	 * @param scriptedObjectBeanName 内部脚本化对象 Bean 的名称
	 */
	protected void prepareScriptBeans(BeanDefinition bd, String scriptFactoryBeanName, String scriptedObjectBeanName) {
		// Avoid recreation of the script bean definition in case of a prototype.
		synchronized (this.scriptBeanFactory) {
			if (!this.scriptBeanFactory.containsBeanDefinition(scriptedObjectBeanName)) {

				this.scriptBeanFactory.registerBeanDefinition(
						scriptFactoryBeanName, createScriptFactoryBeanDefinition(bd));
				ScriptFactory scriptFactory =
						this.scriptBeanFactory.getBean(scriptFactoryBeanName, ScriptFactory.class);
				ScriptSource scriptSource =
						getScriptSource(scriptFactoryBeanName, scriptFactory.getScriptSourceLocator());
				Class<?>[] interfaces = scriptFactory.getScriptInterfaces();

				Class<?>[] scriptedInterfaces = interfaces;
				if (scriptFactory.requiresConfigInterface() && !bd.getPropertyValues().isEmpty()) {
					Class<?> configInterface = createConfigInterface(bd, interfaces);
					scriptedInterfaces = ObjectUtils.addObjectToArray(interfaces, configInterface);
				}

				BeanDefinition objectBd = createScriptedObjectBeanDefinition(
						bd, scriptFactoryBeanName, scriptSource, scriptedInterfaces);
				long refreshCheckDelay = resolveRefreshCheckDelay(bd);
				if (refreshCheckDelay >= 0) {
					objectBd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
				}

				this.scriptBeanFactory.registerBeanDefinition(scriptedObjectBeanName, objectBd);
			}
		}
	}

	/**
	 * 获取给定 {@link ScriptFactory} {@link BeanDefinition} 的刷新检查延迟。如果该
	 * {@link BeanDefinition} 在键 {@link #REFRESH_CHECK_DELAY_ATTRIBUTE} 下有一个类型为有效
	 * {@link Number} 的 {@link org.springframework.core.AttributeAccessor metadata attribute}，则使用
	 * 该值。否则使用 {@link #defaultRefreshCheckDelay} 值。
	 * @param beanDefinition 要检查的 BeanDefinition
	 * @return 刷新检查延迟
	 */
	protected long resolveRefreshCheckDelay(BeanDefinition beanDefinition) {
		long refreshCheckDelay = this.defaultRefreshCheckDelay;
		Object attributeValue = beanDefinition.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
		if (attributeValue instanceof Number) {
			refreshCheckDelay = ((Number) attributeValue).longValue();
		}
		else if (attributeValue instanceof String) {
			refreshCheckDelay = Long.parseLong((String) attributeValue);
		}
		else if (attributeValue != null) {
			throw new BeanDefinitionStoreException("Invalid refresh check delay attribute [" +
					REFRESH_CHECK_DELAY_ATTRIBUTE + "] with value '" + attributeValue +
					"': needs to be of type Number or String");
		}
		return refreshCheckDelay;
	}

	protected boolean resolveProxyTargetClass(BeanDefinition beanDefinition) {
		boolean proxyTargetClass = this.defaultProxyTargetClass;
		Object attributeValue = beanDefinition.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
		if (attributeValue instanceof Boolean) {
			proxyTargetClass = (Boolean) attributeValue;
		}
		else if (attributeValue instanceof String) {
			proxyTargetClass = Boolean.parseBoolean((String) attributeValue);
		}
		else if (attributeValue != null) {
			throw new BeanDefinitionStoreException("Invalid proxy target class attribute [" +
					PROXY_TARGET_CLASS_ATTRIBUTE + "] with value '" + attributeValue +
					"': needs to be of type Boolean or String");
		}
		return proxyTargetClass;
	}

	/**
	 * 根据给定的脚本定义创建一个 ScriptFactory Bean 定义，仅提取与 ScriptFactory 相关的定义数据（即仅
	 * Bean 类和构造器参数）。
	 * @param bd 完整的脚本 Bean 定义
	 * @return 提取出的 ScriptFactory Bean 定义
	 * @see org.springframework.scripting.ScriptFactory
	 */
	protected BeanDefinition createScriptFactoryBeanDefinition(BeanDefinition bd) {
		GenericBeanDefinition scriptBd = new GenericBeanDefinition();
		scriptBd.setBeanClassName(bd.getBeanClassName());
		scriptBd.getConstructorArgumentValues().addArgumentValues(bd.getConstructorArgumentValues());
		return scriptBd;
	}

	/**
	 * 获取给定 Bean 的 ScriptSource，如果尚未缓存则延迟创建它。
	 * @param beanName 脚本化 Bean 的名称
	 * @param scriptSourceLocator 与该 Bean 关联的脚本源定位符
	 * @return 对应的 ScriptSource 实例
	 * @see #convertToScriptSource
	 */
	protected ScriptSource getScriptSource(String beanName, String scriptSourceLocator) {
		return this.scriptSourceCache.computeIfAbsent(beanName, key ->
				convertToScriptSource(beanName, scriptSourceLocator, this.resourceLoader));
	}

	/**
	 * 将给定的脚本源定位符转换为 ScriptSource 实例。
	 *
	 * <p>默认情况下，支持的定位符包括 Spring 资源位置（如 "file:C:/myScript.bsh" 或
	 * "classpath:myPackage/myScript.bsh"）以及内联脚本（"inline:myScriptText..."）。
	 * @param beanName 脚本化 Bean 的名称
	 * @param scriptSourceLocator 脚本源定位符
	 * @param resourceLoader 要使用的 ResourceLoader（如有必要）
	 * @return ScriptSource 实例
	 */
	protected ScriptSource convertToScriptSource(String beanName, String scriptSourceLocator,
			ResourceLoader resourceLoader) {

		if (scriptSourceLocator.startsWith(INLINE_SCRIPT_PREFIX)) {
			return new StaticScriptSource(scriptSourceLocator.substring(INLINE_SCRIPT_PREFIX.length()), beanName);
		}
		else {
			return new ResourceScriptSource(resourceLoader.getResource(scriptSourceLocator));
		}
	}

	/**
	 * 为给定的 Bean 定义创建一个配置接口，为已定义的属性值定义 setter 方法，并定义 init 方法和 destroy
	 * 方法（如果已定义）。
	 *
	 * <p>此实现通过 CGLIB 的 InterfaceMaker 创建接口，并尽可能从给定接口中确定属性类型。
	 * @param bd 要为其创建配置接口的 Bean 定义（属性值等）
	 * @param interfaces 要对照检查的接口（可能定义了与我们应生成的 setter
	 * 相对应的 getter）
	 * @return 配置接口
	 * @see org.springframework.cglib.proxy.InterfaceMaker
	 * @see org.springframework.beans.BeanUtils#findPropertyType
	 */
	protected Class<?> createConfigInterface(BeanDefinition bd, @Nullable Class<?>[] interfaces) {
		InterfaceMaker maker = new InterfaceMaker();
		PropertyValue[] pvs = bd.getPropertyValues().getPropertyValues();
		for (PropertyValue pv : pvs) {
			String propertyName = pv.getName();
			Class<?> propertyType = BeanUtils.findPropertyType(propertyName, interfaces);
			String setterName = "set" + StringUtils.capitalize(propertyName);
			Signature signature = new Signature(setterName, Type.VOID_TYPE, new Type[] {Type.getType(propertyType)});
			maker.add(signature, new Type[0]);
		}
		if (bd.getInitMethodName() != null) {
			Signature signature = new Signature(bd.getInitMethodName(), Type.VOID_TYPE, new Type[0]);
			maker.add(signature, new Type[0]);
		}
		if (StringUtils.hasText(bd.getDestroyMethodName())) {
			Signature signature = new Signature(bd.getDestroyMethodName(), Type.VOID_TYPE, new Type[0]);
			maker.add(signature, new Type[0]);
		}
		return maker.create();
	}

	/**
	 * 为给定接口创建一个组合接口 Class，在单个 Class 中实现给定接口。
	 *
	 * <p>默认实现为给定接口构建一个 JDK 代理类。
	 * @param interfaces 要合并的接口
	 * @return 合并后的接口（以 Class 形式表示）
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
	}

	/**
	 * 根据给定的脚本定义创建脚本化对象的 Bean 定义，提取与脚本化对象相关的定义数据（即除 Bean 类和构造
	 * 器参数之外的所有内容）。
	 * @param bd 完整的脚本 Bean 定义
	 * @param scriptFactoryBeanName 内部 ScriptFactory Bean 的名称
	 * @param scriptSource 脚本化 Bean 的 ScriptSource
	 * @param interfaces 脚本化 Bean 应实现的接口
	 * @return 提取出的 ScriptFactory Bean 定义
	 * @see org.springframework.scripting.ScriptFactory#getScriptedObject
	 */
	protected BeanDefinition createScriptedObjectBeanDefinition(BeanDefinition bd, String scriptFactoryBeanName,
			ScriptSource scriptSource, @Nullable Class<?>[] interfaces) {

		GenericBeanDefinition objectBd = new GenericBeanDefinition(bd);
		objectBd.setFactoryBeanName(scriptFactoryBeanName);
		objectBd.setFactoryMethodName("getScriptedObject");
		objectBd.getConstructorArgumentValues().clear();
		objectBd.getConstructorArgumentValues().addIndexedArgumentValue(0, scriptSource);
		objectBd.getConstructorArgumentValues().addIndexedArgumentValue(1, interfaces);
		return objectBd;
	}

	/**
	 * 为给定的 AOP TargetSource 创建一个可刷新的代理。
	 * @param ts 可刷新的 TargetSource
	 * @param interfaces 代理接口（可为 {@code null}，表示代理目标类实现的
	 * 所有接口）
	 * @return 生成的代理
	 * @see RefreshableScriptTargetSource
	 */
	protected Object createRefreshableProxy(TargetSource ts, @Nullable Class<?>[] interfaces, boolean proxyTargetClass) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(ts);
		ClassLoader classLoader = this.beanClassLoader;

		if (interfaces != null) {
			proxyFactory.setInterfaces(interfaces);
		}
		else {
			Class<?> targetClass = ts.getTargetClass();
			if (targetClass != null) {
				proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.beanClassLoader));
			}
		}

		if (proxyTargetClass) {
			classLoader = null;  // force use of Class.getClassLoader()
			proxyFactory.setProxyTargetClass(true);
		}

		DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(ts);
		introduction.suppressInterface(TargetSource.class);
		proxyFactory.addAdvice(introduction);

		return proxyFactory.getProxy(classLoader);
	}

	/**
	 * 在关闭时销毁内部 Bean 工厂（用于脚本）。
	 */
	@Override
	public void destroy() {
		this.scriptBeanFactory.destroySingletons();
	}

}
