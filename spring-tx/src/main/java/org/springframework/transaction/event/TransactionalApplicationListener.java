/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.function.Consumer;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * 根据{@link TransactionPhase}进行调用的{@link ApplicationListener}。
 * 这是{@link TransactionalEventListener}注解的编程等价物。
 *
 * <p>在监听器实现中添加{@link org.springframework.core.Ordered}, 允许在事务完成之前
 * 或之后运行的其他监听器中对该监听器进行优先级排序。
 *
 * <p><b>注意: 事务性事件监听器仅适用于由{@link org.springframework.transaction.PlatformTransactionManager
 * PlatformTransactionManager}管理的线程绑定事务。</b>由
 * {@link org.springframework.transaction.ReactiveTransactionManager ReactiveTransactionManager}管理的
 * 响应式事务使用Reactor上下文而非线程局部变量, 因此从事件监听器的角度来看,
 * 没有它可以参与的兼容活动事务。
 *
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 * @since 5.3
 * @param <E> 要监听的具体 {@code ApplicationEvent} 子类
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerAdapter
 * @see #forPayload
 */
public interface TransactionalApplicationListener<E extends ApplicationEvent>
		extends ApplicationListener<E>, Ordered {

	/**
	 * 返回在事务同步中的执行顺序。
	 * <p>默认值为{@link Ordered#LOWEST_PRECEDENCE}。
	 * @see org.springframework.transaction.support.TransactionSynchronization#getOrder()
	 */
	@Override
	default int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * 返回监听器的标识符, 以便能够单独引用它。
	 * <p>对于特定的完成回调实现, 可能需要提供特定的id,
	 * 而在其他场景中, 空字符串(作为通用默认值)也是可以接受的。
	 * @see org.springframework.context.event.SmartApplicationListener#getListenerId()
	 * @see TransactionalEventListener#id
	 * @see #addCallback
	 */
	default String getListenerId() {
		return "";
	}

	/**
	 * 返回监听器被调用的{@link TransactionPhase}。
	 * <p>默认阶段为{@link TransactionPhase#AFTER_COMMIT}。
	 */
	default TransactionPhase getTransactionPhase() {
		return TransactionPhase.AFTER_COMMIT;
	}

	/**
	 * 添加一个在事务同步处理中调用的回调, 即在实际事务中触发{@link #processEvent}时。
	 * @param callback 要应用的事务同步回调
	 */
	void addCallback(SynchronizationCallback callback);

	/**
	 * 立即处理给定的{@link ApplicationEvent}。与{@link #onApplicationEvent(ApplicationEvent)}不同,
	 * 调用此方法将直接处理给定事件, 而不会将其延迟到关联的{@link #getTransactionPhase() 事务阶段}。
	 * @param event 要通过目标监听器实现处理的事件
	 */
	void processEvent(E event);


	/**
	 * 为给定的负载消费者创建一个新的{@code TransactionalApplicationListener},
	 * 在默认阶段{@link TransactionPhase#AFTER_COMMIT}中应用。
	 * @param consumer 事件负载消费者
	 * @param <T> 事件负载的类型
	 * @return 对应的 {@code TransactionalApplicationListener} 实例
	 * @see PayloadApplicationEvent#getPayload()
	 * @see TransactionalApplicationListenerAdapter
	 */
	static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
		return forPayload(TransactionPhase.AFTER_COMMIT, consumer);
	}

	/**
	 * 为给定的负载消费者创建一个新的{@code TransactionalApplicationListener}。
	 * @param phase 调用监听器所处的事务阶段
	 * @param consumer 事件负载消费者
	 * @param <T> 事件负载的类型
	 * @return 对应的 {@code TransactionalApplicationListener} 实例
	 * @see PayloadApplicationEvent#getPayload()
	 * @see TransactionalApplicationListenerAdapter
	 */
	static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(
			TransactionPhase phase, Consumer<T> consumer) {

		TransactionalApplicationListenerAdapter<PayloadApplicationEvent<T>> listener =
				new TransactionalApplicationListenerAdapter<>(event -> consumer.accept(event.getPayload()));
		listener.setTransactionPhase(phase);
		return listener;
	}


	/**
	 * 在同步驱动的事件处理上调用的回调, 包装目标监听器调用({@link #processEvent})。
	 *
	 * @see #addCallback
	 * @see #processEvent
	 */
	interface SynchronizationCallback {

		/**
		 * 在事务性事件监听器调用之前调用。
		 * @param event 事务同步即将处理的事件
		 */
		default void preProcessEvent(ApplicationEvent event) {
		}

		/**
		 * 在事务性事件监听器调用之后调用。
		 * @param event 事务同步已完成处理的事件
		 * @param ex 监听器调用期间发生的异常（如果有）
		 */
		default void postProcessEvent(ApplicationEvent event, @Nullable Throwable ex) {
		}
	}

}
