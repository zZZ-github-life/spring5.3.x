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

package org.springframework.jca.support;

import java.util.Timer;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JCA 1.7 {@link javax.resource.spi.BootstrapContext} 接口的简单实现，
 * 用于在本地环境中引导 JCA ResourceAdapter。
 *
 * <p>委托给给定的 WorkManager 和 XATerminator（如果有）。
 * 创建 {@code java.util.Timer} 的简单本地实例。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
 * @see ResourceAdapterFactoryBean
 */
public class SimpleBootstrapContext implements BootstrapContext {

	@Nullable
	private WorkManager workManager;

	@Nullable
	private XATerminator xaTerminator;

	@Nullable
	private TransactionSynchronizationRegistry transactionSynchronizationRegistry;


	/**
	 * 为给定的 WorkManager 创建新的 SimpleBootstrapContext，不提供 XATerminator。
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 */
	public SimpleBootstrapContext(@Nullable WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * 为给定的 WorkManager 和 XATerminator 创建新的 SimpleBootstrapContext。
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 * @param xaTerminator the JCA XATerminator to use (may be {@code null})
	 */
	public SimpleBootstrapContext(@Nullable WorkManager workManager, @Nullable XATerminator xaTerminator) {
		this.workManager = workManager;
		this.xaTerminator = xaTerminator;
	}

	/**
	 * 为给定的 WorkManager、XATerminator 和 TransactionSynchronizationRegistry
	 * 创建新的 SimpleBootstrapContext。
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 * @param xaTerminator the JCA XATerminator to use (may be {@code null})
	 * @param transactionSynchronizationRegistry the TransactionSynchronizationRegistry
	 * to use (may be {@code null})
	 * @since 5.0
	 */
	public SimpleBootstrapContext(@Nullable WorkManager workManager, @Nullable XATerminator xaTerminator,
			@Nullable TransactionSynchronizationRegistry transactionSynchronizationRegistry) {

		this.workManager = workManager;
		this.xaTerminator = xaTerminator;
		this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
	}


	@Override
	public WorkManager getWorkManager() {
		Assert.state(this.workManager != null, "No WorkManager available");
		return this.workManager;
	}

	@Override
	@Nullable
	public XATerminator getXATerminator() {
		return this.xaTerminator;
	}

	@Override
	public Timer createTimer() throws UnavailableException {
		return new Timer();
	}

	@Override
	public boolean isContextSupported(Class<? extends WorkContext> workContextClass) {
		return false;
	}

	@Override
	@Nullable
	public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
		return this.transactionSynchronizationRegistry;
	}

}
