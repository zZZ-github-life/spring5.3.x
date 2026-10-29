/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于标准 JDK {@link java.util.ResourceBundle ResourceBundles} 的
 * {@link java.beans.PropertyEditor} 实现。
 *
 * <p>仅支持<i>从</i> String 转换，不支持<i>到</i> String 的转换。
 *
 * 下面给出一些在（正确配置的）使用基于 XML 元数据的 Spring 容器中使用此类的示例：
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'messages' property is of type java.util.ResourceBundle.
 *        the 'DialogMessages.properties' file exists at the root of the CLASSPATH
 *    --&gt;
 *    &lt;property name="messages" value="DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'DialogMessages.properties' file exists in the 'com/messages' package
 *    --&gt;
 *    &lt;property name="messages" value="com/messages/DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>一个“正确配置的” Spring {@link org.springframework.context.ApplicationContext container}可能
 * 包含 {@link org.springframework.beans.factory.config.CustomEditorConfigurer} 定义，从而使转换可
 * 以透明地完成：
 *
 * <pre class="code"> &lt;bean class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *    &lt;property name="customEditors"&gt;
 *        &lt;map&gt;
 *            &lt;entry key="java.util.ResourceBundle"&gt;
 *                &lt;bean class="org.springframework.beans.propertyeditors.ResourceBundleEditor"/&gt;
 *            &lt;/entry&gt;
 *        &lt;/map&gt;
 *    &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>请注意，此 {@link java.beans.PropertyEditor} <b>不会</b>默认向任何 Spring 基础设施注册。
 *
 * <p>感谢 David Leal Valmana 提出的建议和初始原型。
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ResourceBundleEditor extends PropertyEditorSupport {

	/**
	 * 用于区分基名和区域设置（如果有）的分隔符，在 {@link #setAsText(String) converting from a String}
	 * 时使用。
	 */
	public static final String BASE_NAME_SEPARATOR = "_";


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		Assert.hasText(text, "'text' must not be empty");
		String name = text.trim();

		int separator = name.indexOf(BASE_NAME_SEPARATOR);
		if (separator == -1) {
			setValue(ResourceBundle.getBundle(name));
		}
		else {
			// The name potentially contains locale information
			String baseName = name.substring(0, separator);
			if (!StringUtils.hasText(baseName)) {
				throw new IllegalArgumentException("Invalid ResourceBundle name: '" + text + "'");
			}
			String localeString = name.substring(separator + 1);
			Locale locale = StringUtils.parseLocaleString(localeString);
			setValue(locale != null ? ResourceBundle.getBundle(baseName, locale) : ResourceBundle.getBundle(baseName));
		}
	}

}
