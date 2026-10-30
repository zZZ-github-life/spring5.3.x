/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * TransactionAttribute实现, 通过应用一系列正向和负向的回滚规则来确定给定异常是否应导致事务回滚。
 * 如果没有自定义回滚规则适用, 此属性的行为与DefaultTransactionAttribute相同(在运行时异常时回滚)。
 *
 * <p>{@link TransactionAttributeEditor}创建此类的对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 09.04.2003
 * @see TransactionAttributeEditor
 */
@SuppressWarnings("serial")
public class RuleBasedTransactionAttribute extends DefaultTransactionAttribute implements Serializable {

	/** 描述字符串中异常回滚规则的前缀。 */
	public static final String PREFIX_ROLLBACK_RULE = "-";

	/** 描述字符串中异常提交规则的前缀。 */
	public static final String PREFIX_COMMIT_RULE = "+";


	@Nullable
	private List<RollbackRuleAttribute> rollbackRules;


	/**
	 * 创建一个新的RuleBasedTransactionAttribute, 使用默认设置。
	 * 可以通过bean属性setter进行修改。
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 * @see #setRollbackRules
	 */
	public RuleBasedTransactionAttribute() {
		super();
	}

	/**
	 * 拷贝构造器。可以通过bean属性setter修改定义。
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 * @see #setRollbackRules
	 */
	public RuleBasedTransactionAttribute(RuleBasedTransactionAttribute other) {
		super(other);
		this.rollbackRules = (other.rollbackRules != null ? new ArrayList<>(other.rollbackRules) : null);
	}

	/**
	 * 使用给定的传播行为创建一个新的DefaultTransactionAttribute。
	 * 可以通过bean属性setter进行修改。
	 * @param propagationBehavior TransactionDefinition 接口中的某个传播常量
	 * @param rollbackRules 要应用的 RollbackRuleAttribute 列表
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public RuleBasedTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
		super(propagationBehavior);
		this.rollbackRules = rollbackRules;
	}


	/**
	 * 设置要应用的{@code RollbackRuleAttribute}对象(和/或{@code NoRollbackRuleAttribute}对象)列表。
	 * @see RollbackRuleAttribute
	 * @see NoRollbackRuleAttribute
	 */
	public void setRollbackRules(List<RollbackRuleAttribute> rollbackRules) {
		this.rollbackRules = rollbackRules;
	}

	/**
	 * 返回{@code RollbackRuleAttribute}对象列表(永不为{@code null})。
	 */
	public List<RollbackRuleAttribute> getRollbackRules() {
		if (this.rollbackRules == null) {
			this.rollbackRules = new ArrayList<>();
		}
		return this.rollbackRules;
	}


	/**
	 * 获胜的规则是最浅的规则(即在继承层次结构中离异常最近的规则)。
	 * 如果没有规则适用(-1), 则返回false。
	 * @see TransactionAttribute#rollbackOn(java.lang.Throwable)
	 */
	@Override
	public boolean rollbackOn(Throwable ex) {
		RollbackRuleAttribute winner = null;
		int deepest = Integer.MAX_VALUE;

		if (this.rollbackRules != null) {
			for (RollbackRuleAttribute rule : this.rollbackRules) {
				int depth = rule.getDepth(ex);
				if (depth >= 0 && depth < deepest) {
					deepest = depth;
					winner = rule;
				}
			}
		}

		// User superclass behavior (rollback on unchecked) if no rule matches.
		if (winner == null) {
			return super.rollbackOn(ex);
		}

		return !(winner instanceof NoRollbackRuleAttribute);
	}


	@Override
	public String toString() {
		StringBuilder result = getAttributeDescription();
		if (this.rollbackRules != null) {
			for (RollbackRuleAttribute rule : this.rollbackRules) {
				String sign = (rule instanceof NoRollbackRuleAttribute ? PREFIX_COMMIT_RULE : PREFIX_ROLLBACK_RULE);
				result.append(',').append(sign).append(rule.getExceptionName());
			}
		}
		return result.toString();
	}

}
