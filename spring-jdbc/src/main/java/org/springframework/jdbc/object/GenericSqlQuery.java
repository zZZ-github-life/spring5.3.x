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

package org.springframework.jdbc.object;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link SqlQuery} 的具体变体，可以通过 {@link RowMapper} 进行配置。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> 结果类型
 * @see #setRowMapper
 * @see #setRowMapperClass
 */
public class GenericSqlQuery<T> extends SqlQuery<T> {

	@Nullable
	private RowMapper<T> rowMapper;

	@SuppressWarnings("rawtypes")
	@Nullable
	private Class<? extends RowMapper> rowMapperClass;


	/**
	 * 设置用于此查询的特定 {@link RowMapper} 实例。
	 * @since 4.3.2
	 */
	public void setRowMapper(RowMapper<T> rowMapper) {
		this.rowMapper = rowMapper;
	}

	/**
	 * 为此查询设置 {@link RowMapper} 类，每次执行时都会创建新的
	 * {@link RowMapper} 实例。
	 */
	@SuppressWarnings("rawtypes")
	public void setRowMapperClass(Class<? extends RowMapper> rowMapperClass) {
		this.rowMapperClass = rowMapperClass;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.isTrue(this.rowMapper != null || this.rowMapperClass != null,
				"'rowMapper' or 'rowMapperClass' is required");
	}


	@Override
	@SuppressWarnings("unchecked")
	protected RowMapper<T> newRowMapper(@Nullable Object[] parameters, @Nullable Map<?, ?> context) {
		if (this.rowMapper != null) {
			return this.rowMapper;
		}
		else {
			Assert.state(this.rowMapperClass != null, "No RowMapper set");
			return BeanUtils.instantiateClass(this.rowMapperClass);
		}
	}

}
