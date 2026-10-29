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

package org.springframework.http.converter.xml;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * 的实现，可使用 <a href="https://github.com/FasterXML/jackson-dataformat-xml">
 * 用于读写 XML 编码数据的 Jackson 2.x 扩展组件</a>读写 XML。
 *
 * <p>默认情况下，此转换器支持字符集为 {@code UTF-8} 的 {@code application/xml}、
 * {@code text/xml} 和 {@code application/*+xml}。可以通过设置
 * {@link #setSupportedMediaTypes supportedMediaTypes} 属性来覆盖。
 *
 * <p>默认构造函数使用 {@link Jackson2ObjectMapperBuilder} 提供的默认配置。
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class MappingJackson2XmlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * 使用 {@code Jackson2ObjectMapperBuilder} 提供的默认配置
	 * 构造一个新的 {@code MappingJackson2XmlHttpMessageConverter}。
	 */
	public MappingJackson2XmlHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.xml().build());
	}

	/**
	 * 使用自定义的 {@link ObjectMapper}（必须是 {@link XmlMapper} 实例）
	 * 构造一个新的 {@code MappingJackson2XmlHttpMessageConverter}。
	 * 可以使用 {@link Jackson2ObjectMapperBuilder} 轻松构建它。
	 * @see Jackson2ObjectMapperBuilder#xml()
	 */
	public MappingJackson2XmlHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, new MediaType("application", "xml", StandardCharsets.UTF_8),
				new MediaType("text", "xml", StandardCharsets.UTF_8),
				new MediaType("application", "*+xml", StandardCharsets.UTF_8));
		Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
	}


	/**
	 * {@inheritDoc}
	 * {@code ObjectMapper} 参数必须是 {@link XmlMapper} 实例。
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
		super.setObjectMapper(objectMapper);
	}

}
