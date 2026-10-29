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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * {@link HeaderMapper} 实现的基础类。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <T> type of the instance to and from which headers will be mapped
 */
public abstract class AbstractHeaderMapper<T> implements HeaderMapper<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private String inboundPrefix = "";

	private String outboundPrefix = "";


	/**
	 * 指定一个前缀，添加到映射到 MessageHeaders 的任何用户定义属性所对应的
	 * 消息头部名称之前。默认为空字符串（无前缀）。
	 */
	public void setInboundPrefix(@Nullable String inboundPrefix) {
		this.inboundPrefix = (inboundPrefix != null ? inboundPrefix : "");
	}

	/**
	 * 指定一个前缀，添加到映射到特定协议消息的任何用户定义消息头部所对应的
	 * 协议属性名称之前。默认为空字符串（无前缀）。
	 */
	public void setOutboundPrefix(@Nullable String outboundPrefix) {
		this.outboundPrefix = (outboundPrefix != null ? outboundPrefix : "");
	}


	/**
	 * 生成用于将指定 {@code headerName} 定义的头部设置到特定协议消息中的名称。
	 * @see #setOutboundPrefix
	 */
	protected String fromHeaderName(String headerName) {
		String propertyName = headerName;
		if (StringUtils.hasText(this.outboundPrefix) && !propertyName.startsWith(this.outboundPrefix)) {
			propertyName = this.outboundPrefix + headerName;
		}
		return propertyName;
	}

	/**
	 * 生成用于将指定 {@code propertyName} 定义的头部设置到
	 * {@link MessageHeaders} 实例中的名称。
	 * @see #setInboundPrefix(String)
	 */
	protected String toHeaderName(String propertyName) {
		String headerName = propertyName;
		if (StringUtils.hasText(this.inboundPrefix) && !headerName.startsWith(this.inboundPrefix)) {
			headerName = this.inboundPrefix + propertyName;
		}
		return headerName;
	}

	/**
	 * 返回头部值；如果不存在或与请求的 {@code type} 不匹配，则返回 {@code null}。
	 */
	@Nullable
	protected <V> V getHeaderIfAvailable(Map<String, Object> headers, String name, Class<V> type) {
		Object value = headers.get(name);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping header '" + name + "': expected type [" + type + "], but got [" +
						value.getClass() + "]");
			}
			return null;
		}
		else {
			return type.cast(value);
		}
	}

}
