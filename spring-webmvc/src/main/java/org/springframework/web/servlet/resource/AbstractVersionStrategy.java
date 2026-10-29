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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link VersionStrategy} 实现的抽象基类。
 *
 * <p>支持以下形式的版本：
 * <ul>
 * <li>请求路径中的前缀，例如 "version/static/myresource.js"
 * <li>请求路径中的文件名后缀，例如 "static/myresource-version.js"
 * </ul>
 *
 * <p>注意：此基类<i>不</i>提供生成版本字符串的支持。
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	private final VersionPathStrategy pathStrategy;


	protected AbstractVersionStrategy(VersionPathStrategy pathStrategy) {
		Assert.notNull(pathStrategy, "VersionPathStrategy is required");
		this.pathStrategy = pathStrategy;
	}


	public VersionPathStrategy getVersionPathStrategy() {
		return this.pathStrategy;
	}


	@Override
	@Nullable
	public String extractVersion(String requestPath) {
		return this.pathStrategy.extractVersion(requestPath);
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		return this.pathStrategy.removeVersion(requestPath, version);
	}

	@Override
	public String addVersion(String requestPath, String version) {
		return this.pathStrategy.addVersion(requestPath, version);
	}


	/**
	 * 基于前缀的 {@code VersionPathStrategy}，
	 * 例如 {@code "{version}/path/foo.js"}。
	 */
	protected static class PrefixVersionPathStrategy implements VersionPathStrategy {

		private final String prefix;

		public PrefixVersionPathStrategy(String version) {
			Assert.hasText(version, "Version must not be empty");
			this.prefix = version;
		}

		@Override
		@Nullable
		public String extractVersion(String requestPath) {
			return (requestPath.startsWith(this.prefix) ? this.prefix : null);
		}

		@Override
		public String removeVersion(String requestPath, String version) {
			return requestPath.substring(this.prefix.length());
		}

		@Override
		public String addVersion(String path, String version) {
			if (path.startsWith(".")) {
				return path;
			}
			else {
				return (this.prefix.endsWith("/") || path.startsWith("/") ?
						this.prefix + path : this.prefix + '/' + path);
			}
		}
	}


	/**
	 * 基于文件名的 {@code VersionPathStrategy}，
	 * 例如 {@code "path/foo-{version}.css"}。
	 */
	protected static class FileNameVersionPathStrategy implements VersionPathStrategy {

		private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");

		@Override
		@Nullable
		public String extractVersion(String requestPath) {
			Matcher matcher = pattern.matcher(requestPath);
			if (matcher.find()) {
				String match = matcher.group(1);
				return (match.contains("-") ? match.substring(match.lastIndexOf('-') + 1) : match);
			}
			else {
				return null;
			}
		}

		@Override
		public String removeVersion(String requestPath, String version) {
			return StringUtils.delete(requestPath, "-" + version);
		}

		@Override
		public String addVersion(String requestPath, String version) {
			String baseFilename = StringUtils.stripFilenameExtension(requestPath);
			String extension = StringUtils.getFilenameExtension(requestPath);
			return (baseFilename + '-' + version + '.' + extension);
		}
	}

}
