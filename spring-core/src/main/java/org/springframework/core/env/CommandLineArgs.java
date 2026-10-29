/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * 命令行参数的简单表示，分为“选项参数”和“非选项参数”。
 *
 * @author Chris Beams
 * @since 3.1
 * @see SimpleCommandLineArgsParser
 */
class CommandLineArgs {

	private final Map<String, List<String>> optionArgs = new HashMap<>();
	private final List<String> nonOptionArgs = new ArrayList<>();

	/**
	 * 为给定选项名称添加一个选项参数，并将给定值添加到与该选项关联的值列表（该列表可能包含零个或多个值）。
	 * 给定值可以为 {@code null}，表示该选项在指定时未附带值（例如“--foo”与“--foo=bar”）。
	 */
	public void addOptionArg(String optionName, @Nullable String optionValue) {
		if (!this.optionArgs.containsKey(optionName)) {
			this.optionArgs.put(optionName, new ArrayList<>());
		}
		if (optionValue != null) {
			this.optionArgs.get(optionName).add(optionValue);
		}
	}

	/**
	 * 返回命令行上出现的所有选项参数的集合。
	 */
	public Set<String> getOptionNames() {
		return Collections.unmodifiableSet(this.optionArgs.keySet());
	}

	/**
	 * 返回命令行上是否存在具有给定名称的选项。
	 */
	public boolean containsOption(String optionName) {
		return this.optionArgs.containsKey(optionName);
	}

	/**
	 * 返回与给定选项关联的值列表。{@code null} 表示该选项不存在；空列表表示没有值与该选项关联。
	 */
	@Nullable
	public List<String> getOptionValues(String optionName) {
		return this.optionArgs.get(optionName);
	}

	/**
	 * 将给定值添加到非选项参数列表。
	 */
	public void addNonOptionArg(String value) {
		this.nonOptionArgs.add(value);
	}

	/**
	 * 返回命令行上指定的非选项参数列表。
	 */
	public List<String> getNonOptionArgs() {
		return Collections.unmodifiableList(this.nonOptionArgs);
	}

}
