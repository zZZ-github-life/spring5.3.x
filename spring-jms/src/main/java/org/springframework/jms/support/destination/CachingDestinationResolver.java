/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jms.support.destination;

/**
 * DestinationResolver 接口的扩展，公开了用于清空缓存的方法。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface CachingDestinationResolver extends DestinationResolver {

	/**
	 * 从缓存中移除具有给定名称的目的地（如果此解析器曾对其进行缓存）。
	 * <p>在对指定目的地的访问失败时调用，假定该 JMS Destination 对象可能已失效。
	 * @param destinationName 目标的名称
	 */
	void removeFromCache(String destinationName);

	/**
	 * 清空整个目的地缓存。
	 * <p>在 JMS 提供者整体发生故障时调用。
	 */
	void clearCache();

}
