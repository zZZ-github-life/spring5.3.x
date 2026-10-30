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
 * 类的常量池条目、BootstrapMethods 属性条目以及（ASM 专用）类型表条目。
 *
 * @author Eric Bruneton
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4">JVMS
 *     4.4</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23">JVMS
 *     4.7.23</a>
 */
final class SymbolTable {

  /**
   * 此 SymbolTable 所属的 ClassWriter。仅用于访问 {@link ClassWriter#getCommonSuperClass}，以及通过
   * {@link Attribute#write} 序列化自定义属性。
   */
  final ClassWriter classWriter;

  /**
   * 构造此 SymbolTable 所使用的 ClassReader；如果是从头构造的，则为 {@literal null}。
   */
  private final ClassReader sourceClassReader;

  /** 此符号表所属类的主版本号。 */
  private int majorVersion;

  /** 此符号表所属类的内部名称。 */
  private String className;

  /**
   * {@link #entries} 中 {@link Entry} 实例的总数。其中包括可通过 {@link Entry#next}（递归）访问的条
   * 目。
   */
  private int entryCount;

  /**
   * 此 SymbolTable 中所有条目的哈希集合（包括常量池条目、引导方法条目和类型表条目）。每个
   * {@link Entry} 实例存储在其哈希码对数组大小取模所得的数组索引处。如果多个条目必须存储在同一个数组
   * 索引处，则通过它们的 {@link Entry#next} 字段链接在一起。此类的工厂方法确保此表不包含重复条目。
   */
  private Entry[] entries;

  /**
   * {@link #constantPool} 中常量池项的数量加 1。第一个常量池项的索引为 1，
   * long 和 double 项各占两项。
   */
  private int constantPoolCount;

  /**
   * 与此 SymbolTable 对应的 ClassFile 的 constant_pool JVMS 结构的内容。<i>不</i>包含 ClassFile 的
   * constant_pool_count 字段。
   */
  private ByteVector constantPool;

  /**
   * {@link #bootstrapMethods} 中引导方法的数量。对应于 BootstrapMethods_attribute 的
   * num_bootstrap_methods 字段值。
   */
  private int bootstrapMethodCount;

  /**
   * 与此 SymbolTable 对应的 BootstrapMethods 属性的 'bootstrap_methods' 数组的内容。注意，<i>不</i>
   * 包含 BootstrapMethods_attribute 的前 6 个字节及其 num_bootstrap_methods 字段。
   */
  private ByteVector bootstrapMethods;

  /**
   * {@link #typeTable} 中元素的实际数量。这些元素从索引 0 存储到 typeCount（不含）。数组的其他条目为
   * 空。
   */
  private int typeCount;

  /**
   * 一种 ASM 专用的类型表，用于临时存储不一定会存入常量池的内部名称。此类型表由用于从头计算栈映射帧
   * 的控制流和数据流分析算法使用。此数组存储 {@link Symbol#TYPE_TAG} 和
   * {@link Symbol#UNINITIALIZED_TYPE_TAG}) Symbol。索引为 {@code i} 的类型符号的
   * {@link Symbol#index} 等于 {@code i}（反之亦然）。
   */
  private Entry[] typeTable;

  /**
   * 为给定的 ClassWriter 构造一个新的空 SymbolTable。
   *
   * @param classWriter 一个 ClassWriter。
   */
  SymbolTable(final ClassWriter classWriter) {
    this.classWriter = classWriter;
    this.sourceClassReader = null;
    this.entries = new Entry[256];
    this.constantPoolCount = 1;
    this.constantPool = new ByteVector();
  }

  /**
   * 为给定的 ClassWriter 构造一个新的 SymbolTable，并使用给定 ClassReader 的常量池和引导方法初始化。
   *
   * @param classWriter 一个 ClassWriter。
   * @param classReader 其常量池和引导方法必须被复制以初始化 SymbolTable 的 ClassReader。
   */
  SymbolTable(final ClassWriter classWriter, final ClassReader classReader) {
    this.classWriter = classWriter;
    this.sourceClassReader = classReader;

    // Copy the constant pool binary content.
    byte[] inputBytes = classReader.classFileBuffer;
    int constantPoolOffset = classReader.getItem(1) - 1;
    int constantPoolLength = classReader.header - constantPoolOffset;
    constantPoolCount = classReader.getItemCount();
    constantPool = new ByteVector(constantPoolLength);
    constantPool.putByteArray(inputBytes, constantPoolOffset, constantPoolLength);

    // Add the constant pool items in the symbol table entries. Reserve enough space in 'entries' to
    // avoid too many hash set collisions (entries is not dynamically resized by the addConstant*
    // method calls below), and to account for bootstrap method entries.
    entries = new Entry[constantPoolCount * 2];
    char[] charBuffer = new char[classReader.getMaxStringLength()];
    boolean hasBootstrapMethods = false;
    int itemIndex = 1;
    while (itemIndex < constantPoolCount) {
      int itemOffset = classReader.getItem(itemIndex);
      int itemTag = inputBytes[itemOffset - 1];
      int nameAndTypeItemOffset;
      switch (itemTag) {
        case Symbol.CONSTANT_FIELDREF_TAG:
        case Symbol.CONSTANT_METHODREF_TAG:
        case Symbol.CONSTANT_INTERFACE_METHODREF_TAG:
          nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
          addConstantMemberReference(
              itemIndex,
              itemTag,
              classReader.readClass(itemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
          break;
        case Symbol.CONSTANT_INTEGER_TAG:
        case Symbol.CONSTANT_FLOAT_TAG:
          addConstantIntegerOrFloat(itemIndex, itemTag, classReader.readInt(itemOffset));
          break;
        case Symbol.CONSTANT_NAME_AND_TYPE_TAG:
          addConstantNameAndType(
              itemIndex,
              classReader.readUTF8(itemOffset, charBuffer),
              classReader.readUTF8(itemOffset + 2, charBuffer));
          break;
        case Symbol.CONSTANT_LONG_TAG:
        case Symbol.CONSTANT_DOUBLE_TAG:
          addConstantLongOrDouble(itemIndex, itemTag, classReader.readLong(itemOffset));
          break;
        case Symbol.CONSTANT_UTF8_TAG:
          addConstantUtf8(itemIndex, classReader.readUtf(itemIndex, charBuffer));
          break;
        case Symbol.CONSTANT_METHOD_HANDLE_TAG:
          int memberRefItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 1));
          nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(memberRefItemOffset + 2));
          addConstantMethodHandle(
              itemIndex,
              classReader.readByte(itemOffset),
              classReader.readClass(memberRefItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer));
          break;
        case Symbol.CONSTANT_DYNAMIC_TAG:
        case Symbol.CONSTANT_INVOKE_DYNAMIC_TAG:
          hasBootstrapMethods = true;
          nameAndTypeItemOffset =
              classReader.getItem(classReader.readUnsignedShort(itemOffset + 2));
          addConstantDynamicOrInvokeDynamicReference(
              itemTag,
              itemIndex,
              classReader.readUTF8(nameAndTypeItemOffset, charBuffer),
              classReader.readUTF8(nameAndTypeItemOffset + 2, charBuffer),
              classReader.readUnsignedShort(itemOffset));
          break;
        case Symbol.CONSTANT_STRING_TAG:
        case Symbol.CONSTANT_CLASS_TAG:
        case Symbol.CONSTANT_METHOD_TYPE_TAG:
        case Symbol.CONSTANT_MODULE_TAG:
        case Symbol.CONSTANT_PACKAGE_TAG:
          addConstantUtf8Reference(
              itemIndex, itemTag, classReader.readUTF8(itemOffset, charBuffer));
          break;
        default:
          throw new IllegalArgumentException();
      }
      itemIndex +=
          (itemTag == Symbol.CONSTANT_LONG_TAG || itemTag == Symbol.CONSTANT_DOUBLE_TAG) ? 2 : 1;
    }

    // Copy the BootstrapMethods, if any.
    if (hasBootstrapMethods) {
      copyBootstrapMethods(classReader, charBuffer);
    }
  }

  /**
   * 读取 BootstrapMethods 的 'bootstrap_methods' 数组二进制内容，并将它们作为条目添加到 SymbolTable
   * 中。
   *
   * @param classReader 其引导方法必须被复制以初始化 SymbolTable 的 ClassReader。
   * @param charBuffer 用于读取常量池中字符串的缓冲区。
   */
  private void copyBootstrapMethods(final ClassReader classReader, final char[] charBuffer) {
    // Find attributOffset of the 'bootstrap_methods' array.
    byte[] inputBytes = classReader.classFileBuffer;
    int currentAttributeOffset = classReader.getFirstAttributeOffset();
    for (int i = classReader.readUnsignedShort(currentAttributeOffset - 2); i > 0; --i) {
      String attributeName = classReader.readUTF8(currentAttributeOffset, charBuffer);
      if (Constants.BOOTSTRAP_METHODS.equals(attributeName)) {
        bootstrapMethodCount = classReader.readUnsignedShort(currentAttributeOffset + 6);
        break;
      }
      currentAttributeOffset += 6 + classReader.readInt(currentAttributeOffset + 2);
    }
    if (bootstrapMethodCount > 0) {
      // Compute the offset and the length of the BootstrapMethods 'bootstrap_methods' array.
      int bootstrapMethodsOffset = currentAttributeOffset + 8;
      int bootstrapMethodsLength = classReader.readInt(currentAttributeOffset + 2) - 2;
      bootstrapMethods = new ByteVector(bootstrapMethodsLength);
      bootstrapMethods.putByteArray(inputBytes, bootstrapMethodsOffset, bootstrapMethodsLength);

      // Add each bootstrap method in the symbol table entries.
      int currentOffset = bootstrapMethodsOffset;
      for (int i = 0; i < bootstrapMethodCount; i++) {
        int offset = currentOffset - bootstrapMethodsOffset;
        int bootstrapMethodRef = classReader.readUnsignedShort(currentOffset);
        currentOffset += 2;
        int numBootstrapArguments = classReader.readUnsignedShort(currentOffset);
        currentOffset += 2;
        int hashCode = classReader.readConst(bootstrapMethodRef, charBuffer).hashCode();
        while (numBootstrapArguments-- > 0) {
          int bootstrapArgument = classReader.readUnsignedShort(currentOffset);
          currentOffset += 2;
          hashCode ^= classReader.readConst(bootstrapArgument, charBuffer).hashCode();
        }
        add(new Entry(i, Symbol.BOOTSTRAP_METHOD_TAG, offset, hashCode & 0x7FFFFFFF));
      }
    }
  }

  /**
   * 返回构造此 SymbolTable 所使用的 ClassReader。
   *
   * @return 构造此 SymbolTable 所用的 ClassReader，如果它是从头构造的，则为 {@literal null}。
   */
  ClassReader getSource() {
    return sourceClassReader;
  }

  /**
   * 返回此符号表所属类的主版本号。
   *
   * @return 此符号表所属类的主版本号。
   */
  int getMajorVersion() {
    return majorVersion;
  }

  /**
   * 返回此符号表所属类的内部名称。
   *
   * @return 此符号表所属类的内部名称。
   */
  String getClassName() {
    return className;
  }

  /**
   * 设置此符号表所属类的主版本号和名称。同时将类名添加到常量池中。
   *
   * @param majorVersion 一个 ClassFile 主版本号。
   * @param className 一个内部类名称。
   * @return 具有给定类名的新 Symbol 或已存在 Symbol 的常量池索引。
   */
  int setMajorVersionAndClassName(final int majorVersion, final String className) {
    this.majorVersion = majorVersion;
    this.className = className;
    return addConstantClass(className).index;
  }

  /**
   * 返回此符号表 constant_pool 数组中的项数（加 1）。
   *
   * @return 此符号表 constant_pool 数组中的项数（加 1）。
   */
  int getConstantPoolCount() {
    return constantPoolCount;
  }

  /**
   * 返回此符号表 constant_pool 数组的字节长度。
   *
   * @return 此符号表 constant_pool 数组的字节长度。
   */
  int getConstantPoolLength() {
    return constantPool.length;
  }

  /**
   * 将此符号表的 constant_pool 数组放入给定的 ByteVector，并在其前面加上 constant_pool_count 值。
   *
   * @param output 放置 JVMS ClassFile 的 constant_pool 数组的位置。
   */
  void putConstantPool(final ByteVector output) {
    output.putShort(constantPoolCount).putByteArray(constantPool.data, 0, constantPool.length);
  }

  /**
   * 返回此符号表 BootstrapMethods 属性的字节大小。同时将该属性名添加到常量池中。
   *
   * @return 此符号表 BootstrapMethods 属性的字节大小。
   */
  int computeBootstrapMethodsSize() {
    if (bootstrapMethods != null) {
      addConstantUtf8(Constants.BOOTSTRAP_METHODS);
      return 8 + bootstrapMethods.length;
    } else {
      return 0;
    }
  }

  /**
   * 将此符号表的 BootstrapMethods 属性放入给定的 ByteVector。其中包括 6 个属性头字节和
   * num_bootstrap_methods 值。
   *
   * @param output 放置 JVMS BootstrapMethods 属性的位置。
   */
  void putBootstrapMethods(final ByteVector output) {
    if (bootstrapMethods != null) {
      output
          .putShort(addConstantUtf8(Constants.BOOTSTRAP_METHODS))
          .putInt(bootstrapMethods.length + 2)
          .putShort(bootstrapMethodCount)
          .putByteArray(bootstrapMethods.data, 0, bootstrapMethods.length);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Generic symbol table entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回可能具有给定哈希码的条目列表。
   *
   * @param hashCode 一个 {@link Entry#hashCode} 值。
   * @return 可能具有给定哈希码的条目列表。该列表
   * 通过 {@link Entry#next} 字段存储。
   */
  private Entry get(final int hashCode) {
    return entries[hashCode % entries.length];
  }

  /**
   * 将给定条目放入 {@link #entries} 哈希集合。此方法<i>不</i>检查 {@link #entries} 是否已包含类似条
   * 目。必要时会调整 {@link #entries} 的大小，以尽可能避免哈希冲突（多个条目需要存储在同一个
   * {@link #entries} 数组索引处），同时保持合理的内存占用。
   *
   * @param entry 一个 Entry（必须尚未包含在 {@link #entries} 中）。
   * @return 给定的条目
   */
  private Entry put(final Entry entry) {
    if (entryCount > (entries.length * 3) / 4) {
      int currentCapacity = entries.length;
      int newCapacity = currentCapacity * 2 + 1;
      Entry[] newEntries = new Entry[newCapacity];
      for (int i = currentCapacity - 1; i >= 0; --i) {
        Entry currentEntry = entries[i];
        while (currentEntry != null) {
          int newCurrentEntryIndex = currentEntry.hashCode % newCapacity;
          Entry nextEntry = currentEntry.next;
          currentEntry.next = newEntries[newCurrentEntryIndex];
          newEntries[newCurrentEntryIndex] = currentEntry;
          currentEntry = nextEntry;
        }
      }
      entries = newEntries;
    }
    entryCount++;
    int index = entry.hashCode % entries.length;
    entry.next = entries[index];
    return entries[index] = entry;
  }

  /**
   * 将给定条目添加到 {@link #entries} 哈希集合。此方法<i>不</i>检查 {@link #entries} 是否已包含类似
   * 条目，也<i>不</i>在必要时调整 {@link #entries} 的大小。
   *
   * @param entry 一个 Entry（必须尚未包含在 {@link #entries} 中）。
   */
  private void add(final Entry entry) {
    entryCount++;
    int index = entry.hashCode % entries.length;
    entry.next = entries[index];
    entries[index] = entry;
  }

  // -----------------------------------------------------------------------------------------------
  // Constant pool entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * 将数字或字符串常量添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 要添加到常量池的常量的值。此参数必须是 {@link Integer}、{@link Byte}、
   * {@link Character}、{@link Short}、{@link Boolean}、{@link Float}、{@link Long}、
   * {@link Double}、{@link String}、{@link Type} 或 {@link Handle}。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstant(final Object value) {
    if (value instanceof Integer) {
      return addConstantInteger(((Integer) value).intValue());
    } else if (value instanceof Byte) {
      return addConstantInteger(((Byte) value).intValue());
    } else if (value instanceof Character) {
      return addConstantInteger(((Character) value).charValue());
    } else if (value instanceof Short) {
      return addConstantInteger(((Short) value).intValue());
    } else if (value instanceof Boolean) {
      return addConstantInteger(((Boolean) value).booleanValue() ? 1 : 0);
    } else if (value instanceof Float) {
      return addConstantFloat(((Float) value).floatValue());
    } else if (value instanceof Long) {
      return addConstantLong(((Long) value).longValue());
    } else if (value instanceof Double) {
      return addConstantDouble(((Double) value).doubleValue());
    } else if (value instanceof String) {
      return addConstantString((String) value);
    } else if (value instanceof Type) {
      Type type = (Type) value;
      int typeSort = type.getSort();
      if (typeSort == Type.OBJECT) {
        return addConstantClass(type.getInternalName());
      } else if (typeSort == Type.METHOD) {
        return addConstantMethodType(type.getDescriptor());
      } else { // type is a primitive or array type.
        return addConstantClass(type.getDescriptor());
      }
    } else if (value instanceof Handle) {
      Handle handle = (Handle) value;
      return addConstantMethodHandle(
          handle.getTag(),
          handle.getOwner(),
          handle.getName(),
          handle.getDesc(),
          handle.isInterface());
    } else if (value instanceof ConstantDynamic) {
      ConstantDynamic constantDynamic = (ConstantDynamic) value;
      return addConstantDynamic(
          constantDynamic.getName(),
          constantDynamic.getDescriptor(),
          constantDynamic.getBootstrapMethod(),
          constantDynamic.getBootstrapMethodArgumentsUnsafe());
    } else {
      throw new IllegalArgumentException("value " + value);
    }
  }

  /**
   * 将 CONSTANT_Class_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 类的内部名称。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantClass(final String value) {
    return addConstantUtf8Reference(Symbol.CONSTANT_CLASS_TAG, value);
  }

  /**
   * 将 CONSTANT_Fieldref_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param owner 类的内部名称。
   * @param name 字段名称。
   * @param descriptor 字段描述符。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantFieldref(final String owner, final String name, final String descriptor) {
    return addConstantMemberReference(Symbol.CONSTANT_FIELDREF_TAG, owner, name, descriptor);
  }

  /**
   * 将 CONSTANT_Methodref_info 或 CONSTANT_InterfaceMethodref_info 添加到此符号表的常量池。如果常量
   * 池中已包含类似项，则不做任何操作。
   *
   * @param owner 类的内部名称。
   * @param name 方法名称。
   * @param descriptor 方法描述符。
   * @param isInterface owner 是否为接口。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantMethodref(
      final String owner, final String name, final String descriptor, final boolean isInterface) {
    int tag = isInterface ? Symbol.CONSTANT_INTERFACE_METHODREF_TAG : Symbol.CONSTANT_METHODREF_TAG;
    return addConstantMemberReference(tag, owner, name, descriptor);
  }

  /**
   * 将 CONSTANT_Fieldref_info、CONSTANT_Methodref_info 或 CONSTANT_InterfaceMethodref_info 添加到此
   * 符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param tag {@link Symbol#CONSTANT_FIELDREF_TAG}、{@link Symbol#CONSTANT_METHODREF_TAG} 或
   * {@link Symbol#CONSTANT_INTERFACE_METHODREF_TAG} 之一。
   * @param owner 类的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  private Entry addConstantMemberReference(
      final int tag, final String owner, final String name, final String descriptor) {
    int hashCode = hash(tag, owner, name, descriptor);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.owner.equals(owner)
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.put122(
        tag, addConstantClass(owner).index, addConstantNameAndType(name, descriptor));
    return put(new Entry(constantPoolCount++, tag, owner, name, descriptor, 0, hashCode));
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_Fieldref_info、CONSTANT_Methodref_info 或
   * CONSTANT_InterfaceMethodref_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param tag {@link Symbol#CONSTANT_FIELDREF_TAG}、{@link Symbol#CONSTANT_METHODREF_TAG} 或
   * {@link Symbol#CONSTANT_INTERFACE_METHODREF_TAG} 之一。
   * @param owner 类的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   */
  private void addConstantMemberReference(
      final int index,
      final int tag,
      final String owner,
      final String name,
      final String descriptor) {
    add(new Entry(index, tag, owner, name, descriptor, 0, hash(tag, owner, name, descriptor)));
  }

  /**
   * 将 CONSTANT_String_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 一个字符串。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantString(final String value) {
    return addConstantUtf8Reference(Symbol.CONSTANT_STRING_TAG, value);
  }

  /**
   * 将 CONSTANT_Integer_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 一个 int。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantInteger(final int value) {
    return addConstantIntegerOrFloat(Symbol.CONSTANT_INTEGER_TAG, value);
  }

  /**
   * 将 CONSTANT_Float_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 一个 float。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantFloat(final float value) {
    return addConstantIntegerOrFloat(Symbol.CONSTANT_FLOAT_TAG, Float.floatToRawIntBits(value));
  }

  /**
   * 将 CONSTANT_Integer_info 或 CONSTANT_Float_info 添加到此符号表的常量池。如果常量池中已包含类似项
   * ，则不做任何操作。
   *
   * @param tag {@link Symbol#CONSTANT_INTEGER_TAG} 或 {@link Symbol#CONSTANT_FLOAT_TAG} 之一。
   * @param value 一个 int 或 float。
   * @return 具有给定 tag 和原始值的常量池常量。
   */
  private Symbol addConstantIntegerOrFloat(final int tag, final int value) {
    int hashCode = hash(tag, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag && entry.hashCode == hashCode && entry.data == value) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.putByte(tag).putInt(value);
    return put(new Entry(constantPoolCount++, tag, value, hashCode));
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_Integer_info 或 CONSTANT_Float_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param tag {@link Symbol#CONSTANT_INTEGER_TAG} 或 {@link Symbol#CONSTANT_FLOAT_TAG} 之一。
   * @param value 一个 int 或 float。
   */
  private void addConstantIntegerOrFloat(final int index, final int tag, final int value) {
    add(new Entry(index, tag, value, hash(tag, value)));
  }

  /**
   * 将 CONSTANT_Long_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 一个 long。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantLong(final long value) {
    return addConstantLongOrDouble(Symbol.CONSTANT_LONG_TAG, value);
  }

  /**
   * 将 CONSTANT_Double_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 一个 double。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantDouble(final double value) {
    return addConstantLongOrDouble(Symbol.CONSTANT_DOUBLE_TAG, Double.doubleToRawLongBits(value));
  }

  /**
   * 将 CONSTANT_Long_info 或 CONSTANT_Double_info 添加到此符号表的常量池。如果常量池中已包含类似项，
   * 则不做任何操作。
   *
   * @param tag {@link Symbol#CONSTANT_LONG_TAG} 或 {@link Symbol#CONSTANT_DOUBLE_TAG} 之一。
   * @param value 一个 long 或 double。
   * @return 具有给定 tag 和原始值的常量池常量。
   */
  private Symbol addConstantLongOrDouble(final int tag, final long value) {
    int hashCode = hash(tag, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag && entry.hashCode == hashCode && entry.data == value) {
        return entry;
      }
      entry = entry.next;
    }
    int index = constantPoolCount;
    constantPool.putByte(tag).putLong(value);
    constantPoolCount += 2;
    return put(new Entry(index, tag, value, hashCode));
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_Long_info 或 CONSTANT_Double_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param tag {@link Symbol#CONSTANT_LONG_TAG} 或 {@link Symbol#CONSTANT_DOUBLE_TAG} 之一。
   * @param value 一个 long 或 double。
   */
  private void addConstantLongOrDouble(final int index, final int tag, final long value) {
    add(new Entry(index, tag, value, hash(tag, value)));
  }

  /**
   * 将 CONSTANT_NameAndType_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  int addConstantNameAndType(final String name, final String descriptor) {
    final int tag = Symbol.CONSTANT_NAME_AND_TYPE_TAG;
    int hashCode = hash(tag, name, descriptor);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry.index;
      }
      entry = entry.next;
    }
    constantPool.put122(tag, addConstantUtf8(name), addConstantUtf8(descriptor));
    return put(new Entry(constantPoolCount++, tag, name, descriptor, hashCode)).index;
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_NameAndType_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   */
  private void addConstantNameAndType(final int index, final String name, final String descriptor) {
    final int tag = Symbol.CONSTANT_NAME_AND_TYPE_TAG;
    add(new Entry(index, tag, name, descriptor, hash(tag, name, descriptor)));
  }

  /**
   * 将 CONSTANT_Utf8_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param value 一个字符串。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  int addConstantUtf8(final String value) {
    int hashCode = hash(Symbol.CONSTANT_UTF8_TAG, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.CONSTANT_UTF8_TAG
          && entry.hashCode == hashCode
          && entry.value.equals(value)) {
        return entry.index;
      }
      entry = entry.next;
    }
    constantPool.putByte(Symbol.CONSTANT_UTF8_TAG).putUTF8(value);
    return put(new Entry(constantPoolCount++, Symbol.CONSTANT_UTF8_TAG, value, hashCode)).index;
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_String_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param value 一个字符串。
   */
  private void addConstantUtf8(final int index, final String value) {
    add(new Entry(index, Symbol.CONSTANT_UTF8_TAG, value, hash(Symbol.CONSTANT_UTF8_TAG, value)));
  }

  /**
   * 将 CONSTANT_MethodHandle_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param referenceKind {@link Opcodes#H_GETFIELD}、{@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、
   * {@link Opcodes#H_PUTSTATIC}、{@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、
   * {@link Opcodes#H_INVOKESPECIAL}、{@link Opcodes#H_NEWINVOKESPECIAL} 或
   * {@link Opcodes#H_INVOKEINTERFACE} 之一。
   * @param owner 类或接口的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   * @param isInterface owner 是否为接口。
   * @return 具有给定值的新 Symbol 或已存在的 Symbol。
   */
  Symbol addConstantMethodHandle(
      final int referenceKind,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    final int tag = Symbol.CONSTANT_METHOD_HANDLE_TAG;
    // Note that we don't need to include isInterface in the hash computation, because it is
    // redundant with owner (we can't have the same owner with different isInterface values).
    int hashCode = hash(tag, owner, name, descriptor, referenceKind);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.data == referenceKind
          && entry.owner.equals(owner)
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry;
      }
      entry = entry.next;
    }
    if (referenceKind <= Opcodes.H_PUTSTATIC) {
      constantPool.put112(tag, referenceKind, addConstantFieldref(owner, name, descriptor).index);
    } else {
      constantPool.put112(
          tag, referenceKind, addConstantMethodref(owner, name, descriptor, isInterface).index);
    }
    return put(
        new Entry(constantPoolCount++, tag, owner, name, descriptor, referenceKind, hashCode));
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_MethodHandle_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param referenceKind {@link Opcodes#H_GETFIELD}、{@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、
   * {@link Opcodes#H_PUTSTATIC}、{@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、
   * {@link Opcodes#H_INVOKESPECIAL}、
   * {@link Opcodes#H_NEWINVOKESPECIAL} 或 {@link Opcodes#H_INVOKEINTERFACE} 之一。
   * @param owner 类或接口的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   */
  private void addConstantMethodHandle(
      final int index,
      final int referenceKind,
      final String owner,
      final String name,
      final String descriptor) {
    final int tag = Symbol.CONSTANT_METHOD_HANDLE_TAG;
    int hashCode = hash(tag, owner, name, descriptor, referenceKind);
    add(new Entry(index, tag, owner, name, descriptor, referenceKind, hashCode));
  }

  /**
   * 将 CONSTANT_MethodType_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param methodDescriptor 方法描述符。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  Symbol addConstantMethodType(final String methodDescriptor) {
    return addConstantUtf8Reference(Symbol.CONSTANT_METHOD_TYPE_TAG, methodDescriptor);
  }

  /**
   * 将 CONSTANT_Dynamic_info 添加到此符号表的常量池。同时将相关的引导方法添加到此符号表的
   * BootstrapMethods。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param name 方法名称。
   * @param descriptor 字段描述符。
   * @param bootstrapMethodHandle 引导方法句柄。
   * @param bootstrapMethodArguments 引导方法参数。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  Symbol addConstantDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    Symbol bootstrapMethod = addBootstrapMethod(bootstrapMethodHandle, bootstrapMethodArguments);
    return addConstantDynamicOrInvokeDynamicReference(
        Symbol.CONSTANT_DYNAMIC_TAG, name, descriptor, bootstrapMethod.index);
  }

  /**
   * 将 CONSTANT_InvokeDynamic_info 添加到此符号表的常量池。同时将相关的引导方法添加到此符号表的
   * BootstrapMethods。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param name 方法名称。
   * @param descriptor 方法描述符。
   * @param bootstrapMethodHandle 引导方法句柄。
   * @param bootstrapMethodArguments 引导方法参数。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  Symbol addConstantInvokeDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    Symbol bootstrapMethod = addBootstrapMethod(bootstrapMethodHandle, bootstrapMethodArguments);
    return addConstantDynamicOrInvokeDynamicReference(
        Symbol.CONSTANT_INVOKE_DYNAMIC_TAG, name, descriptor, bootstrapMethod.index);
  }

  /**
   * 将 CONSTANT_Dynamic 或 CONSTANT_InvokeDynamic_info 添加到此符号表的常量池。如果常量池中已包含类
   * 似项，则不做任何操作。
   *
   * @param tag {@link Symbol#CONSTANT_DYNAMIC_TAG} 或 {@link Symbol#CONSTANT_INVOKE_DYNAMIC_TAG} 之一。
   * @param name 方法名称。
   * @param descriptor CONSTANT_DYNAMIC_TAG 的字段描述符，或 CONSTANT_INVOKE_DYNAMIC_TAG 的方法描述符。
   * @param bootstrapMethodIndex BootstrapMethods 属性中引导方法的索引。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  private Symbol addConstantDynamicOrInvokeDynamicReference(
      final int tag, final String name, final String descriptor, final int bootstrapMethodIndex) {
    int hashCode = hash(tag, name, descriptor, bootstrapMethodIndex);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag
          && entry.hashCode == hashCode
          && entry.data == bootstrapMethodIndex
          && entry.name.equals(name)
          && entry.value.equals(descriptor)) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.put122(tag, bootstrapMethodIndex, addConstantNameAndType(name, descriptor));
    return put(
        new Entry(
            constantPoolCount++, tag, null, name, descriptor, bootstrapMethodIndex, hashCode));
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_Dynamic_info 或 CONSTANT_InvokeDynamic_info。
   *
   * @param tag {@link Symbol#CONSTANT_DYNAMIC_TAG} 或 {@link Symbol#CONSTANT_INVOKE_DYNAMIC_TAG} 之一。
   * @param index 新 Symbol 的常量池索引。
   * @param name 方法名称。
   * @param descriptor CONSTANT_DYNAMIC_TAG 的字段描述符或 CONSTANT_INVOKE_DYNAMIC_TAG 的方法描述符。
   * @param bootstrapMethodIndex BootstrapMethods 属性中引导方法的索引。
   */
  private void addConstantDynamicOrInvokeDynamicReference(
      final int tag,
      final int index,
      final String name,
      final String descriptor,
      final int bootstrapMethodIndex) {
    int hashCode = hash(tag, name, descriptor, bootstrapMethodIndex);
    add(new Entry(index, tag, null, name, descriptor, bootstrapMethodIndex, hashCode));
  }

  /**
   * 将 CONSTANT_Module_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param moduleName 模块的完全限定名称（使用点号）。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  Symbol addConstantModule(final String moduleName) {
    return addConstantUtf8Reference(Symbol.CONSTANT_MODULE_TAG, moduleName);
  }

  /**
   * 将 CONSTANT_Package_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param packageName 包的内部名称。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  Symbol addConstantPackage(final String packageName) {
    return addConstantUtf8Reference(Symbol.CONSTANT_PACKAGE_TAG, packageName);
  }

  /**
   * 将 CONSTANT_Class_info、CONSTANT_String_info、CONSTANT_MethodType_info、CONSTANT_Module_info 或
   * CONSTANT_Package_info 添加到此符号表的常量池。如果常量池中已包含类似项，则不做任何操作。
   *
   * @param tag {@link Symbol#CONSTANT_CLASS_TAG}、{@link Symbol#CONSTANT_STRING_TAG}、
   * {@link Symbol#CONSTANT_METHOD_TYPE_TAG}、
   * {@link Symbol#CONSTANT_MODULE_TAG} 或 {@link Symbol#CONSTANT_PACKAGE_TAG} 之一。
   * @param value 内部类名、任意字符串、方法描述符、模块名或包名，具体取决于 tag。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  private Symbol addConstantUtf8Reference(final int tag, final String value) {
    int hashCode = hash(tag, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == tag && entry.hashCode == hashCode && entry.value.equals(value)) {
        return entry;
      }
      entry = entry.next;
    }
    constantPool.put12(tag, addConstantUtf8(value));
    return put(new Entry(constantPoolCount++, tag, value, hashCode));
  }

  /**
   * 向此符号表的常量池中新增一个 CONSTANT_Class_info、CONSTANT_String_info、CONSTANT_MethodType_info
   * 、CONSTANT_Module_info 或 CONSTANT_Package_info。
   *
   * @param index 新 Symbol 的常量池索引。
   * @param tag {@link Symbol#CONSTANT_CLASS_TAG}、{@link Symbol#CONSTANT_STRING_TAG}、
   * {@link Symbol#CONSTANT_METHOD_TYPE_TAG}、
   * {@link Symbol#CONSTANT_MODULE_TAG} 或 {@link Symbol#CONSTANT_PACKAGE_TAG} 之一。
   * @param value 内部类名、任意字符串、方法描述符、模块名或包名，具体取决于 tag。
   */
  private void addConstantUtf8Reference(final int index, final int tag, final String value) {
    add(new Entry(index, tag, value, hash(tag, value)));
  }

  // -----------------------------------------------------------------------------------------------
  // Bootstrap method entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * 将引导方法添加到此符号表的 BootstrapMethods 属性。如果 BootstrapMethods 中已包含类似的引导方法，
   * 则不做任何操作。
   *
   * @param bootstrapMethodHandle 引导方法句柄。
   * @param bootstrapMethodArguments 引导方法参数。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  Symbol addBootstrapMethod(
      final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
    ByteVector bootstrapMethodsAttribute = bootstrapMethods;
    if (bootstrapMethodsAttribute == null) {
      bootstrapMethodsAttribute = bootstrapMethods = new ByteVector();
    }

    // The bootstrap method arguments can be Constant_Dynamic values, which reference other
    // bootstrap methods. We must therefore add the bootstrap method arguments to the constant pool
    // and BootstrapMethods attribute first, so that the BootstrapMethods attribute is not modified
    // while adding the given bootstrap method to it, in the rest of this method.
    int numBootstrapArguments = bootstrapMethodArguments.length;
    int[] bootstrapMethodArgumentIndexes = new int[numBootstrapArguments];
    for (int i = 0; i < numBootstrapArguments; i++) {
      bootstrapMethodArgumentIndexes[i] = addConstant(bootstrapMethodArguments[i]).index;
    }

    // Write the bootstrap method in the BootstrapMethods table. This is necessary to be able to
    // compare it with existing ones, and will be reverted below if there is already a similar
    // bootstrap method.
    int bootstrapMethodOffset = bootstrapMethodsAttribute.length;
    bootstrapMethodsAttribute.putShort(
        addConstantMethodHandle(
                bootstrapMethodHandle.getTag(),
                bootstrapMethodHandle.getOwner(),
                bootstrapMethodHandle.getName(),
                bootstrapMethodHandle.getDesc(),
                bootstrapMethodHandle.isInterface())
            .index);

    bootstrapMethodsAttribute.putShort(numBootstrapArguments);
    for (int i = 0; i < numBootstrapArguments; i++) {
      bootstrapMethodsAttribute.putShort(bootstrapMethodArgumentIndexes[i]);
    }

    // Compute the length and the hash code of the bootstrap method.
    int bootstrapMethodlength = bootstrapMethodsAttribute.length - bootstrapMethodOffset;
    int hashCode = bootstrapMethodHandle.hashCode();
    for (Object bootstrapMethodArgument : bootstrapMethodArguments) {
      hashCode ^= bootstrapMethodArgument.hashCode();
    }
    hashCode &= 0x7FFFFFFF;

    // Add the bootstrap method to the symbol table or revert the above changes.
    return addBootstrapMethod(bootstrapMethodOffset, bootstrapMethodlength, hashCode);
  }

  /**
   * 将引导方法添加到此符号表的 BootstrapMethods 属性。如果 BootstrapMethods 中已包含类似的引导方法，
   * 则不做任何操作（更确切地说，会回退 {@link #bootstrapMethods} 的内容以移除最后重复的引导方法）。
   *
   * @param offset {@link #bootstrapMethods} 中最后一个引导方法的偏移量，以字节为单位。
   * @param length {@link #bootstrapMethods} 中此引导方法的长度，以字节为单位。
   * @param hashCode 此引导方法的哈希码。
   * @return 具有给定值的新的或已存在的 Symbol。
   */
  private Symbol addBootstrapMethod(final int offset, final int length, final int hashCode) {
    final byte[] bootstrapMethodsData = bootstrapMethods.data;
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.BOOTSTRAP_METHOD_TAG && entry.hashCode == hashCode) {
        int otherOffset = (int) entry.data;
        boolean isSameBootstrapMethod = true;
        for (int i = 0; i < length; ++i) {
          if (bootstrapMethodsData[offset + i] != bootstrapMethodsData[otherOffset + i]) {
            isSameBootstrapMethod = false;
            break;
          }
        }
        if (isSameBootstrapMethod) {
          bootstrapMethods.length = offset; // Revert to old position.
          return entry;
        }
      }
      entry = entry.next;
    }
    return put(new Entry(bootstrapMethodCount++, Symbol.BOOTSTRAP_METHOD_TAG, offset, hashCode));
  }

  // -----------------------------------------------------------------------------------------------
  // Type table entries management.
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回索引为给定值的类型表元素。
   *
   * @param typeIndex 类型表索引。
   * @return 具有给定索引的类型表元素。
   */
  Symbol getType(final int typeIndex) {
    return typeTable[typeIndex];
  }

  /**
   * 向此符号表的类型表中添加一个类型。如果类型表中已包含类似类型，则不做任何操作。
   *
   * @param value 内部类名。
   * @return 具有给定值的新的或已存在的类型 Symbol 的索引。
   */
  int addType(final String value) {
    int hashCode = hash(Symbol.TYPE_TAG, value);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.TYPE_TAG && entry.hashCode == hashCode && entry.value.equals(value)) {
        return entry.index;
      }
      entry = entry.next;
    }
    return addTypeInternal(new Entry(typeCount, Symbol.TYPE_TAG, value, hashCode));
  }

  /**
   * 向此符号表的类型表中添加一个 {@link Frame#ITEM_UNINITIALIZED} 类型。如果类型表中已包含类似类型，
   * 则不做任何操作。
   *
   * @param value 内部类名。
   * @param bytecodeOffset 创建此 {@link Frame#ITEM_UNINITIALIZED} 类型值的 NEW 指令的字节码偏移量。
   * @return 具有给定值的新的或已存在的类型 Symbol 的索引。
   */
  int addUninitializedType(final String value, final int bytecodeOffset) {
    int hashCode = hash(Symbol.UNINITIALIZED_TYPE_TAG, value, bytecodeOffset);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.UNINITIALIZED_TYPE_TAG
          && entry.hashCode == hashCode
          && entry.data == bytecodeOffset
          && entry.value.equals(value)) {
        return entry.index;
      }
      entry = entry.next;
    }
    return addTypeInternal(
        new Entry(typeCount, Symbol.UNINITIALIZED_TYPE_TAG, value, bytecodeOffset, hashCode));
  }

  /**
   * 向此符号表的类型表中添加一个合并类型。如果类型表中已包含类似类型，则不做任何操作。
   *
   * @param typeTableIndex1 一个 {@link Symbol#TYPE_TAG} 类型，通过其在类型表中的索引指定。
   * @param typeTableIndex2 另一个 {@link Symbol#TYPE_TAG} 类型，通过其在类型表中的索引指定。
   * @return 对应于给定类型的公共超类的、新的或已存在的 {@link Symbol#TYPE_TAG} 类型 Symbol 的索引。
   */
  int addMergedType(final int typeTableIndex1, final int typeTableIndex2) {
    long data =
        typeTableIndex1 < typeTableIndex2
            ? typeTableIndex1 | (((long) typeTableIndex2) << 32)
            : typeTableIndex2 | (((long) typeTableIndex1) << 32);
    int hashCode = hash(Symbol.MERGED_TYPE_TAG, typeTableIndex1 + typeTableIndex2);
    Entry entry = get(hashCode);
    while (entry != null) {
      if (entry.tag == Symbol.MERGED_TYPE_TAG && entry.hashCode == hashCode && entry.data == data) {
        return entry.info;
      }
      entry = entry.next;
    }
    String type1 = typeTable[typeTableIndex1].value;
    String type2 = typeTable[typeTableIndex2].value;
    int commonSuperTypeIndex = addType(classWriter.getCommonSuperClass(type1, type2));
    put(new Entry(typeCount, Symbol.MERGED_TYPE_TAG, data, hashCode)).info = commonSuperTypeIndex;
    return commonSuperTypeIndex;
  }

  /**
   * 将给定的类型 Symbol 添加到 {@link #typeTable}。
   *
   * @param entry 一个 {@link Symbol#TYPE_TAG} 或 {@link Symbol#UNINITIALIZED_TYPE_TAG} 类型符号。
   * 此 Symbol 的索引必须等于 {@link #typeCount} 的当前值。
   * @return 给定类型在 {@link #typeTable} 中被添加的索引，按假设也等于 entry 的索引。
   */
  private int addTypeInternal(final Entry entry) {
    if (typeTable == null) {
      typeTable = new Entry[16];
    }
    if (typeCount == typeTable.length) {
      Entry[] newTypeTable = new Entry[2 * typeTable.length];
      System.arraycopy(typeTable, 0, newTypeTable, 0, typeTable.length);
      typeTable = newTypeTable;
    }
    typeTable[typeCount++] = entry;
    return put(entry).index;
  }

  // -----------------------------------------------------------------------------------------------
  // Static helper methods to compute hash codes.
  // -----------------------------------------------------------------------------------------------

  private static int hash(final int tag, final int value) {
    return 0x7FFFFFFF & (tag + value);
  }

  private static int hash(final int tag, final long value) {
    return 0x7FFFFFFF & (tag + (int) value + (int) (value >>> 32));
  }

  private static int hash(final int tag, final String value) {
    return 0x7FFFFFFF & (tag + value.hashCode());
  }

  private static int hash(final int tag, final String value1, final int value2) {
    return 0x7FFFFFFF & (tag + value1.hashCode() + value2);
  }

  private static int hash(final int tag, final String value1, final String value2) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode());
  }

  private static int hash(
      final int tag, final String value1, final String value2, final int value3) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode() * (value3 + 1));
  }

  private static int hash(
      final int tag, final String value1, final String value2, final String value3) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode() * value3.hashCode());
  }

  private static int hash(
      final int tag,
      final String value1,
      final String value2,
      final String value3,
      final int value4) {
    return 0x7FFFFFFF & (tag + value1.hashCode() * value2.hashCode() * value3.hashCode() * value4);
  }

  /**
   * SymbolTable 的一个条目。这个私有具体子类继承 {@link Symbol}，并添加了两个仅在 SymbolTable 内部使
   * 用的字段，用于实现符号的哈希集合（以避免符号重复）。请参见 {@link #entries}。
   *
   * @author Eric Bruneton
   */
  private static class Entry extends Symbol {

    /** 此条目的哈希码。 */
    final int hashCode;

    /**
     * 另一个（递归地还有更多）与此条目具有相同哈希码（对 {@link #entries} 的大小取模）的条目。
     */
    Entry next;

    Entry(
        final int index,
        final int tag,
        final String owner,
        final String name,
        final String value,
        final long data,
        final int hashCode) {
      super(index, tag, owner, name, value, data);
      this.hashCode = hashCode;
    }

    Entry(final int index, final int tag, final String value, final int hashCode) {
      super(index, tag, /* owner = */ null, /* name = */ null, value, /* data = */ 0);
      this.hashCode = hashCode;
    }

    Entry(final int index, final int tag, final String value, final long data, final int hashCode) {
      super(index, tag, /* owner = */ null, /* name = */ null, value, data);
      this.hashCode = hashCode;
    }

    Entry(
        final int index, final int tag, final String name, final String value, final int hashCode) {
      super(index, tag, /* owner = */ null, name, value, /* data = */ 0);
      this.hashCode = hashCode;
    }

    Entry(final int index, final int tag, final long data, final int hashCode) {
      super(index, tag, /* owner = */ null, /* name = */ null, /* value = */ null, data);
      this.hashCode = hashCode;
    }
  }
}
