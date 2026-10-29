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

package org.springframework.aop.aspectj.autoproxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator} 的子类，暴露
 * AspectJ 的调用上下文，并理解当多条通知来自同一切面时 AspectJ 的通知优先级规则。
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

	private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();


	/**
	 * 按照 AspectJ 优先级对提供的 {@link Advisor} 实例排序。<p>如果两条通知来自同一个切面，它们将具有
	 * 相同的顺序。同一切面的通知随后按以下规则进一步排序：
	 * <ul>
	 * <li>如果二者中任意一个是 <em>after</em> 通知，则最后声明的通知具有最高优先级（即最后运行）。</li>
	 * <li>否则，最先声明的通知具有最高优先级（即最先运行）。</li>
	 * </ul>
	 * <p><b>重要：</b>Advisor 按优先级顺序排序，从最高优先级到最低优先级。在进入连接点时，最高优先级的
	 * Advisor 应最先运行；在退出连接点时，最高优先级的 Advisor 应最后运行。
	 */
	@Override
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
		for (Advisor advisor : advisors) {
			partiallyComparableAdvisors.add(
					new PartiallyComparableAdvisorHolder(advisor, DEFAULT_PRECEDENCE_COMPARATOR));
		}
		List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
		if (sorted != null) {
			List<Advisor> result = new ArrayList<>(advisors.size());
			for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
				result.add(pcAdvisor.getAdvisor());
			}
			return result;
		}
		else {
			return super.sortAdvisors(advisors);
		}
	}

	/**
	 * 将 {@link ExposeInvocationInterceptor} 添加到通知链的开头。<p>在使用 AspectJ 切点表达式以及使用
	 * AspectJ 风格的通知时，需要这个额外的通知。
	 */
	@Override
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
	}

	@Override
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// TODO: Consider optimization by caching the list of the aspect names
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		for (Advisor advisor : candidateAdvisors) {
			if (advisor instanceof AspectJPointcutAdvisor &&
					((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)) {
				return true;
			}
		}
		return super.shouldSkip(beanClass, beanName);
	}


	/**
	 * 实现 AspectJ 的 {@link PartialComparable} 接口，用于定义偏序。
	 */
	private static class PartiallyComparableAdvisorHolder implements PartialComparable {

		private final Advisor advisor;

		private final Comparator<Advisor> comparator;

		public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
			this.advisor = advisor;
			this.comparator = comparator;
		}

		@Override
		public int compareTo(Object obj) {
			Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
			return this.comparator.compare(this.advisor, otherAdvisor);
		}

		@Override
		public int fallbackCompareTo(Object obj) {
			return 0;
		}

		public Advisor getAdvisor() {
			return this.advisor;
		}

		@Override
		public String toString() {
			Advice advice = this.advisor.getAdvice();
			StringBuilder sb = new StringBuilder(ClassUtils.getShortName(advice.getClass()));
			boolean appended = false;
			if (this.advisor instanceof Ordered) {
				sb.append(": order = ").append(((Ordered) this.advisor).getOrder());
				appended = true;
			}
			if (advice instanceof AbstractAspectJAdvice) {
				sb.append(!appended ? ": " : ", ");
				AbstractAspectJAdvice ajAdvice = (AbstractAspectJAdvice) advice;
				sb.append("aspect name = ");
				sb.append(ajAdvice.getAspectName());
				sb.append(", declaration order = ");
				sb.append(ajAdvice.getDeclarationOrder());
			}
			return sb.toString();
		}
	}

}
