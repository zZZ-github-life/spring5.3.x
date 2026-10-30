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

package org.springframework.jdbc.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

/**
 * {@link KeyHolder} 接口的标准实现，用于保存自动生成的主键
 * （即 JDBC 插入语句可能返回的主键）。
 *
 * <p>为每个插入操作创建一个此类的实例，并将其传递给相应的
 * {@link org.springframework.jdbc.core.JdbcTemplate} 或
 * {@link org.springframework.jdbc.object.SqlUpdate} 方法。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Slawomir Dymitrow
 * @since 1.1
 */
public class GeneratedKeyHolder implements KeyHolder {

	private final List<Map<String, Object>> keyList;


	/**
	 * 使用默认列表创建新的 GeneratedKeyHolder。
	 */
	public GeneratedKeyHolder() {
		this.keyList = new ArrayList<>(1);
	}

	/**
	 * 使用给定列表创建新的 GeneratedKeyHolder。
	 * @param keyList 用于存放键映射的列表
	 */
	public GeneratedKeyHolder(List<Map<String, Object>> keyList) {
		this.keyList = keyList;
	}


	@Override
	@Nullable
	public Number getKey() throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
		return getKeyAs(Number.class);
	}

	@Override
	@Nullable
	public <T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
		if (this.keyList.isEmpty()) {
			return null;
		}
		if (this.keyList.size() > 1 || this.keyList.get(0).size() > 1) {
			throw new InvalidDataAccessApiUsageException(
					"The getKey method should only be used when a single key is returned. " +
					"The current key entry contains multiple keys: " + this.keyList);
		}
		Iterator<Object> keyIter = this.keyList.get(0).values().iterator();
		if (keyIter.hasNext()) {
			Object key = keyIter.next();
			if (key == null || !(keyType.isAssignableFrom(key.getClass()))) {
				throw new DataRetrievalFailureException(
						"The generated key type is not supported. " +
						"Unable to cast [" + (key != null ? key.getClass().getName() : null) +
						"] to [" + keyType.getName() + "].");
			}
			return keyType.cast(key);
		}
		else {
			throw new DataRetrievalFailureException("Unable to retrieve the generated key. " +
					"Check that the table has an identity column enabled.");
		}
	}

	@Override
	@Nullable
	public Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException {
		if (this.keyList.isEmpty()) {
			return null;
		}
		if (this.keyList.size() > 1) {
			throw new InvalidDataAccessApiUsageException(
					"The getKeys method should only be used when keys for a single row are returned. " +
					"The current key list contains keys for multiple rows: " + this.keyList);
		}
		return this.keyList.get(0);
	}

	@Override
	public List<Map<String, Object>> getKeyList() {
		return this.keyList;
	}

}
