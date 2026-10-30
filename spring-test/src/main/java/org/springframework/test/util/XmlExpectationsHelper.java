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

package org.springframework.test.util;

import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 用于 XML 内容断言的辅助类。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XmlExpectationsHelper {

	/**
	 * 将内容解析为 {@link Node} 并应用 {@link Matcher}。
	 */
	public void assertNode(String content, Matcher<? super Node> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", document, matcher);
	}

	private Document parseXmlString(String xml) throws Exception  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new StringReader(xml));
		return documentBuilder.parse(inputSource);
	}

	/**
	 * 将内容解析为 {@link DOMSource} 并应用 {@link Matcher}。
	 * @see <a href="https://github.com/davidehringer/xml-matchers">xml-matchers</a>
	 */
	public void assertSource(String content, Matcher<? super Source> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", new DOMSource(document), matcher);
	}

	/**
	 * 将期望内容字符串和实际内容字符串解析为 XML，并断言两者“相似” -- 即无论顺序如何，它们都包含相同的元素和属性。
	 * <p>使用此方法假定 <a href="https://github.com/xmlunit/xmlunit">XMLUnit</a> 库可用。
	 * @param expected 期望的 XML 内容
	 * @param actual 实际的 XML 内容
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers#xpath(String, Object...)
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers#xpath(String, Map, Object...)
	 */
	public void assertXmlEqual(String expected, String actual) throws Exception {
		XmlUnitDiff diff = new XmlUnitDiff(expected, actual);
		if (diff.hasDifferences()) {
			AssertionErrors.fail("Body content " + diff.toString());
		}
	}


	/**
	 * 用于避免对 XML Unit 产生硬依赖的内部类。
	 */
	private static class XmlUnitDiff {

		private final Diff diff;


		XmlUnitDiff(String expected, String actual) {
			this.diff = DiffBuilder.compare(expected).withTest(actual)
					.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
					.ignoreWhitespace().ignoreComments()
					.checkForSimilar()
					.build();
		}


		public boolean hasDifferences() {
			return this.diff.hasDifferences();
		}

		@Override
		public String toString() {
			return this.diff.toString();
		}

	}

}
