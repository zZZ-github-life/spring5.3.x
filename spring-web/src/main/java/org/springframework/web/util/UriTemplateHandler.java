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

package org.springframework.web.util;

import java.net.URI;
import java.util.Map;

/**
 * 定义使用变量展开 URI 模板的方法。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see org.springframework.web.client.RestTemplate#setUriTemplateHandler(UriTemplateHandler)
 */
public interface UriTemplateHandler {

	/**
	 * 使用 URI 变量映射展开给定的 URI 模板。
	 * @param uriTemplate the URI template
	 * @param uriVariables variable values
	 * @return the created URI instance
	 */
	URI expand(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * 使用 URI 变量数组展开给定的 URI 模板。
	 * @param uriTemplate the URI template
	 * @param uriVariables variable values
	 * @return the created URI instance
	 */
	URI expand(String uriTemplate, Object... uriVariables);

}
