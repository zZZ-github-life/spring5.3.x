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

package org.springframework.context.support;

import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.lang.Nullable;

/**
 * 一个工厂，提供对 ConversionService 的便捷访问，该 ConversionService 使用适合大多数环境的转换器进
 * 行配置。设置 {@link #setConverters "converters"} 属性可补充默认转换器。
 *
 * <p>此实现创建一个 {@link DefaultConversionService}。子类可以覆盖
 * {@link #createConversionService()}，以返回自行选择的 {@link GenericConversionService} 实例。
 *
 * <p>与所有 {@code FactoryBean} 实现一样，此类适用于使用 Spring {@code <beans>} XML 配置 Spring 应
 * 用上下文的场景。使用 {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类配置容器时，只需在 {@link org.springframework.context.annotation.Bean @Bean} 方法中实例化、配
 * 置并返回适当的 {@code ConversionService} 对象。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

	@Nullable
	private Set<?> converters;

	@Nullable
	private GenericConversionService conversionService;


	/**
	 * 配置要添加的自定义转换器对象集合：实现
	 * {@link org.springframework.core.convert.converter.Converter}、
	 * {@link org.springframework.core.convert.converter.ConverterFactory} 或
	 * {@link org.springframework.core.convert.converter.GenericConverter}。
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	@Override
	public void afterPropertiesSet() {
		this.conversionService = createConversionService();
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
	}

	/**
	 * 创建此工厂 Bean 返回的 ConversionService 实例。<p>默认创建一个简单的
	 * {@link GenericConversionService} 实例。子类可以覆盖此方法，以自定义所创建的 ConversionService 实
	 * 例。
	 */
	protected GenericConversionService createConversionService() {
		return new DefaultConversionService();
	}


	// implementing FactoryBean

	@Override
	@Nullable
	public ConversionService getObject() {
		return this.conversionService;
	}

	@Override
	public Class<? extends ConversionService> getObjectType() {
		return GenericConversionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
