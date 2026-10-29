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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Java 字段或方法类型。借助此类可以更轻松地操作类型描述符和方法描述符。
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
public final class Type {

  /** {@code void} 类型的类别。参见 {@link #getSort}。 */
  public static final int VOID = 0;

  /** {@code boolean} 类型的类别。参见 {@link #getSort}。 */
  public static final int BOOLEAN = 1;

  /** {@code char} 类型的类别。参见 {@link #getSort}。 */
  public static final int CHAR = 2;

  /** {@code byte} 类型的类别。参见 {@link #getSort}。 */
  public static final int BYTE = 3;

  /** {@code short} 类型的类别。参见 {@link #getSort}。 */
  public static final int SHORT = 4;

  /** {@code int} 类型的类别。参见 {@link #getSort}。 */
  public static final int INT = 5;

  /** {@code float} 类型的类别。参见 {@link #getSort}。 */
  public static final int FLOAT = 6;

  /** {@code long} 类型的类别。参见 {@link #getSort}。 */
  public static final int LONG = 7;

  /** {@code double} 类型的类别。参见 {@link #getSort}。 */
  public static final int DOUBLE = 8;

  /** 数组引用类型的类别。参见 {@link #getSort}。 */
  public static final int ARRAY = 9;

  /** 对象引用类型的类别。参见 {@link #getSort}。 */
  public static final int OBJECT = 10;

  /** 方法类型的类别。参见 {@link #getSort}。 */
  public static final int METHOD = 11;

  /** （私有）用内部名称表示的对象引用类型的类别。 */
  private static final int INTERNAL = 12;

  /** 基本类型的描述符。 */
  private static final String PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

  /** {@code void} 类型。 */
  public static final Type VOID_TYPE = new Type(VOID, PRIMITIVE_DESCRIPTORS, VOID, VOID + 1);

  /** {@code boolean} 类型。 */
  public static final Type BOOLEAN_TYPE =
      new Type(BOOLEAN, PRIMITIVE_DESCRIPTORS, BOOLEAN, BOOLEAN + 1);

  /** {@code char} 类型。 */
  public static final Type CHAR_TYPE = new Type(CHAR, PRIMITIVE_DESCRIPTORS, CHAR, CHAR + 1);

  /** {@code byte} 类型。 */
  public static final Type BYTE_TYPE = new Type(BYTE, PRIMITIVE_DESCRIPTORS, BYTE, BYTE + 1);

  /** {@code short} 类型。 */
  public static final Type SHORT_TYPE = new Type(SHORT, PRIMITIVE_DESCRIPTORS, SHORT, SHORT + 1);

  /** The {@code int} type. */
  public static final Type INT_TYPE = new Type(INT, PRIMITIVE_DESCRIPTORS, INT, INT + 1);

  /** {@code float} 类型。 */
  public static final Type FLOAT_TYPE = new Type(FLOAT, PRIMITIVE_DESCRIPTORS, FLOAT, FLOAT + 1);

  /** {@code long} 类型。 */
  public static final Type LONG_TYPE = new Type(LONG, PRIMITIVE_DESCRIPTORS, LONG, LONG + 1);

  /** {@code double} 类型。 */
  public static final Type DOUBLE_TYPE =
      new Type(DOUBLE, PRIMITIVE_DESCRIPTORS, DOUBLE, DOUBLE + 1);

  // -----------------------------------------------------------------------------------------------
  // Fields
  // -----------------------------------------------------------------------------------------------

  /**
   * 此类型的种类。取值为 {@link #VOID}、{@link #BOOLEAN}、{@link #CHAR}、{@link #BYTE}、
   * {@link #SHORT}、{@link #INT}、{@link #FLOAT}、{@link #LONG}、{@link #DOUBLE}、{@link #ARRAY}、
   * {@link #OBJECT}、{@link #METHOD} 或 {@link #INTERNAL}。
   */
  private final int sort;

  /**
   * 包含此字段或方法类型值的缓冲区。对于 {@link #OBJECT} 和 {@link #INTERNAL} 类型，该值是内部名称；
   * 其他情况下则是字段描述符或方法描述符。
   *
   * <p>对于 {@link #OBJECT} 类型，此字段还包含描述符：区间 [{@link #valueBegin},{@link #valueEnd})
   * 中的字符是内部名称，区间 [{@link #valueBegin} - 1, {@link #valueEnd} + 1) 中的字符则是描述符。
   */
  private final String valueBuffer;

  /**
   * 此 Java 字段或方法类型的值在 {@link #valueBuffer} 中的起始索引（含）。对于 {@link #OBJECT} 和
   * {@link #INTERNAL} 类型，该值是内部名称；其他情况下则是字段描述符或方法描述符。
   */
  private final int valueBegin;

  /**
   * 此 Java 字段或方法类型的值在 {@link #valueBuffer} 中的结束索引（不含）。对于 {@link #OBJECT} 和
   * {@link #INTERNAL} 类型，该值是内部名称；其他情况下则是字段描述符或方法描述符。
   */
  private final int valueEnd;

  /**
   * 构造一个引用类型。
   *
   * @param sort the sort of this type, see {@link #sort}.
   * @param valueBuffer a buffer containing the value of this field or method type.
   * @param valueBegin the beginning index, inclusive, of the value of this field or method type in
   *     valueBuffer.
   * @param valueEnd the end index, exclusive, of the value of this field or method type in
   *     valueBuffer.
   */
  private Type(final int sort, final String valueBuffer, final int valueBegin, final int valueEnd) {
    this.sort = sort;
    this.valueBuffer = valueBuffer;
    this.valueBegin = valueBegin;
    this.valueEnd = valueEnd;
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get Type(s) from a descriptor, a reflected Method or Constructor, other types, etc.
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回给定类型描述符对应的 {@link Type}。
   *
   * @param typeDescriptor a field or method type descriptor.
   * @return the {@link Type} corresponding to the given type descriptor.
   */
  public static Type getType(final String typeDescriptor) {
    return getTypeInternal(typeDescriptor, 0, typeDescriptor.length());
  }

  /**
   * 返回给定类对应的 {@link Type}。
   *
   * @param clazz a class.
   * @return the {@link Type} corresponding to the given class.
   */
  public static Type getType(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == Integer.TYPE) {
        return INT_TYPE;
      } else if (clazz == Void.TYPE) {
        return VOID_TYPE;
      } else if (clazz == Boolean.TYPE) {
        return BOOLEAN_TYPE;
      } else if (clazz == Byte.TYPE) {
        return BYTE_TYPE;
      } else if (clazz == Character.TYPE) {
        return CHAR_TYPE;
      } else if (clazz == Short.TYPE) {
        return SHORT_TYPE;
      } else if (clazz == Double.TYPE) {
        return DOUBLE_TYPE;
      } else if (clazz == Float.TYPE) {
        return FLOAT_TYPE;
      } else if (clazz == Long.TYPE) {
        return LONG_TYPE;
      } else {
        throw new AssertionError();
      }
    } else {
      return getType(getDescriptor(clazz));
    }
  }

  /**
   * 返回给定构造方法对应的方法 {@link Type}。
   *
   * @param constructor a {@link Constructor} object.
   * @return the method {@link Type} corresponding to the given constructor.
   */
  public static Type getType(final Constructor<?> constructor) {
    return getType(getConstructorDescriptor(constructor));
  }

  /**
   * 返回给定方法对应的方法 {@link Type}。
   *
   * @param method a {@link Method} object.
   * @return the method {@link Type} corresponding to the given method.
   */
  public static Type getType(final Method method) {
    return getType(getMethodDescriptor(method));
  }

  /**
   * 返回此数组类型各元素的类型。此方法只能用于数组类型。
   *
   * @return Returns the type of the elements of this array type.
   */
  public Type getElementType() {
    final int numDimensions = getDimensions();
    return getTypeInternal(valueBuffer, valueBegin + numDimensions, valueEnd);
  }

  /**
   * 返回给定内部名称对应的 {@link Type}。
   *
   * @param internalName an internal name (see {@link Type#getInternalName()}).
   * @return the {@link Type} corresponding to the given internal name.
   */
  public static Type getObjectType(final String internalName) {
    return new Type(
        internalName.charAt(0) == '[' ? ARRAY : INTERNAL, internalName, 0, internalName.length());
  }

  /**
   * 返回给定方法描述符对应的 {@link Type}。等价于 <code>Type.getType(methodDescriptor)</code>。
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} corresponding to the given method descriptor.
   */
  public static Type getMethodType(final String methodDescriptor) {
    return new Type(METHOD, methodDescriptor, 0, methodDescriptor.length());
  }

  /**
   * 返回给定参数类型和返回类型对应的方法 {@link Type}。
   *
   * @param returnType the return type of the method.
   * @param argumentTypes the argument types of the method.
   * @return the method {@link Type} corresponding to the given argument and return types.
   */
  public static Type getMethodType(final Type returnType, final Type... argumentTypes) {
    return getType(getMethodDescriptor(returnType, argumentTypes));
  }

  /**
   * 返回此类型各方法的参数类型。此方法只能用于方法类型。
   *
   * @return the argument types of methods of this type.
   */
  public Type[] getArgumentTypes() {
    return getArgumentTypes(getDescriptor());
  }

  /**
   * 返回给定方法描述符的各参数类型对应的 {@link Type} 值。
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} values corresponding to the argument types of the given method
   *     descriptor.
   */
  public static Type[] getArgumentTypes(final String methodDescriptor) {
    // First step: compute the number of argument types in methodDescriptor.
    int numArgumentTypes = 0;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Parse the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      ++numArgumentTypes;
    }

    // Second step: create a Type instance for each argument type.
    Type[] argumentTypes = new Type[numArgumentTypes];
    // Skip the first character, which is always a '('.
    currentOffset = 1;
    // Parse and create the argument types, one at each loop iteration.
    int currentArgumentTypeIndex = 0;
    while (methodDescriptor.charAt(currentOffset) != ')') {
      final int currentArgumentTypeOffset = currentOffset;
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      argumentTypes[currentArgumentTypeIndex++] =
          getTypeInternal(methodDescriptor, currentArgumentTypeOffset, currentOffset);
    }
    return argumentTypes;
  }

  /**
   * 返回给定方法的各参数类型对应的 {@link Type} 值。
   *
   * @param method a method.
   * @return the {@link Type} values corresponding to the argument types of the given method.
   */
  public static Type[] getArgumentTypes(final Method method) {
    Class<?>[] classes = method.getParameterTypes();
    Type[] types = new Type[classes.length];
    for (int i = classes.length - 1; i >= 0; --i) {
      types[i] = getType(classes[i]);
    }
    return types;
  }

  /**
   * 返回此类型各方法的返回类型。此方法只能用于方法类型。
   *
   * @return the return type of methods of this type.
   */
  public Type getReturnType() {
    return getReturnType(getDescriptor());
  }

  /**
   * 返回给定方法描述符的返回类型对应的 {@link Type}。
   *
   * @param methodDescriptor a method descriptor.
   * @return the {@link Type} corresponding to the return type of the given method descriptor.
   */
  public static Type getReturnType(final String methodDescriptor) {
    return getTypeInternal(
        methodDescriptor, getReturnTypeOffset(methodDescriptor), methodDescriptor.length());
  }

  /**
   * 返回给定方法的返回类型对应的 {@link Type}。
   *
   * @param method a method.
   * @return the {@link Type} corresponding to the return type of the given method.
   */
  public static Type getReturnType(final Method method) {
    return getType(method.getReturnType());
  }

  /**
   * 返回给定方法描述符中返回类型的起始索引。
   *
   * @param methodDescriptor a method descriptor.
   * @return the start index of the return type of the given method descriptor.
   */
  static int getReturnTypeOffset(final String methodDescriptor) {
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Skip the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
    }
    return currentOffset + 1;
  }

  /**
   * 返回给定字段描述符或方法描述符对应的 {@link Type}。
   *
   * @param descriptorBuffer a buffer containing the field or method descriptor.
   * @param descriptorBegin the beginning index, inclusive, of the field or method descriptor in
   *     descriptorBuffer.
   * @param descriptorEnd the end index, exclusive, of the field or method descriptor in
   *     descriptorBuffer.
   * @return the {@link Type} corresponding to the given type descriptor.
   */
  private static Type getTypeInternal(
      final String descriptorBuffer, final int descriptorBegin, final int descriptorEnd) {
    switch (descriptorBuffer.charAt(descriptorBegin)) {
      case 'V':
        return VOID_TYPE;
      case 'Z':
        return BOOLEAN_TYPE;
      case 'C':
        return CHAR_TYPE;
      case 'B':
        return BYTE_TYPE;
      case 'S':
        return SHORT_TYPE;
      case 'I':
        return INT_TYPE;
      case 'F':
        return FLOAT_TYPE;
      case 'J':
        return LONG_TYPE;
      case 'D':
        return DOUBLE_TYPE;
      case '[':
        return new Type(ARRAY, descriptorBuffer, descriptorBegin, descriptorEnd);
      case 'L':
        return new Type(OBJECT, descriptorBuffer, descriptorBegin + 1, descriptorEnd - 1);
      case '(':
        return new Type(METHOD, descriptorBuffer, descriptorBegin, descriptorEnd);
      default:
        throw new IllegalArgumentException("Invalid descriptor: " + descriptorBuffer);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get class names, internal names or descriptors.
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回此类型对应类的二进制名称。此方法不得用于方法类型。
   *
   * @return the binary name of the class corresponding to this type.
   */
  public String getClassName() {
    switch (sort) {
      case VOID:
        return "void";
      case BOOLEAN:
        return "boolean";
      case CHAR:
        return "char";
      case BYTE:
        return "byte";
      case SHORT:
        return "short";
      case INT:
        return "int";
      case FLOAT:
        return "float";
      case LONG:
        return "long";
      case DOUBLE:
        return "double";
      case ARRAY:
        StringBuilder stringBuilder = new StringBuilder(getElementType().getClassName());
        for (int i = getDimensions(); i > 0; --i) {
          stringBuilder.append("[]");
        }
        return stringBuilder.toString();
      case OBJECT:
      case INTERNAL:
        return valueBuffer.substring(valueBegin, valueEnd).replace('/', '.');
      default:
        throw new AssertionError();
    }
  }

  /**
   * 返回此对象类型或数组类型对应类的内部名称。类的内部名称是其完全限定名（即 Class.getName() 返回的
   * 名称，其中的 '.' 被替换为 '/'）。此方法只能用于对象类型或数组类型。
   *
   * @return the internal name of the class corresponding to this object type.
   */
  public String getInternalName() {
    return valueBuffer.substring(valueBegin, valueEnd);
  }

  /**
   * 返回给定类的内部名称。类的内部名称是其完全限定名（即 Class.getName() 返回的名称），其中的 '.' 被
   * 替换为 '/'。
   *
   * @param clazz an object or array class.
   * @return the internal name of the given class.
   */
  public static String getInternalName(final Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  /**
   * 返回此类型对应的描述符。
   *
   * @return the descriptor corresponding to this type.
   */
  public String getDescriptor() {
    if (sort == OBJECT) {
      return valueBuffer.substring(valueBegin - 1, valueEnd + 1);
    } else if (sort == INTERNAL) {
      return 'L' + valueBuffer.substring(valueBegin, valueEnd) + ';';
    } else {
      return valueBuffer.substring(valueBegin, valueEnd);
    }
  }

  /**
   * 返回给定类对应的描述符。
   *
   * @param clazz an object class, a primitive class or an array class.
   * @return the descriptor corresponding to the given class.
   */
  public static String getDescriptor(final Class<?> clazz) {
    StringBuilder stringBuilder = new StringBuilder();
    appendDescriptor(clazz, stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * 返回给定构造方法对应的描述符。
   *
   * @param constructor a {@link Constructor} object.
   * @return the descriptor of the given constructor.
   */
  public static String getConstructorDescriptor(final Constructor<?> constructor) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = constructor.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    return stringBuilder.append(")V").toString();
  }

  /**
   * 返回给定参数类型和返回类型对应的描述符。
   *
   * @param returnType the return type of the method.
   * @param argumentTypes the argument types of the method.
   * @return the descriptor corresponding to the given argument and return types.
   */
  public static String getMethodDescriptor(final Type returnType, final Type... argumentTypes) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    for (Type argumentType : argumentTypes) {
      argumentType.appendDescriptor(stringBuilder);
    }
    stringBuilder.append(')');
    returnType.appendDescriptor(stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * 返回给定方法对应的描述符。
   *
   * @param method a {@link Method} object.
   * @return the descriptor of the given method.
   */
  public static String getMethodDescriptor(final Method method) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    stringBuilder.append(')');
    appendDescriptor(method.getReturnType(), stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * 将此类型对应的描述符追加到给定的字符串缓冲区。
   *
   * @param stringBuilder the string builder to which the descriptor must be appended.
   */
  private void appendDescriptor(final StringBuilder stringBuilder) {
    if (sort == OBJECT) {
      stringBuilder.append(valueBuffer, valueBegin - 1, valueEnd + 1);
    } else if (sort == INTERNAL) {
      stringBuilder.append('L').append(valueBuffer, valueBegin, valueEnd).append(';');
    } else {
      stringBuilder.append(valueBuffer, valueBegin, valueEnd);
    }
  }

  /**
   * 将给定类的描述符追加到给定的字符串构建器。
   *
   * @param clazz the class whose descriptor must be computed.
   * @param stringBuilder the string builder to which the descriptor must be appended.
   */
  private static void appendDescriptor(final Class<?> clazz, final StringBuilder stringBuilder) {
    Class<?> currentClass = clazz;
    while (currentClass.isArray()) {
      stringBuilder.append('[');
      currentClass = currentClass.getComponentType();
    }
    if (currentClass.isPrimitive()) {
      char descriptor;
      if (currentClass == Integer.TYPE) {
        descriptor = 'I';
      } else if (currentClass == Void.TYPE) {
        descriptor = 'V';
      } else if (currentClass == Boolean.TYPE) {
        descriptor = 'Z';
      } else if (currentClass == Byte.TYPE) {
        descriptor = 'B';
      } else if (currentClass == Character.TYPE) {
        descriptor = 'C';
      } else if (currentClass == Short.TYPE) {
        descriptor = 'S';
      } else if (currentClass == Double.TYPE) {
        descriptor = 'D';
      } else if (currentClass == Float.TYPE) {
        descriptor = 'F';
      } else if (currentClass == Long.TYPE) {
        descriptor = 'J';
      } else {
        throw new AssertionError();
      }
      stringBuilder.append(descriptor);
    } else {
      stringBuilder.append('L').append(getInternalName(currentClass)).append(';');
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to get the sort, dimension, size, and opcodes corresponding to a Type or descriptor.
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回此类型的种类。
   *
   * @return {@link #VOID}, {@link #BOOLEAN}, {@link #CHAR}, {@link #BYTE}, {@link #SHORT}, {@link
   *     #INT}, {@link #FLOAT}, {@link #LONG}, {@link #DOUBLE}, {@link #ARRAY}, {@link #OBJECT} or
   *     {@link #METHOD}.
   */
  public int getSort() {
    return sort == INTERNAL ? OBJECT : sort;
  }

  /**
   * 返回此数组类型的维度数。此方法只能用于数组类型。
   *
   * @return the number of dimensions of this array type.
   */
  public int getDimensions() {
    int numDimensions = 1;
    while (valueBuffer.charAt(valueBegin + numDimensions) == '[') {
      numDimensions++;
    }
    return numDimensions;
  }

  /**
   * 返回此类型值的大小。此方法不得用于方法类型。
   *
   * @return the size of values of this type, i.e., 2 for {@code long} and {@code double}, 0 for
   *     {@code void} and 1 otherwise.
   */
  public int getSize() {
    switch (sort) {
      case VOID:
        return 0;
      case BOOLEAN:
      case CHAR:
      case BYTE:
      case SHORT:
      case INT:
      case FLOAT:
      case ARRAY:
      case OBJECT:
      case INTERNAL:
        return 1;
      case LONG:
      case DOUBLE:
        return 2;
      default:
        throw new AssertionError();
    }
  }

  /**
   * 返回此类型各方法的参数和返回值的大小。此方法只能用于方法类型。
   *
   * @return the size of the arguments of the method (plus one for the implicit this argument),
   *     argumentsSize, and the size of its return value, returnSize, packed into a single int i =
   *     {@code (argumentsSize &lt;&lt; 2) | returnSize} (argumentsSize is therefore equal to {@code
   *     i &gt;&gt; 2}, and returnSize to {@code i &amp; 0x03}).
   */
  public int getArgumentsAndReturnSizes() {
    return getArgumentsAndReturnSizes(getDescriptor());
  }

  /**
   * 计算方法的参数和返回值的大小。
   *
   * @param methodDescriptor a method descriptor.
   * @return the size of the arguments of the method (plus one for the implicit this argument),
   *     argumentsSize, and the size of its return value, returnSize, packed into a single int i =
   *     {@code (argumentsSize &lt;&lt; 2) | returnSize} (argumentsSize is therefore equal to {@code
   *     i &gt;&gt; 2}, and returnSize to {@code i &amp; 0x03}).
   */
  public static int getArgumentsAndReturnSizes(final String methodDescriptor) {
    int argumentsSize = 1;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    int currentChar = methodDescriptor.charAt(currentOffset);
    // Parse the argument types and compute their size, one at a each loop iteration.
    while (currentChar != ')') {
      if (currentChar == 'J' || currentChar == 'D') {
        currentOffset++;
        argumentsSize += 2;
      } else {
        while (methodDescriptor.charAt(currentOffset) == '[') {
          currentOffset++;
        }
        if (methodDescriptor.charAt(currentOffset++) == 'L') {
          // Skip the argument descriptor content.
          int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
          currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
        }
        argumentsSize += 1;
      }
      currentChar = methodDescriptor.charAt(currentOffset);
    }
    currentChar = methodDescriptor.charAt(currentOffset + 1);
    if (currentChar == 'V') {
      return argumentsSize << 2;
    } else {
      int returnSize = (currentChar == 'J' || currentChar == 'D') ? 2 : 1;
      return argumentsSize << 2 | returnSize;
    }
  }

  /**
   * 返回适用于此 {@link Type} 的 JVM 指令操作码。此方法不得用于方法类型。
   *
   * @param opcode a JVM instruction opcode. This opcode must be one of ILOAD, ISTORE, IALOAD,
   *     IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR and
   *     IRETURN.
   * @return an opcode that is similar to the given opcode, but adapted to this {@link Type}. For
   *     example, if this type is {@code float} and {@code opcode} is IRETURN, this method returns
   *     FRETURN.
   */
  public int getOpcode(final int opcode) {
    if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
      switch (sort) {
        case BOOLEAN:
        case BYTE:
          return opcode + (Opcodes.BALOAD - Opcodes.IALOAD);
        case CHAR:
          return opcode + (Opcodes.CALOAD - Opcodes.IALOAD);
        case SHORT:
          return opcode + (Opcodes.SALOAD - Opcodes.IALOAD);
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FALOAD - Opcodes.IALOAD);
        case LONG:
          return opcode + (Opcodes.LALOAD - Opcodes.IALOAD);
        case DOUBLE:
          return opcode + (Opcodes.DALOAD - Opcodes.IALOAD);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          return opcode + (Opcodes.AALOAD - Opcodes.IALOAD);
        case METHOD:
        case VOID:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    } else {
      switch (sort) {
        case VOID:
          if (opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return Opcodes.RETURN;
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FRETURN - Opcodes.IRETURN);
        case LONG:
          return opcode + (Opcodes.LRETURN - Opcodes.IRETURN);
        case DOUBLE:
          return opcode + (Opcodes.DRETURN - Opcodes.IRETURN);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return opcode + (Opcodes.ARETURN - Opcodes.IRETURN);
        case METHOD:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Equals, hashCode and toString.
  // -----------------------------------------------------------------------------------------------

  /**
   * 测试给定对象是否与此类型相等。
   *
   * @param object the object to be compared to this type.
   * @return {@literal true} if the given object is equal to this type.
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Type)) {
      return false;
    }
    Type other = (Type) object;
    if ((sort == INTERNAL ? OBJECT : sort) != (other.sort == INTERNAL ? OBJECT : other.sort)) {
      return false;
    }
    int begin = valueBegin;
    int end = valueEnd;
    int otherBegin = other.valueBegin;
    int otherEnd = other.valueEnd;
    // Compare the values.
    if (end - begin != otherEnd - otherBegin) {
      return false;
    }
    for (int i = begin, j = otherBegin; i < end; i++, j++) {
      if (valueBuffer.charAt(i) != other.valueBuffer.charAt(j)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 返回此类型的哈希码值。
   *
   * @return a hash code value for this type.
   */
  @Override
  public int hashCode() {
    int hashCode = 13 * (sort == INTERNAL ? OBJECT : sort);
    if (sort >= ARRAY) {
      for (int i = valueBegin, end = valueEnd; i < end; i++) {
        hashCode = 17 * (hashCode + valueBuffer.charAt(i));
      }
    }
    return hashCode;
  }

  /**
   * 返回此类型的字符串表示形式。
   *
   * @return the descriptor of this type.
   */
  @Override
  public String toString() {
    return getDescriptor();
  }
}
