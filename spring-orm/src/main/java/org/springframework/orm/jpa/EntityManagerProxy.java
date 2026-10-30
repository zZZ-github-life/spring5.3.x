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

package org.springframework.orm.jpa;

import javax.persistence.EntityManager;

/**
 * {@link javax.persistence.EntityManager} 的子接口，由 EntityManager 代理实现。
 * 允许访问底层的目标 EntityManager。
 *
 * <p>此接口主要供框架使用。应用程序代码应优先使用
 * {@link javax.persistence.EntityManager#getDelegate()} 方法来访问
 * 底层资源的原生功能。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface EntityManagerProxy extends EntityManager {

	/**
	 * 返回此代理将委托给的底层 EntityManager。
	 * <p>对于扩展的 EntityManager，这将是关联的原始 EntityManager。
	 * <p>对于共享的（"事务性"）EntityManager，这将是当前与事务关联的
	 * 原始 EntityManager。在事务之外调用时，将抛出 IllegalStateException。
	 * @return 底层的原始 EntityManager（永不为 {@code null}）
	 * @throws IllegalStateException 如果没有可用的底层 EntityManager
	 */
	EntityManager getTargetEntityManager() throws IllegalStateException;

}
