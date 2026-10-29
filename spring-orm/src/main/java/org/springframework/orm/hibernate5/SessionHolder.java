/*
 * Copyright 2002-2020 the original author or authors.
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

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerHolder;

/**
 * 包装 Hibernate {@link Session}（以及可选的 {@link Transaction}）的资源持有器。
 * {@link HibernateTransactionManager} 会将此类的实例绑定到线程，
 * 针对给定的 {@link org.hibernate.SessionFactory}。自 5.1 起继承 {@link EntityManagerHolder}，
 * 在 Hibernate 5.2+ 上自动暴露 {@code EntityManager} 句柄。
 *
 * <p>注意：这是一个 SPI 类，不供应用程序使用。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see HibernateTransactionManager
 * @see SessionFactoryUtils
 */
public class SessionHolder extends EntityManagerHolder {

	@Nullable
	private Transaction transaction;

	@Nullable
	private FlushMode previousFlushMode;


	public SessionHolder(Session session) {
		super(session);
	}


	public Session getSession() {
		return (Session) getEntityManager();
	}

	public void setTransaction(@Nullable Transaction transaction) {
		this.transaction = transaction;
		setTransactionActive(transaction != null);
	}

	@Nullable
	public Transaction getTransaction() {
		return this.transaction;
	}

	public void setPreviousFlushMode(@Nullable FlushMode previousFlushMode) {
		this.previousFlushMode = previousFlushMode;
	}

	@Nullable
	public FlushMode getPreviousFlushMode() {
		return this.previousFlushMode;
	}


	@Override
	public void clear() {
		super.clear();
		this.transaction = null;
		this.previousFlushMode = null;
	}

}
