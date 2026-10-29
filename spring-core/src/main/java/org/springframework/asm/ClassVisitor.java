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
 * 用于访问 Java 类的访问器。此类的方法必须按以下顺序调用：{@code visit} [ {@code visitSource} ] [
 * {@code visitModule} ][ {@code visitNestHost} ][ {@code visitOuterClass} ] (
 * {@code visitAnnotation} | {@code visitTypeAnnotation} | {@code visitAttribute} )* (
 * {@code visitNestMember} | [ {@code * visitPermittedSubclass} ] | {@code visitInnerClass} |
 * {@code visitRecordComponent} | {@code visitField} | {@code visitMethod} )* {@code visitEnd}。
 *
 * @author Eric Bruneton
 */
public abstract class ClassVisitor {

  /**
   * 此访问器实现的 ASM API 版本。此字段的值必须是 {@link Opcodes} 中的某个 {@code ASM}<i>x</i> 值。
   */
  protected final int api;

  /** 此访问器必须将方法调用委托给的类访问器。可能为 {@literal null}。 */
  protected ClassVisitor cv;

  /**
   * 构造一个新的 {@link ClassVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   */
  protected ClassVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link ClassVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   * @param classVisitor the class visitor to which this visitor must delegate method calls. May be
   *     null.
   */
  protected ClassVisitor(final int api, final ClassVisitor classVisitor) {
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
    this.cv = classVisitor;
  }

  /**
   * 此访问器必须将方法调用委托给它的类访问器。可以为 {@literal null}。
   *
   * @return the class visitor to which this visitor must delegate method calls, or {@literal null}.
   */
  public ClassVisitor getDelegate() {
    return cv;
  }

  /**
   * 访问类的头部。
   *
   * @param version the class version. The minor version is stored in the 16 most significant bits,
   *     and the major version in the 16 least significant bits.
   * @param access the class's access flags (see {@link Opcodes}). This parameter also indicates if
   *     the class is deprecated {@link Opcodes#ACC_DEPRECATED} or a record {@link
   *     Opcodes#ACC_RECORD}.
   * @param name the internal name of the class (see {@link Type#getInternalName()}).
   * @param signature the signature of this class. May be {@literal null} if the class is not a
   *     generic one, and does not extend or implement generic classes or interfaces.
   * @param superName the internal of name of the super class (see {@link Type#getInternalName()}).
   *     For interfaces, the super class is {@link Object}. May be {@literal null}, but only for the
   *     {@link Object} class.
   * @param interfaces the internal names of the class's interfaces (see {@link
   *     Type#getInternalName()}). May be {@literal null}.
   */
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    if (api < Opcodes.ASM8 && (access & Opcodes.ACC_RECORD) != 0) {
      throw new UnsupportedOperationException("Records requires ASM8");
    }
    if (cv != null) {
      cv.visit(version, access, name, signature, superName, interfaces);
    }
  }

  /**
   * 访问类的源信息。
   *
   * @param source the name of the source file from which the class was compiled. May be {@literal
   *     null}.
   * @param debug additional debug information to compute the correspondence between source and
   *     compiled elements of the class. May be {@literal null}.
   */
  public void visitSource(final String source, final String debug) {
    if (cv != null) {
      cv.visitSource(source, debug);
    }
  }

  /**
   * 访问与此类对应的模块。
   *
   * @param name the fully qualified name (using dots) of the module.
   * @param access the module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
   *     ACC_MANDATED}.
   * @param version the module version, or {@literal null}.
   * @return a visitor to visit the module values, or {@literal null} if this visitor is not
   *     interested in visiting this module.
   */
  public ModuleVisitor visitModule(final String name, final int access, final String version) {
    if (api < Opcodes.ASM6) {
      throw new UnsupportedOperationException("Module requires ASM6");
    }
    if (cv != null) {
      return cv.visitModule(name, access, version);
    }
    return null;
  }

  /**
   * 访问类的嵌套宿主类。嵌套（nest）是一组同包的类，它们相互共享对私有成员的访问权限。其中一个类称为
   * 宿主，它列出嵌套的其他成员，而这些成员又应链接到它们所属嵌套的宿主。此方法只能调用一次，且仅当被
   * 访问的类是某个嵌套的非宿主成员时才调用。类隐式地就是自身的嵌套，因此以被访问类的名称作为参数调用
   * 此方法是无效的。
   *
   * @param nestHost the internal name of the host class of the nest (see {@link
   *     Type#getInternalName()}).
   */
  public void visitNestHost(final String nestHost) {
    if (api < Opcodes.ASM7) {
      throw new UnsupportedOperationException("NestHost requires ASM7");
    }
    if (cv != null) {
      cv.visitNestHost(nestHost);
    }
  }

  /**
   * 访问此类的封闭类。仅当此类是局部类或匿名类时才必须调用此方法。更多细节请参见 JVMS 4.7.7 节。
   *
   * @param owner internal name of the enclosing class of the class (see {@link
   *     Type#getInternalName()}).
   * @param name the name of the method that contains the class, or {@literal null} if the class is
   *     not enclosed in a method or constructor of its enclosing class (e.g. if it is enclosed in
   *     an instance initializer, static initializer, instance variable initializer, or class
   *     variable initializer).
   * @param descriptor the descriptor of the method that contains the class, or {@literal null} if
   *     the class is not enclosed in a method or constructor of its enclosing class (e.g. if it is
   *     enclosed in an instance initializer, static initializer, instance variable initializer, or
   *     class variable initializer).
   */
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    if (cv != null) {
      cv.visitOuterClass(owner, name, descriptor);
    }
  }

  /**
   * 访问类的一个注解。
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (cv != null) {
      return cv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问类签名中某个类型上的注解。
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
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException("TypeAnnotation requires ASM5");
    }
    if (cv != null) {
      return cv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问类的一个非标准属性。
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (cv != null) {
      cv.visitAttribute(attribute);
    }
  }

  /**
   * 访问嵌套的一个成员。嵌套（nest）是一组同包的类，它们相互共享对私有成员的访问权限。其中一个类称为
   * 宿主，它列出嵌套的其他成员，而这些成员又应链接到它们所属嵌套的宿主。仅当被访问的类是某个嵌套的宿
   * 主时才必须调用此方法。嵌套宿主隐式地就是其自身嵌套的成员，因此以被访问类的名称作为参数调用此方法
   * 是无效的。
   *
   * @param nestMember the internal name of a nest member (see {@link Type#getInternalName()}).
   */
  public void visitNestMember(final String nestMember) {
    if (api < Opcodes.ASM7) {
      throw new UnsupportedOperationException("NestMember requires ASM7");
    }
    if (cv != null) {
      cv.visitNestMember(nestMember);
    }
  }

  /**
   * 访问允许的子类。允许的子类是当前类所允许的子类之一。
   *
   * @param permittedSubclass the internal name of a permitted subclass (see {@link
   *     Type#getInternalName()}).
   */
  public void visitPermittedSubclass(final String permittedSubclass) {
    if (api < Opcodes.ASM9) {
      throw new UnsupportedOperationException("PermittedSubclasses requires ASM9");
    }
    if (cv != null) {
      cv.visitPermittedSubclass(permittedSubclass);
    }
  }

  /**
   * 访问关于内部类的信息。此内部类不一定是被访问类的成员。更确切地说，被此类引用且不是包成员的每个类
   * 或接口 C 都必须用此方法访问。此类必须引用其嵌套类或接口成员，以及其封闭类（如果有）。更多细节请
   * 参见 JVMS 4.7.6 节。
   *
   * @param name the internal name of C (see {@link Type#getInternalName()}).
   * @param outerName the internal name of the class or interface C is a member of (see {@link
   *     Type#getInternalName()}). Must be {@literal null} if C is not the member of a class or
   *     interface (e.g. for local or anonymous classes).
   * @param innerName the (simple) name of C. Must be {@literal null} for anonymous inner classes.
   * @param access the access flags of C originally declared in the source code from which this
   *     class was compiled.
   */
  public void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    if (cv != null) {
      cv.visitInnerClass(name, outerName, innerName, access);
    }
  }

  /**
   * 访问类的一个记录组件。
   *
   * @param name the record component name.
   * @param descriptor the record component descriptor (see {@link Type}).
   * @param signature the record component signature. May be {@literal null} if the record component
   *     type does not use generic types.
   * @return a visitor to visit this record component annotations and attributes, or {@literal null}
   *     if this class visitor is not interested in visiting these annotations and attributes.
   */
  public RecordComponentVisitor visitRecordComponent(
      final String name, final String descriptor, final String signature) {
    if (api < Opcodes.ASM8) {
      throw new UnsupportedOperationException("Record requires ASM8");
    }
    if (cv != null) {
      return cv.visitRecordComponent(name, descriptor, signature);
    }
    return null;
  }

  /**
   * 访问类的一个字段。
   *
   * @param access the field's access flags (see {@link Opcodes}). This parameter also indicates if
   *     the field is synthetic and/or deprecated.
   * @param name the field's name.
   * @param descriptor the field's descriptor (see {@link Type}).
   * @param signature the field's signature. May be {@literal null} if the field's type does not use
   *     generic types.
   * @param value the field's initial value. This parameter, which may be {@literal null} if the
   *     field does not have an initial value, must be an {@link Integer}, a {@link Float}, a {@link
   *     Long}, a {@link Double} or a {@link String} (for {@code int}, {@code float}, {@code long}
   *     or {@code String} fields respectively). <i>This parameter is only used for static
   *     fields</i>. Its value is ignored for non static fields, which must be initialized through
   *     bytecode instructions in constructors or methods.
   * @return a visitor to visit field annotations and attributes, or {@literal null} if this class
   *     visitor is not interested in visiting these annotations and attributes.
   */
  public FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    if (cv != null) {
      return cv.visitField(access, name, descriptor, signature, value);
    }
    return null;
  }

  /**
   * 访问类的一个方法。此方法在每次被调用时<i>必须</i>返回一个新的 {@link MethodVisitor} 实例（或
   * {@literal null}），即它不应返回之前返回过的访问器。
   *
   * @param access the method's access flags (see {@link Opcodes}). This parameter also indicates if
   *     the method is synthetic and/or deprecated.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param signature the method's signature. May be {@literal null} if the method parameters,
   *     return type and exceptions do not use generic types.
   * @param exceptions the internal names of the method's exception classes (see {@link
   *     Type#getInternalName()}). May be {@literal null}.
   * @return an object to visit the byte code of the method, or {@literal null} if this class
   *     visitor is not interested in visiting the code of this method.
   */
  public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    if (cv != null) {
      return cv.visitMethod(access, name, descriptor, signature, exceptions);
    }
    return null;
  }

  /**
   * 访问类的结尾。此方法是最后被调用的方法，用于通知访问器类的所有字段和方法都已访问完毕。
   */
  public void visitEnd() {
    if (cv != null) {
      cv.visitEnd();
    }
  }
}
