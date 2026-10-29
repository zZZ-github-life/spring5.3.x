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

package org.springframework.expression;

/**
 * {@link OperatorOverloader} 可针对任意一对操作数实现的受支持运算。
 *
 * @author Andy Clement
 * @since 3.0
 */
public enum Operation {

	/**
	 * 加法运算。
	 */
	ADD,

	/**
	 * 减法运算。
	 */
	SUBTRACT,

	/**
	 * 除法运算。
	 */
	DIVIDE,

	/**
	 * 乘法运算。
	 */
	MULTIPLY,

	/**
	 * 取模运算。
	 */
	MODULUS,

	/**
	 * 乘方运算。
	 */
	POWER

}
