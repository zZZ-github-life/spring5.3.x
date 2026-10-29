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

package org.springframework.orm.hibernate5;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.springframework.lang.Nullable;

/**
 * Hibernate 代码的回调接口。与 {@link HibernateTemplate} 的执行方法配合使用，
 * 通常作为方法实现中的匿名类。
 * 典型的实现会调用 {@code Session.load/find/update} 来对持久化对象执行
 * 某些操作。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @param <T> the result type
 * @see HibernateTemplate
 * @see HibernateTransactionManager
 */
@FunctionalInterface
public interface HibernateCallback<T> {

	/**
	 * 由 {@code HibernateTemplate.execute} 在存在活动 Hibernate {@code Session} 时调用。
	 * 无需关心 {@code Session} 的激活或关闭，也无需处理事务。
	 * <p>允许返回在回调中创建的结果对象，
	 * 即一个领域对象或一组领域对象。
	 * 抛出的自定义 RuntimeException 会被视为应用异常：
	 * 它将被传播给模板的调用方。
	 * @param session active Hibernate session
	 * @return a result object, or {@code null} if none
	 * @throws HibernateException if thrown by the Hibernate API
	 * @see HibernateTemplate#execute
	 */
	@Nullable
	T doInHibernate(Session session) throws HibernateException;

}
