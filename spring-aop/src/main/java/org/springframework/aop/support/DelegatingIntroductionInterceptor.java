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

package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.aop.IntroductionInterceptor} 接口的便捷实现。
 *
 * <p>子类只需扩展此类并自行实现要引入的接口。在这种情况下，委托就是子类实例本身。或者，也可以由一
 * 个单独的委托实现该接口，并通过 delegate Bean 属性进行设置。
 *
 * <p>委托或子类可以实现任意数量的接口。默认情况下，除 IntroductionInterceptor 之外的所有接口都会从
 * 子类或委托中选取。
 *
 * <p>{@code suppressInterface} 方法可用于抑制由委托实现但不应该引入到所属 AOP 代理的接口。
 *
 * <p>如果委托可序列化，则此类的实例也可序列化。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.11.2003
 * @see #suppressInterface
 * @see DelegatePerTargetObjectIntroductionInterceptor
 */
@SuppressWarnings("serial")
public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * 实际实现这些接口的对象。如果子类实现了所引入的接口，则可能是 "this"。
	 */
	@Nullable
	private Object delegate;


	/**
	 * 构造一个新的 DelegatingIntroductionInterceptor，并提供一个实现要引入接口的委托。
	 * @param delegate 实现所引介接口的委托对象
	 */
	public DelegatingIntroductionInterceptor(Object delegate) {
		init(delegate);
	}

	/**
	 * 构造一个新的 DelegatingIntroductionInterceptor。委托将是子类，该子类必须实现额外的接口。
	 */
	protected DelegatingIntroductionInterceptor() {
		init(this);
	}


	/**
	 * 两个构造函数都使用此初始化方法，因为无法从一个构造函数向另一个构造函数传递 "this" 引用。
	 * @param delegate 委托对象
	 */
	private void init(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		implementInterfacesOnObject(delegate);

		// We don't want to expose the control interface
		suppressInterface(IntroductionInterceptor.class);
		suppressInterface(DynamicIntroductionAdvice.class);
	}


	/**
	 * 如果子类希望在环绕通知中执行自定义行为，则可能需要覆盖此方法。但子类应调用此方法，因为它负责处理
	 * 引入的接口以及向目标转发。
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (isMethodOnIntroducedInterface(mi)) {
			// Using the following method rather than direct reflection, we
			// get correct handling of InvocationTargetException
			// if the introduced method throws an exception.
			Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

			// Massage return value if possible: if the delegate returned itself,
			// we really want to return the proxy.
			if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
				Object proxy = ((ProxyMethodInvocation) mi).getProxy();
				if (mi.getMethod().getReturnType().isInstance(proxy)) {
					retVal = proxy;
				}
			}
			return retVal;
		}

		return doProceed(mi);
	}

	/**
	 * 使用提供的 {@link org.aopalliance.intercept.MethodInterceptor} 继续执行。子类可以覆盖此方法来拦
	 * 截目标对象上的方法调用，当引入需要监视其被引入的对象时，这很有用。对于引入接口上的
	 * {@link MethodInvocation MethodInvocations}，<strong>绝不会</strong>调用此方法。
	 */
	@Nullable
	protected Object doProceed(MethodInvocation mi) throws Throwable {
		// If we get here, just pass the invocation on.
		return mi.proceed();
	}

}
