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

package org.springframework.messaging.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

/**
 * 将消息发送给其每个订阅者的 {@link SubscribableChannel}。
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ExecutorSubscribableChannel extends AbstractSubscribableChannel {

	@Nullable
	private final Executor executor;

	private final List<ExecutorChannelInterceptor> executorInterceptors = new ArrayList<>(4);


	/**
	 * 创建一个新的 {@link ExecutorSubscribableChannel} 实例，
	 * 消息将在调用方线程中发送。
	 */
	public ExecutorSubscribableChannel() {
		this(null);
	}

	/**
	 * 创建一个新的 {@link ExecutorSubscribableChannel} 实例，
	 * 消息将通过指定的执行器发送。
	 * @param executor 用于发送消息的执行器，
	 * 或 {@code null} 表示在调用者线程中执行。
	 */
	public ExecutorSubscribableChannel(@Nullable Executor executor) {
		this.executor = executor;
	}


	@Nullable
	public Executor getExecutor() {
		return this.executor;
	}

	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		super.setInterceptors(interceptors);
		this.executorInterceptors.clear();
		interceptors.forEach(this::updateExecutorInterceptorsFor);
	}

	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		super.addInterceptor(interceptor);
		updateExecutorInterceptorsFor(interceptor);
	}

	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		super.addInterceptor(index, interceptor);
		updateExecutorInterceptorsFor(interceptor);
	}

	private void updateExecutorInterceptorsFor(ChannelInterceptor interceptor) {
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptors.add((ExecutorChannelInterceptor) interceptor);
		}
	}


	@Override
	public boolean sendInternal(Message<?> message, long timeout) {
		for (MessageHandler handler : getSubscribers()) {
			SendTask sendTask = new SendTask(message, handler);
			if (this.executor == null) {
				sendTask.run();
			}
			else {
				this.executor.execute(sendTask);
			}
		}
		return true;
	}


	/**
	 * 使用 ExecutorChannelInterceptors 调用 MessageHandler。
	 */
	private class SendTask implements MessageHandlingRunnable {

		private final Message<?> inputMessage;

		private final MessageHandler messageHandler;

		private int interceptorIndex = -1;

		public SendTask(Message<?> message, MessageHandler messageHandler) {
			this.inputMessage = message;
			this.messageHandler = messageHandler;
		}

		@Override
		public Message<?> getMessage() {
			return this.inputMessage;
		}

		@Override
		public MessageHandler getMessageHandler() {
			return this.messageHandler;
		}

		@Override
		public void run() {
			Message<?> message = this.inputMessage;
			try {
				message = applyBeforeHandle(message);
				if (message == null) {
					return;
				}
				this.messageHandler.handleMessage(message);
				triggerAfterMessageHandled(message, null);
			}
			catch (Exception ex) {
				triggerAfterMessageHandled(message, ex);
				if (ex instanceof MessagingException) {
					throw (MessagingException) ex;
				}
				String description = "Failed to handle " + message + " to " + this + " in " + this.messageHandler;
				throw new MessageDeliveryException(message, description, ex);
			}
			catch (Throwable err) {
				String description = "Failed to handle " + message + " to " + this + " in " + this.messageHandler;
				MessageDeliveryException ex2 = new MessageDeliveryException(message, description, err);
				triggerAfterMessageHandled(message, ex2);
				throw ex2;
			}
		}

		@Nullable
		private Message<?> applyBeforeHandle(Message<?> message) {
			Message<?> messageToUse = message;
			for (ExecutorChannelInterceptor interceptor : executorInterceptors) {
				messageToUse = interceptor.beforeHandle(messageToUse, ExecutorSubscribableChannel.this, this.messageHandler);
				if (messageToUse == null) {
					String name = interceptor.getClass().getSimpleName();
					if (logger.isDebugEnabled()) {
						logger.debug(name + " returned null from beforeHandle, i.e. precluding the send.");
					}
					triggerAfterMessageHandled(message, null);
					return null;
				}
				this.interceptorIndex++;
			}
			return messageToUse;
		}

		private void triggerAfterMessageHandled(Message<?> message, @Nullable Exception ex) {
			for (int i = this.interceptorIndex; i >= 0; i--) {
				ExecutorChannelInterceptor interceptor = executorInterceptors.get(i);
				try {
					interceptor.afterMessageHandled(message, ExecutorSubscribableChannel.this, this.messageHandler, ex);
				}
				catch (Throwable ex2) {
					logger.error("Exception from afterMessageHandled in " + interceptor, ex2);
				}
			}
		}
	}

}
