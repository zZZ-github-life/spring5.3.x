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

package org.springframework.beans.factory.config;

import java.lang.reflect.Field;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于获取静态或非静态字段值的 {@link FactoryBean}。
 *
 * <p>通常用于获取 public static final 常量。用法示例：
 *
 * <pre class="code">
 * // standard definition for exposing a static field, specifying the "staticField" property
 * &lt;bean id="myField" class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"&gt;
 *   &lt;property name="staticField" value="java.sql.Connection.TRANSACTION_SERIALIZABLE"/&gt;
 * &lt;/bean&gt;
 *
 * // convenience version that specifies a static field pattern as bean name
 * &lt;bean id="java.sql.Connection.TRANSACTION_SERIALIZABLE"
 *       class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"/&gt;
 * </pre>
 *
 * <p>如果使用的是 Spring 2.0，也可以对 public static 字段使用以下配置风格。
 *
 * <pre class="code">&lt;util:constant static-field="java.sql.Connection.TRANSACTION_SERIALIZABLE"/&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setStaticField
 */
public class FieldRetrievingFactoryBean
		implements FactoryBean<Object>, BeanNameAware, BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Class<?> targetClass;

	@Nullable
	private Object targetObject;

	@Nullable
	private String targetField;

	@Nullable
	private String staticField;

	@Nullable
	private String beanName;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	// the field we will retrieve
	@Nullable
	private Field fieldObject;


	/**
	 * 设置定义该字段的目标类。仅当目标字段为静态时才需要；否则无论如何都需要指定目标对象。
	 * @see #setTargetObject
	 * @see #setTargetField
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 返回定义该字段的目标类。
	 */
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 设置定义该字段的目标对象。仅当目标字段不是静态时才需要；否则指定目标类即可。
	 * @see #setTargetClass
	 * @see #setTargetField
	 */
	public void setTargetObject(@Nullable Object targetObject) {
		this.targetObject = targetObject;
	}

	/**
	 * 返回定义该字段的目标对象。
	 */
	@Nullable
	public Object getTargetObject() {
		return this.targetObject;
	}

	/**
	 * 设置要获取的字段名称。它可以是静态字段或非静态字段，取决于是否设置了目标对象。
	 * @see #setTargetClass
	 * @see #setTargetObject
	 */
	public void setTargetField(@Nullable String targetField) {
		this.targetField = (targetField != null ? StringUtils.trimAllWhitespace(targetField) : null);
	}

	/**
	 * 返回要获取的字段名称。
	 */
	@Nullable
	public String getTargetField() {
		return this.targetField;
	}

	/**
	 * 设置要获取的完全限定静态字段名，例如 "example.MyExampleClass.MY_EXAMPLE_FIELD"。这是指定
	 * targetClass 和 targetField 的一种便捷替代方式。
	 * @see #setTargetClass
	 * @see #setTargetField
	 */
	public void setStaticField(String staticField) {
		this.staticField = StringUtils.trimAllWhitespace(staticField);
	}

	/**
	 * 如果既未指定 "targetClass"，也未指定 "targetObject" 或 "targetField"，则此
	 * FieldRetrievingFactoryBean 的 Bean 名称将被解释为 "staticField" 模式。这样只需一个 id/name 即可
	 * 编写简洁的 Bean 定义。
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchFieldException {
		if (this.targetClass != null && this.targetObject != null) {
			throw new IllegalArgumentException("Specify either targetClass or targetObject, not both");
		}

		if (this.targetClass == null && this.targetObject == null) {
			if (this.targetField != null) {
				throw new IllegalArgumentException(
						"Specify targetClass or targetObject in combination with targetField");
			}

			// If no other property specified, consider bean name as static field expression.
			if (this.staticField == null) {
				this.staticField = this.beanName;
				Assert.state(this.staticField != null, "No target field specified");
			}

			// Try to parse static field into class and field.
			int lastDotIndex = this.staticField.lastIndexOf('.');
			if (lastDotIndex == -1 || lastDotIndex == this.staticField.length()) {
				throw new IllegalArgumentException(
						"staticField must be a fully qualified class plus static field name: " +
						"e.g. 'example.MyExampleClass.MY_EXAMPLE_FIELD'");
			}
			String className = this.staticField.substring(0, lastDotIndex);
			String fieldName = this.staticField.substring(lastDotIndex + 1);
			this.targetClass = ClassUtils.forName(className, this.beanClassLoader);
			this.targetField = fieldName;
		}

		else if (this.targetField == null) {
			// Either targetClass or targetObject specified.
			throw new IllegalArgumentException("targetField is required");
		}

		// Try to get the exact method first.
		Class<?> targetClass = (this.targetObject != null ? this.targetObject.getClass() : this.targetClass);
		this.fieldObject = targetClass.getField(this.targetField);
	}


	@Override
	@Nullable
	public Object getObject() throws IllegalAccessException {
		if (this.fieldObject == null) {
			throw new FactoryBeanNotInitializedException();
		}
		ReflectionUtils.makeAccessible(this.fieldObject);
		if (this.targetObject != null) {
			// instance field
			return this.fieldObject.get(this.targetObject);
		}
		else {
			// class field
			return this.fieldObject.get(null);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return (this.fieldObject != null ? this.fieldObject.getType() : null);
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}
