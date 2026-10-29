/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * 大多数（即使不是全部）{@link ConversionService} 类型都应实现的配置接口。它整合了
 * {@link ConversionService} 公开的只读操作和 {@link ConverterRegistry} 的变更操作，以便通过它方便
 * 地临时添加和移除 {@link org.springframework.core.convert.converter.Converter Converters}。在应用
 * 上下文引导代码中针对
 * {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment} 实例工作时，
 * 后者特别有用。
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.core.env.ConfigurablePropertyResolver#getConversionService()
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {

}
