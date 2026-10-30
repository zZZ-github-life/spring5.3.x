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

package org.springframework.jdbc.core.namedparam;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.lang.Nullable;

/**
 * 提供使用 {@link SqlParameterSource} 的辅助方法的类，
 * 尤其是与 {@link NamedParameterJdbcTemplate} 配合使用时。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class SqlParameterSourceUtils {

	/**
	 * 创建一组用传入值（{@link Map} 或 bean 对象）填充的
	 * {@link SqlParameterSource} 对象数组。
	 * 这将确定批量操作中包含的内容。
	 * @param candidates 包含要使用的值的对象数组
	 * @return {@link SqlParameterSource} 数组
	 * @see MapSqlParameterSource
	 * @see BeanPropertySqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
	 */
	public static SqlParameterSource[] createBatch(Object... candidates) {
		return createBatch(Arrays.asList(candidates));
	}

	/**
	 * 创建一组用传入值（{@link Map} 或 bean 对象）填充的
	 * {@link SqlParameterSource} 对象数组。
	 * 这将确定批量操作中包含的内容。
	 * @param candidates 包含要使用的值的对象集合
	 * @return {@link SqlParameterSource} 数组
	 * @since 5.0.2
	 * @see MapSqlParameterSource
	 * @see BeanPropertySqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])
	 */
	@SuppressWarnings("unchecked")
	public static SqlParameterSource[] createBatch(Collection<?> candidates) {
		SqlParameterSource[] batch = new SqlParameterSource[candidates.size()];
		int i = 0;
		for (Object candidate : candidates) {
			batch[i] = (candidate instanceof Map ? new MapSqlParameterSource((Map<String, ?>) candidate) :
					new BeanPropertySqlParameterSource(candidate));
			i++;
		}
		return batch;
	}

	/**
	 * 创建一组用传入值填充的 {@link MapSqlParameterSource} 对象数组。
	 * 这将确定批量操作中包含的内容。
	 * @param valueMaps 包含要使用的值的 {@link Map} 实例数组
	 * @return {@link SqlParameterSource} 数组
	 * @see MapSqlParameterSource
	 * @see NamedParameterJdbcTemplate#batchUpdate(String, Map[])
	 */
	public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
		SqlParameterSource[] batch = new SqlParameterSource[valueMaps.length];
		for (int i = 0; i < valueMaps.length; i++) {
			batch[i] = new MapSqlParameterSource(valueMaps[i]);
		}
		return batch;
	}

	/**
	 * 如果参数包含类型信息则创建包装后的值，否则创建普通对象。
	 * @param source 参数值和类型信息的来源
	 * @param parameterName 参数的名称
	 * @return 值对象
	 * @see SqlParameterValue
	 */
	@Nullable
	public static Object getTypedValue(SqlParameterSource source, String parameterName) {
		int sqlType = source.getSqlType(parameterName);
		if (sqlType != SqlParameterSource.TYPE_UNKNOWN) {
			return new SqlParameterValue(sqlType, source.getTypeName(parameterName), source.getValue(parameterName));
		}
		else {
			return source.getValue(parameterName);
		}
	}

	/**
	 * 创建包含大小写不敏感的（小写）参数名及原始名称的 Map。
	 * @param parameterSource 参数名称的来源
	 * @return 可用于参数名称不区分大小写匹配的 Map
	 */
	public static Map<String, String> extractCaseInsensitiveParameterNames(SqlParameterSource parameterSource) {
		Map<String, String> caseInsensitiveParameterNames = new HashMap<>();
		String[] paramNames = parameterSource.getParameterNames();
		if (paramNames != null) {
			for (String name : paramNames) {
				caseInsensitiveParameterNames.put(name.toLowerCase(), name);
			}
		}
		return caseInsensitiveParameterNames;
	}

}
