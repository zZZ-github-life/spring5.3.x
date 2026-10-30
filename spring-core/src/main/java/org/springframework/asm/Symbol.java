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
 * 常量池、BootstrapMethods 属性或类的（ASM 专用）类型表中的一个条目。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4">JVMS
 *     4.4</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23">JVMS
 *     4.7.23</a>
 * @author Eric Bruneton
 */
abstract class Symbol {

  // Tag values for the constant pool entries (using the same order as in the JVMS).

  /** CONSTANT_Class_info JVMS 结构的标签值。 */
  static final int CONSTANT_CLASS_TAG = 7;

  /** CONSTANT_Fieldref_info JVMS 结构的标签值。 */
  static final int CONSTANT_FIELDREF_TAG = 9;

  /** CONSTANT_Methodref_info JVMS 结构的标签值。 */
  static final int CONSTANT_METHODREF_TAG = 10;

  /** CONSTANT_InterfaceMethodref_info JVMS 结构的标签值。 */
  static final int CONSTANT_INTERFACE_METHODREF_TAG = 11;

  /** CONSTANT_String_info JVMS 结构的标签值。 */
  static final int CONSTANT_STRING_TAG = 8;

  /** CONSTANT_Integer_info JVMS 结构的标签值。 */
  static final int CONSTANT_INTEGER_TAG = 3;

  /** CONSTANT_Float_info JVMS 结构的标签值。 */
  static final int CONSTANT_FLOAT_TAG = 4;

  /** CONSTANT_Long_info JVMS 结构的标签值。 */
  static final int CONSTANT_LONG_TAG = 5;

  /** CONSTANT_Double_info JVMS 结构的标签值。 */
  static final int CONSTANT_DOUBLE_TAG = 6;

  /** CONSTANT_NameAndType_info JVMS 结构的标签值。 */
  static final int CONSTANT_NAME_AND_TYPE_TAG = 12;

  /** CONSTANT_Utf8_info JVMS 结构的标签值。 */
  static final int CONSTANT_UTF8_TAG = 1;

  /** CONSTANT_MethodHandle_info JVMS 结构的标签值。 */
  static final int CONSTANT_METHOD_HANDLE_TAG = 15;

  /** CONSTANT_MethodType_info JVMS 结构的标签值。 */
  static final int CONSTANT_METHOD_TYPE_TAG = 16;

  /** CONSTANT_Dynamic_info JVMS 结构的标签值。 */
  static final int CONSTANT_DYNAMIC_TAG = 17;

  /** CONSTANT_InvokeDynamic_info JVMS 结构的标签值。 */
  static final int CONSTANT_INVOKE_DYNAMIC_TAG = 18;

  /** CONSTANT_Module_info JVMS 结构的标签值。 */
  static final int CONSTANT_MODULE_TAG = 19;

  /** CONSTANT_Package_info JVMS 结构的标签值。 */
  static final int CONSTANT_PACKAGE_TAG = 20;

  // Tag values for the BootstrapMethods attribute entries (ASM specific tag).

  /** BootstrapMethods 属性条目的标签值。 */
  static final int BOOTSTRAP_METHOD_TAG = 64;

  // Tag values for the type table entries (ASM specific tags).

  /** （ASM 特有）类的类型表中普通类型条目的标签值。 */
  static final int TYPE_TAG = 128;

  /**
   * 类的类型表中 {@link Frame#ITEM_UNINITIALIZED} 类型条目的标签值。
   */
  static final int UNINITIALIZED_TYPE_TAG = 129;

  /** （ASM 特有）类的类型表中合并类型条目的标签值。 */
  static final int MERGED_TYPE_TAG = 130;

  // Instance fields.

  /**
   * 此符号在常量池、BootstrapMethods 属性或类的（ASM 专用）类型表中的索引
   * （取决于 {@link #tag} 的值）。
   */
  final int index;

  /**
   * 指示此符号类型的标签。必须是此类中定义的静态标签值之一。
   */
  final int tag;

  /**
   * 此符号所属类的内部名称。仅用于 {@link #CONSTANT_FIELDREF_TAG}、{@link #CONSTANT_METHODREF_TAG}、
   * {@link #CONSTANT_INTERFACE_METHODREF_TAG} 和 {@link #CONSTANT_METHOD_HANDLE_TAG} 符号。
   */
  final String owner;

  /**
   * 此符号对应的类字段或方法的名称。仅用于 {@link #CONSTANT_FIELDREF_TAG}、
   * {@link #CONSTANT_METHODREF_TAG}、{@link #CONSTANT_INTERFACE_METHODREF_TAG}、
   * {@link #CONSTANT_NAME_AND_TYPE_TAG}、{@link #CONSTANT_METHOD_HANDLE_TAG}、
   * {@link #CONSTANT_DYNAMIC_TAG} 和 {@link #CONSTANT_INVOKE_DYNAMIC_TAG} 符号。
   */
  final String name;

  /**
   * 此符号的字符串值。具体为：
   *
   * <ul>
   * <li>对于 {@link #CONSTANT_FIELDREF_TAG}、{@link #CONSTANT_METHODREF_TAG}、{@link #CONSTANT_INTERFACE_METHODREF_TAG}、{@link #CONSTANT_NAME_AND_TYPE_TAG}、{@link #CONSTANT_METHOD_HANDLE_TAG}、{@link #CONSTANT_METHOD_TYPE_TAG}、{@link #CONSTANT_DYNAMIC_TAG} 和 {@link #CONSTANT_INVOKE_DYNAMIC_TAG} 符号，为字段或方法描述符，
   * <li>对于 {@link #CONSTANT_UTF8_TAG} 和 {@link #CONSTANT_STRING_TAG} 符号，为任意字符串，
   * <li>对于 {@link #CONSTANT_CLASS_TAG}、{@link #TYPE_TAG} 和 {@link #UNINITIALIZED_TYPE_TAG} 符号，为内部类名，
   * <li>对于其他类型的符号，为 {@literal null}。
   * </ul>
   */
  final String value;

  /**
   * 此符号的数值。具体为：
   *
   * <ul>
   * <li>对于 {@link #CONSTANT_INTEGER_TAG}、{@link #CONSTANT_FLOAT_TAG}、{@link #CONSTANT_LONG_TAG}、{@link #CONSTANT_DOUBLE_TAG} 符号，为符号的值，
   * <li>对于 {@link #CONSTANT_METHOD_HANDLE_TAG} 符号，为 CONSTANT_MethodHandle_info 的 reference_kind 字段值，
   * <li>对于 {@link #CONSTANT_INVOKE_DYNAMIC_TAG} 符号，为 CONSTANT_InvokeDynamic_info 的 bootstrap_method_attr_index 字段值，
   * <li>对于 {@link #CONSTANT_DYNAMIC_TAG} 或 {@link #BOOTSTRAP_METHOD_TAG} 符号，为引导方法在 BootstrapMethods 的 boostrap_methods 数组中的偏移量，
   * <li>对于 {@link #UNINITIALIZED_TYPE_TAG} 符号，为创建 {@link Frame#ITEM_UNINITIALIZED} 类型的 NEW 指令的字节码偏移量，
   * <li>对于 {@link #MERGED_TYPE_TAG} 符号，为两个 {@link #TYPE_TAG} 源类型（在类的类型表中）的索引，
   * <li>对于其他类型的符号，为 0。
   * </ul>
   */
  final long data;

  /**
   * 关于此符号的附加信息，通常以延迟方式计算。<i>警告：比较 Symbol 实例时此字段的值会被
   * 忽略</i>（以避免 SymbolTable 中出现重复条目）。因此，此字段只应包含可根据此类其他
   * 字段计算得出的数据。它包含：
   *
   * <ul>
   * <li>对于 {@link #CONSTANT_METHODREF_TAG}、{@link #CONSTANT_INTERFACE_METHODREF_TAG} 和 {@link #CONSTANT_INVOKE_DYNAMIC_TAG} 符号，为符号方法描述符的 {@link Type#getArgumentsAndReturnSizes}，
   * <li>对于 {@link #CONSTANT_CLASS_TAG} 符号，为此类对应的 InnerClasses_attribute 'classes' 数组中的索引（从 1 开始），
   * <li>对于 {@link #MERGED_TYPE_TAG} 符号，为两个源类型的合并类型（在类的类型表中）的索引，
   * <li>对于其他类型的符号，或者此字段尚未计算时，为 0。
   * </ul>
   */
  int info;

  /**
   * 构造一个新的 Symbol。由于 Symbol 类是抽象类，不能直接使用此构造方法。请改用 {@link SymbolTable}
   * 类的工厂方法。
   *
   * @param index 符号在常量池、BootstrapMethods 属性或类的（ASM 专用）类型表中的索引
   * （取决于 'tag'）。
   * @param tag 符号类型。必须是此类中定义的某个静态 tag 值。
   * @param owner 符号所有者类的内部名称。可以为 {@literal null}。
   * @param name 符号所对应类字段或方法的名称。可以为 {@literal null}。
   * @param value 此符号的字符串值。可以为 {@literal null}。
   * @param data 此符号的数值。
   */
  Symbol(
      final int index,
      final int tag,
      final String owner,
      final String name,
      final String value,
      final long data) {
    this.index = index;
    this.tag = tag;
    this.owner = owner;
    this.name = name;
    this.value = value;
    this.data = data;
  }

  /**
   * 返回对 {@link #value} 调用 {@link Type#getArgumentsAndReturnSizes} 的结果。
   *
   * @return 对 {@link #value} 调用 {@link Type#getArgumentsAndReturnSizes} 的结果
   * （为提高效率而缓存在 {@link #info} 中）。
   * 这仅应用于 {@link #CONSTANT_METHODREF_TAG}、{@link #CONSTANT_INTERFACE_METHODREF_TAG}
   * 和 {@link #CONSTANT_INVOKE_DYNAMIC_TAG} 符号。
   */
  int getArgumentsAndReturnSizes() {
    if (info == 0) {
      info = Type.getArgumentsAndReturnSizes(value);
    }
    return info;
  }
}
