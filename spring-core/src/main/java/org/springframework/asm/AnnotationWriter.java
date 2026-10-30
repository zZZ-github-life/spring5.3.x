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
 * 一个 {@link AnnotationVisitor}，用于生成 Java 虚拟机规范（JVMS）中定义的相应 'annotation' 或
 * 'type_annotation' 结构。AnnotationWriter 实例可以链接成双向链表，通过 {@link #putAnnotations} 方
 * 法可以从该链表生成 Runtime[In]Visible[Type]Annotations 属性。类似地，这类链表的数组可用于生成
 * Runtime[In]VisibleParameterAnnotations 属性。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16">JVMS
 *     4.7.16</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
 *     4.7.20</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class AnnotationWriter extends AnnotationVisitor {

  /** 存储此 AnnotationWriter 所用常量的位置。 */
  private final SymbolTable symbolTable;

  /**
   * 值是否带名称。用于注解默认值和注解数组的 AnnotationWriter 实例使用无名值（即为每个值生成一个
   * 'element_value' 结构，而不是先给出 element_name_index 再跟一个 element_value）。
   */
  private final boolean useNamedValues;

  /**
   * 与目前已访问的注解值相对应的 'annotation' 或 'type_annotation' JVMS 结构。这些结构的所有字段（最
   * 后一个字段 element_value_pairs 数组除外）都必须在此 ByteVector 传递给构造函数之前设置好（
   * num_element_value_pairs 可以设为 0，它会在 {@link #visitEnd()} 中被重置为正确的值）。
   * element_value_pairs 数组在各 visit() 方法中逐步填充。
   *
   * <p>注意：作为上述规则的例外，对于 AnnotationDefault 属性（按定义它只包含一个 element_value），此
   * ByteVector 在传递给构造函数时初始为空，并且 {@link #numElementValuePairsOffset} 被设置为 -1。
   */
  private final ByteVector annotation;

  /**
   * 在 {@link #annotation} 中存储 {@link #numElementValuePairs} 的偏移量（对于 AnnotationDefault 属
   * 性的情况为 -1）。
   */
  private final int numElementValuePairsOffset;

  /** 目前已访问的元素值对数量。 */
  private int numElementValuePairs;

  /**
   * 前一个 AnnotationWriter。此字段用于存储某个 Runtime[In]Visible[Type]Annotations 属性的注解列表。
   * 对于嵌套注解或数组注解（注解类型的注解值）以及 AnnotationDefault 属性，此字段未被使用。
   */
  private final AnnotationWriter previousAnnotation;

  /**
   * 下一个 AnnotationWriter。此字段用于存储某个 Runtime[In]Visible[Type]Annotations 属性的注解列表。
   * 对于嵌套注解或数组注解（注解类型的注解值）以及 AnnotationDefault 属性，此字段未被使用。
   */
  private AnnotationWriter nextAnnotation;

  // -----------------------------------------------------------------------------------------------
  // Constructors and factories
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link AnnotationWriter}。
   *
   * @param symbolTable 用于存储此 AnnotationWriter 中所用常量的位置。
   * @param useNamedValues 值是否带有名称。AnnotationDefault 和注解数组使用不带名称的值。
   * @param annotation 用于存储与所访问内容对应的 'annotation' 或 'type_annotation' JVMS 结构的位置。
   * 该 ByteVector 必须已包含该结构除最后一个字段（element_value_pairs 数组）之外的
   * 所有字段。
   * @param previousAnnotation 此注解所属的 Runtime[In]Visible[Type]Annotations 属性中先前访问的注解；
   * 在其他情况下（例如嵌套注解或数组注解）为 {@literal null}。
   */
  AnnotationWriter(
      final SymbolTable symbolTable,
      final boolean useNamedValues,
      final ByteVector annotation,
      final AnnotationWriter previousAnnotation) {
    super(/* latest api = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.useNamedValues = useNamedValues;
    this.annotation = annotation;
    // By hypothesis, num_element_value_pairs is stored in the last unsigned short of 'annotation'.
    this.numElementValuePairsOffset = annotation.length == 0 ? -1 : annotation.length - 2;
    this.previousAnnotation = previousAnnotation;
    if (previousAnnotation != null) {
      previousAnnotation.nextAnnotation = this;
    }
  }

  /**
   * 使用带名称的值创建一个新的 {@link AnnotationWriter}。
   *
   * @param symbolTable 用于存储此 AnnotationWriter 中所用常量的位置。
   * @param descriptor 注解类的类描述符。
   * @param previousAnnotation 此注解所属的 Runtime[In]Visible[Type]Annotations 属性中先前访问的注解；
   * 在其他情况下（例如嵌套注解或数组注解）为 {@literal null}。
   * @return 针对给定注解描述符的新 {@link AnnotationWriter}。
   */
  static AnnotationWriter create(
      final SymbolTable symbolTable,
      final String descriptor,
      final AnnotationWriter previousAnnotation) {
    // Create a ByteVector to hold an 'annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.
    ByteVector annotation = new ByteVector();
    // Write type_index and reserve space for num_element_value_pairs.
    annotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(
        symbolTable, /* useNamedValues = */ true, annotation, previousAnnotation);
  }

  /**
   * 使用带名称的值创建一个新的 {@link AnnotationWriter}。
   *
   * @param symbolTable 用于存储此 AnnotationWriter 中所用常量的位置。
   * @param typeRef 对被注解类型的引用。此类型引用的种类必须是
   * {@link TypeReference#CLASS_TYPE_PARAMETER}、
   * {@link TypeReference#CLASS_TYPE_PARAMETER_BOUND} 或 {@link TypeReference#CLASS_EXTENDS}。
   * 参见 {@link TypeReference}。
   * @param typePath 指向 'typeRef' 中被注解的类型参数、通配符边界、数组元素类型或静态内部类型的路径。
   * 如果注解以整个 'typeRef' 为目标，则可以为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param previousAnnotation 此注解所属的 Runtime[In]Visible[Type]Annotations 属性中先前访问的注解；
   * 在其他情况下（例如嵌套注解或数组注解）为 {@literal null}。
   * @return 针对给定类型注解引用与描述符的新 {@link AnnotationWriter}。
   */
  static AnnotationWriter create(
      final SymbolTable symbolTable,
      final int typeRef,
      final TypePath typePath,
      final String descriptor,
      final AnnotationWriter previousAnnotation) {
    // Create a ByteVector to hold a 'type_annotation' JVMS structure.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.
    ByteVector typeAnnotation = new ByteVector();
    // Write target_type, target_info, and target_path.
    TypeReference.putTarget(typeRef, typeAnnotation);
    TypePath.put(typePath, typeAnnotation);
    // Write type_index and reserve space for num_element_value_pairs.
    typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(
        symbolTable, /* useNamedValues = */ true, typeAnnotation, previousAnnotation);
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the AnnotationVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(final String name, final Object value) {
    // Case of an element_value with a const_value_index, class_info_index or array_index field.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    if (value instanceof String) {
      annotation.put12('s', symbolTable.addConstantUtf8((String) value));
    } else if (value instanceof Byte) {
      annotation.put12('B', symbolTable.addConstantInteger(((Byte) value).byteValue()).index);
    } else if (value instanceof Boolean) {
      int booleanValue = ((Boolean) value).booleanValue() ? 1 : 0;
      annotation.put12('Z', symbolTable.addConstantInteger(booleanValue).index);
    } else if (value instanceof Character) {
      annotation.put12('C', symbolTable.addConstantInteger(((Character) value).charValue()).index);
    } else if (value instanceof Short) {
      annotation.put12('S', symbolTable.addConstantInteger(((Short) value).shortValue()).index);
    } else if (value instanceof Type) {
      annotation.put12('c', symbolTable.addConstantUtf8(((Type) value).getDescriptor()));
    } else if (value instanceof byte[]) {
      byte[] byteArray = (byte[]) value;
      annotation.put12('[', byteArray.length);
      for (byte byteValue : byteArray) {
        annotation.put12('B', symbolTable.addConstantInteger(byteValue).index);
      }
    } else if (value instanceof boolean[]) {
      boolean[] booleanArray = (boolean[]) value;
      annotation.put12('[', booleanArray.length);
      for (boolean booleanValue : booleanArray) {
        annotation.put12('Z', symbolTable.addConstantInteger(booleanValue ? 1 : 0).index);
      }
    } else if (value instanceof short[]) {
      short[] shortArray = (short[]) value;
      annotation.put12('[', shortArray.length);
      for (short shortValue : shortArray) {
        annotation.put12('S', symbolTable.addConstantInteger(shortValue).index);
      }
    } else if (value instanceof char[]) {
      char[] charArray = (char[]) value;
      annotation.put12('[', charArray.length);
      for (char charValue : charArray) {
        annotation.put12('C', symbolTable.addConstantInteger(charValue).index);
      }
    } else if (value instanceof int[]) {
      int[] intArray = (int[]) value;
      annotation.put12('[', intArray.length);
      for (int intValue : intArray) {
        annotation.put12('I', symbolTable.addConstantInteger(intValue).index);
      }
    } else if (value instanceof long[]) {
      long[] longArray = (long[]) value;
      annotation.put12('[', longArray.length);
      for (long longValue : longArray) {
        annotation.put12('J', symbolTable.addConstantLong(longValue).index);
      }
    } else if (value instanceof float[]) {
      float[] floatArray = (float[]) value;
      annotation.put12('[', floatArray.length);
      for (float floatValue : floatArray) {
        annotation.put12('F', symbolTable.addConstantFloat(floatValue).index);
      }
    } else if (value instanceof double[]) {
      double[] doubleArray = (double[]) value;
      annotation.put12('[', doubleArray.length);
      for (double doubleValue : doubleArray) {
        annotation.put12('D', symbolTable.addConstantDouble(doubleValue).index);
      }
    } else {
      Symbol symbol = symbolTable.addConstant(value);
      annotation.put12(".s.IFJDCS".charAt(symbol.tag), symbol.index);
    }
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    // Case of an element_value with an enum_const_value field.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    annotation
        .put12('e', symbolTable.addConstantUtf8(descriptor))
        .putShort(symbolTable.addConstantUtf8(value));
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    // Case of an element_value with an annotation_value field.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    // Write tag and type_index, and reserve 2 bytes for num_element_value_pairs.
    annotation.put12('@', symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ true, annotation, null);
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    // Case of an element_value with an array_value field.
    // https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    // Write tag, and reserve 2 bytes for num_values. Here we take advantage of the fact that the
    // end of an element_value of array type is similar to the end of an 'annotation' structure: an
    // unsigned short num_values followed by num_values element_value, versus an unsigned short
    // num_element_value_pairs, followed by num_element_value_pairs { element_name_index,
    // element_value } tuples. This allows us to use an AnnotationWriter with unnamed values to
    // visit the array elements. Its num_element_value_pairs will correspond to the number of array
    // elements and will be stored in what is in fact num_values.
    annotation.put12('[', 0);
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ false, annotation, null);
  }

  @Override
  public void visitEnd() {
    if (numElementValuePairsOffset != -1) {
      byte[] data = annotation.data;
      data[numElementValuePairsOffset] = (byte) (numElementValuePairs >>> 8);
      data[numElementValuePairsOffset + 1] = (byte) numElementValuePairs;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回包含此注解及其所有<i>前驱</i>（参见 {@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations 属性的大小。还会将属性名添加到类的常量池中（若不为 null）。
   *
   * @param attributeName “Runtime[In]Visible[Type]Annotations”之一，或 {@literal null}。
   * @return 包含此注解及其所有前驱的 Runtime[In]Visible[Type]Annotations 属性的字节大小。
   * 其中包括 attribute_name_index 和 attribute_length 字段的大小。
   */
  int computeAnnotationsSize(final String attributeName) {
    if (attributeName != null) {
      symbolTable.addConstantUtf8(attributeName);
    }
    // The attribute_name_index, attribute_length and num_annotations fields use 8 bytes.
    int attributeSize = 8;
    AnnotationWriter annotationWriter = this;
    while (annotationWriter != null) {
      attributeSize += annotationWriter.annotation.length;
      annotationWriter = annotationWriter.previousAnnotation;
    }
    return attributeSize;
  }

  /**
   * 返回包含给定注解及其所有<i>前驱</i>（参见 {@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations 属性的大小。还会将这些属性名添加到类的常量池中（如果不为
   * null）。
   *
   * @param lastRuntimeVisibleAnnotation 字段、方法或类的最后一个运行时可见注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param lastRuntimeInvisibleAnnotation 字段、方法或类的最后一个运行时不可见注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param lastRuntimeVisibleTypeAnnotation 字段、方法或类的最后一个运行时可见类型注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param lastRuntimeInvisibleTypeAnnotation 字段、方法或类的最后一个运行时不可见类型注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @return 包含给定注解及其所有前驱的 Runtime[In]Visible[Type]Annotations 属性的字节大小。
   * 其中包括 attribute_name_index 和 attribute_length 字段的大小。
   */
  static int computeAnnotationsSize(
      final AnnotationWriter lastRuntimeVisibleAnnotation,
      final AnnotationWriter lastRuntimeInvisibleAnnotation,
      final AnnotationWriter lastRuntimeVisibleTypeAnnotation,
      final AnnotationWriter lastRuntimeInvisibleTypeAnnotation) {
    int size = 0;
    if (lastRuntimeVisibleAnnotation != null) {
      size +=
          lastRuntimeVisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      size +=
          lastRuntimeInvisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      size +=
          lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      size +=
          lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
    }
    return size;
  }

  /**
   * 将包含此注解及其所有<i>前驱</i>（参见 {@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations 属性放入给定的 ByteVector。注解按照它们被访问的顺序放入。
   *
   * @param attributeNameIndex 属性名称的常量池索引（“Runtime[In]Visible[Type]Annotations”之一）。
   * @param output 放置该属性的位置。
   */
  void putAnnotations(final int attributeNameIndex, final ByteVector output) {
    int attributeLength = 2; // For num_annotations.
    int numAnnotations = 0;
    AnnotationWriter annotationWriter = this;
    AnnotationWriter firstAnnotation = null;
    while (annotationWriter != null) {
      // In case the user forgot to call visitEnd().
      annotationWriter.visitEnd();
      attributeLength += annotationWriter.annotation.length;
      numAnnotations++;
      firstAnnotation = annotationWriter;
      annotationWriter = annotationWriter.previousAnnotation;
    }
    output.putShort(attributeNameIndex);
    output.putInt(attributeLength);
    output.putShort(numAnnotations);
    annotationWriter = firstAnnotation;
    while (annotationWriter != null) {
      output.putByteArray(annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
      annotationWriter = annotationWriter.nextAnnotation;
    }
  }

  /**
   * 将包含给定注解及其所有<i>前驱</i>（参见 {@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations 属性放入给定的 ByteVector。注解按照它们被访问的顺序放入。
   *
   * @param symbolTable 存储 AnnotationWriter 实例中所用常量的位置。
   * @param lastRuntimeVisibleAnnotation 字段、方法或类的最后一个运行时可见注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param lastRuntimeInvisibleAnnotation 字段、方法或类的最后一个运行时不可见注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param lastRuntimeVisibleTypeAnnotation 字段、方法或类的最后一个运行时可见类型注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param lastRuntimeInvisibleTypeAnnotation 字段、方法或类的最后一个运行时不可见类型注解。先前的注解可通过
   * {@link #previousAnnotation} 字段访问。可以为 {@literal null}。
   * @param output 放置这些属性的位置。
   */
  static void putAnnotations(
      final SymbolTable symbolTable,
      final AnnotationWriter lastRuntimeVisibleAnnotation,
      final AnnotationWriter lastRuntimeInvisibleAnnotation,
      final AnnotationWriter lastRuntimeVisibleTypeAnnotation,
      final AnnotationWriter lastRuntimeInvisibleTypeAnnotation,
      final ByteVector output) {
    if (lastRuntimeVisibleAnnotation != null) {
      lastRuntimeVisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_ANNOTATIONS), output);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      lastRuntimeInvisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_ANNOTATIONS), output);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      lastRuntimeVisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      lastRuntimeInvisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output);
    }
  }

  /**
   * 返回包含给定 AnnotationWriter 子数组中所有注解列表的 Runtime[In]VisibleParameterAnnotations 属性
   * 的大小。还会将该属性名添加到类的常量池中。
   *
   * @param attributeName “Runtime[In]VisibleParameterAnnotations”之一。
   * @param annotationWriters AnnotationWriter 列表组成的数组（以其<i>最后</i>一个元素标识）。
   * @param annotableParameterCount annotationWriters 中要考虑的元素个数（考虑元素 [0..annotableParameterCount[）。
   * @return 与给定 AnnotationWriter 列表子数组对应的 Runtime[In]VisibleParameterAnnotations
   * 属性的字节大小。其中包括 attribute_name_index 和 attribute_length 字段的大小。
   */
  static int computeParameterAnnotationsSize(
      final String attributeName,
      final AnnotationWriter[] annotationWriters,
      final int annotableParameterCount) {
    // Note: attributeName is added to the constant pool by the call to computeAnnotationsSize
    // below. This assumes that there is at least one non-null element in the annotationWriters
    // sub-array (which is ensured by the lazy instantiation of this array in MethodWriter).
    // The attribute_name_index, attribute_length and num_parameters fields use 7 bytes, and each
    // element of the parameter_annotations array uses 2 bytes for its num_annotations field.
    int attributeSize = 7 + 2 * annotableParameterCount;
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      attributeSize +=
          annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(attributeName) - 8;
    }
    return attributeSize;
  }

  /**
   * 将包含给定 AnnotationWriter 子数组中所有注解列表的 Runtime[In]VisibleParameterAnnotations 属性放
   * 入给定的 ByteVector。
   *
   * @param attributeNameIndex 属性名称的常量池索引（Runtime[In]VisibleParameterAnnotations 之一）。
   * @param annotationWriters AnnotationWriter 列表组成的数组（以其<i>最后</i>一个元素标识）。
   * @param annotableParameterCount annotationWriters 中要写入的元素个数（写入元素 [0..annotableParameterCount[）。
   * @param output 放置该属性的位置。
   */
  static void putParameterAnnotations(
      final int attributeNameIndex,
      final AnnotationWriter[] annotationWriters,
      final int annotableParameterCount,
      final ByteVector output) {
    // The num_parameters field uses 1 byte, and each element of the parameter_annotations array
    // uses 2 bytes for its num_annotations field.
    int attributeLength = 1 + 2 * annotableParameterCount;
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      attributeLength +=
          annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(null) - 8;
    }
    output.putShort(attributeNameIndex);
    output.putInt(attributeLength);
    output.putByte(annotableParameterCount);
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      AnnotationWriter firstAnnotation = null;
      int numAnnotations = 0;
      while (annotationWriter != null) {
        // In case user the forgot to call visitEnd().
        annotationWriter.visitEnd();
        numAnnotations++;
        firstAnnotation = annotationWriter;
        annotationWriter = annotationWriter.previousAnnotation;
      }
      output.putShort(numAnnotations);
      annotationWriter = firstAnnotation;
      while (annotationWriter != null) {
        output.putByteArray(
            annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
        annotationWriter = annotationWriter.nextAnnotation;
      }
    }
  }
}
