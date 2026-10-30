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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * SpelCompiler 会接收一个常规的已解析表达式，并创建（并加载）一个包含字节码的类，该类执行与该表达
 * 式相同的操作。表达式的编译形式求值速度远快于解释形式。
 *
 * <p>SpelCompiler 目前并不处理所有表达式类型，但覆盖了许多常见情况。该框架是可扩展的，以便将来覆盖
 * 更多情况。为了达到绝对最高的速度，编译后的代码中*不做任何检查*。表达式的编译版本在生成字节码时会
 * 使用在解释执行该表达式过程中获知的信息。例如，如果它知道某个属性解引用似乎总是返回 Map，那么它就
 * 会生成期望该属性解引用结果为 Map 的字节码。这确保了最高性能，但如果解引用结果不是 Map，编译后的
 * 表达式就会失败——就像在常规 Java 程序中传入意外类型的数据时会发生 ClassCastException 一样。
 *
 * <p>由于缺乏检查，很可能有些表达式永远不应被编译，例如某个表达式持续处理不同类型的数据。鉴于这些
 * 情况，编译器必须针对关联的 SpelExpressionParser（通过 {@link SpelParserConfiguration} 对象）选择
 * 性地开启，默认情况下并不开启。
 *
 * <p>可以通过调用 {@code SpelCompiler.compile(expression)} 来编译单个表达式。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.1
 */
public final class SpelCompiler implements Opcodes {

	private static final int CLASSES_DEFINED_LIMIT = 100;

	private static final Log logger = LogFactory.getLog(SpelCompiler.class);

	// A compiler is created for each classloader, it manages a child class loader of that
	// classloader and the child is used to load the compiled expressions.
	private static final Map<ClassLoader, SpelCompiler> compilers = new ConcurrentReferenceHashMap<>();


	// The child ClassLoader used to load the compiled expression classes
	private volatile ChildClassLoader childClassLoader;

	// Counter suffix for generated classes within this SpelCompiler instance
	private final AtomicInteger suffixId = new AtomicInteger(1);


	private SpelCompiler(@Nullable ClassLoader classloader) {
		this.childClassLoader = new ChildClassLoader(classloader);
	}


	/**
	 * 尝试编译所提供的表达式。在编译继续之前，会检查它是否可编译。该检查包括遍历表达式 AST 中的所有节
	 * 点，并确保已掌握足够的状态信息以便为它们生成字节码。
	 * @param expression 要编译的表达式
	 * @return 实现已编译表达式的类的实例，
	 * 如果无法编译则为 {@code null}
	 */
	@Nullable
	public CompiledExpression compile(SpelNodeImpl expression) {
		if (expression.isCompilable()) {
			if (logger.isDebugEnabled()) {
				logger.debug("SpEL: compiling " + expression.toStringAST());
			}
			Class<? extends CompiledExpression> clazz = createExpressionClass(expression);
			if (clazz != null) {
				try {
					return ReflectionUtils.accessibleConstructor(clazz).newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to instantiate CompiledExpression for expression: " +
							expression.toStringAST(), ex);
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("SpEL: unable to compile " + expression.toStringAST());
		}
		return null;
	}

	private int getNextSuffix() {
		return this.suffixId.incrementAndGet();
	}

	/**
	 * 生成封装编译后表达式的类并定义它。生成的类将是 CompiledExpression 的子类型。
	 * @param expressionToCompile 要编译的表达式
	 * @return 表达式调用对象，
	 * 如果在代码生成期间决定不进行编译则为 {@code null}
	 */
	@Nullable
	private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl expressionToCompile) {
		// Create class outline 'spel/ExNNN extends org.springframework.expression.spel.CompiledExpression'
		String className = "spel/Ex" + getNextSuffix();
		String evaluationContextClass = "org/springframework/expression/EvaluationContext";
		ClassWriter cw = new ExpressionClassWriter();
		cw.visit(V1_8, ACC_PUBLIC, className, null, "org/springframework/expression/spel/CompiledExpression", null);

		// Create default constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression",
				"<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		// Create getValue() method
		mv = cw.visitMethod(ACC_PUBLIC, "getValue",
				"(Ljava/lang/Object;L" + evaluationContextClass + ";)Ljava/lang/Object;", null,
				new String[] {"org/springframework/expression/EvaluationException"});
		mv.visitCode();

		CodeFlow cf = new CodeFlow(className, cw);

		// Ask the expression AST to generate the body of the method
		try {
			expressionToCompile.generateCode(mv, cf);
		}
		catch (IllegalStateException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(expressionToCompile.getClass().getSimpleName() +
						".generateCode opted out of compilation: " + ex.getMessage());
			}
			return null;
		}

		CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
		if ("V".equals(cf.lastDescriptor())) {
			mv.visitInsn(ACONST_NULL);
		}
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0, 0);  // not supplied due to COMPUTE_MAXS
		mv.visitEnd();
		cw.visitEnd();

		cf.finish();

		byte[] data = cw.toByteArray();
		// TODO need to make this conditionally occur based on a debug flag
		// dump(expressionToCompile.toStringAST(), clazzName, data);
		return loadClass(StringUtils.replace(className, "/", "."), data);
	}

	/**
	 * 加载编译后的表达式类。需确保不滥用类加载器，因为它们会在内存中锚定编译后的类并阻止 GC。如果你有
	 * 表达式随时间不断重新编译，那么通过定期替换类加载器，至少可以让部分较旧的变体被垃圾回收。
	 * @param name 类名
	 * @param bytes 该类的字节码
	 * @return 已编译表达式的 Class 对象
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends CompiledExpression> loadClass(String name, byte[] bytes) {
		ChildClassLoader ccl = this.childClassLoader;
		if (ccl.getClassesDefinedCount() >= CLASSES_DEFINED_LIMIT) {
			synchronized (this) {
				ChildClassLoader currentCcl = this.childClassLoader;
				if (ccl == currentCcl) {
					// Still the same ClassLoader that needs to be replaced...
					ccl = new ChildClassLoader(ccl.getParent());
					this.childClassLoader = ccl;
				}
				else {
					// Already replaced by some other thread, let's pick it up.
					ccl = currentCcl;
				}
			}
		}
		return (Class<? extends CompiledExpression>) ccl.defineClass(name, bytes);
	}


	/**
	 * 编译器实例的工厂方法。返回的 SpelCompiler 会将一个类加载器作为给定类加载器的子级附加，该子级将用
	 * 于加载编译后的表达式。
	 * @param classLoader 用作编译基础的 ClassLoader
	 * @return 对应的 SpelCompiler 实例
	 */
	public static SpelCompiler getCompiler(@Nullable ClassLoader classLoader) {
		ClassLoader clToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		// Quick check for existing compiler without lock contention
		SpelCompiler compiler = compilers.get(clToUse);
		if (compiler == null) {
			// Full lock now since we're creating a child ClassLoader
			synchronized (compilers) {
				compiler = compilers.get(clToUse);
				if (compiler == null) {
					compiler = new SpelCompiler(clToUse);
					compilers.put(clToUse, compiler);
				}
			}
		}
		return compiler;
	}

	/**
	 * 请求尝试编译指定的表达式。如果表达式的各组件不适合编译，或涉及的数据类型不适合编译，则可能失败。
	 * 用于测试。
	 * @param expression 要编译的表达式
	 * @return 如果表达式编译成功则返回 {@code true}，否则返回 {@code false}。
	 */
	public static boolean compile(Expression expression) {
		return (expression instanceof SpelExpression && ((SpelExpression) expression).compileExpression());
	}

	/**
	 * 请求回退到解释器进行表达式求值。任何编译形式都会被丢弃，但可以通过之后的再次重新编译重新创建。
	 * @param expression 该表达式
	 */
	public static void revertToInterpreted(Expression expression) {
		if (expression instanceof SpelExpression) {
			((SpelExpression) expression).revertToInterpreted();
		}
	}


	/**
	 * ChildClassLoader 将加载生成的编译后表达式类。
	 */
	private static class ChildClassLoader extends URLClassLoader {

		private static final URL[] NO_URLS = new URL[0];

		private final AtomicInteger classesDefinedCount = new AtomicInteger(0);

		public ChildClassLoader(@Nullable ClassLoader classLoader) {
			super(NO_URLS, classLoader);
		}

		public Class<?> defineClass(String name, byte[] bytes) {
			Class<?> clazz = super.defineClass(name, bytes, 0, bytes.length);
			this.classesDefinedCount.incrementAndGet();
			return clazz;
		}

		public int getClassesDefinedCount() {
			return this.classesDefinedCount.get();
		}
	}


	/**
	 * 绑定到 SpelCompiler 的 ClassLoader 的 ASM ClassWriter 扩展。
	 */
	private class ExpressionClassWriter extends ClassWriter {

		public ExpressionClassWriter() {
			super(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		}

		@Override
		protected ClassLoader getClassLoader() {
			return childClassLoader;
		}
	}

}
