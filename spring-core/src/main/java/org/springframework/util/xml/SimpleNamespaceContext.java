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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 {@code javax.xml.namespace.NamespaceContext} 实现。遵循标准的 {@code NamespaceContext} 约
 * 定，并可通过 {@code java.util.Map} 或 {@code java.util.Properties} 对象加载。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SimpleNamespaceContext implements NamespaceContext {

	private final Map<String, String> prefixToNamespaceUri = new HashMap<>();

	private final Map<String, Set<String>> namespaceUriToPrefixes = new HashMap<>();

	private String defaultNamespaceUri = "";


	@Override
	public String getNamespaceURI(String prefix) {
		Assert.notNull(prefix, "No prefix given");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		}
		else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			return this.defaultNamespaceUri;
		}
		else if (this.prefixToNamespaceUri.containsKey(prefix)) {
			return this.prefixToNamespaceUri.get(prefix);
		}
		return "";
	}

	@Override
	@Nullable
	public String getPrefix(String namespaceUri) {
		Set<String> prefixes = getPrefixesSet(namespaceUri);
		return (!prefixes.isEmpty() ? prefixes.iterator().next() : null);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceUri) {
		return getPrefixesSet(namespaceUri).iterator();
	}

	private Set<String> getPrefixesSet(String namespaceUri) {
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (this.defaultNamespaceUri.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX);
		}
		else if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.XML_NS_PREFIX);
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE);
		}
		else {
			Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
			return (prefixes != null ?  Collections.unmodifiableSet(prefixes) : Collections.emptySet());
		}
	}


	/**
	 * 设置此命名空间上下文的绑定。提供的映射必须由字符串键值对组成。
	 */
	public void setBindings(Map<String, String> bindings) {
		bindings.forEach(this::bindNamespaceUri);
	}

	/**
	 * 将给定命名空间绑定为默认命名空间。
	 * @param namespaceUri the namespace uri
	 */
	public void bindDefaultNamespaceUri(String namespaceUri) {
		bindNamespaceUri(XMLConstants.DEFAULT_NS_PREFIX, namespaceUri);
	}

	/**
	 * 将给定前缀绑定到给定命名空间。
	 * @param prefix the namespace prefix
	 * @param namespaceUri the namespace uri
	 */
	public void bindNamespaceUri(String prefix, String namespaceUri) {
		Assert.notNull(prefix, "No prefix given");
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			this.defaultNamespaceUri = namespaceUri;
		}
		else {
			this.prefixToNamespaceUri.put(prefix, namespaceUri);
			Set<String> prefixes =
					this.namespaceUriToPrefixes.computeIfAbsent(namespaceUri, k -> new LinkedHashSet<>());
			prefixes.add(prefix);
		}
	}

	/**
	 * 从此上下文中移除给定前缀。
	 * @param prefix the prefix to be removed
	 */
	public void removeBinding(@Nullable String prefix) {
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			this.defaultNamespaceUri = "";
		}
		else if (prefix != null) {
			String namespaceUri = this.prefixToNamespaceUri.remove(prefix);
			if (namespaceUri != null) {
				Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
				if (prefixes != null) {
					prefixes.remove(prefix);
					if (prefixes.isEmpty()) {
						this.namespaceUriToPrefixes.remove(namespaceUri);
					}
				}
			}
		}
	}

	/**
	 * 移除所有已声明的前缀。
	 */
	public void clear() {
		this.prefixToNamespaceUri.clear();
		this.namespaceUriToPrefixes.clear();
	}

	/**
	 * 返回所有已声明的前缀。
	 */
	public Iterator<String> getBoundPrefixes() {
		return this.prefixToNamespaceUri.keySet().iterator();
	}

}
