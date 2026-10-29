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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Spring通用的事务属性实现。
 * 默认情况下, 在运行时异常(而非受检异常)时回滚。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 16.03.2003
 */
@SuppressWarnings("serial")
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

	@Nullable
	private String descriptor;

	@Nullable
	private String timeoutString;

	@Nullable
	private String qualifier;

	private Collection<String> labels = Collections.emptyList();


	/**
	 * 创建一个新的DefaultTransactionAttribute, 使用默认设置。
	 * 可以通过bean属性setter进行修改。
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionAttribute() {
		super();
	}

	/**
	 * 拷贝构造器。定义可以通过bean属性setter进行修改。
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionAttribute(TransactionAttribute other) {
		super(other);
	}

	/**
	 * 使用给定的传播行为创建一个新的DefaultTransactionAttribute。
	 * 可以通过bean属性setter进行修改。
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}


	/**
	 * 设置此事务属性的描述符, 例如指示该属性应用的位置。
	 * @since 4.3.4
	 */
	public void setDescriptor(@Nullable String descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * 返回此事务属性的描述符, 如果没有则返回{@code null}。
	 * @since 4.3.4
	 */
	@Nullable
	public String getDescriptor() {
		return this.descriptor;
	}

	/**
	 * 设置要应用的超时时间(如果有), 作为可解析为秒数的字符串值。
	 * @since 5.3
	 * @see #setTimeout
	 * @see #resolveAttributeStrings
	 */
	public void setTimeoutString(@Nullable String timeoutString) {
		this.timeoutString = timeoutString;
	}

	/**
	 * 返回要应用的超时时间(如果有), 作为可解析为秒数的字符串值。
	 * @since 5.3
	 * @see #getTimeout
	 * @see #resolveAttributeStrings
	 */
	@Nullable
	public String getTimeoutString() {
		return this.timeoutString;
	}

	/**
	 * 将限定符值与此事务属性关联。
	 * <p>这可以用于选择相应的事务管理器来处理此特定事务。
	 * @since 3.0
	 * @see #resolveAttributeStrings
	 */
	public void setQualifier(@Nullable String qualifier) {
		this.qualifier = qualifier;
	}

	/**
	 * 返回与此事务属性关联的限定符值。
	 * @since 3.0
	 */
	@Override
	@Nullable
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * 将一个或多个标签与此事务属性关联。
	 * <p>这可以用于应用特定的事务行为, 或者纯粹用于描述性目的。
	 * @since 5.3
	 * @see #resolveAttributeStrings
	 */
	public void setLabels(Collection<String> labels) {
		this.labels = labels;
	}

	@Override
	public Collection<String> getLabels() {
		return this.labels;
	}

	/**
	 * 默认行为与EJB类似: 在出现未受检异常({@link RuntimeException})时回滚,
	 * 假定这是超出任何业务规则之外的意外结果。此外, 我们还会尝试在出现{@link Error}时回滚,
	 * 因为这也明显是一个意外结果。相比之下, 受检异常被视为业务异常,
	 * 因此是事务性业务方法的正常预期结果, 即一种仍然允许资源操作正常完成的替代返回值。
	 * <p>这与TransactionTemplate的默认行为基本一致,
	 * 只是TransactionTemplate也会在出现未声明的受检异常(一种边缘情况)时回滚。
	 * 对于声明式事务, 我们期望受检异常被有意声明为业务异常, 默认情况下会提交。
	 * @see org.springframework.transaction.support.TransactionTemplate#execute
	 */
	@Override
	public boolean rollbackOn(Throwable ex) {
		return (ex instanceof RuntimeException || ex instanceof Error);
	}


	/**
	 * 解析定义为可解析字符串的属性值:
	 * {@link #setTimeoutString}、{@link #setQualifier}、{@link #setLabels}。
	 * 这通常用于解析"${...}"占位符。
	 * @param resolver the embedded value resolver to apply, if any
	 * @since 5.3
	 */
	public void resolveAttributeStrings(@Nullable StringValueResolver resolver) {
		String timeoutString = this.timeoutString;
		if (StringUtils.hasText(timeoutString)) {
			if (resolver != null) {
				timeoutString = resolver.resolveStringValue(timeoutString);
			}
			if (StringUtils.hasLength(timeoutString)) {
				try {
					setTimeout(Integer.parseInt(timeoutString));
				}
				catch (RuntimeException ex) {
					throw new IllegalArgumentException(
							"Invalid timeoutString value \"" + timeoutString + "\" - cannot parse into int");
				}
			}
		}

		if (resolver != null) {
			if (this.qualifier != null) {
				this.qualifier = resolver.resolveStringValue(this.qualifier);
			}
			Set<String> resolvedLabels = new LinkedHashSet<>(this.labels.size());
			for (String label : this.labels) {
				resolvedLabels.add(resolver.resolveStringValue(label));
			}
			this.labels = resolvedLabels;
		}
	}

	/**
	 * 返回此事务属性的标识描述。
	 * <p>可供子类使用, 包含在其{@code toString()}结果中。
	 */
	protected final StringBuilder getAttributeDescription() {
		StringBuilder result = getDefinitionDescription();
		if (StringUtils.hasText(this.qualifier)) {
			result.append("; '").append(this.qualifier).append('\'');
		}
		if (!this.labels.isEmpty()) {
			result.append("; ").append(this.labels);
		}
		return result;
	}

}
