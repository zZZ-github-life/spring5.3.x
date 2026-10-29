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

package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * 对所选择视图的断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#view} 访问。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ViewResultMatchers {

	/**
	 * 受保护的构造函数。
	 * 请使用 {@link MockMvcResultMatchers#view()}。
	 */
	protected ViewResultMatchers() {
	}


	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言所选视图名称。
	 */
	public ResultMatcher name(Matcher<? super String> matcher) {
		return result -> {
			ModelAndView mav = result.getModelAndView();
			if (mav == null) {
				fail("No ModelAndView found");
			}
			assertThat("View name", mav.getViewName(), matcher);
		};
	}

	/**
	 * 断言所选视图名称。
	 */
	public ResultMatcher name(String expectedViewName) {
		return result -> {
			ModelAndView mav = result.getModelAndView();
			if (mav == null) {
				fail("No ModelAndView found");
			}
			assertEquals("View name", expectedViewName, mav.getViewName());
		};
	}

}
