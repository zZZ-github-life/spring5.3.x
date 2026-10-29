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

package org.apache.commons.logging.impl;

/**
 * 最初是一个由系统属性配置的简单 Commons Logging 提供者。在 {@code spring-jcl} 中已废弃，实际上等
 * 价于 {@link NoOpLog}。
 *
 * <p>不要直接实例化它，而应调用 {@code LogFactory#getLog(Class/String)}；当 Log4j 和 SLF4J 都不存
 * 在时，它会回退到 {@code java.util.logging}。
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 * @deprecated in {@code spring-jcl} (effectively equivalent to {@link NoOpLog})
 */
@Deprecated
@SuppressWarnings("serial")
public class SimpleLog extends NoOpLog {

	public SimpleLog(String name) {
		super(name);
		System.out.println(SimpleLog.class.getName() + " is deprecated and equivalent to NoOpLog in spring-jcl. " +
				"Use a standard LogFactory.getLog(Class/String) call instead.");
	}

}
