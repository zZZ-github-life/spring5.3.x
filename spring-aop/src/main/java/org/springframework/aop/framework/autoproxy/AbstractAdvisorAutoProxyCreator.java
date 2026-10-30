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

package org.springframework.aop.framework.autoproxy;

import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 通用的自动代理创建器，根据为每个 Bean 检测到的 Advisor 为特定 Bean 构建 AOP 代理。
 *
 * <p>子类可以覆盖 {@link #findCandidateAdvisors()} 方法，以返回适用于任何对象的自定义 Advisor 列表。
 * 子类还可以覆盖继承的 {@link #shouldSkip} 方法，将某些对象排除在自动代理之外。
 *
 * <p>需要排序的 Advisor 或 advice 应使用 {@link org.springframework.core.annotation.Order @Order}
 * 注解，或实现 {@link org.springframework.core.Ordered} 接口。此类使用
 * {@link AnnotationAwareOrderComparator} 对通知器排序。未使用 {@code @Order} 注解或未实现
 * {@code Ordered} 接口的 Advisor 将被视为无序；它们将以未定义的顺序出现在通知器链的末尾。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #findCandidateAdvisors
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

	@Nullable
	private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
	}

	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}


	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}

	/**
	 * 查找所有有资格为此类进行自动代理的 Advisor。
	 * @param beanClass 要为其查找通知器的类
	 * @param beanName 当前被代理的 Bean 的名称
	 * @return 如果没有切点或拦截器，则返回空 List，而不是 {@code null}
	 * @see #findCandidateAdvisors
	 * @see #sortAdvisors
	 * @see #extendAdvisors
	 */
	protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
		extendAdvisors(eligibleAdvisors);
		if (!eligibleAdvisors.isEmpty()) {
			eligibleAdvisors = sortAdvisors(eligibleAdvisors);
		}
		return eligibleAdvisors;
	}

	/**
	 * 查找所有可用于自动代理的候选 Advisor。
	 * @return 候选 Advisor 的 List
	 */
	protected List<Advisor> findCandidateAdvisors() {
		Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
		return this.advisorRetrievalHelper.findAdvisorBeans();
	}

	/**
	 * 搜索给定的候选 Advisor，找出所有可应用于指定 Bean 的 Advisor。
	 * @param candidateAdvisors 候选 Advisor
	 * @param beanClass 目标的 Bean 类
	 * @param beanName 目标的 Bean 名称
	 * @return 适用的 Advisor 的 List
	 * @see ProxyCreationContext#getCurrentProxiedBeanName()
	 */
	protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}

	/**
	 * 返回具有给定名称的 Advisor Bean 是否首先有资格进行代理。
	 * @param beanName Advisor Bean 的名称
	 * @return 该 Bean 是否符合条件
	 */
	protected boolean isEligibleAdvisorBean(String beanName) {
		return true;
	}

	/**
	 * 根据顺序对通知器排序。子类可以选择覆盖此方法以自定义排序策略。
	 * @param advisors 源 Advisor 的 List
	 * @return 排序后的 Advisor 的 List
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		AnnotationAwareOrderComparator.sort(advisors);
		return advisors;
	}

	/**
	 * 扩展钩子，子类可以覆盖它，以便基于目前获得的有序 Advisor 注册额外的 Advisor。
	 * <p>默认实现为空。
	 * <p>通常用于添加 Advisor，以暴露某些后续通知器所需的上下文信息。
	 * @param candidateAdvisors 已确定应用于给定 Bean 的 Advisor
	 */
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
	}

	/**
	 * 此自动代理创建器始终返回预先筛选的 Advisor。
	 */
	@Override
	protected boolean advisorsPreFiltered() {
		return true;
	}


	/**
	 * BeanFactoryAdvisorRetrievalHelper 的子类，委托给外层的 AbstractAdvisorAutoProxyCreator 设施。
	 */
	private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
		}
	}

}
