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

package org.springframework.test.web.client.match;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 用于请求内容 {@code RequestMatcher} 的工厂。通常通过
 * {@link MockRestRequestMatchers#content()} 访问此类的实例。
 * @author Rossen Stoyanchev
 * @since 3.2
 */

public class ContentRequestMatchers {

	private final XmlExpectationsHelper xmlHelper;

	private final JsonExpectationsHelper jsonHelper;


	/**
	 * 类构造函数，不用于直接实例化。
	 * 请使用 {@link MockRestRequestMatchers#content()}。
	 */
	protected ContentRequestMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
		this.jsonHelper = new JsonExpectationsHelper();
	}


	/**
	 * 以 String 形式断言请求内容类型。
	 */
	public RequestMatcher contentType(String expectedContentType) {
		return contentType(MediaType.parseMediaType(expectedContentType));
	}

	/**
	 * 以 {@link MediaType} 形式断言请求内容类型。
	 */
	public RequestMatcher contentType(MediaType expectedContentType) {
		return request -> {
			MediaType actualContentType = request.getHeaders().getContentType();
			assertTrue("Content type not set", actualContentType != null);
			assertEquals("Content type", expectedContentType, actualContentType);
		};
	}

	/**
	 * 断言请求内容类型与给定内容类型兼容，
	 * 兼容性按 {@link MediaType#isCompatibleWith(MediaType)} 定义。
	 */
	public RequestMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * 断言请求内容类型与给定内容类型兼容，
	 * 兼容性按 {@link MediaType#isCompatibleWith(MediaType)} 定义。
	 */
	public RequestMatcher contentTypeCompatibleWith(MediaType contentType) {
		return request -> {
			MediaType actualContentType = request.getHeaders().getContentType();
			assertTrue("Content type not set", actualContentType != null);
			if (actualContentType != null) {
				assertTrue("Content type [" + actualContentType + "] is not compatible with [" + contentType + "]",
						actualContentType.isCompatibleWith(contentType));
			}
		};
	}

	/**
	 * 以 UTF-8 字符串形式获取请求体，并应用给定的 {@link Matcher}。
	 */
	public RequestMatcher string(Matcher<? super String> matcher) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertThat("Request content", mockRequest.getBodyAsString(), matcher);
		};
	}

	/**
	 * 以 UTF-8 字符串形式获取请求体，并与给定 String 进行比较。
	 */
	public RequestMatcher string(String expectedContent) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertEquals("Request content", expectedContent, mockRequest.getBodyAsString());
		};
	}

	/**
	 * 将请求体与给定字节数组进行比较。
	 */
	public RequestMatcher bytes(byte[] expectedContent) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertEquals("Request content", expectedContent, mockRequest.getBodyAsBytes());
		};
	}

	/**
	 * 将请求体解析为表单数据，并与给定 {@code MultiValueMap} 进行比较。
	 * @since 4.3
	 */
	public RequestMatcher formData(MultiValueMap<String, String> expected) {
		return formData(expected, true);
	}

	/**
	 * 对 {@link #formData(MultiValueMap)} 的变体，仅匹配给定子集的预期表单参数。
	 * @since 5.3
	 */
	public RequestMatcher formDataContains(Map<String, String> expected) {
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>(expected.size());
		expected.forEach(multiValueMap::add);
		return formData(multiValueMap, false);
	}

	private RequestMatcher formData(MultiValueMap<String, String> expectedMap, boolean containsExactly) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			MockHttpInputMessage message = new MockHttpInputMessage(mockRequest.getBodyAsBytes());
			message.getHeaders().putAll(mockRequest.getHeaders());
			MultiValueMap<String, String> actualMap = new FormHttpMessageConverter().read(null, message);
			if (containsExactly) {
				assertEquals("Form data", expectedMap, actualMap);
			}
			else {
				assertTrue("Form data " + actualMap, expectedMap.size() <= actualMap.size());
				for (Map.Entry<String, ? extends List<?>> entry : expectedMap.entrySet()) {
					String name = entry.getKey();
					List<?> values = entry.getValue();
					assertTrue("No form parameter '" + name + "'", actualMap.get(name) != null);
					assertTrue("Parameter value count " + values.size(), values.size() <= actualMap.get(name).size());
					for (int i = 0; i < values.size(); i++) {
						assertEquals("Form parameter", values.get(i), actualMap.get(name).get(i));
					}
				}
			}
		};
	}

	/**
	 * 将请求体解析为 multipart 数据，并断言它恰好包含
	 * 给定 {@code MultiValueMap} 中的值。值可以是以下类型：
	 * <ul>
	 * <li>{@code String} - 表单字段
	 * <li>{@link Resource} - 文件内容
	 * <li>{@code byte[]} - 其他原始内容
	 * </ul>
	 * <p><strong>注意：</strong>此方法使用 Apache Commons FileUpload
	 * 库解析 multipart 数据，该库必须在测试类路径上。
	 * @param expectedMap the expected multipart values
	 * @since 5.3
	 */
	public RequestMatcher multipartData(MultiValueMap<String, ?> expectedMap) {
		return multipartData(expectedMap, true);
	}

	/**
	 * 对 {@link #multipartData(MultiValueMap)} 的变体，功能相同，
	 * 但仅针对实际值的一个子集。
	 * @param expectedMap the expected multipart values
	 * @since 5.3
	 */
	public RequestMatcher multipartDataContains(Map<String, ?> expectedMap) {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>(expectedMap.size());
		expectedMap.forEach(map::add);
		return multipartData(map, false);
	}

	@SuppressWarnings("ConstantConditions")
	private RequestMatcher multipartData(MultiValueMap<String, ?> expectedMap, boolean containsExactly) {
		return request -> {
			MultiValueMap<String, ?> actualMap = MultipartHelper.parse(request);
			if (containsExactly) {
				assertEquals("Multipart request content: " + actualMap, expectedMap.size(), actualMap.size());
			}
			for (Map.Entry<String, ? extends List<?>> entry : expectedMap.entrySet()) {
				String name = entry.getKey();
				List<?> values = entry.getValue();
				assertTrue("No Multipart '" + name + "'", actualMap.get(name) != null);
				assertTrue("Multipart value count " + values.size(), containsExactly ?
						values.size() == actualMap.get(name).size() :
						values.size() <= actualMap.get(name).size());
				for (int i = 0; i < values.size(); i++) {
					Object expected = values.get(i);
					Object actual = actualMap.get(name).get(i);
					if (expected instanceof Resource) {
						expected = StreamUtils.copyToByteArray(((Resource) expected).getInputStream());
					}
					if (expected instanceof byte[]) {
						assertTrue("Multipart is not a file", actual instanceof MultipartFile);
						assertEquals("Multipart content", expected, ((MultipartFile) actual).getBytes());
					}
					else if (expected instanceof String) {
						assertTrue("Multipart is not a String", actual instanceof String);
						assertEquals("Multipart content", expected, actual);
					}
					else {
						throw new IllegalArgumentException("Unexpected multipart value: " + expected.getClass());
					}
				}
			}
		};
	}

	/**
	 * 将请求体和给定 String 解析为 XML，并断言二者“相似”——即它们包含相同的元素和属性，
	 * 与顺序无关。
	 * <p>使用此匹配器假定
	 * <a href="https://www.xmlunit.org/">XMLUnit</a> 库可用。
	 * @param expectedXmlContent the expected XML content
	 */
	public RequestMatcher xml(String expectedXmlContent) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertXmlEqual(expectedXmlContent, request.getBodyAsString());
			}
		};
	}

	/**
	 * 将请求内容解析为 {@link Node}，并应用给定的 {@link Matcher}。
	 */
	public RequestMatcher node(Matcher<? super Node> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertNode(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * 将请求内容解析为 {@link DOMSource}，并应用给定的 {@link Matcher}。
	 * @see <a href="https://code.google.com/p/xml-matchers/">https://code.google.com/p/xml-matchers/</a>
	 */
	public RequestMatcher source(Matcher<? super Source> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertSource(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * 将预期字符串和实际字符串解析为 JSON，并断言二者“相似”——即它们包含相同的属性值对，
	 * 与格式无关，并进行宽松检查（可扩展，且数组顺序不严格）。
	 * <p>使用此匹配器需要 <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> 库。
	 * @param expectedJsonContent the expected JSON content
	 * @since 5.0.5
	 */
	public RequestMatcher json(String expectedJsonContent) {
		return json(expectedJsonContent, false);
	}

	/**
	 * 将请求体和给定字符串解析为 JSON，并断言二者“相似”——即它们包含相同的属性值对，
	 * 与格式无关。
	 * <p>可根据 {@code strict} 参数值以两种模式进行比较：
	 * <ul>
	 * <li>{@code true}：严格检查。不可扩展，且数组顺序严格。</li>
	 * <li>{@code false}：宽松检查。可扩展，且数组顺序不严格。</li>
	 * </ul>
	 * <p>使用此匹配器需要 <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> 库。
	 * @param expectedJsonContent the expected JSON content
	 * @param strict enables strict checking
	 * @since 5.0.5
	 */
	public RequestMatcher json(String expectedJsonContent, boolean strict) {
		return request -> {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				this.jsonHelper.assertJsonEqual(expectedJsonContent, mockRequest.getBodyAsString(), strict);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual JSON request content", ex);
			}
		};
	}


	/**
	 * XML {@link RequestMatcher} 的抽象基类。
	 */
	private abstract static class AbstractXmlRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual XML request content", ex);
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}


	private static class MultipartHelper {

		public static MultiValueMap<String, ?> parse(ClientHttpRequest request) {
			MultipartHttpServletRequest servletRequest = adaptToMultipartRequest(request);
			MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
			for (Map.Entry<String, List<MultipartFile>> entry : servletRequest.getMultiFileMap().entrySet()) {
				for (MultipartFile value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			for (Map.Entry<String, String[]> entry : servletRequest.getParameterMap().entrySet()) {
				for (String value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			return result;
		}

		private static MultipartHttpServletRequest adaptToMultipartRequest(ClientHttpRequest request) {
			MockClientHttpRequest source = (MockClientHttpRequest) request;
			MockHttpServletRequest target = new MockHttpServletRequest();
			target.setContent(source.getBodyAsBytes());
			source.getHeaders().forEach((name, values) -> values.forEach(v -> target.addHeader(name, v)));
			return new CommonsMultipartResolver().resolveMultipart(target);
		}
	}

}
