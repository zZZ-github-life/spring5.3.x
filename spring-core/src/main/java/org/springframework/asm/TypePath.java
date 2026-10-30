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
 * 指向外层类型中的类型参数、通配符边界、数组元素类型或静态内部类型的路径。
 *
 * @author Eric Bruneton
 */
public final class TypePath {

  /** 进入数组类型元素类型的类型路径步骤。参见 {@link #getStep}。 */
  public static final int ARRAY_ELEMENT = 0;

  /** 进入类类型嵌套类型的类型路径步骤。参见 {@link #getStep}。 */
  public static final int INNER_TYPE = 1;

  /** 进入通配符类型边界的类型路径步骤。参见 {@link #getStep}。 */
  public static final int WILDCARD_BOUND = 2;

  /** 进入泛型类型类型参数的类型路径步骤。参见 {@link #getStep}。 */
  public static final int TYPE_ARGUMENT = 3;

  /**
   * 存储此 TypePath 对应的 'type_path' 结构（定义见 Java 虚拟机规范，JVMS）的字节数组。该结构在此数
   * 组中的第一个字节由 {@link #typePathOffset} 给出。
   *
   * @see <a
   *     href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.2">JVMS
   *     4.7.20.2</a>
   */
  private final byte[] typePathContainer;

  /** type_path JVMS 结构首字节在 {@link #typePathContainer} 中的偏移量。 */
  private final int typePathOffset;

  /**
   * 构造一个新的 TypePath。
   *
   * @param typePathContainer 包含 type_path JVMS 结构的字节数组。
   * @param typePathOffset type_path 结构首字节在 typePathContainer 中的偏移量。
   */
  TypePath(final byte[] typePathContainer, final int typePathOffset) {
    this.typePathContainer = typePathContainer;
    this.typePathOffset = typePathOffset;
  }

  /**
   * 返回此路径的长度，即其步数。
   *
   * @return 此路径的长度。
   */
  public int getLength() {
    // path_length is stored in the first byte of a type_path.
    return typePathContainer[typePathOffset];
  }

  /**
   * 返回此路径中给定步骤的值。
   *
   * @param index 介于 0 和 {@link #getLength()} 之间（不包含）的索引。
   * @return {@link #ARRAY_ELEMENT}、{@link #INNER_TYPE}、
   * {@link #WILDCARD_BOUND} 或 {@link #TYPE_ARGUMENT} 之一。
   */
  public int getStep(final int index) {
    // Returns the type_path_kind of the path element of the given index.
    return typePathContainer[typePathOffset + 2 * index + 1];
  }

  /**
   * 返回给定步骤所进入的类型参数的索引。此方法只能用于值为 {@link #TYPE_ARGUMENT} 的步骤。
   *
   * @param index 介于 0 和 {@link #getLength()} 之间（不包含）的索引。
   * @return 给定步骤所进入的类型实参的索引。
   */
  public int getStepArgument(final int index) {
    // Returns the type_argument_index of the path element of the given index.
    return typePathContainer[typePathOffset + 2 * index + 2];
  }

  /**
   * 将字符串形式的类型路径（格式与 {@link #toString()} 所用格式相同）转换为 TypePath 对象。
   *
   * @param typePath 字符串形式的类型路径，格式与 {@link #toString()} 使用的相同。可以为 {@literal null} 或空。
   * @return 对应的 TypePath 对象，若路径为空则为 {@literal null}。
   */
  public static TypePath fromString(final String typePath) {
    if (typePath == null || typePath.length() == 0) {
      return null;
    }
    int typePathLength = typePath.length();
    ByteVector output = new ByteVector(typePathLength);
    output.putByte(0);
    int typePathIndex = 0;
    while (typePathIndex < typePathLength) {
      char c = typePath.charAt(typePathIndex++);
      if (c == '[') {
        output.put11(ARRAY_ELEMENT, 0);
      } else if (c == '.') {
        output.put11(INNER_TYPE, 0);
      } else if (c == '*') {
        output.put11(WILDCARD_BOUND, 0);
      } else if (c >= '0' && c <= '9') {
        int typeArg = c - '0';
        while (typePathIndex < typePathLength) {
          c = typePath.charAt(typePathIndex++);
          if (c >= '0' && c <= '9') {
            typeArg = typeArg * 10 + c - '0';
          } else if (c == ';') {
            break;
          } else {
            throw new IllegalArgumentException();
          }
        }
        output.put11(TYPE_ARGUMENT, typeArg);
      } else {
        throw new IllegalArgumentException();
      }
    }
    output.data[0] = (byte) (output.length / 2);
    return new TypePath(output.data, 0);
  }

  /**
   * 返回此类型路径的字符串表示形式。{@link #ARRAY_ELEMENT} 步骤用 '[' 表示，{@link #INNER_TYPE} 步骤
   * 用 '.' 表示，{@link #WILDCARD_BOUND} 步骤用 '*' 表示，{@link #TYPE_ARGUMENT} 步骤用其类型参数索
   * 引的十进制形式后跟 ';' 表示。
   */
  @Override
  public String toString() {
    int length = getLength();
    StringBuilder result = new StringBuilder(length * 2);
    for (int i = 0; i < length; ++i) {
      switch (getStep(i)) {
        case ARRAY_ELEMENT:
          result.append('[');
          break;
        case INNER_TYPE:
          result.append('.');
          break;
        case WILDCARD_BOUND:
          result.append('*');
          break;
        case TYPE_ARGUMENT:
          result.append(getStepArgument(i)).append(';');
          break;
        default:
          throw new AssertionError();
      }
    }
    return result.toString();
  }

  /**
   * 将给定 TypePath 对应的 type_path JVMS 结构放入给定的 ByteVector。
   *
   * @param typePath 一个 TypePath 实例，空路径则为 {@literal null}。
   * @param output 必须写入类型路径的目标。
   */
  static void put(final TypePath typePath, final ByteVector output) {
    if (typePath == null) {
      output.putByte(0);
    } else {
      int length = typePath.typePathContainer[typePath.typePathOffset] * 2 + 1;
      output.putByteArray(typePath.typePathContainer, typePath.typePathOffset, length);
    }
  }
}
