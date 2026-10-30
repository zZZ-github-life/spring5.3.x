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

package org.springframework.messaging.simp;

import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于访问与 SiMP 会话（例如 WebSocket 会话）关联属性的包装类。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class SimpAttributes {

	/** 互斥会话属性的键。 */
	public static final String SESSION_MUTEX_NAME = SimpAttributes.class.getName() + ".MUTEX";

	/** 会话完成后设置的键。 */
	public static final String SESSION_COMPLETED_NAME = SimpAttributes.class.getName() + ".COMPLETED";

	/** 用于存储销毁回调的会话属性名称前缀。 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			SimpAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";

	private static final Log logger = SimpLogging.forLogName(SimpAttributes.class);


	private final String sessionId;

	private final Map<String, Object> attributes;


	/**
	 * 包装给定会话属性映射的构造函数。
	 * @param sessionId 关联会话的 id
	 * @param attributes 属性
	 */
	public SimpAttributes(String sessionId, Map<String, Object> attributes) {
		Assert.notNull(sessionId, "'sessionId' is required");
		Assert.notNull(attributes, "'attributes' is required");
		this.sessionId = sessionId;
		this.attributes = attributes;
	}


	/**
	 * 返回给定名称属性的值（如果有）。
	 * @param name 属性的名称
	 * @return 当前属性值，如果未找到则为 {@code null}
	 */
	@Nullable
	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	/**
	 * 设置给定名称的值，并替换现有值（如果有）。
	 * @param name 属性的名称
	 * @param value 属性的值
	 */
	public void setAttribute(String name, Object value) {
		this.attributes.put(name, value);
	}

	/**
	 * 移除给定名称的属性（如果存在）。
	 * <p>还会移除为指定属性注册的销毁回调（如果有）。但它<i>不会</i>执行该回调。
	 * 这里假定被移除的对象仍将继续使用，并在适当的时候独立销毁。
	 * @param name 属性的名称
	 */
	public void removeAttribute(String name) {
		this.attributes.remove(name);
		removeDestructionCallback(name);
	}

	/**
	 * 获取所有属性的名称。
	 * @return 属性名称的 String 数组，永不为 {@code null}
	 */
	public String[] getAttributeNames() {
		return StringUtils.toStringArray(this.attributes.keySet());
	}

	/**
	 * 注册在指定属性销毁时执行的回调。会话关闭时会执行该回调。
	 * @param name 要为其注册回调的属性名称
	 * @param callback 要执行的销毁回调
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (getSessionMutex()) {
			if (isSessionCompleted()) {
				throw new IllegalStateException("Session id=" + getSessionId() + " already completed");
			}
			this.attributes.put(DESTRUCTION_CALLBACK_NAME_PREFIX + name, callback);
		}
	}

	private void removeDestructionCallback(String name) {
		synchronized (getSessionMutex()) {
			this.attributes.remove(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
		}
	}

	/**
	 * 返回关联会话的 ID。
	 * @return 会话 id 的 String 形式（永不为 {@code null}）
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * 公开用于底层会话同步的对象。
	 * @return 要使用的会话互斥体（永不为 {@code null}）
	 */
	public Object getSessionMutex() {
		Object mutex = this.attributes.get(SESSION_MUTEX_NAME);
		if (mutex == null) {
			mutex = this.attributes;
		}
		return mutex;
	}

	/**
	 * {@link #sessionCompleted()} 是否已被调用。
	 */
	public boolean isSessionCompleted() {
		return (this.attributes.get(SESSION_COMPLETED_NAME) != null);
	}

	/**
	 * 会话完成时调用。执行完成回调。
	 */
	public void sessionCompleted() {
		synchronized (getSessionMutex()) {
			if (!isSessionCompleted()) {
				executeDestructionCallbacks();
				this.attributes.put(SESSION_COMPLETED_NAME, Boolean.TRUE);
			}
		}
	}

	private void executeDestructionCallbacks() {
		this.attributes.forEach((key, value) -> {
			if (key.startsWith(DESTRUCTION_CALLBACK_NAME_PREFIX)) {
				try {
					((Runnable) value).run();
				}
				catch (Throwable ex) {
					logger.error("Uncaught error in session attribute destruction callback", ex);
				}
			}
		});
	}


	/**
	 * 从给定消息中提取 SiMP 会话属性，并将其包装在 {@link SimpAttributes} 实例中。
	 * @param message 要从中提取会话属性的消息
	 */
	public static SimpAttributes fromMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		Map<String, Object> sessionAttributes = SimpMessageHeaderAccessor.getSessionAttributes(headers);
		if (sessionId == null) {
			throw new IllegalStateException("No session id in " + message);
		}
		if (sessionAttributes == null) {
			throw new IllegalStateException("No session attributes in " + message);
		}
		return new SimpAttributes(sessionId, sessionAttributes);
	}

}
