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

package org.springframework.jmx.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Constants;
import org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;
import org.springframework.jmx.export.naming.KeyNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.export.notification.ModelMBeanNotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.MBeanRegistrationSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * JMX 导出器，允许将任何 <i>由 Spring 管理的 Bean</i> 暴露到 JMX
 * {@link javax.management.MBeanServer}，而无需在 Bean 类中定义任何 JMX 特定信息。
 *
 * <p>如果 Bean 实现了某个 JMX 管理接口，MBeanExporter 可以通过其自动检测过程直接将 MBean 注册到服
 * 务器。
 *
 * <p>如果 Bean 未实现任何 JMX 管理接口，MBeanExporter 将使用所提供的 {@link MBeanInfoAssembler} 创
 * 建管理信息。
 *
 * <p>可以通过 {@link #setListeners(MBeanExporterListener[]) listeners} 属性注册一组
 * {@link MBeanExporterListener MBeanExporterListeners}，以便将 MBean 注册和注销事件通知给应用代码。
 *
 * <p>此导出器兼容 MBean 以及 MXBean。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @since 1.2
 * @see #setBeans
 * @see #setAutodetect
 * @see #setAssembler
 * @see #setListeners
 * @see org.springframework.jmx.export.assembler.MBeanInfoAssembler
 * @see MBeanExporterListener
 */
public class MBeanExporter extends MBeanRegistrationSupport implements MBeanExportOperations,
		BeanClassLoaderAware, BeanFactoryAware, InitializingBean, SmartInitializingSingleton, DisposableBean {

	/**
	 * 自动检测模式，表示不应使用自动检测。
	 */
	public static final int AUTODETECT_NONE = 0;

	/**
	 * 自动检测模式，表示只应自动检测有效的 MBean。
	 */
	public static final int AUTODETECT_MBEAN = 1;

	/**
	 * 自动检测模式，表示只有 {@link MBeanInfoAssembler} 才能自动检测 Bean。
	 */
	public static final int AUTODETECT_ASSEMBLER = 2;

	/**
	 * 自动检测模式，表示应使用所有自动检测机制。
	 */
	public static final int AUTODETECT_ALL = AUTODETECT_MBEAN | AUTODETECT_ASSEMBLER;


	/**
	 * 通配符，用于将 {@link javax.management.NotificationListener} 映射到由 {@code MBeanExporter} 注册
	 * 的所有 MBean。
	 */
	private static final String WILDCARD = "*";

	/** 用于 JMX {@code mr_type} "ObjectReference" 的常量。 */
	private static final String MR_TYPE_OBJECT_REFERENCE = "ObjectReference";

	/** 此类中定义的自动检测常量的前缀。 */
	private static final String CONSTANT_PREFIX_AUTODETECT = "AUTODETECT_";


	/** 此类的 Constants 实例。 */
	private static final Constants constants = new Constants(MBeanExporter.class);

	/** 要作为 JMX 受管资源公开的 bean，以 JMX 名称为键。 */
	@Nullable
	private Map<String, Object> beans;

	/** 用于此 MBeanExporter 的自动检测模式。 */
	@Nullable
	private Integer autodetectMode;

	/** 在自动检测 MBean 时是否急切初始化候选 bean。 */
	private boolean allowEagerInit = false;

	/** 存储此导出器要使用的 MBeanInfoAssembler。 */
	private MBeanInfoAssembler assembler = new SimpleReflectiveMBeanInfoAssembler();

	/** 用于为对象创建 ObjectName 的策略。 */
	private ObjectNamingStrategy namingStrategy = new KeyNamingStrategy();

	/** 指示 Spring 是否应修改生成的 ObjectName。 */
	private boolean ensureUniqueRuntimeObjectNames = true;

	/** 指示 Spring 是否应在 MBean 中公开受管资源的 ClassLoader。 */
	private boolean exposeManagedResourceClassLoader = true;

	/** 应从自动检测中排除的一组 bean 名称。 */
	private Set<String> excludedBeans = new HashSet<>();

	/** 在此导出器中注册的 MBeanExporterListener。 */
	@Nullable
	private MBeanExporterListener[] listeners;

	/** 要为此导出器注册的 MBean 注册的 NotificationListener。 */
	@Nullable
	private NotificationListenerBean[] notificationListeners;

	/** 实际已注册的 NotificationListener 的 Map。 */
	private final Map<NotificationListenerBean, ObjectName[]> registeredNotificationListeners = new LinkedHashMap<>();

	/** 存储用于生成延迟初始化代理的 ClassLoader。 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** 存储用于自动检测过程的 BeanFactory。 */
	@Nullable
	private ListableBeanFactory beanFactory;


	/**
	 * 提供要注册到 JMX {@code MBeanServer} 的 Bean 的 {@code Map}。<p>String 键是创建 JMX 对象名的基础。
	 * 默认情况下，将直接根据给定键创建 JMX {@code ObjectName}。可以通过指定自定义
	 * {@code NamingStrategy} 进行定制。<p>值可以是 Bean 实例，也可以是 Bean 名称。Bean 实例通常通过
	 * Bean 引用链接进来。Bean 名称将作为当前工厂中的 Bean 解析，并遵循延迟初始化标记（也就是说，不会触
	 * 发此类 Bean 的初始化）。
	 * @param beans 以 JMX 名称为键、以 Bean 实例或 Bean 名称为值的 Map
	 * @see #setNamingStrategy
	 * @see org.springframework.jmx.export.naming.KeyNamingStrategy
	 * @see javax.management.ObjectName#ObjectName(String)
	 */
	public void setBeans(Map<String, Object> beans) {
		this.beans = beans;
	}

	/**
	 * 设置是否在运行此导出器的 Bean 工厂中自动检测 MBean。如果存在
	 * {@code AutodetectCapableMBeanInfoAssembler}，也会询问它。<p>此功能默认关闭。在此显式指定
	 * {@code true} 以启用自动检测。
	 * @see #setAssembler
	 * @see AutodetectCapableMBeanInfoAssembler
	 * @see #isMBean
	 */
	public void setAutodetect(boolean autodetect) {
		this.autodetectMode = (autodetect ? AUTODETECT_ALL : AUTODETECT_NONE);
	}

	/**
	 * 设置要使用的自动检测模式。
	 * @throws IllegalArgumentException 如果提供的值不是 {@code AUTODETECT_} 常量之一
	 * @see #setAutodetectModeName(String)
	 * @see #AUTODETECT_ALL
	 * @see #AUTODETECT_ASSEMBLER
	 * @see #AUTODETECT_MBEAN
	 * @see #AUTODETECT_NONE
	 */
	public void setAutodetectMode(int autodetectMode) {
		if (!constants.getValues(CONSTANT_PREFIX_AUTODETECT).contains(autodetectMode)) {
			throw new IllegalArgumentException("Only values of autodetect constants allowed");
		}
		this.autodetectMode = autodetectMode;
	}

	/**
	 * 按名称设置要使用的自动检测模式。
	 * @throws IllegalArgumentException 如果提供的值无法解析为
	 * 某个 {@code AUTODETECT_} 常量，或为 {@code null}
	 * @see #setAutodetectMode(int)
	 * @see #AUTODETECT_ALL
	 * @see #AUTODETECT_ASSEMBLER
	 * @see #AUTODETECT_MBEAN
	 * @see #AUTODETECT_NONE
	 */
	public void setAutodetectModeName(String constantName) {
		if (!constantName.startsWith(CONSTANT_PREFIX_AUTODETECT)) {
			throw new IllegalArgumentException("Only autodetect constants allowed");
		}
		this.autodetectMode = (Integer) constants.asNumber(constantName);
	}

	/**
	 * 指定在 Spring 应用上下文中自动检测 MBean 时，是否允许对候选 Bean 进行提前初始化。<p>默认值为
	 * "false"，遵循 Bean 定义上的延迟初始化标志。将其切换为 "true" 可以同时搜索延迟初始化的 Bean，包括
	 * FactoryBean 生成但尚未初始化的对象。
	 */
	public void setAllowEagerInit(boolean allowEagerInit) {
		this.allowEagerInit = allowEagerInit;
	}

	/**
	 * 设置此导出器要使用的 {@code MBeanInfoAssembler} 接口实现。默认是
	 * {@code SimpleReflectiveMBeanInfoAssembler}。<p>传入的装配器可以选择实现
	 * {@code AutodetectCapableMBeanInfoAssembler} 接口，从而能够参与导出器的 MBean 自动检测过程。
	 * @see org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler
	 * @see org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler
	 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
	 * @see #setAutodetect
	 */
	public void setAssembler(MBeanInfoAssembler assembler) {
		this.assembler = assembler;
	}

	/**
	 * 设置此导出器要使用的 {@code ObjectNamingStrategy} 接口实现。默认是 {@code KeyNamingStrategy}。
	 * @see org.springframework.jmx.export.naming.KeyNamingStrategy
	 * @see org.springframework.jmx.export.naming.MetadataNamingStrategy
	 */
	public void setNamingStrategy(ObjectNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * 指示 Spring 是否应确保为运行时注册的 MBean（{@link #registerManagedResource}）由所配置的
	 * {@link ObjectNamingStrategy} 生成的 {@link ObjectName ObjectNames} 被修改：以确保受管
	 * {@code Class} 的每个实例的唯一性。<p>默认值为 {@code true}。
	 * @see #registerManagedResource
	 * @see JmxUtils#appendIdentityToObjectName(javax.management.ObjectName, Object)
	 */
	public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
		this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
	}

	/**
	 * 指示是否应在允许对 MBean 进行任何调用之前，将受管资源暴露在
	 * {@link Thread#getContextClassLoader() thread context ClassLoader} 上。<p>默认值为 {@code true}，
	 * 即暴露一个执行线程上下文 ClassLoader 管理的 {@link SpringModelMBean}。关闭此标志可暴露标准的 JMX
	 * {@link javax.management.modelmbean.RequiredModelMBean}。
	 */
	public void setExposeManagedResourceClassLoader(boolean exposeManagedResourceClassLoader) {
		this.exposeManagedResourceClassLoader = exposeManagedResourceClassLoader;
	}

	/**
	 * 设置应从自动检测中排除的 Bean 名称列表。
	 */
	public void setExcludedBeans(String... excludedBeans) {
		this.excludedBeans.clear();
		Collections.addAll(this.excludedBeans, excludedBeans);
	}

	/**
	 * 添加应从自动检测中排除的 Bean 名称。
	 */
	public void addExcludedBean(String excludedBean) {
		Assert.notNull(excludedBean, "ExcludedBean must not be null");
		this.excludedBeans.add(excludedBean);
	}

	/**
	 * 设置应接收 MBean 注册和注销事件通知的 {@code MBeanExporterListener}。
	 * @see MBeanExporterListener
	 */
	public void setListeners(MBeanExporterListener... listeners) {
		this.listeners = listeners;
	}

	/**
	 * 设置 {@link NotificationListenerBean NotificationListenerBeans}，其中包含将向
	 * {@link MBeanServer} 注册的 {@link javax.management.NotificationListener NotificationListeners}。
	 * @see #setNotificationListenerMappings(java.util.Map)
	 * @see NotificationListenerBean
	 */
	public void setNotificationListeners(NotificationListenerBean... notificationListeners) {
		this.notificationListeners = notificationListeners;
	}

	/**
	 * 设置要注册到 {@link javax.management.MBeanServer} 的
	 * {@link NotificationListener NotificationListeners}。<P>{@code Map} 中每个条目的键是监听器应注册
	 * 到的 MBean 的 {@link javax.management.ObjectName} 的 {@link String} 表示形式或 Bean 名称。为键指
	 * 定星号（{@code *}）将使监听器在启动时与此类注册的所有 MBean 关联。<p>每个条目的值是要注册的
	 * {@link javax.management.NotificationListener}。有关更高级的选项，例如注册
	 * {@link javax.management.NotificationFilter NotificationFilters} 和 handback 对象，请参阅
	 * {@link #setNotificationListeners(NotificationListenerBean[])}。
	 */
	public void setNotificationListenerMappings(Map<?, ? extends NotificationListener> listeners) {
		Assert.notNull(listeners, "'listeners' must not be null");
		List<NotificationListenerBean> notificationListeners =
				new ArrayList<>(listeners.size());

		listeners.forEach((key, listener) -> {
			// Get the listener from the map value.
			NotificationListenerBean bean = new NotificationListenerBean(listener);
			// Get the ObjectName from the map key.
			if (key != null && !WILDCARD.equals(key)) {
				// This listener is mapped to a specific ObjectName.
				bean.setMappedObjectName(key);
			}
			notificationListeners.add(bean);
		});

		this.notificationListeners = notificationListeners.toArray(new NotificationListenerBean[0]);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 仅当需要解析 {@link #setBeans(java.util.Map) "beans"} {@link Map} 中的 Bean 名称，以及自动检测
	 * MBean 时（后一种情况需要 {@code ListableBeanFactory}），才需要此回调。
	 * @see #setBeans
	 * @see #setAutodetect
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
		else {
			logger.debug("MBeanExporter not running in a ListableBeanFactory: autodetection of MBeans not available.");
		}
	}


	//---------------------------------------------------------------------
	// Lifecycle in bean factory: automatically register/unregister beans
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() {
		// If no server was provided then try to find one. This is useful in an environment
		// where there is already an MBeanServer loaded.
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}
	}

	/**
	 * 在常规单例实例化阶段之后自动启动 Bean 注册。
	 * @see #registerBeans()
	 */
	@Override
	public void afterSingletonsInstantiated() {
		try {
			logger.debug("Registering beans for JMX exposure on startup");
			registerBeans();
			registerNotificationListeners();
		}
		catch (RuntimeException ex) {
			// Unregister beans already registered by this exporter.
			unregisterNotificationListeners();
			unregisterBeans();
			throw ex;
		}
	}

	/**
	 * 当外层 {@code ApplicationContext} 被销毁时，注销此导出器通过 JMX 暴露的所有 Bean。
	 */
	@Override
	public void destroy() {
		logger.debug("Unregistering JMX-exposed beans on shutdown");
		unregisterNotificationListeners();
		unregisterBeans();
	}


	//---------------------------------------------------------------------
	// Implementation of MBeanExportOperations interface
	//---------------------------------------------------------------------

	@Override
	public ObjectName registerManagedResource(Object managedResource) throws MBeanExportException {
		Assert.notNull(managedResource, "Managed resource must not be null");
		ObjectName objectName;
		try {
			objectName = getObjectName(managedResource, null);
			if (this.ensureUniqueRuntimeObjectNames) {
				objectName = JmxUtils.appendIdentityToObjectName(objectName, managedResource);
			}
		}
		catch (Throwable ex) {
			throw new MBeanExportException("Unable to generate ObjectName for MBean [" + managedResource + "]", ex);
		}
		registerManagedResource(managedResource, objectName);
		return objectName;
	}

	@Override
	public void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException {
		Assert.notNull(managedResource, "Managed resource must not be null");
		Assert.notNull(objectName, "ObjectName must not be null");
		try {
			if (isMBean(managedResource.getClass())) {
				doRegister(managedResource, objectName);
			}
			else {
				ModelMBean mbean = createAndConfigureMBean(managedResource, managedResource.getClass().getName());
				doRegister(mbean, objectName);
				injectNotificationPublisherIfNecessary(managedResource, mbean, objectName);
			}
		}
		catch (JMException ex) {
			throw new UnableToRegisterMBeanException(
					"Unable to register MBean [" + managedResource + "] with object name [" + objectName + "]", ex);
		}
	}

	@Override
	public void unregisterManagedResource(ObjectName objectName) {
		Assert.notNull(objectName, "ObjectName must not be null");
		doUnregister(objectName);
	}


	//---------------------------------------------------------------------
	// Exporter implementation
	//---------------------------------------------------------------------

	/**
	 * 将已定义的 Bean 注册到 {@link MBeanServer}。<p>每个 Bean 都通过 {@code ModelMBean} 暴露给
	 * {@code MBeanServer}。所使用的 {@code ModelMBean} 接口的实际实现取决于所配置的
	 * {@code ModelMBeanProvider} 接口的实现。默认情况下，使用所有 JMX 实现都提供的
	 * {@code RequiredModelMBean} 类。<p>为每个 Bean 生成的管理接口取决于所使用的
	 * {@code MBeanInfoAssembler} 实现。为每个 Bean 指定的 {@code ObjectName} 取决于所使用的
	 * {@code ObjectNamingStrategy} 接口的实现。
	 */
	protected void registerBeans() {
		// The beans property may be null, for example if we are relying solely on autodetection.
		if (this.beans == null) {
			this.beans = new HashMap<>();
			// Use AUTODETECT_ALL as default in no beans specified explicitly.
			if (this.autodetectMode == null) {
				this.autodetectMode = AUTODETECT_ALL;
			}
		}

		// Perform autodetection, if desired.
		int mode = (this.autodetectMode != null ? this.autodetectMode : AUTODETECT_NONE);
		if (mode != AUTODETECT_NONE) {
			if (this.beanFactory == null) {
				throw new MBeanExportException("Cannot autodetect MBeans if not running in a BeanFactory");
			}
			if (mode == AUTODETECT_MBEAN || mode == AUTODETECT_ALL) {
				// Autodetect any beans that are already MBeans.
				logger.debug("Autodetecting user-defined JMX MBeans");
				autodetect(this.beans, (beanClass, beanName) -> isMBean(beanClass));
			}
			// Allow the assembler a chance to vote for bean inclusion.
			if ((mode == AUTODETECT_ASSEMBLER || mode == AUTODETECT_ALL) &&
					this.assembler instanceof AutodetectCapableMBeanInfoAssembler) {
				autodetect(this.beans, ((AutodetectCapableMBeanInfoAssembler) this.assembler)::includeBean);
			}
		}

		if (!this.beans.isEmpty()) {
			this.beans.forEach((beanName, instance) -> registerBeanNameOrInstance(instance, beanName));
		}
	}

	/**
	 * 返回指定的 Bean 定义是否应被视为延迟初始化。
	 * @param beanFactory 应包含该 Bean 定义的 Bean 工厂
	 * @param beanName 要检查的 Bean 的名称
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 * @see org.springframework.beans.factory.config.BeanDefinition#isLazyInit
	 */
	protected boolean isBeanDefinitionLazyInit(ListableBeanFactory beanFactory, String beanName) {
		return (beanFactory instanceof ConfigurableListableBeanFactory && beanFactory.containsBeanDefinition(beanName) &&
				((ConfigurableListableBeanFactory) beanFactory).getBeanDefinition(beanName).isLazyInit());
	}

	/**
	 * 将单个 Bean 注册到 {@link #setServer MBeanServer}。<p>此方法负责决定 Bean <strong>如何</strong>
	 * 暴露给 {@code MBeanServer}。具体而言，如果所提供的 {@code mapValue} 是配置为延迟初始化的 Bean 的
	 * 名称，则向 {@code MBeanServer} 注册指向该资源的代理，以便遵循延迟加载行为。如果该 Bean 已经是
	 * MBean，则会在没有任何干预的情况下直接注册到 {@code MBeanServer}。对于所有其他 Bean 或 Bean 名称，
	 * 资源本身会直接注册到 {@code MBeanServer}。
	 * @param mapValue 在 beans 映射中为此 Bean 配置的值；
	 * 可以是 Bean 的 {@code String} 名称，也可以是 Bean 本身
	 * @param beanKey 此 Bean 在 beans 映射中关联的键
	 * @return 资源注册时使用的 {@code ObjectName}
	 * @throws MBeanExportException 如果导出失败
	 * @see #setBeans
	 * @see #registerBeanInstance
	 * @see #registerLazyInit
	 */
	protected ObjectName registerBeanNameOrInstance(Object mapValue, String beanKey) throws MBeanExportException {
		try {
			if (mapValue instanceof String) {
				// Bean name pointing to a potentially lazy-init bean in the factory.
				if (this.beanFactory == null) {
					throw new MBeanExportException("Cannot resolve bean names if not running in a BeanFactory");
				}
				String beanName = (String) mapValue;
				if (isBeanDefinitionLazyInit(this.beanFactory, beanName)) {
					ObjectName objectName = registerLazyInit(beanName, beanKey);
					replaceNotificationListenerBeanNameKeysIfNecessary(beanName, objectName);
					return objectName;
				}
				else {
					Object bean = this.beanFactory.getBean(beanName);
					ObjectName objectName = registerBeanInstance(bean, beanKey);
					replaceNotificationListenerBeanNameKeysIfNecessary(beanName, objectName);
					return objectName;
				}
			}
			else {
				// Plain bean instance -> register it directly.
				if (this.beanFactory != null) {
					Map<String, ?> beansOfSameType =
							this.beanFactory.getBeansOfType(mapValue.getClass(), false, this.allowEagerInit);
					for (Map.Entry<String, ?> entry : beansOfSameType.entrySet()) {
						if (entry.getValue() == mapValue) {
							String beanName = entry.getKey();
							ObjectName objectName = registerBeanInstance(mapValue, beanKey);
							replaceNotificationListenerBeanNameKeysIfNecessary(beanName, objectName);
							return objectName;
						}
					}
				}
				return registerBeanInstance(mapValue, beanKey);
			}
		}
		catch (Throwable ex) {
			throw new UnableToRegisterMBeanException(
					"Unable to register MBean [" + mapValue + "] with key '" + beanKey + "'", ex);
		}
	}

	/**
	 * 将 {@code NotificationListener} 映射中用作键的任何 Bean 名称替换为对应的 {@code ObjectName} 值。
	 * @param beanName 要注册的 Bean 的名称
	 * @param objectName 该 Bean 将在 {@code MBeanServer} 中注册时使用的
	 * {@code ObjectName}
	 */
	private void replaceNotificationListenerBeanNameKeysIfNecessary(String beanName, ObjectName objectName) {
		if (this.notificationListeners != null) {
			for (NotificationListenerBean notificationListener : this.notificationListeners) {
				notificationListener.replaceObjectName(beanName, objectName);
			}
		}
	}

	/**
	 * 将现有的 MBean 或普通 Bean 的 MBean 适配器注册到 {@code MBeanServer}。
	 * @param bean 要注册的 Bean，可以是 MBean 或普通 Bean
	 * @param beanKey 此 Bean 在 beans 映射中关联的键
	 * @return 该 Bean 在 {@code MBeanServer} 中注册时使用的
	 * {@code ObjectName}
	 */
	private ObjectName registerBeanInstance(Object bean, String beanKey) throws JMException {
		ObjectName objectName = getObjectName(bean, beanKey);
		Object mbeanToExpose = null;
		if (isMBean(bean.getClass())) {
			mbeanToExpose = bean;
		}
		else {
			DynamicMBean adaptedBean = adaptMBeanIfPossible(bean);
			if (adaptedBean != null) {
				mbeanToExpose = adaptedBean;
			}
		}

		if (mbeanToExpose != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Located MBean '" + beanKey + "': registering with JMX server as MBean [" +
						objectName + "]");
			}
			doRegister(mbeanToExpose, objectName);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Located managed bean '" + beanKey + "': registering with JMX server as MBean [" +
						objectName + "]");
			}
			ModelMBean mbean = createAndConfigureMBean(bean, beanKey);
			doRegister(mbean, objectName);
			injectNotificationPublisherIfNecessary(bean, mbean, objectName);
		}

		return objectName;
	}

	/**
	 * 将配置为延迟初始化的 Bean 通过代理间接注册到 {@code MBeanServer}。
	 * @param beanName Bean 在 {@code BeanFactory} 中的名称
	 * @param beanKey 此 Bean 在 beans 映射中关联的键
	 * @return 该 Bean 在 {@code MBeanServer} 中注册时使用的
	 * {@code ObjectName}
	 */
	private ObjectName registerLazyInit(String beanName, String beanKey) throws JMException {
		Assert.state(this.beanFactory != null, "No BeanFactory set");

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.setFrozen(true);

		if (isMBean(this.beanFactory.getType(beanName))) {
			// A straight MBean... Let's create a simple lazy-init CGLIB proxy for it.
			LazyInitTargetSource targetSource = new LazyInitTargetSource();
			targetSource.setTargetBeanName(beanName);
			targetSource.setBeanFactory(this.beanFactory);
			proxyFactory.setTargetSource(targetSource);

			Object proxy = proxyFactory.getProxy(this.beanClassLoader);
			ObjectName objectName = getObjectName(proxy, beanKey);
			if (logger.isDebugEnabled()) {
				logger.debug("Located MBean '" + beanKey + "': registering with JMX server as lazy-init MBean [" +
						objectName + "]");
			}
			doRegister(proxy, objectName);
			return objectName;
		}

		else {
			// A simple bean... Let's create a lazy-init ModelMBean proxy with notification support.
			NotificationPublisherAwareLazyTargetSource targetSource = new NotificationPublisherAwareLazyTargetSource();
			targetSource.setTargetBeanName(beanName);
			targetSource.setBeanFactory(this.beanFactory);
			proxyFactory.setTargetSource(targetSource);

			Object proxy = proxyFactory.getProxy(this.beanClassLoader);
			ObjectName objectName = getObjectName(proxy, beanKey);
			if (logger.isDebugEnabled()) {
				logger.debug("Located simple bean '" + beanKey + "': registering with JMX server as lazy-init MBean [" +
						objectName + "]");
			}
			ModelMBean mbean = createAndConfigureMBean(proxy, beanKey);
			targetSource.setModelMBean(mbean);
			targetSource.setObjectName(objectName);
			doRegister(mbean, objectName);
			return objectName;
		}
	}

	/**
	 * 检索 Bean 的 {@code ObjectName}。<p>如果该 Bean 实现了 {@code SelfNaming} 接口，则使用
	 * {@code SelfNaming.getObjectName()} 检索 {@code ObjectName}。否则，使用所配置的
	 * {@code ObjectNamingStrategy}。
	 * @param bean Bean 在 {@code BeanFactory} 中的名称
	 * @param beanKey Bean 在 beans 映射中关联的键
	 * @return 所提供 Bean 的 {@code ObjectName}
	 * @throws javax.management.MalformedObjectNameException 如果获取到的 {@code ObjectName} 格式错误
	 */
	protected ObjectName getObjectName(Object bean, @Nullable String beanKey) throws MalformedObjectNameException {
		if (bean instanceof SelfNaming) {
			return ((SelfNaming) bean).getObjectName();
		}
		else {
			return this.namingStrategy.getObjectName(bean, beanKey);
		}
	}

	/**
	 * 确定给定的 Bean 类本身是否符合 MBean 的条件。<p>默认实现委托给 {@link JmxUtils#isMBean}，它会检
	 * 查 {@link javax.management.DynamicMBean} 类，以及具有对应 "*MBean" 接口（Standard MBean）或对应
	 * "*MXBean" 接口（Java MXBean）的类。
	 * @param beanClass 要分析的 Bean 类
	 * @return 该类是否符合 MBean 条件
	 * @see org.springframework.jmx.support.JmxUtils#isMBean(Class)
	 */
	protected boolean isMBean(@Nullable Class<?> beanClass) {
		return JmxUtils.isMBean(beanClass);
	}

	/**
	 * 如果可能，为给定的 Bean 实例构建适配的 MBean。<p>对于 AOP 代理，默认实现会为目标对象的
	 * MBean/MXBean 接口构建 JMX 1.2 StandardMBean，并将该接口的管理操作委托给代理。
	 * @param bean 原始 Bean 实例
	 * @return 适配后的 MBean，如果无法适配则为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected DynamicMBean adaptMBeanIfPossible(Object bean) throws JMException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (targetClass != bean.getClass()) {
			Class<?> ifc = JmxUtils.getMXBeanInterface(targetClass);
			if (ifc != null) {
				if (!ifc.isInstance(bean)) {
					throw new NotCompliantMBeanException("Managed bean [" + bean +
							"] has a target class with an MXBean interface but does not expose it in the proxy");
				}
				return new StandardMBean(bean, ((Class<Object>) ifc), true);
			}
			else {
				ifc = JmxUtils.getMBeanInterface(targetClass);
				if (ifc != null) {
					if (!ifc.isInstance(bean)) {
						throw new NotCompliantMBeanException("Managed bean [" + bean +
								"] has a target class with an MBean interface but does not expose it in the proxy");
					}
					return new StandardMBean(bean, ((Class<Object>) ifc));
				}
			}
		}
		return null;
	}

	/**
	 * 创建为所提供的受管资源配置了适当管理接口的 MBean。
	 * @param managedResource 要作为 MBean 导出的资源
	 * @param beanKey 受管 Bean 关联的键
	 * @see #createModelMBean()
	 * @see #getMBeanInfo(Object, String)
	 */
	protected ModelMBean createAndConfigureMBean(Object managedResource, String beanKey)
			throws MBeanExportException {
		try {
			ModelMBean mbean = createModelMBean();
			mbean.setModelMBeanInfo(getMBeanInfo(managedResource, beanKey));
			mbean.setManagedResource(managedResource, MR_TYPE_OBJECT_REFERENCE);
			return mbean;
		}
		catch (Throwable ex) {
			throw new MBeanExportException("Could not create ModelMBean for managed resource [" +
					managedResource + "] with key '" + beanKey + "'", ex);
		}
	}

	/**
	 * 创建实现 {@code ModelMBean} 的类的实例。<p>调用此方法以获取注册 Bean 时要使用的
	 * {@code ModelMBean} 实例。在注册阶段，每个 Bean 会调用一次此方法，并且必须返回一个新的
	 * {@code ModelMBean} 实例。
	 * @return 实现 {@code ModelMBean} 的类的新实例
	 * @throws javax.management.MBeanException 如果 ModelMBean 创建失败
	 */
	protected ModelMBean createModelMBean() throws MBeanException {
		return (this.exposeManagedResourceClassLoader ? new SpringModelMBean() : new RequiredModelMBean());
	}

	/**
	 * 获取具有所提供键且属于所提供类型的 Bean 的 {@code ModelMBeanInfo}。
	 */
	private ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException {
		ModelMBeanInfo info = this.assembler.getMBeanInfo(managedBean, beanKey);
		if (logger.isInfoEnabled() && ObjectUtils.isEmpty(info.getAttributes()) &&
				ObjectUtils.isEmpty(info.getOperations())) {
			logger.info("Bean with key '" + beanKey +
					"' has been registered as an MBean but has no exposed attributes or operations");
		}
		return info;
	}


	//---------------------------------------------------------------------
	// Autodetection process
	//---------------------------------------------------------------------

	/**
	 * 执行实际的自动检测过程，委托给 {@code AutodetectCallback} 实例来对是否包含给定 Bean 进行投票。
	 * @param callback 在决定是否包含某个 Bean 时使用的
	 * {@code AutodetectCallback}
	 */
	private void autodetect(Map<String, Object> beans, AutodetectCallback callback) {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		Set<String> beanNames = new LinkedHashSet<>(this.beanFactory.getBeanDefinitionCount());
		Collections.addAll(beanNames, this.beanFactory.getBeanDefinitionNames());
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			Collections.addAll(beanNames, ((ConfigurableBeanFactory) this.beanFactory).getSingletonNames());
		}

		for (String beanName : beanNames) {
			if (!isExcluded(beanName) && !isBeanDefinitionAbstract(this.beanFactory, beanName)) {
				try {
					Class<?> beanClass = this.beanFactory.getType(beanName);
					if (beanClass != null && callback.include(beanClass, beanName)) {
						boolean lazyInit = isBeanDefinitionLazyInit(this.beanFactory, beanName);
						Object beanInstance = null;
						if (!lazyInit) {
							beanInstance = this.beanFactory.getBean(beanName);
							if (!beanClass.isInstance(beanInstance)) {
								continue;
							}
						}
						if (!ScopedProxyUtils.isScopedTarget(beanName) && !beans.containsValue(beanName) &&
								(beanInstance == null ||
										!CollectionUtils.containsInstance(beans.values(), beanInstance))) {
							// Not already registered for JMX exposure.
							beans.put(beanName, (beanInstance != null ? beanInstance : beanName));
							if (logger.isDebugEnabled()) {
								logger.debug("Bean with name '" + beanName + "' has been autodetected for JMX exposure");
							}
						}
						else {
							if (logger.isTraceEnabled()) {
								logger.trace("Bean with name '" + beanName + "' is already registered for JMX exposure");
							}
						}
					}
				}
				catch (CannotLoadBeanClassException ex) {
					if (this.allowEagerInit) {
						throw ex;
					}
					// otherwise ignore beans where the class is not resolvable
				}
			}
		}
	}

	/**
	 * 指示特定的 Bean 名称是否存在于排除的 Bean 列表中。
	 */
	private boolean isExcluded(String beanName) {
		return (this.excludedBeans.contains(beanName) ||
					(beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX) &&
							this.excludedBeans.contains(beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length()))));
	}

	/**
	 * 返回指定的 Bean 定义是否应被视为抽象。
	 */
	private boolean isBeanDefinitionAbstract(ListableBeanFactory beanFactory, String beanName) {
		return (beanFactory instanceof ConfigurableListableBeanFactory && beanFactory.containsBeanDefinition(beanName) &&
				((ConfigurableListableBeanFactory) beanFactory).getBeanDefinition(beanName).isAbstract());
	}


	//---------------------------------------------------------------------
	// Notification and listener management
	//---------------------------------------------------------------------

	/**
	 * 如果所提供的受管资源实现了 {@link NotificationPublisherAware}，则注入
	 * {@link org.springframework.jmx.export.notification.NotificationPublisher} 的实例。
	 */
	private void injectNotificationPublisherIfNecessary(
			Object managedResource, @Nullable ModelMBean modelMBean, @Nullable ObjectName objectName) {

		if (managedResource instanceof NotificationPublisherAware && modelMBean != null && objectName != null) {
			((NotificationPublisherAware) managedResource).setNotificationPublisher(
					new ModelMBeanNotificationPublisher(modelMBean, objectName, managedResource));
		}
	}

	/**
	 * 将所配置的 {@link NotificationListener NotificationListeners} 注册到 {@link MBeanServer}。
	 */
	private void registerNotificationListeners() throws MBeanExportException {
		if (this.notificationListeners != null) {
			Assert.state(this.server != null, "No MBeanServer available");
			for (NotificationListenerBean bean : this.notificationListeners) {
				try {
					ObjectName[] mappedObjectNames = bean.getResolvedObjectNames();
					if (mappedObjectNames == null) {
						// Mapped to all MBeans registered by the MBeanExporter.
						mappedObjectNames = getRegisteredObjectNames();
					}
					if (this.registeredNotificationListeners.put(bean, mappedObjectNames) == null) {
						for (ObjectName mappedObjectName : mappedObjectNames) {
							this.server.addNotificationListener(mappedObjectName, bean.getNotificationListener(),
									bean.getNotificationFilter(), bean.getHandback());
						}
					}
				}
				catch (Throwable ex) {
					throw new MBeanExportException("Unable to register NotificationListener", ex);
				}
			}
		}
	}

	/**
	 * 从 {@link MBeanServer} 注销所配置的 {@link NotificationListener NotificationListeners}。
	 */
	private void unregisterNotificationListeners() {
		if (this.server != null) {
			this.registeredNotificationListeners.forEach((bean, mappedObjectNames) -> {
				for (ObjectName mappedObjectName : mappedObjectNames) {
					try {
						this.server.removeNotificationListener(mappedObjectName, bean.getNotificationListener(),
								bean.getNotificationFilter(), bean.getHandback());
					}
					catch (Throwable ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Unable to unregister NotificationListener", ex);
						}
					}
				}
			});
		}
		this.registeredNotificationListeners.clear();
	}

	/**
	 * 在 MBean 被注册时调用。向所有已注册的 {@link MBeanExporterListener MBeanExporterListeners} 通知
	 * 该注册事件。<p>请注意，如果某个 {@link MBeanExporterListener} 在收到通知时抛出（运行时）异常，实
	 * 际上会中断通知过程，任何尚未收到通知的其余监听器将（显然）不会收到
	 * {@link MBeanExporterListener#mbeanRegistered(javax.management.ObjectName)} 回调。
	 * @param objectName 已注册 MBean 的 {@code ObjectName}
	 */
	@Override
	protected void onRegister(ObjectName objectName) {
		notifyListenersOfRegistration(objectName);
	}

	/**
	 * 在 MBean 被注销时调用。向所有已注册的 {@link MBeanExporterListener MBeanExporterListeners} 通知
	 * 该注销事件。<p>请注意，如果某个 {@link MBeanExporterListener} 在收到通知时抛出（运行时）异常，实
	 * 际上会中断通知过程，任何尚未收到通知的其余监听器将（显然）不会收到
	 * {@link MBeanExporterListener#mbeanUnregistered(javax.management.ObjectName)} 回调。
	 * @param objectName 已注销 MBean 的 {@code ObjectName}
	 */
	@Override
	protected void onUnregister(ObjectName objectName) {
		notifyListenersOfUnregistration(objectName);
	}


	/**
	 * 向所有已注册的 {@link MBeanExporterListener MBeanExporterListeners} 通知由所提供的
	 * {@link ObjectName} 标识的 MBean 的注册。
	 */
	private void notifyListenersOfRegistration(ObjectName objectName) {
		if (this.listeners != null) {
			for (MBeanExporterListener listener : this.listeners) {
				listener.mbeanRegistered(objectName);
			}
		}
	}

	/**
	 * 向所有已注册的 {@link MBeanExporterListener MBeanExporterListeners} 通知由所提供的
	 * {@link ObjectName} 标识的 MBean 的注销。
	 */
	private void notifyListenersOfUnregistration(ObjectName objectName) {
		if (this.listeners != null) {
			for (MBeanExporterListener listener : this.listeners) {
				listener.mbeanUnregistered(objectName);
			}
		}
	}


	//---------------------------------------------------------------------
	// Inner classes for internal use
	//---------------------------------------------------------------------

	/**
	 * 自动检测过程的内部回调接口。
	 */
	@FunctionalInterface
	private interface AutodetectCallback {

		/**
		 * 在自动检测过程中调用，以决定是否应包含某个 Bean。
		 * @param beanClass Bean 的类
		 * @param beanName Bean 的名称
		 */
		boolean include(Class<?> beanClass, String beanName);
	}


	/**
	 * 对 {@link LazyInitTargetSource} 的扩展，在需要时会在延迟资源创建时向其注入
	 * {@link org.springframework.jmx.export.notification.NotificationPublisher}。
	 */
	@SuppressWarnings("serial")
	private class NotificationPublisherAwareLazyTargetSource extends LazyInitTargetSource {

		@Nullable
		private ModelMBean modelMBean;

		@Nullable
		private ObjectName objectName;

		public void setModelMBean(ModelMBean modelMBean) {
			this.modelMBean = modelMBean;
		}

		public void setObjectName(ObjectName objectName) {
			this.objectName = objectName;
		}

		@Override
		@Nullable
		public Object getTarget() {
			try {
				return super.getTarget();
			}
			catch (RuntimeException ex) {
				if (logger.isInfoEnabled()) {
					logger.info("Failed to retrieve target for JMX-exposed bean [" + this.objectName + "]: " + ex);
				}
				throw ex;
			}
		}

		@Override
		protected void postProcessTargetObject(Object targetObject) {
			injectNotificationPublisherIfNecessary(targetObject, this.modelMBean, this.objectName);
		}
	}

}
