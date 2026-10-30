/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.transaction.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link TransactionalApplicationListener}适配器, 将事件的处理委托给目标{@link ApplicationListener}实例。
 * 支持与任何常规{@link ApplicationListener}完全相同的功能, 但能感知事件发布者的事务上下文。
 *
 * <p>对于简单的{@link org.springframework.context.PayloadApplicationEvent}处理,
 * 请考虑使用{@link TransactionalApplicationListener#forPayload}工厂方法作为自定义使用此适配器类的便捷替代方案。
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @param <E> 要监听的具体 {@code ApplicationEvent} 子类
 * @see TransactionalApplicationListener
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerMethodAdapter
 */
public class TransactionalApplicationListenerAdapter<E extends ApplicationEvent>
		implements TransactionalApplicationListener<E>, Ordered {

	private final ApplicationListener<E> targetListener;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private TransactionPhase transactionPhase = TransactionPhase.AFTER_COMMIT;

	private String listenerId = "";

	private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();


	/**
	 * 构造一个新的TransactionalApplicationListenerAdapter。
	 * @param targetListener 在指定事务阶段要调用的实际监听器
	 * @see #setTransactionPhase
	 * @see TransactionalApplicationListener#forPayload
	 */
	public TransactionalApplicationListenerAdapter(ApplicationListener<E> targetListener) {
		this.targetListener = targetListener;
	}


	/**
	 * 指定监听器的同步顺序。
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * 返回监听器的同步顺序。
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定调用监听器的事务阶段。
	 * <p>默认值为{@link TransactionPhase#AFTER_COMMIT}。
	 */
	public void setTransactionPhase(TransactionPhase transactionPhase) {
		this.transactionPhase = transactionPhase;
	}

	/**
	 * 返回调用监听器的事务阶段。
	 */
	@Override
	public TransactionPhase getTransactionPhase() {
		return this.transactionPhase;
	}

	/**
	 * 指定用于标识监听器的id。
	 * <p>默认值为空字符串。
	 */
	public void setListenerId(String listenerId) {
		this.listenerId = listenerId;
	}

	/**
	 * 返回用于标识监听器的id。
	 */
	@Override
	public String getListenerId() {
		return this.listenerId;
	}

	@Override
	public void addCallback(SynchronizationCallback callback) {
		Assert.notNull(callback, "SynchronizationCallback must not be null");
		this.callbacks.add(callback);
	}

	@Override
	public void processEvent(E event) {
		this.targetListener.onApplicationEvent(event);
	}


	@Override
	public void onApplicationEvent(E event) {
		if (TransactionSynchronizationManager.isSynchronizationActive() &&
				TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionalApplicationListenerSynchronization<>(event, this, this.callbacks));
		}
	}

}
