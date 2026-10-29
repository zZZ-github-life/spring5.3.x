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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 抽象的正则表达式切点基类 Bean。JavaBean 属性包括：
 * <ul>
 * <li>pattern：用于匹配完全限定方法名的正则表达式。具体正则表达式语法取决于子类（例如 Perl5 正则表达式）
 * <li>patterns：替代属性，接受由模式组成的 String 数组。结果将是这些模式的并集。
 * </ul>
 *
 * <p>注意：正则表达式必须构成匹配。例如，{@code .*get.*} 将匹配 com.mycom.Foo.getBar()，而
 * {@code get.*} 则不会。
 *
 * <p>此基类可序列化。子类应将所有字段声明为 transient；反序列化时将再次调用
 * {@link #initPatternRepresentation} 方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1
 * @see JdkRegexpMethodPointcut
 */
@SuppressWarnings("serial")
public abstract class AbstractRegexpMethodPointcut extends StaticMethodMatcherPointcut
		implements Serializable {

	/**
	 * 要匹配的正则表达式。
	 */
	private String[] patterns = new String[0];

	/**
	 * 用于<strong>不</strong>匹配的正则表达式。
	 */
	private String[] excludedPatterns = new String[0];


	/**
	 * 只有一个模式时使用的便捷方法。请使用此方法或 {@link #setPatterns} 之一，不要同时使用两者。
	 * @see #setPatterns
	 */
	public void setPattern(String pattern) {
		setPatterns(pattern);
	}

	/**
	 * 设置用于定义要匹配的方法的正则表达式。匹配结果是所有这些表达式的并集；只要其中任一表达式匹配，该
	 * 切点就匹配。
	 * @see #setPattern
	 */
	public void setPatterns(String... patterns) {
		Assert.notEmpty(patterns, "'patterns' must not be empty");
		this.patterns = new String[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			this.patterns[i] = StringUtils.trimWhitespace(patterns[i]);
		}
		initPatternRepresentation(this.patterns);
	}

	/**
	 * 返回用于方法匹配的正则表达式。
	 */
	public String[] getPatterns() {
		return this.patterns;
	}

	/**
	 * 只有一个排除模式时使用的便捷方法。请使用此方法或 {@link #setExcludedPatterns} 之一，不要同时使用
	 * 两者。
	 * @see #setExcludedPatterns
	 */
	public void setExcludedPattern(String excludedPattern) {
		setExcludedPatterns(excludedPattern);
	}

	/**
	 * 设置用于定义要排除匹配的方法的正则表达式。匹配结果是所有这些表达式的并集；只要其中任一表达式匹配，
	 * 该切点就匹配。
	 * @see #setExcludedPattern
	 */
	public void setExcludedPatterns(String... excludedPatterns) {
		Assert.notEmpty(excludedPatterns, "'excludedPatterns' must not be empty");
		this.excludedPatterns = new String[excludedPatterns.length];
		for (int i = 0; i < excludedPatterns.length; i++) {
			this.excludedPatterns[i] = StringUtils.trimWhitespace(excludedPatterns[i]);
		}
		initExcludedPatternRepresentation(this.excludedPatterns);
	}

	/**
	 * 返回用于排除匹配的正则表达式。
	 */
	public String[] getExcludedPatterns() {
		return this.excludedPatterns;
	}


	/**
	 * 尝试将正则表达式与目标类的完全限定名以及方法声明类加方法名进行匹配。
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return (matchesPattern(ClassUtils.getQualifiedMethodName(method, targetClass)) ||
				(targetClass != method.getDeclaringClass() &&
						matchesPattern(ClassUtils.getQualifiedMethodName(method, method.getDeclaringClass()))));
	}

	/**
	 * 将指定的候选对象与已配置的模式进行匹配。
	 * @param signatureString "java.lang.Object.hashCode" style signature
	 * @return whether the candidate matches at least one of the specified patterns
	 */
	protected boolean matchesPattern(String signatureString) {
		for (int i = 0; i < this.patterns.length; i++) {
			boolean matched = matches(signatureString, i);
			if (matched) {
				for (int j = 0; j < this.excludedPatterns.length; j++) {
					boolean excluded = matchesExclusion(signatureString, j);
					if (excluded) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}


	/**
	 * 子类必须实现此方法以初始化正则表达式切点。可被多次调用。
	 * <p>此方法将从 {@link #setPatterns} 方法中调用，也会在反序列化时调用。
	 * @param patterns the patterns to initialize
	 * @throws IllegalArgumentException in case of an invalid pattern
	 */
	protected abstract void initPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * 子类必须实现此方法以初始化正则表达式切点。可被多次调用。
	 * <p>此方法将从 {@link #setExcludedPatterns} 方法中调用，也会在反序列化时调用。
	 * @param patterns the patterns to initialize
	 * @throws IllegalArgumentException in case of an invalid pattern
	 */
	protected abstract void initExcludedPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * 给定索引处的模式是否匹配给定的 String？
	 * @param pattern the {@code String} pattern to match
	 * @param patternIndex index of pattern (starting from 0)
	 * @return {@code true} if there is a match, {@code false} otherwise
	 */
	protected abstract boolean matches(String pattern, int patternIndex);

	/**
	 * 给定索引处的排除模式是否匹配给定的 String？
	 * @param pattern the {@code String} pattern to match
	 * @param patternIndex index of pattern (starting from 0)
	 * @return {@code true} if there is a match, {@code false} otherwise
	 */
	protected abstract boolean matchesExclusion(String pattern, int patternIndex);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractRegexpMethodPointcut)) {
			return false;
		}
		AbstractRegexpMethodPointcut otherPointcut = (AbstractRegexpMethodPointcut) other;
		return (Arrays.equals(this.patterns, otherPointcut.patterns) &&
				Arrays.equals(this.excludedPatterns, otherPointcut.excludedPatterns));
	}

	@Override
	public int hashCode() {
		int result = 27;
		for (String pattern : this.patterns) {
			result = 13 * result + pattern.hashCode();
		}
		for (String excludedPattern : this.excludedPatterns) {
			result = 13 * result + excludedPattern.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": patterns " + ObjectUtils.nullSafeToString(this.patterns) +
				", excluded patterns " + ObjectUtils.nullSafeToString(this.excludedPatterns);
	}

}
