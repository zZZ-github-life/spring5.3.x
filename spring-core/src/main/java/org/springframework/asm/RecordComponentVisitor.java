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
 * 用于访问记录组件的访问器。此类的方法必须按以下顺序调用：( {@code visitAnnotation} |
 * {@code visitTypeAnnotation} | {@code visitAttribute} )* {@code visitEnd}。
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public abstract class RecordComponentVisitor {
  /**
   * 此访问器实现的 ASM API 版本。此字段的值必须是 {@link Opcodes#ASM8} 或
   * {@link Opcodes#ASM9} 之一。
   */
  protected final int api;

  /**
   * 此访问器必须将方法调用委托给它的记录访问器。可以为 {@literal null}。
   */
  protected RecordComponentVisitor delegate;

  /**
   * 构造一个新的 {@link RecordComponentVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link Opcodes#ASM8}
   *     or {@link Opcodes#ASM9}.
   */
  protected RecordComponentVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link RecordComponentVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be {@link Opcodes#ASM8}.
   * @param recordComponentVisitor the record component visitor to which this visitor must delegate
   *     method calls. May be null.
   */
  protected RecordComponentVisitor(
      final int api, final RecordComponentVisitor recordComponentVisitor) {
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
    this.delegate = recordComponentVisitor;
  }

  /**
   * 此访问器必须将方法调用委托给它的记录访问器。可以为 {@literal null}。
   *
   * @return the record visitor to which this visitor must delegate method calls, or {@literal
   *     null}.
   */
  public RecordComponentVisitor getDelegate() {
    return delegate;
  }

  /**
   * 访问记录组件的一个注解。
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问记录组件签名中某个类型上的注解。
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   *     {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
   *     TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
   *     {@link TypeReference}.
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
    if (delegate != null) {
      return delegate.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问记录组件的一个非标准属性。
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (delegate != null) {
      delegate.visitAttribute(attribute);
    }
  }

  /**
   * 访问记录组件的结尾。此方法是最后一个被调用的方法，用于告知访问器所有内容都已访问完毕。
   */
  public void visitEnd() {
    if (delegate != null) {
      delegate.visitEnd();
    }
  }
}
