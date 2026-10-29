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

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.beans.factory.FactoryBean}，引导指定的 JCA 1.7
 * {@link javax.resource.spi.ResourceAdapter}，使用本地
 * {@link javax.resource.spi.BootstrapContext} 启动它，并将其暴露给 bean 引用。
 * 它还会在上下文关闭时停止 ResourceAdapter。这对应于 JCA 1.7 规范中
 * 本地环境下的 "非管理" 引导。
 *
 * <p>这本质上是一个适配器，用于以 bean 风格引导 JCA ResourceAdapter，
 * 允许通过 bean 属性指定 BootstrapContext 或其元素（如 JCA WorkManager）。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setResourceAdapter
 * @see #setBootstrapContext
 * @see #setWorkManager
 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
 * @see javax.resource.spi.ResourceAdapter#stop()
 */
public class ResourceAdapterFactoryBean implements FactoryBean<ResourceAdapter>, InitializingBean, DisposableBean {

	@Nullable
	private ResourceAdapter resourceAdapter;

	@Nullable
	private BootstrapContext bootstrapContext;

	@Nullable
	private WorkManager workManager;

	@Nullable
	private XATerminator xaTerminator;


	/**
	 * 将目标 JCA ResourceAdapter 指定为类，使用其默认配置进行实例化。
	 * <p>或者，通过 "resourceAdapter" 属性指定预配置的 ResourceAdapter 实例。
	 * @see #setResourceAdapter
	 */
	public void setResourceAdapterClass(Class<? extends ResourceAdapter> resourceAdapterClass) {
		this.resourceAdapter = BeanUtils.instantiateClass(resourceAdapterClass);
	}

	/**
	 * 指定目标 JCA ResourceAdapter，以已配置但尚未启动的实例形式传入。
	 * 这通常作为内部 bean 定义发生，通过供应商特定的 bean 属性配置 ResourceAdapter 实例。
	 */
	public void setResourceAdapter(ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * 指定用于启动 ResourceAdapter 的 JCA BootstrapContext。
	 * <p>或者，你可以将各个部分（如 JCA WorkManager）指定为单独的引用。
	 * @see #setWorkManager
	 * @see #setXaTerminator
	 */
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	/**
	 * 指定用于引导 ResourceAdapter 的 JCA WorkManager。
	 * @see #setBootstrapContext
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * 指定用于引导 ResourceAdapter 的 JCA XATerminator。
	 * @see #setBootstrapContext
	 */
	public void setXaTerminator(XATerminator xaTerminator) {
		this.xaTerminator = xaTerminator;
	}


	/**
	 * 构建 BootstrapContext 并使用它启动 ResourceAdapter。
	 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
	 */
	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.resourceAdapter == null) {
			throw new IllegalArgumentException("'resourceAdapter' or 'resourceAdapterClass' is required");
		}
		if (this.bootstrapContext == null) {
			this.bootstrapContext = new SimpleBootstrapContext(this.workManager, this.xaTerminator);
		}
		this.resourceAdapter.start(this.bootstrapContext);
	}


	@Override
	@Nullable
	public ResourceAdapter getObject() {
		return this.resourceAdapter;
	}

	@Override
	public Class<? extends ResourceAdapter> getObjectType() {
		return (this.resourceAdapter != null ? this.resourceAdapter.getClass() : ResourceAdapter.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 停止 ResourceAdapter。
	 * @see javax.resource.spi.ResourceAdapter#stop()
	 */
	@Override
	public void destroy() {
		if (this.resourceAdapter != null) {
			this.resourceAdapter.stop();
		}
	}

}
