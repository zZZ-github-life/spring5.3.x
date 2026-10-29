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

package org.springframework.core.env;

import java.util.function.Predicate;

/**
 * 可被 {@link Environment} {@linkplain Environment#acceptsProfiles(Profiles) 接受}的 profile 谓词。
 *
 *
 * <p>可以直接实现，但更常见的是使用 {@link #of(String...) of(...)} 工厂方法创建。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
@FunctionalInterface
public interface Profiles {

	/**
	 * 测试此 {@code Profiles} 实例是否与给定的活动 profile 谓词<em>匹配</em>。
	 * @param activeProfiles a predicate that tests whether a given profile is
	 * currently active
	 */
	boolean matches(Predicate<String> activeProfiles);


	/**
	 * 创建一个新的 {@link Profiles} 实例，用于检查是否与给定的 <em>profile 字符串</em>匹配。
	 * <p>只要给定的任一 profile 字符串匹配，返回的实例就会
	 * {@linkplain Profiles#matches(Predicate) 匹配}。
	 * <p>profile 字符串可以包含简单的 profile 名称（例如 {@code "production"}），
	 * 也可以包含 profile 表达式。profile 表达式允许表达更复杂的 profile 逻辑，
	 * 例如 {@code "production & cloud"}。
	 * <p>profile 表达式中支持以下运算符。
	 * <ul>
	 * <li>{@code !} - profile 或 profile 表达式的逻辑 <em>非</em></li>
	 * <li>{@code &} - profile 或 profile 表达式的逻辑 <em>与</em></li>
	 * <li>{@code |} - profile 或 profile 表达式的逻辑 <em>或</em></li>
	 * </ul>
	 * <p>请注意，{@code &} 和 {@code |} 运算符不能混用而不加括号。
	 * 例如 {@code "a & b | c"} 不是有效的表达式；
	 * 必须表示为 {@code "(a & b) | c"} 或 {@code "a & (b | c)"}。
	 * <p>从 Spring Framework 5.1.17 起，此方法返回的两个 {@code Profiles} 实例
	 * 如果使用相同的 <em>profile 字符串</em>创建，则被视为彼此等价
	 * （就 {@code equals()} 和 {@code hashCode()} 的语义而言）。
	 * @param profiles the <em>profile strings</em> to include
	 * @return a new {@link Profiles} instance
	 */
	static Profiles of(String... profiles) {
		return ProfilesParser.parse(profiles);
	}

}
