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

package org.springframework.remoting.support;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * 基于 {@link RemoteInvocation} 对象序列化的远程服务访问器的抽象基类。
 *
 * 提供 "remoteInvocationFactory" 属性，默认策略为 {@link DefaultRemoteInvocationFactory}。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setRemoteInvocationFactory
 * @see RemoteInvocation
 * @see RemoteInvocationFactory
 * @see DefaultRemoteInvocationFactory
 */
public abstract class RemoteInvocationBasedAccessor extends UrlBasedRemoteAccessor {

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();


	/**
	 * 设置此访问器要使用的 RemoteInvocationFactory。默认为 {@link DefaultRemoteInvocationFactory}。
	 * <p>自定义调用工厂可以为调用添加更多上下文信息，例如用户凭据。
	 */
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory =
				(remoteInvocationFactory != null ? remoteInvocationFactory : new DefaultRemoteInvocationFactory());
	}

	/**
	 * 返回此访问器使用的 RemoteInvocationFactory。
	 */
	public RemoteInvocationFactory getRemoteInvocationFactory() {
		return this.remoteInvocationFactory;
	}

	/**
	 * 为给定的 AOP 方法调用创建一个新的 RemoteInvocation 对象。
	 * <p>默认实现委托给已配置的 {@link #setRemoteInvocationFactory RemoteInvocationFactory}。可以在子
	 * 类中覆盖此方法，以提供包含额外调用参数（例如用户凭据）的自定义 RemoteInvocation 子类。
	 * <p>请注意，最好构建一个可复用的自定义 RemoteInvocationFactory 策略，而不是覆盖此方法。
	 * @param methodInvocation 当前 AOP 方法调用
	 * @return RemoteInvocation 对象
	 * @see RemoteInvocationFactory#createRemoteInvocation
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return getRemoteInvocationFactory().createRemoteInvocation(methodInvocation);
	}

	/**
	 * 重建给定 RemoteInvocationResult 对象中包含的调用结果。
	 * <p>默认实现调用默认的 {@code recreate()} 方法。可以在子类中覆盖此方法以提供自定义重建逻辑，可能
	 * 还会处理返回的结果对象。
	 * @param result 要重新创建的 RemoteInvocationResult
	 * @return 如果调用结果是成功的返回值，则返回该返回值
	 * @throws Throwable 如果调用结果是异常
	 * @see RemoteInvocationResult#recreate()
	 */
	@Nullable
	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		return result.recreate();
	}

}
