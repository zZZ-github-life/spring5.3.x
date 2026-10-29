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

package org.springframework.web.servlet.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * {@code <eval>} 标签会求值一个 Spring 表达式（SpEL），并打印结果
 * 或将其赋值给变量。它支持由隐式变量和作用域属性构成的标准
 * JSP 求值上下文。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需？</th>
 * <th>是否支持运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>expression</td>
 * <td>true</td>
 * <td>true</td>
 * <td>要执行的求值表达式。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 HTML 转义。
 * 覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 JavaScript 转义。
 * 默认为 {@code false}。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>var 的作用域。支持 'application'、'session'、'request' 和 'page'
 * 作用域。默认为 page 作用域。除非同时定义了 var 属性，
 * 否则此属性无效。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>用于导出求值结果的变量名称。
 * 如果未指定，求值结果将被转换为 String 并作为输出
 * 写出。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0.1
 */
@SuppressWarnings("serial")
public class EvalTag extends HtmlEscapingAwareTag {

	/**
	 * 页面级 {@link EvaluationContext} 实例所对应的
	 * {@link javax.servlet.jsp.PageContext} 属性。
	 */
	private static final String EVALUATION_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.EVALUATION_CONTEXT";


	private final ExpressionParser expressionParser = new SpelExpressionParser();

	@Nullable
	private Expression expression;

	@Nullable
	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	/**
	 * 设置要执行的求值表达式。
	 */
	public void setExpression(String expression) {
		this.expression = this.expressionParser.parseExpression(expression);
	}

	/**
	 * 设置用于公开求值结果的变量名称。
	 * 默认将结果渲染到当前的 JspWriter。
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置用于导出求值结果的作用域。
	 * 除非同时定义了 var，否则此属性没有意义。
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * 以布尔值形式为该标签设置 JavaScript 转义。
	 * 默认为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	public int doStartTagInternal() throws JspException {
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		EvaluationContext evaluationContext =
				(EvaluationContext) this.pageContext.getAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE);
		if (evaluationContext == null) {
			evaluationContext = createEvaluationContext(this.pageContext);
			this.pageContext.setAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE, evaluationContext);
		}
		if (this.var != null) {
			Object result = (this.expression != null ? this.expression.getValue(evaluationContext) : null);
			this.pageContext.setAttribute(this.var, result, this.scope);
		}
		else {
			try {
				String result = (this.expression != null ?
						this.expression.getValue(evaluationContext, String.class) : null);
				result = ObjectUtils.getDisplayString(result);
				result = htmlEscape(result);
				result = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(result) : result);
				this.pageContext.getOut().print(result);
			}
			catch (IOException ex) {
				throw new JspException(ex);
			}
		}
		return EVAL_PAGE;
	}

	private EvaluationContext createEvaluationContext(PageContext pageContext) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new JspPropertyAccessor(pageContext));
		context.addPropertyAccessor(new MapAccessor());
		context.addPropertyAccessor(new EnvironmentAccessor());
		context.setBeanResolver(new BeanFactoryResolver(getRequestContext().getWebApplicationContext()));
		ConversionService conversionService = getConversionService(pageContext);
		if (conversionService != null) {
			context.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		return context;
	}

	@Nullable
	private ConversionService getConversionService(PageContext pageContext) {
		return (ConversionService) pageContext.getRequest().getAttribute(ConversionService.class.getName());
	}


	@SuppressWarnings("deprecation")
	private static class JspPropertyAccessor implements PropertyAccessor {

		private final PageContext pageContext;

		@Nullable
		private final javax.servlet.jsp.el.VariableResolver variableResolver;

		public JspPropertyAccessor(PageContext pageContext) {
			this.pageContext = pageContext;
			this.variableResolver = pageContext.getVariableResolver();
		}

		@Override
		@Nullable
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			return (target == null &&
					(resolveImplicitVariable(name) != null || this.pageContext.findAttribute(name) != null));
		}

		@Override
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			Object implicitVar = resolveImplicitVariable(name);
			if (implicitVar != null) {
				return new TypedValue(implicitVar);
			}
			return new TypedValue(this.pageContext.findAttribute(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
			throw new UnsupportedOperationException();
		}

		@Nullable
		private Object resolveImplicitVariable(String name) throws AccessException {
			if (this.variableResolver == null) {
				return null;
			}
			try {
				return this.variableResolver.resolveVariable(name);
			}
			catch (Exception ex) {
				throw new AccessException(
						"Unexpected exception occurred accessing '" + name + "' as an implicit variable", ex);
			}
		}
	}

}
