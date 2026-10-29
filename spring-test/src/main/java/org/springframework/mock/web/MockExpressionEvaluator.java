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

package org.springframework.mock.web;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;

/**
 * JSP 2.0 {@link javax.servlet.jsp.el.ExpressionEvaluator} 接口的 mock 实现，
 * 委托给 Apache JSTL 的 {@link ExpressionEvaluatorManager}。
 * 仅在测试自定义 JSP 标签时需要用于测试应用程序。
 *
 * <p>请注意，要使用此表达式求值器，类路径上必须存在
 * Apache JSTL 实现（jstl.jar、standard.jar）。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager
 */
@SuppressWarnings("deprecation")
public class MockExpressionEvaluator extends javax.servlet.jsp.el.ExpressionEvaluator {

	private final PageContext pageContext;


	/**
	 * 为给定 PageContext 创建一个新的 MockExpressionEvaluator。
	 * @param pageContext the JSP PageContext to run in
	 */
	public MockExpressionEvaluator(PageContext pageContext) {
		this.pageContext = pageContext;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public javax.servlet.jsp.el.Expression parseExpression(final String expression, final Class expectedType,
			final javax.servlet.jsp.el.FunctionMapper functionMapper) throws javax.servlet.jsp.el.ELException {

		return new javax.servlet.jsp.el.Expression() {
			@Override
			public Object evaluate(javax.servlet.jsp.el.VariableResolver variableResolver) throws javax.servlet.jsp.el.ELException {
				return doEvaluate(expression, expectedType, functionMapper);
			}
		};
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(String expression, Class expectedType, javax.servlet.jsp.el.VariableResolver variableResolver,
			javax.servlet.jsp.el.FunctionMapper functionMapper) throws javax.servlet.jsp.el.ELException {

		return doEvaluate(expression, expectedType, functionMapper);
	}

	@SuppressWarnings("rawtypes")
	protected Object doEvaluate(String expression, Class expectedType, javax.servlet.jsp.el.FunctionMapper functionMapper)
			throws javax.servlet.jsp.el.ELException {

		try {
			return ExpressionEvaluatorManager.evaluate("JSP EL expression", expression, expectedType, this.pageContext);
		}
		catch (JspException ex) {
			throw new javax.servlet.jsp.el.ELException("Parsing of JSP EL expression \"" + expression + "\" failed", ex);
		}
	}

}
