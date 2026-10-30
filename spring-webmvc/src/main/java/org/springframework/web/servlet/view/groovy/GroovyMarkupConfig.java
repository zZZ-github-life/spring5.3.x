/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.view.groovy;

import groovy.text.markup.MarkupTemplateEngine;

/**
 * 用于在 Web 环境中配置和管理 Groovy {@link MarkupTemplateEngine}
 * 以便自动查找的接口。由 {@link GroovyMarkupView} 检测并使用。
 *
 * @author Brian Clozel
 * @since 4.1
 * @see GroovyMarkupConfigurer
 */
public interface GroovyMarkupConfig {

	/**
	 * 返回当前 Web 应用上下文的 Groovy {@link MarkupTemplateEngine}。
	 * 它可能专属于某个 servlet，也可能在根上下文中共享。
	 * @return Groovy 的 MarkupTemplateEngine 引擎
	 */
	MarkupTemplateEngine getTemplateEngine();

}
