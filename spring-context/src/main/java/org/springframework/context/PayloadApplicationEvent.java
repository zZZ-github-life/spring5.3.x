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

package org.springframework.context;

import java.util.function.Consumer;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.util.Assert;

/**
 * 携带任意载荷的 {@link ApplicationEvent}。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 * @param <T> 事件负载的类型
 * @see ApplicationEventPublisher#publishEvent(Object)
 * @see ApplicationListener#forPayload(Consumer)
 */
@SuppressWarnings("serial")
public class PayloadApplicationEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {

	private final T payload;


	/**
	 * 创建一个新的 PayloadApplicationEvent。
	 * @param source 事件最初发生的对象（永不为 {@code null}）
	 * @param payload 负载对象（永不为 {@code null}）
	 */
	public PayloadApplicationEvent(Object source, T payload) {
		super(source);
		Assert.notNull(payload, "Payload must not be null");
		this.payload = payload;
	}


	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getPayload()));
	}

	/**
	 * 返回事件的载荷。
	 */
	public T getPayload() {
		return this.payload;
	}

}
