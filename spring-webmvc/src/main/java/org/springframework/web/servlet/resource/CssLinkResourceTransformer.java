/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceTransformer} 实现，用于修改 CSS 文件中的链接，
 * 使其与应暴露给客户端的公共 URL 路径匹配（例如，
 * 在 URL 中插入基于内容计算的 MD5 哈希）。
 *
 * <p>该实现会查找 CSS {@code @import} 语句中的链接，
 * 以及 CSS {@code url()} 函数中的链接。随后所有链接都会经过
 * {@link ResourceResolverChain}，并相对于
 * 包含它们的 CSS 文件所在位置进行解析。如果解析成功，则修改链接，
 * 否则保留原始链接。
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Log logger = LogFactory.getLog(CssLinkResourceTransformer.class);

	private final List<LinkParser> linkParsers = new ArrayList<>(2);


	public CssLinkResourceTransformer() {
		this.linkParsers.add(new ImportStatementLinkParser());
		this.linkParsers.add(new UrlFunctionLinkParser());
	}


	@SuppressWarnings("deprecation")
	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		resource = transformerChain.transform(request, resource);

		String filename = resource.getFilename();
		if (!"css".equals(StringUtils.getFilenameExtension(filename)) ||
				resource instanceof EncodedResourceResolver.EncodedResource ||
				resource instanceof GzipResourceResolver.GzippedResource) {
			return resource;
		}

		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		String content = new String(bytes, DEFAULT_CHARSET);

		SortedSet<ContentChunkInfo> links = new TreeSet<>();
		for (LinkParser parser : this.linkParsers) {
			parser.parse(content, links);
		}

		if (links.isEmpty()) {
			return resource;
		}

		int index = 0;
		StringWriter writer = new StringWriter();
		for (ContentChunkInfo linkContentChunkInfo : links) {
			writer.write(content.substring(index, linkContentChunkInfo.getStart()));
			String link = content.substring(linkContentChunkInfo.getStart(), linkContentChunkInfo.getEnd());
			String newLink = null;
			if (!hasScheme(link)) {
				String absolutePath = toAbsolutePath(link, request);
				newLink = resolveUrlPath(absolutePath, request, resource, transformerChain);
			}
			writer.write(newLink != null ? newLink : link);
			index = linkContentChunkInfo.getEnd();
		}
		writer.write(content.substring(index));

		return new TransformedResource(resource, writer.toString().getBytes(DEFAULT_CHARSET));
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(':');
		return ((schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0);
	}


	/**
	 * 提取表示链接的内容片段。
	 */
	@FunctionalInterface
	protected interface LinkParser {

		void parse(String content, SortedSet<ContentChunkInfo> result);

	}


	/**
	 * {@link LinkParser} 实现的抽象基类。
	 */
	protected abstract static class AbstractLinkParser implements LinkParser {

		/** 返回用于搜索链接的关键字，例如 "@import"、"url("。 */
		protected abstract String getKeyword();

		@Override
		public void parse(String content, SortedSet<ContentChunkInfo> result) {
			int position = 0;
			while (true) {
				position = content.indexOf(getKeyword(), position);
				if (position == -1) {
					return;
				}
				position += getKeyword().length();
				while (Character.isWhitespace(content.charAt(position))) {
					position++;
				}
				if (content.charAt(position) == '\'') {
					position = extractLink(position, "'", content, result);
				}
				else if (content.charAt(position) == '"') {
					position = extractLink(position, "\"", content, result);
				}
				else {
					position = extractLink(position, content, result);
				}
			}
		}

		protected int extractLink(int index, String endKey, String content, SortedSet<ContentChunkInfo> linksToAdd) {
			int start = index + 1;
			int end = content.indexOf(endKey, start);
			linksToAdd.add(new ContentChunkInfo(start, end));
			return end + endKey.length();
		}

		/**
		 * 在关键字匹配之后、移除空白之后，且当
		 * 下一个字符既不是单引号也不是双引号时调用。
		 */
		protected abstract int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd);
	}


	private static class ImportStatementLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "@import";
		}

		@Override
		protected int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd) {
			if (content.startsWith("url(", index)) {
				// Ignore: UrlFunctionLinkParser will handle it.
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Unexpected syntax for @import link at index " + index);
			}
			return index;
		}
	}


	private static class UrlFunctionLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "url(";
		}

		@Override
		protected int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd) {
			// A url() function without unquoted
			return extractLink(index - 1, ")", content, linksToAdd);
		}
	}


	private static class ContentChunkInfo implements Comparable<ContentChunkInfo> {

		private final int start;

		private final int end;

		ContentChunkInfo(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		@Override
		public int compareTo(ContentChunkInfo other) {
			return Integer.compare(this.start, other.start);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ContentChunkInfo)) {
				return false;
			}
			ContentChunkInfo otherCci = (ContentChunkInfo) other;
			return (this.start == otherCci.start && this.end == otherCci.end);
		}

		@Override
		public int hashCode() {
			return this.start * 31 + this.end;
		}
	}

}
