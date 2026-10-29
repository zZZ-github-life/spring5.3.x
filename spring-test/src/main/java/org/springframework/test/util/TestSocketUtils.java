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

package org.springframework.test.util;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

import javax.net.ServerSocketFactory;

import org.springframework.util.Assert;

/**
 * 用于在 {@code localhost} 上查找可用 TCP 端口以用于集成测试场景的简单实用工具。
 *
 * <p>这是 {@link org.springframework.util.SocketUtils} 的受限形式，后者自 Spring Framework 5.3.16 起已废弃，并在 Spring Framework 6.0 中移除。
 *
 * <p>{@code TestSocketUtils} 可用于在可用随机端口上启动外部服务器的集成测试。然而，这些实用工具不保证给定端口的后续可用性，因此不可靠。建议不要使用 {@code TestSocketUtils} 为服务器查找可用的本地端口，而应依赖服务器在随机<em>临时</em>端口（由服务器选择或由操作系统分配）上启动的能力。要与该服务器交互，应向服务器查询其当前使用的端口。
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Chris Bono
 * @since 5.3.24
 */
public class TestSocketUtils {

	/**
	 * 查找可用 TCP 端口时使用的端口范围的最小值。
	 */
	static final int PORT_RANGE_MIN = 1024;

	/**
	 * 查找可用 TCP 端口时使用的端口范围的最大值。
	 */
	static final int PORT_RANGE_MAX = 65535;

	private static final int PORT_RANGE_PLUS_ONE = PORT_RANGE_MAX - PORT_RANGE_MIN + 1;

	private static final int MAX_ATTEMPTS = 1_000;

	private static final Random random = new Random(System.nanoTime());

	private static final TestSocketUtils INSTANCE = new TestSocketUtils();


	/**
	 * 尽管 {@code TestSocketUtils} 仅由静态实用方法组成，但此构造器被有意设计为 {@code public}。
	 * <h5>理由</h5>
	 * <p>此类中的静态方法可以在 XML 配置文件中使用 Spring 表达式语言（SpEL）通过以下语法调用。
	 * <pre><code>
	 * &lt;bean id="myBean" ... p:port="#{T(org.springframework.test.util.TestSocketUtils).findAvailableTcpPort()}" /&gt;</code>
	 * </pre>
	 * <p>如果此构造器是 {@code private}，则每次使用时都需要向 SpEL 的 {@code T()} 函数提供完全限定的类名。因此，此构造器为 {@code public} 这一事实允许您减少 SpEL 相关的样板配置，如以下示例所示。
	 * <pre><code>
	 * &lt;bean id="socketUtils" class="org.springframework.test.util.TestSocketUtils" /&gt;
	 * &lt;bean id="myBean" ... p:port="#{socketUtils.findAvailableTcpPort()}" /&gt;</code>
	 * </pre>
	 */
	public TestSocketUtils() {
	}

	/**
	 * 从范围 [1024, 65535] 中随机选择一个可用 TCP 端口。
	 * @return an available TCP port number
	 * @throws IllegalStateException if no available port could be found
	 */
	public static int findAvailableTcpPort() {
		return INSTANCE.findAvailableTcpPortInternal();
	}


	/**
	 * {@link #findAvailableTcpPort()} 的内部实现。
	 * <p>仅为测试目的而使用包级私有可见性。
	 */
	int findAvailableTcpPortInternal() {
		int candidatePort;
		int searchCounter = 0;
		do {
			Assert.state(++searchCounter <= MAX_ATTEMPTS, () -> String.format(
					"Could not find an available TCP port in the range [%d, %d] after %d attempts",
					PORT_RANGE_MIN, PORT_RANGE_MAX, MAX_ATTEMPTS));
			candidatePort = PORT_RANGE_MIN + random.nextInt(PORT_RANGE_PLUS_ONE);
		}
		while (!isPortAvailable(candidatePort));

		return candidatePort;
	}

	/**
	 * 确定指定的 TCP 端口当前在 {@code localhost} 上是否可用。
	 * <p>仅为测试目的而使用包级私有可见性。
	 */
	boolean isPortAvailable(int port) {
		try {
			ServerSocket serverSocket = ServerSocketFactory.getDefault()
					.createServerSocket(port, 1, InetAddress.getByName("localhost"));
			serverSocket.close();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
