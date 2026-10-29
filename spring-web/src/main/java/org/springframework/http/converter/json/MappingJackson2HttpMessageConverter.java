/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} 的实现，
 * 可使用 <a href="https://github.com/FasterXML/jackson">Jackson 2.x</a> 的
 * {@link ObjectMapper} 读写 JSON。
 *
 * <p>此转换器可用于绑定到类型化的 bean 或无类型的 {@code HashMap} 实例。
 *
 * <p>默认情况下，此转换器支持字符集为 {@code UTF-8} 的 {@code application/json}
 * 和 {@code application/*+json}。可以通过设置
 * {@link #setSupportedMediaTypes supportedMediaTypes} 属性来覆盖。
 *
 * <p>默认构造函数使用 {@link Jackson2ObjectMapperBuilder} 提供的默认配置。
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author Arjen Poutsma
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1.2
 */
public class MappingJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	@Nullable
	private String jsonPrefix;


	/**
	 * 使用 {@link Jackson2ObjectMapperBuilder} 提供的默认配置
	 * 构造一个新的 {@link MappingJackson2HttpMessageConverter}。
	 */
	public MappingJackson2HttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	/**
	 * 使用自定义的 {@link ObjectMapper} 构造一个新的
	 * {@link MappingJackson2HttpMessageConverter}。
	 * 可以使用 {@link Jackson2ObjectMapperBuilder} 轻松构建它。
	 * @see Jackson2ObjectMapperBuilder#json()
	 */
	public MappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
	}


	/**
	 * 指定用于此视图 JSON 输出的自定义前缀。
	 * 默认为无。
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图输出的 JSON 是否应以 ")]}', " 为前缀。默认为 {@code false}。
	 * <p>以这种方式为 JSON 字符串添加前缀有助于防止 JSON 劫持。
	 * 该前缀使字符串在语法上无法作为脚本，从而无法被劫持。
	 * 在将字符串解析为 JSON 之前应去掉该前缀。
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
		if (this.jsonPrefix != null) {
			generator.writeRaw(this.jsonPrefix);
		}
	}

}
