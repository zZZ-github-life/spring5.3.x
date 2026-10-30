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

final class RecordComponentWriter extends RecordComponentVisitor {
  /** 存储此 RecordComponentWriter 所用常量的位置。 */
  private final SymbolTable symbolTable;

  // Note: fields are ordered as in the record_component_info structure, and those related to
  // attributes are ordered as in Section 4.7 of the JVMS.

  /** Record 属性的 name_index 字段。 */
  private final int nameIndex;

  /** Record 属性的 descriptor_index 字段。 */
  private final int descriptorIndex;

  /**
   * 此记录组件 Signature 属性的 signature_index 字段；如果没有 Signature 属性，则为 0。
   */
  private int signatureIndex;

  /**
   * 此记录组件最后一个运行时可见注解。之前的注解可通过 {@link AnnotationWriter#previousAnnotation}
   * 字段访问。可以为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 此记录组件最后一个运行时不可见注解。之前的注解可通过 {@link AnnotationWriter#previousAnnotation}
   * 字段访问。可以为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * 此记录组件最后一个运行时可见类型注解。之前的注解可通过
   * {@link AnnotationWriter#previousAnnotation} 字段访问。可以为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 此记录组件最后一个运行时不可见类型注解。之前的注解可通过
   * {@link AnnotationWriter#previousAnnotation} 字段访问。可以为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * 此记录组件的第一个非标准属性。后续属性可通过 {@link Attribute#nextAttribute} 字段访问。可以为
   * {@literal null}。
   *
   * <p><b>WARNING</b>：此列表按访问顺序的<i>相反</i>顺序存储属性。firstAttribute 实际上是
   * {@link #visitAttribute(Attribute)} 中最后访问的属性。{@link #putRecordComponentInfo(ByteVector)}
   * 方法按此列表定义的顺序写入属性，即用户指定的相反顺序。
   */
  private Attribute firstAttribute;

  /**
   * 构造一个新的 {@link RecordComponentWriter}。
   *
   * @param symbolTable 用于存储此 RecordComponentWriter 中所用常量的位置。
   * @param name 记录组件的名称。
   * @param descriptor 记录组件的描述符（参见 {@link Type}）。
   * @param signature 记录组件的签名。可以为 {@literal null}。
   */
  RecordComponentWriter(
      final SymbolTable symbolTable,
      final String name,
      final String descriptor,
      final String signature) {
    super(/* latest api = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the FieldVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
    } else {
      return lastRuntimeInvisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastRuntimeVisibleTypeAnnotation);
    } else {
      return lastRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    // Store the attributes in the <i>reverse</i> order of their visit by this method.
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回此 RecordComponentWriter 生成的记录组件 JVMS 结构的大小。同时将此记录组件各属性的名称添加到
   * 常量池中。
   *
   * @return Record 属性中 record_component_info 的字节大小。
   */
  int computeRecordComponentInfoSize() {
    // name_index, descriptor_index and attributes_count fields use 6 bytes.
    int size = 6;
    size += Attribute.computeAttributesSize(symbolTable, 0, signatureIndex);
    size +=
        AnnotationWriter.computeAnnotationsSize(
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation);
    if (firstAttribute != null) {
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * 将此 RecordComponentWriter 生成的记录组件内容放入给定的 ByteVector 中。
   *
   * @param output 放置 record_component_info 结构的位置。
   */
  void putRecordComponentInfo(final ByteVector output) {
    output.putShort(nameIndex).putShort(descriptorIndex);
    // Compute and put the attributes_count field.
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    int attributesCount = 0;
    if (signatureIndex != 0) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
    }
    output.putShort(attributesCount);
    Attribute.putAttributes(symbolTable, 0, signatureIndex, output);
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        output);
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }

  /**
   * 将此记录组件的属性收集到给定的属性原型集合中。
   *
   * @param attributePrototypes 一组属性原型。
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
  }
}
