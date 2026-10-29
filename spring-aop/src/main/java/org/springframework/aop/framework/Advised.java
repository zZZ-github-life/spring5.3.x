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

package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * 由持有 AOP 代理工厂配置的类实现的接口。该配置包括拦截器和其他通知、Advisor，以及被代理的接口。
 *
 * <p>从 Spring 获得的任何 AOP 代理都可以强制转换为该接口，以便操作其 AOP 通知。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 * @see org.springframework.aop.framework.AdvisedSupport
 */
public interface Advised extends TargetClassAware {

	/**
	 * 返回 Advised 配置是否已被冻结；若已冻结，则无法进行任何通知更改。
	 */
	boolean isFrozen();

	/**
	 * 我们是否代理整个目标类，而不是指定的接口？
	 */
	boolean isProxyTargetClass();

	/**
	 * 返回由 AOP 代理所代理的接口。
	 * <p>不会包含目标类，虽然目标类也可能被代理。
	 */
	Class<?>[] getProxiedInterfaces();

	/**
	 * 确定给定的接口是否被代理。
	 * @param intf the interface to check
	 */
	boolean isInterfaceProxied(Class<?> intf);

	/**
	 * 更改此 {@code Advised} 对象使用的 {@code TargetSource}。
	 * <p>仅在配置未 {@linkplain #isFrozen 冻结} 时有效。
	 * @param targetSource new TargetSource to use
	 */
	void setTargetSource(TargetSource targetSource);

	/**
	 * 返回此 {@code Advised} 对象使用的 {@code TargetSource}。
	 */
	TargetSource getTargetSource();

	/**
	 * 设置 AOP 框架是否应将该代理暴露为 {@link ThreadLocal}，以便通过 {@link AopContext} 类获取。
	 * <p>如果被通知的对象需要在自身调用方法时应用通知，则可能需要暴露该代理。否则，当被通知的对象在
	 * {@code this} 上调用方法时，不会应用任何通知。
	 * <p>默认值为 {@code false}，以获得最佳性能。
	 */
	void setExposeProxy(boolean exposeProxy);

	/**
	 * 返回工厂是否应将代理暴露为 {@link ThreadLocal}。
	 * <p>如果被通知的对象需要在自身调用方法时应用通知，则可能需要暴露该代理。否则，当被通知的对象在
	 * {@code this} 上调用方法时，不会应用任何通知。
	 * <p>获取代理类似于 EJB 调用 {@code getEJBObject()}。
	 * @see AopContext
	 */
	boolean isExposeProxy();

	/**
	 * 设置此代理配置是否已预先过滤，从而仅包含适用的 advisor（与此代理的目标类匹配）。
	 * <p>默认值为 "false"。如果 advisor 已经预先过滤，请将其设置为 "true"，这意味着在为代理调用构建实
	 * 际 advisor 链时可以跳过 ClassFilter 检查。
	 * @see org.springframework.aop.ClassFilter
	 */
	void setPreFiltered(boolean preFiltered);

	/**
	 * 返回此代理配置是否已预先过滤，从而仅包含适用的 advisor（与此代理的目标类匹配）。
	 */
	boolean isPreFiltered();

	/**
	 * 返回应用于此代理的 advisor。
	 * @return a list of Advisors applying to this proxy (never {@code null})
	 */
	Advisor[] getAdvisors();

	/**
	 * 返回应用于此代理的 advisor 数量。
	 * <p>默认实现委托给 {@code getAdvisors().length}。
	 * @since 5.3.1
	 */
	default int getAdvisorCount() {
		return getAdvisors().length;
	}

	/**
	 * 在 advisor 链的末尾添加一个 advisor。
	 * <p>该 Advisor 可以是 {@link org.springframework.aop.IntroductionAdvisor}，此时当再次从相关工厂获
	 * 取代理时，新的接口将可用。
	 * @param advisor the advisor to add to the end of the chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * 在链中的指定位置添加一个 Advisor。
	 * @param advisor the advisor to add at the specified position in the chain
	 * @param pos position in chain (0 is head). Must be valid.
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * 移除给定的 advisor。
	 * @param advisor the advisor to remove
	 * @return {@code true} if the advisor was removed; {@code false}
	 * if the advisor was not found and hence could not be removed
	 */
	boolean removeAdvisor(Advisor advisor);

	/**
	 * 移除给定索引处的 advisor。
	 * @param index the index of advisor to remove
	 * @throws AopConfigException if the index is invalid
	 */
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * 返回给定 advisor 的索引（从 0 开始），如果没有此类 advisor 应用于此代理，则返回 -1。
	 * <p>该方法的返回值可用作 advisors 数组的索引。
	 * @param advisor the advisor to search for
	 * @return index from 0 of this advisor, or -1 if there's no such advisor
	 */
	int indexOf(Advisor advisor);

	/**
	 * 替换给定的 advisor。
	 * <p><b>Note:</b> 如果该 advisor 是 {@link org.springframework.aop.IntroductionAdvisor}，而替换者
	 * 不是该类型或实现了不同的接口，则需要重新获取代理，否则旧接口将不再受支持，新接口也不会被实现。
	 * @param a the advisor to replace
	 * @param b the advisor to replace it with
	 * @return whether it was replaced. If the advisor wasn't found in the
	 * list of advisors, this method returns {@code false} and does nothing.
	 * @throws AopConfigException in case of invalid advice
	 */
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * 将给定的 AOP Alliance 通知添加到通知（拦截器）链的末尾。
	 * <p>它会被包装在一个带始终应用的切点的 DefaultPointcutAdvisor 中，并以这种包装形式从
	 * {@code getAdvisors()} 方法返回。
	 * <p>注意，给定的通知将应用于代理上的所有调用，甚至包括 {@code toString()} 方法！请使用适当的通知
	 * 实现或指定适当的切点，以应用于更窄的方法集合。
	 * @param advice the advice to add to the tail of the chain
	 * @throws AopConfigException in case of invalid advice
	 * @see #addAdvice(int, Advice)
	 * @see org.springframework.aop.support.DefaultPointcutAdvisor
	 */
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * 在通知链中的指定位置添加给定的 AOP Alliance Advice。
	 * <p>它会被包装在一个带始终应用的切点的
	 * {@link org.springframework.aop.support.DefaultPointcutAdvisor} 中，并以这种包装形式从
	 * {@link #getAdvisors()} 方法返回。
	 * <p>注意：给定的通知将应用于代理上的所有调用，甚至包括 {@code toString()} 方法！请使用适当的通知
	 * 实现或指定适当的切点，以应用于更窄的方法集合。
	 * @param pos index from 0 (head)
	 * @param advice the advice to add at the specified position in the advice chain
	 * @throws AopConfigException in case of invalid advice
	 */
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * 移除包含给定通知的 Advisor。
	 * @param advice the advice to remove
	 * @return {@code true} of the advice was found and removed;
	 * {@code false} if there was no such advice
	 */
	boolean removeAdvice(Advice advice);

	/**
	 * 返回给定 AOP Alliance Advice 的索引（从 0 开始），如果没有此类通知作为此代理的通知，则返回 -1。
	 * <p>该方法的返回值可用作 advisors 数组的索引。
	 * @param advice the AOP Alliance advice to search for
	 * @return index from 0 of this advice, or -1 if there's no such advice
	 */
	int indexOf(Advice advice);

	/**
	 * 由于 {@code toString()} 通常会委托给目标，此方法返回 AOP 代理的对应结果。
	 * @return a string description of the proxy configuration
	 */
	String toProxyConfigString();

}
