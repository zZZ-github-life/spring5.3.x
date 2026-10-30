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

package org.springframework.expression.spel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 管理编译过程所生成的类。
 *
 * <p>在生成字节码的过程中记录中间编译状态。还包括各种字节码生成辅助函数。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.1
 */
public class CodeFlow implements Opcodes {

	/**
	 * 所生成类的名称。通常用于生成访问所生成类型上新生成字段的代码。
	 */
	private final String className;

	/**
	 * 当前正在生成的类。
	 */
	private final ClassWriter classWriter;

	/**
	 * 记录字节码栈顶内容的类型（即前一个表达式组件的输出类型）。新作用域用于求值子表达式，例如方法调用
	 * 表达式中参数值对应的表达式。
	 */
	private final Deque<List<String>> compilationScopes;

	/**
	 * 当 SpEL ast 节点被调用来为主求值方法生成代码时，可以注册以向该类添加字段。任何已注册的
	 * FieldAdder 都会在生成完主求值函数后调用。
	 */
	@Nullable
	private List<FieldAdder> fieldAdders;

	/**
	 * 当 SpEL ast 节点被调用来为主求值方法生成代码时，可以注册以向类中的静态初始化器添加代码。任何已注
	 * 册的 ClinitAdder 都会在生成完主求值函数后调用。
	 */
	@Nullable
	private List<ClinitAdder> clinitAdders;

	/**
	 * 当代码生成需要在类级字段中保存值时，用它来跟踪下一个可用的字段 id（用作名称后缀）。
	 */
	private int nextFieldId = 1;

	/**
	 * 当代码生成需要方法内的中间变量时，此方法记录下一个可用变量（变量 0 是 'this'）。
	 */
	private int nextFreeVariableId = 1;


	/**
	 * 为给定类构造新的 {@code CodeFlow}。
	 * @param className 类名
	 * @param classWriter 对应的 ASM {@code ClassWriter}
	 */
	public CodeFlow(String className, ClassWriter classWriter) {
		this.className = className;
		this.classWriter = classWriter;
		this.compilationScopes = new ArrayDeque<>();
		this.compilationScopes.add(new ArrayList<String>());
	}


	/**
	 * 压入用于加载目标对象的字节码（即作为第一个参数传递给 CompiledExpression.getValue(target,
	 * context) 的内容）。
	 * @param mv 插入加载指令的目标访问器
	 */
	public void loadTarget(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD, 1);
	}

	/**
	 * 压入用于加载 EvaluationContext 的字节码（传递给编译表达式方法的第二个参数）。
	 * @param mv 插入加载指令的目标访问器
	 * @since 4.3.4
	 */
	public void loadEvaluationContext(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD, 2);
	}

	/**
	 * 记录最近求值的表达式元素的描述符。
	 * @param descriptor 最近求值元素的类型描述符
	 */
	public void pushDescriptor(@Nullable String descriptor) {
		if (descriptor != null) {
			this.compilationScopes.element().add(descriptor);
		}
	}

	/**
	 * 进入新的编译作用域，通常是因为有嵌套的表达式求值。例如在求值方法调用表达式的参数时，每个参数都会
	 * 在新的作用域中求值。
	 */
	public void enterCompilationScope() {
		this.compilationScopes.push(new ArrayList<>());
	}

	/**
	 * 退出编译作用域，通常是在嵌套表达式求值完成后。例如在求值完方法调用的某个参数后，此方法会将我们返
	 * 回到之前的（外层）作用域。
	 */
	public void exitCompilationScope() {
		this.compilationScopes.pop();
	}

	/**
	 * 返回当前位于栈顶（在当前作用域中）的项的说明符。
	 */
	@Nullable
	public String lastDescriptor() {
		return CollectionUtils.lastElement(this.compilationScopes.peek());
	}

	/**
	 * 如果 codeflow 显示最后一个表达式的求值结果为 java.lang.Boolean，则插入必要的指令将其拆箱为
	 * boolean 基本类型。
	 * @param mv 插入新指令的目标访问器
	 */
	public void unboxBooleanIfNecessary(MethodVisitor mv) {
		if ("Ljava/lang/Boolean".equals(lastDescriptor())) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
		}
	}

	/**
	 * 在生成完主表达式求值方法后调用，此方法会回调任何已注册的 FieldAdder 或 ClinitAdder，以便向表示已
	 * 编译表达式的类添加额外信息。
	 */
	public void finish() {
		if (this.fieldAdders != null) {
			for (FieldAdder fieldAdder : this.fieldAdders) {
				fieldAdder.generateField(this.classWriter, this);
			}
		}
		if (this.clinitAdders != null) {
			MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
			mv.visitCode();
			this.nextFreeVariableId = 0;  // to 0 because there is no 'this' in a clinit
			for (ClinitAdder clinitAdder : this.clinitAdders) {
				clinitAdder.generateCode(mv, this);
			}
			mv.visitInsn(RETURN);
			mv.visitMaxs(0,0);  // not supplied due to COMPUTE_MAXS
			mv.visitEnd();
		}
	}

	/**
	 * 注册一个 FieldAdder，它会在所生成的类中添加新字段，以支持 ast 节点主 generateCode() 方法生成的代
	 * 码。
	 */
	public void registerNewField(FieldAdder fieldAdder) {
		if (this.fieldAdders == null) {
			this.fieldAdders = new ArrayList<>();
		}
		this.fieldAdders.add(fieldAdder);
	}

	/**
	 * 注册一个 ClinitAdder，它会在所生成类的静态初始化器中添加代码，以支持 ast 节点主 generateCode()
	 * 方法生成的代码。
	 */
	public void registerNewClinit(ClinitAdder clinitAdder) {
		if (this.clinitAdders == null) {
			this.clinitAdders = new ArrayList<>();
		}
		this.clinitAdders.add(clinitAdder);
	}

	public int nextFieldId() {
		return this.nextFieldId++;
	}

	public int nextFreeVariableId() {
		return this.nextFreeVariableId++;
	}

	public String getClassName() {
		return this.className;
	}


	/**
	 * 插入任何必要的强制转换和取值调用，以便从装箱类型转换为基本类型值。
	 * @param mv 插入指令的目标方法访问器
	 * @param ch 期望输出的基本类型
	 * @param stackDescriptor 栈顶类型的描述符
	 */
	public static void insertUnboxInsns(MethodVisitor mv, char ch, @Nullable String stackDescriptor) {
		if (stackDescriptor == null) {
			return;
		}
		switch (ch) {
			case 'Z':
				if (!stackDescriptor.equals("Ljava/lang/Boolean")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
				break;
			case 'B':
				if (!stackDescriptor.equals("Ljava/lang/Byte")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				break;
			case 'C':
				if (!stackDescriptor.equals("Ljava/lang/Character")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				break;
			case 'D':
				if (!stackDescriptor.equals("Ljava/lang/Double")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				break;
			case 'F':
				if (!stackDescriptor.equals("Ljava/lang/Float")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				break;
			case 'I':
				if (!stackDescriptor.equals("Ljava/lang/Integer")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				break;
			case 'J':
				if (!stackDescriptor.equals("Ljava/lang/Long")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
				break;
			case 'S':
				if (!stackDescriptor.equals("Ljava/lang/Short")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				break;
			default:
				throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	/**
	 * 对于数字，使用数字上的相应方法将其转换为所请求的基本类型。
	 * @param mv 插入指令的目标方法访问器
	 * @param targetDescriptor 期望输出的基本类型
	 * @param stackDescriptor 栈顶类型的描述符
	 */
	public static void insertUnboxNumberInsns(
			MethodVisitor mv, char targetDescriptor, @Nullable String stackDescriptor) {

		if (stackDescriptor == null) {
			return;
		}

		switch (targetDescriptor) {
			case 'D':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
				break;
			case 'F':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
				break;
			case 'J':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
				break;
			case 'I':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
				break;
			// does not handle Z, B, C, S
			default:
				throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + targetDescriptor + "'");
		}
	}

	/**
	 * 根据栈上的内容和所需的目标类型，插入任何必要的数值转换字节码。
	 * @param mv 放置指令的目标方法访问器
	 * @param targetDescriptor 目标类型的（基本类型）描述符
	 * @param stackDescriptor 栈顶操作数的描述符
	 */
	public static void insertAnyNecessaryTypeConversionBytecodes(MethodVisitor mv, char targetDescriptor, String stackDescriptor) {
		if (CodeFlow.isPrimitive(stackDescriptor)) {
			char stackTop = stackDescriptor.charAt(0);
			if (stackTop == 'I' || stackTop == 'B' || stackTop == 'S' || stackTop == 'C') {
				if (targetDescriptor == 'D') {
					mv.visitInsn(I2D);
				}
				else if (targetDescriptor == 'F') {
					mv.visitInsn(I2F);
				}
				else if (targetDescriptor == 'J') {
					mv.visitInsn(I2L);
				}
				else if (targetDescriptor == 'I') {
					// nop
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
				}
			}
			else if (stackTop == 'J') {
				if (targetDescriptor == 'D') {
					mv.visitInsn(L2D);
				}
				else if (targetDescriptor == 'F') {
					mv.visitInsn(L2F);
				}
				else if (targetDescriptor == 'J') {
					// nop
				}
				else if (targetDescriptor == 'I') {
					mv.visitInsn(L2I);
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
				}
			}
			else if (stackTop == 'F') {
				if (targetDescriptor == 'D') {
					mv.visitInsn(F2D);
				}
				else if (targetDescriptor == 'F') {
					// nop
				}
				else if (targetDescriptor == 'J') {
					mv.visitInsn(F2L);
				}
				else if (targetDescriptor == 'I') {
					mv.visitInsn(F2I);
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
				}
			}
			else if (stackTop == 'D') {
				if (targetDescriptor == 'D') {
					// nop
				}
				else if (targetDescriptor == 'F') {
					mv.visitInsn(D2F);
				}
				else if (targetDescriptor == 'J') {
					mv.visitInsn(D2L);
				}
				else if (targetDescriptor == 'I') {
					mv.visitInsn(D2I);
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackDescriptor + " to " + targetDescriptor);
				}
			}
		}
	}


	/**
	 * 创建方法的 JVM 签名描述符。它由用括号括起来的方法参数描述符，加上返回类型的描述符组成。请注意，
	 * 这里的描述符是 JVM 描述符，不同于编译器使用的其他描述符形式，后者不包含末尾的分号。
	 * @param method 方法
	 * @return 字符串形式的签名描述符（例如 "(ILjava/lang/String;)V"）
	 */
	public static String createSignatureDescriptor(Method method) {
		Class<?>[] params = method.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (Class<?> param : params) {
			sb.append(toJvmDescriptor(param));
		}
		sb.append(')');
		sb.append(toJvmDescriptor(method.getReturnType()));
		return sb.toString();
	}

	/**
	 * 创建构造函数的 JVM 签名描述符。它由用括号括起来的构造函数参数描述符，加上返回类型的描述符（始终
	 * 为 "V"）组成。请注意，这里的描述符是 JVM 描述符，不同于编译器使用的其他描述符形式，后者不包含末
	 * 尾的分号。
	 * @param ctor 构造器
	 * @return 字符串形式的签名描述符（例如 "(ILjava/lang/String;)V"）
	 */
	public static String createSignatureDescriptor(Constructor<?> ctor) {
		Class<?>[] params = ctor.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (Class<?> param : params) {
			sb.append(toJvmDescriptor(param));
		}
		sb.append(")V");
		return sb.toString();
	}

	/**
	 * 确定指定类的 JVM 描述符。不同于编译过程中使用的其他描述符，这是 JVM 所需的描述符，因此它包含任何
	 * 必要的末尾分号（例如 Ljava/lang/String; 而不是 Ljava/lang/String）。
	 * @param clazz 一个类
	 * @return 该类的 JVM 描述符
	 */
	public static String toJvmDescriptor(Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		if (clazz.isArray()) {
			while (clazz.isArray()) {
				sb.append('[');
				clazz = clazz.getComponentType();
			}
		}
		if (clazz.isPrimitive()) {
			if (clazz == Boolean.TYPE) {
				sb.append('Z');
			}
			else if (clazz == Byte.TYPE) {
				sb.append('B');
			}
			else if (clazz == Character.TYPE) {
				sb.append('C');
			}
			else if (clazz == Double.TYPE) {
				sb.append('D');
			}
			else if (clazz == Float.TYPE) {
				sb.append('F');
			}
			else if (clazz == Integer.TYPE) {
				sb.append('I');
			}
			else if (clazz == Long.TYPE) {
				sb.append('J');
			}
			else if (clazz == Short.TYPE) {
				sb.append('S');
			}
			else if (clazz == Void.TYPE) {
				sb.append('V');
			}
		}
		else {
			sb.append('L');
			sb.append(clazz.getName().replace('.', '/'));
			sb.append(';');
		}
		return sb.toString();
	}

	/**
	 * 确定对象实例（或 {@code null}）的描述符。
	 * @param value 一个对象（可能为 {@code null}）
	 * @return 该对象的类型描述符
	 * （对于 {@code null} 值，描述符为 "Ljava/lang/Object"）
	 */
	public static String toDescriptorFromObject(@Nullable Object value) {
		if (value == null) {
			return "Ljava/lang/Object";
		}
		else {
			return toDescriptor(value.getClass());
		}
	}

	/**
	 * 判断描述符对应的是 boolean 基本类型还是 boolean 引用类型。
	 * @param descriptor 类型描述符
	 * @return 如果该描述符与 boolean 兼容则返回 {@code true}
	 */
	public static boolean isBooleanCompatible(@Nullable String descriptor) {
		return (descriptor != null && (descriptor.equals("Z") || descriptor.equals("Ljava/lang/Boolean")));
	}

	/**
	 * 判断描述符是否对应基本类型。
	 * @param descriptor 类型描述符
	 * @return 如果是基本类型则返回 {@code true}
	 */
	public static boolean isPrimitive(@Nullable String descriptor) {
		return (descriptor != null && descriptor.length() == 1);
	}

	/**
	 * 判断描述符是否对应基本类型数组（例如 "[[I"）。
	 * @param descriptor 可能为基本类型数组的描述符
	 * @return 如果该描述符表示基本类型数组则返回 {@code true}
	 */
	public static boolean isPrimitiveArray(@Nullable String descriptor) {
		if (descriptor == null) {
			return false;
		}
		boolean primitive = true;
		for (int i = 0, max = descriptor.length(); i < max; i++) {
			char ch = descriptor.charAt(i);
			if (ch == '[') {
				continue;
			}
			primitive = (ch != 'L');
			break;
		}
		return primitive;
	}

	/**
	 * 判断装箱/拆箱能否从一种类型转换到另一种类型。假定至少有一种类型是装箱形式（即单字符描述符）。
	 * @return 如果可通过装箱从一个描述符得到另一个则返回 {@code true}
	 */
	public static boolean areBoxingCompatible(String desc1, String desc2) {
		if (desc1.equals(desc2)) {
			return true;
		}
		if (desc1.length() == 1) {
			if (desc1.equals("Z")) {
				return desc2.equals("Ljava/lang/Boolean");
			}
			else if (desc1.equals("D")) {
				return desc2.equals("Ljava/lang/Double");
			}
			else if (desc1.equals("F")) {
				return desc2.equals("Ljava/lang/Float");
			}
			else if (desc1.equals("I")) {
				return desc2.equals("Ljava/lang/Integer");
			}
			else if (desc1.equals("J")) {
				return desc2.equals("Ljava/lang/Long");
			}
		}
		else if (desc2.length() == 1) {
			if (desc2.equals("Z")) {
				return desc1.equals("Ljava/lang/Boolean");
			}
			else if (desc2.equals("D")) {
				return desc1.equals("Ljava/lang/Double");
			}
			else if (desc2.equals("F")) {
				return desc1.equals("Ljava/lang/Float");
			}
			else if (desc2.equals("I")) {
				return desc1.equals("Ljava/lang/Integer");
			}
			else if (desc2.equals("J")) {
				return desc1.equals("Ljava/lang/Long");
			}
		}
		return false;
	}

	/**
	 * 判断提供的描述符是否对应受支持的数字类型或 boolean。编译过程（目前）只支持某些数字类型，包括
	 * double、float、long 和 int。
	 * @param descriptor 某个类型的描述符
	 * @return 如果该描述符表示受支持的数值类型或 boolean 则返回 {@code true}
	 */
	public static boolean isPrimitiveOrUnboxableSupportedNumberOrBoolean(@Nullable String descriptor) {
		if (descriptor == null) {
			return false;
		}
		if (isPrimitiveOrUnboxableSupportedNumber(descriptor)) {
			return true;
		}
		return ("Z".equals(descriptor) || descriptor.equals("Ljava/lang/Boolean"));
	}

	/**
	 * 判断提供的描述符是否对应受支持的数字。编译过程（目前）只支持某些数字类型，包括 double、float、
	 * long 和 int。
	 * @param descriptor 某个类型的描述符
	 * @return 如果该描述符表示受支持的数值类型则返回 {@code true}
	 */
	public static boolean isPrimitiveOrUnboxableSupportedNumber(@Nullable String descriptor) {
		if (descriptor == null) {
			return false;
		}
		if (descriptor.length() == 1) {
			return "DFIJ".contains(descriptor);
		}
		if (descriptor.startsWith("Ljava/lang/")) {
			String name = descriptor.substring("Ljava/lang/".length());
			if (name.equals("Double") || name.equals("Float") || name.equals("Integer") || name.equals("Long")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断给定数字在字节码级别的数值运算中是否应被视为整数。
	 * @param number 要检查的数字
	 * @return 如果它是 {@link Integer}、{@link Short} 或 {@link Byte} 则返回 {@code true}
	 */
	public static boolean isIntegerForNumericOp(Number number) {
		return (number instanceof Integer || number instanceof Short || number instanceof Byte);
	}

	/**
	 * 将类型描述符转换为单字符基本类型描述符。
	 * @param descriptor 应具有基本类型表示的类型的描述符
	 * @return 基本类型输入描述符对应的单字符描述符
	 */
	public static char toPrimitiveTargetDesc(String descriptor) {
		if (descriptor.length() == 1) {
			return descriptor.charAt(0);
		}
		else if (descriptor.equals("Ljava/lang/Boolean")) {
			return 'Z';
		}
		else if (descriptor.equals("Ljava/lang/Byte")) {
			return 'B';
		}
		else if (descriptor.equals("Ljava/lang/Character")) {
			return 'C';
		}
		else if (descriptor.equals("Ljava/lang/Double")) {
			return 'D';
		}
		else if (descriptor.equals("Ljava/lang/Float")) {
			return 'F';
		}
		else if (descriptor.equals("Ljava/lang/Integer")) {
			return 'I';
		}
		else if (descriptor.equals("Ljava/lang/Long")) {
			return 'J';
		}
		else if (descriptor.equals("Ljava/lang/Short")) {
			return 'S';
		}
		else {
			throw new IllegalStateException("No primitive for '" + descriptor + "'");
		}
	}

	/**
	 * 为提供的描述符插入适当的 CHECKCAST 指令。
	 * @param mv 插入指令的目标访问器
	 * @param descriptor 要转换到的类型的描述符
	 */
	public static void insertCheckCast(MethodVisitor mv, @Nullable String descriptor) {
		if (descriptor != null && descriptor.length() != 1) {
			if (descriptor.charAt(0) == '[') {
				if (isPrimitiveArray(descriptor)) {
					mv.visitTypeInsn(CHECKCAST, descriptor);
				}
				else {
					mv.visitTypeInsn(CHECKCAST, descriptor + ";");
				}
			}
			else {
				if (!descriptor.equals("Ljava/lang/Object")) {
					// This is chopping off the 'L' to leave us with "java/lang/String"
					mv.visitTypeInsn(CHECKCAST, descriptor.substring(1));
				}
			}
		}
	}

	/**
	 * 确定特定类型对应的适当装箱指令（如果需要装箱），并将该指令插入提供的 visitor。
	 * @param mv 新指令的目标访问器
	 * @param descriptor 可能需要也可能不需要装箱的类型的描述符
	 */
	public static void insertBoxIfNecessary(MethodVisitor mv, @Nullable String descriptor) {
		if (descriptor != null && descriptor.length() == 1) {
			insertBoxIfNecessary(mv, descriptor.charAt(0));
		}
	}

	/**
	 * 确定特定类型对应的适当装箱指令（如果需要装箱），并将该指令插入提供的 visitor。
	 * @param mv 新指令的目标访问器
	 * @param ch 可能需要装箱的类型的描述符
	 */
	public static void insertBoxIfNecessary(MethodVisitor mv, char ch) {
		switch (ch) {
			case 'Z':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
				break;
			case 'B':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				break;
			case 'C':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				break;
			case 'D':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				break;
			case 'F':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				break;
			case 'I':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				break;
			case 'J':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				break;
			case 'S':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				break;
			case 'L':
			case 'V':
			case '[':
				// no box needed
				break;
			default:
				throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	/**
	 * 推断类型的描述符。描述符类似于 JVM 类型名称，但缺少末尾的 ';'，因此 Object 的描述符是
	 * "Ljava/lang/Object"，int 的描述符是 "I"。
	 * @param type 要为其确定描述符的类型（可能是基本类型）
	 * @return 描述符
	 */
	public static String toDescriptor(Class<?> type) {
		String name = type.getName();
		if (type.isPrimitive()) {
			switch (name.length()) {
				case 3:
					return "I";
				case 4:
					if (name.equals("byte")) {
						return "B";
					}
					else if (name.equals("char")) {
						return "C";
					}
					else if (name.equals("long")) {
						return "J";
					}
					else if (name.equals("void")) {
						return "V";
					}
					break;
				case 5:
					if (name.equals("float")) {
						return "F";
					}
					else if (name.equals("short")) {
						return "S";
					}
					break;
				case 6:
					if (name.equals("double")) {
						return "D";
					}
					break;
				case 7:
					if (name.equals("boolean")) {
						return "Z";
					}
					break;
			}
		}
		else {
			if (name.charAt(0) != '[') {
				return "L" + type.getName().replace('.', '/');
			}
			else {
				if (name.endsWith(";")) {
					return name.substring(0, name.length() - 1).replace('.', '/');
				}
				else {
					return name;  // array has primitive component type
				}
			}
		}
		return "";
	}

	/**
	 * 创建描述符数组，表示所提供方法的参数类型。如果没有参数，则返回零长度数组。
	 * @param method 一个 Method
	 * @return 描述符字符串数组，每个方法参数对应一项
	 */
	public static String[] toParamDescriptors(Method method) {
		return toDescriptors(method.getParameterTypes());
	}

	/**
	 * 创建描述符数组，表示所提供构造函数的参数类型。如果没有参数，则返回零长度数组。
	 * @param ctor 一个 Constructor
	 * @return 描述符字符串数组，每个构造器参数对应一项
	 */
	public static String[] toParamDescriptors(Constructor<?> ctor) {
		return toDescriptors(ctor.getParameterTypes());
	}

	/**
	 * 从类数组创建描述符数组。
	 * @param types 输入的类数组
	 * @return 描述符数组
	 */
	public static String[] toDescriptors(Class<?>[] types) {
		int typesCount = types.length;
		String[] descriptors = new String[typesCount];
		for (int p = 0; p < typesCount; p++) {
			descriptors[p] = toDescriptor(types[p]);
		}
		return descriptors;
	}

	/**
	 * 创建用于在栈上加载数字的最优指令。
	 * @param mv 插入字节码的位置
	 * @param value 要加载的值
	 */
	public static void insertOptimalLoad(MethodVisitor mv, int value) {
		if (value < 6) {
			mv.visitInsn(ICONST_0+value);
		}
		else if (value < Byte.MAX_VALUE) {
			mv.visitIntInsn(BIPUSH, value);
		}
		else if (value < Short.MAX_VALUE) {
			mv.visitIntInsn(SIPUSH, value);
		}
		else {
			mv.visitLdcInsn(value);
		}
	}

	/**
	 * 生成适当的字节码以将栈项存入数组。使用的指令取决于该类型是基本类型还是引用类型。
	 * @param mv 插入字节码的位置
	 * @param arrayElementType 数组元素的类型
	 */
	public static void insertArrayStore(MethodVisitor mv, String arrayElementType) {
		if (arrayElementType.length()==1) {
			switch (arrayElementType.charAt(0)) {
				case 'I':
					mv.visitInsn(IASTORE);
					break;
				case 'J':
					mv.visitInsn(LASTORE);
					break;
				case 'F':
					mv.visitInsn(FASTORE);
					break;
				case 'D':
					mv.visitInsn(DASTORE);
					break;
				case 'B':
					mv.visitInsn(BASTORE);
					break;
				case 'C':
					mv.visitInsn(CASTORE);
					break;
				case 'S':
					mv.visitInsn(SASTORE);
					break;
				case 'Z':
					mv.visitInsn(BASTORE);
					break;
				default:
					throw new IllegalArgumentException(
							"Unexpected arraytype " + arrayElementType.charAt(0));
			}
		}
		else {
			mv.visitInsn(AASTORE);
		}
	}

	/**
	 * 确定用于 NEWARRAY 字节码的适当 T 标记。
	 * @param arraytype 数组的基本类型组件类型
	 * @return 用于 NEWARRAY 的 T 标记
	 */
	public static int arrayCodeFor(String arraytype) {
		switch (arraytype.charAt(0)) {
			case 'I': return T_INT;
			case 'J': return T_LONG;
			case 'F': return T_FLOAT;
			case 'D': return T_DOUBLE;
			case 'B': return T_BYTE;
			case 'C': return T_CHAR;
			case 'S': return T_SHORT;
			case 'Z': return T_BOOLEAN;
			default:
				throw new IllegalArgumentException("Unexpected arraytype " + arraytype.charAt(0));
		}
	}

	/**
	 * 返回所提供的数组类型是否具有核心组件引用类型。
	 */
	public static boolean isReferenceTypeArray(String arraytype) {
		int length = arraytype.length();
		for (int i = 0; i < length; i++) {
			char ch = arraytype.charAt(i);
			if (ch == '[') {
				continue;
			}
			return (ch == 'L');
		}
		return false;
	}

	/**
	 * 生成用于构建数组的正确字节码。使用的操作码以及随操作码传递的签名可能因数组类型的签名而异。
	 * @param mv 插入代码的目标方法访问器
	 * @param size 数组的大小
	 * @param arraytype 数组的类型
	 */
	public static void insertNewArrayCode(MethodVisitor mv, int size, String arraytype) {
		insertOptimalLoad(mv, size);
		if (arraytype.length() == 1) {
			mv.visitIntInsn(NEWARRAY, CodeFlow.arrayCodeFor(arraytype));
		}
		else {
			if (arraytype.charAt(0) == '[') {
				// Handling the nested array case here.
				// If vararg is [[I then we want [I and not [I;
				if (CodeFlow.isReferenceTypeArray(arraytype)) {
					mv.visitTypeInsn(ANEWARRAY, arraytype + ";");
				}
				else {
					mv.visitTypeInsn(ANEWARRAY, arraytype);
				}
			}
			else {
				mv.visitTypeInsn(ANEWARRAY, arraytype.substring(1));
			}
		}
	}

	/**
	 * 用于数学运算符，处理将栈上（可能已装箱的）数字转换为基本数值类型。<p>例如，从 Integer 到 double
	 * 只需调用 'Number.doubleValue()'，但从 int 到 double 需要使用字节码 'i2d'。
	 * @param mv 应追加指令的方法访问器
	 * @param stackDescriptor 栈上操作数的描述符
	 * @param targetDescriptor 基本类型描述符
	 */
	public static void insertNumericUnboxOrPrimitiveTypeCoercion(
			MethodVisitor mv, @Nullable String stackDescriptor, char targetDescriptor) {

		if (!CodeFlow.isPrimitive(stackDescriptor)) {
			CodeFlow.insertUnboxNumberInsns(mv, targetDescriptor, stackDescriptor);
		}
		else {
			CodeFlow.insertAnyNecessaryTypeConversionBytecodes(mv, targetDescriptor, stackDescriptor);
		}
	}

	public static String toBoxedDescriptor(String primitiveDescriptor) {
		switch (primitiveDescriptor.charAt(0)) {
			case 'I': return "Ljava/lang/Integer";
			case 'J': return "Ljava/lang/Long";
			case 'F': return "Ljava/lang/Float";
			case 'D': return "Ljava/lang/Double";
			case 'B': return "Ljava/lang/Byte";
			case 'C': return "Ljava/lang/Character";
			case 'S': return "Ljava/lang/Short";
			case 'Z': return "Ljava/lang/Boolean";
			default:
				throw new IllegalArgumentException("Unexpected non primitive descriptor " + primitiveDescriptor);
		}
	}


	/**
	 * 用于生成字段的接口。
	 */
	@FunctionalInterface
	public interface FieldAdder {

		void generateField(ClassWriter cw, CodeFlow codeflow);
	}


	/**
	 * 用于生成 {@code clinit} 静态初始化块的接口。
	 */
	@FunctionalInterface
	public interface ClinitAdder {

		void generateCode(MethodVisitor mv, CodeFlow codeflow);
	}

}
