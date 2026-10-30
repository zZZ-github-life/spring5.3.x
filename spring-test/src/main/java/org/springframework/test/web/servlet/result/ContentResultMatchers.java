/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.MediaType;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 响应内容断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#content} 访问。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class ContentResultMatchers {

	private final XmlExpectationsHelper xmlHelper;

	private final JsonExpectationsHelper jsonHelper;


	/**
	 * 受保护的构造函数。
	 * 请使用 {@link MockMvcResultMatchers#content()}。
	 */
	protected ContentResultMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
		this.jsonHelper = new JsonExpectationsHelper();
	}


	/**
	 * 断言 ServletResponse 的内容类型。给定的内容类型必须完全匹配，
	 * 包括类型、子类型和参数。若只需检查类型和子类型，请参见
	 * {@link #contentTypeCompatibleWith(String)}。
	 */
	public ResultMatcher contentType(String contentType) {
		return contentType(MediaType.parseMediaType(contentType));
	}

	/**
	 * 将 ServletResponse 的内容类型解析为 MediaType 后对其进行断言。
	 * 给定的内容类型必须完全匹配，包括类型、子类型和参数。
	 * 若只需检查类型和子类型，请参见 {@link #contentTypeCompatibleWith(MediaType)}。
	 */
	public ResultMatcher contentType(MediaType contentType) {
		return result -> {
			String actual = result.getResponse().getContentType();
			assertNotNull("Content type not set", actual);
			assertEquals("Content type", contentType, MediaType.parseMediaType(actual));
		};
	}

	/**
	 * 断言 ServletResponse 的内容类型与给定内容类型兼容，
	 * 兼容性按 {@link MediaType#isCompatibleWith(MediaType)} 的定义判断。
	 */
	public ResultMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * 断言 ServletResponse 的内容类型与给定内容类型兼容，
	 * 兼容性按 {@link MediaType#isCompatibleWith(MediaType)} 的定义判断。
	 */
	public ResultMatcher contentTypeCompatibleWith(MediaType contentType) {
		return result -> {
			String actual = result.getResponse().getContentType();
			assertNotNull("Content type not set", actual);
			MediaType actualContentType = MediaType.parseMediaType(actual);
			assertTrue("Content type [" + actual + "] is not compatible with [" + contentType + "]",
					actualContentType.isCompatibleWith(contentType));
		};
	}

	/**
	 * 断言 ServletResponse 中的字符编码。
	 * @since 5.3.10
	 * @see StandardCharsets
	 * @see #encoding(String)
	 */
	public ResultMatcher encoding(Charset characterEncoding) {
		return encoding(characterEncoding.name());
	}

	/**
	 * 断言 ServletResponse 中的字符编码。
	 * @see HttpServletResponse#getCharacterEncoding()
	 */
	public ResultMatcher encoding(String characterEncoding) {
		return result -> {
			String actual = result.getResponse().getCharacterEncoding();
			assertEquals("Character encoding", characterEncoding, actual);
		};
	}

	/**
	 * 使用 Hamcrest {@link Matcher} 断言响应体内容。
	 * <pre class="code">
	 * mockMvc.perform(get("/path"))
	 *   .andExpect(content().string(containsString("text")));
	 * </pre>
	 */
	public ResultMatcher string(Matcher<? super String> matcher) {
		return result -> assertThat("Response content", result.getResponse().getContentAsString(), matcher);
	}

	/**
	 * 以字符串形式断言响应体内容。
	 */
	public ResultMatcher string(String expectedContent) {
		return result -> assertEquals("Response content", expectedContent, result.getResponse().getContentAsString());
	}

	/**
	 * 以字节数组形式断言响应体内容。
	 */
	public ResultMatcher bytes(byte[] expectedContent) {
		return result -> assertEquals("Response content", expectedContent, result.getResponse().getContentAsByteArray());
	}

	/**
	 * 将响应内容和给定字符串解析为 XML，并断言两者“相似”——
	 * 即无论顺序如何，它们包含相同的元素和属性。
	 * <p>使用此匹配器需要 <a
	 * href="https://www.xmlunit.org/">XMLUnit</a> 库。
	 * @param xmlContent 期望的 XML 内容
	 * @see MockMvcResultMatchers#xpath(String, Object...)
	 * @see MockMvcResultMatchers#xpath(String, Map, Object...)
	 */
	public ResultMatcher xml(String xmlContent) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			this.xmlHelper.assertXmlEqual(xmlContent, content);
		};
	}

	/**
	 * 将响应内容解析为 {@link Node}，并应用给定的 Hamcrest {@link Matcher}。
	 */
	public ResultMatcher node(Matcher<? super Node> matcher) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			this.xmlHelper.assertNode(content, matcher);
		};
	}

	/**
	 * 将响应内容解析为 {@link DOMSource}，并应用给定的 Hamcrest {@link Matcher}。
	 * @see <a href="https://code.google.com/p/xml-matchers/">xml-matchers</a>
	 */
	public ResultMatcher source(Matcher<? super Source> matcher) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			this.xmlHelper.assertSource(content, matcher);
		};
	}

	/**
	 * 将预期字符串和实际字符串解析为 JSON，并断言两者“相似”——
	 * 即无论格式如何，它们包含相同的属性-值对，且使用宽松检查
	 * （可扩展，数组顺序不严格）。
	 * @param jsonContent 期望的 JSON 内容
	 * @since 4.1
	 */
	public ResultMatcher json(String jsonContent) {
		return json(jsonContent, false);
	}

	/**
	 * 将响应内容和给定字符串解析为 JSON，并断言两者“相似”——
	 * 即无论格式如何，它们包含相同的属性-值对。
	 * <p>根据 {@code strict} 参数值，可以按两种模式进行比较：
	 * <ul>
	 * <li>{@code true}：严格检查。不可扩展，且数组顺序严格。</li>
	 * <li>{@code false}：宽松检查。可扩展，且数组顺序不严格。</li>
	 * </ul>
	 * <p>使用此匹配器需要 <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> 库。
	 * @param jsonContent 期望的 JSON 内容
	 * @param strict 启用严格检查
	 * @since 4.2
	 */
	public ResultMatcher json(String jsonContent, boolean strict) {
		return result -> {
			String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			this.jsonHelper.assertJsonEqual(jsonContent, content, strict);
		};
	}

}
