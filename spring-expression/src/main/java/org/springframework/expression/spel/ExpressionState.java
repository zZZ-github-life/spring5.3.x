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

package org.springframework.expression.spel;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * ExpressionState 用于维护每次表达式求值过程中的状态：对它的任何更改都不会被其他表达式看到，但它提
 * 供了一个位置来保存局部变量，并让复合表达式中的组件表达式能够传递状态。这与 EvaluationContext 形
 * 成对比，后者在多次表达式求值之间共享，对它的任何更改都会被其他表达式或任何选择查询该上下文的代码
 * 看到。
 *
 * <p>它还充当定义各种 AST 节点可能需要的通用工具例程的位置。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class ExpressionState {

	private final EvaluationContext relatedContext;

	private final TypedValue rootObject;

	private final SpelParserConfiguration configuration;

	@Nullable
	private Deque<TypedValue> contextObjects;

	@Nullable
	private Deque<VariableScope> variableScopes;

	// When entering a new scope there is a new base object which should be used
	// for '#this' references (or to act as a target for unqualified references).
	// This ArrayDeque captures those objects at each nested scope level.
	// For example:
	// #list1.?[#list2.contains(#this)]
	// On entering the selection we enter a new scope, and #this is now the
	// element from list1
	@Nullable
	private ArrayDeque<TypedValue> scopeRootObjects;


	public ExpressionState(EvaluationContext context) {
		this(context, context.getRootObject(), new SpelParserConfiguration(false, false));
	}

	public ExpressionState(EvaluationContext context, SpelParserConfiguration configuration) {
		this(context, context.getRootObject(), configuration);
	}

	public ExpressionState(EvaluationContext context, TypedValue rootObject) {
		this(context, rootObject, new SpelParserConfiguration(false, false));
	}

	public ExpressionState(EvaluationContext context, TypedValue rootObject, SpelParserConfiguration configuration) {
		Assert.notNull(context, "EvaluationContext must not be null");
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.relatedContext = context;
		this.rootObject = rootObject;
		this.configuration = configuration;
	}


	/**
	 * 活动上下文对象是解析属性等非限定引用时所依据的对象。
	 */
	public TypedValue getActiveContextObject() {
		if (CollectionUtils.isEmpty(this.contextObjects)) {
			return this.rootObject;
		}
		return this.contextObjects.element();
	}

	public void pushActiveContextObject(TypedValue obj) {
		if (this.contextObjects == null) {
			this.contextObjects = new ArrayDeque<>();
		}
		this.contextObjects.push(obj);
	}

	public void popActiveContextObject() {
		if (this.contextObjects == null) {
			this.contextObjects = new ArrayDeque<>();
		}
		try {
			this.contextObjects.pop();
		}
		catch (NoSuchElementException ex) {
			throw new IllegalStateException("Cannot pop active context object: stack is empty");
		}
	}

	public TypedValue getRootContextObject() {
		return this.rootObject;
	}

	public TypedValue getScopeRootContextObject() {
		if (CollectionUtils.isEmpty(this.scopeRootObjects)) {
			return this.rootObject;
		}
		return this.scopeRootObjects.element();
	}

	/**
	 * 将指定 {@link Supplier} 创建的值赋给求值上下文中的命名变量。<p>与
	 * {@link #setVariable(String, Object)} 不同，此方法只应在支持表达式内赋值时调用。
	 * @param name 要赋值的变量名称
	 * @param valueSupplier 提供要赋给该变量的值的 Supplier
	 * @return 包装所赋值的 {@link TypedValue}
	 * @since 5.2.24
	 * @see EvaluationContext#assignVariable(String, Supplier)
	 */
	public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
		return this.relatedContext.assignVariable(name, valueSupplier);
	}

	/**
	 * 将求值上下文中的命名变量设置为指定值。<p>与 {@link #assignVariable(String, Supplier)} 不同，此方
	 * 法只应以编程方式调用。
	 * @param name 要设置的变量名称
	 * @param value 要放入该变量的值
	 * @see EvaluationContext#setVariable(String, Object)
	 */
	public void setVariable(String name, @Nullable Object value) {
		this.relatedContext.setVariable(name, value);
	}

	public TypedValue lookupVariable(String name) {
		Object value = this.relatedContext.lookupVariable(name);
		return (value != null ? new TypedValue(value) : TypedValue.NULL);
	}

	public TypeComparator getTypeComparator() {
		return this.relatedContext.getTypeComparator();
	}

	public Class<?> findType(String type) throws EvaluationException {
		return this.relatedContext.getTypeLocator().findType(type);
	}

	public Object convertValue(Object value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
		Object result = this.relatedContext.getTypeConverter().convertValue(
				value, TypeDescriptor.forObject(value), targetTypeDescriptor);
		if (result == null) {
			throw new IllegalStateException("Null conversion result for value [" + value + "]");
		}
		return result;
	}

	public TypeConverter getTypeConverter() {
		return this.relatedContext.getTypeConverter();
	}

	@Nullable
	public Object convertValue(TypedValue value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
		Object val = value.getValue();
		return this.relatedContext.getTypeConverter().convertValue(
				val, TypeDescriptor.forObject(val), targetTypeDescriptor);
	}

	/*
	 * A new scope is entered when a function is invoked.
	 */
	public void enterScope(Map<String, Object> argMap) {
		initVariableScopes().push(new VariableScope(argMap));
		initScopeRootObjects().push(getActiveContextObject());
	}

	public void enterScope() {
		initVariableScopes().push(new VariableScope(Collections.emptyMap()));
		initScopeRootObjects().push(getActiveContextObject());
	}

	public void enterScope(String name, Object value) {
		initVariableScopes().push(new VariableScope(name, value));
		initScopeRootObjects().push(getActiveContextObject());
	}

	public void exitScope() {
		initVariableScopes().pop();
		initScopeRootObjects().pop();
	}

	public void setLocalVariable(String name, Object value) {
		initVariableScopes().element().setVariable(name, value);
	}

	@Nullable
	public Object lookupLocalVariable(String name) {
		for (VariableScope scope : initVariableScopes()) {
			if (scope.definesVariable(name)) {
				return scope.lookupVariable(name);
			}
		}
		return null;
	}

	private Deque<VariableScope> initVariableScopes() {
		if (this.variableScopes == null) {
			this.variableScopes = new ArrayDeque<>();
			// top-level empty variable scope
			this.variableScopes.add(new VariableScope());
		}
		return this.variableScopes;
	}

	private Deque<TypedValue> initScopeRootObjects() {
		if (this.scopeRootObjects == null) {
			this.scopeRootObjects = new ArrayDeque<>();
		}
		return this.scopeRootObjects;
	}

	public TypedValue operate(Operation op, @Nullable Object left, @Nullable Object right) throws EvaluationException {
		OperatorOverloader overloader = this.relatedContext.getOperatorOverloader();
		if (overloader.overridesOperation(op, left, right)) {
			Object returnValue = overloader.operate(op, left, right);
			return new TypedValue(returnValue);
		}
		else {
			String leftType = (left == null ? "null" : left.getClass().getName());
			String rightType = (right == null? "null" : right.getClass().getName());
			throw new SpelEvaluationException(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES, op, leftType, rightType);
		}
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return this.relatedContext.getPropertyAccessors();
	}

	public EvaluationContext getEvaluationContext() {
		return this.relatedContext;
	}

	public SpelParserConfiguration getConfiguration() {
		return this.configuration;
	}


	/**
	 * 调用函数时会进入新的作用域，用于保存函数调用的参数。如果参数名称与更高层作用域中的名称冲突，则在
	 * 函数执行期间将无法访问更高层作用域中的那些名称。函数返回时，退出该作用域。
	 */
	private static class VariableScope {

		private final Map<String, Object> vars = new HashMap<>();

		public VariableScope() {
		}

		public VariableScope(@Nullable Map<String, Object> arguments) {
			if (arguments != null) {
				this.vars.putAll(arguments);
			}
		}

		public VariableScope(String name, Object value) {
			this.vars.put(name,value);
		}

		public Object lookupVariable(String name) {
			return this.vars.get(name);
		}

		public void setVariable(String name, Object value) {
			this.vars.put(name,value);
		}

		public boolean definesVariable(String name) {
			return this.vars.containsKey(name);
		}
	}

}
