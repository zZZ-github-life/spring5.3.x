/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.util.StringUtils;

/**
 * 属性编辑器, 将字符串转换为{@link TransactionAttributeSource}。
 * 事务属性字符串必须可由此包中的{@link TransactionAttributeEditor}解析。
 *
 * <p>字符串采用属性语法, 格式为:<br>
 * {@code FQCN.methodName=<事务属性字符串>}
 *
 * <p>例如:<br>
 * {@code com.mycompany.mycode.MyClass.myMethod=PROPAGATION_MANDATORY,ISOLATION_DEFAULT}
 *
 * <p><b>注意:</b> 指定的类必须是方法定义所在的类; 在实现接口的情况下, 为接口类名。
 *
 * <p>注意: 将为给定名称注册所有重载方法。不支持显式注册特定的重载方法。
 * 支持"xxx*"映射, 例如"notify*"对应"notify"和"notifyAll"。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 26.04.2003
 * @see TransactionAttributeEditor
 */
public class TransactionAttributeSourceEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
		if (StringUtils.hasLength(text)) {
			// Use properties editor to tokenize the hold string.
			PropertiesEditor propertiesEditor = new PropertiesEditor();
			propertiesEditor.setAsText(text);
			Properties props = (Properties) propertiesEditor.getValue();

			// Now we have properties, process each one individually.
			TransactionAttributeEditor tae = new TransactionAttributeEditor();
			Enumeration<?> propNames = props.propertyNames();
			while (propNames.hasMoreElements()) {
				String name = (String) propNames.nextElement();
				String value = props.getProperty(name);
				// Convert value to a transaction attribute.
				tae.setAsText(value);
				TransactionAttribute attr = (TransactionAttribute) tae.getValue();
				// Register name and attribute.
				source.addTransactionalMethod(name, attr);
			}
		}
		setValue(source);
	}

}
