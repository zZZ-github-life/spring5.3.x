/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;

import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * 用于创建适合处理传入 {@link org.springframework.messaging.Message} 的
 * {@link InvocableHandlerMethod} 的工厂。
 *
 * <p>通常由需要灵活方法签名的监听器端点使用。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface MessageHandlerMethodFactory {

	/**
	 * 创建能够处理指定方法端点的 {@link InvocableHandlerMethod}。
	 * @param bean Bean 实例
	 * @param method 要调用的方法
	 * @return 适用于该方法的 {@link InvocableHandlerMethod}
	 */
	InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method);

}
