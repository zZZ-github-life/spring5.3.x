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

package org.springframework.web.accept;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 从查询参数解析所请求内容类型的策略。
 * 默认查询参数名为 {@literal "format"}。
 *
 * <p>你可以通过 {@link #addMapping(String, MediaType)} 注册键（即查询参数的预期值）
 * 与 MediaType 之间的静态映射。
 * 从 5.0 开始，此策略还支持通过
 * {@link org.springframework.http.MediaTypeFactory#getMediaType} 对键进行动态查找。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private String parameterName = "format";


	/**
	 * 使用给定的文件扩展名与媒体类型映射创建实例。
	 */
	public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * 设置用于确定所请求媒体类型的参数名称。
	 * <p>默认情况下设置为 {@code "format"}。
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	@Nullable
	protected String getMediaTypeKey(NativeWebRequest request) {
		return request.getParameter(getParameterName());
	}

}
