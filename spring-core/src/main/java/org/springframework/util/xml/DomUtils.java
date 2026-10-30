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

package org.springframework.util.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于操作 DOM API 的便捷方法，特别是用于操作 DOM Node 和 DOM Element。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Costin Leau
 * @author Arjen Poutsma
 * @author Luke Taylor
 * @since 1.2
 * @see org.w3c.dom.Node
 * @see org.w3c.dom.Element
 */
public abstract class DomUtils {

	/**
	 * 检索给定 DOM 元素中与任一给定元素名匹配的所有子元素。只查看给定元素的直接子级，不会进一步深入
	 * （与 DOM API 的 {@code getElementsByTagName} 方法相反）。
	 * @param ele 要分析的 DOM 元素
	 * @param childEleNames 要查找的子元素名称
	 * @return 子 {@code org.w3c.dom.Element} 实例的 List
	 * @see org.w3c.dom.Element
	 * @see org.w3c.dom.Element#getElementsByTagName
	 */
	public static List<Element> getChildElementsByTagName(Element ele, String... childEleNames) {
		Assert.notNull(ele, "Element must not be null");
		Assert.notNull(childEleNames, "Element names collection must not be null");
		List<String> childEleNameList = Arrays.asList(childEleNames);
		NodeList nl = ele.getChildNodes();
		List<Element> childEles = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && nodeNameMatch(node, childEleNameList)) {
				childEles.add((Element) node);
			}
		}
		return childEles;
	}

	/**
	 * 检索给定 DOM 元素中与给定元素名匹配的所有子元素。只查看给定元素的直接子级，不会进一步深入（与
	 * DOM API 的 {@code getElementsByTagName} 方法相反）。
	 * @param ele 要分析的 DOM 元素
	 * @param childEleName 要查找的子元素名称
	 * @return 子 {@code org.w3c.dom.Element} 实例的 List
	 * @see org.w3c.dom.Element
	 * @see org.w3c.dom.Element#getElementsByTagName
	 */
	public static List<Element> getChildElementsByTagName(Element ele, String childEleName) {
		return getChildElementsByTagName(ele, new String[] {childEleName});
	}

	/**
	 * 返回按名称标识的第一个子元素的工具方法。
	 * @param ele 要分析的 DOM 元素
	 * @param childEleName 要查找的子元素名称
	 * @return {@code org.w3c.dom.Element} 实例，如果未找到则为 {@code null}
	 */
	@Nullable
	public static Element getChildElementByTagName(Element ele, String childEleName) {
		Assert.notNull(ele, "Element must not be null");
		Assert.notNull(childEleName, "Element name must not be null");
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && nodeNameMatch(node, childEleName)) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * 返回按名称标识的第一个子元素值的工具方法。
	 * @param ele 要分析的 DOM 元素
	 * @param childEleName 要查找的子元素名称
	 * @return 提取出的文本值，如果未找到子元素则为 {@code null}
	 */
	@Nullable
	public static String getChildElementValueByTagName(Element ele, String childEleName) {
		Element child = getChildElementByTagName(ele, childEleName);
		return (child != null ? getTextValue(child) : null);
	}

	/**
	 * 检索给定 DOM 元素的所有子元素。
	 * @param ele 要分析的 DOM 元素
	 * @return 子 {@code org.w3c.dom.Element} 实例的 List
	 */
	public static List<Element> getChildElements(Element ele) {
		Assert.notNull(ele, "Element must not be null");
		NodeList nl = ele.getChildNodes();
		List<Element> childEles = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				childEles.add((Element) node);
			}
		}
		return childEles;
	}

	/**
	 * 从给定 DOM 元素中提取文本值，忽略 XML 注释。
	 * <p>将所有 CharacterData 节点和 EntityReference 节点追加到单个 String 值中，不包括 Comment 节点。
	 * 只暴露用户实际指定的文本，不包含任何类型的默认值。
	 * @see CharacterData
	 * @see EntityReference
	 * @see Comment
	 */
	public static String getTextValue(Element valueEle) {
		Assert.notNull(valueEle, "Element must not be null");
		StringBuilder sb = new StringBuilder();
		NodeList nl = valueEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
				sb.append(item.getNodeValue());
			}
		}
		return sb.toString();
	}

	/**
	 * 命名空间感知的相等比较。如果 {@link Node#getLocalName} 或 {@link Node#getNodeName} 等于
	 * {@code desiredName}，则返回 {@code true}，否则返回 {@code false}。
	 */
	public static boolean nodeNameEquals(Node node, String desiredName) {
		Assert.notNull(node, "Node must not be null");
		Assert.notNull(desiredName, "Desired name must not be null");
		return nodeNameMatch(node, desiredName);
	}

	/**
	 * 返回一个将回调调用转换为 DOM {@code Node} 的 SAX {@code ContentHandler}。
	 * @param node 要向其发布事件的节点
	 * @return 内容处理器
	 */
	public static ContentHandler createContentHandler(Node node) {
		return new DomContentHandler(node);
	}

	/**
	 * 将给定节点的名称和本地名称与给定的目标名称进行匹配。
	 */
	private static boolean nodeNameMatch(Node node, String desiredName) {
		return (desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName()));
	}

	/**
	 * 将给定节点的名称和本地名称与给定的多个目标名称进行匹配。
	 */
	private static boolean nodeNameMatch(Node node, Collection<?> desiredNames) {
		return (desiredNames.contains(node.getNodeName()) || desiredNames.contains(node.getLocalName()));
	}

}
