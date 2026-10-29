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

package org.springframework.web.bind.support;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingErrorProcessor;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;

/**
 * 用于在 Spring 应用上下文中进行声明式配置的便捷 {@link WebBindingInitializer}。
 * 允许在多个控制器/处理器之间复用预先配置好的初始化器。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setDirectFieldAccess
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #setValidator(Validator)
 * @see #setConversionService(ConversionService)
 * @see #setPropertyEditorRegistrar
 */
public class ConfigurableWebBindingInitializer implements WebBindingInitializer {

	private boolean autoGrowNestedPaths = true;

	private boolean directFieldAccess = false;

	@Nullable
	private MessageCodesResolver messageCodesResolver;

	@Nullable
	private BindingErrorProcessor bindingErrorProcessor;

	@Nullable
	private Validator validator;

	@Nullable
	private ConversionService conversionService;

	@Nullable
	private PropertyEditorRegistrar[] propertyEditorRegistrars;


	/**
	 * 设置绑定器是否应尝试 "自动增长" 包含 null 值的嵌套路径。
	 * <p>如果为 "true"，null 路径位置将用默认对象值填充并继续遍历，
	 * 而不是导致异常。此标志还可在访问越界索引时启用集合元素的自动增长。
	 * <p>标准 DataBinder 上的默认值为 "true"。请注意，此功能仅支持 bean 属性访问
	 * （DataBinder 的默认模式），不支持字段访问。
	 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
	 * @see org.springframework.validation.DataBinder#setAutoGrowNestedPaths
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * 返回绑定器是否应尝试 "自动增长" 包含 null 值的嵌套路径。
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * 设置是否使用直接字段访问而不是 bean 属性访问。
	 * <p>默认为 {@code false}，即使用 bean 属性访问。
	 * 将此值切换为 {@code true} 可强制使用直接字段访问。
	 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
	 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
	 */
	public final void setDirectFieldAccess(boolean directFieldAccess) {
		this.directFieldAccess = directFieldAccess;
	}

	/**
	 * 返回是否使用直接字段访问而不是 bean 属性访问。
	 */
	public boolean isDirectFieldAccess() {
		return this.directFieldAccess;
	}

	/**
	 * 设置用于将错误解析为消息代码的策略。
	 * 将给定策略应用于此控制器使用的所有数据绑定器。
	 * <p>默认为 {@code null}，即使用数据绑定器的默认策略。
	 * @see org.springframework.validation.DataBinder#setMessageCodesResolver
	 */
	public final void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * 返回用于将错误解析为消息代码的策略。
	 */
	@Nullable
	public final MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * 设置用于处理绑定错误的策略，即必需字段错误和
	 * {@code PropertyAccessException}。
	 * <p>默认为 {@code null}，即使用数据绑定器的默认策略。
	 * @see org.springframework.validation.DataBinder#setBindingErrorProcessor
	 */
	public final void setBindingErrorProcessor(@Nullable BindingErrorProcessor bindingErrorProcessor) {
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * 返回用于处理绑定错误的策略。
	 */
	@Nullable
	public final BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * 设置每次绑定步骤之后要应用的 Validator。
	 */
	public final void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	/**
	 * 返回每次绑定步骤之后要应用的 Validator（如果有）。
	 */
	@Nullable
	public final Validator getValidator() {
		return this.validator;
	}

	/**
	 * 指定应用于每个 DataBinder 的 ConversionService。
	 * @since 3.0
	 */
	public final void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回应用于每个 DataBinder 的 ConversionService。
	 */
	@Nullable
	public final ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 指定应用于每个 DataBinder 的单个 PropertyEditorRegistrar。
	 */
	public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrars = new PropertyEditorRegistrar[] {propertyEditorRegistrar};
	}

	/**
	 * 指定应用于每个 DataBinder 的多个 PropertyEditorRegistrar。
	 */
	public final void setPropertyEditorRegistrars(@Nullable PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * 返回应用于每个 DataBinder 的 PropertyEditorRegistrar。
	 */
	@Nullable
	public final PropertyEditorRegistrar[] getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}


	@Override
	public void initBinder(WebDataBinder binder) {
		binder.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
		if (this.directFieldAccess) {
			binder.initDirectFieldAccess();
		}
		if (this.messageCodesResolver != null) {
			binder.setMessageCodesResolver(this.messageCodesResolver);
		}
		if (this.bindingErrorProcessor != null) {
			binder.setBindingErrorProcessor(this.bindingErrorProcessor);
		}
		if (this.validator != null && binder.getTarget() != null &&
				this.validator.supports(binder.getTarget().getClass())) {
			binder.setValidator(this.validator);
		}
		if (this.conversionService != null) {
			binder.setConversionService(this.conversionService);
		}
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				propertyEditorRegistrar.registerCustomEditors(binder);
			}
		}
	}

}
