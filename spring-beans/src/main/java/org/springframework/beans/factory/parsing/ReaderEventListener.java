/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import java.util.EventListener;

/**
 * 接收 Bean 定义读取过程中组件、别名和导入注册回调的接口。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see ReaderContext
 */
public interface ReaderEventListener extends EventListener {

	/**
	 * 通知：给定的默认值已注册。
	 * @param defaultsDefinition 默认值的描述符
	 * @see org.springframework.beans.factory.xml.DocumentDefaultsDefinition
	 */
	void defaultsRegistered(DefaultsDefinition defaultsDefinition);

	/**
	 * 通知：给定的组件已注册。
	 * @param componentDefinition 新组件的描述符
	 * @see BeanComponentDefinition
	 */
	void componentRegistered(ComponentDefinition componentDefinition);

	/**
	 * 通知：给定的别名已注册。
	 * @param aliasDefinition 新别名的描述符
	 */
	void aliasRegistered(AliasDefinition aliasDefinition);

	/**
	 * 通知：给定的导入已处理。
	 * @param importDefinition 导入的描述符
	 */
	void importProcessed(ImportDefinition importDefinition);

}
