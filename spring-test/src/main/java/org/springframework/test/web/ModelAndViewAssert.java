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

package org.springframework.test.web;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * 一组断言，旨在简化处理 Spring Web MVC {@link org.springframework.web.servlet.ModelAndView ModelAndView} 对象的测试场景。
 *
 * <p>适用于 JUnit 4 和 TestNG。所有 {@code assert*()} 方法都会抛出 {@link AssertionError AssertionErrors}。
 *
 * @author Sam Brannen
 * @author Alef Arendsen
 * @author Bram Smeets
 * @since 2.5
 * @see org.springframework.web.servlet.ModelAndView
 */
public abstract class ModelAndViewAssert {

	/**
	 * 检查给定 {@code modelName} 下的模型值是否存在，并根据 {@code expectedType} 检查其类型。如果模型条目存在且类型匹配，则返回该模型值。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param modelName 要添加到模型中的对象的名称（永不为 {@code null}）
	 * @param expectedType 模型值的期望类型
	 * @return 模型值
	 */
	@SuppressWarnings("unchecked")
	public static <T> T assertAndReturnModelAttributeOfType(ModelAndView mav, String modelName, Class<T> expectedType) {
		Map<String, Object> model = mav.getModel();
		Object obj = model.get(modelName);
		if (obj == null) {
			fail("Model attribute with name '" + modelName + "' is null");
		}
		assertTrue("Model attribute is not of expected type '" + expectedType.getName() + "' but rather of type '" +
				obj.getClass().getName() + "'", expectedType.isAssignableFrom(obj.getClass()));
		return (T) obj;
	}

	/**
	 * 逐个比较列表中的每个条目，而不先对列表排序。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param modelName 要添加到模型中的对象的名称（永不为 {@code null}）
	 * @param expectedList 期望的列表
	 */
	@SuppressWarnings("rawtypes")
	public static void assertCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList) {
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
		assertTrue("Size of model list is '" + modelList.size() + "' while size of expected list is '" +
				expectedList.size() + "'", expectedList.size() == modelList.size());
		assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
				expectedList.equals(modelList));
	}

	/**
	 * 断言模型属性是否可用。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param modelName 要添加到模型中的对象的名称（永不为 {@code null}）
	 */
	public static void assertModelAttributeAvailable(ModelAndView mav, String modelName) {
		Map<String, Object> model = mav.getModel();
		assertTrue("Model attribute with name '" + modelName + "' is not available", model.containsKey(modelName));
	}

	/**
	 * 将给定的 {@code expectedValue} 与绑定在给定 {@code modelName} 下的模型值进行比较。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param modelName 要添加到模型中的对象的名称（永不为 {@code null}）
	 * @param expectedValue 模型值
	 */
	public static void assertModelAttributeValue(ModelAndView mav, String modelName, Object expectedValue) {
		Object modelValue = assertAndReturnModelAttributeOfType(mav, modelName, Object.class);
		assertTrue("Model value with name '" + modelName + "' is not the same as the expected value which was '" +
				expectedValue + "'", modelValue.equals(expectedValue));
	}

	/**
	 * 检查 {@code expectedModel}，看模型中的所有元素是否都出现且相等。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param expectedModel 期望的模型
	 */
	public static void assertModelAttributeValues(ModelAndView mav, Map<String, Object> expectedModel) {
		Map<String, Object> model = mav.getModel();

		if (!model.keySet().equals(expectedModel.keySet())) {
			StringBuilder sb = new StringBuilder("Keyset of expected model does not match.\n");
			appendNonMatchingSetsErrorMessage(expectedModel.keySet(), model.keySet(), sb);
			fail(sb.toString());
		}

		StringBuilder sb = new StringBuilder();
		model.forEach((modelName, mavValue) -> {
			Object assertionValue = expectedModel.get(modelName);
			if (!assertionValue.equals(mavValue)) {
				sb.append("Value under name '").append(modelName).append("' differs, should have been '").append(
					assertionValue).append("' but was '").append(mavValue).append("'\n");
			}
		});

		if (sb.length() != 0) {
			sb.insert(0, "Values of expected model do not match.\n");
			fail(sb.toString());
		}
	}

	/**
	 * 在分别对两个列表排序后（可选地使用比较器），逐个比较列表中的每个条目。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param modelName 要添加到模型中的对象的名称（永不为 {@code null}）
	 * @param expectedList 期望的列表
	 * @param comparator 要使用的比较器（可以为 {@code null}）。
	 * 如果不指定比较器，两个列表将不使用任何比较器进行排序。
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void assertSortAndCompareListModelAttribute(
			ModelAndView mav, String modelName, List expectedList, Comparator comparator) {

		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
		assertTrue("Size of model list is '" + modelList.size() + "' while size of expected list is '" +
				expectedList.size() + "'", expectedList.size() == modelList.size());

		modelList.sort(comparator);
		expectedList.sort(comparator);

		assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
				expectedList.equals(modelList));
	}

	/**
	 * 检查 ModelAndView 中的视图名是否与给定的 {@code expectedName} 匹配。
	 * @param mav 要据此测试的 ModelAndView（永不为 {@code null}）
	 * @param expectedName 模型值的名称
	 */
	public static void assertViewName(ModelAndView mav, String expectedName) {
		assertTrue("View name is not equal to '" + expectedName + "' but was '" + mav.getViewName() + "'",
				ObjectUtils.nullSafeEquals(expectedName, mav.getViewName()));
	}


	private static void appendNonMatchingSetsErrorMessage(
			Set<String> assertionSet, Set<String> incorrectSet, StringBuilder sb) {

		Set<String> tempSet = new HashSet<>(incorrectSet);
		tempSet.removeAll(assertionSet);

		if (!tempSet.isEmpty()) {
			sb.append("Set has too many elements:\n");
			for (Object element : tempSet) {
				sb.append('-');
				sb.append(element);
				sb.append('\n');
			}
		}

		tempSet = new HashSet<>(assertionSet);
		tempSet.removeAll(incorrectSet);

		if (!tempSet.isEmpty()) {
			sb.append("Set is missing elements:\n");
			for (Object element : tempSet) {
				sb.append('-');
				sb.append(element);
				sb.append('\n');
			}
		}
	}

}
