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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

/**
 * Atom Feed 视图的抽象超类，使用
 * <a href="https://github.com/rometools/rome">ROME</a> 包。
 *
 * <p><b>注意：从 Spring 4.1 开始，此实现基于 ROME 的 {@code com.rometools}
 * 变体，版本为 1.5。请升级你的构建依赖。</b>
 *
 * <p>应用特有的视图类将扩展此类。
 * 视图内容保存在子类中，而不是保存在模板中。
 * 主要入口点是 {@link #buildFeedMetadata} 和 {@link #buildFeedEntries}。
 *
 * <p>感谢 Jettro Coenradie 和 Sergio Bossa 提供原始的 feed 视图原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see #buildFeedMetadata
 * @see #buildFeedEntries
 * @see <a href="https://www.atomenabled.org/developers/syndication/">Atom Syndication Format</a>
 */
public abstract class AbstractAtomFeedView extends AbstractFeedView<Feed> {

	/**
	 * 所使用的默认 feed 类型。
	 */
	public static final String DEFAULT_FEED_TYPE = "atom_1.0";

	private String feedType = DEFAULT_FEED_TYPE;


	public AbstractAtomFeedView() {
		setContentType("application/atom+xml");
	}

	/**
	 * 设置要使用的 Rome feed 类型。
	 * <p>默认为 Atom 1.0。
	 * @see Feed#setFeedType(String)
	 * @see #DEFAULT_FEED_TYPE
	 */
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}

	/**
	 * 创建一个新的 Feed 实例以承载条目。
	 * <p>默认返回 Atom 1.0 feed，但子类可以指定任意 Feed。
	 * @see #setFeedType(String)
	 */
	@Override
	protected Feed newFeed() {
		return new Feed(this.feedType);
	}

	/**
	 * 调用 {@link #buildFeedEntries(Map, HttpServletRequest, HttpServletResponse)}
	 * 以获取 feed 条目列表。
	 */
	@Override
	protected final void buildFeedEntries(Map<String, Object> model, Feed feed,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		List<Entry> entries = buildFeedEntries(model, request, response);
		feed.setEntries(entries);
	}

	/**
	 * 子类必须实现此方法，以便根据 model 构建 feed 条目。
	 * <p>注意，传入的 HTTP response 仅应用于设置 cookie 或其他 HTTP 头。
	 * 构建好的 feed 本身将在该方法返回后自动写入 response。
	 * @param model 模型 Map
	 * @param request 在需要 Locale 等情况下使用。不应查看属性。
	 * @param response 在需要设置 Cookie 的情况下使用。不应向其写入。
	 * @return 要添加到 feed 的条目
	 * @throws Exception 文档构建期间发生的任何异常
	 * @see Entry
	 */
	protected abstract List<Entry> buildFeedEntries(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
