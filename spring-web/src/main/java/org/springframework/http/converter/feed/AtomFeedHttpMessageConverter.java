/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.converter.feed;

import com.rometools.rome.feed.atom.Feed;

import org.springframework.http.MediaType;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} 的实现，
 * 可读写 Atom feed。具体而言，此转换器可以处理
 * <a href="https://github.com/rometools/rome">ROME</a> 项目中的 {@link Feed} 对象。
 *
 * <p><b>注意：从 Spring 4.1 起，这基于 ROME 的 {@code com.rometools}
 * 变体，版本 1.5。请升级您的构建依赖。</b>
 *
 * <p>默认情况下，此转换器读写媒体类型（{@code application/atom+xml}）。
 * 可以通过 {@link #setSupportedMediaTypes supportedMediaTypes} 属性覆盖。
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 * @see Feed
 */
public class AtomFeedHttpMessageConverter extends AbstractWireFeedHttpMessageConverter<Feed> {

	public AtomFeedHttpMessageConverter() {
		super(new MediaType("application", "atom+xml"));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Feed.class.isAssignableFrom(clazz);
	}

}
