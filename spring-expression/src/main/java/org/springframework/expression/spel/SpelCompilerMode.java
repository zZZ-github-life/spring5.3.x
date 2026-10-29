/*
 * Copyright 2002-2014 the original author or authors.
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

/**
 * 捕获可用于表达式求值的编译器的可能配置设置。
 *
 * @author Andy Clement
 * @since 4.1
 */
public enum SpelCompilerMode {

	/**
	 * 编译器已关闭；这是默认设置。
	 */
	OFF,

	/**
	 * 在即时模式下，表达式会尽快编译（通常在 1 次解释执行之后）。如果已编译的表达式失败，它会向调用方
	 * 抛出异常。
	 */
	IMMEDIATE,

	/**
	 * 在混合模式下，表达式求值会随时间在解释执行与编译执行之间静默切换。经过若干次运行后，表达式会被编
	 * 译。如果之后它失败了（可能是由于推断出的类型信息发生变化），这将在内部被捕获，系统会切换回解释模
	 * 式。之后它可能再次进行编译。
	 */
	MIXED

}
