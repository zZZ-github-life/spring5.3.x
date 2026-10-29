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

package org.springframework.http.converter.json;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.springframework.util.Base64Utils;

/**
 * 一个简单的工具类，用于获取 Google Gson 2.x 的 {@link GsonBuilder}，
 * 该构建器在读写 JSON 时对 {@code byte[]} 属性进行 Base64 编码。
 *
 * @author Juergen Hoeller
 * @author Roy Clarkson
 * @since 4.1
 * @see GsonFactoryBean#setBase64EncodeByteArrays
 * @see org.springframework.util.Base64Utils
 */
public abstract class GsonBuilderUtils {

	/**
	 * 获取一个 {@link GsonBuilder}，在读写 JSON 时对 {@code byte[]}
	 * 属性进行 Base64 编码。
	 * <p>将通过 {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
	 * 注册自定义的 {@link com.google.gson.TypeAdapter}，以 Base64 编码的字符串
	 * （而非 JSON 数组）序列化和反序列化 {@code byte[]} 属性。
	 */
	public static GsonBuilder gsonBuilderWithBase64EncodedByteArrays() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
		return builder;
	}


	private static class Base64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

		@Override
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(Base64Utils.encodeToString(src));
		}

		@Override
		public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) {
			return Base64Utils.decodeFromString(json.getAsString());
		}
	}

}
