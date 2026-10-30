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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * AOP 代理配置管理器的基类。这些管理器本身并不是 AOP 代理，但此类的子类通常是工厂，可直接从中获取
 * AOP 代理实例。
 *
 * <p>此类让子类无需操心 Advice 和 Advisor 的日常管理工作，但它并不实际实现代理创建方法，这些方法由
 * 子类提供。
 *
 * <p>此类可序列化；子类则不必可序列化。此类用于保存代理的快照。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.AopProxy
 */
public class AdvisedSupport extends ProxyConfig implements Advised {

	/** 使用 Spring 2.0 的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = 2651364800145442165L;


	/**
	 * 没有目标时使用的规范 TargetSource，此时行为由各 advisor 提供。
	 */
	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;


	/** 包级保护，以便为提升效率而直接访问。 */
	TargetSource targetSource = EMPTY_TARGET_SOURCE;

	/** Advisor 是否已针对特定目标类进行过滤。 */
	private boolean preFiltered = false;

	/** 要使用的 AdvisorChainFactory。 */
	AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	/** 以 Method 为键、advisor 链 List 为值的缓存。 */
	private transient Map<MethodCacheKey, List<Object>> methodCache;

	/**
	 * 代理需要实现的接口。保存在 List 中以维持注册顺序，从而按指定的接口顺序创建 JDK 代理。
	 */
	private List<Class<?>> interfaces = new ArrayList<>();

	/**
	 * Advisor 列表。如果添加的是 Advice，则会先将其包装成 Advisor，再添加到该列表中。
	 */
	private List<Advisor> advisors = new ArrayList<>();


	/**
	 * 无参构造方法，供按 JavaBean 方式使用。
	 */
	public AdvisedSupport() {
		this.methodCache = new ConcurrentHashMap<>(32);
	}

	/**
	 * 使用给定参数创建 AdvisedSupport 实例。
	 * @param interfaces 被代理的接口
	 */
	public AdvisedSupport(Class<?>... interfaces) {
		this();
		setInterfaces(interfaces);
	}


	/**
	 * 将给定的对象设置为目标。将为该对象创建一个 SingletonTargetSource。
	 * @see #setTargetSource
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setTarget(Object target) {
		setTargetSource(new SingletonTargetSource(target));
	}

	@Override
	public void setTargetSource(@Nullable TargetSource targetSource) {
		this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
	}

	@Override
	public TargetSource getTargetSource() {
		return this.targetSource;
	}

	/**
	 * 设置要代理的目标类，表示该代理应当可以强制转换为给定的类。
	 * <p>在内部，将为给定的目标类使用 {@link org.springframework.aop.target.EmptyTargetSource}。所需代
	 * 理的类型将在实际创建代理时确定。
	 * <p>这是对设置 "targetSource" 或 "target" 的替代方案，适用于我们希望基于目标类（可以是接口或具体
	 * 类）创建代理，而又没有完全可用的 TargetSource 的情况。
	 * @see #setTargetSource
	 * @see #setTarget
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetSource = EmptyTargetSource.forClass(targetClass);
	}

	@Override
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetSource.getTargetClass();
	}

	@Override
	public void setPreFiltered(boolean preFiltered) {
		this.preFiltered = preFiltered;
	}

	@Override
	public boolean isPreFiltered() {
		return this.preFiltered;
	}

	/**
	 * 设置要使用的 advisor 链工厂。
	 * <p>默认为 {@link DefaultAdvisorChainFactory}。
	 */
	public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
		Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
		this.advisorChainFactory = advisorChainFactory;
	}

	/**
	 * 返回要使用的 advisor 链工厂（永不为 {@code null}）。
	 */
	public AdvisorChainFactory getAdvisorChainFactory() {
		return this.advisorChainFactory;
	}


	/**
	 * 设置要代理的接口。
	 */
	public void setInterfaces(Class<?>... interfaces) {
		Assert.notNull(interfaces, "Interfaces must not be null");
		this.interfaces.clear();
		for (Class<?> ifc : interfaces) {
			addInterface(ifc);
		}
	}

	/**
	 * 添加一个新的被代理接口。
	 * @param intf 要额外代理的接口
	 */
	public void addInterface(Class<?> intf) {
		Assert.notNull(intf, "Interface must not be null");
		if (!intf.isInterface()) {
			throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
		}
		if (!this.interfaces.contains(intf)) {
			this.interfaces.add(intf);
			adviceChanged();
		}
	}

	/**
	 * 移除被代理的接口。
	 * <p>如果给定的接口未被代理，则不做任何操作。
	 * @param intf 要从代理中移除的接口
	 * @return 如果该接口已被移除，则返回 {@code true}；如果未找到该接口
	 * 因而无法移除，则返回 {@code false}
	 */
	public boolean removeInterface(Class<?> intf) {
		return this.interfaces.remove(intf);
	}

	@Override
	public Class<?>[] getProxiedInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public boolean isInterfaceProxied(Class<?> intf) {
		for (Class<?> proxyIntf : this.interfaces) {
			if (intf.isAssignableFrom(proxyIntf)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public final Advisor[] getAdvisors() {
		return this.advisors.toArray(new Advisor[0]);
	}

	@Override
	public int getAdvisorCount() {
		return this.advisors.size();
	}

	@Override
	public void addAdvisor(Advisor advisor) {
		int pos = this.advisors.size();
		addAdvisor(pos, advisor);
	}

	@Override
	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
		if (advisor instanceof IntroductionAdvisor) {
			validateIntroductionAdvisor((IntroductionAdvisor) advisor);
		}
		addAdvisorInternal(pos, advisor);
	}

	@Override
	public boolean removeAdvisor(Advisor advisor) {
		int index = indexOf(advisor);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public void removeAdvisor(int index) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
		}
		if (index < 0 || index > this.advisors.size() - 1) {
			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
					"This configuration only has " + this.advisors.size() + " advisors.");
		}

		Advisor advisor = this.advisors.remove(index);
		if (advisor instanceof IntroductionAdvisor) {
			IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
			// We need to remove introduction interfaces.
			for (Class<?> ifc : ia.getInterfaces()) {
				removeInterface(ifc);
			}
		}

		adviceChanged();
	}

	@Override
	public int indexOf(Advisor advisor) {
		Assert.notNull(advisor, "Advisor must not be null");
		return this.advisors.indexOf(advisor);
	}

	@Override
	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
		Assert.notNull(a, "Advisor a must not be null");
		Assert.notNull(b, "Advisor b must not be null");
		int index = indexOf(a);
		if (index == -1) {
			return false;
		}
		removeAdvisor(index);
		addAdvisor(index, b);
		return true;
	}

	/**
	 * 将给定的所有 advisor 添加到此代理配置中。
	 * @param advisors 要注册的通知器
	 */
	public void addAdvisors(Advisor... advisors) {
		addAdvisors(Arrays.asList(advisors));
	}

	/**
	 * 将给定的所有 advisor 添加到此代理配置中。
	 * @param advisors 要注册的通知器
	 */
	public void addAdvisors(Collection<Advisor> advisors) {
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (!CollectionUtils.isEmpty(advisors)) {
			for (Advisor advisor : advisors) {
				if (advisor instanceof IntroductionAdvisor) {
					validateIntroductionAdvisor((IntroductionAdvisor) advisor);
				}
				Assert.notNull(advisor, "Advisor must not be null");
				this.advisors.add(advisor);
			}
			adviceChanged();
		}
	}

	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
		advisor.validateInterfaces();
		// If the advisor passed validation, we can make the change.
		Class<?>[] ifcs = advisor.getInterfaces();
		for (Class<?> ifc : ifcs) {
			addInterface(ifc);
		}
	}

	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
		Assert.notNull(advisor, "Advisor must not be null");
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (pos > this.advisors.size()) {
			throw new IllegalArgumentException(
					"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
		}
		this.advisors.add(pos, advisor);
		adviceChanged();
	}

	/**
	 * 允许不受限制地访问 {@link List}，即 {@link Advisor Advisors} 列表。
	 * <p>请谨慎使用，并在做任何修改时记得 {@link #adviceChanged() fire advice changed events}。
	 */
	protected final List<Advisor> getAdvisorsInternal() {
		return this.advisors;
	}

	@Override
	public void addAdvice(Advice advice) throws AopConfigException {
		int pos = this.advisors.size();
		addAdvice(pos, advice);
	}

	/**
	 * 除非通知实现了 IntroductionInfo，否则无法通过这种方式添加引介。
	 */
	@Override
	public void addAdvice(int pos, Advice advice) throws AopConfigException {
		Assert.notNull(advice, "Advice must not be null");
		if (advice instanceof IntroductionInfo) {
			// We don't need an IntroductionAdvisor for this kind of introduction:
			// It's fully self-describing.
			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
		}
		else if (advice instanceof DynamicIntroductionAdvice) {
			// We need an IntroductionAdvisor for this kind of introduction.
			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
		}
		else {
			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
		}
	}

	@Override
	public boolean removeAdvice(Advice advice) throws AopConfigException {
		int index = indexOf(advice);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public int indexOf(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		for (int i = 0; i < this.advisors.size(); i++) {
			Advisor advisor = this.advisors.get(i);
			if (advisor.getAdvice() == advice) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 给定的通知是否包含在此代理配置中的任一 advisor 内？
	 * @param advice 要检查是否包含的通知
	 * @return 是否包含此通知实例
	 */
	public boolean adviceIncluded(@Nullable Advice advice) {
		if (advice != null) {
			for (Advisor advisor : this.advisors) {
				if (advisor.getAdvice() == advice) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 统计给定类的通知数量。
	 * @param adviceClass 要检查的通知类
	 * @return 此类或其子类的拦截器数量
	 */
	public int countAdvicesOfType(@Nullable Class<?> adviceClass) {
		int count = 0;
		if (adviceClass != null) {
			for (Advisor advisor : this.advisors) {
				if (adviceClass.isInstance(advisor.getAdvice())) {
					count++;
				}
			}
		}
		return count;
	}


	/**
	 * 根据此配置，为给定的方法确定一组 {@link org.aopalliance.intercept.MethodInterceptor} 对象。
	 * @param method 被代理的方法
	 * @param targetClass 目标类
	 * @return MethodInterceptor 列表（也可能包含
	 * InterceptorAndDynamicMethodMatchers）
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
		MethodCacheKey cacheKey = new MethodCacheKey(method);
		List<Object> cached = this.methodCache.get(cacheKey);
		if (cached == null) {
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}

	/**
	 * 在通知发生变更时调用。
	 */
	protected void adviceChanged() {
		this.methodCache.clear();
	}

	/**
	 * 在通过无参构造方法创建的新实例上调用此方法，以根据给定的对象创建配置的独立副本。
	 * @param other 要从中复制配置的 AdvisedSupport 对象
	 */
	protected void copyConfigurationFrom(AdvisedSupport other) {
		copyConfigurationFrom(other, other.targetSource, new ArrayList<>(other.advisors));
	}

	/**
	 * 从给定的 AdvisedSupport 对象复制 AOP 配置，但允许替换为新的 TargetSource 和给定的拦截器链。
	 * @param other 要从中获取代理配置的 AdvisedSupport 对象
	 * @param targetSource 新的 TargetSource
	 * @param advisors 用于该链的 Advisor
	 */
	protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
		copyFrom(other);
		this.targetSource = targetSource;
		this.advisorChainFactory = other.advisorChainFactory;
		this.interfaces = new ArrayList<>(other.interfaces);
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				validateIntroductionAdvisor((IntroductionAdvisor) advisor);
			}
			Assert.notNull(advisor, "Advisor must not be null");
			this.advisors.add(advisor);
		}
		adviceChanged();
	}

	/**
	 * 构建此 AdvisedSupport 的仅含配置的副本，并替换其 TargetSource。
	 */
	AdvisedSupport getConfigurationOnlyCopy() {
		AdvisedSupport copy = new AdvisedSupport();
		copy.copyFrom(this);
		copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
		copy.advisorChainFactory = this.advisorChainFactory;
		copy.interfaces = new ArrayList<>(this.interfaces);
		copy.advisors = new ArrayList<>(this.advisors);
		return copy;
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.methodCache = new ConcurrentHashMap<>(32);
	}

	@Override
	public String toProxyConfigString() {
		return toString();
	}

	/**
	 * 用于调试/诊断。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
		sb.append(this.advisors.size()).append(" advisors ");
		sb.append(this.advisors).append("; ");
		sb.append("targetSource [").append(this.targetSource).append("]; ");
		sb.append(super.toString());
		return sb.toString();
	}


	/**
	 * 围绕 Method 的简单包装类。在缓存方法时用作键，以便高效地进行 equals 和 hashCode 比较。
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final Method method;

		private final int hashCode;

		public MethodCacheKey(Method method) {
			this.method = method;
			this.hashCode = method.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof MethodCacheKey &&
					this.method == ((MethodCacheKey) other).method));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.method.getName().compareTo(other.method.getName());
			if (result == 0) {
				result = this.method.toString().compareTo(other.method.toString());
			}
			return result;
		}
	}

}
