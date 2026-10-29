/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.transaction.support;

/**
 * 资源持有者需要实现的通用接口。
 * 允许 Spring 的事务基础设施在必要时对持有者进行内省和重置。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see ResourceHolderSupport
 * @see ResourceHolderSynchronization
 */
public interface ResourceHolder {

	/**
	 * 重置此持有者的事务状态。
	 */
	void reset();

	/**
	 * 通知此持有者它已从事务同步中解绑。
	 */
	void unbound();

	/**
	 * 确定此持有者是否被视为"无效"，即作为前一个线程的残留。
	 */
	boolean isVoid();

}
