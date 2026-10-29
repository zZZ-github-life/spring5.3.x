/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.aop;

/**
 * 异常通知的标记接口。
 *
 * <p>此接口上没有任何方法，因为方法是通过反射调用的。实现类必须实现以下形式的方法：
 *
 * <pre class="code">void afterThrowing([Method, args, target], ThrowableSubclass);</pre>
 *
 * <p>以下是有效方法的一些示例：
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * 前三个参数是可选的，只有在我们需要更多连接点信息时才有用，如同 AspectJ 的<b>after-throwing</b>
 * 通知。
 *
 * <p><b>注意：</b>如果异常通知方法自身抛出异常，它将覆盖原始异常（即更改向用户抛出的异常）。覆盖后
 * 的异常通常是 RuntimeException；这与任何方法签名都兼容。然而，如果异常通知方法抛出受检异常，则该
 * 异常必须与目标方法声明的异常相匹配，因此在某种程度上会与特定的目标方法签名耦合。<b>不要抛出与目
 * 标方法签名不兼容的未声明受检异常！</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AfterReturningAdvice
 * @see MethodBeforeAdvice
 */
public interface ThrowsAdvice extends AfterAdvice {

}
