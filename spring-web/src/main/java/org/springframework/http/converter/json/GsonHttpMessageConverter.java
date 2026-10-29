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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} 的实现，
 * 可使用 <a href="https://code.google.com/p/google-gson/">Google Gson</a> 库
 * 读写 JSON。
 *
 * <p>此转换器可用于绑定到类型化的 bean 或无类型的 {@code HashMap}。
 * 默认情况下，它支持字符集为 {@code UTF-8} 的 {@code application/json}
 * 和 {@code application/*+json}。
 *
 * <p>已针对 Gson 2.8 测试；兼容 Gson 2.0 及更高版本。
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.1
 * @see com.google.gson.Gson
 * @see com.google.gson.GsonBuilder
 * @see #setGson
 */
public class GsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {

	private Gson gson;


	/**
	 * 使用默认配置构造一个新的 {@code GsonHttpMessageConverter}。
	 */
	public GsonHttpMessageConverter() {
		this.gson = new Gson();
	}

	/**
	 * 使用给定的委托构造一个新的 {@code GsonHttpMessageConverter}。
	 * @param gson the Gson instance to use
	 * @since 5.0
	 */
	public GsonHttpMessageConverter(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}


	/**
	 * 设置要使用的 {@code Gson} 实例。
	 * 如果未设置，将使用默认的 {@link Gson#Gson() Gson} 实例。
	 * <p>设置自定义配置的 {@code Gson} 是进一步控制
	 * JSON 序列化过程的一种方式。
	 * @see #GsonHttpMessageConverter(Gson)
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
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		return getGson().fromJson(reader, resolvedType);
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
		// In Gson, toJson with a type argument will exclusively use that given type,
		// ignoring the actual type of the object... which might be more specific,
		// e.g. a subclass of the specified type which includes additional fields.
		// As a consequence, we're only passing in parameterized type declarations
		// which might contain extra generics that the object instance doesn't retain.
		if (type instanceof ParameterizedType) {
			getGson().toJson(object, type, writer);
		}
		else {
			getGson().toJson(object, writer);
		}
	}

}
