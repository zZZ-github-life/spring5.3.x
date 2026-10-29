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

package org.springframework.core;

/**
 * {@link ParameterNameDiscoverer} 策略接口的默认实现，使用 Java 8 标准反射机制，
 * 并回退到基于 ASM 的 {@link LocalVariableTableParameterNameDiscoverer}
 * 来检查类文件中的调试信息（例如针对使用早期 Java 版本编译的类）。
 *
 * <p>如果存在 Kotlin 反射实现，
 * {@link KotlinReflectionParameterNameDiscoverer} 会被添加到列表的最前面，
 * 并用于 Kotlin 类和接口。在以 GraalVM 原生镜像编译或运行时，
 * 不会使用 {@code KotlinReflectionParameterNameDiscoverer}。
 *
 * <p>可以通过 {@link #addDiscoverer(ParameterNameDiscoverer)} 添加更多发现器。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 4.0
 * @see StandardReflectionParameterNameDiscoverer
 * @see LocalVariableTableParameterNameDiscoverer
 * @see KotlinReflectionParameterNameDiscoverer
 */
public class DefaultParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {

	public DefaultParameterNameDiscoverer() {
		// TODO Remove this conditional inclusion when upgrading to Kotlin 1.5, see https://youtrack.jetbrains.com/issue/KT-44594
		if (KotlinDetector.isKotlinReflectPresent() && !NativeDetector.inNativeImage()) {
			addDiscoverer(new KotlinReflectionParameterNameDiscoverer());
		}
		addDiscoverer(new StandardReflectionParameterNameDiscoverer());
		addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
	}

}
