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

package org.springframework.context.annotation;

/**
 * 列举各种作用域代理选项。
 *
 * <p>有关作用域代理到底是什么的更完整讨论，请参见 Spring 参考文档中题为“<em>将作用域 Bean 作为依赖
 * 项</em>”的一节。
 *
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
public enum ScopedProxyMode {

	/**
	 * 默认通常等于 {@link #NO}，除非在组件扫描指令级别配置了不同的默认值。
	 */
	DEFAULT,

	/**
	 * 不创建作用域代理。
	 *
	 * <p>当与非单例作用域实例一起使用时，此代理模式通常没有用；如果要将其用作依赖项，则应优先使用
	 * {@link #INTERFACES} 或 {@link #TARGET_CLASS} 代理模式。
	 */
	NO,

	/**
	 * 创建一个 JDK 动态代理，实现目标对象类所暴露的<i>所有</i>接口。
	 */
	INTERFACES,

	/**
	 * 创建一个基于类的代理（使用 CGLIB）。
	 */
	TARGET_CLASS

}
