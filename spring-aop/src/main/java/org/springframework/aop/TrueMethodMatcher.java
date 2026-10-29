/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 匹配所有方法的规范 MethodMatcher 实例。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
final class TrueMethodMatcher implements MethodMatcher, Serializable {

	public static final TrueMethodMatcher INSTANCE = new TrueMethodMatcher();


	/**
	 * 强制实施单例模式。
	 */
	private TrueMethodMatcher() {
	}


	@Override
	public boolean isRuntime() {
		return false;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		// Should never be invoked as isRuntime returns false.
		throw new UnsupportedOperationException();
	}


	@Override
	public String toString() {
		return "MethodMatcher.TRUE";
	}

	/**
	 * 为支持序列化所需。反序列化时替换为规范实例，以保护单例模式。这是重写 {@code equals()} 的替代方案。
	 *
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
