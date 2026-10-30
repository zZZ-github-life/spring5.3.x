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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * {@code VersionStrategy}，它依赖作为请求路径前缀应用的固定版本，
 * 例如缩短的 SHA、版本名称、发布日期等。
 *
 * <p>例如，当无法使用 {@link ContentVersionStrategy} 时（例如使用
 * 负责加载 JavaScript 资源并需要知道其相对路径的
 * JavaScript 模块加载器时），这很有用。
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see VersionResourceResolver
 */
public class FixedVersionStrategy extends AbstractVersionStrategy {

	private final String version;


	/**
	 * 使用给定版本字符串创建一个新的 FixedVersionStrategy。
	 * @param version 要使用的固定版本字符串
	 */
	public FixedVersionStrategy(String version) {
		super(new PrefixVersionPathStrategy(version));
		this.version = version;
	}


	@Override
	public String getResourceVersion(Resource resource) {
		return this.version;
	}

}
