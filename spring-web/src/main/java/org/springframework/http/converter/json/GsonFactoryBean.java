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

import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 用于创建 Google Gson 2.x {@link Gson} 实例的 {@link FactoryBean}。
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.1
 */
public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

	private boolean base64EncodeByteArrays = false;

	private boolean serializeNulls = false;

	private boolean prettyPrinting = false;

	private boolean disableHtmlEscaping = false;

	@Nullable
	private String dateFormatPattern;

	@Nullable
	private Gson gson;


	/**
	 * 读写 JSON 时是否对 {@code byte[]} 属性进行 Base64 编码。
	 * <p>设置为 {@code true} 时，将通过
	 * {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
	 * 注册自定义的 {@link com.google.gson.TypeAdapter}，以 Base64 编码的字符串
	 * （而非 JSON 数组）序列化和反序列化 {@code byte[]} 属性。
	 * @see GsonBuilderUtils#gsonBuilderWithBase64EncodedByteArrays()
	 */
	public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
		this.base64EncodeByteArrays = base64EncodeByteArrays;
	}

	/**
	 * 写入 JSON 时是否使用 {@link GsonBuilder#serializeNulls()} 选项。
	 * 这是按如下方式设置 {@code Gson} 的快捷方式：
	 * <pre class="code">
	 * new GsonBuilder().serializeNulls().create();
	 * </pre>
	 */
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
	 * 写入 JSON 时是否使用 {@link GsonBuilder#setPrettyPrinting()}。
	 * 这是按如下方式设置 {@code Gson} 的快捷方式：
	 * <pre class="code">
	 * new GsonBuilder().setPrettyPrinting().create();
	 * </pre>
	 */
	public void setPrettyPrinting(boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	/**
	 * 写入 JSON 时是否使用 {@link GsonBuilder#disableHtmlEscaping()}。
	 * 设置为 {@code true} 可禁用 JSON 中的 HTML 转义。这是
	 * 按如下方式设置 {@code Gson} 的快捷方式：
	 * <pre class="code">
	 * new GsonBuilder().disableHtmlEscaping().create();
	 * </pre>
	 */
	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	/**
	 * 使用 {@link SimpleDateFormat} 风格的模式定义日期/时间格式。
	 * 这是按如下方式设置 {@code Gson} 的快捷方式：
	 * <pre class="code">
	 * new GsonBuilder().setDateFormat(dateFormatPattern).create();
	 * </pre>
	 */
	public void setDateFormatPattern(String dateFormatPattern) {
		this.dateFormatPattern = dateFormatPattern;
	}


	@Override
	public void afterPropertiesSet() {
		GsonBuilder builder = (this.base64EncodeByteArrays ?
				GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays() : new GsonBuilder());
		if (this.serializeNulls) {
			builder.serializeNulls();
		}
		if (this.prettyPrinting) {
			builder.setPrettyPrinting();
		}
		if (this.disableHtmlEscaping) {
			builder.disableHtmlEscaping();
		}
		if (this.dateFormatPattern != null) {
			builder.setDateFormat(this.dateFormatPattern);
		}
		this.gson = builder.create();
	}


	/**
	 * 返回创建的 Gson 实例。
	 */
	@Override
	@Nullable
	public Gson getObject() {
		return this.gson;
	}

	@Override
	public Class<?> getObjectType() {
		return Gson.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
