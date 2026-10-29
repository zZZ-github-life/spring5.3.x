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
 * 一个 {@link ClassVisitor}，用于生成与 Java 虚拟机规范（JVMS）中定义相对应的 ClassFile 结构。它既
 * 可以单独使用，从头生成一个 Java 类，也可以与一个或多个 {@link ClassReader} 及适配器
 * {@link ClassVisitor} 一起使用，从现有的一个或多个 Java 类生成修改后的类。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html">JVMS 4</a>
 * @author Eric Bruneton
 */
public class ClassWriter extends ClassVisitor {

  /**
   * 一个标志，用于自动计算方法的最大栈大小和最大局部变量数量。如果设置了此标志，那么
   * {@link #visitMethod} 方法返回的 {@link MethodVisitor} 的 {@link MethodVisitor#visitMaxs} 方法的
   * 参数将被忽略，并根据每个方法的签名和字节码自动计算。
   *
   * <p><b>注意：</b>对于版本为 {@link Opcodes#V1_7} 或更高的类，此选项要求存在有效的栈映射帧。此时最
   * 大栈大小将根据这些帧以及它们之间的字节码指令计算。如果栈映射帧不存在或必须重新计算，请改用
   * {@link #COMPUTE_FRAMES}。
   *
   * @see #ClassWriter(int)
   */
  public static final int COMPUTE_MAXS = 1;

  /**
   * 一个标志，用于从头自动计算方法的栈映射帧。如果设置了此标志，那么对
   * {@link MethodVisitor#visitFrame} 方法的调用将被忽略，栈映射帧将根据方法的字节码重新计算。
   * {@link MethodVisitor#visitMaxs} 方法的参数也会被忽略，并根据字节码重新计算。换言之，
   * {@link #COMPUTE_FRAMES} 蕴含 {@link #COMPUTE_MAXS}。
   *
   * @see #ClassWriter(int)
   */
  public static final int COMPUTE_FRAMES = 2;

  /**
   * 传递给构造函数的标志。必须是 {@link #COMPUTE_MAXS} 和 {@link #COMPUTE_FRAMES} 中的零个或多个。
   */
  private final int flags;

  // Note: fields are ordered as in the ClassFile structure, and those related to attributes are
  // ordered as in Section 4.7 of the JVMS.

  /**
   * JVMS ClassFile 结构的 minor_version 和 major_version 字段。minor_version 存储在最高的 16 位中，
   * major_version 存储在最低的 16 位中。
   */
  private int version;

  /** 此类的符号表（包含 constant_pool 和 BootstrapMethods）。 */
  private final SymbolTable symbolTable;

  /**
   * JVMS ClassFile 结构的 access_flags 字段。此字段可以包含 ASM 特有的访问标志，例如
   * {@link Opcodes#ACC_DEPRECATED} 或 {@link Opcodes#ACC_RECORD}，这些标志在生成 ClassFile 结构时会
   * 被移除。
   */
  private int accessFlags;

  /** JVMS ClassFile 结构的 this_class 字段。 */
  private int thisClass;

  /** JVMS ClassFile 结构的 super_class 字段。 */
  private int superClass;

  /** JVMS ClassFile 结构的 interface_count 字段。 */
  private int interfaceCount;

  /** JVMS ClassFile 结构的 'interfaces' 数组。 */
  private int[] interfaces;

  /**
   * 此类的字段，存储在通过各自 {@link FieldWriter#fv} 字段链接起来的 {@link FieldWriter} 链表中。此
   * 字段存储该链表的第一个元素。
   */
  private FieldWriter firstField;

  /**
   * 此类的字段，存储在通过各自 {@link FieldWriter#fv} 字段链接起来的 {@link FieldWriter} 链表中。此
   * 字段存储该链表的最后一个元素。
   */
  private FieldWriter lastField;

  /**
   * 此类的方法，存储在通过各自 {@link MethodWriter#mv} 字段链接起来的 {@link MethodWriter} 链表中。
   * 此字段存储该链表的第一个元素。
   */
  private MethodWriter firstMethod;

  /**
   * 此类的方法，存储在通过各自 {@link MethodWriter#mv} 字段链接起来的 {@link MethodWriter} 链表中。
   * 此字段存储该链表的最后一个元素。
   */
  private MethodWriter lastMethod;

  /** InnerClasses 属性的 number_of_classes 字段，或 0。 */
  private int numberOfInnerClasses;

  /** InnerClasses 属性的 'classes' 数组，或 {@literal null}。 */
  private ByteVector innerClasses;

  /** EnclosingMethod 属性的 class_index 字段，或 0。 */
  private int enclosingClassIndex;

  /** EnclosingMethod 属性的 method_index 字段。 */
  private int enclosingMethodIndex;

  /** Signature 属性的 signature_index 字段，或 0。 */
  private int signatureIndex;

  /** SourceFile 属性的 source_file_index 字段，或 0。 */
  private int sourceFileIndex;

  /** SourceDebugExtension 属性的 debug_extension 字段，或 {@literal null}。 */
  private ByteVector debugExtension;

  /**
   * 此类最后一个运行时可见注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 字段
   * 访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 此类最后一个运行时不可见注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 字
   * 段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * 此类最后一个运行时可见类型注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation}
   * 字段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 此类最后一个运行时不可见类型注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation}
   * 字段访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /** 此类的 Module 属性，或 {@literal null}。 */
  private ModuleWriter moduleWriter;

  /** NestHost 属性的 host_class_index 字段，或 0。 */
  private int nestHostClassIndex;

  /** NestMembers 属性的 number_of_classes 字段，或 0。 */
  private int numberOfNestMemberClasses;

  /** NestMembers 属性的 'classes' 数组，或 {@literal null}。 */
  private ByteVector nestMemberClasses;

  /** PermittedSubclasses 属性的 number_of_classes 字段，或 0。 */
  private int numberOfPermittedSubclasses;

  /** PermittedSubclasses 属性的 'classes' 数组，或 {@literal null}。 */
  private ByteVector permittedSubclasses;

  /**
   * 此类的记录组件，存储在通过各自 {@link RecordComponentWriter#delegate} 字段链接起来的
   * {@link RecordComponentWriter} 链表中。此字段存储该链表的第一个元素。
   */
  private RecordComponentWriter firstRecordComponent;

  /**
   * 此类的记录组件，存储在通过各自 {@link RecordComponentWriter#delegate} 字段链接起来的
   * {@link RecordComponentWriter} 链表中。此字段存储该链表的最后一个元素。
   */
  private RecordComponentWriter lastRecordComponent;

  /**
   * 此类第一个非标准属性。后续属性可以通过 {@link Attribute#nextAttribute} 字段访问。可能为
   * {@literal null}。
   *
   * <p><b>警告</b>：此链表以与其访问顺序<i>相反</i>的顺序存储属性。firstAttribute 实际上是
   * {@link #visitAttribute} 中最后访问的属性。{@link #toByteArray} 方法按此链表定义的顺序写入属性，
   * 也就是用户指定的相反顺序。
   */
  private Attribute firstAttribute;

  /**
   * 指示在 {@link MethodWriter} 中必须自动计算的内容。必须是 {@link MethodWriter#COMPUTE_NOTHING}、
   * {@link MethodWriter#COMPUTE_MAX_STACK_AND_LOCAL}、{@link MethodWriter#COMPUTE_INSERTED_FRAMES}
   * 或 {@link MethodWriter#COMPUTE_ALL_FRAMES} 之一。
   */
  private int compute;

  // -----------------------------------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link ClassWriter} 对象。
   *
   * @param flags option flags that can be used to modify the default behavior of this class. Must
   *     be zero or more of {@link #COMPUTE_MAXS} and {@link #COMPUTE_FRAMES}.
   */
  public ClassWriter(final int flags) {
    this(null, flags);
  }

  /**
   * 构造一个新的 {@link ClassWriter} 对象，并为“以添加为主”的字节码变换启用优化。这些优化如下：
   *
   * <ul>
   * <li>原始类的常量池和引导方法会原样复制到新类中，从而节省时间。如有必要，新的常量池项和引导方法会追加到末尾，但未使用的常量池项和引导方法<i>不会</i>被删除。
   * <li>未被变换的方法会原样从原始类字节码复制到新类中（即不为所有方法指令发出 visit 事件），这可以节省<i>大量</i>时间。未变换方法的判定方式是：{@link ClassReader} 收到的 {@link MethodVisitor} 对象来自 {@link ClassWriter}（而不是来自任何其他 {@link ClassVisitor} 实例）。
   * </ul>
   *
   * @param classReader the {@link ClassReader} used to read the original class. It will be used to
   *     copy the entire constant pool and bootstrap methods from the original class and also to
   *     copy other fragments of original bytecode where applicable.
   * @param flags option flags that can be used to modify the default behavior of this class. Must
   *     be zero or more of {@link #COMPUTE_MAXS} and {@link #COMPUTE_FRAMES}. <i>These option flags
   *     do not affect methods that are copied as is in the new class. This means that neither the
   *     maximum stack size nor the stack frames will be computed for these methods</i>.
   */
  public ClassWriter(final ClassReader classReader, final int flags) {
    super(/* latest api = */ Opcodes.ASM9);
    this.flags = flags;
    symbolTable = classReader == null ? new SymbolTable(this) : new SymbolTable(this, classReader);
    if ((flags & COMPUTE_FRAMES) != 0) {
      compute = MethodWriter.COMPUTE_ALL_FRAMES;
    } else if ((flags & COMPUTE_MAXS) != 0) {
      compute = MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL;
    } else {
      compute = MethodWriter.COMPUTE_NOTHING;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Accessors
  // -----------------------------------------------------------------------------------------------

  /**
   * 如果所有给定标志都已传递给构造函数，则返回 true。
   *
   * @param flags some option flags. Must be zero or more of {@link #COMPUTE_MAXS} and {@link
   *     #COMPUTE_FRAMES}.
   * @return true if all the given flags, or more, were passed to the constructor.
   */
  public boolean hasFlags(final int flags) {
    return (this.flags & flags) == flags;
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the ClassVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public final void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    this.version = version;
    this.accessFlags = access;
    this.thisClass = symbolTable.setMajorVersionAndClassName(version & 0xFFFF, name);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
    this.superClass = superName == null ? 0 : symbolTable.addConstantClass(superName).index;
    if (interfaces != null && interfaces.length > 0) {
      interfaceCount = interfaces.length;
      this.interfaces = new int[interfaceCount];
      for (int i = 0; i < interfaceCount; ++i) {
        this.interfaces[i] = symbolTable.addConstantClass(interfaces[i]).index;
      }
    }
    if (compute == MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL && (version & 0xFFFF) >= Opcodes.V1_7) {
      compute = MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES;
    }
  }

  @Override
  public final void visitSource(final String file, final String debug) {
    if (file != null) {
      sourceFileIndex = symbolTable.addConstantUtf8(file);
    }
    if (debug != null) {
      debugExtension = new ByteVector().encodeUtf8(debug, 0, Integer.MAX_VALUE);
    }
  }

  @Override
  public final ModuleVisitor visitModule(
      final String name, final int access, final String version) {
    return moduleWriter =
        new ModuleWriter(
            symbolTable,
            symbolTable.addConstantModule(name).index,
            access,
            version == null ? 0 : symbolTable.addConstantUtf8(version));
  }

  @Override
  public final void visitNestHost(final String nestHost) {
    nestHostClassIndex = symbolTable.addConstantClass(nestHost).index;
  }

  @Override
  public final void visitOuterClass(
      final String owner, final String name, final String descriptor) {
    enclosingClassIndex = symbolTable.addConstantClass(owner).index;
    if (name != null && descriptor != null) {
      enclosingMethodIndex = symbolTable.addConstantNameAndType(name, descriptor);
    }
  }

  @Override
  public final AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
    } else {
      return lastRuntimeInvisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public final AnnotationVisitor visitTypeAnnotation(
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
  public final void visitAttribute(final Attribute attribute) {
    // Store the attributes in the <i>reverse</i> order of their visit by this method.
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public final void visitNestMember(final String nestMember) {
    if (nestMemberClasses == null) {
      nestMemberClasses = new ByteVector();
    }
    ++numberOfNestMemberClasses;
    nestMemberClasses.putShort(symbolTable.addConstantClass(nestMember).index);
  }

  @Override
  public final void visitPermittedSubclass(final String permittedSubclass) {
    if (permittedSubclasses == null) {
      permittedSubclasses = new ByteVector();
    }
    ++numberOfPermittedSubclasses;
    permittedSubclasses.putShort(symbolTable.addConstantClass(permittedSubclass).index);
  }

  @Override
  public final void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    if (innerClasses == null) {
      innerClasses = new ByteVector();
    }
    // Section 4.7.6 of the JVMS states "Every CONSTANT_Class_info entry in the constant_pool table
    // which represents a class or interface C that is not a package member must have exactly one
    // corresponding entry in the classes array". To avoid duplicates we keep track in the info
    // field of the Symbol of each CONSTANT_Class_info entry C whether an inner class entry has
    // already been added for C. If so, we store the index of this inner class entry (plus one) in
    // the info field. This trick allows duplicate detection in O(1) time.
    Symbol nameSymbol = symbolTable.addConstantClass(name);
    if (nameSymbol.info == 0) {
      ++numberOfInnerClasses;
      innerClasses.putShort(nameSymbol.index);
      innerClasses.putShort(outerName == null ? 0 : symbolTable.addConstantClass(outerName).index);
      innerClasses.putShort(innerName == null ? 0 : symbolTable.addConstantUtf8(innerName));
      innerClasses.putShort(access);
      nameSymbol.info = numberOfInnerClasses;
    }
    // Else, compare the inner classes entry nameSymbol.info - 1 with the arguments of this method
    // and throw an exception if there is a difference?
  }

  @Override
  public final RecordComponentVisitor visitRecordComponent(
      final String name, final String descriptor, final String signature) {
    RecordComponentWriter recordComponentWriter =
        new RecordComponentWriter(symbolTable, name, descriptor, signature);
    if (firstRecordComponent == null) {
      firstRecordComponent = recordComponentWriter;
    } else {
      lastRecordComponent.delegate = recordComponentWriter;
    }
    return lastRecordComponent = recordComponentWriter;
  }

  @Override
  public final FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    FieldWriter fieldWriter =
        new FieldWriter(symbolTable, access, name, descriptor, signature, value);
    if (firstField == null) {
      firstField = fieldWriter;
    } else {
      lastField.fv = fieldWriter;
    }
    return lastField = fieldWriter;
  }

  @Override
  public final MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    MethodWriter methodWriter =
        new MethodWriter(symbolTable, access, name, descriptor, signature, exceptions, compute);
    if (firstMethod == null) {
      firstMethod = methodWriter;
    } else {
      lastMethod.mv = methodWriter;
    }
    return lastMethod = methodWriter;
  }

  @Override
  public final void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Other public methods
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回由此 ClassWriter 构建的类文件的内容。
   *
   * @return the binary content of the JVMS ClassFile structure that was built by this ClassWriter.
   * @throws ClassTooLargeException if the constant pool of the class is too large.
   * @throws MethodTooLargeException if the Code attribute of a method is too large.
   */
  public byte[] toByteArray() {
    // First step: compute the size in bytes of the ClassFile structure.
    // The magic field uses 4 bytes, 10 mandatory fields (minor_version, major_version,
    // constant_pool_count, access_flags, this_class, super_class, interfaces_count, fields_count,
    // methods_count and attributes_count) use 2 bytes each, and each interface uses 2 bytes too.
    int size = 24 + 2 * interfaceCount;
    int fieldsCount = 0;
    FieldWriter fieldWriter = firstField;
    while (fieldWriter != null) {
      ++fieldsCount;
      size += fieldWriter.computeFieldInfoSize();
      fieldWriter = (FieldWriter) fieldWriter.fv;
    }
    int methodsCount = 0;
    MethodWriter methodWriter = firstMethod;
    while (methodWriter != null) {
      ++methodsCount;
      size += methodWriter.computeMethodInfoSize();
      methodWriter = (MethodWriter) methodWriter.mv;
    }

    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    int attributesCount = 0;
    if (innerClasses != null) {
      ++attributesCount;
      size += 8 + innerClasses.length;
      symbolTable.addConstantUtf8(Constants.INNER_CLASSES);
    }
    if (enclosingClassIndex != 0) {
      ++attributesCount;
      size += 10;
      symbolTable.addConstantUtf8(Constants.ENCLOSING_METHOD);
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && (version & 0xFFFF) < Opcodes.V1_5) {
      ++attributesCount;
      size += 6;
      symbolTable.addConstantUtf8(Constants.SYNTHETIC);
    }
    if (signatureIndex != 0) {
      ++attributesCount;
      size += 8;
      symbolTable.addConstantUtf8(Constants.SIGNATURE);
    }
    if (sourceFileIndex != 0) {
      ++attributesCount;
      size += 8;
      symbolTable.addConstantUtf8(Constants.SOURCE_FILE);
    }
    if (debugExtension != null) {
      ++attributesCount;
      size += 6 + debugExtension.length;
      symbolTable.addConstantUtf8(Constants.SOURCE_DEBUG_EXTENSION);
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      ++attributesCount;
      size += 6;
      symbolTable.addConstantUtf8(Constants.DEPRECATED);
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeVisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeInvisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
    }
    if (symbolTable.computeBootstrapMethodsSize() > 0) {
      ++attributesCount;
      size += symbolTable.computeBootstrapMethodsSize();
    }
    if (moduleWriter != null) {
      attributesCount += moduleWriter.getAttributeCount();
      size += moduleWriter.computeAttributesSize();
    }
    if (nestHostClassIndex != 0) {
      ++attributesCount;
      size += 8;
      symbolTable.addConstantUtf8(Constants.NEST_HOST);
    }
    if (nestMemberClasses != null) {
      ++attributesCount;
      size += 8 + nestMemberClasses.length;
      symbolTable.addConstantUtf8(Constants.NEST_MEMBERS);
    }
    if (permittedSubclasses != null) {
      ++attributesCount;
      size += 8 + permittedSubclasses.length;
      symbolTable.addConstantUtf8(Constants.PERMITTED_SUBCLASSES);
    }
    int recordComponentCount = 0;
    int recordSize = 0;
    if ((accessFlags & Opcodes.ACC_RECORD) != 0 || firstRecordComponent != null) {
      RecordComponentWriter recordComponentWriter = firstRecordComponent;
      while (recordComponentWriter != null) {
        ++recordComponentCount;
        recordSize += recordComponentWriter.computeRecordComponentInfoSize();
        recordComponentWriter = (RecordComponentWriter) recordComponentWriter.delegate;
      }
      ++attributesCount;
      size += 8 + recordSize;
      symbolTable.addConstantUtf8(Constants.RECORD);
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    // IMPORTANT: this must be the last part of the ClassFile size computation, because the previous
    // statements can add attribute names to the constant pool, thereby changing its size!
    size += symbolTable.getConstantPoolLength();
    int constantPoolCount = symbolTable.getConstantPoolCount();
    if (constantPoolCount > 0xFFFF) {
      throw new ClassTooLargeException(symbolTable.getClassName(), constantPoolCount);
    }

    // Second step: allocate a ByteVector of the correct size (in order to avoid any array copy in
    // dynamic resizes) and fill it with the ClassFile content.
    ByteVector result = new ByteVector(size);
    result.putInt(0xCAFEBABE).putInt(version);
    symbolTable.putConstantPool(result);
    int mask = (version & 0xFFFF) < Opcodes.V1_5 ? Opcodes.ACC_SYNTHETIC : 0;
    result.putShort(accessFlags & ~mask).putShort(thisClass).putShort(superClass);
    result.putShort(interfaceCount);
    for (int i = 0; i < interfaceCount; ++i) {
      result.putShort(interfaces[i]);
    }
    result.putShort(fieldsCount);
    fieldWriter = firstField;
    while (fieldWriter != null) {
      fieldWriter.putFieldInfo(result);
      fieldWriter = (FieldWriter) fieldWriter.fv;
    }
    result.putShort(methodsCount);
    boolean hasFrames = false;
    boolean hasAsmInstructions = false;
    methodWriter = firstMethod;
    while (methodWriter != null) {
      hasFrames |= methodWriter.hasFrames();
      hasAsmInstructions |= methodWriter.hasAsmInstructions();
      methodWriter.putMethodInfo(result);
      methodWriter = (MethodWriter) methodWriter.mv;
    }
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    result.putShort(attributesCount);
    if (innerClasses != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.INNER_CLASSES))
          .putInt(innerClasses.length + 2)
          .putShort(numberOfInnerClasses)
          .putByteArray(innerClasses.data, 0, innerClasses.length);
    }
    if (enclosingClassIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.ENCLOSING_METHOD))
          .putInt(4)
          .putShort(enclosingClassIndex)
          .putShort(enclosingMethodIndex);
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && (version & 0xFFFF) < Opcodes.V1_5) {
      result.putShort(symbolTable.addConstantUtf8(Constants.SYNTHETIC)).putInt(0);
    }
    if (signatureIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.SIGNATURE))
          .putInt(2)
          .putShort(signatureIndex);
    }
    if (sourceFileIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.SOURCE_FILE))
          .putInt(2)
          .putShort(sourceFileIndex);
    }
    if (debugExtension != null) {
      int length = debugExtension.length;
      result
          .putShort(symbolTable.addConstantUtf8(Constants.SOURCE_DEBUG_EXTENSION))
          .putInt(length)
          .putByteArray(debugExtension.data, 0, length);
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      result.putShort(symbolTable.addConstantUtf8(Constants.DEPRECATED)).putInt(0);
    }
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        result);
    symbolTable.putBootstrapMethods(result);
    if (moduleWriter != null) {
      moduleWriter.putAttributes(result);
    }
    if (nestHostClassIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.NEST_HOST))
          .putInt(2)
          .putShort(nestHostClassIndex);
    }
    if (nestMemberClasses != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.NEST_MEMBERS))
          .putInt(nestMemberClasses.length + 2)
          .putShort(numberOfNestMemberClasses)
          .putByteArray(nestMemberClasses.data, 0, nestMemberClasses.length);
    }
    if (permittedSubclasses != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.PERMITTED_SUBCLASSES))
          .putInt(permittedSubclasses.length + 2)
          .putShort(numberOfPermittedSubclasses)
          .putByteArray(permittedSubclasses.data, 0, permittedSubclasses.length);
    }
    if ((accessFlags & Opcodes.ACC_RECORD) != 0 || firstRecordComponent != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.RECORD))
          .putInt(recordSize + 2)
          .putShort(recordComponentCount);
      RecordComponentWriter recordComponentWriter = firstRecordComponent;
      while (recordComponentWriter != null) {
        recordComponentWriter.putRecordComponentInfo(result);
        recordComponentWriter = (RecordComponentWriter) recordComponentWriter.delegate;
      }
    }
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, result);
    }

    // Third step: replace the ASM specific instructions, if any.
    if (hasAsmInstructions) {
      return replaceAsmInstructions(result.data, hasFrames);
    } else {
      return result.data;
    }
  }

  /**
   * 返回给定类文件的等价形式，其中 ASM 特有的指令已替换为标准指令。这是通过 ClassReader -&gt;
   * ClassWriter 的往返过程完成的。
   *
   * @param classFile a class file containing ASM specific instructions, generated by this
   *     ClassWriter.
   * @param hasFrames whether there is at least one stack map frames in 'classFile'.
   * @return an equivalent of 'classFile', with the ASM specific instructions replaced with standard
   *     ones.
   */
  private byte[] replaceAsmInstructions(final byte[] classFile, final boolean hasFrames) {
    final Attribute[] attributes = getAttributePrototypes();
    firstField = null;
    lastField = null;
    firstMethod = null;
    lastMethod = null;
    lastRuntimeVisibleAnnotation = null;
    lastRuntimeInvisibleAnnotation = null;
    lastRuntimeVisibleTypeAnnotation = null;
    lastRuntimeInvisibleTypeAnnotation = null;
    moduleWriter = null;
    nestHostClassIndex = 0;
    numberOfNestMemberClasses = 0;
    nestMemberClasses = null;
    numberOfPermittedSubclasses = 0;
    permittedSubclasses = null;
    firstRecordComponent = null;
    lastRecordComponent = null;
    firstAttribute = null;
    compute = hasFrames ? MethodWriter.COMPUTE_INSERTED_FRAMES : MethodWriter.COMPUTE_NOTHING;
    new ClassReader(classFile, 0, /* checkClassVersion = */ false)
        .accept(
            this,
            attributes,
            (hasFrames ? ClassReader.EXPAND_FRAMES : 0) | ClassReader.EXPAND_ASM_INSNS);
    return toByteArray();
  }

  /**
   * 返回此类及其字段和方法所使用的属性原型。
   *
   * @return the prototypes of the attributes used by this class, its fields and its methods.
   */
  private Attribute[] getAttributePrototypes() {
    Attribute.Set attributePrototypes = new Attribute.Set();
    attributePrototypes.addAttributes(firstAttribute);
    FieldWriter fieldWriter = firstField;
    while (fieldWriter != null) {
      fieldWriter.collectAttributePrototypes(attributePrototypes);
      fieldWriter = (FieldWriter) fieldWriter.fv;
    }
    MethodWriter methodWriter = firstMethod;
    while (methodWriter != null) {
      methodWriter.collectAttributePrototypes(attributePrototypes);
      methodWriter = (MethodWriter) methodWriter.mv;
    }
    RecordComponentWriter recordComponentWriter = firstRecordComponent;
    while (recordComponentWriter != null) {
      recordComponentWriter.collectAttributePrototypes(attributePrototypes);
      recordComponentWriter = (RecordComponentWriter) recordComponentWriter.delegate;
    }
    return attributePrototypes.toArray();
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods: constant pool management for Attribute sub classes
  // -----------------------------------------------------------------------------------------------

  /**
   * 将数字或字符串常量添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此
   * 方法供 {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param value the value of the constant to be added to the constant pool. This parameter must be
   *     an {@link Integer}, a {@link Float}, a {@link Long}, a {@link Double} or a {@link String}.
   * @return the index of a new or already existing constant item with the given value.
   */
  public int newConst(final Object value) {
    return symbolTable.addConstant(value).index;
  }

  /**
   * 将 UTF8 字符串添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法
   * 供 {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param value the String value.
   * @return the index of a new or already existing UTF8 item.
   */
  // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
  public int newUTF8(final String value) {
    return symbolTable.addConstantUtf8(value);
  }

  /**
   * 将类引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param value the internal name of the class (see {@link Type#getInternalName()}).
   * @return the index of a new or already existing class reference item.
   */
  public int newClass(final String value) {
    return symbolTable.addConstantClass(value).index;
  }

  /**
   * 将方法类型引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法
   * 供 {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param methodDescriptor method descriptor of the method type.
   * @return the index of a new or already existing method type reference item.
   */
  public int newMethodType(final String methodDescriptor) {
    return symbolTable.addConstantMethodType(methodDescriptor).index;
  }

  /**
   * 将模块引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param moduleName name of the module.
   * @return the index of a new or already existing module reference item.
   */
  public int newModule(final String moduleName) {
    return symbolTable.addConstantModule(moduleName).index;
  }

  /**
   * 将包引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param packageName name of the package in its internal form.
   * @return the index of a new or already existing module reference item.
   */
  public int newPackage(final String packageName) {
    return symbolTable.addConstantPackage(packageName).index;
  }

  /**
   * 将句柄添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param tag the kind of this handle. Must be {@link Opcodes#H_GETFIELD}, {@link
   *     Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC}, {@link
   *     Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL},
   *     {@link Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of the field or method owner class (see {@link
   *     Type#getInternalName()}).
   * @param name the name of the field or method.
   * @param descriptor the descriptor of the field or method.
   * @return the index of a new or already existing method type reference item.
   * @deprecated this method is superseded by {@link #newHandle(int, String, String, String,
   *     boolean)}.
   */
  @Deprecated
  public int newHandle(
      final int tag, final String owner, final String name, final String descriptor) {
    return newHandle(tag, owner, name, descriptor, tag == Opcodes.H_INVOKEINTERFACE);
  }

  /**
   * 将句柄添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param tag the kind of this handle. Must be {@link Opcodes#H_GETFIELD}, {@link
   *     Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC}, {@link
   *     Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL},
   *     {@link Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of the field or method owner class (see {@link
   *     Type#getInternalName()}).
   * @param name the name of the field or method.
   * @param descriptor the descriptor of the field or method.
   * @param isInterface true if the owner is an interface.
   * @return the index of a new or already existing method type reference item.
   */
  public int newHandle(
      final int tag,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    return symbolTable.addConstantMethodHandle(tag, owner, name, descriptor, isInterface).index;
  }

  /**
   * 将动态常量引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法
   * 供 {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param name name of the invoked method.
   * @param descriptor field descriptor of the constant type.
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments.
   * @return the index of a new or already existing dynamic constant reference item.
   */
  public int newConstantDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    return symbolTable.addConstantDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        .index;
  }

  /**
   * 将 invokedynamic 引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>
   * 此方法供 {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param name name of the invoked method.
   * @param descriptor descriptor of the invoke method.
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments.
   * @return the index of a new or already existing invokedynamic reference item.
   */
  public int newInvokeDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    return symbolTable.addConstantInvokeDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        .index;
  }

  /**
   * 将字段引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param owner the internal name of the field's owner class (see {@link Type#getInternalName()}).
   * @param name the field's name.
   * @param descriptor the field's descriptor.
   * @return the index of a new or already existing field reference item.
   */
  public int newField(final String owner, final String name, final String descriptor) {
    return symbolTable.addConstantFieldref(owner, name, descriptor).index;
  }

  /**
   * 将方法引用添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param owner the internal name of the method's owner class (see {@link
   *     Type#getInternalName()}).
   * @param name the method's name.
   * @param descriptor the method's descriptor.
   * @param isInterface {@literal true} if {@code owner} is an interface.
   * @return the index of a new or already existing method reference item.
   */
  public int newMethod(
      final String owner, final String name, final String descriptor, final boolean isInterface) {
    return symbolTable.addConstantMethodref(owner, name, descriptor, isInterface).index;
  }

  /**
   * 将名称和类型添加到正在构建的类的常量池中。如果常量池中已存在类似项，则不做任何操作。<i>此方法供
   * {@link Attribute} 子类使用，类生成器或适配器通常不需要调用它。</i>
   *
   * @param name a name.
   * @param descriptor a type descriptor.
   * @return the index of a new or already existing name and type item.
   */
  public int newNameType(final String name, final String descriptor) {
    return symbolTable.addConstantNameAndType(name, descriptor);
  }

  // -----------------------------------------------------------------------------------------------
  // Default method to compute common super classes when computing stack map frames
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回两个给定类型的公共超类型。此方法的默认实现会<i>加载</i>这两个给定的类，并使用
   * java.lang.Class 的方法查找公共超类。可以重写此方法，以其他方式计算该公共超类型，特别是无需实际加
   * 载任何类，或者将当前正由此 ClassWriter 生成、因而当然无法加载（它仍在构建中）的类考虑在内。
   *
   * @param type1 the internal name of a class (see {@link Type#getInternalName()}).
   * @param type2 the internal name of another class (see {@link Type#getInternalName()}).
   * @return the internal name of the common super class of the two given classes (see {@link
   *     Type#getInternalName()}).
   */
  protected String getCommonSuperClass(final String type1, final String type2) {
    ClassLoader classLoader = getClassLoader();
    Class<?> class1;
    try {
      class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
    } catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(type1, e);
    }
    Class<?> class2;
    try {
      class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
    } catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(type2, e);
    }
    if (class1.isAssignableFrom(class2)) {
      return type1;
    }
    if (class2.isAssignableFrom(class1)) {
      return type2;
    }
    if (class1.isInterface() || class2.isInterface()) {
      return "java/lang/Object";
    } else {
      do {
        class1 = class1.getSuperclass();
      } while (!class1.isAssignableFrom(class2));
      return class1.getName().replace('.', '/');
    }
  }

  /**
   * 返回 {@link #getCommonSuperClass(String, String)} 的默认实现所使用的 {@link ClassLoader}，默认情
   * 况下为 {@link ClassWriter} 运行时类型的类加载器。
   *
   * @return ClassLoader
   */
  protected ClassLoader getClassLoader() {
    // SPRING PATCH: prefer thread context ClassLoader for application classes
    ClassLoader classLoader = null;
    try {
      classLoader = Thread.currentThread().getContextClassLoader();
    } catch (Throwable ex) {
      // Cannot access thread context ClassLoader - falling back...
    }
    return (classLoader != null ? classLoader : getClass().getClassLoader());
  }
}
