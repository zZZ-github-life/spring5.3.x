/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

/**
 * 需要与底层资源被视为相等的透明资源代理所要实现的接口，例如用于一致的查找键比较。请注意，该接口确
 * 实蕴含此类特殊语义，并不构成通用的 mixin！
 *
 * <p>此类包装器会在
 * {@link org.springframework.transaction.support.TransactionSynchronizationManager} 中进行键比较时
 * 自动被解包。
 *
 * <p>只应完全透明的代理（例如用于重定向或服务查找）实现此接口。用新行为装饰目标对象的代理（例如
 * AOP 代理）在这里 <i>不</i> 符合条件！
 *
 * @author Juergen Hoeller
 * @since 2.5.4
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public interface InfrastructureProxy {

	/**
	 * 返回底层资源（永不为 {@code null}）。
	 */
	Object getWrappedObject();

}
