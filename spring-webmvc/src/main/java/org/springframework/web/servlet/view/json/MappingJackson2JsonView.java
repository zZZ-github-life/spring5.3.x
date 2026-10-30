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

package org.springframework.web.servlet.view.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;

/**
 * Spring MVC {@link View}，使用 <a href="https://github.com/FasterXML/jackson">Jackson 2</a>
 * 的 {@link ObjectMapper} 序列化当前请求的 model，从而渲染 JSON 内容。
 *
 * <p>默认情况下，model map 的全部内容（框架特定的类除外）都将被编码为 JSON。
 * 如果 model 只包含一个键，你可以通过 {@link #setExtractValueFromSingleKeyModel}
 * 将其单独提取出来并编码为 JSON。
 *
 * <p>默认构造器使用 {@link Jackson2ObjectMapperBuilder} 提供的默认配置。
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1.2
 */
public class MappingJackson2JsonView extends AbstractJackson2View {

	/**
	 * 默认内容类型："application/json"。
	 * 可以通过 {@link #setContentType} 覆盖。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/json";

	@Nullable
	private String jsonPrefix;

	@Nullable
	private Set<String> modelKeys;

	private boolean extractValueFromSingleKeyModel = false;


	/**
	 * 使用 {@link Jackson2ObjectMapperBuilder} 提供的默认配置构造一个新的
	 * {@code MappingJackson2JsonView}，并将内容类型设置为 {@code application/json}。
	 */
	public MappingJackson2JsonView() {
		super(Jackson2ObjectMapperBuilder.json().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * 使用提供的 {@link ObjectMapper} 构造一个新的 {@code MappingJackson2JsonView}，
	 * 并将内容类型设置为 {@code application/json}。
	 * @since 4.2.1
	 */
	public MappingJackson2JsonView(ObjectMapper objectMapper) {
		super(objectMapper, DEFAULT_CONTENT_TYPE);
	}


	/**
	 * 指定用于此视图 JSON 输出的自定义前缀。
	 * 默认为无前缀。
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图输出的 JSON 是否应以 <code>")]}', "</code> 作为前缀。
	 * 默认值为 {@code false}。
	 * <p>以这种方式为 JSON 字符串添加前缀有助于防止 JSON 劫持（JSON Hijacking）。
	 * 该前缀会使字符串在语法上无法作为脚本执行，从而无法被劫持。
	 * 在将字符串解析为 JSON 之前应去除该前缀。
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModelKey(String modelKey) {
		this.modelKeys = Collections.singleton(modelKey);
	}

	/**
	 * 设置 model 中应由此视图渲染的属性。
	 * 设置后，所有其他 model 属性都将被忽略。
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
		this.modelKeys = modelKeys;
	}

	/**
	 * 返回 model 中应由此视图渲染的属性。
	 */
	@Nullable
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}

	/**
	 * 设置是将会包含单个属性的 model 序列化为 map，
	 * 还是从 model 中提取单个值并直接对其进行序列化。
	 * <p>设置此标志的效果类似于在使用 {@code @ResponseBody}
	 * 请求处理方法时使用 {@code MappingJackson2HttpMessageConverter}。
	 * <p>默认值为 {@code false}。
	 */
	public void setExtractValueFromSingleKeyModel(boolean extractValueFromSingleKeyModel) {
		this.extractValueFromSingleKeyModel = extractValueFromSingleKeyModel;
	}

	/**
	 * 从给定 model 中过滤掉不需要的属性。
	 * 返回值可以是另一个 {@link Map}，也可以是单个值对象。
	 * <p>默认实现会移除 {@link BindingResult} 实例
	 * 以及未包含在 {@link #setModelKeys modelKeys} 属性中的条目。
	 * @param model 模型，将传递给 {@link #renderMergedOutputModel}
	 * @return 要渲染的值
	 */
	@Override
	protected Object filterModel(Map<String, Object> model) {
		Map<String, Object> result = CollectionUtils.newHashMap(model.size());
		Set<String> modelKeys = (!CollectionUtils.isEmpty(this.modelKeys) ? this.modelKeys : model.keySet());
		model.forEach((clazz, value) -> {
			if (!(value instanceof BindingResult) && modelKeys.contains(clazz) &&
					!clazz.equals(JsonView.class.getName()) &&
					!clazz.equals(FilterProvider.class.getName())) {
				result.put(clazz, value);
			}
		});
		return (this.extractValueFromSingleKeyModel && result.size() == 1 ? result.values().iterator().next() : result);
	}

	@Override
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
		if (this.jsonPrefix != null) {
			generator.writeRaw(this.jsonPrefix);
		}
	}

}
