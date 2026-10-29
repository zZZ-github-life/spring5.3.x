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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;

/**
 * 缓存特定的求值上下文，它以延迟方式将方法参数添加为 SpEL 变量。这种延迟特性消除了为发现参数而对类
 * 字节码进行不必要的解析。
 *
 * <p>还定义了一组“不可用变量”（即在访问时应立即导致异常的变量）。这对于验证条件不匹配很有用，即使
 * 并非所有潜在变量都存在。
 *
 * <p>为限制对象创建，使用了一个不美观的构造函数（而不是为延迟执行使用专门的类似 'closure' 的类）。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
class CacheEvaluationContext extends MethodBasedEvaluationContext {

	private final Set<String> unavailableVariables = new HashSet<>(1);


	CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
			ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject, method, arguments, parameterNameDiscoverer);
	}


	/**
	 * 将指定变量名称添加为该上下文的不可用变量。任何尝试访问此变量的表达式都应导致异常。
	 * <p>这允许验证可能使用某个变量的表达式，即使该变量尚不可用。因此，任何尝试使用该变量的表达式都应
	 * 求值失败。
	 */
	public void addUnavailableVariable(String name) {
		this.unavailableVariables.add(name);
	}


	/**
	 * 仅在需要时加载参数信息。
	 */
	@Override
	@Nullable
	public Object lookupVariable(String name) {
		if (this.unavailableVariables.contains(name)) {
			throw new VariableNotAvailableException(name);
		}
		return super.lookupVariable(name);
	}

}
