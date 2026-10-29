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

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;

/**
 * 用于 URI 路径模式的解析器，生成随后可与请求匹配的
 * {@link PathPattern} 实例。
 *
 * <p>{@link PathPatternParser} 和 {@link PathPattern} 专为 Web 应用中的
 * HTTP URL 路径而设计；在这类应用中，大量 URI 路径模式会持续与传入请求
 * 匹配，因此需要高效匹配。
 *
 * <p>有关路径模式语法的详细信息，请参见 {@link PathPattern}。
 *
 * @author Andy Clement
 * @since 5.0
 */
public class PathPatternParser {

	private boolean matchOptionalTrailingSeparator = true;

	private boolean caseSensitive = true;

	private PathContainer.Options pathOptions = PathContainer.Options.HTTP_PATH;


	/**
	 * 此解析器生成的 {@link PathPattern} 是否应自动匹配带末尾斜杠的
	 * 请求路径。
	 * <p>如果设置为 {@code true}，不带末尾斜杠的 {@code PathPattern}
	 * 也将匹配带末尾斜杠的请求路径。如果设置为
	 * {@code false}，{@code PathPattern} 将只匹配带末尾斜杠的请求路径。
	 * <p>默认值为 {@code true}。
	 */
	public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
		this.matchOptionalTrailingSeparator = matchOptionalTrailingSeparator;
	}

	/**
	 * 是否启用了可选的末尾斜杠匹配。
	 */
	public boolean isMatchOptionalTrailingSeparator() {
		return this.matchOptionalTrailingSeparator;
	}

	/**
	 * 路径模式匹配是否应区分大小写。
	 * <p>默认值为 {@code true}。
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * 是否启用了区分大小写的模式匹配。
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	/**
	 * 设置解析模式时使用的选项。这些选项应与解析输入路径时使用的
	 * 选项相同。
	 * <p>默认使用
	 * {@link org.springframework.http.server.PathContainer.Options#HTTP_PATH}。
	 * @since 5.2
	 */
	public void setPathOptions(PathContainer.Options pathOptions) {
		this.pathOptions = pathOptions;
	}

	/**
	 * 返回{@link #setPathOptions 已配置}的模式解析选项。
	 * @since 5.2
	 */
	public PathContainer.Options getPathOptions() {
		return this.pathOptions;
	}


	/**
	 * 逐个字符地处理路径模式内容，在分隔符边界处将其拆分为路径元素，
	 * 并在每个阶段验证结构。生成一个可用于快速匹配路径的 PathPattern
	 * 对象。每次调用此方法都会委托给一个新的
	 * {@link InternalPathPatternParser} 实例，因为该类不是线程安全的。
	 * @param pathPattern the input path pattern, e.g. /project/{name}
	 * @return a PathPattern for quickly matching paths against request paths
	 * @throws PatternParseException in case of parse errors
	 */
	public PathPattern parse(String pathPattern) throws PatternParseException {
		return new InternalPathPatternParser(this).parse(pathPattern);
	}


	/**
	 * 共享的只读 {@code PathPatternParser} 实例。使用默认设置：
	 * <ul>
	 * <li>{@code matchOptionalTrailingSeparator=true}
	 * <li>{@code caseSensitivetrue}
	 * <li>{@code pathOptions=PathContainer.Options.HTTP_PATH}
	 * </ul>
	 */
	public final static PathPatternParser defaultInstance = new PathPatternParser() {

		@Override
		public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
			raiseError();
		}

		@Override
		public void setCaseSensitive(boolean caseSensitive) {
			raiseError();
		}

		@Override
		public void setPathOptions(PathContainer.Options pathOptions) {
			raiseError();
		}

		private void raiseError() {
			throw new UnsupportedOperationException(
					"This is a read-only, shared instance that cannot be modified");
		}
	};
}
