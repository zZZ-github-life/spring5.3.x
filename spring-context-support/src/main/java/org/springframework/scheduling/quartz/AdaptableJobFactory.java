/*
 * Copyright 2002-2018 the original author or authors.
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

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.util.ReflectionUtils;

/**
 * {@link JobFactory} 实现，支持 {@link java.lang.Runnable}
 * 对象以及标准的 Quartz {@link org.quartz.Job} 实例。
 *
 * <p>从 Spring 4.1 开始，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DelegatingJob
 * @see #adaptJob(Object)
 */
public class AdaptableJobFactory implements JobFactory {

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		try {
			Object jobObject = createJobInstance(bundle);
			return adaptJob(jobObject);
		}
		catch (Throwable ex) {
			throw new SchedulerException("Job instantiation failed", ex);
		}
	}

	/**
	 * 创建指定 job 类的实例。
	 * <p>可以重写以对 job 实例进行后处理。
	 * @param bundle the TriggerFiredBundle from which the JobDetail
	 * and other info relating to the trigger firing can be obtained
	 * @return the job instance
	 * @throws Exception if job instantiation failed
	 */
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Class<?> jobClass = bundle.getJobDetail().getJobClass();
		return ReflectionUtils.accessibleConstructor(jobClass).newInstance();
	}

	/**
	 * 将给定的 job 对象适配为 Quartz Job 接口。
	 * <p>默认实现直接支持 Quartz Job
	 * 以及 Runnable，后者会被包装在 DelegatingJob 中。
	 * @param jobObject the original instance of the specified job class
	 * @return the adapted Quartz Job instance
	 * @throws Exception if the given job could not be adapted
	 * @see DelegatingJob
	 */
	protected Job adaptJob(Object jobObject) throws Exception {
		if (jobObject instanceof Job) {
			return (Job) jobObject;
		}
		else if (jobObject instanceof Runnable) {
			return new DelegatingJob((Runnable) jobObject);
		}
		else {
			throw new IllegalArgumentException(
					"Unable to execute job class [" + jobObject.getClass().getName() +
					"]: only [org.quartz.Job] and [java.lang.Runnable] supported.");
		}
	}

}
