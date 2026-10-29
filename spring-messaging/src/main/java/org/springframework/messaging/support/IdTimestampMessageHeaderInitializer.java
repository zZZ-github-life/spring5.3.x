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

package org.springframework.messaging.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

/**
 * 用于自定义 ID 和 TIMESTAMP 消息头部生成策略的
 * {@link org.springframework.messaging.support.MessageHeaderInitializer MessageHeaderInitializer}。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class IdTimestampMessageHeaderInitializer implements MessageHeaderInitializer {

	private static final IdGenerator ID_VALUE_NONE_GENERATOR = () -> MessageHeaders.ID_VALUE_NONE;


	@Nullable
	private IdGenerator idGenerator;

	private boolean enableTimestamp;


	/**
	 * 配置用于初始化 {@code MessageHeaderAccessor} 实例的 IdGenerator 策略。
	 * <p>默认情况下此属性设置为 {@code null}，此时使用
	 * {@link org.springframework.messaging.MessageHeaders} 的默认 IdGenerator。
	 * <p>若要完全不生成 id，请参阅 {@link #setDisableIdGeneration()}。
	 */
	public void setIdGenerator(@Nullable IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	/**
	 * 返回配置的 {@code IdGenerator}（如果有）。
	 */
	@Nullable
	public IdGenerator getIdGenerator() {
		return this.idGenerator;
	}

	/**
	 * 使用一种完全禁用 id 生成的 id 生成策略调用 {@link #setIdGenerator} 的快捷方式。
	 */
	public void setDisableIdGeneration() {
		this.idGenerator = ID_VALUE_NONE_GENERATOR;
	}

	/**
	 * 是否在初始化的 {@code MessageHeaderAccessor} 实例上自动添加
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP} 头部。
	 * <p>默认情况下此属性设置为 false。
	 */
	public void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * 返回时间戳头部是否已启用。
	 */
	public boolean isEnableTimestamp() {
		return this.enableTimestamp;
	}


	@Override
	public void initHeaders(MessageHeaderAccessor headerAccessor) {
		IdGenerator idGenerator = getIdGenerator();
		if (idGenerator != null) {
			headerAccessor.setIdGenerator(idGenerator);
		}
		headerAccessor.setEnableTimestamp(isEnableTimestamp());
	}

}
