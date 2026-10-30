/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.web.servlet.FlashMap;

/**
 * {@link Model} 接口的专门化，控制器可用它来选择
 * 重定向场景所需的属性。由于添加重定向属性的意图非常明确
 * ——即用于重定向 URL，属性值可以格式化为 String 并按该形式存储，
 * 以便能够附加到查询字符串，或在
 * {@code org.springframework.web.servlet.view.RedirectView} 中作为 URI 变量展开。
 *
 * <p>此接口还提供了添加 flash 属性的方式。有关
 * flash 属性的总体概述，请参见 {@link FlashMap}。你可以使用
 * {@link RedirectAttributes} 存储 flash 属性，它们会自动
 * 传播到当前请求的 "output" FlashMap 中。
 *
 * <p>在 {@code @Controller} 中的示例用法：
 * <pre class="code">
 * &#064;RequestMapping(value = "/accounts", method = RequestMethod.POST)
 * public String handle(Account account, BindingResult result, RedirectAttributes redirectAttrs) {
 *   if (result.hasErrors()) {
 *     return "accounts/new";
 *   }
 *   // Save account ...
 *   redirectAttrs.addAttribute("id", account.getId()).addFlashAttribute("message", "Account created!");
 *   return "redirect:/accounts/{id}";
 * }
 * </pre>
 *
 * <p>方法被调用时 RedirectAttributes 模型为空，且除非该方法
 * 返回重定向视图名称或 RedirectView，否则永远不会
 * 被使用。
 *
 * <p>重定向之后，flash 属性会自动添加到为目标 URL
 * 提供服务的控制器的模型中。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface RedirectAttributes extends Model {

	@Override
	RedirectAttributes addAttribute(String attributeName, @Nullable Object attributeValue);

	@Override
	RedirectAttributes addAttribute(Object attributeValue);

	@Override
	RedirectAttributes addAllAttributes(Collection<?> attributeValues);

	@Override
	RedirectAttributes mergeAttributes(Map<String, ?> attributes);

	/**
	 * 添加给定的 flash 属性。
	 * @param attributeName 属性名称；永不为 {@code null}
	 * @param attributeValue 属性值；可以为 {@code null}
	 */
	RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue);

	/**
	 * 使用
	 * {@link org.springframework.core.Conventions#getVariableName generated name} 添加给定的 flash 存储。
	 * @param attributeValue flash 属性的值；永不为 {@code null}
	 */
	RedirectAttributes addFlashAttribute(Object attributeValue);

	/**
	 * 返回可用于 flash 存储的候选属性，或返回空 Map。
	 */
	Map<String, ?> getFlashAttributes();
}
