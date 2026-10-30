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

package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.RemoteScheduler;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link FactoryBean}，创建并配置 Quartz {@link org.quartz.Scheduler}，
 * 作为 Spring 应用上下文的一部分管理其生命周期，
 * 并将 Scheduler 作为 bean 引用暴露以供依赖注入。
 *
 * <p>允许注册 JobDetail、Calendar 和 Trigger，在初始化时
 * 自动启动调度器，在销毁时关闭。
 * 在只需要在启动时静态注册 job 的场景中，
 * 应用代码无需访问 Scheduler 实例本身。
 *
 * <p>对于运行时的动态 job 注册，使用此 SchedulerFactoryBean
 * 的 bean 引用以直接访问 Quartz Scheduler
 * （{@code org.quartz.Scheduler}）。这允许您创建新的 job
 * 和 trigger，以及控制和监控整个 Scheduler。
 *
 * <p>请注意，Quartz 在每次执行时都会实例化新的 Job，
 * 与 Timer 不同——Timer 使用在重复执行之间共享的
 * TimerTask 实例。只有 JobDetail 描述符是共享的。
 *
 * <p>使用持久化 job 时，强烈建议在 Spring 管理的
 * （或纯 JTA）事务中执行 Scheduler 上的所有操作。
 * 否则，数据库锁将无法正常工作，甚至可能被破坏。
 * （详见 {@link #setDataSource setDataSource} 的 javadoc。）
 *
 * <p>实现事务执行的首选方式是在业务外观层
 * 声明事务边界，这将自动应用于
 * 在这些范围内执行的 Scheduler 操作。
 * 或者，您可以为 Scheduler 本身添加事务通知。
 *
 * <p>从 Spring 4.1 开始，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 18.02.2004
 * @see #setDataSource
 * @see org.quartz.Scheduler
 * @see org.quartz.SchedulerFactory
 * @see org.quartz.impl.StdSchedulerFactory
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean
 */
public class SchedulerFactoryBean extends SchedulerAccessor implements FactoryBean<Scheduler>,
		BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle {

	/**
	 * 线程数属性。
	 */
	public static final String PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount";

	/**
	 * 默认线程数。
	 */
	public static final int DEFAULT_THREAD_COUNT = 10;


	private static final ThreadLocal<ResourceLoader> configTimeResourceLoaderHolder = new ThreadLocal<>();

	private static final ThreadLocal<Executor> configTimeTaskExecutorHolder = new ThreadLocal<>();

	private static final ThreadLocal<DataSource> configTimeDataSourceHolder = new ThreadLocal<>();

	private static final ThreadLocal<DataSource> configTimeNonTransactionalDataSourceHolder = new ThreadLocal<>();


	/**
	 * 返回当前配置的 Quartz Scheduler 的 {@link ResourceLoader}，
	 * 供 {@link ResourceLoaderClassLoadHelper} 使用。
	 * <p>此实例将在对应 Scheduler 初始化之前设置，
	 * 并在之后立即重置。因此仅在配置期间可用。
	 * @see #setApplicationContext
	 * @see ResourceLoaderClassLoadHelper
	 */
	@Nullable
	public static ResourceLoader getConfigTimeResourceLoader() {
		return configTimeResourceLoaderHolder.get();
	}

	/**
	 * 返回当前配置的 Quartz Scheduler 的 {@link Executor}，
	 * 供 {@link LocalTaskExecutorThreadPool} 使用。
	 * <p>此实例将在对应 Scheduler 初始化之前设置，
	 * 并在之后立即重置。因此仅在配置期间可用。
	 * @since 2.0
	 * @see #setTaskExecutor
	 * @see LocalTaskExecutorThreadPool
	 */
	@Nullable
	public static Executor getConfigTimeTaskExecutor() {
		return configTimeTaskExecutorHolder.get();
	}

	/**
	 * 返回当前配置的 Quartz Scheduler 的 {@link DataSource}，
	 * 供 {@link LocalDataSourceJobStore} 使用。
	 * <p>此实例将在对应 Scheduler 初始化之前设置，
	 * 并在之后立即重置。因此仅在配置期间可用。
	 * @since 1.1
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	@Nullable
	public static DataSource getConfigTimeDataSource() {
		return configTimeDataSourceHolder.get();
	}

	/**
	 * 返回当前配置的 Quartz Scheduler 的非事务性 {@link DataSource}，
	 * 供 {@link LocalDataSourceJobStore} 使用。
	 * <p>此实例将在对应 Scheduler 初始化之前设置，
	 * 并在之后立即重置。因此仅在配置期间可用。
	 * @since 1.1
	 * @see #setNonTransactionalDataSource
	 * @see LocalDataSourceJobStore
	 */
	@Nullable
	public static DataSource getConfigTimeNonTransactionalDataSource() {
		return configTimeNonTransactionalDataSourceHolder.get();
	}


	@Nullable
	private SchedulerFactory schedulerFactory;

	private Class<? extends SchedulerFactory> schedulerFactoryClass = StdSchedulerFactory.class;

	@Nullable
	private String schedulerName;

	@Nullable
	private Resource configLocation;

	@Nullable
	private Properties quartzProperties;

	@Nullable
	private Executor taskExecutor;

	@Nullable
	private DataSource dataSource;

	@Nullable
	private DataSource nonTransactionalDataSource;

	@Nullable
	private Map<String, ?> schedulerContextMap;

	@Nullable
	private String applicationContextSchedulerContextKey;

	@Nullable
	private JobFactory jobFactory;

	private boolean jobFactorySet = false;

	private boolean autoStartup = true;

	private int startupDelay = 0;

	private int phase = DEFAULT_PHASE;

	private boolean exposeSchedulerInRepository = false;

	private boolean waitForJobsToCompleteOnShutdown = false;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private Scheduler scheduler;


	/**
	 * 设置要使用的外部 Quartz {@link SchedulerFactory} 实例。
	 * <p>默认为内部 {@link StdSchedulerFactory} 实例。如果调用此方法，
	 * 它将覆盖通过 {@link #setSchedulerFactoryClass} 指定的任何类，
	 * 以及通过 {@link #setConfigLocation}、{@link #setQuartzProperties}、
	 * {@link #setTaskExecutor} 或 {@link #setDataSource} 指定的任何设置。
	 * <p><b>注意：</b>对于外部提供的 {@code SchedulerFactory} 实例，
	 * 此处 {@code SchedulerFactoryBean} 中的本地设置（如 {@link #setConfigLocation}
	 * 或 {@link #setQuartzProperties}）将被忽略，期望外部
	 * {@code SchedulerFactory} 实例自行初始化。
	 * @since 4.3.15
	 * @see #setSchedulerFactoryClass
	 */
	public void setSchedulerFactory(SchedulerFactory schedulerFactory) {
		this.schedulerFactory = schedulerFactory;
	}

	/**
	 * 设置要使用的 Quartz {@link SchedulerFactory} 实现类。
	 * <p>默认为 {@link StdSchedulerFactory} 类，从 {@code quartz.jar} 中
	 * 读取标准的 {@code quartz.properties}。要应用自定义的 Quartz 属性，
	 * 请在此本地 {@code SchedulerFactoryBean} 实例上指定
	 * {@link #setConfigLocation "configLocation"} 和/或
	 * {@link #setQuartzProperties "quartzProperties"} 等。
	 * @see org.quartz.impl.StdSchedulerFactory
	 * @see #setConfigLocation
	 * @see #setQuartzProperties
	 * @see #setTaskExecutor
	 * @see #setDataSource
	 */
	public void setSchedulerFactoryClass(Class<? extends SchedulerFactory> schedulerFactoryClass) {
		this.schedulerFactoryClass = schedulerFactoryClass;
	}

	/**
	 * 设置通过 SchedulerFactory 创建的 Scheduler 名称，
	 * 作为 {@code org.quartz.scheduler.instanceName} 属性的替代方案。
	 * <p>如果未指定，名称将从 Quartz 属性
	 * （{@code org.quartz.scheduler.instanceName}）中获取，
	 * 或作为后备从声明的 {@code SchedulerFactoryBean} bean 名称中获取。
	 * @see #setBeanName
	 * @see StdSchedulerFactory#PROP_SCHED_INSTANCE_NAME
	 * @see org.quartz.SchedulerFactory#getScheduler()
	 * @see org.quartz.SchedulerFactory#getScheduler(String)
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * 设置 Quartz 属性配置文件的位置，例如
	 * 类路径资源 "classpath:quartz.properties"。
	 * <p>注意：当所有必要属性都通过此 bean 本地指定时，
	 * 或依赖 Quartz 的默认配置时，可以省略。
	 * @see #setQuartzProperties
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置 Quartz 属性，如 "org.quartz.threadPool.class"。
	 * <p>可用于覆盖 Quartz 属性配置文件中的值，
	 * 或本地指定所有必要属性。
	 * @see #setConfigLocation
	 */
	public void setQuartzProperties(Properties quartzProperties) {
		this.quartzProperties = quartzProperties;
	}

	/**
	 * 设置 Spring 管理的 {@link Executor} 用作 Quartz 后端。
	 * 通过 Quartz SPI 暴露为线程池。
	 * <p>可用于将本地 JDK ThreadPoolExecutor 或 CommonJ
	 * WorkManager 指定为 Quartz 后端，以避免 Quartz 的手动线程创建。
	 * <p>默认情况下，将使用通过相应 Quartz 属性配置的
	 * Quartz SimpleThreadPool。
	 * @since 2.0
	 * @see #setQuartzProperties
	 * @see LocalTaskExecutorThreadPool
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
	 * @see org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 设置供 Scheduler 使用的默认 {@link DataSource}。
	 * <p>注意：如果设置了此项，Quartz 设置中不应定义
	 * job store 的 "dataSource"，以避免无意义的双重配置。
	 * 同时，也不要定义 "org.quartz.jobStore.class" 属性。
	 * （您可以显式定义 Spring 的 {@link LocalDataSourceJobStore}，
	 * 但使用此方法时默认就是它。）
	 * <p>将使用 Quartz JobStoreCMT 的 Spring 特定子类。
	 * 因此，强烈建议所有 Scheduler 操作都在
	 * Spring 管理的（或纯 JTA）事务中执行。
	 * 否则，数据库锁将无法正常工作，甚至可能被破坏
	 * （例如，如果在没有事务的情况下尝试在 Oracle 上获取锁）。
	 * <p>支持事务性和非事务性的 DataSource 访问。
	 * 使用非 XA DataSource 和本地 Spring 事务时，单个 DataSource
	 * 参数即可满足需求。对于 XA DataSource 和全局 JTA 事务，
	 * 应设置 SchedulerFactoryBean 的 "nonTransactionalDataSource" 属性，
	 * 传入一个不参与全局事务的非 XA DataSource。
	 * @since 1.1
	 * @see #setNonTransactionalDataSource
	 * @see #setQuartzProperties
	 * @see #setTransactionManager
	 * @see LocalDataSourceJobStore
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置用于<i>非事务性访问</i>的 {@link DataSource}。
	 * <p>仅在默认 DataSource 是 XA DataSource（始终参与事务）
	 * 时才需要此设置：在这种情况下，应将该 DataSource 的
	 * 非 XA 版本指定为 "nonTransactionalDataSource"。
	 * <p>对于本地 DataSource 实例和 Spring 事务，这并不相关。
	 * 在那里指定单个默认 DataSource 作为 "dataSource" 即可。
	 * @since 1.1
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	public void setNonTransactionalDataSource(DataSource nonTransactionalDataSource) {
		this.nonTransactionalDataSource = nonTransactionalDataSource;
	}

	/**
	 * 通过给定的 Map 在 Scheduler 上下文中注册对象。
	 * 这些对象将对在此 Scheduler 中运行的任何 Job 可用。
	 * <p>注意：当使用持久化 Job（其 JobDetail 将保存在
	 * 数据库中）时，不要将 Spring 管理的 bean 或
	 * ApplicationContext 引用放入 JobDataMap 中，
	 * 而应放入 SchedulerContext 中。
	 * @param schedulerContextAsMap 以 String 为键、任意对象为值的 Map（例如由 Spring 管理的 Bean）
	 * @see JobDetailFactoryBean#setJobDataAsMap
	 */
	public void setSchedulerContextAsMap(Map<String, ?> schedulerContextAsMap) {
		this.schedulerContextMap = schedulerContextAsMap;
	}

	/**
	 * 设置要在 SchedulerContext 中暴露的 {@link ApplicationContext}
	 * 引用的键，例如 "applicationContext"。默认为无。
	 * 仅在 Spring ApplicationContext 中运行时适用。
	 * <p>注意：当使用持久化 Job（其 JobDetail 将保存在
	 * 数据库中）时，不要将 ApplicationContext 引用放入
	 * JobDataMap 中，而应放入 SchedulerContext 中。
	 * <p>对于 QuartzJobBean，该引用将作为 bean 属性
	 * 应用到 Job 实例。在此场景下，"applicationContext" 属性
	 * 将对应 "setApplicationContext" 方法。
	 * <p>请注意，BeanFactory 回调接口（如 ApplicationContextAware）
	 * 不会自动应用到 Quartz Job 实例，因为 Quartz
	 * 本身负责其 Job 的生命周期。
	 * @see JobDetailFactoryBean#setApplicationContextJobDataKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextSchedulerContextKey(String applicationContextSchedulerContextKey) {
		this.applicationContextSchedulerContextKey = applicationContextSchedulerContextKey;
	}

	/**
	 * 设置此 Scheduler 使用的 Quartz {@link JobFactory}。
	 * <p>默认为 Spring 的 {@link AdaptableJobFactory}，支持
	 * {@link java.lang.Runnable} 对象以及标准的 Quartz
	 * {@link org.quartz.Job} 实例。请注意，此默认设置仅适用于
	 * <i>本地</i> Scheduler，不适用于 RemoteScheduler
	 * （Quartz 不支持在 RemoteScheduler 上设置自定义 JobFactory）。
	 * <p>指定 Spring 的 {@link SpringBeanJobFactory} 实例
	 * （通常作为内部 bean 定义）以自动从指定的
	 * job 数据映射和调度器上下文中填充 job 的 bean 属性。
	 * @since 2.0
	 * @see AdaptableJobFactory
	 * @see SpringBeanJobFactory
	 */
	public void setJobFactory(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
		this.jobFactorySet = true;
	}

	/**
	 * 设置是否在初始化后自动启动调度器。
	 * <p>默认为 "true"；设置为 "false" 以允许手动启动。
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回此调度器是否配置为自动启动。如果为 "true"，
	 * 调度器将在上下文刷新后以及开始延迟（如果有）之后启动。
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定此调度器应在哪个阶段启动和停止。
	 * 启动顺序从低到高，关闭顺序则相反。
	 * 默认值为 {@code Integer.MAX_VALUE}，表示此调度器
	 * 尽可能晚启动，尽可能早停止。
	 * @since 3.0
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此调度器将在哪个阶段启动和停止。
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * 设置在初始化后异步启动调度器之前
	 * 等待的秒数。默认为 0，即在初始化此 bean 时
	 * 立即同步启动。
	 * <p>如果不应在整个应用程序启动之前运行任何 job，
	 * 将此设置为 10 或 20 秒是有意义的。
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * 设置是否在 Quartz {@link SchedulerRepository} 中暴露
	 * Spring 管理的 {@link Scheduler} 实例。默认为 "false"，
	 * 因为 Spring 管理的 Scheduler 通常专门用于
	 * 在 Spring 上下文内访问。
	 * <p>将此标志切换为 "true" 以全局暴露 Scheduler。
	 * 除非您有一个依赖此行为的现有 Spring 应用程序，
	 * 否则不推荐这样做。请注意，这种全局暴露在早期
	 * Spring 版本中是意外的默认行为；从 Spring 2.5.6 开始已修复。
	 */
	public void setExposeSchedulerInRepository(boolean exposeSchedulerInRepository) {
		this.exposeSchedulerInRepository = exposeSchedulerInRepository;
	}

	/**
	 * 设置是否在关闭时等待运行中的 job 完成。
	 * <p>默认为 "false"。如果您更倾向于
	 * 完全完成的 job 而接受更长的关闭阶段，请切换为 "true"。
	 * @see org.quartz.Scheduler#shutdown(boolean)
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	//---------------------------------------------------------------------
	// Implementation of InitializingBean interface
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.dataSource == null && this.nonTransactionalDataSource != null) {
			this.dataSource = this.nonTransactionalDataSource;
		}

		if (this.applicationContext != null && this.resourceLoader == null) {
			this.resourceLoader = this.applicationContext;
		}

		// Initialize the Scheduler instance...
		this.scheduler = prepareScheduler(prepareSchedulerFactory());
		try {
			registerListeners();
			registerJobsAndTriggers();
		}
		catch (Exception ex) {
			try {
				this.scheduler.shutdown(true);
			}
			catch (Exception ex2) {
				logger.debug("Scheduler shutdown exception after registration failure", ex2);
			}
			throw ex;
		}
	}


	/**
	 * 如有必要，创建 SchedulerFactory 并将本地定义的 Quartz 属性应用于其中。
	 * @return 已初始化的 SchedulerFactory
	 */
	private SchedulerFactory prepareSchedulerFactory() throws SchedulerException, IOException {
		SchedulerFactory schedulerFactory = this.schedulerFactory;
		if (schedulerFactory == null) {
			// Create local SchedulerFactory instance (typically a StdSchedulerFactory)
			schedulerFactory = BeanUtils.instantiateClass(this.schedulerFactoryClass);
			if (schedulerFactory instanceof StdSchedulerFactory) {
				initSchedulerFactory((StdSchedulerFactory) schedulerFactory);
			}
			else if (this.configLocation != null || this.quartzProperties != null ||
					this.taskExecutor != null || this.dataSource != null) {
				throw new IllegalArgumentException(
						"StdSchedulerFactory required for applying Quartz properties: " + schedulerFactory);
			}
			// Otherwise, no local settings to be applied via StdSchedulerFactory.initialize(Properties)
		}
		// Otherwise, assume that externally provided factory has been initialized with appropriate settings
		return schedulerFactory;
	}

	/**
	 * 初始化给定的 SchedulerFactory，将本地定义的 Quartz 属性应用于其中。
	 * @param schedulerFactory 要初始化的 SchedulerFactory
	 */
	private void initSchedulerFactory(StdSchedulerFactory schedulerFactory) throws SchedulerException, IOException {
		Properties mergedProps = new Properties();
		if (this.resourceLoader != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS,
					ResourceLoaderClassLoadHelper.class.getName());
		}

		if (this.taskExecutor != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS,
					LocalTaskExecutorThreadPool.class.getName());
		}
		else {
			// Set necessary default properties here, as Quartz will not apply
			// its default configuration when explicitly given properties.
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
			mergedProps.setProperty(PROP_THREAD_COUNT, Integer.toString(DEFAULT_THREAD_COUNT));
		}

		if (this.configLocation != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading Quartz config from [" + this.configLocation + "]");
			}
			PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
		}

		CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);
		if (this.dataSource != null) {
			mergedProps.putIfAbsent(StdSchedulerFactory.PROP_JOB_STORE_CLASS, LocalDataSourceJobStore.class.getName());
		}

		// Determine scheduler name across local settings and Quartz properties...
		if (this.schedulerName != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.schedulerName);
		}
		else {
			String nameProp = mergedProps.getProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME);
			if (nameProp != null) {
				this.schedulerName = nameProp;
			}
			else if (this.beanName != null) {
				mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.beanName);
				this.schedulerName = this.beanName;
			}
		}

		schedulerFactory.initialize(mergedProps);
	}

	private Scheduler prepareScheduler(SchedulerFactory schedulerFactory) throws SchedulerException {
		if (this.resourceLoader != null) {
			// Make given ResourceLoader available for SchedulerFactory configuration.
			configTimeResourceLoaderHolder.set(this.resourceLoader);
		}
		if (this.taskExecutor != null) {
			// Make given TaskExecutor available for SchedulerFactory configuration.
			configTimeTaskExecutorHolder.set(this.taskExecutor);
		}
		if (this.dataSource != null) {
			// Make given DataSource available for SchedulerFactory configuration.
			configTimeDataSourceHolder.set(this.dataSource);
		}
		if (this.nonTransactionalDataSource != null) {
			// Make given non-transactional DataSource available for SchedulerFactory configuration.
			configTimeNonTransactionalDataSourceHolder.set(this.nonTransactionalDataSource);
		}

		// Get Scheduler instance from SchedulerFactory.
		try {
			Scheduler scheduler = createScheduler(schedulerFactory, this.schedulerName);
			populateSchedulerContext(scheduler);

			if (!this.jobFactorySet && !(scheduler instanceof RemoteScheduler)) {
				// Use AdaptableJobFactory as default for a local Scheduler, unless when
				// explicitly given a null value through the "jobFactory" bean property.
				this.jobFactory = new AdaptableJobFactory();
			}
			if (this.jobFactory != null) {
				if (this.applicationContext != null && this.jobFactory instanceof ApplicationContextAware) {
					((ApplicationContextAware) this.jobFactory).setApplicationContext(this.applicationContext);
				}
				if (this.jobFactory instanceof SchedulerContextAware) {
					((SchedulerContextAware) this.jobFactory).setSchedulerContext(scheduler.getContext());
				}
				scheduler.setJobFactory(this.jobFactory);
			}
			return scheduler;
		}

		finally {
			if (this.resourceLoader != null) {
				configTimeResourceLoaderHolder.remove();
			}
			if (this.taskExecutor != null) {
				configTimeTaskExecutorHolder.remove();
			}
			if (this.dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
			if (this.nonTransactionalDataSource != null) {
				configTimeNonTransactionalDataSourceHolder.remove();
			}
		}
	}

	/**
	 * 为给定的工厂和调度器名称创建 Scheduler 实例。
	 * 由 {@link #afterPropertiesSet} 调用。
	 * <p>默认实现调用 SchedulerFactory 的 {@code getScheduler}
	 * 方法。可以重写以自定义创建 Scheduler。
	 * @param schedulerFactory 用于创建 Scheduler 的工厂
	 * @param schedulerName 要创建的调度器名称
	 * @return Scheduler 实例
	 * @throws SchedulerException 由 Quartz 方法抛出时
	 * @see #afterPropertiesSet
	 * @see org.quartz.SchedulerFactory#getScheduler
	 */
	protected Scheduler createScheduler(SchedulerFactory schedulerFactory, @Nullable String schedulerName)
			throws SchedulerException {

		// Override thread context ClassLoader to work around naive Quartz ClassLoadHelper loading.
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader = (this.resourceLoader != null &&
				this.resourceLoader.getClassLoader() != threadContextClassLoader);
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.resourceLoader.getClassLoader());
		}
		try {
			SchedulerRepository repository = SchedulerRepository.getInstance();
			synchronized (repository) {
				Scheduler existingScheduler = (schedulerName != null ? repository.lookup(schedulerName) : null);
				Scheduler newScheduler = schedulerFactory.getScheduler();
				if (newScheduler == existingScheduler) {
					throw new IllegalStateException("Active Scheduler of name '" + schedulerName + "' already registered " +
							"in Quartz SchedulerRepository. Cannot create a new Spring-managed Scheduler of the same name!");
				}
				if (!this.exposeSchedulerInRepository) {
					// Need to remove it in this case, since Quartz shares the Scheduler instance by default!
					SchedulerRepository.getInstance().remove(newScheduler.getSchedulerName());
				}
				return newScheduler;
			}
		}
		finally {
			if (overrideClassLoader) {
				// Reset original thread context ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

	/**
	 * 在 Quartz SchedulerContext 中暴露指定的上下文属性和/或
	 * 当前 ApplicationContext。
	 */
	private void populateSchedulerContext(Scheduler scheduler) throws SchedulerException {
		// Put specified objects into Scheduler context.
		if (this.schedulerContextMap != null) {
			scheduler.getContext().putAll(this.schedulerContextMap);
		}

		// Register ApplicationContext in Scheduler context.
		if (this.applicationContextSchedulerContextKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
					"SchedulerFactoryBean needs to be set up in an ApplicationContext " +
					"to be able to handle an 'applicationContextSchedulerContextKey'");
			}
			scheduler.getContext().put(this.applicationContextSchedulerContextKey, this.applicationContext);
		}
	}


	/**
	 * 启动 Quartz Scheduler，遵循 "startupDelay" 设置。
	 * @param scheduler 要启动的 Scheduler
	 * @param startupDelay 异步启动 Scheduler 之前等待的秒数
	 */
	protected void startScheduler(final Scheduler scheduler, final int startupDelay) throws SchedulerException {
		if (startupDelay <= 0) {
			logger.info("Starting Quartz Scheduler now");
			scheduler.start();
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Will start Quartz Scheduler [" + scheduler.getSchedulerName() +
						"] in " + startupDelay + " seconds");
			}
			// Not using the Quartz startDelayed method since we explicitly want a daemon
			// thread here, not keeping the JVM alive in case of all other threads ending.
			Thread schedulerThread = new Thread() {
				@Override
				public void run() {
					try {
						TimeUnit.SECONDS.sleep(startupDelay);
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						// simply proceed
					}
					if (logger.isInfoEnabled()) {
						logger.info("Starting Quartz Scheduler now, after delay of " + startupDelay + " seconds");
					}
					try {
						scheduler.start();
					}
					catch (SchedulerException ex) {
						throw new SchedulingException("Could not start Quartz Scheduler after delay", ex);
					}
				}
			};
			schedulerThread.setName("Quartz Scheduler [" + scheduler.getSchedulerName() + "]");
			schedulerThread.setDaemon(true);
			schedulerThread.start();
		}
	}


	//---------------------------------------------------------------------
	// Implementation of FactoryBean interface
	//---------------------------------------------------------------------

	@Override
	public Scheduler getScheduler() {
		Assert.state(this.scheduler != null, "No Scheduler set");
		return this.scheduler;
	}

	@Override
	@Nullable
	public Scheduler getObject() {
		return this.scheduler;
	}

	@Override
	public Class<? extends Scheduler> getObjectType() {
		return (this.scheduler != null ? this.scheduler.getClass() : Scheduler.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// Implementation of SmartLifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				startScheduler(this.scheduler, this.startupDelay);
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not start Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				this.scheduler.standby();
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not stop Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public boolean isRunning() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				return !this.scheduler.isInStandbyMode();
			}
			catch (SchedulerException ex) {
				return false;
			}
		}
		return false;
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * 在 bean 工厂关闭时关闭 Quartz 调度器，
	 * 停止所有已调度的 job。
	 */
	@Override
	public void destroy() throws SchedulerException {
		if (this.scheduler != null) {
			logger.info("Shutting down Quartz Scheduler");
			this.scheduler.shutdown(this.waitForJobsToCompleteOnShutdown);
		}
	}

}
