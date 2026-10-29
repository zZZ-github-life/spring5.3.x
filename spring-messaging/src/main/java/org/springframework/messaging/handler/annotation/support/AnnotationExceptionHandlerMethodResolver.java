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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;

/**
 * {@link AbstractExceptionHandlerMethodResolver} 的子类，用于在给定类中查找
 * 使用 {@link MessageExceptionHandler} 注解的方法。实际的异常处理类型
 * 从注解中提取（如果存在），否则从方法签名中提取作为回退选项。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AnnotationExceptionHandlerMethodResolver extends AbstractExceptionHandlerMethodResolver {

	/**
	 * 在给定类型中查找 {@link MessageExceptionHandler} 方法的构造函数。
	 * @param handlerType the type to introspect
	 */
	public AnnotationExceptionHandlerMethodResolver(Class<?> handlerType) {
		super(initExceptionMappings(handlerType));
	}

	private static Map<Class<? extends Throwable>, Method> initExceptionMappings(Class<?> handlerType) {
		Map<Method, MessageExceptionHandler> methods = MethodIntrospector.selectMethods(handlerType,
				(MethodIntrospector.MetadataLookup<MessageExceptionHandler>) method ->
						AnnotatedElementUtils.findMergedAnnotation(method, MessageExceptionHandler.class));

		Map<Class<? extends Throwable>, Method> result = new HashMap<>();
		for (Map.Entry<Method, MessageExceptionHandler> entry : methods.entrySet()) {
			Method method = entry.getKey();
			List<Class<? extends Throwable>> exceptionTypes = new ArrayList<>(Arrays.asList(entry.getValue().value()));
			if (exceptionTypes.isEmpty()) {
				exceptionTypes.addAll(getExceptionsFromMethodSignature(method));
			}
			for (Class<? extends Throwable> exceptionType : exceptionTypes) {
				Method oldMethod = result.put(exceptionType, method);
				if (oldMethod != null && !oldMethod.equals(method)) {
					throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
							exceptionType + "]: {" + oldMethod + ", " + method + "}");
				}
			}
		}
		return result;
	}

}
