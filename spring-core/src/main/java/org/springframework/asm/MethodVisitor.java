// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.springframework.asm;

/**
 * 用于访问 Java 方法的访问者。该类的方法必须按以下顺序调用：( {@code visitParameter} )* [
 * {@code visitAnnotationDefault} ] ( {@code visitAnnotation} |
 * {@code visitAnnotableParameterCount} | {@code visitParameterAnnotation}
 * {@code visitTypeAnnotation} | {@code visitAttribute} )* [ {@code visitCode} ( {@code visitFrame}
 * | {@code visit<i>X</i>Insn} | {@code visitLabel} | {@code visitInsnAnnotation} |
 * {@code visitTryCatchBlock} | {@code visitTryCatchAnnotation} | {@code visitLocalVariable} |
 * {@code visitLocalVariableAnnotation} | {@code visitLineNumber} )* {@code visitMaxs} ]
 * {@code visitEnd}。此外，{@code visit<i>X</i>Insn} 和 {@code visitLabel} 方法必须按照被访问代码的
 * 字节码指令顺序依次调用，{@code visitInsnAnnotation} 必须在被注解的指令<i>之后</i>调用，
 * {@code visitTryCatchBlock} 必须在作为参数传入的标签被访问<i>之前</i>调用，
 * {@code visitTryCatchBlockAnnotation} 必须在相应的 try catch 块被访问<i>之后</i>调用，而
 * {@code visitLocalVariable}、{@code visitLocalVariableAnnotation} 和 {@code visitLineNumber} 方法
 * 必须在作为参数传入的标签被访问<i>之后</i>调用。
 *
 * @author Eric Bruneton
 */
public abstract class MethodVisitor {

  private static final String REQUIRES_ASM5 = "This feature requires ASM5";

  /**
   * 此访问者实现的 ASM API 版本。该字段的值必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 值之一。
   */
  protected final int api;

  /**
   * 此访问者必须将方法调用委托给的方法访问者。可以为 {@literal null}。
   */
  protected MethodVisitor mv;

  /**
   * 构造一个新的 {@link MethodVisitor}。
   *
   * @param api 此访问器实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 值之一。
   */
  protected MethodVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link MethodVisitor}。
   *
   * @param api 此访问器实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 值之一。
   * @param methodVisitor 此访问器必须将方法调用委托给的方法访问器。可以为 null。
   */
  protected MethodVisitor(final int api, final MethodVisitor methodVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: no preview mode check for ASM experimental
    this.api = api;
    this.mv = methodVisitor;
  }

  /**
   * 此访问者必须将方法调用委托给的方法访问者。可以为 {@literal null}。
   *
   * @return 此访问器必须将方法调用委托给的方法访问器，或 {@literal null}。
   */
  public MethodVisitor getDelegate() {
    return mv;
  }

  // -----------------------------------------------------------------------------------------------
  // Parameters, annotations and non standard attributes
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问此方法的一个参数。
   *
   * @param name 参数名称，如果未提供则为 {@literal null}。
   * @param access 参数的访问标志，仅允许 {@code ACC_FINAL}、{@code ACC_SYNTHETIC}
   * 和/或 {@code ACC_MANDATED}（参见 {@link Opcodes}）。
   */
  public void visitParameter(final String name, final int access) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      mv.visitParameter(name, access);
    }
  }

  /**
   * 访问此注解接口方法的默认值。
   *
   * @return 用于访问此注解接口方法实际默认值的访问器；如果此访问器不关心访问该默认值，
   * 则为 {@literal null}。传给此注解访问器各方法的 'name' 参数会被忽略。
   * 此外，必须对此注解访问器恰好调用一个 visit 方法，随后调用 visitEnd。
   */
  public AnnotationVisitor visitAnnotationDefault() {
    if (mv != null) {
      return mv.visitAnnotationDefault();
    }
    return null;
  }

  /**
   * 访问此方法的一个注解。
   *
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (mv != null) {
      return mv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问方法签名中某个类型上的注解。
   *
   * @param typeRef 对被注解类型的引用。该类型引用的种类必须是
   * {@link TypeReference#METHOD_TYPE_PARAMETER}、
   * {@link TypeReference#METHOD_TYPE_PARAMETER_BOUND}、{@link TypeReference#METHOD_RETURN}、
   * {@link TypeReference#METHOD_RECEIVER}、{@link TypeReference#METHOD_FORMAL_PARAMETER} 或
   * {@link TypeReference#THROWS}。参见 {@link TypeReference}。
   * @param typePath 指向 'typeRef' 中被注解的类型实参、通配符绑定、数组元素类型或
   * 静态内部类型的路径。如果注解作用于整个 'typeRef'，则可以为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问可以带有注解的方法参数数量。默认情况下（即未调用此方法时），由方法描述符定义的所有方法参数都
   * 可以带有注解。
   *
   * @param parameterCount 可以带有注解的方法参数数量。此数字必须小于或等于
   * 方法描述符中的参数类型数量。当方法包含合成参数，且为参数注解计算参数索引时
   * 忽略这些参数时，它可以严格小于该数量（参见
   * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18）。
   * @param visible 如果为 {@literal true}，则定义可在运行时具有可见注解的方法参数数量；
   * 如果为 {@literal false}，则定义可在运行时具有不可见注解的方法参数数量。
   */
  public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    if (mv != null) {
      mv.visitAnnotableParameterCount(parameterCount, visible);
    }
  }

  /**
   * 访问此方法某个参数的注解。
   *
   * @param parameter 参数索引。此索引必须严格小于方法描述符中的参数数量，
   * 并且严格小于 {@link #visitAnnotableParameterCount} 中指定的参数数量。
   * 重要说明：<i>参数索引 i 不必对应方法描述符中的第 i 个参数描述符</i>，
   * 特别是在存在合成参数的情况下（参见
   * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18）。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
   */
  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String descriptor, final boolean visible) {
    if (mv != null) {
      return mv.visitParameterAnnotation(parameter, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问此方法的非标准属性。
   *
   * @param attribute 一个属性。
   */
  public void visitAttribute(final Attribute attribute) {
    if (mv != null) {
      mv.visitAttribute(attribute);
    }
  }

  /** 开始访问方法的代码（如果有，即非抽象方法）。 */
  public void visitCode() {
    if (mv != null) {
      mv.visitCode();
    }
  }

  /**
   * 访问局部变量和操作数栈元素的当前状态。对于跟在 GOTO 或 THROW 等无条件分支指令之后、作为跳转指令
   * 的目标、或者以异常处理器块开头的任何指令 <b>i</b>，都必须在它<i>之前</i>紧接着调用此方法。被访问
   * 的类型必须描述在执行 <b>i</b> <i>之前</i>那一时刻局部变量和操作数栈元素的值。<br>
   * <br>
   * (*) 仅对版本大于或等于 {@link Opcodes#V1_6} 的类，这是必需的。<br>
   * <br>
   * 方法的帧必须以展开形式或压缩形式给出（所有帧必须使用相同的格式，即不得在单个方法中混用展开帧和压
   * 缩帧）：
   *
   * <ul>
   * <li>在展开形式中，所有帧都必须具有 F_NEW 类型。
   * <li>在压缩形式中，帧基本上是相对于前一帧状态的“增量”：
   * <ul>
   * <li>{@link Opcodes#F_SAME} 表示与前一帧具有完全相同的局部变量且操作数栈为空的帧。
   * <li>{@link Opcodes#F_SAME1} 表示与前一帧具有完全相同的局部变量且栈上只有一个值的帧（<code>numStack</code> 为 1，<code>stack[0]</code> 包含该栈项类型的值）。
   * <li>{@link Opcodes#F_APPEND} 表示当前局部变量与前一帧中的局部变量相同、但额外定义了局部变量的帧（<code>numLocal</code> 为 1、2 或 3，<code>local</code> 元素包含表示新增类型的值）。
   * <li>{@link Opcodes#F_CHOP} 表示当前局部变量与前一帧中的局部变量相同、但缺少最后 1-3 个局部变量且操作数栈为空的帧（<code>numLocal</code> 为 1、2 或 3）。
   * <li>{@link Opcodes#F_FULL} 表示完整的帧数据。
   *       </ul>
   * </ul>
   *
   * <br>
   * 这两种情况下，对应于方法参数和访问标志的第一个帧都是隐式的，且不能被访问。此外，为同一代码位置访
   * 问两个或更多帧是非法的（即两次 visitFrame 调用之间必须至少访问一条指令）。
   *
   * @param type 此栈映射帧的类型。对于扩展帧必须是 {@link Opcodes#F_NEW}，
   * 对于压缩帧必须是 {@link Opcodes#F_FULL}、{@link Opcodes#F_APPEND}、
   * {@link Opcodes#F_CHOP}、{@link Opcodes#F_SAME} 或 {@link Opcodes#F_APPEND}、
   * {@link Opcodes#F_SAME1}。
   * @param numLocal 所访问帧中局部变量的数量。Long 和 double 值各计为一个变量。
   * @param local 此帧中的局部变量类型。此数组不得修改。
   * 基本类型由 {@link Opcodes#TOP}、{@link Opcodes#INTEGER}、{@link Opcodes#FLOAT}、
   * {@link Opcodes#LONG}、{@link Opcodes#DOUBLE}、{@link Opcodes#NULL} 或
   * {@link Opcodes#UNINITIALIZED_THIS} 表示（long 和 double 用单个元素表示）。
   * 引用类型由 String 对象（表示内部名称，参见 {@link Type#getInternalName()}）表示，
   * 未初始化类型由 Label 对象表示（此标签指示创建该未初始化值的 NEW 指令）。
   * @param numStack 所访问帧中操作数栈元素的数量。Long 和 double 值各计为一个栈元素。
   * @param stack 此帧中的操作数栈类型。此数组不得修改。
   * 其内容与 “local” 数组的格式相同。
   * @throws IllegalStateException 如果在另一个帧之后紧接着访问帧，且两者之间没有任何指令
   * （除非此帧是 Opcodes#F_SAME 帧，此时会被静默忽略）。
   */
  public void visitFrame(
      final int type,
      final int numLocal,
      final Object[] local,
      final int numStack,
      final Object[] stack) {
    if (mv != null) {
      mv.visitFrame(type, numLocal, local, numStack, stack);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Normal instructions
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问一条零操作数指令。
   *
   * @param opcode 要访问的指令的操作码。此操作码为 NOP、ACONST_NULL、ICONST_M1、ICONST_0、ICONST_1、
   * ICONST_2、ICONST_3、ICONST_4、ICONST_5、LCONST_0、LCONST_1、FCONST_0、FCONST_1、
   * FCONST_2、DCONST_0、DCONST_1、IALOAD、LALOAD、FALOAD、DALOAD、AALOAD、BALOAD、CALOAD、
   * SALOAD、IASTORE、LASTORE、FASTORE、DASTORE、AASTORE、BASTORE、CASTORE、SASTORE、POP、
   * POP2、DUP、DUP_X1、DUP_X2、DUP2、DUP2_X1、DUP2_X2、SWAP、IADD、LADD、FADD、DADD、
   * ISUB、LSUB、FSUB、DSUB、IMUL、LMUL、FMUL、DMUL、IDIV、LDIV、FDIV、DDIV、IREM、LREM、
   * FREM、DREM、INEG、LNEG、FNEG、DNEG、ISHL、LSHL、ISHR、LSHR、IUSHR、LUSHR、IAND、LAND、
   * IOR、LOR、IXOR、LXOR、I2L、I2F、I2D、L2I、L2F、L2D、F2I、F2L、F2D、D2I、D2L、D2F、
   * I2B、I2C、I2S、LCMP、FCMPL、FCMPG、DCMPL、DCMPG、IRETURN、LRETURN、FRETURN、DRETURN、
   * ARETURN、RETURN、ARRAYLENGTH、ATHROW、MONITORENTER 或 MONITOREXIT。
   */
  public void visitInsn(final int opcode) {
    if (mv != null) {
      mv.visitInsn(opcode);
    }
  }

  /**
   * 访问一条带单个 int 操作数的指令。
   *
   * @param opcode 要访问的指令的操作码。此操作码为 BIPUSH、SIPUSH 或 NEWARRAY。
   * @param operand 要访问的指令的操作数。<br>
   * 当操作码为 BIPUSH 时，操作数值应介于 Byte.MIN_VALUE 与 Byte.MAX_VALUE 之间。<br>
   * 当操作码为 SIPUSH 时，操作数值应介于 Short.MIN_VALUE 与 Short.MAX_VALUE 之间。<br>
   * 当操作码为 NEWARRAY 时，操作数值应为 {@link Opcodes#T_BOOLEAN}、{@link Opcodes#T_CHAR}、
   * {@link Opcodes#T_FLOAT}、{@link Opcodes#T_DOUBLE}、{@link Opcodes#T_BYTE}、
   * {@link Opcodes#T_SHORT}、{@link Opcodes#T_INT} 或 {@link Opcodes#T_LONG} 之一。
   */
  public void visitIntInsn(final int opcode, final int operand) {
    if (mv != null) {
      mv.visitIntInsn(opcode, operand);
    }
  }

  /**
   * 访问一条局部变量指令。局部变量指令是加载或存储局部变量值的指令。
   *
   * @param opcode 要访问的局部变量指令的操作码。此操作码为 ILOAD、LLOAD、FLOAD、DLOAD、ALOAD、
   * ISTORE、LSTORE、FSTORE、DSTORE、ASTORE 或 RET。
   * @param varIndex 要访问的指令的操作数。此操作数是局部变量的索引。
   */
  public void visitVarInsn(final int opcode, final int varIndex) {
    if (mv != null) {
      mv.visitVarInsn(opcode, varIndex);
    }
  }

  /**
   * 访问一条类型指令。类型指令是接受类的内部名称作为参数的指令（参见 {@link Type#getInternalName()}
   * ）。
   *
   * @param opcode 要访问的类型指令的操作码。此操作码为 NEW、ANEWARRAY、CHECKCAST 或 INSTANCEOF。
   * @param type 要访问的指令的操作数。此操作数必须是对象或数组类的内部名称
   * （参见 {@link Type#getInternalName()}）。
   */
  public void visitTypeInsn(final int opcode, final String type) {
    if (mv != null) {
      mv.visitTypeInsn(opcode, type);
    }
  }

  /**
   * 访问一条字段指令。字段指令是加载或存储对象字段值的指令。
   *
   * @param opcode 要访问的类型指令的操作码。此操作码为 GETSTATIC、PUTSTATIC、GETFIELD 或 PUTFIELD。
   * @param owner 字段所有者类的内部名称（参见 {@link Type#getInternalName()}）。
   * @param name 字段的名称。
   * @param descriptor 字段的描述符（参见 {@link Type}）。
   */
  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    if (mv != null) {
      mv.visitFieldInsn(opcode, owner, name, descriptor);
    }
  }

  /**
   * 访问一条方法指令。方法指令是调用方法的指令。
   *
   * @param opcode 要访问的类型指令的操作码。此操作码为 INVOKEVIRTUAL、INVOKESPECIAL、
   * INVOKESTATIC 或 INVOKEINTERFACE。
   * @param owner 方法所有者类的内部名称（参见 {@link Type#getInternalName()}）。
   * @param name 方法的名称。
   * @param descriptor 方法的描述符（参见 {@link Type}）。
   * @deprecated use {@link #visitMethodInsn(int, String, String, String, boolean)} instead.
   */
  @Deprecated
  public void visitMethodInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    int opcodeAndSource = opcode | (api < Opcodes.ASM5 ? Opcodes.SOURCE_DEPRECATED : 0);
    visitMethodInsn(opcodeAndSource, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE);
  }

  /**
   * 访问一条方法指令。方法指令是调用方法的指令。
   *
   * @param opcode 要访问的类型指令的操作码。此操作码为 INVOKEVIRTUAL、INVOKESPECIAL、
   * INVOKESTATIC 或 INVOKEINTERFACE。
   * @param owner 方法所有者类的内部名称（参见 {@link Type#getInternalName()}）。
   * @param name 方法的名称。
   * @param descriptor 方法的描述符（参见 {@link Type}）。
   * @param isInterface 方法的所有者类是否为接口。
   */
  public void visitMethodInsn(
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    if (api < Opcodes.ASM5 && (opcode & Opcodes.SOURCE_DEPRECATED) == 0) {
      if (isInterface != (opcode == Opcodes.INVOKEINTERFACE)) {
        throw new UnsupportedOperationException("INVOKESPECIAL/STATIC on interfaces requires ASM5");
      }
      visitMethodInsn(opcode, owner, name, descriptor);
      return;
    }
    if (mv != null) {
      mv.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
    }
  }

  /**
   * 访问一条 invokedynamic 指令。
   *
   * @param name 方法的名称。
   * @param descriptor 方法的描述符（参见 {@link Type}）。
   * @param bootstrapMethodHandle 引导方法。
   * @param bootstrapMethodArguments 引导方法的常量参数。每个参数必须是 {@link Integer}、{@link Float}、
   * {@link Long}、{@link Double}、{@link String}、{@link Type}、{@link Handle} 或
   * {@link ConstantDynamic} 值。此方法允许修改数组内容，
   * 因此调用方应预期该数组可能发生变化。
   */
  public void visitInvokeDynamicInsn(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }
  }

  /**
   * 访问一条跳转指令。跳转指令是可以跳转到另一条指令的指令。
   *
   * @param opcode 要访问的类型指令的操作码。此操作码为 IFEQ、IFNE、IFLT、IFGE、IFGT、IFLE、
   * IF_ICMPEQ、IF_ICMPNE、IF_ICMPLT、IF_ICMPGE、IF_ICMPGT、IF_ICMPLE、IF_ACMPEQ、
   * IF_ACMPNE、GOTO、JSR、IFNULL 或 IFNONNULL。
   * @param label 要访问的指令的操作数。此操作数是一个标签，
   * 用于指定该跳转指令可能跳转到的指令。
   */
  public void visitJumpInsn(final int opcode, final Label label) {
    if (mv != null) {
      mv.visitJumpInsn(opcode, label);
    }
  }

  /**
   * 访问一个标签。标签所指定的是紧随其后将被访问的指令。
   *
   * @param label 一个 {@link Label} 对象。
   */
  public void visitLabel(final Label label) {
    if (mv != null) {
      mv.visitLabel(label);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Special instructions
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问一条 LDC 指令。注意，Java 虚拟机的未来版本可能会增加新的常量类型。为了便于检测新的常量类型，
   * 该方法的实现应检查意外的常量类型，如下所示：
   *
   * <pre>
   * if (cst instanceof Integer) {
   *     // ...
   * } else if (cst instanceof Float) {
   *     // ...
   * } else if (cst instanceof Long) {
   *     // ...
   * } else if (cst instanceof Double) {
   *     // ...
   * } else if (cst instanceof String) {
   *     // ...
   * } else if (cst instanceof Type) {
   *     int sort = ((Type) cst).getSort();
   *     if (sort == Type.OBJECT) {
   *         // ...
   *     } else if (sort == Type.ARRAY) {
   *         // ...
   *     } else if (sort == Type.METHOD) {
   *         // ...
   *     } else {
   *         // throw an exception
   *     }
   * } else if (cst instanceof Handle) {
   *     // ...
   * } else if (cst instanceof ConstantDynamic) {
   *     // ...
   * } else {
   *     // throw an exception
   * }
   * </pre>
   *
   * @param value 要加载到栈上的常量。此参数必须是非 null 的 {@link Integer}、{@link Float}、
   * {@link Long}、{@link Double}、{@link String}，对于类版本为 49 的类，
   * 可以是 OBJECT 或 ARRAY 种类的 {@link Type}（用于 {@code .class} 常量）；
   * 对于 MethodType，可以是 METHOD 种类的 {@link Type}；
   * 对于类版本为 51 的类，可以是 {@link Handle}（用于 MethodHandle 常量）；
   * 对于类版本为 55 的类，可以是 {@link ConstantDynamic}（用于常量动态）。
   */
  public void visitLdcInsn(final Object value) {
    if (api < Opcodes.ASM5
        && (value instanceof Handle
            || (value instanceof Type && ((Type) value).getSort() == Type.METHOD))) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (api < Opcodes.ASM7 && value instanceof ConstantDynamic) {
      throw new UnsupportedOperationException("This feature requires ASM7");
    }
    if (mv != null) {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * 访问一条 IINC 指令。
   *
   * @param varIndex 要递增的局部变量索引。
   * @param increment 局部变量递增的量。
   */
  public void visitIincInsn(final int varIndex, final int increment) {
    if (mv != null) {
      mv.visitIincInsn(varIndex, increment);
    }
  }

  /**
   * 访问一条 TABLESWITCH 指令。
   *
   * @param min 最小键值。
   * @param max 最大键值。
   * @param dflt 默认处理器块的起始。
   * @param labels 各处理器块的起始。{@code labels[i]} 是 {@code min + i} 键对应处理器块的起始。
   */
  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label... labels) {
    if (mv != null) {
      mv.visitTableSwitchInsn(min, max, dflt, labels);
    }
  }

  /**
   * 访问一条 LOOKUPSWITCH 指令。
   *
   * @param dflt 默认处理器块的起始。
   * @param keys 各键的值。
   * @param labels 各处理器块的起始。{@code labels[i]} 是 {@code keys[i]} 键对应处理器块的起始。
   */
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    if (mv != null) {
      mv.visitLookupSwitchInsn(dflt, keys, labels);
    }
  }

  /**
   * 访问一条 MULTIANEWARRAY 指令。
   *
   * @param descriptor 数组类型描述符（参见 {@link Type}）。
   * @param numDimensions 要分配的数组维度数量。
   */
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    if (mv != null) {
      mv.visitMultiANewArrayInsn(descriptor, numDimensions);
    }
  }

  /**
   * 访问指令上的注解。此方法必须在被注解的指令<i>之后</i>紧接着调用。对于同一条指令，它可以被调用多
   * 次。
   *
   * @param typeRef 对被注解类型的引用。该类型引用的种类必须是 {@link TypeReference#INSTANCEOF}、
   * {@link TypeReference#NEW}、{@link TypeReference#CONSTRUCTOR_REFERENCE}、
   * {@link TypeReference#METHOD_REFERENCE}、{@link TypeReference#CAST}、
   * {@link TypeReference#CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、
   * {@link TypeReference#METHOD_INVOCATION_TYPE_ARGUMENT}、
   * {@link TypeReference#CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT} 或
   * {@link TypeReference#METHOD_REFERENCE_TYPE_ARGUMENT}。参见 {@link TypeReference}。
   * @param typePath 指向 'typeRef' 中被注解的类型实参、通配符绑定、数组元素类型或
   * 静态内部类型的路径。如果注解作用于整个 'typeRef'，则可以为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
   */
  public AnnotationVisitor visitInsnAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  // -----------------------------------------------------------------------------------------------
  // Exceptions table entries, debug information, max stack and max locals
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问一个 try catch 块。
   *
   * @param start 异常处理器作用域的起始（包含）。
   * @param end 异常处理器作用域的结束（不包含）。
   * @param handler 异常处理器代码的起始。
   * @param type 处理器所处理异常类型的内部名称（参见 {@link Type#getInternalName()}），
   * 或 {@literal null} 以捕获所有异常（用于“finally”块）。
   * @throws IllegalArgumentException 如果其中一个标签已被此访问器（通过 {@link #visitLabel} 方法）访问过。
   */
  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    if (mv != null) {
      mv.visitTryCatchBlock(start, end, handler, type);
    }
  }

  /**
   * 访问异常处理器类型上的注解。对于被注解的异常处理器，此方法必须在 {@link #visitTryCatchBlock} <i>
   * 之后</i>调用。对于同一个异常处理器，它可以被调用多次。
   *
   * @param typeRef 对被注解类型的引用。该类型引用的种类必须是 {@link TypeReference#EXCEPTION_PARAMETER}。
   * 参见 {@link TypeReference}。
   * @param typePath 指向 'typeRef' 中被注解的类型实参、通配符绑定、数组元素类型或
   * 静态内部类型的路径。如果注解作用于整个 'typeRef'，则可以为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
   */
  public AnnotationVisitor visitTryCatchAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问一个局部变量声明。
   *
   * @param name 局部变量的名称。
   * @param descriptor 此局部变量的类型描述符。
   * @param signature 此局部变量的类型签名。如果该局部变量类型未使用泛型，则可以为 {@literal null}。
   * @param start 此局部变量作用域对应的第一条指令（包含）。
   * @param end 此局部变量作用域对应的最后一条指令（不包含）。
   * @param index 局部变量的索引。
   * @throws IllegalArgumentException 如果其中一个标签尚未被此访问器（通过 {@link #visitLabel} 方法）访问过。
   */
  public void visitLocalVariable(
      final String name,
      final String descriptor,
      final String signature,
      final Label start,
      final Label end,
      final int index) {
    if (mv != null) {
      mv.visitLocalVariable(name, descriptor, signature, start, end, index);
    }
  }

  /**
   * 访问局部变量类型上的注解。
   *
   * @param typeRef 对被注解类型的引用。该类型引用的种类必须是 {@link TypeReference#LOCAL_VARIABLE}
   * 或 {@link TypeReference#RESOURCE_VARIABLE}。参见 {@link TypeReference}。
   * @param typePath 指向 'typeRef' 中被注解的类型实参、通配符绑定、数组元素类型或
   * 静态内部类型的路径。如果注解作用于整个 'typeRef'，则可以为 {@literal null}。
   * @param start 构成此局部变量作用域的连续范围所对应的第一条指令（包含）。
   * @param end 构成此局部变量作用域的连续范围所对应的最后一条指令（不包含）。
   * 此数组的大小必须与 'start' 数组相同。
   * @param index 局部变量在各范围中的索引。此数组的大小必须与 'start' 数组相同。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
   */
  public AnnotationVisitor visitLocalVariableAnnotation(
      final int typeRef,
      final TypePath typePath,
      final Label[] start,
      final Label[] end,
      final int[] index,
      final String descriptor,
      final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitLocalVariableAnnotation(
          typeRef, typePath, start, end, index, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问一个行号声明。
   *
   * @param line 行号。此编号指向编译该类的源文件。
   * @param start 与此行号对应的第一条指令。
   * @throws IllegalArgumentException 如果 {@code start} 尚未被此访问器（通过 {@link #visitLabel} 方法）访问过。
   */
  public void visitLineNumber(final int line, final Label start) {
    if (mv != null) {
      mv.visitLineNumber(line, start);
    }
  }

  /**
   * 访问方法的最大栈大小和最大局部变量数量。
   *
   * @param maxStack 方法的最大栈大小。
   * @param maxLocals 方法的最大局部变量数量。
   */
  public void visitMaxs(final int maxStack, final int maxLocals) {
    if (mv != null) {
      mv.visitMaxs(maxStack, maxLocals);
    }
  }

  /**
   * 访问方法的结尾。此方法是最后一个被调用的方法，用于通知访问者方法的所有注解和属性都已访问完毕。
   */
  public void visitEnd() {
    if (mv != null) {
      mv.visitEnd();
    }
  }
}
