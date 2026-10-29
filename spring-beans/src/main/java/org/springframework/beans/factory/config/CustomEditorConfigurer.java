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

import java.beans.PropertyEditor;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanFactoryPostProcessor} 实现，便于注册自定义的 {@link PropertyEditor property editors}。
 *
 * <p>如果你希望注册 {@link PropertyEditor} 实例，从 Spring 2.0 起的推荐用法是使用自定义的
 * {@link PropertyEditorRegistrar} 实现，由它们在给定的
 * {@link org.springframework.beans.PropertyEditorRegistry registry} 上注册任何所需的编辑器实例。每
 * 个 PropertyEditorRegistrar 都可以注册任意数量的自定义编辑器。
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="propertyEditorRegistrars"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="mypackage.MyCustomDateEditorRegistrar"/&gt;
 *       &lt;bean class="mypackage.MyObjectEditorRegistrar"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>通过 {@code customEditors} 属性注册 {@link PropertyEditor} <em>类</em>是完全可以的。这样
 * Spring 会为每次编辑尝试创建它们的新实例：
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="customEditors"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="java.util.Date" value="mypackage.MyCustomDateEditor"/&gt;
 *       &lt;entry key="mypackage.MyObject" value="mypackage.MyObjectEditor"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>注意，不应通过 {@code customEditors} 属性注册 {@link PropertyEditor} Bean 实例，因为
 * {@link PropertyEditor PropertyEditors} 是有状态的，那样每次编辑尝试都必须对这些实例进行同步。如
 * 果需要控制 {@link PropertyEditor PropertyEditors} 的实例化过程，请使用
 * {@link PropertyEditorRegistrar} 来注册它们。
 *
 * <p>还支持 "java.lang.String[]" 风格的数组类名和基本类型类名（例如 "boolean"）。实际的类名解析委
 * 托给 {@link ClassUtils}。
 *
 * <p><b>注意：</b>通过此配置器注册的自定义属性编辑器<i>不</i>适用于数据绑定。用于数据绑定的自定义
 * 编辑器需要注册到 {@link org.springframework.validation.DataBinder} 上：使用公共基类或委托给公共
 * 的 PropertyEditorRegistrar 实现，以便在那里复用编辑器注册。
 *
 * @author Juergen Hoeller
 * @since 27.02.2004
 * @see java.beans.PropertyEditor
 * @see org.springframework.beans.PropertyEditorRegistrar
 * @see ConfigurableBeanFactory#addPropertyEditorRegistrar
 * @see ConfigurableBeanFactory#registerCustomEditor
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomEditorConfigurer implements BeanFactoryPostProcessor, Ordered {

	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private PropertyEditorRegistrar[] propertyEditorRegistrars;

	@Nullable
	private Map<Class<?>, Class<? extends PropertyEditor>> customEditors;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定要应用于当前应用上下文中定义的 Bean 的
	 * {@link PropertyEditorRegistrar PropertyEditorRegistrars}。
	 * <p>这使得可以与 {@link org.springframework.validation.DataBinder DataBinders} 等共享
	 * {@code PropertyEditorRegistrars}。此外，它还避免了对自定义编辑器进行同步的需要：
	 * {@code PropertyEditorRegistrar} 总会在每次 Bean 创建尝试时创建新的编辑器实例。
	 * @see ConfigurableListableBeanFactory#addPropertyEditorRegistrar
	 */
	public void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * 通过 {@link Map} 指定要注册的自定义编辑器，以所需类型的类名作为键，以关联的
	 * {@link PropertyEditor} 的类名作为值。
	 * @see ConfigurableListableBeanFactory#registerCustomEditor
	 */
	public void setCustomEditors(Map<Class<?>, Class<? extends PropertyEditor>> customEditors) {
		this.customEditors = customEditors;
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				beanFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
			}
		}
		if (this.customEditors != null) {
			this.customEditors.forEach(beanFactory::registerCustomEditor);
		}
	}

}
