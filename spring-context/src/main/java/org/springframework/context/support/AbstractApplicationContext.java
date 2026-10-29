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

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

/**
 * {@link org.springframework.context.ApplicationContext} 接口的抽象实现。不强制规定用于配置的存储
 * 类型；只实现通用的上下文功能。采用模板方法设计模式，要求具体子类实现抽象方法。
 *
 * <p>与普通 BeanFactory 不同，ApplicationContext 应当检测其内部 Bean 工厂中定义的特定 Bean：因此，
 * 本类会自动注册在上下文中定义为 Bean 的
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}、
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors} 和
 * {@link org.springframework.context.ApplicationListener ApplicationListeners}。
 *
 * <p>还可以在上下文中以名称为 "messageSource" 的 Bean 形式提供
 * {@link org.springframework.context.MessageSource}；否则，消息解析会委托给父上下文。此外，可以在
 * 上下文中以类型为 {@link org.springframework.context.event.ApplicationEventMulticaster} 的
 * "applicationEventMulticaster" Bean 形式提供应用事件多播器；否则将使用类型为
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} 的默认多播器。
 *
 * <p>通过扩展 {@link org.springframework.core.io.DefaultResourceLoader} 实现资源加载。因此，除非在
 * 子类中重写了 {@link #getResourceByPath} 方法，否则会将非 URL 资源路径视为类路径资源（支持包含包
 * 路径的完整类路径资源名，例如 "mypackage/myresource.dat"）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * 工厂中 MessageSource Bean 的名称。如果未提供，则消息解析委托给父级。
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * 工厂中 LifecycleProcessor Bean 的名称。如果未提供，则使用 DefaultLifecycleProcessor。
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * 工厂中 ApplicationEventMulticaster Bean 的名称。如果未提供，则使用默认的
	 * SimpleApplicationEventMulticaster。
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * 由 {@code spring.spel.ignore} 系统属性控制的布尔标志，指示 Spring 忽略 SpEL，即不初始化 SpEL 基
	 * 础设施。
	 *
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/** 此类使用的日志记录器。可供子类使用。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 此上下文的唯一 id（如果有）。 */
	private String id = ObjectUtils.identityToString(this);

	/** 显示名称。 */
	private String displayName = ObjectUtils.identityToString(this);

	/** 父上下文。 */
	@Nullable
	private ApplicationContext parent;

	/** 此上下文使用的 Environment。 */
	@Nullable
	private ConfigurableEnvironment environment;

	/** 刷新时要应用的 BeanFactoryPostProcessor。 */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	/** 此上下文启动时的系统时间（毫秒）。 */
	private long startupDate;

	/** 指示此上下文当前是否处于活动状态的标志。 */
	private final AtomicBoolean active = new AtomicBoolean();

	/** 指示此上下文是否已关闭的标志。 */
	private final AtomicBoolean closed = new AtomicBoolean();

	/** 用于 "refresh" 和 "destroy" 的同步监视器。 */
	private final Object startupShutdownMonitor = new Object();

	/** 对 JVM 关闭钩子的引用（如果已注册）。 */
	@Nullable
	private Thread shutdownHook;

	/** 此上下文使用的 ResourcePatternResolver。 */
	private ResourcePatternResolver resourcePatternResolver;

	/** 用于管理此上下文中 bean 生命周期的 LifecycleProcessor。 */
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	/** 我们将此接口的实现委托给它的 MessageSource。 */
	@Nullable
	private MessageSource messageSource;

	/** 事件发布中使用的辅助类。 */
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** 应用启动指标。 */
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/** 静态指定的监听器。 */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

	/** 在 refresh 之前注册的本地监听器。 */
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/** 在多播器设置之前发布的 ApplicationEvent。 */
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * 创建一个没有父上下文的新 AbstractApplicationContext。
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * 使用给定的父上下文创建一个新的 AbstractApplicationContext。
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * 设置此应用上下文的唯一 id。
	 *
	 * <p>默认值为上下文实例的对象 id；如果上下文本身被定义为 Bean，则为该上下文 Bean 的名称。
	 * @param id the unique id of the context
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * 为此上下文设置一个友好名称。通常在具体上下文实现初始化期间完成。
	 *
	 * <p>默认值为上下文实例的对象 id。
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * 返回此上下文的友好名称。
	 * @return a display name for this context (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * 返回父上下文；如果没有父上下文（即此上下文是上下文层次的根），则返回 {@code null}。
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * 为此应用上下文设置 {@code Environment}。
	 *
	 * <p>默认值由 {@link #createEnvironment()} 确定。用此方法替换默认值是一种选择，但也可以考虑通过
	 * {@link #getEnvironment()} 进行配置。无论采用哪种方式，此类修改都应在 {@link #refresh()} <em>之前
	 * </em>完成。
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * 以可配置的形式返回此应用上下文的 {@code Environment}，以便进一步定制。
	 *
	 * <p>如果未指定，将通过 {@link #createEnvironment()} 初始化默认环境。
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回一个新的 {@link StandardEnvironment}。
	 *
	 * <p>子类可以重写此方法，以提供自定义的 {@link ConfigurableEnvironment} 实现。
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * 如果已经可用，则将此上下文的内部 Bean 工厂作为 AutowireCapableBeanFactory 返回。
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * 返回此上下文首次加载时的时间戳（毫秒）。
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * 将给定事件发布给所有监听器。
	 *
	 * <p>注意：监听器在 MessageSource 之后初始化，以便能在监听器实现中访问它。因此，MessageSource 实现
	 * 无法发布事件。
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * 将给定事件发布给所有监听器。
	 *
	 * <p>注意：监听器在 MessageSource 之后初始化，以便能在监听器实现中访问它。因此，MessageSource 实现
	 * 无法发布事件。
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * 将给定事件发布给所有监听器。
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 * @param eventType the resolved event type, if known
	 * @since 4.2
	 */
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// Decorate event as an ApplicationEvent if necessary
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		}
		else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// Multicast right now if possible - or lazily once the multicaster is initialized
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// Publish event via parent context as well...
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * 返回上下文使用的内部 ApplicationEventMulticaster。
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * 返回上下文使用的内部 LifecycleProcessor。
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * 返回用于将位置模式解析为 Resource 实例的 ResourcePatternResolver。默认是
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}，支持 Ant 风格的
	 * 位置模式。
	 *
	 * <p>可以在子类中重写，以使用扩展的解析策略，例如在 Web 环境中。
	 *
	 * <p><b>需要解析位置模式时不要调用此方法。</b>而应调用上下文的 {@code getResources} 方法，它会委托
	 * 给 ResourcePatternResolver。
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * 设置此应用上下文的父上下文。
	 *
	 * <p>如果父上下文非 {@code null} 且其环境是 {@link ConfigurableEnvironment} 的实例，则父
	 * {@linkplain ApplicationContext#getEnvironment() environment} 会与此（子）应用上下文的环境进行
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged}。
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * 返回将应用到内部 BeanFactory 的 BeanFactoryPostProcessor 列表。
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * 返回静态指定的 ApplicationListener 列表。
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			/*刷新之前的准备动作。
				1.记录当前事件
				2.将容器变为激活状态
				3.初始化程序员实现的自己定义的一些资源
				4.验证程序设定的所有必须的属性
				5.初始化早期的事件监听器集合，早期的应用事件集合
				*/
			prepareRefresh();

			/*获取容器的bean工厂
			*
			* 	刷新前尝试用cas的方式加锁
			* 	加锁成功，准备刷新spring容器。
			* 	然后获取获取容器的bean工厂（工厂实例为DefaultListableBeanFactory），设置该工厂的id
			* */
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			/*给工厂添加一些配置：
			* 	1.添加类加载器
			* 	2.添加默认的表达式解析器（如果有），属性编辑注册器
			* 	3.添加两个bean的后置处理器（BeanPostProcessor）。’ApplicationContextAwareProcessor‘， ‘ApplicationListenerDetector’
			* 	3.处理spring的默认主要接口的默认实现
			* 	4.向容器的单例池中添加需要的4个单例bean
			* 	5.忽略默认依赖检查
			* */
			prepareBeanFactory(beanFactory);

			try {

				/*空壳方法， 让用户自己实现此方法。
				*
				* 	目的是在spring执行所有的后置处理器之前，留给用户对bean工厂做一些操作。如，添加自己的后置处理器
				* 	此时还未开始调用bean工厂的后置处理器。bean还未扫描
				* */
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");

				// Invoke factory processors registered as beans in the context.
				/*
				* 开始调用bean工厂的后置处理。这里会执行spring两个内置的后置处理器‘ConfigurationClassPostProcessor’，‘EventListenerMethodProcessor’
				*
				* 	ConfigurationClassPostProcessor
				* 		1.这个处理会扫描并解析我们配置的bean，并封装成BeanDefinitio注入到bean工厂
				* 		2.执行我们自定义的后置处理器（实现了这两个接口的BeanDefinitionRegistryPostProcessor，BeanFactoryPostProcessor）
				*		3.将配置文件类使用cglib生成代理类，并增强
				*
				* 	EventListenerMethodProcessor
				* 		1.收集bean工厂的EventListenerFactory接口，并添加到该处理器内部
				* */
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				/*
				* 注册bean的后置处理器（该处理器的作用是在bean实例化前后提供处理方法）
				* 	1.将所有实现了BeanPostProcessor的bean注册到bean工厂
				* 	2.调整ApplicationListenerDetector处理器到链表的最后
				* */
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

				/*
				* 初始化MessageSource，spring支持国际化信息：
				* 	1.向容器和单例池中注册messageSource实例
				*
				* note：根据客户端的语言，返回对应的信息资源。比如做web程序浏览器端用的中文，那么返回中文相关提示，如果是英文则返回英文相关提示
				* */
				initMessageSource();


				/*
				* 初始化一个默认的事件发布器。
				* 	1.向容器和bean工厂的单例池中添加applicationEventMulticaster实例
				* */
				initApplicationEventMulticaster();

				/*
				* 空壳方法，留着子类实现，在此时机提供回调。
				*
				* 	目的是在spring刷新时提供回调，让开发者做一些自己的事，如 初始化其他的Bean用的
				* 	此时，spring的扫描已经完成，但所有的单例还为开始初始化
				* */
				onRefresh();

				/*
				* 注册监听器：
				*	1.所有实现ApplicationListener接口的监听器
				* 	将监听器注册到上面的事件发布器当中
				* */
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				/*
				* 在这里完成bean工厂的初始化！
				* 		1.创建容器的单例池，在BeanDefinition中如果处于非懒加载的，单例的bean会在这里创建，这里就是spring的bean的生命周期。
				* 		2.添加字符串解析器
				* 		3.在实例化单例池之前，先初始化LoadTimeWeaverAware接口的实现类
				* */
				finishBeanFactoryInitialization(beanFactory);

				/*
				* 完成刷新。
				* 	1.发布（触发）容器刷新事件
				* 	2.清空不需要的缓存，
				* 	3.添加，调用生命周期处理器，
				* 	4.处理相应的自定义生命周期接口
				* */
				finishRefresh();
			}
			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}
			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				/*
				* 重置缓存，刷新结束
				* */
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}

	/**
	 * 为刷新准备此上下文，设置其启动日期和活动标志，并执行属性源的任何初始化。
	 *
	 * 设置容器的启动日期startDateTime
	 * 设置active标志,用于标识容器的激活状态
	 * 初始化property source abstraction,用于配置属性处理
	 * 校验必要的属性设置,如环境environment和配置文件路径
	 * 对系统属性和环境变量进行准备处理
	 * 对容器内部属性配置的子类做一些准备工作
	 * 初始化容器内部一些属性,如容器类加载器、事件处理器等
	 * 校验容器内部属性配置的子类的必要设置
	 * 存储容器启动阶段的快照,用于容器关闭时显示位标
	 * 初始化资源路径,如类路径或证书路径等
	 */
	protected void prepareRefresh() {
		//记录当前时间，并且设置AnnotationConfigApplicationContext为活动状态
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		//
		/*
		* 空壳方法，用于程序员定义子类，实现该方法，用于在容器初始化或刷新前，加载一些资源。
		*
		* */
		initPropertySources();


		// see ConfigurablePropertyResolver#setRequiredProperties
		//验证所有必须的属性是否存在，不存在则抛出异常。验证的系统启动参数，如在系统必须要存在的参数，比如你在-D参数中新增了一个参数bmlxx,那么你需要检查它必须存在
		//ac.getEnvironment().setRequiredProperties("bmlxx");
		getEnvironment().validateRequiredProperties();


		//初始化监听器，如果已有一些监听事件则添加到applicationListeners 然后清空
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>用实际实例替换任何占位属性源。
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * 指示子类刷新内部 Bean 工厂。
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		refreshBeanFactory();
		return getBeanFactory();
	}

	/**
	 * 配置工厂的标准上下文特征，例如上下文的 ClassLoader 和后处理器。
	 * @param beanFactory the BeanFactory to configure
	 *
	 * 设置BeanFactory的class loader为当前context的class loader,方便加载bean类。
	 * 设置BeanExpressionResolver,用于解析Bean中的表达式语言如#{...}。
	 * 添加PropertyEditorRegistrar实现,用于属性编辑转换。
	 * 设置BeanFactory的ResourceLoader,用于加载外部资源文件。
	 * 设置BeanFactory的ApplicationEventPublisher,用于事件发布。
	 * 注册一些内建的aware接口,如EnvironmentAware、ResourceLoaderAware等。
	 * 添加一些BeanPostProcessor实现,如ApplicationContextAwareProcessor等。
	 * 注册一些内建的scope,如"prototype"和"singleton" 作用域。
	 * 自定义一些内建bean的名称,如environment、systemProperties等。
	 * 许多子类会覆盖这个方法定制初始化逻辑。
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {

		//设置bean工厂的类加载器
		beanFactory.setBeanClassLoader(getClassLoader());

		//是否使用spring的表达式，如果使用则添加表达式解析器到bean工厂
		if (!shouldIgnoreSpel) {
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}

		//添加属性编辑注册器到bean工厂 ，注册器作用未知
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		//添加spring的后置处理器（每次添加都会在集合的末尾），此后置处理器用来实现bean实现aware接口的回调
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

		//添加需要忽略的依赖检查到HashSet集合
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		//添加到resolvableDependencies的集合当中，目的时为了解决注入时一个接口有多个实现，默认自动注入registerResolvableDependency第二个参数的实现
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		//添加spring的后置处理器
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		//给spring容器注册系统需要的一些单例bean
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}

	/**
	 * 在标准初始化之后修改应用上下文的内部 Bean 工厂。此时初始定义资源已加载，但尚未运行任何后处理器，
	 * 也未注册任何派生的 Bean 定义，最重要的是，尚未实例化任何 Bean。
	 *
	 * <p>此模板方法允许在某些 AbstractApplicationContext 子类中注册特殊的 BeanPostProcessor 等。
	 * @param beanFactory the bean factory used by the application context
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * 实例化并调用所有已注册的 BeanFactoryPostProcessor Bean，如果指定了显式顺序，则遵循该顺序。
	 *
	 * <p>必须在单例实例化之前调用。
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * 实例化并注册所有 BeanPostProcessor Bean，如果指定了显式顺序，则遵循该顺序。
	 *
	 * <p>必须在实例化任何应用 Bean 之前调用。
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * 初始化 MessageSource。如果此上下文中未定义，则使用父级的。
	 *
	 * 检查容器中是否已经存在名为 "messageSource" 的Bean。如果已经存在，将其作为消息源。这意味着您可以在Spring配置中自定义消息源，而不是使用默认配置。
	 *
	 * 如果容器中没有找到 "messageSource" 的Bean，那么默认情况下，Spring会创建一个 DelegatingMessageSource 类型的Bean作为消息源，并将其注册到容器中。DelegatingMessageSource 是一个复合消息源，它可以包含多个消息源，并按照优先级顺序查找消息。
	 *
	 * 设置消息源的父级消息源（Parent Message Source）。这是为了支持消息源的层次结构，允许消息的继承和覆盖。
	 *
	 * 初始化消息源的各种属性，如消息源的编码、默认语言等。
	 */
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * 初始化 ApplicationEventMulticaster。如果上下文中未定义，则使用 SimpleApplicationEventMulticaster。
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 *
	 *
	 * 检查容器中是否已经存在名为 "applicationEventMulticaster" 的Bean。如果已经存在，将其作为应用程序事件多播器。这意味着您可以在Spring配置中自定义应用程序事件多播器，而不是使用默认配置。
	 *
	 * 如果容器中没有找到 "applicationEventMulticaster" 的Bean，那么默认情况下，Spring会创建一个 SimpleApplicationEventMulticaster 类型的Bean作为应用程序事件多播器，并将其注册到容器中。SimpleApplicationEventMulticaster 是一个简单的多播器，它可以广播事件给所有注册的监听器。
	 *
	 * 初始化应用程序事件多播器的各种属性，如执行事件监听器的线程池配置、异常处理策略等。
	 */
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化 LifecycleProcessor。如果上下文中未定义，则使用 DefaultLifecycleProcessor。
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 模板方法，可重写以添加特定于上下文的刷新工作。在初始化特殊 Bean 时、实例化单例之前调用。
	 *
	 * <p>此实现为空。
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * 将实现 ApplicationListener 的 Bean 添加为监听器。不影响其他监听器，它们无需作为 Bean 也可以添加。
	 *
	 * 从容器中查找所有标注有@EventListener的Bean,注册为应用事件监听器。
	 * 如果配置了applicationListenerDetector属性,会使用该检测器进一步查找监听器。
	 * 将查找到的监听器注册到容器的事件广播器中。
	 * 如果有ApplicationEventMulticaster的bean,则将其设置为事件广播器。
	 * 最后将已注册的监听器初始化并调用contextRefreshedEvent回调通知
	 */
	protected void registerListeners() {
		// Register statically specified listeners first.
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		/*
		* 发布早期的事件
		* */
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * 完成此上下文 Bean 工厂的初始化，初始化所有剩余的单例 Bean。
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		/*
		* 查看bean工厂中是否有一个conversionService的bean，如果有就注册到bean工厂的转化服务当中
		* */
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no BeanFactoryPostProcessor
		// (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		/*
		* 如果之前没有注册过， 注册一个字符串解析器
		* */
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(new StringValueResolver() {
				@Override
				public String resolveStringValue(String strVal) {
					return AbstractApplicationContext.this.getEnvironment().resolvePlaceholders(strVal);
				}
			});
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		/*
		* 修改状态，冻结bean。所有的beanDefinition不会再被修改了，因为马上要实例化了
		* */
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		/*
		* 实例化剩下的非懒加载的单例bean
		* */
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * 完成此上下文的刷新，调用 LifecycleProcessor 的 onRefresh() 方法并发布
	 * {@link org.springframework.context.event.ContextRefreshedEvent}。
	 */
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		/*
		* 清除资源缓存。该集合中存在一个元数据读取器，存放我们扫描的bean的类相关数据，类的真实文件信息
		*
		* */
		clearResourceCaches();
		/*
		* 向容器和单例池中添加一个默认的生命周期处理器
		* */
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		/*
		* 实现所有的生命周期接口的回调。
		* 	生命周期接口的扩展，用于哪些需要在ApplicationContext刷新和启动/或按特定顺序关闭时启动的对象
		* 	保证容器在启动后调用start方法开始生命周期，并且在Spring关闭的时候调用stop方法来结束生命周期，通常用来配置后台程序，比如MQ进行轮询等
		* */
		getLifecycleProcessor().onRefresh();

		/*
		* 发布容器刷新事件：
		* 	1.用户可以自己实现监听器（ApplicationListener），监听这个事件（ContextRefreshedEvent），在容器初始化完成或刷新完成时调用。
		* 	2.可同步或异步触发
		* */
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * 在抛出异常后取消此上下文的刷新尝试，重置 {@code active} 标志。
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * 重置 Spring 的通用反射元数据缓存，特别是 {@link ReflectionUtils}、{@link AnnotationUtils}、
	 * {@link ResolvableType} 和 {@link CachedIntrospectionResults} 缓存。
	 * @since 4.2
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 */
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * 向 JVM 运行时注册一个{@linkplain Thread#getName() named}为 {@code SpringContextShutdownHook} 的
	 * 关闭钩子，在 JVM 关闭时关闭此上下文，除非届时它已经关闭。
	 *
	 * <p>实际的关闭过程委托给 {@code doClose()}。
	 * @see Runtime#addShutdownHook
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
	 * @see #close()
	 * @see #doClose()
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * 此实例销毁时的回调，最初附加到 {@code DisposableBean} 实现（在 5.0 中已不再如此）。
	 *
	 * <p>{@link #close()} 方法是关闭 ApplicationContext 的原生方式，此方法只是委托给它。
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * 关闭此应用上下文，销毁其 Bean 工厂中的所有 Bean。
	 *
	 * <p>实际的关闭过程委托给 {@code doClose()}。如果注册了 JVM 关闭钩子，也会将其移除，因为不再需要。
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * 实际执行上下文关闭：发布 ContextClosedEvent，并销毁此应用上下文 Bean 工厂中的单例。
	 *
	 * <p>由 {@code close()} 和 JVM 关闭钩子（如果有）共同调用。
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			destroyBeans();

			// Close the state of this context itself.
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			onClose();

			// Reset local application listeners to pre-refresh state.
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			this.active.set(false);
		}
	}

	/**
	 * 用于销毁此上下文管理的所有 Bean 的模板方法。默认实现会销毁此上下文中所有缓存的单例，调用
	 * {@code DisposableBean.destroy()} 和/或指定的 "destroy-method"。
	 *
	 * <p>可以重写以在标准单例销毁之前或之后立即添加上下文特定的 Bean 销毁步骤，此时上下文的
	 * BeanFactory 仍处于活动状态。
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * 模板方法，可重写以添加特定于上下文的关闭工作。默认实现为空。
	 *
	 * <p>在 {@link #doClose} 的关闭过程结束时调用，此时此上下文的 BeanFactory 已关闭。如果自定义关闭逻
	 * 辑需要在 BeanFactory 仍处于活动状态时执行，请改为重写 {@link #destroyBeans()} 方法。
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * 断言此上下文的 BeanFactory 当前处于活动状态，如果不是，则抛出 {@link IllegalStateException}。
	 *
	 * <p>由所有依赖于活动上下文的 {@link BeanFactory} 委托方法调用，尤其是所有 Bean 访问器方法。
	 *
	 * <p>默认实现检查此上下文整体的 {@link #isActive() 'active'} 状态。可以重写以进行更具体的检查，或
	 * 者在此类情况下 {@link #getBeanFactory()} 本身抛出异常时改为空操作。
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * 如果父上下文实现了 ConfigurableApplicationContext，则返回父上下文的内部 Bean 工厂；否则返回父上
	 * 下文本身。
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * 返回上下文使用的内部 MessageSource。
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * 如果父上下文的内部消息源也是 AbstractApplicationContext，则返回它；否则返回父上下文本身。
	 */
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * 子类必须实现此方法以执行实际的配置加载。此方法由 {@link #refresh()} 在完成任何其他初始化工作之前
	 * 调用。
	 *
	 * <p>子类要么创建一个新的 Bean 工厂并持有对其的引用，要么返回其持有的单个 BeanFactory 实例。在后一
	 * 种情况下，如果多次刷新上下文，通常会抛出 IllegalStateException。
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * 子类必须实现此方法以释放其内部 Bean 工厂。此方法由 {@link #close()} 在完成所有其他关闭工作之后调
	 * 用。
	 *
	 * <p>永远不应抛出异常，而应记录关闭失败。
	 */
	protected abstract void closeBeanFactory();

	/**
	 * 子类必须在此返回其内部 Bean 工厂。它们应高效地实现查找，以便可以反复调用而不会带来性能损失。
	 *
	 * <p>注意：子类在返回内部 Bean 工厂之前应检查上下文是否仍处于活动状态。一旦上下文关闭，内部工厂通
	 * 常应被视为不可用。
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * 返回有关此上下文的信息。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
