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
 * 对出现在类、字段或方法声明中或指令上的类型的引用。此类引用指明被引用类型在类中出现的位置，例如
 * 'extends'、'implements' 或 'throws' 子句，'new' 指令，'catch' 子句，类型转换，局部变量声明等。
 *
 * @author Eric Bruneton
 */
public class TypeReference {

  /**
   * 指向泛型类类型参数的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CLASS_TYPE_PARAMETER = 0x00;

  /**
   * 指向泛型方法类型参数的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_TYPE_PARAMETER = 0x01;

  /**
   * 指向类的超类或该类所实现的某个接口的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CLASS_EXTENDS = 0x10;

  /**
   * 指向泛型类类型参数边界的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CLASS_TYPE_PARAMETER_BOUND = 0x11;

  /**
   * 指向泛型方法类型参数边界的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_TYPE_PARAMETER_BOUND = 0x12;

  /** 以字段类型为目标类型的类型引用的类别。参见 {@link #getSort}。 */
  public static final int FIELD = 0x13;

  /** 以方法返回类型为目标类型的类型引用的类别。参见 {@link #getSort}。 */
  public static final int METHOD_RETURN = 0x14;

  /**
   * 指向方法接收者类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_RECEIVER = 0x15;

  /**
   * 指向方法形参类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_FORMAL_PARAMETER = 0x16;

  /**
   * 指向方法 throws 子句中声明的异常类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int THROWS = 0x17;

  /**
   * 指向方法中局部变量类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int LOCAL_VARIABLE = 0x40;

  /**
   * 指向方法中资源变量类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int RESOURCE_VARIABLE = 0x41;

  /**
   * 指向方法中 'catch' 子句异常类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int EXCEPTION_PARAMETER = 0x42;

  /**
   * 指向 'instanceof' 指令中声明类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int INSTANCEOF = 0x43;

  /**
   * 指向 'new' 指令所创建对象类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int NEW = 0x44;

  /**
   * 指向构造方法引用接收者类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CONSTRUCTOR_REFERENCE = 0x45;

  /**
   * 指向方法引用接收者类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_REFERENCE = 0x46;

  /**
   * 指向显式或隐式类型转换指令中声明类型的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CAST = 0x47;

  /**
   * 指向构造方法调用中泛型构造方法类型参数的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;

  /**
   * 指向方法调用中泛型方法类型参数的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;

  /**
   * 指向构造方法引用中泛型构造方法类型参数的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;

  /**
   * 指向方法引用中泛型方法类型参数的类型引用的种类。参见 {@link #getSort}。
   */
  public static final int METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

  /**
   * 此类型引用对应的 target_type 和 target_info 结构（定义见 Java 虚拟机规范，JVMS）。target_type 占
   * 一个字节，target_info 联合体的所有字段最多占 3 个字节（localvar_target 除外，它由专门的方法
   * {@link MethodVisitor#visitLocalVariableAnnotation} 处理）。因此，这两个结构都能存入一个 int。
   *
   * <p>此 int 字段的最高有效字节存储 target_type（在本类公共 API 中称为 TypeReference 的 'sort'），
   * 接下来的字节是 target_info 字段。取决于 target_type，此字段的 1 个、2 个甚至 3 个最低有效字节可
   * 能未被使用。引用字节码偏移量的 target_info 字段被置为 0（这些偏移量在 ClassReader 中被忽略，并在
   * MethodWriter 中重新计算）。
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
   *     4.7.20</a>
   * @see <a
   *     href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.1">JVMS
   *     4.7.20.1</a>
   */
  private final int targetTypeAndInfo;

  /**
   * 构造一个新的 TypeReference。
   *
   * @param typeRef 类型引用的 int 编码值，
   * 如同在 {@link ClassVisitor#visitTypeAnnotation} 等与类型注解相关的 visit 方法中收到的值。
   */
  public TypeReference(final int typeRef) {
    this.targetTypeAndInfo = typeRef;
  }

  /**
   * 返回给定种类的类型引用。
   *
   * @param sort {@link #FIELD}、{@link #METHOD_RETURN}、{@link #METHOD_RECEIVER}、{@link #LOCAL_VARIABLE}、
   * {@link #RESOURCE_VARIABLE}、{@link #INSTANCEOF}、{@link #NEW}、
   * {@link #CONSTRUCTOR_REFERENCE} 或 {@link #METHOD_REFERENCE} 之一。
   * @return 给定种类的类型引用。
   */
  public static TypeReference newTypeReference(final int sort) {
    return new TypeReference(sort << 24);
  }

  /**
   * 返回对泛型类或泛型方法类型参数的引用。
   *
   * @param sort {@link #CLASS_TYPE_PARAMETER} 或 {@link #METHOD_TYPE_PARAMETER} 之一。
   * @param paramIndex 类型参数索引。
   * @return 对给定泛型类或方法类型参数的引用。
   */
  public static TypeReference newTypeParameterReference(final int sort, final int paramIndex) {
    return new TypeReference((sort << 24) | (paramIndex << 16));
  }

  /**
   * 返回对泛型类或泛型方法类型参数边界的引用。
   *
   * @param sort {@link #CLASS_TYPE_PARAMETER} 或 {@link #METHOD_TYPE_PARAMETER} 之一。
   * @param paramIndex 类型参数索引。
   * @param boundIndex 上述类型参数内的类型边界索引。
   * @return 对给定泛型类或方法类型参数边界的引用。
   */
  public static TypeReference newTypeParameterBoundReference(
      final int sort, final int paramIndex, final int boundIndex) {
    return new TypeReference((sort << 24) | (paramIndex << 16) | (boundIndex << 8));
  }

  /**
   * 返回对类的超类或该类 'implements' 子句中某个接口的引用。
   *
   * @param itfIndex 类 'implements' 子句中接口的索引，或 -1 表示引用该类的超类。
   * @return 对给定类的超类型的引用。
   */
  public static TypeReference newSuperTypeReference(final int itfIndex) {
    return new TypeReference((CLASS_EXTENDS << 24) | ((itfIndex & 0xFFFF) << 8));
  }

  /**
   * 返回对方法形参类型的引用。
   *
   * @param paramIndex 形式参数索引。
   * @return 对给定方法形式参数类型的引用。
   */
  public static TypeReference newFormalParameterReference(final int paramIndex) {
    return new TypeReference((METHOD_FORMAL_PARAMETER << 24) | (paramIndex << 16));
  }

  /**
   * 返回对方法 'throws' 子句中异常类型的引用。
   *
   * @param exceptionIndex 方法 'throws' 子句中异常的索引。
   * @return 对给定异常类型的引用。
   */
  public static TypeReference newExceptionReference(final int exceptionIndex) {
    return new TypeReference((THROWS << 24) | (exceptionIndex << 8));
  }

  /**
   * 返回对方法 'catch' 子句中声明异常类型的引用。
   *
   * @param tryCatchBlockIndex try catch 块的索引（按其被 visitTryCatchBlock 访问的顺序）。
   * @return 对给定异常类型的引用。
   */
  public static TypeReference newTryCatchReference(final int tryCatchBlockIndex) {
    return new TypeReference((EXCEPTION_PARAMETER << 24) | (tryCatchBlockIndex << 8));
  }

  /**
   * 返回对构造方法调用、方法调用或引用中类型参数类型的引用。
   *
   * @param sort {@link #CAST}、{@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、
   * {@link #METHOD_INVOCATION_TYPE_ARGUMENT}、
   * {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT} 或 {@link #METHOD_REFERENCE_TYPE_ARGUMENT} 之一。
   * @param argIndex 类型实参索引。
   * @return 对给定类型实参的类型的引用。
   */
  public static TypeReference newTypeArgumentReference(final int sort, final int argIndex) {
    return new TypeReference((sort << 24) | argIndex);
  }

  /**
   * 返回此类型引用的种类。
   *
   * @return {@link #CLASS_TYPE_PARAMETER}、{@link #METHOD_TYPE_PARAMETER}、{@link #CLASS_EXTENDS}、
   * {@link #CLASS_TYPE_PARAMETER_BOUND}、{@link #METHOD_TYPE_PARAMETER_BOUND}、{@link #FIELD}、
   * {@link #METHOD_RETURN}、{@link #METHOD_RECEIVER}、{@link #METHOD_FORMAL_PARAMETER}、
   * {@link #THROWS}、{@link #LOCAL_VARIABLE}、{@link #RESOURCE_VARIABLE}、{@link #EXCEPTION_PARAMETER}、
   * {@link #INSTANCEOF}、{@link #NEW}、{@link #CONSTRUCTOR_REFERENCE}、{@link #METHOD_REFERENCE}、
   * {@link #CAST}、{@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、
   * {@link #METHOD_INVOCATION_TYPE_ARGUMENT}、
   * {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT} 或 {@link #METHOD_REFERENCE_TYPE_ARGUMENT} 之一。
   */
  public int getSort() {
    return targetTypeAndInfo >>> 24;
  }

  /**
   * 返回此类型引用所引用类型参数的索引。此方法只能用于种类为 {@link #CLASS_TYPE_PARAMETER}、
   * {@link #METHOD_TYPE_PARAMETER}、{@link #CLASS_TYPE_PARAMETER_BOUND} 或
   * {@link #METHOD_TYPE_PARAMETER_BOUND} 的类型引用。
   *
   * @return 类型参数索引。
   */
  public int getTypeParameterIndex() {
    return (targetTypeAndInfo & 0x00FF0000) >> 16;
  }

  /**
   * 返回此类型引用所引用的类型参数边界在类型参数 {@link #getTypeParameterIndex} 中的索引。仅限于种类
   * 为 {@link #CLASS_TYPE_PARAMETER_BOUND} 或 {@link #METHOD_TYPE_PARAMETER_BOUND} 的类型引用。
   *
   * @return 类型参数边界索引。
   */
  public int getTypeParameterBoundIndex() {
    return (targetTypeAndInfo & 0x0000FF00) >> 8;
  }

  /**
   * 返回此类型引用所引用的类“超类型”的索引。此方法只能用于种类为 {@link #CLASS_EXTENDS} 的类型引用。
   *
   * @return 类 'implements' 子句中接口的索引；若此类型引用引用的是超类类型则为 -1。
   */
  public int getSuperTypeIndex() {
    return (short) ((targetTypeAndInfo & 0x00FFFF00) >> 8);
  }

  /**
   * 返回此类型引用所引用其类型的形参的索引。此方法只能用于种类为 {@link #METHOD_FORMAL_PARAMETER} 的
   * 类型引用。
   *
   * @return 形式参数索引。
   */
  public int getFormalParameterIndex() {
    return (targetTypeAndInfo & 0x00FF0000) >> 16;
  }

  /**
   * 返回方法 'throws' 子句中其类型被此类型引用所引用的异常的索引。此方法只能用于种类为
   * {@link #THROWS} 的类型引用。
   *
   * @return 方法 'throws' 子句中异常的索引。
   */
  public int getExceptionIndex() {
    return (targetTypeAndInfo & 0x00FFFF00) >> 8;
  }

  /**
   * 返回其 'catch' 类型被此类型引用所引用的 try-catch 块的索引（采用 visitTryCatchBlock 访问它们的顺
   * 序）。此方法只能用于种类为 {@link #EXCEPTION_PARAMETER} 的类型引用。
   *
   * @return 方法 'throws' 子句中异常的索引。
   */
  public int getTryCatchBlockIndex() {
    return (targetTypeAndInfo & 0x00FFFF00) >> 8;
  }

  /**
   * 返回此类型引用所引用类型参数的索引。此方法仅适用于如下种类的类型引用：{@link #CAST}、
   * {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、{@link #METHOD_INVOCATION_TYPE_ARGUMENT}、
   * {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}、{@link #METHOD_REFERENCE_TYPE_ARGUMENT}。
   *
   * @return 类型参数索引。
   */
  public int getTypeArgumentIndex() {
    return targetTypeAndInfo & 0xFF;
  }

  /**
   * 返回此类型引用的 int 编码值，适用于与类型注解相关的 visit 方法，例如 visitTypeAnnotation。
   *
   * @return 此类型引用的 int 编码值。
   */
  public int getValue() {
    return targetTypeAndInfo;
  }

  /**
   * 将给定的 target_type 和 target_info JVMS 结构放入给定的 ByteVector。
   *
   * @param targetTypeAndInfo 按 {@link #targetTypeAndInfo} 中方式编码的 target_type 和 target_info 结构。
   * 不支持 LOCAL_VARIABLE 和 RESOURCE_VARIABLE 目标类型。
   * @param output 必须写入类型引用的目标。
   */
  static void putTarget(final int targetTypeAndInfo, final ByteVector output) {
    switch (targetTypeAndInfo >>> 24) {
      case CLASS_TYPE_PARAMETER:
      case METHOD_TYPE_PARAMETER:
      case METHOD_FORMAL_PARAMETER:
        output.putShort(targetTypeAndInfo >>> 16);
        break;
      case FIELD:
      case METHOD_RETURN:
      case METHOD_RECEIVER:
        output.putByte(targetTypeAndInfo >>> 24);
        break;
      case CAST:
      case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
      case METHOD_INVOCATION_TYPE_ARGUMENT:
      case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
      case METHOD_REFERENCE_TYPE_ARGUMENT:
        output.putInt(targetTypeAndInfo);
        break;
      case CLASS_EXTENDS:
      case CLASS_TYPE_PARAMETER_BOUND:
      case METHOD_TYPE_PARAMETER_BOUND:
      case THROWS:
      case EXCEPTION_PARAMETER:
      case INSTANCEOF:
      case NEW:
      case CONSTRUCTOR_REFERENCE:
      case METHOD_REFERENCE:
        output.put12(targetTypeAndInfo >>> 24, (targetTypeAndInfo & 0xFFFF00) >> 8);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }
}
