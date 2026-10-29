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

package org.springframework.jms.connection;

import java.util.ArrayList;
import java.util.List;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.springframework.util.Assert;

/**
 * JMS ExceptionListener 接口的实现，支持链式调用，
 * 允许按顺序添加多个 ExceptionListener 实例。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ChainedExceptionListener implements ExceptionListener {

	/** ExceptionListener 列表。 */
	private final List<ExceptionListener> delegates = new ArrayList<>(2);


	/**
	 * 向链式委托列表中添加一个 ExceptionListener。
	 */
	public final void addDelegate(ExceptionListener listener) {
		Assert.notNull(listener, "ExceptionListener must not be null");
		this.delegates.add(listener);
	}

	/**
	 * 返回所有已注册的 ExceptionListener 委托（以数组形式）。
	 */
	public final ExceptionListener[] getDelegates() {
		return this.delegates.toArray(new ExceptionListener[0]);
	}


	@Override
	public void onException(JMSException ex) {
		for (ExceptionListener listener : this.delegates) {
			listener.onException(ex);
		}
	}

}
