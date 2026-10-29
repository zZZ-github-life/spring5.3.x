/*
 * Copyright 2002-2015 the original author or authors.
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

import javax.sql.DataSource;

/**
 * {@code EmbeddedDatabase} 用作嵌入式数据库实例的句柄。
 *
 * <p>{@code EmbeddedDatabase} 同时也是一个 {@link DataSource}，
 * 并增加了 {@link #shutdown} 操作，以便可以优雅地关闭嵌入式数据库实例。
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @since 3.0
 */
public interface EmbeddedDatabase extends DataSource {

	/**
	 * 关闭此嵌入式数据库。
	 */
	void shutdown();

}
