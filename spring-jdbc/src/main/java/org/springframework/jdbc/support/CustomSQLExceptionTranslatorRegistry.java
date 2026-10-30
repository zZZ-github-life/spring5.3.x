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

package org.springframework.jdbc.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * 用于与特定数据库关联的自定义
 * {@link org.springframework.jdbc.support.SQLExceptionTranslator} 实例的注册表，
 * 允许根据名为 "sql-error-codes.xml" 的配置文件中包含的值覆盖转换行为。
 *
 * @author Thomas Risberg
 * @since 3.1.1
 * @see SQLErrorCodesFactory
 */
public final class CustomSQLExceptionTranslatorRegistry {

	private static final Log logger = LogFactory.getLog(CustomSQLExceptionTranslatorRegistry.class);

	/**
	 * 跟踪单个实例，以便将其返回给请求它的类。
	 */
	private static final CustomSQLExceptionTranslatorRegistry instance = new CustomSQLExceptionTranslatorRegistry();


	/**
	 * 返回单例实例。
	 */
	public static CustomSQLExceptionTranslatorRegistry getInstance() {
		return instance;
	}


	/**
	 * 保存特定数据库自定义转换器的 Map 注册表。
	 * 键是数据库产品名称，定义于
	 * {@link org.springframework.jdbc.support.SQLErrorCodesFactory}。
	 */
	private final Map<String, SQLExceptionTranslator> translatorMap = new HashMap<>();


	/**
	 * 创建 {@link CustomSQLExceptionTranslatorRegistry} 类的新实例。
	 * <p>非 public 以强制实现单例设计模式。
	 */
	private CustomSQLExceptionTranslatorRegistry() {
	}


	/**
	 * 为指定的数据库名称注册新的自定义转换器。
	 * @param dbName 数据库名称
	 * @param translator 自定义转换器
	 */
	public void registerTranslator(String dbName, SQLExceptionTranslator translator) {
		SQLExceptionTranslator replaced = this.translatorMap.put(dbName, translator);
		if (logger.isDebugEnabled()) {
			if (replaced != null) {
				logger.debug("Replacing custom translator [" + replaced + "] for database '" + dbName +
						"' with [" + translator + "]");
			}
			else {
				logger.debug("Adding custom translator of type [" + translator.getClass().getName() +
						"] for database '" + dbName + "'");
			}
		}
	}

	/**
	 * 查找指定数据库的自定义转换器。
	 * @param dbName 数据库名称
	 * @return 自定义转换器，如果未找到则为 {@code null}
	 */
	@Nullable
	public SQLExceptionTranslator findTranslatorForDatabase(String dbName) {
		return this.translatorMap.get(dbName);
	}

}
