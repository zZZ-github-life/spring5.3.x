/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * YAML 工厂的基类。
 *
 * <p>自 Spring Framework 5.0.6 起，需要 SnakeYAML 1.18 或更高版本。
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public abstract class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;

	private Set<String> supportedTypes = Collections.emptySet();


	/**
	 * 文档匹配器的映射，允许调用方有选择地仅使用 YAML 资源中的部分文档。在 YAML 中，文档由 {@code ---}
	 * 行分隔，每个文档在匹配之前都会转换为属性。例如
	 * <pre class="code">
	 * environment: dev
	 * url: https://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:https://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * 当使用以下内容映射时
	 * <pre class="code">
	 * setDocumentMatchers(properties -&gt;
	 *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
	 * </pre>
	 * 最终会得到
	 * <pre class="code">
	 * environment=prod
	 * url=https://foo.bar.com
	 * name=My Cool App
	 * </pre>
	 */
	public void setDocumentMatchers(DocumentMatcher... matchers) {
		this.documentMatchers = Arrays.asList(matchers);
	}

	/**
	 * 指示对于所有 {@link #setDocumentMatchers(DocumentMatcher...) document matchers} 都弃权的文档是否
	 * 仍然匹配的标志。默认是 {@code true}。
	 */
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * 用于解析资源的方法。每个资源都将转换为 Map，因此此属性用于决定在此工厂的最终输出中保留哪些映射条
	 * 目。默认是 {@link ResolutionMethod#OVERRIDE}。
	 */
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * 设置要加载的 YAML {@link Resource resources} 的位置。
	 * @see ResolutionMethod
	 */
	public void setResources(Resource... resources) {
		this.resources = resources;
	}

	/**
	 * 设置可从 YAML 文档加载的支持类型。
	 * <p>如果未配置支持的类型，则仅支持在 YAML 文档中遇到的 Java 标准类（如
	 * {@link org.yaml.snakeyaml.constructor.SafeConstructor} 中所定义）。如果遇到不支持的类型，则在处
	 * 理相应 YAML 节点时将抛出 {@link IllegalStateException}。
	 * @param supportedTypes 受支持的类型；若为空数组则清除受支持的类型
	 * @since 5.1.16
	 * @see #createYaml()
	 */
	public void setSupportedTypes(Class<?>... supportedTypes) {
		if (ObjectUtils.isEmpty(supportedTypes)) {
			this.supportedTypes = Collections.emptySet();
		}
		else {
			Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
			this.supportedTypes = Arrays.stream(supportedTypes).map(Class::getName)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		}
	}

	/**
	 * 为子类提供处理从所提供资源中解析的 Yaml 的机会。每个资源依次解析，其中的文档会根据
	 * {@link #setDocumentMatchers(DocumentMatcher...) matchers} 检查。如果文档匹配，则会连同其
	 * Properties 表示一起传入回调。取决于 {@link #setResolutionMethod(ResolutionMethod)}，并非所有文档
	 * 都会被解析。
	 * @param callback 找到匹配文档后要委托的回调
	 * @see #createYaml()
	 */
	protected void process(MatchCallback callback) {
		Yaml yaml = createYaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	/**
	 * 创建要使用的 {@link Yaml} 实例。
	 * <p>默认实现将 "allowDuplicateKeys" 标志设置为 {@code false}，从而启用 SnakeYAML 1.18+ 中内置的重
	 * 复键处理。
	 * <p>自 Spring Framework 5.1.16 起，如果已配置自定义的
	 * {@linkplain #setSupportedTypes supported types}，默认实现会创建一个 {@code Yaml} 实例，用于过滤
	 * YAML 文档中遇到的不支持类型。如果遇到不支持的类型，则在处理该节点时将抛出
	 * {@link IllegalStateException}。
	 * @see LoaderOptions#setAllowDuplicateKeys(boolean)
	 */
	protected Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		DumperOptions dumperOptions = new DumperOptions();
		return new Yaml(new FilteringConstructor(loaderOptions), new Representer(dumperOptions),
				dumperOptions, loaderOptions);
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading from YAML: " + resource);
			}
			try (Reader reader = new UnicodeReader(resource.getInputStream())) {
				for (Object object : yaml.loadAll(reader)) {
					if (object != null && process(asMap(object), callback)) {
						count++;
						if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") +
							" from YAML resource: " + resource);
				}
			}
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return (count > 0);
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
				this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (logger.isWarnEnabled()) {
			logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object object) {
		// YAML can have numbers as keys
		Map<String, Object> result = new LinkedHashMap<>();
		if (!(object instanceof Map)) {
			// A document can be a text literal
			result.put("document", object);
			return result;
		}

		Map<Object, Object> map = (Map<Object, Object>) object;
		map.forEach((key, value) -> {
			if (value instanceof Map) {
				value = asMap(value);
			}
			if (key instanceof CharSequence) {
				result.put(key.toString(), value);
			}
			else {
				// It has to be a map key in this case
				result.put("[" + key.toString() + "]", value);
			}
		});
		return result;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = CollectionFactory.createStringAdaptingProperties();
		properties.putAll(getFlattenedMap(map));

		if (this.documentMatchers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Merging document (no matchers set): " + map);
			}
			callback.process(properties, map);
			return true;
		}

		MatchStatus result = MatchStatus.ABSTAIN;
		for (DocumentMatcher matcher : this.documentMatchers) {
			MatchStatus match = matcher.matches(properties);
			result = MatchStatus.getMostSpecific(match, result);
			if (match == MatchStatus.FOUND) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched document with document matcher: " + properties);
				}
				callback.process(properties, map);
				return true;
			}
		}

		if (result == MatchStatus.ABSTAIN && this.matchDefault) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matched document with default matcher: " + map);
			}
			callback.process(properties, map);
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Unmatched document: " + map);
		}
		return false;
	}

	/**
	 * 返回给定映射的扁平化版本，递归处理任何嵌套的 Map 或 Collection 值。结果映射中的条目保持与源相同
	 * 的顺序。当使用来自 {@link MatchCallback} 的 Map 调用时，结果将包含与 {@link MatchCallback}
	 * Properties 相同的值。
	 * @param source 源映射
	 * @return 扁平化后的映射
	 * @since 4.1.3
	 */
	protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		buildFlattenedMap(result, source, null);
		return result;
	}

	private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
		source.forEach((key, value) -> {
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[")) {
					key = path + key;
				}
				else {
					key = path + '.' + key;
				}
			}
			if (value instanceof String) {
				result.put(key, value);
			}
			else if (value instanceof Map) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) value;
				buildFlattenedMap(result, map, key);
			}
			else if (value instanceof Collection) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>) value;
				if (collection.isEmpty()) {
					result.put(key, "");
				}
				else {
					int count = 0;
					for (Object object : collection) {
						buildFlattenedMap(result, Collections.singletonMap(
								"[" + (count++) + "]", object), key);
					}
				}
			}
			else {
				result.put(key, (value != null ? value : ""));
			}
		});
	}


	/**
	 * 用于处理 YAML 解析结果的回调接口。
	 */
	@FunctionalInterface
	public interface MatchCallback {

		/**
		 * 处理解析结果的给定表示。
		 * @param properties 要处理的属性（扁平化表示，若为集合或映射则使用带索引的键）
		 * @param map 结果映射（保留 YAML 文档中的原始值结构）
		 */
		void process(Properties properties, Map<String, Object> map);
	}


	/**
	 * 用于测试属性是否匹配的策略接口。
	 */
	@FunctionalInterface
	public interface DocumentMatcher {

		/**
		 * 测试给定属性是否匹配。
		 * @param properties 要测试的属性
		 * @return 匹配状态
		 */
		MatchStatus matches(Properties properties);
	}


	/**
	 * 从 {@link DocumentMatcher#matches(java.util.Properties)} 返回的状态。
	 */
	public enum MatchStatus {

		/**
		 * 找到匹配。
		 */
		FOUND,

		/**
		 * 未找到匹配。
		 */
		NOT_FOUND,

		/**
		 * 不应考虑该匹配器。
		 */
		ABSTAIN;

		/**
		 * 比较两个 {@link MatchStatus} 项，返回最具体的状态。
		 */
		public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
			return (a.ordinal() < b.ordinal() ? a : b);
		}
	}


	/**
	 * 用于解析资源的方法。
	 */
	public enum ResolutionMethod {

		/**
		 * 替换列表中靠前的值。
		 */
		OVERRIDE,

		/**
		 * 替换列表中靠前的值，忽略任何失败。
		 */
		OVERRIDE_AND_IGNORE,

		/**
		 * 取列表中存在的第一个资源并仅使用它。
		 */
		FIRST_FOUND
	}


	/**
	 * 支持过滤不支持类型的 {@link Constructor}。
	 * <p>如果在 YAML 文档中遇到不支持的类型，将从 {@link #getClassForName} 抛出
	 * {@link IllegalStateException}。
	 */
	private class FilteringConstructor extends Constructor {

		FilteringConstructor(LoaderOptions loaderOptions) {
			super(loaderOptions);
		}

		@Override
		protected Class<?> getClassForName(String name) throws ClassNotFoundException {
			Assert.state(YamlProcessor.this.supportedTypes.contains(name),
					() -> "Unsupported type encountered in YAML document: " + name);
			return super.getClassForName(name);
		}
	}

}
