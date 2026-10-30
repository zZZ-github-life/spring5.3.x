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

package org.springframework.scheduling.quartz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * 访问 Quartz Scheduler 的通用基类，即在
 * {@link org.quartz.Scheduler} 实例上注册 job、trigger 和 listener。
 *
 * <p>有关具体用法，请查看 {@link SchedulerFactoryBean} 和
 * {@link SchedulerAccessorBean} 类。
 *
 * <p>从 Spring 4.1 开始，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5.6
 */
public abstract class SchedulerAccessor implements ResourceLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean overwriteExistingJobs = false;

	@Nullable
	private String[] jobSchedulingDataLocations;

	@Nullable
	private List<JobDetail> jobDetails;

	@Nullable
	private Map<String, Calendar> calendars;

	@Nullable
	private List<Trigger> triggers;

	@Nullable
	private SchedulerListener[] schedulerListeners;

	@Nullable
	private JobListener[] globalJobListeners;

	@Nullable
	private TriggerListener[] globalTriggerListeners;

	@Nullable
	private PlatformTransactionManager transactionManager;

	@Nullable
	protected ResourceLoader resourceLoader;


	/**
	 * 设置此 SchedulerFactoryBean 上定义的任何 job 是否应覆盖
	 * 现有的 job 定义。默认为 "false"，不覆盖已从
	 * 持久化 job store 读取的已注册 job。
	 */
	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	/**
	 * 设置遵循 "job_scheduling_data_1_5" XSD 或更高版本的
	 * Quartz job 定义 XML 文件的位置。可以指定用于自动
	 * 注册在此类文件中定义的 job，可能除了
	 * 直接在此 SchedulerFactoryBean 上定义的 job 之外。
	 * @see org.quartz.xml.XMLSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocation(String jobSchedulingDataLocation) {
		this.jobSchedulingDataLocations = new String[] {jobSchedulingDataLocation};
	}

	/**
	 * 设置遵循 "job_scheduling_data_1_5" XSD 或更高版本的
	 * Quartz job 定义 XML 文件的位置列表。可以指定用于自动
	 * 注册在此类文件中定义的 job，可能除了
	 * 直接在此 SchedulerFactoryBean 上定义的 job 之外。
	 * @see org.quartz.xml.XMLSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocations(String... jobSchedulingDataLocations) {
		this.jobSchedulingDataLocations = jobSchedulingDataLocations;
	}

	/**
	 * 向此 FactoryBean 创建的 Scheduler 注册
	 * JobDetail 对象列表，供 Trigger 引用。
	 * <p>当 Trigger 本身确定 JobDetail 时，不需要此设置：
	 * 在这种情况下，JobDetail 会与 Trigger 一起隐式注册。
	 * @see #setTriggers
	 * @see org.quartz.JobDetail
	 */
	public void setJobDetails(JobDetail... jobDetails) {
		// Use modifiable ArrayList here, to allow for further adding of
		// JobDetail objects during autodetection of JobDetail-aware Triggers.
		this.jobDetails = new ArrayList<>(Arrays.asList(jobDetails));
	}

	/**
	 * 向此 FactoryBean 创建的 Scheduler 注册
	 * Quartz Calendar 对象列表，供 Trigger 引用。
	 * @param calendars 以日历名称为键、Calendar 对象为值的 Map
	 * @see org.quartz.Calendar
	 */
	public void setCalendars(Map<String, Calendar> calendars) {
		this.calendars = calendars;
	}

	/**
	 * 向此 FactoryBean 创建的 Scheduler 注册
	 * Trigger 对象列表。
	 * <p>如果 Trigger 本身确定对应的 JobDetail，
	 * 则该 job 会自动注册到 Scheduler 中。
	 * 否则，相应的 JobDetail 需要通过此 FactoryBean 的
	 * "jobDetails" 属性进行注册。
	 * @see #setJobDetails
	 * @see org.quartz.JobDetail
	 */
	public void setTriggers(Trigger... triggers) {
		this.triggers = Arrays.asList(triggers);
	}

	/**
	 * 指定要与 Scheduler 注册的 Quartz SchedulerListener。
	 */
	public void setSchedulerListeners(SchedulerListener... schedulerListeners) {
		this.schedulerListeners = schedulerListeners;
	}

	/**
	 * 指定要与 Scheduler 注册的全局 Quartz JobListener。
	 * 此类 JobListener 将应用于 Scheduler 中的所有 Job。
	 */
	public void setGlobalJobListeners(JobListener... globalJobListeners) {
		this.globalJobListeners = globalJobListeners;
	}

	/**
	 * 指定要与 Scheduler 注册的全局 Quartz TriggerListener。
	 * 此类 TriggerListener 将应用于 Scheduler 中的所有 Trigger。
	 */
	public void setGlobalTriggerListeners(TriggerListener... globalTriggerListeners) {
		this.globalTriggerListeners = globalTriggerListeners;
	}

	/**
	 * 设置用于注册此 SchedulerFactoryBean 定义的
	 * job 和 trigger 的事务管理器。默认为无；
	 * 只有在为 Scheduler 指定了 DataSource 时设置此选项才有意义。
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	/**
	 * 注册 job 和 trigger（尽可能在事务中）。
	 */
	protected void registerJobsAndTriggers() throws SchedulerException {
		TransactionStatus transactionStatus = null;
		if (this.transactionManager != null) {
			transactionStatus = this.transactionManager.getTransaction(TransactionDefinition.withDefaults());
		}

		try {
			if (this.jobSchedulingDataLocations != null) {
				ClassLoadHelper clh = new ResourceLoaderClassLoadHelper(this.resourceLoader);
				clh.initialize();
				XMLSchedulingDataProcessor dataProcessor = new XMLSchedulingDataProcessor(clh);
				for (String location : this.jobSchedulingDataLocations) {
					dataProcessor.processFileAndScheduleJobs(location, getScheduler());
				}
			}

			// Register JobDetails.
			if (this.jobDetails != null) {
				for (JobDetail jobDetail : this.jobDetails) {
					addJobToScheduler(jobDetail);
				}
			}
			else {
				// Create empty list for easier checks when registering triggers.
				this.jobDetails = new ArrayList<>();
			}

			// Register Calendars.
			if (this.calendars != null) {
				for (String calendarName : this.calendars.keySet()) {
					Calendar calendar = this.calendars.get(calendarName);
					getScheduler().addCalendar(calendarName, calendar, true, true);
				}
			}

			// Register Triggers.
			if (this.triggers != null) {
				for (Trigger trigger : this.triggers) {
					addTriggerToScheduler(trigger);
				}
			}
		}

		catch (Throwable ex) {
			if (transactionStatus != null) {
				try {
					this.transactionManager.rollback(transactionStatus);
				}
				catch (TransactionException tex) {
					logger.error("Job registration exception overridden by rollback exception", ex);
					throw tex;
				}
			}
			if (ex instanceof SchedulerException) {
				throw (SchedulerException) ex;
			}
			if (ex instanceof Exception) {
				throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage(), ex);
			}
			throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage());
		}

		if (transactionStatus != null) {
			this.transactionManager.commit(transactionStatus);
		}
	}

	/**
	 * 将给定的 job 添加到 Scheduler（如果尚不存在）。
	 * 如果设置了 "overwriteExistingJobs"，则在任何情况下都会覆盖。
	 * @param jobDetail 要添加的作业
	 * @return 如果作业实际被添加，则为 {@code true}；
	 * 如果它之前已存在，则为 {@code false}
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addJobToScheduler(JobDetail jobDetail) throws SchedulerException {
		if (this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null) {
			getScheduler().addJob(jobDetail, true);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 将给定的 trigger 添加到 Scheduler（如果尚不存在）。
	 * 如果设置了 "overwriteExistingJobs"，则在任何情况下都会覆盖。
	 * @param trigger 要添加的触发器
	 * @return 如果触发器实际被添加，则为 {@code true}；
	 * 如果它之前已存在，则为 {@code false}
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addTriggerToScheduler(Trigger trigger) throws SchedulerException {
		boolean triggerExists = (getScheduler().getTrigger(trigger.getKey()) != null);
		if (triggerExists && !this.overwriteExistingJobs) {
			return false;
		}

		// Check if the Trigger is aware of an associated JobDetail.
		JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().remove("jobDetail");
		if (triggerExists) {
			if (jobDetail != null && this.jobDetails != null &&
					!this.jobDetails.contains(jobDetail) && addJobToScheduler(jobDetail)) {
				this.jobDetails.add(jobDetail);
			}
			try {
				getScheduler().rescheduleJob(trigger.getKey(), trigger);
			}
			catch (ObjectAlreadyExistsException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unexpectedly encountered existing trigger on rescheduling, assumably due to " +
							"cluster race condition: " + ex.getMessage() + " - can safely be ignored");
				}
			}
		}
		else {
			try {
				if (jobDetail != null && this.jobDetails != null && !this.jobDetails.contains(jobDetail) &&
						(this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null)) {
					getScheduler().scheduleJob(jobDetail, trigger);
					this.jobDetails.add(jobDetail);
				}
				else {
					getScheduler().scheduleJob(trigger);
				}
			}
			catch (ObjectAlreadyExistsException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unexpectedly encountered existing trigger on job scheduling, assumably due to " +
							"cluster race condition: " + ex.getMessage() + " - can safely be ignored");
				}
				if (this.overwriteExistingJobs) {
					getScheduler().rescheduleJob(trigger.getKey(), trigger);
				}
			}
		}
		return true;
	}

	/**
	 * 将所有指定的 listener 注册到 Scheduler。
	 */
	protected void registerListeners() throws SchedulerException {
		ListenerManager listenerManager = getScheduler().getListenerManager();
		if (this.schedulerListeners != null) {
			for (SchedulerListener listener : this.schedulerListeners) {
				listenerManager.addSchedulerListener(listener);
			}
		}
		if (this.globalJobListeners != null) {
			for (JobListener listener : this.globalJobListeners) {
				listenerManager.addJobListener(listener);
			}
		}
		if (this.globalTriggerListeners != null) {
			for (TriggerListener listener : this.globalTriggerListeners) {
				listenerManager.addTriggerListener(listener);
			}
		}
	}


	/**
	 * 模板方法，确定要操作的 Scheduler。
	 * 由子类实现。
	 */
	protected abstract Scheduler getScheduler();

}
