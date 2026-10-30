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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringValueResolver;

/**
 * 简单的{@link TransactionAttributeSource}实现, 允许按注册名称匹配属性。
 *
 * @author Juergen Hoeller
 * @since 21.08.2003
 * @see #isMatch
 * @see MethodMapTransactionAttributeSource
 */
@SuppressWarnings("serial")
public class NameMatchTransactionAttributeSource
		implements TransactionAttributeSource, EmbeddedValueResolverAware, InitializingBean, Serializable {

	/**
	 * 子类可用的日志记录器。
	 * <p>静态字段以优化序列化。
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchTransactionAttributeSource.class);

	/** 键为方法名; 值为TransactionAttribute。 */
	private final Map<String, TransactionAttribute> nameMap = new HashMap<>();

	@Nullable
	private StringValueResolver embeddedValueResolver;


	/**
	 * 设置名称/属性映射, 由方法名(例如"myMethod")和{@link TransactionAttribute}实例组成。
	 * @see #setProperties
	 * @see TransactionAttribute
	 */
	public void setNameMap(Map<String, TransactionAttribute> nameMap) {
		nameMap.forEach(this::addTransactionalMethod);
	}

	/**
	 * 将给定的属性解析为名称/属性映射。
	 * <p>期望方法名作为键, 字符串属性定义作为值,
	 * 可通过{@link TransactionAttributeEditor}解析为{@link TransactionAttribute}实例。
	 * @see #setNameMap
	 * @see TransactionAttributeEditor
	 */
	public void setProperties(Properties transactionAttributes) {
		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		Enumeration<?> propNames = transactionAttributes.propertyNames();
		while (propNames.hasMoreElements()) {
			String methodName = (String) propNames.nextElement();
			String value = transactionAttributes.getProperty(methodName);
			tae.setAsText(value);
			TransactionAttribute attr = (TransactionAttribute) tae.getValue();
			addTransactionalMethod(methodName, attr);
		}
	}

	/**
	 * 为事务方法添加属性。
	 * <p>方法名可以是精确匹配, 也可以是"xxx*"、"*xxx"或"*xxx*"模式以匹配多个方法。
	 * @param methodName 方法名称
	 * @param attr 与方法关联的属性
	 */
	public void addTransactionalMethod(String methodName, TransactionAttribute attr) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding transactional method [" + methodName + "] with attribute [" + attr + "]");
		}
		if (this.embeddedValueResolver != null && attr instanceof DefaultTransactionAttribute) {
			((DefaultTransactionAttribute) attr).resolveAttributeStrings(this.embeddedValueResolver);
		}
		this.nameMap.put(methodName, attr);
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void afterPropertiesSet()  {
		for (TransactionAttribute attr : this.nameMap.values()) {
			if (attr instanceof DefaultTransactionAttribute) {
				((DefaultTransactionAttribute) attr).resolveAttributeStrings(this.embeddedValueResolver);
			}
		}
	}


	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		if (!ClassUtils.isUserLevelMethod(method)) {
			return null;
		}

		// Look for direct name match.
		String methodName = method.getName();
		TransactionAttribute attr = this.nameMap.get(methodName);

		if (attr == null) {
			// Look for most specific name match.
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName) &&
						(bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					attr = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return attr;
	}

	/**
	 * 确定给定的方法名是否匹配映射名称。
	 * <p>默认实现检查"xxx*"、"*xxx"和"*xxx*"匹配, 以及直接相等。可以在子类中重写。
	 * @param methodName 该类的方法名称
	 * @param mappedName 描述符中的名称
	 * @return 如果名称匹配，则返回 {@code true}
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchTransactionAttributeSource)) {
			return false;
		}
		NameMatchTransactionAttributeSource otherTas = (NameMatchTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}

}
