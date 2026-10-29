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

package org.springframework.transaction.interceptor;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@link TransactionAttribute}对象的PropertyEditor。接受如下格式的字符串:
 * <p>{@code PROPAGATION_NAME, ISOLATION_NAME, readOnly, timeout_NNNN,+Exception1,-Exception2}
 * <p>其中只需要传播代码。例如:
 * <p>{@code PROPAGATION_MANDATORY, ISOLATION_DEFAULT}
 *
 * <p>标记可以按<strong>任意</strong>顺序排列。传播和隔离代码必须使用TransactionDefinition类中常量的名称。
 * 超时值以秒为单位。如果未指定超时, 事务管理器将应用特定于该事务管理器的默认超时。
 *
 * <p>异常名称子字符串前的"+"表示即使抛出此异常事务也应提交; "-"表示应回滚。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 24.04.2003
 * @see org.springframework.transaction.TransactionDefinition
 * @see org.springframework.core.Constants
 */
public class TransactionAttributeEditor extends PropertyEditorSupport {

	/**
	 * 格式为PROPAGATION_NAME,ISOLATION_NAME,readOnly,timeout_NNNN,+Exception1,-Exception2。
	 * null或空字符串表示该方法是非事务性的。
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasLength(text)) {
			// tokenize it with ","
			String[] tokens = StringUtils.commaDelimitedListToStringArray(text);
			RuleBasedTransactionAttribute attr = new RuleBasedTransactionAttribute();
			for (String token : tokens) {
				// Trim leading and trailing whitespace.
				String trimmedToken = StringUtils.trimWhitespace(token.trim());
				// Check whether token contains illegal whitespace within text.
				if (StringUtils.containsWhitespace(trimmedToken)) {
					throw new IllegalArgumentException(
							"Transaction attribute token contains illegal whitespace: [" + trimmedToken + "]");
				}
				// Check token type.
				if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_PROPAGATION)) {
					attr.setPropagationBehaviorName(trimmedToken);
				}
				else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_ISOLATION)) {
					attr.setIsolationLevelName(trimmedToken);
				}
				else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_TIMEOUT)) {
					String value = trimmedToken.substring(DefaultTransactionAttribute.PREFIX_TIMEOUT.length());
					attr.setTimeoutString(value);
				}
				else if (trimmedToken.equals(RuleBasedTransactionAttribute.READ_ONLY_MARKER)) {
					attr.setReadOnly(true);
				}
				else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_COMMIT_RULE)) {
					attr.getRollbackRules().add(new NoRollbackRuleAttribute(trimmedToken.substring(1)));
				}
				else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_ROLLBACK_RULE)) {
					attr.getRollbackRules().add(new RollbackRuleAttribute(trimmedToken.substring(1)));
				}
				else {
					throw new IllegalArgumentException("Invalid transaction attribute token: [" + trimmedToken + "]");
				}
			}
			attr.resolveAttributeStrings(null);  // placeholders expected to be pre-resolved
			setValue(attr);
		}
		else {
			setValue(null);
		}
	}

}
