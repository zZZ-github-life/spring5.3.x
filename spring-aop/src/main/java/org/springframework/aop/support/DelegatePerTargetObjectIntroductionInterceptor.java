/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Map;
import java.util.WeakHashMap;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.aop.IntroductionInterceptor} 接口的便捷实现。
 *
 * <p>与 {@link DelegatingIntroductionInterceptor} 不同，此类的单个实例可用于通知多个目标对象，并且
 * 每个目标对象都将拥有<i>自己的</i>委托（而 DelegatingIntroductionInterceptor 共享同一个委托，因而
 * 在所有目标之间共享同一状态）。
 *
 * <p>{@code suppressInterface} 方法可用于抑制由委托类实现但不应该引入到所属 AOP 代理的接口。
 *
 * <p>如果各个委托可序列化，则此类的实例也可序列化。
 *
 * <p><i>注意：此类与 {@link DelegatingIntroductionInterceptor} 之间存在一些实现上的相似之处，这提
 * 示将来可能通过重构提取一个公共父类。</i>
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 * @see #suppressInterface
 * @see DelegatingIntroductionInterceptor
 */
@SuppressWarnings("serial")
public class DelegatePerTargetObjectIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * 对键保持弱引用，因为我们不希望干扰垃圾回收。
	 */
	private final Map<Object, Object> delegateMap = new WeakHashMap<>();

	private Class<?> defaultImplType;

	private Class<?> interfaceType;


	public DelegatePerTargetObjectIntroductionInterceptor(Class<?> defaultImplType, Class<?> interfaceType) {
		this.defaultImplType = defaultImplType;
		this.interfaceType = interfaceType;
		// Create a new delegate now (but don't store it in the map).
		// We do this for two reasons:
		// 1) to fail early if there is a problem instantiating delegates
		// 2) to populate the interface map once and once only
		Object delegate = createNewDelegate();
		implementInterfacesOnObject(delegate);
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
			Object delegate = getIntroductionDelegateFor(mi.getThis());

			// Using the following method rather than direct reflection,
			// we get correct handling of InvocationTargetException
			// if the introduced method throws an exception.
			Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());

			// Massage return value if possible: if the delegate returned itself,
			// we really want to return the proxy.
			if (retVal == delegate && mi instanceof ProxyMethodInvocation) {
				retVal = ((ProxyMethodInvocation) mi).getProxy();
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

	private Object getIntroductionDelegateFor(@Nullable Object targetObject) {
		synchronized (this.delegateMap) {
			if (this.delegateMap.containsKey(targetObject)) {
				return this.delegateMap.get(targetObject);
			}
			else {
				Object delegate = createNewDelegate();
				this.delegateMap.put(targetObject, delegate);
				return delegate;
			}
		}
	}

	private Object createNewDelegate() {
		try {
			return ReflectionUtils.accessibleConstructor(this.defaultImplType).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot create default implementation for '" +
					this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
		}
	}

}
