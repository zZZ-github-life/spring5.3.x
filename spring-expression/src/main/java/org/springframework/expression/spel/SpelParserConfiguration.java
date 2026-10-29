/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.expression.spel;

import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;

/**
 * SpEL 表达式解析器的配置对象。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @since 3.0
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 */
public class SpelParserConfiguration {

	/** 用于配置 SpEL 表达式解析器默认编译模式的系统属性：{@value}。 */
	public static final String SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spring.expression.compiler.mode";


	private static final SpelCompilerMode defaultCompilerMode;

	static {
		String compilerMode = SpringProperties.getProperty(SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
		defaultCompilerMode = (compilerMode != null ?
				SpelCompilerMode.valueOf(compilerMode.toUpperCase()) : SpelCompilerMode.OFF);
	}


	private final SpelCompilerMode compilerMode;

	@Nullable
	private final ClassLoader compilerClassLoader;

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;

	private final int maximumAutoGrowSize;


	/**
	 * 使用默认设置创建新的 {@code SpelParserConfiguration} 实例。
	 */
	public SpelParserConfiguration() {
		this(null, null, false, false, Integer.MAX_VALUE);
	}

	/**
	 * 创建新的 {@code SpelParserConfiguration} 实例。
	 * @param compilerMode the compiler mode for the parser
	 * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
	}

	/**
	 * 创建新的 {@code SpelParserConfiguration} 实例。
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @see #SpelParserConfiguration(boolean, boolean, int)
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * 创建新的 {@code SpelParserConfiguration} 实例。
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size that the collection can auto grow
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
	}

	/**
	 * 创建新的 {@code SpelParserConfiguration} 实例。
	 * @param compilerMode the compiler mode that parsers using this configuration object should use
	 * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size that the collection can auto grow
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

		this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
	}


	/**
	 * 返回使用此配置对象的解析器的编译器模式。
	 */
	public SpelCompilerMode getCompilerMode() {
		return this.compilerMode;
	}

	/**
	 * 返回用作表达式编译基础的 ClassLoader。
	 */
	@Nullable
	public ClassLoader getCompilerClassLoader() {
		return this.compilerClassLoader;
	}

	/**
	 * 如果应自动增长 {@code null} 引用，则返回 {@code true}。
	 */
	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	/**
	 * 如果应自动增长集合，则返回 {@code true}。
	 */
	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

	/**
	 * 返回集合可自动增长的最大大小。
	 */
	public int getMaximumAutoGrowSize() {
		return this.maximumAutoGrowSize;
	}

}
