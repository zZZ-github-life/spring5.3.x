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

import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

/**
 * {@link AdaptableJobFactory} 的子类，还支持 Spring 风格的
 * bean 属性依赖注入。这本质上是 Spring 的 {@link QuartzJobBean}
 * 在 Quartz {@link org.quartz.spi.JobFactory} 形式下的直接等价物。
 *
 * <p>将调度器上下文、job 数据映射和触发器数据映射条目
 * 作为 bean 属性值应用。如果找不到匹配的 bean 属性，
 * 该条目默认会被简单地忽略。这与 QuartzJobBean 的行为类似。
 *
 * <p>从 Spring 4.1 开始，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SchedulerFactoryBean#setJobFactory
 * @see QuartzJobBean
 */
public class SpringBeanJobFactory extends AdaptableJobFactory
		implements ApplicationContextAware, SchedulerContextAware {

	@Nullable
	private String[] ignoredUnknownProperties;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private SchedulerContext schedulerContext;


	/**
	 * 指定应忽略的未知属性（在 bean 中未找到）。
	 * <p>默认为 {@code null}，表示所有未知属性
	 * 都应被忽略。指定空数组以在遇到任何未知属性时
	 * 抛出异常，或指定在特定 job 类上找不到对应属性时
	 * 应被忽略的属性名称列表（所有其他未知属性
	 * 仍会触发异常）。
	 */
	public void setIgnoredUnknownProperties(String... ignoredUnknownProperties) {
		this.ignoredUnknownProperties = ignoredUnknownProperties;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setSchedulerContext(SchedulerContext schedulerContext) {
		this.schedulerContext = schedulerContext;
	}


	/**
	 * 创建 job 实例，使用从调度器上下文、
	 * job 数据映射和触发器数据映射中获取的属性值进行填充。
	 */
	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Object job = (this.applicationContext != null ?
				this.applicationContext.getAutowireCapableBeanFactory().createBean(
						bundle.getJobDetail().getJobClass(), AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false) :
				super.createJobInstance(bundle));

		if (isEligibleForPropertyPopulation(job)) {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
			MutablePropertyValues pvs = new MutablePropertyValues();
			if (this.schedulerContext != null) {
				pvs.addPropertyValues(this.schedulerContext);
			}
			pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
			pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
			if (this.ignoredUnknownProperties != null) {
				for (String propName : this.ignoredUnknownProperties) {
					if (pvs.contains(propName) && !bw.isWritableProperty(propName)) {
						pvs.removePropertyValue(propName);
					}
				}
				bw.setPropertyValues(pvs);
			}
			else {
				bw.setPropertyValues(pvs, true);
			}
		}

		return job;
	}

	/**
	 * 返回给定的 job 对象是否符合填充其 bean 属性的条件。
	 * <p>默认实现忽略 {@link QuartzJobBean} 实例，
	 * 它们会自行注入 bean 属性。
	 * @param jobObject the job object to introspect
	 * @see QuartzJobBean
	 */
	protected boolean isEligibleForPropertyPopulation(Object jobObject) {
		return (!(jobObject instanceof QuartzJobBean));
	}

}
