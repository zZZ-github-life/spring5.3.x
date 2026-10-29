/*
 * Copyright 2002-2014 the original author or authors.
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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Quartz Job 接口的简单实现，将传入的
 * JobDataMap 以及 SchedulerContext 作为 bean 属性值
 * 进行应用。这很合适，因为每次执行都会创建新的 Job 实例。
 * JobDataMap 条目会覆盖 SchedulerContext 中相同键的条目。
 *
 * <p>例如，假设 JobDataMap 包含键
 * "myParam"，值为 "5"：那么 Job 实现可以暴露
 * 类型为 int 的 bean 属性 "myParam" 来接收这样的值，
 * 即方法 "setMyParam(int)"。这也适用于复杂
 * 类型，如业务对象等。
 *
 * <p><b>请注意，将依赖注入应用于 Job 实例的推荐方式
 * 是通过 JobFactory：</b>即指定
 * {@link SpringBeanJobFactory} 作为 Quartz JobFactory（通常通过
 * {@link SchedulerFactoryBean#setJobFactory SchedulerFactoryBean 的 "jobFactory" 属性}）。
 * 这允许实现依赖注入的 Quartz Job，
 * 而不依赖于 Spring 基类。
 *
 * @author Juergen Hoeller
 * @since 18.02.2004
 * @see org.quartz.JobExecutionContext#getMergedJobDataMap()
 * @see org.quartz.Scheduler#getContext()
 * @see SchedulerFactoryBean#setSchedulerContextAsMap
 * @see SpringBeanJobFactory
 * @see SchedulerFactoryBean#setJobFactory
 */
public abstract class QuartzJobBean implements Job {

	/**
	 * 此实现将传入的 job 数据映射作为 bean 属性值
	 * 进行应用，然后委托给 {@code executeInternal}。
	 * @see #executeInternal
	 */
	@Override
	public final void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValues(context.getScheduler().getContext());
			pvs.addPropertyValues(context.getMergedJobDataMap());
			bw.setPropertyValues(pvs, true);
		}
		catch (SchedulerException ex) {
			throw new JobExecutionException(ex);
		}
		executeInternal(context);
	}

	/**
	 * 执行实际的 job。job 数据映射将已经被
	 * execute 作为 bean 属性值进行应用。约定与
	 * 标准 Quartz execute 方法完全相同。
	 * @see #execute
	 */
	protected abstract void executeInternal(JobExecutionContext context) throws JobExecutionException;

}
