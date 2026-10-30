/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jca.cci.core;

import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 定义在 EIS 上使用 CCI 的基本操作集接口。
 * 由 CciTemplate 实现。不常使用，但作为增强可测试性的有用选项，
 * 因为它可以很容易地被模拟或存根化。
 *
 * <p>或者，可以模拟标准 CCI 基础设施。
 * 然而，模拟此接口的工作量要少得多。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciTemplate
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public interface CciOperations {

	/**
	 * 使用 CCI 在 EIS 上执行请求，实现为在 CCI Connection 上工作的回调操作。
	 * 这允许在 Spring 管理的 CCI 环境中实现任意数据访问操作：
	 * 即参与 Spring 管理的事务并将 JCA ResourceException 转换为
	 * Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象的集合。
	 * @param action 指定该操作的回调对象
	 * @return 该操作返回的结果对象（如果有）
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(ConnectionCallback<T> action) throws DataAccessException;

	/**
	 * 使用 CCI 在 EIS 上执行请求，实现为在 CCI Interaction 上工作的回调操作。
	 * 这允许在 Spring 管理的 CCI 环境中，在单个 Interaction 上实现任意数据访问操作：
	 * 即参与 Spring 管理的事务并将 JCA ResourceException 转换为
	 * Spring 的 DataAccessException 层次结构。
	 * <p>回调操作可以返回结果对象，例如领域对象或领域对象的集合。
	 * @param action 指定该操作的回调对象
	 * @return 该操作返回的结果对象（如果有）
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(InteractionCallback<T> action) throws DataAccessException;

	/**
	 * 使用 CCI 在 EIS 上执行指定的交互。
	 * @param spec 定义该交互的 CCI InteractionSpec 实例
	 * （特定于连接器）
	 * @param inputRecord 输入记录
	 * @return 输出记录
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	Record execute(InteractionSpec spec, Record inputRecord) throws DataAccessException;

	/**
	 * 使用 CCI 在 EIS 上执行指定的交互。
	 * @param spec 定义该交互的 CCI InteractionSpec 实例
	 * （特定于连接器）
	 * @param inputRecord 输入记录
	 * @param outputRecord 输出记录
	 * @throws DataAccessException 如果出现任何问题
	 */
	void execute(InteractionSpec spec, Record inputRecord, Record outputRecord) throws DataAccessException;

	/**
	 * 使用 CCI 在 EIS 上执行指定的交互。
	 * @param spec 定义该交互的 CCI InteractionSpec 实例
	 * （特定于连接器）
	 * @param inputCreator 创建要使用的输入记录的对象
	 * @return 输出记录
	 * @throws DataAccessException 如果出现任何问题
	 */
	Record execute(InteractionSpec spec, RecordCreator inputCreator) throws DataAccessException;

	/**
	 * 使用 CCI 在 EIS 上执行指定的交互。
	 * @param spec 定义该交互的 CCI InteractionSpec 实例
	 * （特定于连接器）
	 * @param inputRecord 输入记录
	 * @param outputExtractor 将输出记录转换为结果对象的对象
	 * @return 使用 RecordExtractor 对象提取的输出数据
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(InteractionSpec spec, Record inputRecord, RecordExtractor<T> outputExtractor)
			throws DataAccessException;

	/**
	 * 使用 CCI 在 EIS 上执行指定的交互。
	 * @param spec 定义该交互的 CCI InteractionSpec 实例
	 * （特定于连接器）
	 * @param inputCreator 创建要使用的输入记录的对象
	 * @param outputExtractor 将输出记录转换为结果对象的对象
	 * @return 使用 RecordExtractor 对象提取的输出数据
	 * @throws DataAccessException 如果出现任何问题
	 */
	@Nullable
	<T> T execute(InteractionSpec spec, RecordCreator inputCreator, RecordExtractor<T> outputExtractor)
			throws DataAccessException;

}
