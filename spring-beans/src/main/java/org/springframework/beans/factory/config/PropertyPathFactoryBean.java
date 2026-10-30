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

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 在给定目标对象上求值属性路径的 {@link FactoryBean}。
 *
 * <p>目标对象可以直接指定，也可以通过 Bean 名称指定。
 *
 * <p>用法示例：
 *
 * <pre class="code">&lt;!-- target bean to be referenced by name --&gt;
 * &lt;bean id="tb" class="org.springframework.beans.TestBean" singleton="false"&gt;
 *   &lt;property name="age" value="10"/&gt;
 *   &lt;property name="spouse"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="11"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 12, which is the value of property 'age' of the inner bean --&gt;
 * &lt;bean id="propertyPath1" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetObject"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="12"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="propertyPath" value="age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 11, which is the value of property 'spouse.age' of bean 'tb' --&gt;
 * &lt;bean id="propertyPath2" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetBeanName" value="tb"/&gt;
 *   &lt;property name="propertyPath" value="spouse.age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;bean id="tb.age" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"/&gt;</pre>
 *
 * <p>如果使用 Spring 2.0 且配置文件支持 XML Schema，也可以对属性路径访问使用以下配置风格。（更多示
 * 例另请参阅 Spring 参考手册中标题为 'XML Schema-based configuration' 的附录。）
 *
 * <pre class="code"> &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;util:property-path id="name" path="testBean.age"/&gt;</pre>
 *
 * 感谢 Matthias Ernst 提出的建议和初始原型！
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 * @see #setTargetObject
 * @see #setTargetBeanName
 * @see #setPropertyPath
 */
public class PropertyPathFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PropertyPathFactoryBean.class);

	@Nullable
	private BeanWrapper targetBeanWrapper;

	@Nullable
	private String targetBeanName;

	@Nullable
	private String propertyPath;

	@Nullable
	private Class<?> resultType;

	@Nullable
	private String beanName;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 指定要将属性路径应用到的目标对象。或者指定目标 Bean 名称。
	 * @param targetObject 目标对象，例如 Bean 引用
	 * 或内部 Bean
	 * @see #setTargetBeanName
	 */
	public void setTargetObject(Object targetObject) {
		this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(targetObject);
	}

	/**
	 * 指定要将属性路径应用到的目标 Bean 名称。或者直接指定目标对象。
	 * @param targetBeanName 要在所属 Bean 工厂中查找的 Bean 名称
	 * （例如 "testBean"）
	 * @see #setTargetObject
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = StringUtils.trimAllWhitespace(targetBeanName);
	}

	/**
	 * 指定要应用到目标上的属性路径。
	 * @param propertyPath 属性路径，可以嵌套
	 * （例如 "age" 或 "spouse.age"）
	 */
	public void setPropertyPath(String propertyPath) {
		this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
	}

	/**
	 * 指定对属性路径求值所得结果的类型。
	 * <p>注意：对于直接指定的目标对象或单例目标 Bean，这是不必要的，因为可以通过内省确定类型。仅当目标
	 * 是原型时，如果你需要按类型匹配（例如用于自动装配），才需要指定此项。
	 * @param resultType 结果类型，例如 "java.lang.Integer"
	 */
	public void setResultType(Class<?> resultType) {
		this.resultType = resultType;
	}

	/**
	 * 如果既未指定 "targetObject"，也未指定 "targetBeanName" 或 "propertyPath"，则此
	 * PropertyPathFactoryBean 的 Bean 名称将被解释为 "beanName.property" 模式。这样只需一个 id/name 即
	 * 可编写简洁的 Bean 定义。
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;

		if (this.targetBeanWrapper != null && this.targetBeanName != null) {
			throw new IllegalArgumentException("Specify either 'targetObject' or 'targetBeanName', not both");
		}

		if (this.targetBeanWrapper == null && this.targetBeanName == null) {
			if (this.propertyPath != null) {
				throw new IllegalArgumentException(
						"Specify 'targetObject' or 'targetBeanName' in combination with 'propertyPath'");
			}

			// No other properties specified: check bean name.
			int dotIndex = (this.beanName != null ? this.beanName.indexOf('.') : -1);
			if (dotIndex == -1) {
				throw new IllegalArgumentException(
						"Neither 'targetObject' nor 'targetBeanName' specified, and PropertyPathFactoryBean " +
						"bean name '" + this.beanName + "' does not follow 'beanName.property' syntax");
			}
			this.targetBeanName = this.beanName.substring(0, dotIndex);
			this.propertyPath = this.beanName.substring(dotIndex + 1);
		}

		else if (this.propertyPath == null) {
			// either targetObject or targetBeanName specified
			throw new IllegalArgumentException("'propertyPath' is required");
		}

		if (this.targetBeanWrapper == null && this.beanFactory.isSingleton(this.targetBeanName)) {
			// Eagerly fetch singleton target bean, and determine result type.
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
			this.resultType = this.targetBeanWrapper.getPropertyType(this.propertyPath);
		}
	}


	@Override
	@Nullable
	public Object getObject() throws BeansException {
		BeanWrapper target = this.targetBeanWrapper;
		if (target != null) {
			if (logger.isWarnEnabled() && this.targetBeanName != null &&
					this.beanFactory instanceof ConfigurableBeanFactory &&
					((ConfigurableBeanFactory) this.beanFactory).isCurrentlyInCreation(this.targetBeanName)) {
				logger.warn("Target bean '" + this.targetBeanName + "' is still in creation due to a circular " +
						"reference - obtained value for property '" + this.propertyPath + "' may be outdated!");
			}
		}
		else {
			// Fetch prototype target bean...
			Assert.state(this.beanFactory != null, "No BeanFactory available");
			Assert.state(this.targetBeanName != null, "No target bean name specified");
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			target = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		}
		Assert.state(this.propertyPath != null, "No property path specified");
		return target.getPropertyValue(this.propertyPath);
	}

	@Override
	public Class<?> getObjectType() {
		return this.resultType;
	}

	/**
	 * 虽然此 FactoryBean 常用于单例目标，但为属性路径调用的 getter 可能在每次调用时返回新对象，因此我
	 * 们必须假定在每次 {@link #getObject()} 调用时不会返回同一个对象。
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}

}
