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

package org.springframework.web.servlet.view.feed;

import java.io.OutputStreamWriter;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Atom 和 RSS Feed 视图的抽象基类，使用
 * <a href="https://github.com/rometools/rome">ROME</a> 包。
 *
 * <p><b>注意：从 Spring 4.1 开始，此实现基于 ROME 的 {@code com.rometools}
 * 变体，版本为 1.5。请升级你的构建依赖。</b>
 *
 * <p>应用特有的视图类通常应扩展
 * {@link AbstractRssFeedView} 或 {@link AbstractAtomFeedView}，而不是直接扩展此类。
 *
 * <p>感谢 Jettro Coenradie 和 Sergio Bossa 提供原始的 feed 视图原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> the {@link WireFeed} type
 * @see AbstractRssFeedView
 * @see AbstractAtomFeedView
 */
public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		T wireFeed = newFeed();
		buildFeedMetadata(model, wireFeed, request);
		buildFeedEntries(model, wireFeed, request, response);

		setResponseContentType(request, response);
		if (!StringUtils.hasText(wireFeed.getEncoding())) {
			wireFeed.setEncoding("UTF-8");
		}

		WireFeedOutput feedOutput = new WireFeedOutput();
		ServletOutputStream out = response.getOutputStream();
		feedOutput.output(wireFeed, new OutputStreamWriter(out, wireFeed.getEncoding()));
		out.flush();
	}

	/**
	 * 创建一个新的 feed 以承载条目。
	 * @return the newly created Feed instance
	 */
	protected abstract T newFeed();

	/**
	 * 填充 feed 元数据（标题、链接、描述等）。
	 * <p>默认实现为空。子类可以重写此方法，
	 * 添加标题、链接描述等元数据字段。
	 * @param model the model, in case meta information must be populated from it
	 * @param feed the feed being populated
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 */
	protected void buildFeedMetadata(Map<String, Object> model, T feed, HttpServletRequest request) {
	}

	/**
	 * 子类必须实现此方法，以便根据 model 构建 feed 条目。
	 * <p>注意，传入的 HTTP response 仅应用于设置 cookie 或其他 HTTP 头。
	 * 构建好的 feed 本身将在该方法返回后自动写入 response。
	 * @param model the model Map
	 * @param feed the feed to add entries to
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 * @throws Exception any exception that occurred during building
	 */
	protected abstract void buildFeedEntries(Map<String, Object> model, T feed,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
