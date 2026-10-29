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

package org.springframework.web.context.request;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * RequestAttributes 实现的抽象支持类，为请求特定的销毁回调以及更新已访问的会话属性提供请求完成机
 * 制。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #requestCompleted()
 */
public abstract class AbstractRequestAttributes implements RequestAttributes {

	/** 从属性名称 String 到销毁回调 Runnable 的映射。 */
	protected final Map<String, Runnable> requestDestructionCallbacks = new LinkedHashMap<>(8);

	private volatile boolean requestActive = true;


	/**
	 * 表示请求已完成。
	 * <p>执行所有请求销毁回调，并更新在请求处理期间访问过的会话属性。
	 */
	public void requestCompleted() {
		executeRequestDestructionCallbacks();
		updateAccessedSessionAttributes();
		this.requestActive = false;
	}

	/**
	 * 判断原始请求是否仍处于活动状态。
	 * @see #requestCompleted()
	 */
	protected final boolean isRequestActive() {
		return this.requestActive;
	}

	/**
	 * 注册给定的回调，使其在请求完成后执行。
	 * @param name the name of the attribute to register the callback for
	 * @param callback the callback to be executed for destruction
	 */
	protected final void registerRequestDestructionCallback(String name, Runnable callback) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(callback, "Callback must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.put(name, callback);
		}
	}

	/**
	 * 移除指定属性的请求销毁回调（如果存在）。
	 * @param name the name of the attribute to remove the callback for
	 */
	protected final void removeRequestDestructionCallback(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.remove(name);
		}
	}

	/**
	 * 执行所有已注册为在请求完成后执行的回调。
	 */
	private void executeRequestDestructionCallbacks() {
		synchronized (this.requestDestructionCallbacks) {
			for (Runnable runnable : this.requestDestructionCallbacks.values()) {
				runnable.run();
			}
			this.requestDestructionCallbacks.clear();
		}
	}

	/**
	 * 更新在请求处理期间访问过的所有会话属性，以便将其可能已更新的状态暴露给底层的会话管理器。
	 */
	protected abstract void updateAccessedSessionAttributes();

}
