/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 函数引用的形式为 "#someFunction(a,b,c)"。函数可以在表达式求值之前就在上下文中定义。函数也可以是
 * 静态 Java 方法，在表达式调用之前注册到上下文中。
 *
 * <p>函数非常简单。（目前）参数并不属于定义的一部分，因此名称必须唯一。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FunctionReference extends SpelNodeImpl {

	private final String name;

	// Captures the most recently used method for the function invocation *if* the method
	// can safely be used for compilation (i.e. no argument conversion is going on)
	@Nullable
	private volatile Method method;


	public FunctionReference(String functionName, int startPos, int endPos, SpelNodeImpl... arguments) {
		super(startPos, endPos, arguments);
		this.name = functionName;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue value = state.lookupVariable(this.name);
		if (value == TypedValue.NULL) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
		}
		if (!(value.getValue() instanceof Method)) {
			// Possibly a static Java method registered as a function
			throw new SpelEvaluationException(
					SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
		}

		try {
			return executeFunctionJLRMethod(state, (Method) value.getValue());
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getStartPosition());
			throw ex;
		}
	}

	/**
	 * 执行以 {@code java.lang.reflect.Method} 表示的函数。
	 * @param state 表达式求值状态
	 * @param method 要调用的方法
	 * @return 所调用 Java 方法的返回值
	 * @throws EvaluationException 如果调用该方法时出现任何问题
	 */
	private TypedValue executeFunctionJLRMethod(ExpressionState state, Method method) throws EvaluationException {
		Object[] functionArgs = getArguments(state);

		if (!method.isVarArgs()) {
			int declaredParamCount = method.getParameterCount();
			if (declaredParamCount != functionArgs.length) {
				throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
						functionArgs.length, declaredParamCount);
			}
		}
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.FUNCTION_MUST_BE_STATIC, ClassUtils.getQualifiedMethodName(method), this.name);
		}

		// Convert arguments if necessary and remap them for varargs if required
		TypeConverter converter = state.getEvaluationContext().getTypeConverter();
		boolean argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
		if (method.isVarArgs()) {
			functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
					method.getParameterTypes(), functionArgs);
		}
		boolean compilable = false;

		try {
			ReflectionUtils.makeAccessible(method);
			Object result = method.invoke(method.getClass(), functionArgs);
			compilable = !argumentConversionOccurred;
			return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
		}
		catch (Exception ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
					this.name, ex.getMessage());
		}
		finally {
			if (compilable) {
				this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
				this.method = method;
			}
			else {
				this.exitTypeDescriptor = null;
				this.method = null;
			}
		}
	}

	@Override
	public String toStringAST() {
		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (int i = 0; i < getChildCount(); i++) {
			sj.add(getChild(i).toStringAST());
		}
		return '#' + this.name + sj;
	}

	/**
	 * 计算函数的参数，它们是此表达式节点的子节点。
	 * @return 函数调用的参数值数组
	 */
	private Object[] getArguments(ExpressionState state) throws EvaluationException {
		// Compute arguments to the function
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = this.children[i].getValueInternal(state).getValue();
		}
		return arguments;
	}

	@Override
	public boolean isCompilable() {
		Method method = this.method;
		if (method == null) {
			return false;
		}
		int methodModifiers = method.getModifiers();
		if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
			return false;
		}
		for (SpelNodeImpl child : this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		Method method = this.method;
		Assert.state(method != null, "No method handle");
		String classDesc = method.getDeclaringClass().getName().replace('.', '/');
		generateCodeForArguments(mv, cf, method, this.children);
		mv.visitMethodInsn(INVOKESTATIC, classDesc, method.getName(),
				CodeFlow.createSignatureDescriptor(method), false);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
