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

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * 用于发现方法和构造函数参数名的接口。
 *
 * <p>参数名发现并非总是可行，但有多种策略可以尝试，例如查找可能在编译时生成的调试信息，以及查找可
 * 能随 AspectJ 注解方法一起出现的 argname 注解值。
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
public interface ParameterNameDiscoverer {

	/**
	 * 返回方法的参数名；如果无法确定，则返回 {@code null}。
	 *
	 * <p>如果参数名仅对给定方法的部分参数可用而对其他参数不可用，则数组中的各个条目可以为 {@code null}
	 * 。不过，建议在可行的情况下改用占位参数名。
	 * @param method 要为其查找参数名称的方法
	 * @return 如果可以解析则返回参数名称数组，否则返回 {@code null}
	 */
	@Nullable
	String[] getParameterNames(Method method);

	/**
	 * 返回构造函数的参数名；如果无法确定，则返回 {@code null}。
	 *
	 * <p>如果参数名仅对给定构造函数的部分参数可用而对其他参数不可用，则数组中的各个条目可以为
	 * {@code null}。不过，建议在可行的情况下改用占位参数名。
	 * @param ctor 要为其查找参数名称的构造器
	 * @return 如果可以解析则返回参数名称数组，否则返回 {@code null}
	 */
	@Nullable
	String[] getParameterNames(Constructor<?> ctor);

}
