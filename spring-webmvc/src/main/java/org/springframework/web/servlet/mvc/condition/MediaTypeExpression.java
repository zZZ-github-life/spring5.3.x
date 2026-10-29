/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 媒体类型表达式（例如 "text/plain"、"!text/plain"）的约定，
 * 如 {@code @RequestMapping} 注解中为 "consumes" 和
 * "produces" 条件所定义。
 * @author Rossen Stoyanchev
 * @since 3.1
 *
 * @see RequestMapping#consumes()
 * @see RequestMapping#produces()
 */
public interface MediaTypeExpression {

	MediaType getMediaType();

	boolean isNegated();

}
