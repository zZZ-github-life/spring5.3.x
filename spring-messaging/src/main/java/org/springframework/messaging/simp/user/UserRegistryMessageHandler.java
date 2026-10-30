/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * {@code MessageHandler}，处理来自其他应用服务器的用户注册表广播，
 * 并定期广播本地用户注册表的内容。
 *
 * <p>聚合信息维护在 {@link MultiServerUserRegistry} 中。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class UserRegistryMessageHandler implements MessageHandler, ApplicationListener<BrokerAvailabilityEvent> {

	private final MultiServerUserRegistry userRegistry;

	private final SimpMessagingTemplate brokerTemplate;

	private final String broadcastDestination;

	private final TaskScheduler scheduler;

	private final UserRegistryTask schedulerTask = new UserRegistryTask();

	@Nullable
	private volatile ScheduledFuture<?> scheduledFuture;

	private long registryExpirationPeriod = TimeUnit.SECONDS.toMillis(20);


	/**
	 * 构造函数。
	 * @param userRegistry 包含本地和远程用户注册表信息的注册表
	 * @param brokerTemplate 用于广播本地注册表信息的模板
	 * @param broadcastDestination 要广播到的目标地址
	 * @param scheduler 要使用的任务调度器
	 */
	public UserRegistryMessageHandler(MultiServerUserRegistry userRegistry,
			SimpMessagingTemplate brokerTemplate, String broadcastDestination, TaskScheduler scheduler) {

		Assert.notNull(userRegistry, "'userRegistry' is required");
		Assert.notNull(brokerTemplate, "'brokerTemplate' is required");
		Assert.hasText(broadcastDestination, "'broadcastDestination' is required");
		Assert.notNull(scheduler, "'scheduler' is required");

		this.userRegistry = userRegistry;
		this.brokerTemplate = brokerTemplate;
		this.broadcastDestination = broadcastDestination;
		this.scheduler = scheduler;
	}


	/**
	 * 返回已配置的用于广播 UserRegistry 信息的目标。
	 */
	public String getBroadcastDestination() {
		return this.broadcastDestination;
	}

	/**
	 * 配置远程用户注册表快照被视为过期之前的时间量（以毫秒为单位）。
	 * <p>默认设置为 20 秒（值 20000）。
	 * @param milliseconds 过期时间，单位为毫秒
	 */
	@SuppressWarnings("unused")
	public void setRegistryExpirationPeriod(long milliseconds) {
		this.registryExpirationPeriod = milliseconds;
	}

	/**
	 * 返回已配置的注册表过期时间。
	 */
	public long getRegistryExpirationPeriod() {
		return this.registryExpirationPeriod;
	}


	@Override
	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		if (event.isBrokerAvailable()) {
			long delay = getRegistryExpirationPeriod() / 2;
			this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(this.schedulerTask, delay);
		}
		else {
			ScheduledFuture<?> future = this.scheduledFuture;
			if (future != null ){
				future.cancel(true);
				this.scheduledFuture = null;
			}
		}
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		MessageConverter converter = this.brokerTemplate.getMessageConverter();
		this.userRegistry.addRemoteRegistryDto(message, converter, getRegistryExpirationPeriod());
	}


	private class UserRegistryTask implements Runnable {

		@Override
		public void run() {
			try {
				SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
				accessor.setHeader(SimpMessageHeaderAccessor.IGNORE_ERROR, true);
				accessor.setLeaveMutable(true);
				Object payload = userRegistry.getLocalRegistryDto();
				brokerTemplate.convertAndSend(getBroadcastDestination(), payload, accessor.getMessageHeaders());
			}
			finally {
				userRegistry.purgeExpiredRegistries();
			}
		}
	}

}
