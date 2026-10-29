/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server;

import java.util.List;

import org.springframework.util.MultiValueMap;

/**
 * URI 路径的结构化表示，通过 {@link #parsePath(String)} 解析为
 * 一系列 {@link Separator} 和 {@link PathSegment} 元素。
 *
 * <p>每个 {@link PathSegment} 以解码形式公开其内容，并移除路径参数。
 * 这样可以安全地逐个路径段进行匹配，而不会因为解码后的保留字符
 * 改变路径结构而产生风险。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface PathContainer {

	/**
	 * 此实例所解析的原始路径。
	 */
	String value();

	/**
	 * 包含的路径元素，为 {@link Separator} 或 {@link PathSegment}。
	 */
	List<Element> elements();

	/**
	 * 从元素列表中的给定偏移量提取子路径。
	 * @param index the start element index (inclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int index) {
		return subPath(index, elements().size());
	}

	/**
	 * 从元素列表的给定起始偏移量（含）到结束偏移量（不含）
	 * 提取子路径。
	 * @param startIndex the start element index (inclusive)
	 * @param endIndex the end element index (exclusive)
	 * @return the sub-path
	 */
	default PathContainer subPath(int startIndex, int endIndex) {
		return DefaultPathContainer.subPath(this, startIndex, endIndex);
	}


	/**
	 * 将路径值解析为一系列 {@code "/"} {@link Separator Separator}
	 * 和 {@link PathSegment PathSegment} 元素。
	 * @param path the encoded, raw path value to parse
	 * @return the parsed path
	 */
	static PathContainer parsePath(String path) {
		return DefaultPathContainer.createFromUrlPath(path, Options.HTTP_PATH);
	}

	/**
	 * 将路径值解析为一系列 {@link Separator Separator} 和
	 * {@link PathSegment PathSegment} 元素。
	 * @param path the encoded, raw path value to parse
	 * @param options to customize parsing
	 * @return the parsed path
	 * @since 5.2
	 */
	static PathContainer parsePath(String path, Options options) {
		return DefaultPathContainer.createFromUrlPath(path, options);
	}


	/**
	 * 路径元素，即分隔符或路径段。
	 */
	interface Element {

		/**
		 * 此元素的未经修改的原始值。
		 */
		String value();
	}


	/**
	 * 路径分隔符元素。
	 */
	interface Separator extends Element {
	}


	/**
	 * 路径段元素。
	 */
	interface PathSegment extends Element {

		/**
		 * 返回用于路径匹配的路径段值（已解码并清理）。
		 */
		String valueToMatch();

		/**
		 * 将 {@link #valueToMatch()} 公开为字符数组。
		 */
		char[] valueToMatchAsChars();

		/**
		 * 与此路径段关联的路径参数。
		 * @return an unmodifiable map containing the parameters
		 */
		MultiValueMap<String, String> parameters();
	}


	/**
	 * 用于根据输入路径类型自定义解析的选项。
	 * @since 5.2
	 */
	class Options {

		/**
		 * HTTP URL 路径的选项。
		 * <p>使用分隔符 '/'，并进行 URL 解码和路径参数解析。
		 */
		public final static Options HTTP_PATH = Options.create('/', true);

		/**
		 * 消息路由的选项。
		 * <p>使用分隔符 '.'，既不进行 URL 解码，也不解析路径参数。
		 * 段值中分隔符字符的转义序列仍会被解码。
		 */
		public final static Options MESSAGE_ROUTE = Options.create('.', false);

		private final char separator;

		private final boolean decodeAndParseSegments;

		private Options(char separator, boolean decodeAndParseSegments) {
			this.separator = separator;
			this.decodeAndParseSegments = decodeAndParseSegments;
		}

		public char separator() {
			return this.separator;
		}

		public boolean shouldDecodeAndParseSegments() {
			return this.decodeAndParseSegments;
		}

		/**
		 * 使用给定设置创建 {@link Options} 实例。
		 * @param separator the separator for parsing the path into segments;
		 * currently this must be slash or dot.
		 * @param decodeAndParseSegments whether to URL decode path segment
		 * values and parse path parameters. If set to false, only escape
		 * sequences for the separator char are decoded.
		 */
		public static Options create(char separator, boolean decodeAndParseSegments) {
			return new Options(separator, decodeAndParseSegments);
		}
	}

}
