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

package org.springframework.jdbc.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

/**
 * 用于特定数据库的自定义 {@link SQLExceptionTranslator} 实例的注册表。
 *
 * @author Thomas Risberg
 * @since 3.1.1
 */
public class CustomSQLExceptionTranslatorRegistrar implements InitializingBean {

	/**
	 * 保存特定数据库自定义转换器的 Map 注册表。
	 * 键是数据库产品名称，定义于
	 * {@link org.springframework.jdbc.support.SQLErrorCodesFactory}。
	 */
	private final Map<String, SQLExceptionTranslator> translators = new HashMap<>();


	/**
	 * 设置 {@link SQLExceptionTranslator} 引用的 Map，其中键必须是
	 * {@code sql-error-codes.xml} 文件中定义的数据库名称。
	 * <p>请注意，除非数据库名称匹配，否则现有的转换器将保留；一旦匹配，
	 * 新的转换器将替换现有的转换器。
	 */
	public void setTranslators(Map<String, SQLExceptionTranslator> translators) {
		this.translators.putAll(translators);
	}

	@Override
	public void afterPropertiesSet() {
		this.translators.forEach((dbName, translator) ->
				CustomSQLExceptionTranslatorRegistry.getInstance().registerTranslator(dbName, translator));
	}

}
