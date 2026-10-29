/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} 的实现，
 * 可使用 <a href="https://javaee.github.io/jsonb-spec/">JSON Binding API</a>
 * 读写 JSON。
 *
 * <p>此转换器可用于绑定到类型化的 bean 或无类型的 {@code HashMap}。
 * 默认情况下，它支持字符集为 {@code UTF-8} 的 {@code application/json}
 * 和 {@code application/*+json}。
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see javax.json.bind.Jsonb
 * @see javax.json.bind.JsonbBuilder
 * @see #setJsonb
 */
public class JsonbHttpMessageConverter extends AbstractJsonHttpMessageConverter {

	private Jsonb jsonb;


	/**
	 * 使用默认配置构造一个新的 {@code JsonbHttpMessageConverter}。
	 */
	public JsonbHttpMessageConverter() {
		this(JsonbBuilder.create());
	}

	/**
	 * 使用给定配置构造一个新的 {@code JsonbHttpMessageConverter}。
	 * @param config the {@code JsonbConfig} for the underlying delegate
	 */
	public JsonbHttpMessageConverter(JsonbConfig config) {
		this.jsonb = JsonbBuilder.create(config);
	}

	/**
	 * 使用给定的委托构造一个新的 {@code JsonbHttpMessageConverter}。
	 * @param jsonb the Jsonb instance to use
	 */
	public JsonbHttpMessageConverter(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}


	/**
	 * 设置要使用的 {@code Jsonb} 实例。
	 * 如果未设置，将创建默认的 {@code Jsonb} 实例。
	 * <p>设置自定义配置的 {@code Jsonb} 是进一步控制
	 * JSON 序列化过程的一种方式。
	 * @see #JsonbHttpMessageConverter(Jsonb)
	 * @see #JsonbHttpMessageConverter(JsonbConfig)
	 * @see JsonbBuilder
	 */
	public void setJsonb(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}

	/**
	 * 返回此转换器已配置的 {@code Jsonb} 实例。
	 */
	public Jsonb getJsonb() {
		return this.jsonb;
	}


	@Override
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		return getJsonb().fromJson(reader, resolvedType);
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
		if (type instanceof ParameterizedType) {
			getJsonb().toJson(object, type, writer);
		}
		else {
			getJsonb().toJson(object, writer);
		}
	}

}
