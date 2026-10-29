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

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;

/**
 * 依赖作为请求路径前缀应用的固定版本的 {@code VersionStrategy}，例如缩短的 SHA、版本名称、发布日期等。
 *
 * <p>例如，当无法使用 {@link ContentVersionStrategy} 时（例如使用负责加载 JavaScript 资源并需要知道其相对路径的 JavaScript 模块加载器时），这很有用。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionResourceResolver
 */
public class FixedVersionStrategy extends AbstractPrefixVersionStrategy {

	private final Mono<String> versionMono;


	/**
	 * 使用给定版本字符串创建新的 FixedVersionStrategy。
	 * @param version the fixed version string to use
	 */
	public FixedVersionStrategy(String version) {
		super(version);
		this.versionMono = Mono.just(version);
	}


	@Override
	public Mono<String> getResourceVersion(Resource resource) {
		return this.versionMono;
	}

}
