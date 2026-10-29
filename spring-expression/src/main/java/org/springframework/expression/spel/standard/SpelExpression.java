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

package org.springframework.expression.spel.standard;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个 {@code SpelExpression} 表示已解析（有效）的表达式，可在指定的上下文中求值。表达式可以独立求
 * 值，也可以在指定的上下文中求值。在表达式求值期间，可能会要求上下文解析对类型、Bean、属性和方法的
 * 引用。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class SpelExpression implements Expression {

	// Number of times to interpret an expression before compiling it
	private static final int INTERPRETED_COUNT_THRESHOLD = 100;

	// Number of times to try compiling an expression before giving up
	private static final int FAILED_ATTEMPTS_THRESHOLD = 100;


	private final String expression;

	private final SpelNodeImpl ast;

	private final SpelParserConfiguration configuration;

	// The default context is used if no override is supplied by the user
	@Nullable
	private EvaluationContext evaluationContext;

	// Holds the compiled form of the expression (if it has been compiled)
	@Nullable
	private volatile CompiledExpression compiledAst;

	// Count of many times as the expression been interpreted - can trigger compilation
	// when certain limit reached
	private final AtomicInteger interpretedCount = new AtomicInteger();

	// The number of times compilation was attempted and failed - enables us to eventually
	// give up trying to compile it when it just doesn't seem to be possible.
	private final AtomicInteger failedAttempts = new AtomicInteger();


	/**
	 * 构造一个表达式，仅供解析器使用。
	 */
	public SpelExpression(String expression, SpelNodeImpl ast, SpelParserConfiguration configuration) {
		this.expression = expression;
		this.ast = ast;
		this.configuration = configuration;
	}


	/**
	 * 设置求值调用中未指定时使用的求值上下文。
	 * @param evaluationContext the evaluation context to use
	 */
	public void setEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	/**
	 * 返回求值调用中未提供时使用的默认求值上下文。
	 * @return the default evaluation context
	 */
	public EvaluationContext getEvaluationContext() {
		if (this.evaluationContext == null) {
			this.evaluationContext = new StandardEvaluationContext();
		}
		return this.evaluationContext;
	}


	// implementing Expression

	@Override
	public String getExpressionString() {
		return this.expression;
	}

	@Override
	@Nullable
	public Object getValue() throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				EvaluationContext context = getEvaluationContext();
				return compiledAst.getValue(context.getRootObject().getValue(), context);
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getValue(@Nullable Class<T> expectedResultType) throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				EvaluationContext context = getEvaluationContext();
				Object result = compiledAst.getValue(context.getRootObject().getValue(), context);
				if (expectedResultType == null) {
					return (T) result;
				}
				else {
					return ExpressionUtils.convertTypedValue(
							getEvaluationContext(), new TypedValue(result), expectedResultType);
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(
				expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	@Nullable
	public Object getValue(@Nullable Object rootObject) throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				return compiledAst.getValue(rootObject, getEvaluationContext());
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> expectedResultType) throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				Object result = compiledAst.getValue(rootObject, getEvaluationContext());
				if (expectedResultType == null) {
					return (T)result;
				}
				else {
					return ExpressionUtils.convertTypedValue(
							getEvaluationContext(), new TypedValue(result), expectedResultType);
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(
				expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	@Nullable
	public Object getValue(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				return compiledAst.getValue(context.getRootObject().getValue(), context);
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				Object result = compiledAst.getValue(context.getRootObject().getValue(), context);
				if (expectedResultType != null) {
					return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
				}
				else {
					return (T) result;
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	@Nullable
	public Object getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				return compiledAst.getValue(rootObject, context);
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> expectedResultType)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext is required");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				Object result = compiledAst.getValue(rootObject, context);
				if (expectedResultType != null) {
					return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
				}
				else {
					return (T) result;
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	@Nullable
	public Class<?> getValueType() throws EvaluationException {
		return getValueType(getEvaluationContext());
	}

	@Override
	@Nullable
	public Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException {
		return getValueType(getEvaluationContext(), rootObject);
	}

	@Override
	@Nullable
	public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
		return (typeDescriptor != null ? typeDescriptor.getType() : null);
	}

	@Override
	@Nullable
	public Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
		return (typeDescriptor != null ? typeDescriptor.getType() : null);
	}

	@Override
	@Nullable
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return getValueTypeDescriptor(getEvaluationContext());
	}

	@Override
	@Nullable
	public TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	@Nullable
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	@Nullable
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext is required");
		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
		return this.ast.isWritable(
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		return this.ast.isWritable(new ExpressionState(context, this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		return this.ast.isWritable(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
	}

	@Override
	public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		this.ast.setValue(
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		this.ast.setValue(new ExpressionState(context, this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext is required");
		this.ast.setValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration), value);
	}


	/**
	 * 如果表达式求值次数超过触发编译的阈值，则编译该表达式。
	 * @param expressionState the expression state used to determine compilation mode
	 */
	private void checkCompile(ExpressionState expressionState) {
		this.interpretedCount.incrementAndGet();
		SpelCompilerMode compilerMode = expressionState.getConfiguration().getCompilerMode();
		if (compilerMode != SpelCompilerMode.OFF) {
			if (compilerMode == SpelCompilerMode.IMMEDIATE) {
				if (this.interpretedCount.get() > 1) {
					compileExpression();
				}
			}
			else {
				// compilerMode = SpelCompilerMode.MIXED
				if (this.interpretedCount.get() > INTERPRETED_COUNT_THRESHOLD) {
					compileExpression();
				}
			}
		}
	}

	/**
	 * 执行表达式编译。仅当所有节点的出口类型描述符都已确定时才会成功。如果编译失败且失败次数超过 100
	 * 次，则该表达式将不再被视为适合编译。
	 * @return whether this expression has been successfully compiled
	 */
	public boolean compileExpression() {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			// Previously compiled
			return true;
		}
		if (this.failedAttempts.get() > FAILED_ATTEMPTS_THRESHOLD) {
			// Don't try again
			return false;
		}

		synchronized (this) {
			if (this.compiledAst != null) {
				// Compiled by another thread before this thread got into the sync block
				return true;
			}
			try {
				SpelCompiler compiler = SpelCompiler.getCompiler(this.configuration.getCompilerClassLoader());
				compiledAst = compiler.compile(this.ast);
				if (compiledAst != null) {
					// Successfully compiled
					this.compiledAst = compiledAst;
					return true;
				}
				else {
					// Failed to compile
					this.failedAttempts.incrementAndGet();
					return false;
				}
			}
			catch (Exception ex) {
				// Failed to compile
				this.failedAttempts.incrementAndGet();

				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
					return false;
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_COMPILING_EXPRESSION);
				}
			}
		}
	}

	/**
	 * 如果表达式此前一直使用编译形式，则使其回退为解释执行。它还会重置编译尝试失败计数（通常如果表达式
	 * 在 100 次尝试后仍无法编译，就不再被视为可编译）。
	 */
	public void revertToInterpreted() {
		this.compiledAst = null;
		this.interpretedCount.set(0);
		this.failedAttempts.set(0);
	}

	/**
	 * 返回该表达式的抽象语法树。
	 */
	public SpelNode getAST() {
		return this.ast;
	}

	/**
	 * 生成该表达式抽象语法树的字符串表示。理想情况下它应看起来像输入的表达式，但要格式良好，因为在解析
	 * 表达式期间会丢弃任何不必要的空白。
	 * @return the string representation of the AST
	 */
	public String toStringAST() {
		return this.ast.toStringAST();
	}

	private TypedValue toTypedValue(@Nullable Object object) {
		return (object != null ? new TypedValue(object) : TypedValue.NULL);
	}

}
