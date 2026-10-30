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

package org.springframework.context.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanExpressionResolver} 接口的标准实现，使用
 * Spring 的表达式模块解析和求值 Spring EL。
 *
 * <p>所在 {@code BeanFactory} 中的所有 Bean 都会以其常见 Bean 名称作为预定义变量提供，包括
 * "environment"、"systemProperties" 和 "systemEnvironment" 等标准上下文 Bean。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see BeanExpressionContext#getBeanFactory()
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see org.springframework.expression.spel.support.StandardEvaluationContext
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

	/** 默认表达式前缀："#{"。 */
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/** 默认表达式后缀："}"。 */
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

	private ExpressionParser expressionParser;

	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256);

	private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);

	private final ParserContext beanExpressionParserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}
		@Override
		public String getExpressionPrefix() {
			return expressionPrefix;
		}
		@Override
		public String getExpressionSuffix() {
			return expressionSuffix;
		}
	};


	/**
	 * 使用默认设置创建一个新的 {@code StandardBeanExpressionResolver}。
	 */
	public StandardBeanExpressionResolver() {
		this.expressionParser = new SpelExpressionParser();
	}

	/**
	 * 使用给定的 Bean 类加载器创建一个新的 {@code StandardBeanExpressionResolver}，并以它作为表达式编
	 * 译的基础。
	 * @param beanClassLoader 工厂的 Bean 类加载器
	 */
	public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
		this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
	}


	/**
	 * 设置表达式字符串的起始前缀。默认值为 "#{"。
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * 设置表达式字符串的结束后缀。默认值为 "}"。
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}

	/**
	 * 指定用于表达式解析的 EL 解析器。<p>默认是一个
	 * {@link org.springframework.expression.spel.standard.SpelExpressionParser}，兼容标准的 Unified EL
	 * 风格表达式语法。
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	@Override
	@Nullable
	public Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException {
		if (!StringUtils.hasLength(value)) {
			return value;
		}
		try {
			Expression expr = this.expressionCache.get(value);
			if (expr == null) {
				expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
				this.expressionCache.put(value, expr);
			}
			StandardEvaluationContext sec = this.evaluationCache.get(beanExpressionContext);
			if (sec == null) {
				sec = new StandardEvaluationContext(beanExpressionContext);
				sec.addPropertyAccessor(new BeanExpressionContextAccessor());
				sec.addPropertyAccessor(new BeanFactoryAccessor());
				sec.addPropertyAccessor(new MapAccessor());
				sec.addPropertyAccessor(new EnvironmentAccessor());
				sec.setBeanResolver(new BeanFactoryResolver(beanExpressionContext.getBeanFactory()));
				sec.setTypeLocator(new StandardTypeLocator(beanExpressionContext.getBeanFactory().getBeanClassLoader()));
				sec.setTypeConverter(new StandardTypeConverter(() -> {
					ConversionService cs = beanExpressionContext.getBeanFactory().getConversionService();
					return (cs != null ? cs : DefaultConversionService.getSharedInstance());
				}));
				customizeEvaluationContext(sec);
				this.evaluationCache.put(beanExpressionContext, sec);
			}
			return expr.getValue(sec);
		}
		catch (Throwable ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * 用于自定义表达式求值上下文的模板方法。<p>默认实现为空。
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

}
