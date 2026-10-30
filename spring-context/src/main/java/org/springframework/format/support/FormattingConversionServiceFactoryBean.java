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

package org.springframework.format.support;

import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * 一个工厂，提供对 {@code FormattingConversionService} 的便捷访问，该服务配置了用于数字和日期时间
 * 等常见类型的转换器和格式化器。
 *
 * <p>可以通过 {@link #setConverters(Set)} 和 {@link #setFormatters(Set)} 以声明方式注册额外的转换
 * 器和格式化器。另一种方式是通过实现 {@link FormatterRegistrar} 接口在代码中注册转换器和格式化器。
 * 然后可以通过 {@link #setFormatterRegistrars(Set)} 配置要使用的注册器集合。
 *
 * <p>在代码中注册转换器和格式化器的一个很好的例子是 {@code JodaTimeFormatterRegistrar}，它注册了许
 * 多与日期相关的格式化器和转换器。有关更详细的情况列表，请参见
 * {@link #setFormatterRegistrars(Set)}
 *
 * <p>与所有 {@code FactoryBean} 实现一样，此类适用于使用 Spring {@code <beans>} XML 配置 Spring 应
 * 用上下文时使用。使用 {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类配置容器时，只需从 {@link org.springframework.context.annotation.Bean @Bean} 方法中实例化、配
 * 置并返回适当的 {@code FormattingConversionService} 对象即可。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.0
 */
public class FormattingConversionServiceFactoryBean
		implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

	@Nullable
	private Set<?> converters;

	@Nullable
	private Set<?> formatters;

	@Nullable
	private Set<FormatterRegistrar> formatterRegistrars;

	private boolean registerDefaultFormatters = true;

	@Nullable
	private StringValueResolver embeddedValueResolver;

	@Nullable
	private FormattingConversionService conversionService;


	/**
	 * 配置应添加的自定义转换器对象集合。
	 * @param converters 以下任意类型的实例：
	 * {@link org.springframework.core.convert.converter.Converter}、
	 * {@link org.springframework.core.convert.converter.ConverterFactory}、
	 * {@link org.springframework.core.convert.converter.GenericConverter}
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	/**
	 * 配置应添加的自定义格式化器对象集合。
	 * @param formatters {@link Formatter} 或 {@link AnnotationFormatterFactory} 的实例
	 */
	public void setFormatters(Set<?> formatters) {
		this.formatters = formatters;
	}

	/**
	 * <p>配置要调用的 FormatterRegistrars 集合，以便在通过 {@link #setConverters(Set)} 和
	 * {@link #setFormatters(Set)} 以声明方式添加的转换器和格式化器之外，注册其他 Converters 和
	 * Formatters。
	 * <p>当为某个格式化类别（例如 Date 格式化）注册多个相关的转换器和格式化器时，FormatterRegistrars
	 * 很有用。支持该格式化类别所需的所有相关类型都可以在一个地方注册。
	 * <p>FormatterRegistrars 还可用于注册以其自身 &lt;T&gt; 之外的其他特定字段类型为索引的 Formatters，
	 * 或者用于从 Printer/Parser 对注册 Formatter。
	 * @see FormatterRegistry#addFormatterForFieldType(Class, Formatter)
	 * @see FormatterRegistry#addFormatterForFieldType(Class, Printer, Parser)
	 */
	public void setFormatterRegistrars(Set<FormatterRegistrar> formatterRegistrars) {
		this.formatterRegistrars = formatterRegistrars;
	}

	/**
	 * 指示是否应注册默认格式化器。
	 * <p>默认情况下会注册内置格式化器。此标志可用于关闭该行为，仅依赖显式注册的格式化器。
	 * @see #setFormatters(Set)
	 * @see #setFormatterRegistrars(Set)
	 */
	public void setRegisterDefaultFormatters(boolean registerDefaultFormatters) {
		this.registerDefaultFormatters = registerDefaultFormatters;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}


	@Override
	public void afterPropertiesSet() {
		this.conversionService = new DefaultFormattingConversionService(this.embeddedValueResolver, this.registerDefaultFormatters);
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
		registerFormatters(this.conversionService);
	}

	private void registerFormatters(FormattingConversionService conversionService) {
		if (this.formatters != null) {
			for (Object formatter : this.formatters) {
				if (formatter instanceof Formatter<?>) {
					conversionService.addFormatter((Formatter<?>) formatter);
				}
				else if (formatter instanceof AnnotationFormatterFactory<?>) {
					conversionService.addFormatterForFieldAnnotation((AnnotationFormatterFactory<?>) formatter);
				}
				else {
					throw new IllegalArgumentException(
							"Custom formatters must be implementations of Formatter or AnnotationFormatterFactory");
				}
			}
		}
		if (this.formatterRegistrars != null) {
			for (FormatterRegistrar registrar : this.formatterRegistrars) {
				registrar.registerFormatters(conversionService);
			}
		}
	}


	@Override
	@Nullable
	public FormattingConversionService getObject() {
		return this.conversionService;
	}

	@Override
	public Class<? extends FormattingConversionService> getObjectType() {
		return FormattingConversionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
