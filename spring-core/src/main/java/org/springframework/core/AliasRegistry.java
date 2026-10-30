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

package org.springframework.core;

/**
 * 用于管理别名的公共接口。
 * 作为 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 的父接口。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface AliasRegistry {

	/**
	 * 给定一个名称，为其注册别名。
	 * @param name 规范名称
	 * @param alias 要注册的别名
	 * @throws IllegalStateException 如果该别名已被使用且不允许覆盖
	 */
	void registerAlias(String name, String alias);

	/**
	 * 从此注册表中移除指定的别名。
	 * @param alias 要移除的别名
	 * @throws IllegalStateException 如果未找到该别名
	 */
	void removeAlias(String alias);

	/**
	 * 判断给定名称是否被定义为别名（而非实际注册的组件的名称）。
	 * @param name 要检查的名称
	 * @return 给定名称是否为别名
	 */
	boolean isAlias(String name);

	/**
	 * 返回给定名称的别名（如果已定义）。
	 * @param name 要检查别名的名称
	 * @return 别名数组，若没有则为空数组
	 */
	String[] getAliases(String name);

}
