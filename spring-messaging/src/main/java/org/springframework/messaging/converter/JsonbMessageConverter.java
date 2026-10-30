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

package org.springframework.messaging.converter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.springframework.util.Assert;

/**
 * 使用 <a href="https://javaee.github.io/jsonb-spec/">JSON Binding API</a>
 * 读写 JSON 的 {@link MessageConverter} 实现。
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see javax.json.bind.Jsonb
 * @see javax.json.bind.JsonbBuilder
 * @see #setJsonb
 */
public class JsonbMessageConverter extends AbstractJsonMessageConverter {

	private Jsonb jsonb;


	/**
	 * 使用默认配置构造新的 {@code JsonbMessageConverter}。
	 */
	public JsonbMessageConverter() {
		this.jsonb = JsonbBuilder.create();
	}

	/**
	 * 使用给定的配置构造新的 {@code JsonbMessageConverter}。
	 * @param config 底层委托使用的 {@code JsonbConfig}
	 */
	public JsonbMessageConverter(JsonbConfig config) {
		this.jsonb = JsonbBuilder.create(config);
	}

	/**
	 * 使用给定的委托构造新的 {@code JsonbMessageConverter}。
	 * @param jsonb 要使用的 Jsonb 实例
	 */
	public JsonbMessageConverter(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}


	/**
	 * 设置要使用的 {@code Jsonb} 实例。
	 * 如果未设置，将创建默认的 {@code Jsonb} 实例。
	 * <p>设置自定义配置的 {@code Jsonb} 是进一步控制 JSON 序列化过程的一种方式。
	 * @see #JsonbMessageConverter(Jsonb)
	 * @see #JsonbMessageConverter(JsonbConfig)
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
	protected Object fromJson(Reader reader, Type resolvedType) {
		return getJsonb().fromJson(reader, resolvedType);
	}

	@Override
	protected Object fromJson(String payload, Type resolvedType) {
		return getJsonb().fromJson(payload, resolvedType);
	}

	@Override
	protected void toJson(Object payload, Type resolvedType, Writer writer) {
		if (resolvedType instanceof ParameterizedType) {
			getJsonb().toJson(payload, resolvedType, writer);
		}
		else {
			getJsonb().toJson(payload, writer);
		}
	}

	@Override
	protected String toJson(Object payload, Type resolvedType) {
		if (resolvedType instanceof ParameterizedType) {
			return getJsonb().toJson(payload, resolvedType);
		}
		else {
			return getJsonb().toJson(payload);
		}
	}

}
