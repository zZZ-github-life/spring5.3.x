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

package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 在 Bean 定义读取过程中传递的上下文，封装了所有相关的配置以及状态。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ReaderContext {

	private final Resource resource;

	private final ProblemReporter problemReporter;

	private final ReaderEventListener eventListener;

	private final SourceExtractor sourceExtractor;


	/**
	 * 构造一个新的 {@code ReaderContext}。
	 * @param resource XML Bean 定义资源
	 * @param problemReporter 正在使用的问题报告器
	 * @param eventListener 正在使用的事件监听器
	 * @param sourceExtractor 正在使用的源提取器
	 */
	public ReaderContext(Resource resource, ProblemReporter problemReporter,
			ReaderEventListener eventListener, SourceExtractor sourceExtractor) {

		this.resource = resource;
		this.problemReporter = problemReporter;
		this.eventListener = eventListener;
		this.sourceExtractor = sourceExtractor;
	}

	public final Resource getResource() {
		return this.resource;
	}


	// Errors and warnings

	/**
	 * 抛出致命错误。
	 */
	public void fatal(String message, @Nullable Object source) {
		fatal(message, source, null, null);
	}

	/**
	 * 抛出致命错误。
	 */
	public void fatal(String message, @Nullable Object source, @Nullable Throwable cause) {
		fatal(message, source, null, cause);
	}

	/**
	 * 抛出致命错误。
	 */
	public void fatal(String message, @Nullable Object source, @Nullable ParseState parseState) {
		fatal(message, source, parseState, null);
	}

	/**
	 * 抛出致命错误。
	 */
	public void fatal(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.fatal(new Problem(message, location, parseState, cause));
	}

	/**
	 * 抛出普通错误。
	 */
	public void error(String message, @Nullable Object source) {
		error(message, source, null, null);
	}

	/**
	 * 抛出普通错误。
	 */
	public void error(String message, @Nullable Object source, @Nullable Throwable cause) {
		error(message, source, null, cause);
	}

	/**
	 * 抛出普通错误。
	 */
	public void error(String message, @Nullable Object source, @Nullable ParseState parseState) {
		error(message, source, parseState, null);
	}

	/**
	 * 抛出普通错误。
	 */
	public void error(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.error(new Problem(message, location, parseState, cause));
	}

	/**
	 * 抛出非关键警告。
	 */
	public void warning(String message, @Nullable Object source) {
		warning(message, source, null, null);
	}

	/**
	 * 抛出非关键警告。
	 */
	public void warning(String message, @Nullable Object source, @Nullable Throwable cause) {
		warning(message, source, null, cause);
	}

	/**
	 * 抛出非关键警告。
	 */
	public void warning(String message, @Nullable Object source, @Nullable ParseState parseState) {
		warning(message, source, parseState, null);
	}

	/**
	 * 抛出非关键警告。
	 */
	public void warning(String message, @Nullable Object source, @Nullable ParseState parseState, @Nullable Throwable cause) {
		Location location = new Location(getResource(), source);
		this.problemReporter.warning(new Problem(message, location, parseState, cause));
	}


	// Explicit parse events

	/**
	 * 触发 defaults-registered 事件。
	 */
	public void fireDefaultsRegistered(DefaultsDefinition defaultsDefinition) {
		this.eventListener.defaultsRegistered(defaultsDefinition);
	}

	/**
	 * 触发 component-registered 事件。
	 */
	public void fireComponentRegistered(ComponentDefinition componentDefinition) {
		this.eventListener.componentRegistered(componentDefinition);
	}

	/**
	 * 触发 alias-registered 事件。
	 */
	public void fireAliasRegistered(String beanName, String alias, @Nullable Object source) {
		this.eventListener.aliasRegistered(new AliasDefinition(beanName, alias, source));
	}

	/**
	 * 触发 import-processed 事件。
	 */
	public void fireImportProcessed(String importedResource, @Nullable Object source) {
		this.eventListener.importProcessed(new ImportDefinition(importedResource, source));
	}

	/**
	 * 触发 import-processed 事件。
	 */
	public void fireImportProcessed(String importedResource, Resource[] actualResources, @Nullable Object source) {
		this.eventListener.importProcessed(new ImportDefinition(importedResource, actualResources, source));
	}


	// Source extraction

	/**
	 * 返回正在使用的源提取器。
	 */
	public SourceExtractor getSourceExtractor() {
		return this.sourceExtractor;
	}

	/**
	 * 针对给定的源对象调用源提取器。
	 * @param sourceCandidate 原始源对象
	 * @return 要存储的源对象；若没有则为 {@code null}。
	 * @see #getSourceExtractor()
	 * @see SourceExtractor#extractSource
	 */
	@Nullable
	public Object extractSource(Object sourceCandidate) {
		return this.sourceExtractor.extractSource(sourceCandidate, this.resource);
	}

}
