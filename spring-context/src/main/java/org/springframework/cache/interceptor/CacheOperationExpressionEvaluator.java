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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.Cache;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

/**
 * 处理 SpEL 表达式解析的工具类。旨在用作可重用、线程安全的组件。
 *
 * <p>出于性能原因，使用 {@link AnnotatedElementKey} 执行内部缓存。
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.1
 */
class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

	/**
	 * 指示没有结果变量。
	 */
	public static final Object NO_RESULT = new Object();

	/**
	 * 指示结果变量完全不可使用。
	 */
	public static final Object RESULT_UNAVAILABLE = new Object();

	/**
	 * 保存结果对象的变量名称。
	 */
	public static final String RESULT_VARIABLE = "result";


	private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

	private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<>(64);


	/**
	 * 创建一个 {@link EvaluationContext}。
	 * @param caches 当前缓存
	 * @param method 方法
	 * @param args 方法参数
	 * @param target 目标对象
	 * @param targetClass 目标类
	 * @param result 返回值（可以为 {@code null}），
	 * 如果此时没有返回值，则为 {@link #NO_RESULT}
	 * @return 求值上下文
	 */
	public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass, Method targetMethod,
			@Nullable Object result, @Nullable BeanFactory beanFactory) {

		CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
				caches, method, args, target, targetClass);
		CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
				rootObject, targetMethod, args, getParameterNameDiscoverer());
		if (result == RESULT_UNAVAILABLE) {
			evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
		}
		else if (result != NO_RESULT) {
			evaluationContext.setVariable(RESULT_VARIABLE, result);
		}
		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		return evaluationContext;
	}

	@Nullable
	public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
	}

	public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
				evalContext, Boolean.class)));
	}

	public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return (Boolean.TRUE.equals(getExpression(this.unlessCache, methodKey, unlessExpression).getValue(
				evalContext, Boolean.class)));
	}

	/**
	 * 清除所有缓存。
	 */
	void clear() {
		this.keyCache.clear();
		this.conditionCache.clear();
		this.unlessCache.clear();
	}

}
