/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 确定给定异常是否应导致回滚的规则。
 *
 * <p>可以应用多个此类规则来确定事务在抛出异常后是应该提交还是回滚。
 *
 * <p>每条规则基于一个异常模式, 该模式可以是异常类型(必须是{@code Throwable}的子类)的完全限定类名
 * 或完全限定类名的子字符串, 目前不支持通配符。例如, {@code "javax.servlet.ServletException"}
 * 或{@code "ServletException"}的值将匹配{@code javax.servlet.ServletException}及其子类。
 *
 * <p>异常模式可以分别在{@link #RollbackRuleAttribute(Class)}和{@link #RollbackRuleAttribute(String)}
 * 中指定为{@link Class}引用或{@link String}。当异常类型被指定为类引用时, 其完全限定名将用作模式。
 * 有关回滚规则语义、模式和可能意外匹配的警告, 请参阅
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}的javadoc。
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 09.04.2003
 * @see NoRollbackRuleAttribute
 */
@SuppressWarnings("serial")
public class RollbackRuleAttribute implements Serializable{

	/**
	 * {@link RuntimeException RuntimeException}的{@linkplain RollbackRuleAttribute 回滚规则}。
	 */
	public static final RollbackRuleAttribute ROLLBACK_ON_RUNTIME_EXCEPTIONS =
			new RollbackRuleAttribute(RuntimeException.class);


	/**
	 * 可以持有异常并解析类名, 但始终需要完全限定名。
	 * 这种方式进行多次字符串比较, 但我们多久才需要在异常后决定是否回滚事务呢?
	 */
	private final String exceptionPattern;


	/**
	 * 为给定的{@code exceptionType}创建{@code RollbackRuleAttribute}类的新实例。
	 * <p>这是构造匹配给定异常类型、其子类和嵌套类的回滚规则的首选方式。
	 * <p>有关回滚规则语义、模式和可能意外匹配的警告, 请参阅
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}的javadoc。
	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
	 * of {@code Throwable}
	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
	 * not a {@code Throwable} type or is {@code null}
	 */
	public RollbackRuleAttribute(Class<?> exceptionType) {
		Assert.notNull(exceptionType, "'exceptionType' cannot be null");
		if (!Throwable.class.isAssignableFrom(exceptionType)) {
			throw new IllegalArgumentException(
					"Cannot construct rollback rule from [" + exceptionType.getName() + "]: it's not a Throwable");
		}
		this.exceptionPattern = exceptionType.getName();
	}

	/**
	 * 为给定的{@code exceptionPattern}创建{@code RollbackRuleAttribute}类的新实例。
	 * <p>有关回滚规则语义、模式和可能意外匹配的警告, 请参阅
	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}的javadoc。
	 * @param exceptionPattern the exception name pattern; can also be a fully
	 * package-qualified class name
	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
	 * is {@code null} or empty
	 */
	public RollbackRuleAttribute(String exceptionPattern) {
		Assert.hasText(exceptionPattern, "'exceptionPattern' cannot be null or empty");
		this.exceptionPattern = exceptionPattern;
	}


	/**
	 * 获取此规则用于匹配的已配置异常名称模式。
	 * @see #getDepth(Throwable)
	 */
	public String getExceptionName() {
		return this.exceptionPattern;
	}

	/**
	 * 返回父类匹配的深度, 具有以下语义。
	 * <ul>
	 * <li>{@code -1}表示此规则不匹配给定的{@code exception}。</li>
	 * <li>{@code 0}表示此规则直接匹配给定的{@code exception}。</li>
	 * <li>任何其他正值表示此规则在父类层次结构中匹配给定的{@code exception},
	 * 该值为类层次结构中给定{@code exception}与此规则直接匹配的异常之间的层数。</li>
	 * </ul>
	 * <p>在比较匹配给定异常的回滚规则时, 匹配深度较小的规则获胜。
	 * 例如, 直接匹配({@code depth == 0})胜过父类层次结构中的匹配({@code depth > 0})。
	 * <p>对嵌套异常类型或类似命名的异常类型的匹配将返回一个深度值,
	 * 表示在类层次结构中相应层级的匹配, 如同直接匹配一样。
	 */
	public int getDepth(Throwable exception) {
		return getDepth(exception.getClass(), 0);
	}


	private int getDepth(Class<?> exceptionType, int depth) {
		if (exceptionType.getName().contains(this.exceptionPattern)) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		if (exceptionType == Throwable.class) {
			return -1;
		}
		return getDepth(exceptionType.getSuperclass(), depth + 1);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RollbackRuleAttribute)) {
			return false;
		}
		RollbackRuleAttribute rhs = (RollbackRuleAttribute) other;
		return this.exceptionPattern.equals(rhs.exceptionPattern);
	}

	@Override
	public int hashCode() {
		return this.exceptionPattern.hashCode();
	}

	@Override
	public String toString() {
		return "RollbackRuleAttribute with pattern [" + this.exceptionPattern + "]";
	}

}
