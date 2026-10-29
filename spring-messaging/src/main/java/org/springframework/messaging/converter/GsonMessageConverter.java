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

package org.springframework.messaging.converter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;

import org.springframework.util.Assert;

/**
 * 使用 <a href="https://code.google.com/p/google-gson/">Google Gson</a>
 * 读写 JSON 的 {@link MessageConverter} 实现。
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see com.google.gson.Gson
 * @see com.google.gson.GsonBuilder
 * @see #setGson
 */
public class GsonMessageConverter extends AbstractJsonMessageConverter {

	private Gson gson;


	/**
	 * 使用默认配置构造新的 {@code GsonMessageConverter}。
	 */
	public GsonMessageConverter() {
		this.gson = new Gson();
	}

	/**
	 * 使用给定的委托构造新的 {@code GsonMessageConverter}。
	 * @param gson the Gson instance to use
	 */
	public GsonMessageConverter(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}


	/**
	 * 设置要使用的 {@code Gson} 实例。
	 * 如果未设置，将使用默认的 {@link Gson#Gson() Gson} 实例。
	 * <p>设置自定义配置的 {@code Gson} 是进一步控制 JSON 序列化过程的一种方式。
	 * @see #GsonMessageConverter(Gson)
	 */
	public void setGson(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}

	/**
	 * 返回此转换器已配置的 {@code Gson} 实例。
	 */
	public Gson getGson() {
		return this.gson;
	}


	@Override
	protected Object fromJson(Reader reader, Type resolvedType) {
		return getGson().fromJson(reader, resolvedType);
	}

	@Override
	protected Object fromJson(String payload, Type resolvedType) {
		return getGson().fromJson(payload, resolvedType);
	}

	@Override
	protected void toJson(Object payload, Type resolvedType, Writer writer) {
		if (resolvedType instanceof ParameterizedType) {
			getGson().toJson(payload, resolvedType, writer);
		}
		else {
			getGson().toJson(payload, writer);
		}
	}

	@Override
	protected String toJson(Object payload, Type resolvedType) {
		if (resolvedType instanceof ParameterizedType) {
			return getGson().toJson(payload, resolvedType);
		}
		else {
			return getGson().toJson(payload);
		}
	}

}
