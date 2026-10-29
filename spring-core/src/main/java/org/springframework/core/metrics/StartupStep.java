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

package org.springframework.core.metrics;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * 记录 {@link ApplicationStartup} 期间发生的特定阶段或操作的指标信息的步骤。
 *
 * <p>{@code StartupStep} 的生命周期如下：
 * <ol>
 * <li>通过调用 {@link ApplicationStartup#start(String) 应用启动}来创建步骤并开始，并为其分配唯一的 {@link StartupStep#getId() id}。
 * <li>然后我们可以在处理过程中通过 {@link Tags} 附加信息
 * <li>接着需要标记步骤的 {@link #end() 结束}
 * </ol>
 *
 * <p>实现可以跟踪步骤的“执行时间”或其他指标。
 *
 * @author Brian Clozel
 * @since 5.3
 */
public interface StartupStep {

	/**
	 * 返回启动步骤的名称。
	 * <p>步骤名称描述当前的操作或阶段。这个技术
	 * 名称应以 "." 划分命名空间，并且可以复用来描述应用程序启动期间
	 * 类似步骤的其他实例。
	 */
	String getName();

	/**
	 * 返回此步骤在应用程序启动中的唯一 id。
	 */
	long getId();

	/**
	 * 如果可用，返回父步骤的 id。
	 * <p>父步骤是在创建当前步骤时
	 * 最近启动的步骤。
	 */
	@Nullable
	Long getParentId();

	/**
	 * 向该步骤添加一个 {@link Tag}。
	 * @param key tag key
	 * @param value tag value
	 */
	StartupStep tag(String key, String value);

	/**
	 * 向该步骤添加一个 {@link Tag}。
	 * @param key tag key
	 * @param value {@link Supplier} for the tag value
	 */
	StartupStep tag(String key, Supplier<String> value);

	/**
	 * 返回此步骤的 {@link Tag} 集合。
	 */
	Tags getTags();

	/**
	 * 记录步骤的状态以及诸如执行时间之类的其他指标。
	 * <p>一旦结束，就不允许更改步骤状态。
	 */
	void end();


	/**
	 * 不可变的 {@link Tag} 集合。
	 */
	interface Tags extends Iterable<Tag> {
	}


	/**
	 * 用于存储步骤元数据的简单键/值关联。
	 */
	interface Tag {

		/**
		 * 返回 {@code Tag} 名称。
		 */
		String getKey();

		/**
		 * 返回 {@code Tag} 值。
		 */
		String getValue();
	}

}
