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

package org.springframework.remoting;

import org.springframework.core.NestedRuntimeException;

/**
 * 通用的远程访问异常。任何 remoting 协议的服务代理都应抛出此异常或其子类，以便透明地暴露普通的
 * Java 业务接口。
 *
 * <p>使用符合规范的代理时，切换实际的 remoting 协议（例如不再使用 Hessian）不会影响客户端代码。客
 * 户端使用服务所暴露的普通、自然的 Java 业务接口工作。客户端对象只需通过 Bean 引用接收其所需接口的
 * 实现，就像使用本地 Bean 一样。
 *
 * <p>客户端如果愿意，可以捕获 RemoteAccessException，但由于远程访问错误通常不可恢复，它很可能会让
 * 此类异常传播到更高层，由更高层以通用方式处理。在这种情况下，客户端代码不会显示出参与远程访问的迹
 * 象，因为不存在任何 remoting 特定的依赖。
 *
 * <p>即使从远程服务代理切换到同一接口的本地实现，也仅涉及配置问题。显然，客户端代码应略微意识到它
 * <i>可能正在</i>针对远程服务工作，例如就那些会导致不必要往返的重复方法调用而言。不过，它不必知道
 * 自己是<i>实际正在</i>针对远程服务还是本地实现工作，也不必知道底层使用哪种 remoting 协议。
 *
 * @author Juergen Hoeller
 * @since 14.05.2003
 */
public class RemoteAccessException extends NestedRuntimeException {

	/** 使用 Spring 1.2 中的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = -4906825139312227864L;


	/**
	 * RemoteAccessException 的构造器。
	 * @param msg the detail message
	 */
	public RemoteAccessException(String msg) {
		super(msg);
	}

	/**
	 * RemoteAccessException 的构造器。
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * remoting API such as RMI)
	 */
	public RemoteAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
