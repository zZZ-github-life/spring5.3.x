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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 解析后的 Spring Expression Language 格式表达式中所有 AST 节点的公共超类型。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class SpelNodeImpl implements SpelNode, Opcodes {

	private static final SpelNodeImpl[] NO_CHILDREN = new SpelNodeImpl[0];


	private final int startPos;

	private final int endPos;

	protected SpelNodeImpl[] children = SpelNodeImpl.NO_CHILDREN;

	@Nullable
	private SpelNodeImpl parent;

	/**
	 * 指示此表达式节点结果的类型描述符。它在已知时即被设置。对于字面量节点，它立即可知。对于属性访问或
	 * 方法调用，它在对该节点进行一次求值后才可知。
	 * <p>该描述符类似于字节码形式，但更易于使用。它不包含末尾的分号（对于非数组引用类型）。一些示例：
	 * Ljava/lang/String、I、[I
	 */
	@Nullable
	protected volatile String exitTypeDescriptor;


	public SpelNodeImpl(int startPos, int endPos, SpelNodeImpl... operands) {
		this.startPos = startPos;
		this.endPos = endPos;
		if (!ObjectUtils.isEmpty(operands)) {
			this.children = operands;
			for (SpelNodeImpl operand : operands) {
				Assert.notNull(operand, "Operand must not be null");
				operand.parent = this;
			}
		}
	}


	/**
	 * 如果下一个子节点是所列出的类之一，则返回 {@code true}。
	 */
	protected boolean nextChildIs(Class<?>... classes) {
		if (this.parent != null) {
			SpelNodeImpl[] peers = this.parent.children;
			for (int i = 0, max = peers.length; i < max; i++) {
				if (this == peers[i]) {
					if (i + 1 >= max) {
						return false;
					}
					Class<?> peerClass = peers[i + 1].getClass();
					for (Class<?> desiredClass : classes) {
						if (peerClass == desiredClass) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	@Override
	@Nullable
	public final Object getValue(ExpressionState expressionState) throws EvaluationException {
		return getValueInternal(expressionState).getValue();
	}

	@Override
	public final TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException {
		return getValueInternal(expressionState);
	}

	// by default Ast nodes are not writable
	@Override
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException {
		setValueInternal(expressionState, () -> new TypedValue(newValue));
	}

	/**
	 * 对表达式求值得到一个节点，然后将指定的 {@link Supplier} 创建的新值设置到该节点上。
	 * <p>例如，如果表达式求值结果为属性引用，那么该属性将被设置为新值。
	 * <p>当值应当延迟计算时，优先使用此方法而不是 {@link #setValue(ExpressionState, Object)}。
	 * <p>默认情况下，此方法抛出 {@link SpelEvaluationException}，实际上禁用了此特性。子类可以重写此方
	 * 法以提供实际的实现。
	 * @param expressionState 当前的表达式状态（包含上下文）
	 * @param valueSupplier 新值的 Supplier
	 * @throws EvaluationException 如果对表达式求值或设置新值时出现任何问题
	 * @since 5.2.24
	 */
	public TypedValue setValueInternal(ExpressionState expressionState, Supplier<TypedValue> valueSupplier)
			throws EvaluationException {

		throw new SpelEvaluationException(getStartPosition(), SpelMessage.SETVALUE_NOT_SUPPORTED, getClass());
	}

	@Override
	public SpelNode getChild(int index) {
		return this.children[index];
	}

	@Override
	public int getChildCount() {
		return this.children.length;
	}

	@Override
	@Nullable
	public Class<?> getObjectClass(@Nullable Object obj) {
		if (obj == null) {
			return null;
		}
		return (obj instanceof Class ? ((Class<?>) obj) : obj.getClass());
	}

	@Override
	public int getStartPosition() {
		return this.startPos;
	}

	@Override
	public int getEndPosition() {
		return this.endPos;
	}

	/**
	 * 检查节点能否编译为字节码。各节点的判断理由可能不同，但通常涉及检查节点的出口类型描述符是否已知，
	 * 以及所有相关子节点是否可编译。
	 * @return 如果此节点可编译为字节码则返回 {@code true}
	 */
	public boolean isCompilable() {
		return false;
	}

	/**
	 * 为此节点生成字节码并写入所提供的 visitor。有关当前正在编译的表达式的上下文信息可在 codeflow 对象
	 * 中获取，例如包括当前位于栈上的对象类型的信息。
	 * @param mv 生成代码的目标 ASM MethodVisitor
	 * @param cf 包含栈上内容信息的上下文对象
	 */
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		throw new IllegalStateException(getClass().getName() +" has no generateCode(..) method");
	}

	@Nullable
	public String getExitDescriptor() {
		return this.exitTypeDescriptor;
	}

	@Nullable
	protected final <T> T getValue(ExpressionState state, Class<T> desiredReturnType) throws EvaluationException {
		return ExpressionUtils.convertTypedValue(state.getEvaluationContext(), getValueInternal(state), desiredReturnType);
	}

	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		throw new SpelEvaluationException(getStartPosition(), SpelMessage.NOT_ASSIGNABLE, toStringAST());
	}

	public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;


	/**
	 * 生成用于构建指定方法的参数值的代码。此方法会考虑被调用的方法是否为可变参数方法，如果是，则参数值
	 * 会被适当地打包为数组。
	 * @param mv 生成代码的方法访问器
	 * @param cf 当前的代码流
	 * @param member 正在为其准备参数的方法或构造器
	 * @param arguments 表达式提供的参数值对应的表达式节点
	 */
	protected static void generateCodeForArguments(MethodVisitor mv, CodeFlow cf, Member member, SpelNodeImpl[] arguments) {
		String[] paramDescriptors = null;
		boolean isVarargs = false;
		if (member instanceof Constructor) {
			Constructor<?> ctor = (Constructor<?>) member;
			paramDescriptors = CodeFlow.toDescriptors(ctor.getParameterTypes());
			isVarargs = ctor.isVarArgs();
		}
		else { // Method
			Method method = (Method)member;
			paramDescriptors = CodeFlow.toDescriptors(method.getParameterTypes());
			isVarargs = method.isVarArgs();
		}
		if (isVarargs) {
			// The final parameter may or may not need packaging into an array, or nothing may
			// have been passed to satisfy the varargs and so something needs to be built.
			int p = 0; // Current supplied argument being processed
			int childCount = arguments.length;

			// Fulfill all the parameter requirements except the last one
			for (p = 0; p < paramDescriptors.length - 1; p++) {
				generateCodeForArgument(mv, cf, arguments[p], paramDescriptors[p]);
			}

			SpelNodeImpl lastChild = (childCount == 0 ? null : arguments[childCount - 1]);
			String arrayType = paramDescriptors[paramDescriptors.length - 1];
			// Determine if the final passed argument is already suitably packaged in array
			// form to be passed to the method
			if (lastChild != null && arrayType.equals(lastChild.getExitDescriptor())) {
				generateCodeForArgument(mv, cf, lastChild, paramDescriptors[p]);
			}
			else {
				arrayType = arrayType.substring(1); // trim the leading '[', may leave other '['
				// build array big enough to hold remaining arguments
				CodeFlow.insertNewArrayCode(mv, childCount - p, arrayType);
				// Package up the remaining arguments into the array
				int arrayindex = 0;
				while (p < childCount) {
					SpelNodeImpl child = arguments[p];
					mv.visitInsn(DUP);
					CodeFlow.insertOptimalLoad(mv, arrayindex++);
					generateCodeForArgument(mv, cf, child, arrayType);
					CodeFlow.insertArrayStore(mv, arrayType);
					p++;
				}
			}
		}
		else {
			for (int i = 0; i < paramDescriptors.length;i++) {
				generateCodeForArgument(mv, cf, arguments[i], paramDescriptors[i]);
			}
		}
	}

	/**
	 * 请求参数生成其字节码，然后追加相应的装箱/拆箱/检查转换，以确保其与期望的参数描述符匹配。
	 */
	protected static void generateCodeForArgument(MethodVisitor mv, CodeFlow cf, SpelNodeImpl argument, String paramDesc) {
		cf.enterCompilationScope();
		argument.generateCode(mv, cf);
		String lastDesc = cf.lastDescriptor();
		Assert.state(lastDesc != null, "No last descriptor");
		boolean primitiveOnStack = CodeFlow.isPrimitive(lastDesc);
		// Check if need to box it for the method reference?
		if (primitiveOnStack && paramDesc.charAt(0) == 'L') {
			CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
		}
		else if (paramDesc.length() == 1 && !primitiveOnStack) {
			CodeFlow.insertUnboxInsns(mv, paramDesc.charAt(0), lastDesc);
		}
		else if (!paramDesc.equals(lastDesc)) {
			// This would be unnecessary in the case of subtyping (e.g. method takes Number but Integer passed in)
			CodeFlow.insertCheckCast(mv, paramDesc);
		}
		cf.exitCompilationScope();
	}

}
