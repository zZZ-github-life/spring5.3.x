/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import java.lang.reflect.Method;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.MethodInvocationInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * 用于对所选处理器或处理器方法进行断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#handler} 访问。
 *
 * <p><strong>注意：</strong>断言用于处理请求的控制器方法的预期
 * 仅对使用 {@link RequestMappingHandlerMapping} 和 {@link RequestMappingHandlerAdapter}
 * 处理的请求有效，Spring MVC Java 配置和 XML 命名空间默认使用它们。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class HandlerResultMatchers {

	/**
	 * 受保护的构造函数。
	 * 请使用 {@link MockMvcResultMatchers#handler()}。
	 */
	protected HandlerResultMatchers() {
	}


	/**
	 * 断言处理请求的处理器类型。
	 */
	public ResultMatcher handlerType(Class<?> type) {
		return result -> {
			Object handler = result.getHandler();
			assertNotNull("No handler", handler);
			Class<?> actual = handler.getClass();
			if (handler instanceof HandlerMethod) {
				actual = ((HandlerMethod) handler).getBeanType();
			}
			assertEquals("Handler type", type, ClassUtils.getUserClass(actual));
		};
	}

	/**
	 * 断言用于处理请求的控制器方法。
	 * <p>预期的方法通过“mock”控制器方法调用来指定，
	 * 类似于 {@link MvcUriComponentsBuilder#fromMethodCall(Object)}。
	 * <p>例如，给定以下控制器：
	 * <pre class="code">
	 * &#064;RestController
	 * public class SimpleController {
	 *
	 *     &#064;RequestMapping("/")
	 *     public ResponseEntity&lt;Void&gt; handle() {
	 *         return ResponseEntity.ok().build();
	 *     }
	 * }
	 * </pre>
	 * <p>静态导入了 {@link MvcUriComponentsBuilder#on} 的测试
	 * 可以按如下方式执行：
	 * <pre class="code">
	 * mockMvc.perform(get("/"))
	 *     .andExpect(handler().methodCall(on(SimpleController.class).handle()));
	 * </pre>
	 * @param obj either the value returned from a "mock" controller invocation
	 * or the "mock" controller itself after an invocation
	 */
	public ResultMatcher methodCall(Object obj) {
		return result -> {
			if (!(obj instanceof MethodInvocationInfo)) {
				fail(String.format("The supplied object [%s] is not an instance of %s. " +
						"Ensure that you invoke the handler method via MvcUriComponentsBuilder.on().",
						obj, MethodInvocationInfo.class.getName()));
			}
			MethodInvocationInfo invocationInfo = (MethodInvocationInfo) obj;
			Method expected = invocationInfo.getControllerMethod();
			Method actual = getHandlerMethod(result).getMethod();
			assertEquals("Handler method", expected, actual);
		};
	}

	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言用于处理请求的控制器方法名称。
	 */
	public ResultMatcher methodName(Matcher<? super String> matcher) {
		return result -> {
			HandlerMethod handlerMethod = getHandlerMethod(result);
			assertThat("Handler method", handlerMethod.getMethod().getName(), matcher);
		};
	}

	/**
	 * 断言用于处理请求的控制器方法名称。
	 */
	public ResultMatcher methodName(String name) {
		return result -> {
			HandlerMethod handlerMethod = getHandlerMethod(result);
			assertEquals("Handler method", name, handlerMethod.getMethod().getName());
		};
	}

	/**
	 * 断言用于处理请求的控制器方法。
	 */
	public ResultMatcher method(Method method) {
		return result -> {
			HandlerMethod handlerMethod = getHandlerMethod(result);
			assertEquals("Handler method", method, handlerMethod.getMethod());
		};
	}


	private static HandlerMethod getHandlerMethod(MvcResult result) {
		Object handler = result.getHandler();
		assertTrue("Not a HandlerMethod: " + handler, handler instanceof HandlerMethod);
		return (HandlerMethod) handler;
	}

}
