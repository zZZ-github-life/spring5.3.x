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

package org.springframework.transaction.support;

import java.io.Serializable;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;

/**
 * {@link TransactionDefinition} 接口的默认实现，
 * 提供 bean 风格的配置和合理的默认值
 * （PROPAGATION_REQUIRED，ISOLATION_DEFAULT，TIMEOUT_DEFAULT，readOnly=false）。
 *
 * <p>作为 {@link TransactionTemplate} 和
 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute} 的基类。
 *
 * @author Juergen Hoeller
 * @since 08.05.2003
 */
@SuppressWarnings("serial")
public class DefaultTransactionDefinition implements TransactionDefinition, Serializable {

	/** TransactionDefinition 中定义的传播常量前缀。 */
	public static final String PREFIX_PROPAGATION = "PROPAGATION_";

	/** TransactionDefinition 中定义的隔离常量前缀。 */
	public static final String PREFIX_ISOLATION = "ISOLATION_";

	/** 描述字符串中事务超时值的前缀。 */
	public static final String PREFIX_TIMEOUT = "timeout_";

	/** 描述字符串中只读事务的标记。 */
	public static final String READ_ONLY_MARKER = "readOnly";


	/** TransactionDefinition 的常量实例。 */
	static final Constants constants = new Constants(TransactionDefinition.class);

	private int propagationBehavior = PROPAGATION_REQUIRED;

	private int isolationLevel = ISOLATION_DEFAULT;

	private int timeout = TIMEOUT_DEFAULT;

	private boolean readOnly = false;

	@Nullable
	private String name;


	/**
	 * 创建新的 DefaultTransactionDefinition，使用默认设置。
	 * 可以通过 bean 属性修改器进行修改。
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionDefinition() {
	}

	/**
	 * 拷贝构造函数。定义可以通过 bean 属性修改器进行修改。
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionDefinition(TransactionDefinition other) {
		this.propagationBehavior = other.getPropagationBehavior();
		this.isolationLevel = other.getIsolationLevel();
		this.timeout = other.getTimeout();
		this.readOnly = other.isReadOnly();
		this.name = other.getName();
	}

	/**
	 * 使用给定的传播行为创建新的 DefaultTransactionDefinition。
	 * 可以通过 bean 属性修改器进行修改。
	 * @param propagationBehavior TransactionDefinition 接口中的传播常量之一
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionDefinition(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}


	/**
	 * 通过 TransactionDefinition 中对应常量的名称设置传播行为，例如 "PROPAGATION_REQUIRED"。
	 * @param constantName 常量名称
	 * @throws IllegalArgumentException 如果提供的值无法解析为 {@code PROPAGATION_} 常量之一或为 {@code null}
	 * @see #setPropagationBehavior
	 * @see #PROPAGATION_REQUIRED
	 */
	public final void setPropagationBehaviorName(String constantName) throws IllegalArgumentException {
		if (!constantName.startsWith(PREFIX_PROPAGATION)) {
			throw new IllegalArgumentException("Only propagation constants allowed");
		}
		setPropagationBehavior(constants.asNumber(constantName).intValue());
	}

	/**
	 * 设置传播行为。必须是 TransactionDefinition 接口中的传播常量之一。默认为 PROPAGATION_REQUIRED。
	 * <p>专为 {@link #PROPAGATION_REQUIRED} 或 {@link #PROPAGATION_REQUIRES_NEW} 设计，
	 * 因为它仅适用于新启动的事务。如果您希望在参与具有不同隔离级别的现有事务时
	 * 拒绝隔离级别声明，请考虑将事务管理器上的 "validateExistingTransactions" 标志切换为 "true"。
	 * <p>请注意，不支持自定义隔离级别的事务管理器在给定 {@link #ISOLATION_DEFAULT}
	 * 以外的任何级别时将抛出异常。
	 * @throws IllegalArgumentException 如果提供的值不是 {@code PROPAGATION_} 常量之一
	 * @see #PROPAGATION_REQUIRED
	 */
	public final void setPropagationBehavior(int propagationBehavior) {
		if (!constants.getValues(PREFIX_PROPAGATION).contains(propagationBehavior)) {
			throw new IllegalArgumentException("Only values of propagation constants allowed");
		}
		this.propagationBehavior = propagationBehavior;
	}

	@Override
	public final int getPropagationBehavior() {
		return this.propagationBehavior;
	}

	/**
	 * 通过 TransactionDefinition 中对应常量的名称设置隔离级别，例如 "ISOLATION_DEFAULT"。
	 * @param constantName 常量名称
	 * @throws IllegalArgumentException 如果提供的值无法解析为 {@code ISOLATION_} 常量之一或为 {@code null}
	 * @see #setIsolationLevel
	 * @see #ISOLATION_DEFAULT
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (!constantName.startsWith(PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * 设置隔离级别。必须是 TransactionDefinition 接口中的隔离常量之一。默认为 ISOLATION_DEFAULT。
	 * <p>专为 {@link #PROPAGATION_REQUIRED} 或 {@link #PROPAGATION_REQUIRES_NEW} 设计，
	 * 因为它仅适用于新启动的事务。如果您希望在参与具有不同隔离级别的现有事务时
	 * 拒绝隔离级别声明，请考虑将事务管理器上的 "validateExistingTransactions" 标志切换为 "true"。
	 * <p>请注意，不支持自定义隔离级别的事务管理器在给定 {@link #ISOLATION_DEFAULT}
	 * 以外的任何级别时将抛出异常。
	 * @throws IllegalArgumentException 如果提供的值不是 {@code ISOLATION_} 常量之一
	 * @see #ISOLATION_DEFAULT
	 */
	public final void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(PREFIX_ISOLATION).contains(isolationLevel)) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = isolationLevel;
	}

	@Override
	public final int getIsolationLevel() {
		return this.isolationLevel;
	}

	/**
	 * 设置要应用的超时时间，以秒为单位。默认为 TIMEOUT_DEFAULT（-1）。
	 * <p>专为 {@link #PROPAGATION_REQUIRED} 或 {@link #PROPAGATION_REQUIRES_NEW} 设计，
	 * 因为它仅适用于新启动的事务。
	 * <p>请注意，不支持超时的事务管理器在给定 {@link #TIMEOUT_DEFAULT}
	 * 以外的任何超时值时将抛出异常。
	 * @see #TIMEOUT_DEFAULT
	 */
	public final void setTimeout(int timeout) {
		if (timeout < TIMEOUT_DEFAULT) {
			throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
		}
		this.timeout = timeout;
	}

	@Override
	public final int getTimeout() {
		return this.timeout;
	}

	/**
	 * 设置是否优化为只读事务。默认为 "false"。
	 * <p>只读标志适用于任何事务上下文，无论是由实际资源事务支持
	 * （{@link #PROPAGATION_REQUIRED}/{@link #PROPAGATION_REQUIRES_NEW}）
	 * 还是在资源级别以非事务方式运行（{@link #PROPAGATION_SUPPORTS}）。
	 * 在后一种情况下，该标志仅适用于应用程序内的托管资源，如 Hibernate {@code Session}。
	 * <p>这仅作为实际事务子系统的提示；它<i>不一定</i>会导致写访问尝试失败。
	 * 无法解读只读提示的事务管理器在被要求只读事务时<i>不会</i>抛出异常。
	 */
	public final void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public final boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 设置此事务的名称。默认为无。
	 * <p>这将用作事务监视器中显示的事务名称（如果适用，例如 WebLogic 的）。
	 */
	public final void setName(String name) {
		this.name = name;
	}

	@Override
	@Nullable
	public final String getName() {
		return this.name;
	}


	/**
	 * 此实现比较 {@code toString()} 的结果。
	 * @see #toString()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof TransactionDefinition && toString().equals(other.toString())));
	}

	/**
	 * 此实现返回 {@code toString()} 的哈希码。
	 * @see #toString()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * 返回此事务定义的标识性描述。
	 * <p>格式与 {@link org.springframework.transaction.interceptor.TransactionAttributeEditor}
	 * 使用的格式匹配，以便能够将 {@code toString} 结果输入到
	 * {@link org.springframework.transaction.interceptor.TransactionAttribute} 类型的 bean 属性中。
	 * <p>必须在子类中重写以实现正确的 {@code equals} 和 {@code hashCode} 行为。
	 * 或者，可以重写 {@link #equals} 和 {@link #hashCode} 本身。
	 * @see #getDefinitionDescription()
	 * @see org.springframework.transaction.interceptor.TransactionAttributeEditor
	 */
	@Override
	public String toString() {
		return getDefinitionDescription().toString();
	}

	/**
	 * 返回此事务定义的标识性描述。
	 * <p>供子类使用，包含在其 {@code toString()} 结果中。
	 */
	protected final StringBuilder getDefinitionDescription() {
		StringBuilder result = new StringBuilder();
		result.append(constants.toCode(this.propagationBehavior, PREFIX_PROPAGATION));
		result.append(',');
		result.append(constants.toCode(this.isolationLevel, PREFIX_ISOLATION));
		if (this.timeout != TIMEOUT_DEFAULT) {
			result.append(',');
			result.append(PREFIX_TIMEOUT).append(this.timeout);
		}
		if (this.readOnly) {
			result.append(',');
			result.append(READ_ONLY_MARKER);
		}
		return result;
	}

}
