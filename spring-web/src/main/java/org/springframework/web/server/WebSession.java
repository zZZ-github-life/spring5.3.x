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

package org.springframework.web.server;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 使用服务器端会话的主要契约，可跨 HTTP 请求访问会话属性。
 *
 * <p>创建 {@code WebSession} 实例并不会自动启动会话，
 * 因此也不会将会话 id 发送给客户端（通常通过 Cookie）。
 * 会话会在添加会话属性时隐式启动。
 * 也可以通过 {@link #start()} 显式创建会话。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSession {

	/**
	 * 返回唯一的会话标识符。
	 */
	String getId();

	/**
	 * 返回保存会话属性的 Map。
	 */
	Map<String, Object> getAttributes();

	/**
	 * 如果存在，返回会话属性值。
	 * @param name 属性名称
	 * @param <T> 属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T getAttribute(String name) {
		return (T) getAttributes().get(name);
	}

	/**
	 * 返回会话属性值，如果不存在则抛出
	 * {@link IllegalArgumentException}。
	 * @param name 属性名称
	 * @param <T> 属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	default <T> T getRequiredAttribute(String name) {
		T value = getAttribute(name);
		Assert.notNull(value, () -> "Required attribute '" + name + "' is missing.");
		return value;
	}

	/**
	 * 返回会话属性值，或返回一个默认的后备值。
	 * @param name 属性名称
	 * @param defaultValue 作为替代返回的默认值
	 * @param <T> 属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	default <T> T getAttributeOrDefault(String name, T defaultValue) {
		return (T) getAttributes().getOrDefault(name, defaultValue);
	}

	/**
	 * 强制创建会话，使得在调用 {@link #save()} 时发送会话 id。
	 */
	void start();

	/**
	 * 与客户端之间的会话是否已通过 {@link #start()} 显式启动，
	 * 或通过添加会话属性隐式启动。
	 * 如果为 "false"，则不会将会话 id 发送给客户端，
	 * 并且 {@link #save()} 方法实际上是一个空操作。
	 */
	boolean isStarted();

	/**
	 * 为会话生成新的 id，并更新底层会话存储以反映新的 id。
	 * 调用成功后，{@link #getId()} 将反映新的会话 id。
	 * @return 完成通知（成功或错误）
	 */
	Mono<Void> changeSessionId();

	/**
	 * 使当前会话失效并清除会话存储。
	 * @return 完成通知（成功或错误）
	 */
	Mono<Void> invalidate();

	/**
	 * 按如下方式通过 {@code WebSessionStore} 保存会话：
	 * <ul>
	 * <li>如果会话是新的（即已创建但从未持久化），则必须已通过 {@link #start()}
	 * 显式启动，或通过添加属性隐式启动，否则此方法不应产生任何效果。
	 * <li>如果会话是通过 {@code WebSessionStore} 检索到的，
	 * 则此方法的实现必须检查会话是否已被 {@link #invalidate() 失效}，
	 * 若是则返回错误。
	 * </ul>
	 * <p>请注意，此方法不打算供应用直接使用。
	 * 相反，它会在响应提交之前被自动调用。
	 * @return 用于指示成功或错误完成的 {@code Mono}
	 */
	Mono<Void> save();

	/**
	 * 如果会话在 {@link #getMaxIdleTime() maxIdleTime} 过后已过期，
	 * 则返回 {@code true}。
	 * <p>通常应在处理请求开始时自动进行过期检查：访问会话时会自动检查，
	 * 必要时创建新的 {@code WebSession} 实例，这样应用默认就不必担心
	 * 会话过期的问题。
	 */
	boolean isExpired();

	/**
	 * 返回会话创建的时间。
	 */
	Instant getCreationTime();

	/**
	 * 返回因用户活动（例如 HTTP 请求）而导致会话最后一次被访问的时间。
	 * 与 {@link #getMaxIdleTime() maxIdleTimeInSeconds} 结合使用，
	 * 有助于确定会话何时{@link #isExpired() 过期}。
	 */
	Instant getLastAccessTime();

	/**
	 * 配置会话被视为过期之前，在
	 * {@link #getLastAccessTime() lastAccessTime} 之后可能经过的最长时间。
	 * 负值表示会话不应过期。
	 */
	void setMaxIdleTime(Duration maxIdleTime);

	/**
	 * 返回在 {@link #getLastAccessTime() lastAccessTime} 之后、
	 * 会话过期之前的最长时间。负值表示会话不会过期。
	 */
	Duration getMaxIdleTime();

}
