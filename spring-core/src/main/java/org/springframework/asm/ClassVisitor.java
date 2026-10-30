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
   * @param api 此访问器实现的 ASM API 版本，必须是 {@link Opcodes} 中 {@code ASM}<i>x</i> 值之一。
   */
  protected ClassVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link ClassVisitor}。
   *
   * @param api 此访问器实现的 ASM API 版本，必须是 {@link Opcodes} 中 {@code ASM}<i>x</i> 值之一。
   * @param classVisitor 此访问器必须将方法调用委托给的类访问器。可以为 null。
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
   * @return 此访问器必须将方法调用委托给的类访问器，或 {@literal null}。
   */
  public ClassVisitor getDelegate() {
    return cv;
  }

  /**
   * 访问类的头部。
   *
   * @param version 类版本。次版本存储在最高的 16 位中，主版本存储在最低的 16 位中。
   * @param access 类的访问标志（参见 {@link Opcodes}）。此参数还指示该类是否已废弃
   * （{@link Opcodes#ACC_DEPRECATED}）或是否为记录类（{@link Opcodes#ACC_RECORD}）。
   * @param name 类的内部名（参见 {@link Type#getInternalName()}）。
   * @param signature 此类的签名。如果该类不是泛型类，且不继承或实现泛型类或接口，则可以为
   * {@literal null}。
   * @param superName 超类的内部名（参见 {@link Type#getInternalName()}）。对于接口，超类是
   * {@link Object}。可以为 {@literal null}，但仅对 {@link Object} 类成立。
   * @param interfaces 该类各接口的内部名（参见 {@link Type#getInternalName()}）。可以为 {@literal null}。
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
   * @param source 编译该类所用源文件的名称。可以为 {@literal null}。
   * @param debug 用于计算该类的源代码元素与编译后元素之间对应关系的附加调试信息。可以为
   * {@literal null}。
   */
  public void visitSource(final String source, final String debug) {
    if (cv != null) {
      cv.visitSource(source, debug);
    }
  }

  /**
   * 访问与此类对应的模块。
   *
   * @param name 模块的完全限定名（使用点分隔）。
   * @param access 模块访问标志，取 {@code ACC_OPEN}、{@code ACC_SYNTHETIC} 和 {@code ACC_MANDATED} 之一。
   * @param version 模块版本，或 {@literal null}。
   * @return 用于访问模块值的访问器；如果此访问器不关心访问该模块，则为 {@literal null}。
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
   * @param nestHost 嵌套（nest）宿主类的内部名（参见 {@link Type#getInternalName()}）。
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
   * @param owner 该类的封闭类的内部名（参见 {@link Type#getInternalName()}）。
   * @param name 包含该类的方法名称；如果该类未封闭在其封闭类的某个方法或构造器中（例如封闭在
   * 实例初始化块、静态初始化块、实例变量初始化或类变量初始化中），则为 {@literal null}。
   * @param descriptor 包含该类的方法描述符；如果该类未封闭在其封闭类的某个方法或构造器中（例如封闭在
   * 实例初始化块、静态初始化块、实例变量初始化或类变量初始化中），则为 {@literal null}。
   */
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    if (cv != null) {
      cv.visitOuterClass(owner, name, descriptor);
    }
  }

  /**
   * 访问类的一个注解。
   *
   * @param descriptor 注解类的类描述符。
   * @param visible 如果该注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
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
   * @param typeRef 对被注解类型的引用。此类型引用的种类必须是
   * {@link TypeReference#CLASS_TYPE_PARAMETER}、
   * {@link TypeReference#CLASS_TYPE_PARAMETER_BOUND} 或 {@link TypeReference#CLASS_EXTENDS}。
   * 参见 {@link TypeReference}。
   * @param typePath 指向 'typeRef' 中被注解的类型参数、通配符边界、数组元素类型或静态内部类型的路径。
   * 如果注解以整个 'typeRef' 为目标，则可以为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果该注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器；如果此访问器不关心访问该注解，则为 {@literal null}。
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
   * @param attribute 一个属性。
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
   * @param nestMember 嵌套成员的内部名（参见 {@link Type#getInternalName()}）。
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
   * @param permittedSubclass 被允许的子类的内部名（参见 {@link Type#getInternalName()}）。
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
   * @param name C 的内部名（参见 {@link Type#getInternalName()}）。
   * @param outerName C 作为其成员的类或接口的内部名（参见 {@link Type#getInternalName()}）。如果 C 不是
   * 某个类或接口的成员（例如局部类或匿名类），则必须为 {@literal null}。
   * @param innerName C 的（简单）名称。对于匿名内部类，必须为 {@literal null}。
   * @param access 在编译此类的源代码中最初声明的 C 的访问标志。
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
   * @param name 记录组件的名称。
   * @param descriptor 记录组件的描述符（参见 {@link Type}）。
   * @param signature 记录组件的签名。如果该记录组件的类型不使用泛型类型，则可以为 {@literal null}。
   * @return 用于访问该记录组件注解和属性的访问器；如果此类型访问器不关心访问这些注解和属性，
   * 则为 {@literal null}。
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
   * @param access 字段的访问标志（参见 {@link Opcodes}）。此参数还指示该字段是否为合成字段和/或
   * 已废弃字段。
   * @param name 字段的名称。
   * @param descriptor 字段的描述符（参见 {@link Type}）。
   * @param signature 字段的签名。如果该字段的类型不使用泛型类型，则可以为 {@literal null}。
   * @param value 字段的初始值。如果字段没有初始值，此参数可以为 {@literal null}；否则它必须是
   * {@link Integer}、{@link Float}、{@link Long}、{@link Double} 或 {@link String}
   * （分别对应 {@code int}、{@code float}、{@code long} 或 {@code String} 字段）。
   * <i>此参数仅用于静态字段</i>。对于非静态字段，其值会被忽略，这类字段必须通过
   * 构造器或方法中的字节码指令进行初始化。
   * @return 用于访问字段注解和属性的访问器；如果此类型访问器不关心访问这些注解和属性，
   * 则为 {@literal null}。
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
   * @param access 方法的访问标志（参见 {@link Opcodes}）。此参数还指示该方法是否为合成方法和/或
   * 已废弃方法。
   * @param name 方法的名称。
   * @param descriptor 方法的描述符（参见 {@link Type}）。
   * @param signature 方法的签名。如果方法参数、返回类型和异常都不使用泛型类型，则可以为
   * {@literal null}。
   * @param exceptions 方法各异常类的内部名（参见 {@link Type#getInternalName()}）。可以为
   * {@literal null}。
   * @return 用于访问该方法字节码的对象；如果此类型访问器不关心访问该方法的代码，则为
   * {@literal null}。
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
