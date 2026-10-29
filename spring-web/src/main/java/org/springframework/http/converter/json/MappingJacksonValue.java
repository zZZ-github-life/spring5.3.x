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

package org.springframework.http.converter.json;

import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.lang.Nullable;

/**
 * 一个简单的持有器，用于保存要通过
 * {@link MappingJackson2HttpMessageConverter} 序列化的 POJO，
 * 以及要传入转换器的其他序列化指令。
 *
 * <p>在服务器端，此包装器由 {@code ResponseBodyInterceptor}
 * 在内容协商选择了要使用的转换器之后、写入之前添加。
 *
 * <p>在客户端，只需包装 POJO 并将其传入 {@code RestTemplate} 即可。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class MappingJacksonValue {

	private Object value;

	@Nullable
	private Class<?> serializationView;

	@Nullable
	private FilterProvider filters;


	/**
	 * 创建一个新实例，包装给定的待序列化 POJO。
	 * @param value the Object to be serialized
	 */
	public MappingJacksonValue(Object value) {
		this.value = value;
	}


	/**
	 * 修改要序列化的 POJO。
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 返回需要序列化的 POJO。
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 设置用于序列化该 POJO 的序列化视图。
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
	 * @see com.fasterxml.jackson.annotation.JsonView
	 */
	public void setSerializationView(@Nullable Class<?> serializationView) {
		this.serializationView = serializationView;
	}

	/**
	 * 返回要使用的序列化视图。
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
	 * @see com.fasterxml.jackson.annotation.JsonView
	 */
	@Nullable
	public Class<?> getSerializationView() {
		return this.serializationView;
	}

	/**
	 * 设置用于序列化该 POJO 的 Jackson 过滤器提供者。
	 * @since 4.2
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writer(FilterProvider)
	 * @see com.fasterxml.jackson.annotation.JsonFilter
	 * @see Jackson2ObjectMapperBuilder#filters(FilterProvider)
	 */
	public void setFilters(@Nullable FilterProvider filters) {
		this.filters = filters;
	}

	/**
	 * 返回要使用的 Jackson 过滤器提供者。
	 * @since 4.2
	 * @see com.fasterxml.jackson.databind.ObjectMapper#writer(FilterProvider)
	 * @see com.fasterxml.jackson.annotation.JsonFilter
	 */
	@Nullable
	public FilterProvider getFilters() {
		return this.filters;
	}

}
