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

package org.springframework.web.servlet.view.xml;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.stream.StreamResult;

import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Spring-MVC {@link View}，允许在响应渲染期间将 {@link Marshaller} 编组（marshalling）的结果
 * 作为响应上下文内容输出。
 *
 * <p>要编组的对象作为 model 中的参数提供，
 * 然后在渲染响应时被{@linkplain #locateToBeMarshalled(Map) 检测}。
 * 用户可以通过 {@link #setModelKey(String) sourceKey} 属性在 model 中指定特定条目，
 * 或者让 Spring 自行定位源对象。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MarshallingView extends AbstractView {

	/**
	 * 默认内容类型。可作为 bean 属性覆盖。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	@Nullable
	private Marshaller marshaller;

	@Nullable
	private String modelKey;


	/**
	 * 构造一个未设置 {@link Marshaller} 的 {@code MarshallingView}。
	 * 必须在构造之后通过调用 {@link #setMarshaller} 设置 marshaller。
	 */
	public MarshallingView() {
		setContentType(DEFAULT_CONTENT_TYPE);
		setExposePathVariables(false);
	}

	/**
	 * 构造一个设置了给定 {@link Marshaller} 的 {@code MarshallingView}。
	 */
	public MarshallingView(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
	}


	/**
	 * 设置此视图要使用的 {@link Marshaller}。
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 设置表示待编组对象的 model key 名称。
	 * 如果未指定，将在 model map 中搜索受支持的值类型。
	 * @see Marshaller#supports(Class)
	 */
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected void initApplicationContext() {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
	}


	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Object toBeMarshalled = locateToBeMarshalled(model);
		if (toBeMarshalled == null) {
			throw new IllegalStateException("Unable to locate object to be marshalled in model: " + model);
		}

		Assert.state(this.marshaller != null, "No Marshaller set");
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		this.marshaller.marshal(toBeMarshalled, new StreamResult(baos));

		setResponseContentType(request, response);
		response.setContentLength(baos.size());
		baos.writeTo(response.getOutputStream());
	}

	/**
	 * 定位要编组的对象。
	 * <p>默认实现会首先尝试在已配置的
	 * {@linkplain #setModelKey(String) model key}（如果有）下查找，
	 * 然后再尝试定位 {@linkplain Marshaller#supports(Class) 受支持类型}的对象。
	 * @param model 模型 Map
	 * @return 要编组的对象（如果未找到则为 {@code null}）
	 * @throws IllegalStateException 如果由 {@linkplain #setModelKey(String) model key} 指定的模型对象不受编组器支持
	 * @see #setModelKey(String)
	 */
	@Nullable
	protected Object locateToBeMarshalled(Map<String, Object> model) throws IllegalStateException {
		if (this.modelKey != null) {
			Object value = model.get(this.modelKey);
			if (value == null) {
				throw new IllegalStateException("Model contains no object with key [" + this.modelKey + "]");
			}
			if (!isEligibleForMarshalling(this.modelKey, value)) {
				throw new IllegalStateException("Model object [" + value + "] retrieved via key [" +
						this.modelKey + "] is not supported by the Marshaller");
			}
			return value;
		}
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			Object value = entry.getValue();
			if (value != null && (model.size() == 1 || !(value instanceof BindingResult)) &&
					isEligibleForMarshalling(entry.getKey(), value)) {
				return value;
			}
		}
		return null;
	}

	/**
	 * 检查当前视图 model 中的给定值是否适合通过已配置的 {@link Marshaller} 进行编组。
	 * <p>默认实现会调用 {@link Marshaller#supports(Class)}，
	 * 并在适用时先对给定的 {@link JAXBElement} 进行解包。
	 * @param modelKey 该值在模型中的键（从不为 {@code null}）
	 * @param value 要检查的值（从不为 {@code null}）
	 * @return 给定值是否应被视为符合条件
	 * @see Marshaller#supports(Class)
	 */
	protected boolean isEligibleForMarshalling(String modelKey, Object value) {
		Assert.state(this.marshaller != null, "No Marshaller set");
		Class<?> classToCheck = value.getClass();
		if (value instanceof JAXBElement) {
			classToCheck = ((JAXBElement<?>) value).getDeclaredType();
		}
		return this.marshaller.supports(classToCheck);
	}

}
