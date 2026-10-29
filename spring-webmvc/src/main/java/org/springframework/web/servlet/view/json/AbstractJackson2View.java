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

package org.springframework.web.servlet.view.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractView;

/**
 * 基于 Jackson 且与内容类型无关的 {@link AbstractView} 实现的抽象基类。
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.1
 */
public abstract class AbstractJackson2View extends AbstractView {

	private ObjectMapper objectMapper;

	private JsonEncoding encoding = JsonEncoding.UTF8;

	@Nullable
	private Boolean prettyPrint;

	private boolean disableCaching = true;

	protected boolean updateContentLength = false;


	protected AbstractJackson2View(ObjectMapper objectMapper, String contentType) {
		this.objectMapper = objectMapper;
		configurePrettyPrint();
		setContentType(contentType);
		setExposePathVariables(false);
	}

	/**
	 * 设置此视图的 {@code ObjectMapper}。
	 * 如果未设置，将使用默认的 {@link ObjectMapper#ObjectMapper() ObjectMapper}。
	 * <p>设置自定义配置的 {@code ObjectMapper} 是进一步控制 JSON 序列化过程的一种方式。
	 * 另一种方式是使用 Jackson 提供的注解标注要序列化的类型，
	 * 在这种情况下无需自定义配置的 ObjectMapper。
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * 返回此视图的 {@code ObjectMapper}。
	 */
	public final ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * 设置此视图的 {@code JsonEncoding}。
	 * 默认使用 {@linkplain JsonEncoding#UTF8 UTF-8}。
	 */
	public void setEncoding(JsonEncoding encoding) {
		Assert.notNull(encoding, "'encoding' must not be null");
		this.encoding = encoding;
	}

	/**
	 * 返回此视图的 {@code JsonEncoding}。
	 */
	public final JsonEncoding getEncoding() {
		return this.encoding;
	}

	/**
	 * 写出输出时是否使用默认的格式化输出（pretty printer）。
	 * 这是按如下方式配置 {@code ObjectMapper} 的快捷方式：
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * </pre>
	 * <p>默认值为 {@code false}。
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}

	/**
	 * 禁用对生成的 JSON 的缓存。
	 * <p>默认值为 {@code true}，这将阻止客户端缓存生成的 JSON。
	 */
	public void setDisableCaching(boolean disableCaching) {
		this.disableCaching = disableCaching;
	}

	/**
	 * 是否更新响应的 'Content-Length' 头。当设为
	 * {@code true} 时，将缓冲响应以确定内容长度
	 * 并设置响应的 'Content-Length' 头。
	 * <p>默认设置为 {@code false}。
	 */
	public void setUpdateContentLength(boolean updateContentLength) {
		this.updateContentLength = updateContentLength;
	}

	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		setResponseContentType(request, response);
		response.setCharacterEncoding(this.encoding.getJavaName());
		if (this.disableCaching) {
			response.addHeader("Cache-Control", "no-store");
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		ByteArrayOutputStream temporaryStream = null;
		OutputStream stream;

		if (this.updateContentLength) {
			temporaryStream = createTemporaryOutputStream();
			stream = temporaryStream;
		}
		else {
			stream = response.getOutputStream();
		}

		Object value = filterAndWrapModel(model, request);
		writeContent(stream, value);

		if (temporaryStream != null) {
			writeToResponse(response, temporaryStream);
		}
	}

	/**
	 * 过滤 model，并可选地将其包装到 {@link MappingJacksonValue} 容器中。
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @param request current HTTP request
	 * @return the wrapped or unwrapped value to be rendered
	 */
	protected Object filterAndWrapModel(Map<String, Object> model, HttpServletRequest request) {
		Object value = filterModel(model);
		Class<?> serializationView = (Class<?>) model.get(JsonView.class.getName());
		FilterProvider filters = (FilterProvider) model.get(FilterProvider.class.getName());
		if (serializationView != null || filters != null) {
			MappingJacksonValue container = new MappingJacksonValue(value);
			if (serializationView != null) {
				container.setSerializationView(serializationView);
			}
			if (filters != null) {
				container.setFilters(filters);
			}
			value = container;
		}
		return value;
	}

	/**
	 * 将实际的 JSON 内容写入输出流。
	 * @param stream the output stream to use
	 * @param object the value to be rendered, as returned from {@link #filterModel}
	 * @throws IOException if writing failed
	 */
	protected void writeContent(OutputStream stream, Object object) throws IOException {
		try (JsonGenerator generator = this.objectMapper.getFactory().createGenerator(stream, this.encoding)) {
			writePrefix(generator, object);

			Object value = object;
			Class<?> serializationView = null;
			FilterProvider filters = null;

			if (value instanceof MappingJacksonValue) {
				MappingJacksonValue container = (MappingJacksonValue) value;
				value = container.getValue();
				serializationView = container.getSerializationView();
				filters = container.getFilters();
			}

			ObjectWriter objectWriter = (serializationView != null ?
					this.objectMapper.writerWithView(serializationView) : this.objectMapper.writer());
			if (filters != null) {
				objectWriter = objectWriter.with(filters);
			}
			objectWriter.writeValue(generator, value);

			writeSuffix(generator, object);
			generator.flush();
		}
	}


	/**
	 * 设置 model 中应由此视图渲染的属性。
	 * 设置后，所有其他 model 属性都将被忽略。
	 */
	public abstract void setModelKey(String modelKey);

	/**
	 * 从给定 model 中过滤掉不需要的属性。
	 * 返回值可以是另一个 {@link Map}，也可以是单个值对象。
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @return the value to be rendered
	 */
	protected abstract Object filterModel(Map<String, Object> model);

	/**
	 * 在主要内容之前写入前缀。
	 * @param generator the generator to use for writing content.
	 * @param object the object to write to the output message.
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 在主要内容之后写入后缀。
	 * @param generator the generator to use for writing content.
	 * @param object the object to write to the output message.
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

}
