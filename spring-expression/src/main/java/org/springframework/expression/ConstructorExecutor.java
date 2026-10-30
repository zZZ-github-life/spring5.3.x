/*
 * Copyright 2002-2013 the original author or authors.
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


// TODO Is the resolver/executor model too pervasive in this package?
/**
 * 执行器由解析器构建，并可被基础设施缓存，以便快速重复某项操作，而无需再次回到解析器。例如，要在某
 * 个类上运行的特定构造器可能由反射构造器解析器发现——随后它会构建一个执行该构造器的
 * ConstructorExecutor，并且该 ConstructorExecutor 可以被复用，无需再回到解析器重新发现构造器。
 *
 * <p>它们可能变得陈旧，此时应抛出 AccessException——这将促使基础设施回到解析器请求一个新的执行器。
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface ConstructorExecutor {

	/**
	 * 在指定的上下文中使用指定的参数执行构造器。
	 * @param context 执行该命令所在的求值上下文
	 * @param arguments 构造器调用的参数，其数量和类型应与该命令运行所需匹配
	 * @return 新对象
	 * @throws AccessException 如果执行命令时出现问题，或 CommandExecutor 已不再有效
	 */
	TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
