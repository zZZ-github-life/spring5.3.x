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

package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;

import org.springframework.lang.Nullable;

/**
 * 简单的 {@link SecurityContextProvider} 实现。
 *
 * @author Costin Leau
 * @since 3.0
 */
public class SimpleSecurityContextProvider implements SecurityContextProvider {

	@Nullable
	private final AccessControlContext acc;


	/**
	 * 构造一个新的 {@code SimpleSecurityContextProvider} 实例。<p>每次调用时都会从当前线程获取安全上下
	 * 文。
	 */
	public SimpleSecurityContextProvider() {
		this(null);
	}

	/**
	 * 构造一个新的 {@code SimpleSecurityContextProvider} 实例。<p>如果给定的控制上下文为 null，则每次
	 * 调用时都会从当前线程获取安全上下文。
	 * @param acc access control context (can be {@code null})
	 * @see AccessController#getContext()
	 */
	public SimpleSecurityContextProvider(@Nullable AccessControlContext acc) {
		this.acc = acc;
	}


	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.acc != null ? this.acc : AccessController.getContext());
	}

}
