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
 * 用于访问 Java 注解的访问器。此类的方法必须按以下顺序调用： ( {@code visit} | {@code visitEnum} |
 * {@code visitAnnotation} | {@code visitArray} )* {@code visitEnd}。
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public abstract class AnnotationVisitor {

  /**
   * 此访问器实现的 ASM API 版本。此字段的值必须是 {@link Opcodes} 中的某个 {@code ASM}<i>x</i> 值。
   */
  protected final int api;

  /**
   * 此访问器必须将方法调用委托给它的注解访问器。可以为 {@literal null}。
   */
  protected AnnotationVisitor av;

  /**
   * 构造一个新的 {@link AnnotationVisitor}。
   *
   * @param api 此访问器实现的 ASM API 版本，必须是 {@link Opcodes} 中 {@code ASM}<i>x</i> 值之一。
   */
  protected AnnotationVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link AnnotationVisitor}。
   *
   * @param api 此访问器实现的 ASM API 版本，必须是 {@link Opcodes} 中 {@code ASM}<i>x</i> 值之一。
   * @param annotationVisitor 此访问器必须将方法调用委托给的注解访问器。可以为 {@literal null}。
   */
  protected AnnotationVisitor(final int api, final AnnotationVisitor annotationVisitor) {
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
    this.av = annotationVisitor;
  }

  /**
   * 此访问器必须将方法调用委托给它的注解访问器。可以为 {@literal null}。
   *
   * @return 此访问器必须将方法调用委托给的注解访问器，或 {@literal null}。
   */
  public AnnotationVisitor getDelegate() {
    return av;
  }

  /**
   * 访问注解的一个基本类型值。
   *
   * @param name 值的名称。
   * @param value 实际值，其类型必须是 {@link Byte}、{@link Boolean}、{@link Character}、{@link Short}、
   * {@link Integer}、{@link Long}、{@link Float}、{@link Double}、{@link String}，或种类为
   * {@link Type#OBJECT} 或 {@link Type#ARRAY} 的 {@link Type}。该值也可以是 byte、
   * boolean、short、char、int、long、float 或 double 值的数组（等价于使用
   * {@link #visitArray} 并依次访问每个数组元素，但更方便）。
   */
  public void visit(final String name, final Object value) {
    if (av != null) {
      av.visit(name, value);
    }
  }

  /**
   * 访问注解的一个枚举值。
   *
   * @param name 值的名称。
   * @param descriptor 枚举类的类描述符。
   * @param value 实际的枚举值。
   */
  public void visitEnum(final String name, final String descriptor, final String value) {
    if (av != null) {
      av.visitEnum(name, descriptor, value);
    }
  }

  /**
   * 访问注解的一个嵌套注解值。
   *
   * @param name 值的名称。
   * @param descriptor 嵌套注解类的类描述符。
   * @return 用于访问实际嵌套注解值的访问器；如果此访问器不关心访问该嵌套注解，则为
   * {@literal null}。<i>在对此注解访问器调用其他方法之前，必须先完整访问该嵌套注解值</i>。
   */
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    if (av != null) {
      return av.visitAnnotation(name, descriptor);
    }
    return null;
  }

  /**
   * 访问注解的一个数组值。请注意，基本类型值数组（例如 byte、boolean、short、char、int、long、float
   * 或 double）可以作为 value 传递给 {@link #visit visit}。对于非空的基本类型值数组，
   * {@link ClassReader} 正是这样做的。
   *
   * @param name 值的名称。
   * @return 用于访问实际数组值元素的访问器；如果此访问器不关心访问这些值，则为
   * {@literal null}。传递给此访问器各方法的 'name' 参数会被忽略。
   * <i>在对此注解访问器调用其他方法之前，必须先访问所有数组值</i>。
   */
  public AnnotationVisitor visitArray(final String name) {
    if (av != null) {
      return av.visitArray(name);
    }
    return null;
  }

  /** 访问注解的结尾。 */
  public void visitEnd() {
    if (av != null) {
      av.visitEnd();
    }
  }
}
