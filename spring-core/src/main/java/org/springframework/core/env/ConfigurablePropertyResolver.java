/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;

/**
 * 大多数（若非全部）{@link PropertyResolver} 类型都要实现的配置接口。
 * 提供用于访问和自定义在将属性值从一种类型转换为另一种类型时所使用的
 * {@link org.springframework.core.convert.ConversionService ConversionService} 的功能。
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * 返回在执行属性类型转换时使用的 {@link ConfigurableConversionService}。
	 * <p>所返回转换服务的可配置特性使得可以方便地添加和移除单个 {@code Converter} 实例：
	 * <pre class="code">
	 * ConfigurableConversionService cs = env.getConversionService();
	 * cs.addConverter(new FooConverter());
	 * </pre>
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	ConfigurableConversionService getConversionService();

	/**
	 * 设置在执行属性类型转换时要使用的 {@link ConfigurableConversionService}。
	 * <p><strong>注意：</strong>作为完全替换 {@code ConversionService} 的替代方案，
	 * 可以考虑通过深入访问 {@link #getConversionService()} 并调用诸如 {@code #addConverter}
	 * 之类的方法来添加或移除单个 {@code Converter} 实例。
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see #getConversionService()
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	void setConversionService(ConfigurableConversionService conversionService);

	/**
	 * 设置此解析器所替换的占位符必须以其开头的前缀。
	 */
	void setPlaceholderPrefix(String placeholderPrefix);

	/**
	 * 设置此解析器所替换的占位符必须以其结尾的后缀。
	 */
	void setPlaceholderSuffix(String placeholderSuffix);

	/**
	 * 指定此解析器所替换的占位符与其关联默认值之间的分隔字符，
	 * 如果不应将任何此类特殊字符作为值分隔符处理，则为 {@code null}。
	 */
	void setValueSeparator(@Nullable String valueSeparator);

	/**
	 * 设置当在给定属性值中遇到无法解析的嵌套占位符时是否抛出异常。
	 * {@code false} 值表示严格解析，即会抛出异常。{@code true} 值表示
	 * 无法解析的嵌套占位符应以其未解析的 ${...} 形式原样传递。
	 * <p>{@link #getProperty(String)} 及其变体的实现必须检查此处设置的值，
	 * 以确定属性值包含无法解析的占位符时的正确行为。
	 * @since 3.2
	 */
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	/**
	 * 指定必须存在哪些属性，以便由 {@link #validateRequiredProperties()} 进行验证。
	 */
	void setRequiredProperties(String... requiredProperties);

	/**
	 * 验证由 {@link #setRequiredProperties} 指定的每个属性都存在且解析为
	 * 非 {@code null} 值。
	 * @throws MissingRequiredPropertiesException 如果任一必需属性无法解析。
	 */
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
