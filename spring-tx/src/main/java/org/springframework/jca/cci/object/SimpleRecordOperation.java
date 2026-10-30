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

package org.springframework.jca.cci.object;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * EIS 操作对象，接受传入的 CCI 输入 Record 并返回相应的 CCI 输出 Record。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public class SimpleRecordOperation extends EisOperation {

	/**
	 * 允许作为 JavaBean 使用的构造器。
	 */
	public SimpleRecordOperation() {
	}

	/**
	 * 便捷的构造器，接受 ConnectionFactory 和各规范
	 * （连接规范和交互规范）。
	 * @param connectionFactory 用于获取连接的 ConnectionFactory
	 */
	public SimpleRecordOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		getCciTemplate().setConnectionFactory(connectionFactory);
		setInteractionSpec(interactionSpec);
	}


	/**
	 * 执行此操作对象封装的 CCI 交互。
	 * <p>此方法将调用 CCI 的 {@code Interaction.execute} 返回输出 Record 的变体。
	 * @param inputRecord 输入记录
	 * @return 输出记录
	 * @throws DataAccessException 如果出现任何问题
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record)
	 */
	@Nullable
	public Record execute(Record inputRecord) throws DataAccessException {
		InteractionSpec interactionSpec = getInteractionSpec();
		Assert.state(interactionSpec != null, "No InteractionSpec set");
		return getCciTemplate().execute(interactionSpec, inputRecord);
	}

	/**
	 * 执行此操作对象封装的 CCI 交互。
	 * <p>此方法将调用 CCI 的 {@code Interaction.execute} 并传入输出 Record 的变体。
	 * @param inputRecord 输入记录
	 * @param outputRecord 输出记录
	 * @throws DataAccessException 如果出现任何问题
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record, Record)
	 */
	public void execute(Record inputRecord, Record outputRecord) throws DataAccessException {
		InteractionSpec interactionSpec = getInteractionSpec();
		Assert.state(interactionSpec != null, "No InteractionSpec set");
		getCciTemplate().execute(interactionSpec, inputRecord, outputRecord);
	}

}
