/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * 组合 {@link DatabasePopulator}，委托给一组给定的 {@code DatabasePopulator}
 * 实现，依次执行所有脚本。
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @author Mark Paluch
 * @since 5.3
 */
public class CompositeDatabasePopulator implements DatabasePopulator {

	private final List<DatabasePopulator> populators = new ArrayList<>(4);


	/**
	 * 创建一个空的 {@code CompositeDatabasePopulator}。
	 * @see #setPopulators
	 * @see #addPopulators
	 */
	public CompositeDatabasePopulator() {
	}

	/**
	 * 使用给定的 populator 创建一个 {@code CompositeDatabasePopulator}。
	 * @param populators 一个或多个要委托的填充器。
	 */
	public CompositeDatabasePopulator(Collection<DatabasePopulator> populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.addAll(populators);
	}

	/**
	 * 使用给定的 populator 创建一个 {@code CompositeDatabasePopulator}。
	 * @param populators 一个或多个要委托的填充器。
	 */
	public CompositeDatabasePopulator(DatabasePopulator... populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.addAll(Arrays.asList(populators));
	}


	/**
	 * 指定要委托的一个或多个 populator。
	 */
	public void setPopulators(DatabasePopulator... populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.clear();
		this.populators.addAll(Arrays.asList(populators));
	}

	/**
	 * 向委托列表中增加一个或多个 populator。
	 */
	public void addPopulators(DatabasePopulator... populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.addAll(Arrays.asList(populators));
	}

	@Override
	public Mono<Void> populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "Connection must not be null");
		return Flux.fromIterable(this.populators).concatMap(populator -> populator.populate(connection))
				.then();
	}

}
