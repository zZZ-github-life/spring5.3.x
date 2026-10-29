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

package org.springframework.messaging.support;

import java.util.List;

/**
 * 维护 {@link org.springframework.messaging.support.ChannelInterceptor
 * ChannelInterceptors} 列表并允许拦截消息发送的
 * {@link org.springframework.messaging.MessageChannel MessageChannel}。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface InterceptableChannel {

	/**
	 * 设置通道拦截器列表，清除所有现有拦截器。
	 */
	void setInterceptors(List<ChannelInterceptor> interceptors);

	/**
	 * 将通道拦截器添加到列表末尾。
	 */
	void addInterceptor(ChannelInterceptor interceptor);

	/**
	 * 在指定索引处添加通道拦截器。
	 */
	void addInterceptor(int index, ChannelInterceptor interceptor);

	/**
	 * 返回配置的拦截器列表。
	 */
	List<ChannelInterceptor> getInterceptors();

	/**
	 * 移除给定的拦截器。
	 */
	boolean removeInterceptor(ChannelInterceptor interceptor);

	/**
	 * 移除指定索引处的拦截器。
	 */
	ChannelInterceptor removeInterceptor(int index);

}
