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
 * 用于访问 Java 字段的访问器。此类的方法必须按以下顺序调用：( {@code visitAnnotation} |
 * {@code visitTypeAnnotation} | {@code visitAttribute} )* {@code visitEnd}。
 *
 * @author Eric Bruneton
 */
public abstract class FieldVisitor {

  /**
   * 此访问器实现的 ASM API 版本。此字段的值必须是 {@link Opcodes} 中 {@code ASM}<i>x</i> 值之一。
   */
  protected final int api;

  /** 此访问器必须将方法调用委托给的字段访问器。可能为 {@literal null}。 */
  protected FieldVisitor fv;

  /**
   * 构造一个新的 {@link FieldVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   */
  protected FieldVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link FieldVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   * @param fieldVisitor the field visitor to which this visitor must delegate method calls. May be
   *     null.
   */
  protected FieldVisitor(final int api, final FieldVisitor fieldVisitor) {
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
    this.fv = fieldVisitor;
  }

  /**
   * 此访问器必须将方法调用委托给该字段访问器。可能为 {@literal null}。
   *
   * @return the field visitor to which this visitor must delegate method calls, or {@literal null}.
   */
  public FieldVisitor getDelegate() {
    return fv;
  }

  /**
   * 访问字段的一个注解。
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (fv != null) {
      return fv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问字段类型上的一个注解。
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   *     {@link TypeReference#FIELD}. See {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   *     'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException("This feature requires ASM5");
    }
    if (fv != null) {
      return fv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问字段的一个非标准属性。
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (fv != null) {
      fv.visitAttribute(attribute);
    }
  }

  /**
   * 访问字段的结束。此方法是最后一个被调用的方法，用于通知访问器该字段的所有注解和属性都已访问完毕。
   */
  public void visitEnd() {
    if (fv != null) {
      fv.visitEnd();
    }
  }
}
