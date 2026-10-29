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

package org.springframework.jdbc.core.namedparam;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.lang.Nullable;

/**
 * JdbcDaoSupport 的扩展，同时暴露 NamedParameterJdbcTemplate。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 */
public class NamedParameterJdbcDaoSupport extends JdbcDaoSupport {

	@Nullable
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


	/**
	 * 基于已配置的 JdbcTemplate 创建 NamedParameterJdbcTemplate。
	 */
	@Override
	protected void initTemplateConfig() {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		if (jdbcTemplate != null) {
			this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		}
	}

	/**
	 * 返回包装已配置 JdbcTemplate 的 NamedParameterJdbcTemplate。
	 */
	@Nullable
	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return this.namedParameterJdbcTemplate;
	}

}
