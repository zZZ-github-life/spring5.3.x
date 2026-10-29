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

package org.springframework.cglib.core;

/**
 * CGLIB 的 {@link DefaultNamingPolicy} 的自定义扩展，将生成类名中的标记从“ByCGLIB”改为
 * “BySpringCGLIB”。
 *
 * <p>这主要用于避免常规 CGLIB 版本（被某些其他库使用）与 Spring 内嵌变体之间的冲突，以防同一个类恰
 * 好因不同目的而被代理。
 *
 * @author Juergen Hoeller
 * @since 3.2.8
 */
public class SpringNamingPolicy extends DefaultNamingPolicy {

	public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

	@Override
	protected String getTag() {
		return "BySpringCGLIB";
	}

}
