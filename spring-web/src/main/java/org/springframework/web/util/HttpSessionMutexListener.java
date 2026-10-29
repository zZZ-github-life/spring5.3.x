/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.util;

import java.io.Serializable;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * 一个 Servlet HttpSessionListener，在创建 HttpSession 时自动公开
 * 会话互斥锁。可在 {@code web.xml} 中注册为监听器。
 *
 * <p>会话互斥锁保证在整个会话生命周期内都是同一个对象，可通过
 * {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键获取。它可作为当前
 * 会话加锁时进行同步的安全引用。
 *
 * <p>在许多情况下，HttpSession 引用本身也是安全的互斥锁，因为对于
 * 同一个活动逻辑会话，它始终是相同的对象引用。但是，不同 Servlet
 * 容器并不保证这一点；唯一 100% 安全的方式是使用会话互斥锁。
 *
 * @author Juergen Hoeller
 * @since 1.2.7
 * @see WebUtils#SESSION_MUTEX_ATTRIBUTE
 * @see WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
 * @see org.springframework.web.servlet.mvc.AbstractController#setSynchronizeOnSession
 */
public class HttpSessionMutexListener implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		event.getSession().setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Mutex());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		event.getSession().removeAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
	}


	/**
	 * 要注册的互斥锁。
	 * 除了作为用于同步的普通 Object 外无需其他内容。
	 * 应可序列化，以支持 HttpSession 持久化。
	 */
	@SuppressWarnings("serial")
	private static class Mutex implements Serializable {
	}

}
