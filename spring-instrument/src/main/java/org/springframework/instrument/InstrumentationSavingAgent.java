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

package org.springframework.instrument;

import java.lang.instrument.Instrumentation;

/**
 * Java agent，用于保存来自 JVM 的 {@link Instrumentation} 接口，以供后续使用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
 */
public final class InstrumentationSavingAgent {

	private static volatile Instrumentation instrumentation;


	private InstrumentationSavingAgent() {
	}


	/**
	 * 保存 JVM 暴露的 {@link Instrumentation} 接口。
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * 保存 JVM 暴露的 {@link Instrumentation} 接口。使用 Attach API 动态加载此 Agent 时需要此方法。
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * 返回 JVM 暴露的 {@link Instrumentation} 接口。
	 * <p>请注意，除非在 JVM 启动时实际指定了该 agent，否则此类通常不会出现在 classpath 中。如果希望根
	 * 据 agent 是否可用进行条件检查，请考虑改用
	 * {@link org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver#getInstrumentation()}，
	 * 它在 classpath 中没有 agent 类时同样可以工作。
	 * @return the {@code Instrumentation} instance previously saved when
	 * the {@link #premain} or {@link #agentmain} methods was called by the JVM;
	 * will be {@code null} if this class was not used as Java agent when this
	 * JVM was started or it wasn't installed as agent using the Attach API.
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver#getInstrumentation()
	 */
	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}

}
