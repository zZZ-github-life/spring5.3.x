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

package org.springframework.http.codec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;

/**
 * 用于加载默认编解码器配置器类名的内部委托。
 * 与 support 包中的默认实现之间是一种松散关系，
 * 实际上只需知道要使用的默认类名。
 *
 * @author Juergen Hoeller
 * @since 5.0.1
 * @see ClientCodecConfigurer#create()
 * @see ServerCodecConfigurer#create()
 */
final class CodecConfigurerFactory {

	private static final String DEFAULT_CONFIGURERS_PATH = "CodecConfigurer.properties";

	private static final Map<Class<?>, Class<?>> defaultCodecConfigurers = new HashMap<>(4);

	static {
		try {
			Properties props = PropertiesLoaderUtils.loadProperties(
					new ClassPathResource(DEFAULT_CONFIGURERS_PATH, CodecConfigurerFactory.class));
			for (String ifcName : props.stringPropertyNames()) {
				String implName = props.getProperty(ifcName);
				Class<?> ifc = ClassUtils.forName(ifcName, CodecConfigurerFactory.class.getClassLoader());
				Class<?> impl = ClassUtils.forName(implName, CodecConfigurerFactory.class.getClassLoader());
				defaultCodecConfigurers.put(ifc, impl);
			}
		}
		catch (IOException | ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}


	private CodecConfigurerFactory() {
	}


	@SuppressWarnings("unchecked")
	public static <T extends CodecConfigurer> T create(Class<T> ifc) {
		Class<?> impl = defaultCodecConfigurers.get(ifc);
		if (impl == null) {
			throw new IllegalStateException("No default codec configurer found for " + ifc);
		}
		return (T) BeanUtils.instantiateClass(impl);
	}

}
