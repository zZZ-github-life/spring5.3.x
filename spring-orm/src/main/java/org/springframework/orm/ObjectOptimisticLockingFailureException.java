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

package org.springframework.orm;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.lang.Nullable;

/**
 * 当映射对象发生乐观锁冲突时抛出的异常。
 * 提供有关持久化类和标识符的信息。
 *
 * @author Juergen Hoeller
 * @since 13.10.2003
 */
@SuppressWarnings("serial")
public class ObjectOptimisticLockingFailureException extends OptimisticLockingFailureException {

	@Nullable
	private final Object persistentClass;

	@Nullable
	private final Object identifier;


	/**
	 * 使用给定消息创建一个通用的 ObjectOptimisticLockingFailureException，
	 * 不包含任何受影响对象的信息。
	 * @param msg 详细消息
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(String msg, Throwable cause) {
		super(msg, cause);
		this.persistentClass = null;
		this.identifier = null;
	}

	/**
	 * 为给定对象创建一个新的 ObjectOptimisticLockingFailureException，
	 * 使用默认的 "optimistic locking failed" 消息。
	 * @param persistentClass 持久化类
	 * @param identifier 加锁失败的对象 ID
	 */
	public ObjectOptimisticLockingFailureException(Class<?> persistentClass, Object identifier) {
		this(persistentClass, identifier, null);
	}

	/**
	 * 为给定对象创建一个新的 ObjectOptimisticLockingFailureException，
	 * 使用默认的 "optimistic locking failed" 消息。
	 * @param persistentClass 持久化类
	 * @param identifier 加锁失败的对象 ID
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			Class<?> persistentClass, Object identifier, @Nullable Throwable cause) {

		this(persistentClass, identifier,
				"Object of class [" + persistentClass.getName() + "] with identifier [" + identifier +
				"]: optimistic locking failed", cause);
	}

	/**
	 * 为给定对象创建一个新的 ObjectOptimisticLockingFailureException，
	 * 使用给定的显式消息。
	 * @param persistentClass 持久化类
	 * @param identifier 加锁失败的对象 ID
	 * @param msg 详细消息
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			Class<?> persistentClass, Object identifier, String msg, @Nullable Throwable cause) {

		super(msg, cause);
		this.persistentClass = persistentClass;
		this.identifier = identifier;
	}

	/**
	 * 为给定对象创建一个新的 ObjectOptimisticLockingFailureException，
	 * 使用默认的 "optimistic locking failed" 消息。
	 * @param persistentClassName 持久化类的名称
	 * @param identifier 加锁失败的对象 ID
	 */
	public ObjectOptimisticLockingFailureException(String persistentClassName, Object identifier) {
		this(persistentClassName, identifier, null);
	}

	/**
	 * 为给定对象创建一个新的 ObjectOptimisticLockingFailureException，
	 * 使用默认的 "optimistic locking failed" 消息。
	 * @param persistentClassName 持久化类的名称
	 * @param identifier 加锁失败的对象 ID
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			String persistentClassName, Object identifier, @Nullable Throwable cause) {

		this(persistentClassName, identifier,
				"Object of class [" + persistentClassName + "] with identifier [" + identifier +
				"]: optimistic locking failed", cause);
	}

	/**
	 * 为给定对象创建一个新的 ObjectOptimisticLockingFailureException，
	 * 使用给定的显式消息。
	 * @param persistentClassName 持久化类的名称
	 * @param identifier 加锁失败的对象 ID
	 * @param msg 详细消息
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			String persistentClassName, Object identifier, String msg, @Nullable Throwable cause) {

		super(msg, cause);
		this.persistentClass = persistentClassName;
		this.identifier = identifier;
	}


	/**
	 * 返回锁定失败对象所属的持久化类。
	 * 如果未指定 Class，则此方法返回 null。
	 */
	@Nullable
	public Class<?> getPersistentClass() {
		return (this.persistentClass instanceof Class ? (Class<?>) this.persistentClass : null);
	}

	/**
	 * 返回锁定失败对象所属的持久化类的名称。
	 * 适用于 Class 对象和 String 名称两种形式。
	 */
	@Nullable
	public String getPersistentClassName() {
		if (this.persistentClass instanceof Class) {
			return ((Class<?>) this.persistentClass).getName();
		}
		return (this.persistentClass != null ? this.persistentClass.toString() : null);
	}

	/**
	 * 返回锁定失败对象的标识符。
	 */
	@Nullable
	public Object getIdentifier() {
		return this.identifier;
	}

}
