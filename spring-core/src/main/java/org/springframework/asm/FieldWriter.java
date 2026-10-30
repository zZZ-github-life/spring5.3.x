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
 * 一个 {@link FieldVisitor}，用于生成与 Java 虚拟机规范（JVMS）中定义相对应的 'field_info' 结构。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5">JVMS
 *     4.5</a>
 * @author Eric Bruneton
 */
final class FieldWriter extends FieldVisitor {

  /** 存储此 FieldWriter 所用常量的位置。 */
  private final SymbolTable symbolTable;

  // Note: fields are ordered as in the field_info structure, and those related to attributes are
  // ordered as in Section 4.7 of the JVMS.

  /**
   * field_info JVMS 结构的 access_flags 字段。此字段可以包含 ASM 特有的访问标志，例如
   * {@link Opcodes#ACC_DEPRECATED}，这些标志在生成 ClassFile 结构时会被移除。
   */
  private final int accessFlags;

  /** field_info JVMS 结构的 name_index 字段。 */
  private final int nameIndex;

  /** field_info JVMS 结构的 descriptor_index 字段。 */
  private final int descriptorIndex;

  /**
   * 此 field_info 的 Signature 属性的 signature_index 字段，如果没有 Signature 属性则为 0。
   */
  private int signatureIndex;

  /**
   * 此 field_info 的 ConstantValue 属性的 constantvalue_index 字段，如果没有 ConstantValue 属性则为
   * 0。
   */
  private int constantValueIndex;

  /**
   * 此字段最后一个运行时可见注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 字
   * 段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 此字段最后一个运行时不可见注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation}
   * 字段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * 此字段最后一个运行时可见类型注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation}
   * 字段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 此字段最后一个运行时不可见类型注解。之前的注解可以通过
   * {@link AnnotationWriter#previousAnnotation} 字段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * 此字段第一个非标准属性。后续属性可以通过 {@link Attribute#nextAttribute} 字段访问。可能为
   * {@literal null}。
   *
   * <p><b>警告</b>：此链表以与其访问顺序<i>相反</i>的顺序存储属性。firstAttribute 实际上是
   * {@link #visitAttribute} 中最后访问的属性。{@link #putFieldInfo} 方法按此链表定义的顺序写入属性，
   * 也就是用户指定的相反顺序。
   */
  private Attribute firstAttribute;

  // -----------------------------------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link FieldWriter}。
   *
   * @param symbolTable 用于存储此 FieldWriter 中所用常量的位置。
   * @param access 字段的访问标志（参见 {@link Opcodes}）。
   * @param name 字段的名称。
   * @param descriptor 字段的描述符（参见 {@link Type}）。
   * @param signature 字段的签名。可以为 {@literal null}。
   * @param constantValue 字段的常量值。可以为 {@literal null}。
   */
  FieldWriter(
      final SymbolTable symbolTable,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object constantValue) {
    super(/* latest api = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.accessFlags = access;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
    if (constantValue != null) {
      this.constantValueIndex = symbolTable.addConstant(constantValue).index;
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
   * 返回此 FieldWriter 生成的 field_info JVMS 结构的大小。同时还会将该字段各属性的名称添加到常量池中
   * 。
   *
   * @return field_info JVMS 结构的字节大小。
   */
  int computeFieldInfoSize() {
    // The access_flags, name_index, descriptor_index and attributes_count fields use 8 bytes.
    int size = 8;
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    if (constantValueIndex != 0) {
      // ConstantValue attributes always use 8 bytes.
      symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE);
      size += 8;
    }
    size += Attribute.computeAttributesSize(symbolTable, accessFlags, signatureIndex);
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
   * 将此 FieldWriter 生成的 field_info JVMS 结构的内容放入给定的 ByteVector 中。
   *
   * @param output 放置 field_info 结构的位置。
   */
  void putFieldInfo(final ByteVector output) {
    boolean useSyntheticAttribute = symbolTable.getMajorVersion() < Opcodes.V1_5;
    // Put the access_flags, name_index and descriptor_index fields.
    int mask = useSyntheticAttribute ? Opcodes.ACC_SYNTHETIC : 0;
    output.putShort(accessFlags & ~mask).putShort(nameIndex).putShort(descriptorIndex);
    // Compute and put the attributes_count field.
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    int attributesCount = 0;
    if (constantValueIndex != 0) {
      ++attributesCount;
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
      ++attributesCount;
    }
    if (signatureIndex != 0) {
      ++attributesCount;
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
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
    // Put the field_info attributes.
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    if (constantValueIndex != 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE))
          .putInt(2)
          .putShort(constantValueIndex);
    }
    Attribute.putAttributes(symbolTable, accessFlags, signatureIndex, output);
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
   * 将此字段的属性收集到给定的属性原型集合中。
   *
   * @param attributePrototypes 一组属性原型。
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
  }
}
