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

package org.springframework.test.web.servlet.result;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hamcrest.Matcher;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 请求断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#request} 访问。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class RequestResultMatchers {

	/**
	 * 受保护的构造函数。
	 * <p>请使用 {@link MockMvcResultMatchers#request()}。
	 */
	protected RequestResultMatchers() {
	}


	/**
	 * 断言异步处理已启动，通常是由于控制器方法返回
	 * {@link Callable} 或 {@link DeferredResult}。
	 * <p>测试将等待 {@code Callable} 完成，以便可以使用
	 * {@link #asyncResult(Matcher)} 或 {@link #asyncResult(Object)}
	 * 断言结果值。
	 * <p>由于 {@link MockHttpServletRequest} 不执行异步分派，
	 * {@code Callable} 和 {@code DeferredResult} 都不会完成整个处理过程。
	 * @see #asyncNotStarted()
	 */
	public ResultMatcher asyncStarted() {
		return result -> assertAsyncStarted(result.getRequest());
	}

	/**
	 * 断言异步处理未启动。
	 * @see #asyncStarted()
	 */
	public ResultMatcher asyncNotStarted() {
		return result -> assertFalse("Async started", result.getRequest().isAsyncStarted());
	}

	/**
	 * 使用给定匹配器断言异步处理的结果。
	 * <p>当控制器方法返回 {@link Callable} 或 {@link WebAsyncTask} 时，
	 * 可以使用此方法。
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher asyncResult(Matcher<? super T> matcher) {
		return result -> {
			HttpServletRequest request = result.getRequest();
			assertAsyncStarted(request);
			assertThat("Async result", (T) result.getAsyncResult(), matcher);
		};
	}

	/**
	 * 断言异步处理的结果。
	 * <p>当控制器方法返回 {@link Callable} 或 {@link WebAsyncTask} 时，
	 * 可以使用此方法。匹配的值是 {@code Callable} 返回的值或引发的异常。
	 */
	public ResultMatcher asyncResult(@Nullable Object expectedResult) {
		return result -> {
			HttpServletRequest request = result.getRequest();
			assertAsyncStarted(request);
			assertEquals("Async result", expectedResult, result.getAsyncResult());
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言请求属性值。
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
		return result -> {
			T value = (T) result.getRequest().getAttribute(name);
			assertThat("Request attribute '" + name + "'", value, matcher);
		};
	}

	/**
	 * 断言请求属性值。
	 */
	public ResultMatcher attribute(String name, @Nullable Object expectedValue) {
		return result ->
				assertEquals("Request attribute '" + name + "'", expectedValue, result.getRequest().getAttribute(name));
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言会话属性值。
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher sessionAttribute(String name, Matcher<? super T> matcher) {
		return result -> {
			HttpSession session = result.getRequest().getSession();
			Assert.state(session != null, "No HttpSession");
			T value = (T) session.getAttribute(name);
			assertThat("Session attribute '" + name + "'", value, matcher);
		};
	}

	/**
	 * 断言会话属性值。
	 */
	public ResultMatcher sessionAttribute(String name, @Nullable Object value) {
		return result -> {
			HttpSession session = result.getRequest().getSession();
			Assert.state(session != null, "No HttpSession");
			assertEquals("Session attribute '" + name + "'", value, session.getAttribute(name));
		};
	}

	/**
	 * 断言给定会话属性不存在。
	 * @since 5.2.1
	 */
	public ResultMatcher sessionAttributeDoesNotExist(String... names) {
		return result -> {
			HttpSession session = result.getRequest().getSession();
			Assert.state(session != null, "No HttpSession");
			for (String name : names) {
				assertNull("Session attribute '" + name + "' exists", session.getAttribute(name));
			}
		};
	}

	private static void assertAsyncStarted(HttpServletRequest request) {
		assertTrue("Async not started", request.isAsyncStarted());
	}

}
