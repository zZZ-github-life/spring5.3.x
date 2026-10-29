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

package org.apache.commons.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准 Apache Commons Logging 的 {@code LogFactory} 类的一个最小化子类，重写了抽象的
 * {@code getInstance} 查找方法。它仅用于标准 {@code commons-logging} jar 意外出现在类路径上、由标
 * 准 {@code LogFactory} 类执行 META-INF 服务发现的情况。此实现只是委托给 Spring 通用的 {@link Log}
 * 工厂方法。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @deprecated since it is only meant to be used in the above-mentioned fallback scenario
 */
@Deprecated
public class LogFactoryService extends LogFactory {

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();


	@Override
	public Log getInstance(Class<?> clazz) {
		return getInstance(clazz.getName());
	}

	@Override
	public Log getInstance(String name) {
		return LogAdapter.createLog(name);
	}


	// Just in case some code happens to call uncommon Commons Logging methods...

	public void setAttribute(String name, Object value) {
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public String[] getAttributeNames() {
		return this.attributes.keySet().toArray(new String[0]);
	}

	public void release() {
	}

}
