/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

/**
 * 受支持的嵌入式数据库类型。
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 3.0
 */
public enum EmbeddedDatabaseType {

	/** <a href="https://hsqldb.org">Hypersonic</a> 嵌入式 Java SQL 数据库。 */
	HSQL,

	/** <a href="https://h2database.com">H2</a> 嵌入式 Java SQL 数据库引擎。 */
	H2,

	/** <a href="https://db.apache.org/derby">Apache Derby</a> 嵌入式 SQL 数据库。 */
	DERBY

}
