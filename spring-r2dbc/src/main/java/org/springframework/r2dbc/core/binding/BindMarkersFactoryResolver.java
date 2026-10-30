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

package org.springframework.r2dbc.core.binding;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * 使用 {@link BindMarkerFactoryProvider} 从 {@link ConnectionFactory} 解析
 * {@link BindMarkersFactory}。方言解析使用 Spring 的
 * {@link SpringFactoriesLoader spring.factories} 来确定可用的扩展。
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BindMarkersFactory
 * @see SpringFactoriesLoader
 */
public final class BindMarkersFactoryResolver {

	private static final List<BindMarkerFactoryProvider> DETECTORS = SpringFactoriesLoader.loadFactories(
			BindMarkerFactoryProvider.class, BindMarkersFactoryResolver.class.getClassLoader());


	/**
	 * 通过检查 {@link ConnectionFactory} 及其元数据来获取 {@link BindMarkersFactory}。
	 * @param connectionFactory 要检查的连接工厂
	 * @return 解析后的 {@link BindMarkersFactory}
	 * @throws NoBindMarkersFactoryException 如果无法解析出 {@link BindMarkersFactory}
	 */
	public static BindMarkersFactory resolve(ConnectionFactory connectionFactory) {
		for (BindMarkerFactoryProvider detector : DETECTORS) {
			BindMarkersFactory bindMarkersFactory = detector.getBindMarkers(connectionFactory);
			if (bindMarkersFactory != null) {
				return bindMarkersFactory;
			}
		}
		throw new NoBindMarkersFactoryException(String.format(
				"Cannot determine a BindMarkersFactory for %s using %s",
				connectionFactory.getMetadata().getName(), connectionFactory));
	}


	private BindMarkersFactoryResolver() {
	}


	/**
	 * SPI，用于扩展 Spring 默认的 R2DBC BindMarkersFactory 发现机制。
	 * 此接口的实现通过 Spring 的 {@link SpringFactoriesLoader} 机制发现。
	 * @see SpringFactoriesLoader
	 */
	@FunctionalInterface
	public interface BindMarkerFactoryProvider {

		/**
		 * 返回用于 {@link ConnectionFactory} 的 {@link BindMarkersFactory}。
		 * @param connectionFactory 要与 {@link BindMarkersFactory} 一起使用的连接工厂
		 * @return 如果 {@link BindMarkerFactoryProvider} 能提供
		 * 绑定标记工厂对象，则为 {@link BindMarkersFactory}，
		 * 否则为 {@code null}
		 */
		@Nullable
		BindMarkersFactory getBindMarkers(ConnectionFactory connectionFactory);
	}


	/**
	 * 当 {@link BindMarkersFactoryResolver} 无法解析
	 * {@link BindMarkersFactory} 时抛出的异常。
	 */
	@SuppressWarnings("serial")
	public static class NoBindMarkersFactoryException extends NonTransientDataAccessException {

		/**
		 * NoBindMarkersFactoryException 的构造函数。
		 * @param msg 详细消息
		 */
		public NoBindMarkersFactoryException(String msg) {
			super(msg);
		}
	}


	/**
	 * 内置的绑定标记工厂。当其他提供者以更高优先级注册时，
	 * 通常作为最后一个 {@link BindMarkerFactoryProvider} 使用。
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	static class BuiltInBindMarkersFactoryProvider implements BindMarkerFactoryProvider {

		private static final Map<String, BindMarkersFactory> BUILTIN =
				new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);

		static {
			BUILTIN.put("H2", BindMarkersFactory.indexed("$", 1));
			BUILTIN.put("MariaDB", BindMarkersFactory.anonymous("?"));
			BUILTIN.put("Microsoft SQL Server", BindMarkersFactory.named("@", "P", 32,
					BuiltInBindMarkersFactoryProvider::filterBindMarker));
			BUILTIN.put("MySQL", BindMarkersFactory.anonymous("?"));
			BUILTIN.put("Oracle", BindMarkersFactory.named(":", "P", 32,
					BuiltInBindMarkersFactoryProvider::filterBindMarker));
			BUILTIN.put("PostgreSQL", BindMarkersFactory.indexed("$", 1));
		}


		@Override
		public BindMarkersFactory getBindMarkers(ConnectionFactory connectionFactory) {
			ConnectionFactoryMetadata metadata = connectionFactory.getMetadata();
			BindMarkersFactory r2dbcDialect = BUILTIN.get(metadata.getName());
			if (r2dbcDialect != null) {
				return r2dbcDialect;
			}
			for (String it : BUILTIN.keySet()) {
				if (metadata.getName().contains(it)) {
					return BUILTIN.get(it);
				}
			}
			return null;
		}

		private static String filterBindMarker(CharSequence input) {
			StringBuilder builder = new StringBuilder(input.length());
			for (int i = 0; i < input.length(); i++) {
				char ch = input.charAt(i);
				// ascii letter or digit
				if (Character.isLetterOrDigit(ch) && ch < 127) {
					builder.append(ch);
				}
			}
			if (builder.length() == 0) {
				return "";
			}
			return "_" + builder.toString();
		}
	}

}
