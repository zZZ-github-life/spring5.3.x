/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;

/**
 * {@link Pointcut} 的一部分：检查目标方法是否有资格获得通知。
 *
 * <p>MethodMatcher 可以<b>静态</b>求值，也可以在<b>运行时</b>（动态地）求值。静态匹配涉及方法以及
 * （可能）方法属性。动态匹配还可以获得特定调用的参数，以及此前应用于该连接点的通知运行所产生的任何
 * 影响。
 *
 * <p>如果某个实现从其 {@link #isRuntime()} 方法返回 {@code false}，则可以静态执行求值，并且对于该
 * 方法的所有调用，无论参数是什么，结果都相同。这意味着，如果 {@link #isRuntime()} 方法返回
 * {@code false}，则 3 参数版本的 {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法
 * 将永远不会被调用。
 *
 * <p>如果某个实现从其 2 参数版本的 {@link #matches(java.lang.reflect.Method, Class)} 方法返回
 * {@code true}，并且其 {@link #isRuntime()} 方法返回 {@code true}，则会在<i>相关通知每次可能执行之
 * 前立即</i>调用 3 参数版本的 {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法，以
 * 决定该通知是否应当运行。之前的所有通知（例如拦截器链中较早的拦截器）都已运行完毕，因此它们在参数
 * 或 ThreadLocal 状态中产生的任何状态变更在求值时都可用。
 *
 * <p>此接口的具体实现通常应提供 {@link Object#equals(Object)} 和 {@link Object#hashCode()} 的适当
 * 实现，以便匹配器可用于缓存场景 &mdash; 例如，在由 CGLIB 生成的代理中。
 *
 * @author Rod Johnson
 * @since 11.11.2003
 * @see Pointcut
 * @see ClassFilter
 */
public interface MethodMatcher {

	/**
	 * 执行静态检查，判断给定方法是否匹配。<p>如果此方法返回 {@code false}，或者 {@link #isRuntime()}
	 * 方法返回 {@code false}，则不会进行运行时检查（即不会调用
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])}）。
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @return whether this method matches statically
	 */
	boolean matches(Method method, Class<?> targetClass);

	/**
	 * 此 MethodMatcher 是否为动态的，也就是说，即使 2 参数版本的 matches 方法返回 {@code true}，也必须
	 * 在运行时对 {@link #matches(java.lang.reflect.Method, Class, Object[])} 方法进行最终调用吗？<p>可
	 * 以在创建 AOP 代理时调用，并且无需在每次方法调用之前再次调用。
	 * @return whether a runtime match via the 3-arg
	 * {@link #matches(java.lang.reflect.Method, Class, Object[])} method
	 * is required if static matching passed
	 */
	boolean isRuntime();

	/**
	 * 检查此方法是否存在运行时（动态）匹配，该方法必须已经通过静态匹配。<p>仅当 2 参数版本的 matches
	 * 方法对给定方法和目标类返回 {@code true}，并且 {@link #isRuntime()} 方法返回 {@code true} 时，才
	 * 会调用此方法。它会在通知可能运行之前立即调用，且是在通知链中较早的通知运行之后。
	 * @param method the candidate method
	 * @param targetClass the target class
	 * @param args arguments to the method
	 * @return whether there's a runtime match
	 * @see MethodMatcher#matches(Method, Class)
	 */
	boolean matches(Method method, Class<?> targetClass, Object... args);


	/**
	 * 匹配所有方法的规范实例。
	 */
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
