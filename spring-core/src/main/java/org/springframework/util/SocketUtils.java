/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.util;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.net.ServerSocketFactory;

/**
 * 用于处理网络套接字的简单实用方法 &mdash; 例如，用于在 {@code localhost} 上查找可用端口。
 *
 * <p>在此类中，TCP 端口是指 {@link ServerSocket} 的端口；而 UDP 端口是指 {@link DatagramSocket} 的
 * 端口。
 *
 * <p>{@code SocketUtils} 在 Spring Framework 4.0 中引入，主要为了协助编写在可用随机端口上启动外部
 * 服务器的集成测试。然而，这些工具并不保证给定端口在此后仍然可用，因此并不可靠。与其使用
 * {@code SocketUtils} 为服务器查找可用的本地端口，不如依赖服务器自身能够在其选择或由操作系统分配的
 * 随机端口上启动的能力。要与该服务器交互，应向服务器查询其当前使用的端口。
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 4.0
 * @deprecated as of Spring Framework 5.3.16, to be removed in 6.0; see
 * {@link SocketUtils class-level Javadoc} for details.
 */
@Deprecated
public class SocketUtils {

	/**
	 * 查找可用套接字端口时所用端口范围的默认最小值。
	 */
	public static final int PORT_RANGE_MIN = 1024;

	/**
	 * 查找可用套接字端口时所用端口范围的默认最大值。
	 */
	public static final int PORT_RANGE_MAX = 65535;


	private static final Random random = new Random(System.nanoTime());


	/**
	 * 尽管 {@code SocketUtils} 仅由静态工具方法组成，但此构造函数被有意设为 {@code public}。
	 * <h4>理由</h4>
	 * <p>此类中的静态方法可以在 XML 配置文件中使用 Spring Expression Language（SpEL）及以下语法进行调
	 * 用。
	 * <pre><code>&lt;bean id="bean1" ... p:port="#{T(org.springframework.util.SocketUtils).findAvailableTcpPort(12000)}" /&gt;</code></pre>
	 * 如果此构造函数为 {@code private}，则每次使用时都需要为 SpEL 的 {@code T()} 函数提供完全限定的类
	 * 名。因此，此构造函数为 {@code public} 这一事实允许你像以下示例所示那样减少 SpEL 的样板配置。
	 * <pre><code>&lt;bean id="socketUtils" class="org.springframework.util.SocketUtils" /&gt;
	 * &lt;bean id="bean1" ... p:port="#{socketUtils.findAvailableTcpPort(12000)}" /&gt;
	 * &lt;bean id="bean2" ... p:port="#{socketUtils.findAvailableTcpPort(30000)}" /&gt;</code></pre>
	 */
	public SocketUtils() {
	}


	/**
	 * 从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 TCP 端口。
	 * @return 一个可用的 TCP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableTcpPort() {
		return findAvailableTcpPort(PORT_RANGE_MIN);
	}

	/**
	 * 从范围 [{@code minPort}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 TCP 端口。
	 * @param minPort 最小端口号
	 * @return 一个可用的 TCP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableTcpPort(int minPort) {
		return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
	}

	/**
	 * 从范围 [{@code minPort}, {@code maxPort}] 中随机选择一个可用的 TCP 端口。
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 一个可用的 TCP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableTcpPort(int minPort, int maxPort) {
		return SocketType.TCP.findAvailablePort(minPort, maxPort);
	}

	/**
	 * 查找所需数量的可用 TCP 端口，每个端口从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}]
	 * 中随机选择。
	 * @param numRequested 要查找的可用端口数量
	 * @return 可用 TCP 端口号的排序集合
	 * @throws IllegalStateException 如果找不到所请求数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableTcpPorts(int numRequested) {
		return findAvailableTcpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * 查找所需数量的可用 TCP 端口，每个端口从范围 [{@code minPort}, {@code maxPort}] 中随机选择。
	 * @param numRequested 要查找的可用端口数量
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 可用 TCP 端口号的排序集合
	 * @throws IllegalStateException 如果找不到所请求数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableTcpPorts(int numRequested, int minPort, int maxPort) {
		return SocketType.TCP.findAvailablePorts(numRequested, minPort, maxPort);
	}

	/**
	 * 从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 UDP 端口。
	 * @return 一个可用的 UDP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableUdpPort() {
		return findAvailableUdpPort(PORT_RANGE_MIN);
	}

	/**
	 * 从范围 [{@code minPort}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 UDP 端口。
	 * @param minPort 最小端口号
	 * @return 一个可用的 UDP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableUdpPort(int minPort) {
		return findAvailableUdpPort(minPort, PORT_RANGE_MAX);
	}

	/**
	 * 从范围 [{@code minPort}, {@code maxPort}] 中随机选择一个可用的 UDP 端口。
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 一个可用的 UDP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableUdpPort(int minPort, int maxPort) {
		return SocketType.UDP.findAvailablePort(minPort, maxPort);
	}

	/**
	 * 查找所需数量的可用 UDP 端口，每个端口从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}]
	 * 中随机选择。
	 * @param numRequested 要查找的可用端口数量
	 * @return 可用 UDP 端口号的排序集合
	 * @throws IllegalStateException 如果找不到所请求数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableUdpPorts(int numRequested) {
		return findAvailableUdpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * 查找所需数量的可用 UDP 端口，每个端口从范围 [{@code minPort}, {@code maxPort}] 中随机选择。
	 * @param numRequested 要查找的可用端口数量
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 可用 UDP 端口号的排序集合
	 * @throws IllegalStateException 如果找不到所请求数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableUdpPorts(int numRequested, int minPort, int maxPort) {
		return SocketType.UDP.findAvailablePorts(numRequested, minPort, maxPort);
	}


	private enum SocketType {

		TCP {
			@Override
			protected boolean isPortAvailable(int port) {
				try {
					ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
							port, 1, InetAddress.getByName("localhost"));
					serverSocket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		},

		UDP {
			@Override
			protected boolean isPortAvailable(int port) {
				try {
					DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
					socket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		};

		/**
		 * 判断此 {@code SocketType} 的指定端口当前在 {@code localhost} 上是否可用。
		 */
		protected abstract boolean isPortAvailable(int port);

		/**
		 * 在范围 [{@code minPort}, {@code maxPort}] 内查找一个伪随机端口号。
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * @return 指定范围内的随机端口号
		 */
		private int findRandomPort(int minPort, int maxPort) {
			int portRange = maxPort - minPort;
			return minPort + random.nextInt(portRange + 1);
		}

		/**
		 * 为此 {@code SocketType} 查找一个可用端口，从范围 [{@code minPort}, {@code maxPort}] 中随机选择。
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * @return 此套接字类型的可用端口号
		 * @throws IllegalStateException 如果找不到可用端口
		 */
		int findAvailablePort(int minPort, int maxPort) {
			Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
			Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
			Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

			int portRange = maxPort - minPort;
			int candidatePort;
			int searchCounter = 0;
			do {
				if (searchCounter > portRange) {
					throw new IllegalStateException(String.format(
							"Could not find an available %s port in the range [%d, %d] after %d attempts",
							name(), minPort, maxPort, searchCounter));
				}
				candidatePort = findRandomPort(minPort, maxPort);
				searchCounter++;
			}
			while (!isPortAvailable(candidatePort));

			return candidatePort;
		}

		/**
		 * 为此 {@code SocketType} 查找所需数量的可用端口，每个端口从范围 [{@code minPort}, {@code maxPort}
		 * ] 中随机选择。
		 * @param numRequested 要查找的可用端口数量
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * @return 该套接字类型下可用端口号的有序集合
		 * @throws IllegalStateException 如果找不到请求数量的可用端口
		 */
		SortedSet<Integer> findAvailablePorts(int numRequested, int minPort, int maxPort) {
			Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
			Assert.isTrue(maxPort > minPort, "'maxPort' must be greater than 'minPort'");
			Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);
			Assert.isTrue(numRequested > 0, "'numRequested' must be greater than 0");
			Assert.isTrue((maxPort - minPort) >= numRequested,
					"'numRequested' must not be greater than 'maxPort' - 'minPort'");

			SortedSet<Integer> availablePorts = new TreeSet<>();
			int attemptCount = 0;
			while ((++attemptCount <= numRequested + 100) && availablePorts.size() < numRequested) {
				availablePorts.add(findAvailablePort(minPort, maxPort));
			}

			if (availablePorts.size() != numRequested) {
				throw new IllegalStateException(String.format(
						"Could not find %d available %s ports in the range [%d, %d]",
						numRequested, name(), minPort, maxPort));
			}

			return availablePorts;
		}
	}

}
