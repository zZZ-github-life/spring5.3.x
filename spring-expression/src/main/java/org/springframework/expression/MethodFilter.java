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

package org.springframework.expression;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MethodFilter 实例允许 SpEL 用户微调方法解析过程的行为。方法解析（将表达式中的方法名转换为要调用
 * 的真实方法）通常会通过简单调用 'Class.getMethods()' 获取候选方法，并选择第一个适合输入参数的方法。
 * 通过注册 MethodFilter，用户可以接收回调并更改被视为合适的方法。
 *
 * @author Andy Clement
 * @since 3.0.1
 */
@FunctionalInterface
public interface MethodFilter {

	/**
	 * 由方法解析器调用，以便 SpEL 用户整理可能被调用的候选方法列表。该过滤器可以移除不应被视为候选的方
	 * 法，并可以对结果排序。随后，解析器在寻找合适的候选方法时会按照过滤器返回的方法顺序进行搜索。
	 * @param methods 解析器原本要从中选择的完整方法列表
	 * @return 输入方法可能的一个子集，可按相关度排序
	 */
	List<Method> filter(List<Method> methods);

}
