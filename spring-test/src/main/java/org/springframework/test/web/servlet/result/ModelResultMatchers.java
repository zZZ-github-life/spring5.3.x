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

import org.hamcrest.Matcher;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * 模型断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#model} 访问。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class ModelResultMatchers {

	/**
	 * 受保护的构造函数。
	 * 请使用 {@link MockMvcResultMatchers#model()}。
	 */
	protected ModelResultMatchers() {
	}


	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言模型属性值。
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			assertThat("Model attribute '" + name + "'", (T) mav.getModel().get(name), matcher);
		};
	}

	/**
	 * 断言模型属性值。
	 */
	public ResultMatcher attribute(String name, @Nullable Object value) {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			assertEquals("Model attribute '" + name + "'", value, mav.getModel().get(name));
		};
	}

	/**
	 * 断言给定模型属性存在。
	 */
	public ResultMatcher attributeExists(String... names) {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			for (String name : names) {
				assertNotNull("Model attribute '" + name + "' does not exist", mav.getModel().get(name));
			}
		};
	}

	/**
	 * 断言给定模型属性不存在。
	 */
	public ResultMatcher attributeDoesNotExist(String... names) {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			for (String name : names) {
				assertNull("Model attribute '" + name + "' exists", mav.getModel().get(name));
			}
		};
	}

	/**
	 * 断言给定模型属性有错误。
	 */
	public ResultMatcher attributeErrorCount(String name, int expectedCount) {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			Errors errors = getBindingResult(mav, name);
			assertEquals("Binding/validation error count for attribute '" + name + "',",
					expectedCount, errors.getErrorCount());
		};
	}

	/**
	 * 断言给定模型属性有错误。
	 */
	public ResultMatcher attributeHasErrors(String... names) {
		return mvcResult -> {
			ModelAndView mav = getModelAndView(mvcResult);
			for (String name : names) {
				BindingResult result = getBindingResult(mav, name);
				assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
			}
		};
	}

	/**
	 * 断言给定模型属性没有错误。
	 */
	public ResultMatcher attributeHasNoErrors(String... names) {
		return mvcResult -> {
			ModelAndView mav = getModelAndView(mvcResult);
			for (String name : names) {
				BindingResult result = getBindingResult(mav, name);
				assertFalse("Unexpected errors for attribute '" + name + "': " + result.getAllErrors(),
						result.hasErrors());
			}
		};
	}

	/**
	 * 断言给定模型属性字段有错误。
	 */
	public ResultMatcher attributeHasFieldErrors(String name, String... fieldNames) {
		return mvcResult -> {
			ModelAndView mav = getModelAndView(mvcResult);
			BindingResult result = getBindingResult(mav, name);
			assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
			for (String fieldName : fieldNames) {
				boolean hasFieldErrors = result.hasFieldErrors(fieldName);
				assertTrue("No errors for field '" + fieldName + "' of attribute '" + name + "'", hasFieldErrors);
			}
		};
	}

	/**
	 * 使用精确字符串匹配断言模型属性的字段错误码。
	 * @since 4.1
	 */
	public ResultMatcher attributeHasFieldErrorCode(String name, String fieldName, String error) {
		return mvcResult -> {
			ModelAndView mav = getModelAndView(mvcResult);
			BindingResult result = getBindingResult(mav, name);
			assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
			FieldError fieldError = result.getFieldError(fieldName);
			assertNotNull("No errors for field '" + fieldName + "' of attribute '" + name + "'", fieldError);
			String code = fieldError.getCode();
			assertEquals("Field error code", error, code);
		};
	}

	/**
	 * 使用 {@link org.hamcrest.Matcher} 断言模型属性的字段错误码。
	 * @since 4.1
	 */
	public ResultMatcher attributeHasFieldErrorCode(String name, String fieldName,
			Matcher<? super String> matcher) {

		return mvcResult -> {
			ModelAndView mav = getModelAndView(mvcResult);
			BindingResult result = getBindingResult(mav, name);
			assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
			FieldError fieldError = result.getFieldError(fieldName);
			assertNotNull("No errors for field '" + fieldName + "' of attribute '" + name + "'", fieldError);
			String code = fieldError.getCode();
			assertThat("Field name '" + fieldName + "' of attribute '" + name + "'", code, matcher);
		};
	}

	/**
	 * 断言模型中错误的总数。
	 */
	public ResultMatcher errorCount(int expectedCount) {
		return result -> {
			int actualCount = getErrorCount(getModelAndView(result).getModelMap());
			assertEquals("Binding/validation error count", expectedCount, actualCount);
		};
	}

	/**
	 * 断言模型有错误。
	 */
	public ResultMatcher hasErrors() {
		return result -> {
			int count = getErrorCount(getModelAndView(result).getModelMap());
			assertTrue("Expected binding/validation errors", count != 0);
		};
	}

	/**
	 * 断言模型没有错误。
	 */
	public ResultMatcher hasNoErrors() {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			for (Object value : mav.getModel().values()) {
				if (value instanceof Errors) {
					assertFalse("Unexpected binding/validation errors: " + value, ((Errors) value).hasErrors());
				}
			}
		};
	}

	/**
	 * 断言模型属性的数量。
	 */
	public ResultMatcher size(int size) {
		return result -> {
			ModelAndView mav = getModelAndView(result);
			int actual = 0;
			for (String key : mav.getModel().keySet()) {
				if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
					actual++;
				}
			}
			assertEquals("Model size", size, actual);
		};
	}

	private ModelAndView getModelAndView(MvcResult mvcResult) {
		ModelAndView mav = mvcResult.getModelAndView();
		assertNotNull("No ModelAndView found", mav);
		return mav;
	}

	private BindingResult getBindingResult(ModelAndView mav, String name) {
		BindingResult result = (BindingResult) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
		assertNotNull("No BindingResult for attribute: " + name, result);
		return result;
	}

	private int getErrorCount(ModelMap model) {
		int count = 0;
		for (Object value : model.values()) {
			if (value instanceof Errors) {
				count += ((Errors) value).getErrorCount();
			}
		}
		return count;
	}

}
