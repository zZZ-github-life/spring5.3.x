/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;

/**
 * 拦截新对象的构造过程。
 *
 * <p>用户应实现 {@link #construct(ConstructorInvocation)} 方法来修改原有行为。例如，下面的类实现了
 * 一个单例拦截器（被拦截的类只允许有一个唯一实例）：
 *
 * <pre class=code>
 * class DebuggingInterceptor implements ConstructorInterceptor {
 *   Object instance=null;
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     if(instance==null) {
 *       return instance=i.proceed();
 *     } else {
 *       throw new Exception("singleton does not allow multiple instance");
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
public interface ConstructorInterceptor extends Interceptor  {

	/**
	 * 实现此方法以在新对象构造前后执行额外的处理。有礼貌的实现通常会调用 {@link Joinpoint#proceed()}。
	 * @param invocation the construction joinpoint
	 * @return the newly created object, which is also the result of
	 * the call to {@link Joinpoint#proceed()}; might be replaced by
	 * the interceptor
	 * @throws Throwable if the interceptors or the target object
	 * throws an exception
	 */
	@Nonnull
	Object construct(ConstructorInvocation invocation) throws Throwable;

}
