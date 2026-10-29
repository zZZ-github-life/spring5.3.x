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
 * 基本块的输入和输出栈映射帧。
 *
 * <p>栈映射帧分两步计算：
 *
 * <ul>
 * <li>在 MethodWriter 访问每条指令的过程中，通过模拟指令对所谓“输出帧”先前状态的作用，更新当前基本块结束处的帧状态。
 * <li>所有指令访问完成后，在 MethodWriter 中使用不动点算法计算每个基本块的“输入帧”（即基本块开始处的栈映射帧）。参见 {@link MethodWriter#computeAllFrames}。
 * </ul>
 *
 * <p>输出栈映射帧是相对于基本块的输入帧计算的，而在计算输出帧时该输入帧尚不可知。因此，必须能够表
 * 示抽象类型，例如“输入帧局部变量中位置 x 处的类型”或“距输入帧栈顶 x 处的类型”，甚至是“输入帧中位
 * 置 x 处、数组维度多（或少）y 的类型”。这就解释了此类中使用的相当复杂的类型格式，下文将予以说明。
 *
 * <p>输入帧和输出帧的局部变量与操作数栈包含以下称为“抽象类型”的值。抽象类型由名为 DIM、KIND、FLAGS
 * 和 VALUE 的 4 个字段表示，为了获得更好的性能和内存效率，它们被打包在单个 int 值中：
 *
 * <pre>
 *   =====================================
 *   |...DIM|KIND|.F|...............VALUE|
 *   =====================================
 * </pre>
 *
 * <ul>
 * <li>DIM 字段存储于最高 6 位，是表示数组维度的有符号数（范围为 -32 到 31，含两端）。可以通过 {@link #DIM_MASK} 和 {@link #DIM_SHIFT} 右移来获取它。
 * <li>KIND 字段存储于 4 位，表示所用 VALUE 的种类。这 4 位可以通过 {@link #KIND_MASK} 获取，且无需任何移位，其值必须等于 {@link #CONSTANT_KIND}、{@link #REFERENCE_KIND}、{@link #UNINITIALIZED_KIND}、{@link #LOCAL_KIND} 或 {@link #STACK_KIND}。
 * <li>FLAGS 字段存储于 2 位，最多包含 2 个布尔标志。目前只定义了一个标志，即 {@link #TOP_IF_LONG_OR_DOUBLE_FLAG}。
 * <li>VALUE 字段存储于其余 20 位，包含以下之一：
 * <ul>
 * <li>如果 KIND 等于 {@link #CONSTANT_KIND}，则为以下常量之一：{@link #ITEM_TOP}、{@link #ITEM_ASM_BOOLEAN}、{@link #ITEM_ASM_BYTE}、{@link #ITEM_ASM_CHAR}、{@link #ITEM_ASM_SHORT}、{@link #ITEM_INTEGER}、{@link #ITEM_FLOAT}、{@link #ITEM_LONG}、{@link #ITEM_DOUBLE}、{@link #ITEM_NULL} 或 {@link #ITEM_UNINITIALIZED_THIS}。
 * <li>如果 KIND 等于 {@link #REFERENCE_KIND}，则为 {@link Symbol#TYPE_TAG} {@link Symbol} 在 {@link SymbolTable} 类型表中的索引。
 * <li>如果 KIND 等于 {@link #UNINITIALIZED_KIND}，则为 {@link Symbol#UNINITIALIZED_TYPE_TAG} {@link Symbol} 在 SymbolTable 类型表中的索引。
 * <li>如果 KIND 等于 {@link #LOCAL_KIND}，则为输入栈帧中局部变量的索引。
 * <li>如果 KIND 等于 {@link #STACK_KIND}，则为相对于输入栈帧栈顶的位置，
 *       </ul>
 * </ul>
 *
 * <p>输出帧可以包含任意种类以及数组维度为正或为负的抽象类型（甚至可以是未赋值的类型，用 0 表示——它
 * 不对应任何有效的抽象类型值）。输入帧只能包含数组维度为正或为 {@literal null} 的 CONSTANT_KIND、
 * REFERENCE_KIND 或 UNINITIALIZED_KIND 抽象类型。在所有情况下，类型表都只包含内部类型名（禁止使用
 * 数组类型描述符——数组维度必须通过 DIM 字段表示）。
 *
 * <p>LONG 和 DOUBLE 类型始终使用两个槽位表示（LONG + TOP 或 DOUBLE + TOP），无论是局部变量还是操作
 * 数栈中都是如此。这对于模拟 DUPx_y 指令是必要的，因为其效果取决于栈中抽象类型所表示的具体类型（而
 * 这些具体类型并不总是已知）。
 *
 * @author Eric Bruneton
 */
class Frame {

  // Constants used in the StackMapTable attribute.
  // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4.

  static final int SAME_FRAME = 0;
  static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64;
  static final int RESERVED = 128;
  static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247;
  static final int CHOP_FRAME = 248;
  static final int SAME_FRAME_EXTENDED = 251;
  static final int APPEND_FRAME = 252;
  static final int FULL_FRAME = 255;

  static final int ITEM_TOP = 0;
  static final int ITEM_INTEGER = 1;
  static final int ITEM_FLOAT = 2;
  static final int ITEM_DOUBLE = 3;
  static final int ITEM_LONG = 4;
  static final int ITEM_NULL = 5;
  static final int ITEM_UNINITIALIZED_THIS = 6;
  static final int ITEM_OBJECT = 7;
  static final int ITEM_UNINITIALIZED = 8;
  // Additional, ASM specific constants used in abstract types below.
  private static final int ITEM_ASM_BOOLEAN = 9;
  private static final int ITEM_ASM_BYTE = 10;
  private static final int ITEM_ASM_CHAR = 11;
  private static final int ITEM_ASM_SHORT = 12;

  // The size and offset in bits of each field of an abstract type.

  private static final int DIM_SIZE = 6;
  private static final int KIND_SIZE = 4;
  private static final int FLAGS_SIZE = 2;
  private static final int VALUE_SIZE = 32 - DIM_SIZE - KIND_SIZE - FLAGS_SIZE;

  private static final int DIM_SHIFT = KIND_SIZE + FLAGS_SIZE + VALUE_SIZE;
  private static final int KIND_SHIFT = FLAGS_SIZE + VALUE_SIZE;
  private static final int FLAGS_SHIFT = VALUE_SIZE;

  // Bitmasks to get each field of an abstract type.

  private static final int DIM_MASK = ((1 << DIM_SIZE) - 1) << DIM_SHIFT;
  private static final int KIND_MASK = ((1 << KIND_SIZE) - 1) << KIND_SHIFT;
  private static final int VALUE_MASK = (1 << VALUE_SIZE) - 1;

  // Constants to manipulate the DIM field of an abstract type.

  /** 加到抽象类型上以得到数组维度加一类型的常量。 */
  private static final int ARRAY_OF = +1 << DIM_SHIFT;

  /** 加到抽象类型上以得到数组维度减一类型的常量。 */
  private static final int ELEMENT_OF = -1 << DIM_SHIFT;

  // Possible values for the KIND field of an abstract type.

  private static final int CONSTANT_KIND = 1 << KIND_SHIFT;
  private static final int REFERENCE_KIND = 2 << KIND_SHIFT;
  private static final int UNINITIALIZED_KIND = 3 << KIND_SHIFT;
  private static final int LOCAL_KIND = 4 << KIND_SHIFT;
  private static final int STACK_KIND = 5 << KIND_SHIFT;

  // Possible flags for the FLAGS field of an abstract type.

  /**
   * 用于 LOCAL_KIND 和 STACK_KIND 抽象类型的标志，表示如果解析出的具体类型是 LONG 或 DOUBLE，则应改
   * 用 TOP（因为该值已被 xSTORE 指令部分覆盖）。
   */
  private static final int TOP_IF_LONG_OR_DOUBLE_FLAG = 1 << FLAGS_SHIFT;

  // Useful predefined abstract types (all the possible CONSTANT_KIND types).

  private static final int TOP = CONSTANT_KIND | ITEM_TOP;
  private static final int BOOLEAN = CONSTANT_KIND | ITEM_ASM_BOOLEAN;
  private static final int BYTE = CONSTANT_KIND | ITEM_ASM_BYTE;
  private static final int CHAR = CONSTANT_KIND | ITEM_ASM_CHAR;
  private static final int SHORT = CONSTANT_KIND | ITEM_ASM_SHORT;
  private static final int INTEGER = CONSTANT_KIND | ITEM_INTEGER;
  private static final int FLOAT = CONSTANT_KIND | ITEM_FLOAT;
  private static final int LONG = CONSTANT_KIND | ITEM_LONG;
  private static final int DOUBLE = CONSTANT_KIND | ITEM_DOUBLE;
  private static final int NULL = CONSTANT_KIND | ITEM_NULL;
  private static final int UNINITIALIZED_THIS = CONSTANT_KIND | ITEM_UNINITIALIZED_THIS;

  // -----------------------------------------------------------------------------------------------
  // Instance fields
  // -----------------------------------------------------------------------------------------------

  /** 这些输入和输出栈映射帧对应的基本块。 */
  Label owner;

  /** 输入栈映射帧的局部变量。这是一个抽象类型数组。 */
  private int[] inputLocals;

  /** 输入栈映射帧的栈。这是一个抽象类型数组。 */
  private int[] inputStack;

  /** 输出栈映射帧的局部变量。这是一个抽象类型数组。 */
  private int[] outputLocals;

  /** 输出栈映射帧的栈。这是一个抽象类型数组。 */
  private int[] outputStack;

  /**
   * 输出栈的起始位置，相对于输入栈。此偏移量始终为负或 null。null 偏移量表示输出栈必须追加到输入栈之
   * 后。-n 偏移量表示输出栈的前 n 个元素必须替换输入栈顶部的 n 个元素，而其余元素必须追加到输入栈之
   * 后。
   */
  private short outputStackStart;

  /** 栈顶元素在 {@link #outputStack} 中的索引。 */
  private short outputStackTop;

  /** 基本块中已初始化的类型数量。参见 {@link #initializations}。 */
  private int initializationCount;

  /**
   * 在基本块中被初始化的抽象类型。对 UNINITIALIZED 或 UNINITIALIZED_THIS 抽象类型调用构造函数时，必
   * 须替换局部变量和操作数栈中该类型的<i>每一次出现</i>。这无法在算法的第一步中完成，因为在这一步中
   * 局部变量和操作数栈的类型仍是抽象的。因此，必须存储在基本块中被调用的构造函数的抽象类型，以便在算
   * 法的第二步（此时帧已完全计算）中执行此替换。请注意，此数组可能包含相对于输入局部变量或输入栈的抽
   * 象类型。
   */
  private int[] initializations;

  // -----------------------------------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 Frame。
   *
   * @param owner the basic block to which these input and output stack map frames correspond.
   */
  Frame(final Label owner) {
    this.owner = owner;
  }

  /**
   * 将此帧设置为给定帧的值。
   *
   * <p>警告：调用此方法后，两个帧共享相同的数据结构。建议丢弃给定的帧以避免意外的副作用。
   *
   * @param frame The new frame value.
   */
  final void copyFrom(final Frame frame) {
    inputLocals = frame.inputLocals;
    inputStack = frame.inputStack;
    outputStackStart = 0;
    outputLocals = frame.outputLocals;
    outputStack = frame.outputStack;
    outputStackTop = frame.outputStackTop;
    initializationCount = frame.initializationCount;
    initializations = frame.initializations;
  }

  // -----------------------------------------------------------------------------------------------
  // Static methods to get abstract types from other type formats
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回与给定公共 API 帧元素类型相对应的抽象类型。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param type a frame element type described using the same format as in {@link
   *     MethodVisitor#visitFrame}, i.e. either {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link
   *     Opcodes#FLOAT}, {@link Opcodes#LONG}, {@link Opcodes#DOUBLE}, {@link Opcodes#NULL}, or
   *     {@link Opcodes#UNINITIALIZED_THIS}, or the internal name of a class, or a Label designating
   *     a NEW instruction (for uninitialized types).
   * @return the abstract type corresponding to the given frame element type.
   */
  static int getAbstractTypeFromApiFormat(final SymbolTable symbolTable, final Object type) {
    if (type instanceof Integer) {
      return CONSTANT_KIND | ((Integer) type).intValue();
    } else if (type instanceof String) {
      String descriptor = Type.getObjectType((String) type).getDescriptor();
      return getAbstractTypeFromDescriptor(symbolTable, descriptor, 0);
    } else {
      return UNINITIALIZED_KIND
          | symbolTable.addUninitializedType("", ((Label) type).bytecodeOffset);
    }
  }

  /**
   * 返回与类内部名称相对应的抽象类型。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param internalName the internal name of a class. This must <i>not</i> be an array type
   *     descriptor.
   * @return the abstract type value corresponding to the given internal name.
   */
  static int getAbstractTypeFromInternalName(
      final SymbolTable symbolTable, final String internalName) {
    return REFERENCE_KIND | symbolTable.addType(internalName);
  }

  /**
   * 返回与给定类型描述符相对应的抽象类型。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param buffer a string ending with a type descriptor.
   * @param offset the start offset of the type descriptor in buffer.
   * @return the abstract type corresponding to the given type descriptor.
   */
  private static int getAbstractTypeFromDescriptor(
      final SymbolTable symbolTable, final String buffer, final int offset) {
    String internalName;
    switch (buffer.charAt(offset)) {
      case 'V':
        return 0;
      case 'Z':
      case 'C':
      case 'B':
      case 'S':
      case 'I':
        return INTEGER;
      case 'F':
        return FLOAT;
      case 'J':
        return LONG;
      case 'D':
        return DOUBLE;
      case 'L':
        internalName = buffer.substring(offset + 1, buffer.length() - 1);
        return REFERENCE_KIND | symbolTable.addType(internalName);
      case '[':
        int elementDescriptorOffset = offset + 1;
        while (buffer.charAt(elementDescriptorOffset) == '[') {
          ++elementDescriptorOffset;
        }
        int typeValue;
        switch (buffer.charAt(elementDescriptorOffset)) {
          case 'Z':
            typeValue = BOOLEAN;
            break;
          case 'C':
            typeValue = CHAR;
            break;
          case 'B':
            typeValue = BYTE;
            break;
          case 'S':
            typeValue = SHORT;
            break;
          case 'I':
            typeValue = INTEGER;
            break;
          case 'F':
            typeValue = FLOAT;
            break;
          case 'J':
            typeValue = LONG;
            break;
          case 'D':
            typeValue = DOUBLE;
            break;
          case 'L':
            internalName = buffer.substring(elementDescriptorOffset + 1, buffer.length() - 1);
            typeValue = REFERENCE_KIND | symbolTable.addType(internalName);
            break;
          default:
            throw new IllegalArgumentException(
                "Invalid descriptor fragment: " + buffer.substring(elementDescriptorOffset));
        }
        return ((elementDescriptorOffset - offset) << DIM_SHIFT) | typeValue;
      default:
        throw new IllegalArgumentException("Invalid descriptor: " + buffer.substring(offset));
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Methods related to the input frame
  // -----------------------------------------------------------------------------------------------

  /**
   * 根据给定的方法描述设置输入帧。此方法用于初始化方法的第一个帧，该帧是隐式的（即不显式存储在
   * StackMapTable 属性中）。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param access the method's access flags.
   * @param descriptor the method descriptor.
   * @param maxLocals the maximum number of local variables of the method.
   */
  final void setInputFrameFromDescriptor(
      final SymbolTable symbolTable,
      final int access,
      final String descriptor,
      final int maxLocals) {
    inputLocals = new int[maxLocals];
    inputStack = new int[0];
    int inputLocalIndex = 0;
    if ((access & Opcodes.ACC_STATIC) == 0) {
      if ((access & Constants.ACC_CONSTRUCTOR) == 0) {
        inputLocals[inputLocalIndex++] =
            REFERENCE_KIND | symbolTable.addType(symbolTable.getClassName());
      } else {
        inputLocals[inputLocalIndex++] = UNINITIALIZED_THIS;
      }
    }
    for (Type argumentType : Type.getArgumentTypes(descriptor)) {
      int abstractType =
          getAbstractTypeFromDescriptor(symbolTable, argumentType.getDescriptor(), 0);
      inputLocals[inputLocalIndex++] = abstractType;
      if (abstractType == LONG || abstractType == DOUBLE) {
        inputLocals[inputLocalIndex++] = TOP;
      }
    }
    while (inputLocalIndex < maxLocals) {
      inputLocals[inputLocalIndex++] = TOP;
    }
  }

  /**
   * 根据给定的公共 API 帧描述设置输入帧。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param numLocal the number of local variables.
   * @param local the local variable types, described using the same format as in {@link
   *     MethodVisitor#visitFrame}.
   * @param numStack the number of operand stack elements.
   * @param stack the operand stack types, described using the same format as in {@link
   *     MethodVisitor#visitFrame}.
   */
  final void setInputFrameFromApiFormat(
      final SymbolTable symbolTable,
      final int numLocal,
      final Object[] local,
      final int numStack,
      final Object[] stack) {
    int inputLocalIndex = 0;
    for (int i = 0; i < numLocal; ++i) {
      inputLocals[inputLocalIndex++] = getAbstractTypeFromApiFormat(symbolTable, local[i]);
      if (local[i] == Opcodes.LONG || local[i] == Opcodes.DOUBLE) {
        inputLocals[inputLocalIndex++] = TOP;
      }
    }
    while (inputLocalIndex < inputLocals.length) {
      inputLocals[inputLocalIndex++] = TOP;
    }
    int numStackTop = 0;
    for (int i = 0; i < numStack; ++i) {
      if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
        ++numStackTop;
      }
    }
    inputStack = new int[numStack + numStackTop];
    int inputStackIndex = 0;
    for (int i = 0; i < numStack; ++i) {
      inputStack[inputStackIndex++] = getAbstractTypeFromApiFormat(symbolTable, stack[i]);
      if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
        inputStack[inputStackIndex++] = TOP;
      }
    }
    outputStackTop = 0;
    initializationCount = 0;
  }

  final int getInputStackSize() {
    return inputStack.length;
  }

  // -----------------------------------------------------------------------------------------------
  // Methods related to the output frame
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回输出帧中给定局部变量索引处存储的抽象类型。
   *
   * @param localIndex the index of the local variable whose value must be returned.
   * @return the abstract type stored at the given local variable index in the output frame.
   */
  private int getLocal(final int localIndex) {
    if (outputLocals == null || localIndex >= outputLocals.length) {
      // If this local has never been assigned in this basic block, it is still equal to its value
      // in the input frame.
      return LOCAL_KIND | localIndex;
    } else {
      int abstractType = outputLocals[localIndex];
      if (abstractType == 0) {
        // If this local has never been assigned in this basic block, so it is still equal to its
        // value in the input frame.
        abstractType = outputLocals[localIndex] = LOCAL_KIND | localIndex;
      }
      return abstractType;
    }
  }

  /**
   * 替换输出帧中给定局部变量索引处存储的抽象类型。
   *
   * @param localIndex the index of the output frame local variable that must be set.
   * @param abstractType the value that must be set.
   */
  private void setLocal(final int localIndex, final int abstractType) {
    // Create and/or resize the output local variables array if necessary.
    if (outputLocals == null) {
      outputLocals = new int[10];
    }
    int outputLocalsLength = outputLocals.length;
    if (localIndex >= outputLocalsLength) {
      int[] newOutputLocals = new int[Math.max(localIndex + 1, 2 * outputLocalsLength)];
      System.arraycopy(outputLocals, 0, newOutputLocals, 0, outputLocalsLength);
      outputLocals = newOutputLocals;
    }
    // Set the local variable.
    outputLocals[localIndex] = abstractType;
  }

  /**
   * 将给定的抽象类型压入输出帧栈。
   *
   * @param abstractType an abstract type.
   */
  private void push(final int abstractType) {
    // Create and/or resize the output stack array if necessary.
    if (outputStack == null) {
      outputStack = new int[10];
    }
    int outputStackLength = outputStack.length;
    if (outputStackTop >= outputStackLength) {
      int[] newOutputStack = new int[Math.max(outputStackTop + 1, 2 * outputStackLength)];
      System.arraycopy(outputStack, 0, newOutputStack, 0, outputStackLength);
      outputStack = newOutputStack;
    }
    // Pushes the abstract type on the output stack.
    outputStack[outputStackTop++] = abstractType;
    // Updates the maximum size reached by the output stack, if needed (note that this size is
    // relative to the input stack size, which is not known yet).
    short outputStackSize = (short) (outputStackStart + outputStackTop);
    if (outputStackSize > owner.outputStackMax) {
      owner.outputStackMax = outputStackSize;
    }
  }

  /**
   * 将与给定描述符相对应的抽象类型压入输出帧栈。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param descriptor a type or method descriptor (in which case its return type is pushed).
   */
  private void push(final SymbolTable symbolTable, final String descriptor) {
    int typeDescriptorOffset =
        descriptor.charAt(0) == '(' ? Type.getReturnTypeOffset(descriptor) : 0;
    int abstractType = getAbstractTypeFromDescriptor(symbolTable, descriptor, typeDescriptorOffset);
    if (abstractType != 0) {
      push(abstractType);
      if (abstractType == LONG || abstractType == DOUBLE) {
        push(TOP);
      }
    }
  }

  /**
   * 从输出帧栈中弹出一个抽象类型并返回其值。
   *
   * @return the abstract type that has been popped from the output frame stack.
   */
  private int pop() {
    if (outputStackTop > 0) {
      return outputStack[--outputStackTop];
    } else {
      // If the output frame stack is empty, pop from the input stack.
      return STACK_KIND | -(--outputStackStart);
    }
  }

  /**
   * 从输出帧栈中弹出给定数量的抽象类型。
   *
   * @param elements the number of abstract types that must be popped.
   */
  private void pop(final int elements) {
    if (outputStackTop >= elements) {
      outputStackTop -= elements;
    } else {
      // If the number of elements to be popped is greater than the number of elements in the output
      // stack, clear it, and pop the remaining elements from the input stack.
      outputStackStart -= elements - outputStackTop;
      outputStackTop = 0;
    }
  }

  /**
   * 从输出帧栈中弹出与给定描述符所描述的相同数量的抽象类型。
   *
   * @param descriptor a type or method descriptor (in which case its argument types are popped).
   */
  private void pop(final String descriptor) {
    char firstDescriptorChar = descriptor.charAt(0);
    if (firstDescriptorChar == '(') {
      pop((Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1);
    } else if (firstDescriptorChar == 'J' || firstDescriptorChar == 'D') {
      pop(2);
    } else {
      pop(1);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to handle uninitialized types
  // -----------------------------------------------------------------------------------------------

  /**
   * 将一个抽象类型添加到基本块中调用了构造函数的类型列表中。
   *
   * @param abstractType an abstract type on a which a constructor is invoked.
   */
  private void addInitializedType(final int abstractType) {
    // Create and/or resize the initializations array if necessary.
    if (initializations == null) {
      initializations = new int[2];
    }
    int initializationsLength = initializations.length;
    if (initializationCount >= initializationsLength) {
      int[] newInitializations =
          new int[Math.max(initializationCount + 1, 2 * initializationsLength)];
      System.arraycopy(initializations, 0, newInitializations, 0, initializationsLength);
      initializations = newInitializations;
    }
    // Store the abstract type.
    initializations[initializationCount++] = abstractType;
  }

  /**
   * 返回与给定抽象类型相对应的“已初始化”抽象类型。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param abstractType an abstract type.
   * @return the REFERENCE_KIND abstract type corresponding to abstractType if it is
   *     UNINITIALIZED_THIS or an UNINITIALIZED_KIND abstract type for one of the types on which a
   *     constructor is invoked in the basic block. Otherwise returns abstractType.
   */
  private int getInitializedType(final SymbolTable symbolTable, final int abstractType) {
    if (abstractType == UNINITIALIZED_THIS
        || (abstractType & (DIM_MASK | KIND_MASK)) == UNINITIALIZED_KIND) {
      for (int i = 0; i < initializationCount; ++i) {
        int initializedType = initializations[i];
        int dim = initializedType & DIM_MASK;
        int kind = initializedType & KIND_MASK;
        int value = initializedType & VALUE_MASK;
        if (kind == LOCAL_KIND) {
          initializedType = dim + inputLocals[value];
        } else if (kind == STACK_KIND) {
          initializedType = dim + inputStack[inputStack.length - value];
        }
        if (abstractType == initializedType) {
          if (abstractType == UNINITIALIZED_THIS) {
            return REFERENCE_KIND | symbolTable.addType(symbolTable.getClassName());
          } else {
            return REFERENCE_KIND
                | symbolTable.addType(symbolTable.getType(abstractType & VALUE_MASK).value);
          }
        }
      }
    }
    return abstractType;
  }

  // -----------------------------------------------------------------------------------------------
  // Main method, to simulate the execution of each instruction on the output frame
  // -----------------------------------------------------------------------------------------------

  /**
   * 在输出栈帧上模拟给定指令的动作。
   *
   * @param opcode the opcode of the instruction.
   * @param arg the numeric operand of the instruction, if any.
   * @param argSymbol the Symbol operand of the instruction, if any.
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   */
  void execute(
      final int opcode, final int arg, final Symbol argSymbol, final SymbolTable symbolTable) {
    // Abstract types popped from the stack or read from local variables.
    int abstractType1;
    int abstractType2;
    int abstractType3;
    int abstractType4;
    switch (opcode) {
      case Opcodes.NOP:
      case Opcodes.INEG:
      case Opcodes.LNEG:
      case Opcodes.FNEG:
      case Opcodes.DNEG:
      case Opcodes.I2B:
      case Opcodes.I2C:
      case Opcodes.I2S:
      case Opcodes.GOTO:
      case Opcodes.RETURN:
        break;
      case Opcodes.ACONST_NULL:
        push(NULL);
        break;
      case Opcodes.ICONST_M1:
      case Opcodes.ICONST_0:
      case Opcodes.ICONST_1:
      case Opcodes.ICONST_2:
      case Opcodes.ICONST_3:
      case Opcodes.ICONST_4:
      case Opcodes.ICONST_5:
      case Opcodes.BIPUSH:
      case Opcodes.SIPUSH:
      case Opcodes.ILOAD:
        push(INTEGER);
        break;
      case Opcodes.LCONST_0:
      case Opcodes.LCONST_1:
      case Opcodes.LLOAD:
        push(LONG);
        push(TOP);
        break;
      case Opcodes.FCONST_0:
      case Opcodes.FCONST_1:
      case Opcodes.FCONST_2:
      case Opcodes.FLOAD:
        push(FLOAT);
        break;
      case Opcodes.DCONST_0:
      case Opcodes.DCONST_1:
      case Opcodes.DLOAD:
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.LDC:
        switch (argSymbol.tag) {
          case Symbol.CONSTANT_INTEGER_TAG:
            push(INTEGER);
            break;
          case Symbol.CONSTANT_LONG_TAG:
            push(LONG);
            push(TOP);
            break;
          case Symbol.CONSTANT_FLOAT_TAG:
            push(FLOAT);
            break;
          case Symbol.CONSTANT_DOUBLE_TAG:
            push(DOUBLE);
            push(TOP);
            break;
          case Symbol.CONSTANT_CLASS_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/Class"));
            break;
          case Symbol.CONSTANT_STRING_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/String"));
            break;
          case Symbol.CONSTANT_METHOD_TYPE_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/invoke/MethodType"));
            break;
          case Symbol.CONSTANT_METHOD_HANDLE_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/invoke/MethodHandle"));
            break;
          case Symbol.CONSTANT_DYNAMIC_TAG:
            push(symbolTable, argSymbol.value);
            break;
          default:
            throw new AssertionError();
        }
        break;
      case Opcodes.ALOAD:
        push(getLocal(arg));
        break;
      case Opcodes.LALOAD:
      case Opcodes.D2L:
        pop(2);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.DALOAD:
      case Opcodes.L2D:
        pop(2);
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.AALOAD:
        pop(1);
        abstractType1 = pop();
        push(abstractType1 == NULL ? abstractType1 : ELEMENT_OF + abstractType1);
        break;
      case Opcodes.ISTORE:
      case Opcodes.FSTORE:
      case Opcodes.ASTORE:
        abstractType1 = pop();
        setLocal(arg, abstractType1);
        if (arg > 0) {
          int previousLocalType = getLocal(arg - 1);
          if (previousLocalType == LONG || previousLocalType == DOUBLE) {
            setLocal(arg - 1, TOP);
          } else if ((previousLocalType & KIND_MASK) == LOCAL_KIND
              || (previousLocalType & KIND_MASK) == STACK_KIND) {
            // The type of the previous local variable is not known yet, but if it later appears
            // to be LONG or DOUBLE, we should then use TOP instead.
            setLocal(arg - 1, previousLocalType | TOP_IF_LONG_OR_DOUBLE_FLAG);
          }
        }
        break;
      case Opcodes.LSTORE:
      case Opcodes.DSTORE:
        pop(1);
        abstractType1 = pop();
        setLocal(arg, abstractType1);
        setLocal(arg + 1, TOP);
        if (arg > 0) {
          int previousLocalType = getLocal(arg - 1);
          if (previousLocalType == LONG || previousLocalType == DOUBLE) {
            setLocal(arg - 1, TOP);
          } else if ((previousLocalType & KIND_MASK) == LOCAL_KIND
              || (previousLocalType & KIND_MASK) == STACK_KIND) {
            // The type of the previous local variable is not known yet, but if it later appears
            // to be LONG or DOUBLE, we should then use TOP instead.
            setLocal(arg - 1, previousLocalType | TOP_IF_LONG_OR_DOUBLE_FLAG);
          }
        }
        break;
      case Opcodes.IASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.SASTORE:
      case Opcodes.FASTORE:
      case Opcodes.AASTORE:
        pop(3);
        break;
      case Opcodes.LASTORE:
      case Opcodes.DASTORE:
        pop(4);
        break;
      case Opcodes.POP:
      case Opcodes.IFEQ:
      case Opcodes.IFNE:
      case Opcodes.IFLT:
      case Opcodes.IFGE:
      case Opcodes.IFGT:
      case Opcodes.IFLE:
      case Opcodes.IRETURN:
      case Opcodes.FRETURN:
      case Opcodes.ARETURN:
      case Opcodes.TABLESWITCH:
      case Opcodes.LOOKUPSWITCH:
      case Opcodes.ATHROW:
      case Opcodes.MONITORENTER:
      case Opcodes.MONITOREXIT:
      case Opcodes.IFNULL:
      case Opcodes.IFNONNULL:
        pop(1);
        break;
      case Opcodes.POP2:
      case Opcodes.IF_ICMPEQ:
      case Opcodes.IF_ICMPNE:
      case Opcodes.IF_ICMPLT:
      case Opcodes.IF_ICMPGE:
      case Opcodes.IF_ICMPGT:
      case Opcodes.IF_ICMPLE:
      case Opcodes.IF_ACMPEQ:
      case Opcodes.IF_ACMPNE:
      case Opcodes.LRETURN:
      case Opcodes.DRETURN:
        pop(2);
        break;
      case Opcodes.DUP:
        abstractType1 = pop();
        push(abstractType1);
        push(abstractType1);
        break;
      case Opcodes.DUP_X1:
        abstractType1 = pop();
        abstractType2 = pop();
        push(abstractType1);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP_X2:
        abstractType1 = pop();
        abstractType2 = pop();
        abstractType3 = pop();
        push(abstractType1);
        push(abstractType3);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP2:
        abstractType1 = pop();
        abstractType2 = pop();
        push(abstractType2);
        push(abstractType1);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP2_X1:
        abstractType1 = pop();
        abstractType2 = pop();
        abstractType3 = pop();
        push(abstractType2);
        push(abstractType1);
        push(abstractType3);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP2_X2:
        abstractType1 = pop();
        abstractType2 = pop();
        abstractType3 = pop();
        abstractType4 = pop();
        push(abstractType2);
        push(abstractType1);
        push(abstractType4);
        push(abstractType3);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.SWAP:
        abstractType1 = pop();
        abstractType2 = pop();
        push(abstractType1);
        push(abstractType2);
        break;
      case Opcodes.IALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.SALOAD:
      case Opcodes.IADD:
      case Opcodes.ISUB:
      case Opcodes.IMUL:
      case Opcodes.IDIV:
      case Opcodes.IREM:
      case Opcodes.IAND:
      case Opcodes.IOR:
      case Opcodes.IXOR:
      case Opcodes.ISHL:
      case Opcodes.ISHR:
      case Opcodes.IUSHR:
      case Opcodes.L2I:
      case Opcodes.D2I:
      case Opcodes.FCMPL:
      case Opcodes.FCMPG:
        pop(2);
        push(INTEGER);
        break;
      case Opcodes.LADD:
      case Opcodes.LSUB:
      case Opcodes.LMUL:
      case Opcodes.LDIV:
      case Opcodes.LREM:
      case Opcodes.LAND:
      case Opcodes.LOR:
      case Opcodes.LXOR:
        pop(4);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.FALOAD:
      case Opcodes.FADD:
      case Opcodes.FSUB:
      case Opcodes.FMUL:
      case Opcodes.FDIV:
      case Opcodes.FREM:
      case Opcodes.L2F:
      case Opcodes.D2F:
        pop(2);
        push(FLOAT);
        break;
      case Opcodes.DADD:
      case Opcodes.DSUB:
      case Opcodes.DMUL:
      case Opcodes.DDIV:
      case Opcodes.DREM:
        pop(4);
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.LSHL:
      case Opcodes.LSHR:
      case Opcodes.LUSHR:
        pop(3);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.IINC:
        setLocal(arg, INTEGER);
        break;
      case Opcodes.I2L:
      case Opcodes.F2L:
        pop(1);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.I2F:
        pop(1);
        push(FLOAT);
        break;
      case Opcodes.I2D:
      case Opcodes.F2D:
        pop(1);
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.F2I:
      case Opcodes.ARRAYLENGTH:
      case Opcodes.INSTANCEOF:
        pop(1);
        push(INTEGER);
        break;
      case Opcodes.LCMP:
      case Opcodes.DCMPL:
      case Opcodes.DCMPG:
        pop(4);
        push(INTEGER);
        break;
      case Opcodes.JSR:
      case Opcodes.RET:
        throw new IllegalArgumentException("JSR/RET are not supported with computeFrames option");
      case Opcodes.GETSTATIC:
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.PUTSTATIC:
        pop(argSymbol.value);
        break;
      case Opcodes.GETFIELD:
        pop(1);
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.PUTFIELD:
        pop(argSymbol.value);
        pop();
        break;
      case Opcodes.INVOKEVIRTUAL:
      case Opcodes.INVOKESPECIAL:
      case Opcodes.INVOKESTATIC:
      case Opcodes.INVOKEINTERFACE:
        pop(argSymbol.value);
        if (opcode != Opcodes.INVOKESTATIC) {
          abstractType1 = pop();
          if (opcode == Opcodes.INVOKESPECIAL && argSymbol.name.charAt(0) == '<') {
            addInitializedType(abstractType1);
          }
        }
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.INVOKEDYNAMIC:
        pop(argSymbol.value);
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.NEW:
        push(UNINITIALIZED_KIND | symbolTable.addUninitializedType(argSymbol.value, arg));
        break;
      case Opcodes.NEWARRAY:
        pop();
        switch (arg) {
          case Opcodes.T_BOOLEAN:
            push(ARRAY_OF | BOOLEAN);
            break;
          case Opcodes.T_CHAR:
            push(ARRAY_OF | CHAR);
            break;
          case Opcodes.T_BYTE:
            push(ARRAY_OF | BYTE);
            break;
          case Opcodes.T_SHORT:
            push(ARRAY_OF | SHORT);
            break;
          case Opcodes.T_INT:
            push(ARRAY_OF | INTEGER);
            break;
          case Opcodes.T_FLOAT:
            push(ARRAY_OF | FLOAT);
            break;
          case Opcodes.T_DOUBLE:
            push(ARRAY_OF | DOUBLE);
            break;
          case Opcodes.T_LONG:
            push(ARRAY_OF | LONG);
            break;
          default:
            throw new IllegalArgumentException();
        }
        break;
      case Opcodes.ANEWARRAY:
        String arrayElementType = argSymbol.value;
        pop();
        if (arrayElementType.charAt(0) == '[') {
          push(symbolTable, '[' + arrayElementType);
        } else {
          push(ARRAY_OF | REFERENCE_KIND | symbolTable.addType(arrayElementType));
        }
        break;
      case Opcodes.CHECKCAST:
        String castType = argSymbol.value;
        pop();
        if (castType.charAt(0) == '[') {
          push(symbolTable, castType);
        } else {
          push(REFERENCE_KIND | symbolTable.addType(castType));
        }
        break;
      case Opcodes.MULTIANEWARRAY:
        pop(arg);
        push(symbolTable, argSymbol.value);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Frame merging methods, used in the second step of the stack map frame computation algorithm
  // -----------------------------------------------------------------------------------------------

  /**
   * 计算与给定抽象输出类型相对应的具体输出类型。
   *
   * @param abstractOutputType an abstract output type.
   * @param numStack the size of the input stack, used to resolve abstract output types of
   *     STACK_KIND kind.
   * @return the concrete output type corresponding to 'abstractOutputType'.
   */
  private int getConcreteOutputType(final int abstractOutputType, final int numStack) {
    int dim = abstractOutputType & DIM_MASK;
    int kind = abstractOutputType & KIND_MASK;
    if (kind == LOCAL_KIND) {
      // By definition, a LOCAL_KIND type designates the concrete type of a local variable at
      // the beginning of the basic block corresponding to this frame (which is known when
      // this method is called, but was not when the abstract type was computed).
      int concreteOutputType = dim + inputLocals[abstractOutputType & VALUE_MASK];
      if ((abstractOutputType & TOP_IF_LONG_OR_DOUBLE_FLAG) != 0
          && (concreteOutputType == LONG || concreteOutputType == DOUBLE)) {
        concreteOutputType = TOP;
      }
      return concreteOutputType;
    } else if (kind == STACK_KIND) {
      // By definition, a STACK_KIND type designates the concrete type of a local variable at
      // the beginning of the basic block corresponding to this frame (which is known when
      // this method is called, but was not when the abstract type was computed).
      int concreteOutputType = dim + inputStack[numStack - (abstractOutputType & VALUE_MASK)];
      if ((abstractOutputType & TOP_IF_LONG_OR_DOUBLE_FLAG) != 0
          && (concreteOutputType == LONG || concreteOutputType == DOUBLE)) {
        concreteOutputType = TOP;
      }
      return concreteOutputType;
    } else {
      return abstractOutputType;
    }
  }

  /**
   * 将给定 {@link Frame} 的输入帧与此 {@link Frame} 的输入帧和输出帧合并。如果给定帧被此操作更改，则
   * 返回 {@literal true}（此 {@link Frame} 的输入帧和输出帧永远不会被更改）。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param dstFrame the {@link Frame} whose input frame must be updated. This should be the frame
   *     of a successor, in the control flow graph, of the basic block corresponding to this frame.
   * @param catchTypeIndex if 'frame' corresponds to an exception handler basic block, the type
   *     table index of the caught exception type, otherwise 0.
   * @return {@literal true} if the input frame of 'frame' has been changed by this operation.
   */
  final boolean merge(
      final SymbolTable symbolTable, final Frame dstFrame, final int catchTypeIndex) {
    boolean frameChanged = false;

    // Compute the concrete types of the local variables at the end of the basic block corresponding
    // to this frame, by resolving its abstract output types, and merge these concrete types with
    // those of the local variables in the input frame of dstFrame.
    int numLocal = inputLocals.length;
    int numStack = inputStack.length;
    if (dstFrame.inputLocals == null) {
      dstFrame.inputLocals = new int[numLocal];
      frameChanged = true;
    }
    for (int i = 0; i < numLocal; ++i) {
      int concreteOutputType;
      if (outputLocals != null && i < outputLocals.length) {
        int abstractOutputType = outputLocals[i];
        if (abstractOutputType == 0) {
          // If the local variable has never been assigned in this basic block, it is equal to its
          // value at the beginning of the block.
          concreteOutputType = inputLocals[i];
        } else {
          concreteOutputType = getConcreteOutputType(abstractOutputType, numStack);
        }
      } else {
        // If the local variable has never been assigned in this basic block, it is equal to its
        // value at the beginning of the block.
        concreteOutputType = inputLocals[i];
      }
      // concreteOutputType might be an uninitialized type from the input locals or from the input
      // stack. However, if a constructor has been called for this class type in the basic block,
      // then this type is no longer uninitialized at the end of basic block.
      if (initializations != null) {
        concreteOutputType = getInitializedType(symbolTable, concreteOutputType);
      }
      frameChanged |= merge(symbolTable, concreteOutputType, dstFrame.inputLocals, i);
    }

    // If dstFrame is an exception handler block, it can be reached from any instruction of the
    // basic block corresponding to this frame, in particular from the first one. Therefore, the
    // input locals of dstFrame should be compatible (i.e. merged) with the input locals of this
    // frame (and the input stack of dstFrame should be compatible, i.e. merged, with a one
    // element stack containing the caught exception type).
    if (catchTypeIndex > 0) {
      for (int i = 0; i < numLocal; ++i) {
        frameChanged |= merge(symbolTable, inputLocals[i], dstFrame.inputLocals, i);
      }
      if (dstFrame.inputStack == null) {
        dstFrame.inputStack = new int[1];
        frameChanged = true;
      }
      frameChanged |= merge(symbolTable, catchTypeIndex, dstFrame.inputStack, 0);
      return frameChanged;
    }

    // Compute the concrete types of the stack operands at the end of the basic block corresponding
    // to this frame, by resolving its abstract output types, and merge these concrete types with
    // those of the stack operands in the input frame of dstFrame.
    int numInputStack = inputStack.length + outputStackStart;
    if (dstFrame.inputStack == null) {
      dstFrame.inputStack = new int[numInputStack + outputStackTop];
      frameChanged = true;
    }
    // First, do this for the stack operands that have not been popped in the basic block
    // corresponding to this frame, and which are therefore equal to their value in the input
    // frame (except for uninitialized types, which may have been initialized).
    for (int i = 0; i < numInputStack; ++i) {
      int concreteOutputType = inputStack[i];
      if (initializations != null) {
        concreteOutputType = getInitializedType(symbolTable, concreteOutputType);
      }
      frameChanged |= merge(symbolTable, concreteOutputType, dstFrame.inputStack, i);
    }
    // Then, do this for the stack operands that have pushed in the basic block (this code is the
    // same as the one above for local variables).
    for (int i = 0; i < outputStackTop; ++i) {
      int abstractOutputType = outputStack[i];
      int concreteOutputType = getConcreteOutputType(abstractOutputType, numStack);
      if (initializations != null) {
        concreteOutputType = getInitializedType(symbolTable, concreteOutputType);
      }
      frameChanged |=
          merge(symbolTable, concreteOutputType, dstFrame.inputStack, numInputStack + i);
    }
    return frameChanged;
  }

  /**
   * 将给定抽象类型数组中给定索引处的类型与给定类型合并。如果该类型数组被此操作修改，则返回
   * {@literal true}。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param sourceType the abstract type with which the abstract type array element must be merged.
   *     This type should be of {@link #CONSTANT_KIND}, {@link #REFERENCE_KIND} or {@link
   *     #UNINITIALIZED_KIND} kind, with positive or {@literal null} array dimensions.
   * @param dstTypes an array of abstract types. These types should be of {@link #CONSTANT_KIND},
   *     {@link #REFERENCE_KIND} or {@link #UNINITIALIZED_KIND} kind, with positive or {@literal
   *     null} array dimensions.
   * @param dstIndex the index of the type that must be merged in dstTypes.
   * @return {@literal true} if the type array has been modified by this operation.
   */
  private static boolean merge(
      final SymbolTable symbolTable,
      final int sourceType,
      final int[] dstTypes,
      final int dstIndex) {
    int dstType = dstTypes[dstIndex];
    if (dstType == sourceType) {
      // If the types are equal, merge(sourceType, dstType) = dstType, so there is no change.
      return false;
    }
    int srcType = sourceType;
    if ((sourceType & ~DIM_MASK) == NULL) {
      if (dstType == NULL) {
        return false;
      }
      srcType = NULL;
    }
    if (dstType == 0) {
      // If dstTypes[dstIndex] has never been assigned, merge(srcType, dstType) = srcType.
      dstTypes[dstIndex] = srcType;
      return true;
    }
    int mergedType;
    if ((dstType & DIM_MASK) != 0 || (dstType & KIND_MASK) == REFERENCE_KIND) {
      // If dstType is a reference type of any array dimension.
      if (srcType == NULL) {
        // If srcType is the NULL type, merge(srcType, dstType) = dstType, so there is no change.
        return false;
      } else if ((srcType & (DIM_MASK | KIND_MASK)) == (dstType & (DIM_MASK | KIND_MASK))) {
        // If srcType has the same array dimension and the same kind as dstType.
        if ((dstType & KIND_MASK) == REFERENCE_KIND) {
          // If srcType and dstType are reference types with the same array dimension,
          // merge(srcType, dstType) = dim(srcType) | common super class of srcType and dstType.
          mergedType =
              (srcType & DIM_MASK)
                  | REFERENCE_KIND
                  | symbolTable.addMergedType(srcType & VALUE_MASK, dstType & VALUE_MASK);
        } else {
          // If srcType and dstType are array types of equal dimension but different element types,
          // merge(srcType, dstType) = dim(srcType) - 1 | java/lang/Object.
          int mergedDim = ELEMENT_OF + (srcType & DIM_MASK);
          mergedType = mergedDim | REFERENCE_KIND | symbolTable.addType("java/lang/Object");
        }
      } else if ((srcType & DIM_MASK) != 0 || (srcType & KIND_MASK) == REFERENCE_KIND) {
        // If srcType is any other reference or array type,
        // merge(srcType, dstType) = min(srcDdim, dstDim) | java/lang/Object
        // where srcDim is the array dimension of srcType, minus 1 if srcType is an array type
        // with a non reference element type (and similarly for dstDim).
        int srcDim = srcType & DIM_MASK;
        if (srcDim != 0 && (srcType & KIND_MASK) != REFERENCE_KIND) {
          srcDim = ELEMENT_OF + srcDim;
        }
        int dstDim = dstType & DIM_MASK;
        if (dstDim != 0 && (dstType & KIND_MASK) != REFERENCE_KIND) {
          dstDim = ELEMENT_OF + dstDim;
        }
        mergedType =
            Math.min(srcDim, dstDim) | REFERENCE_KIND | symbolTable.addType("java/lang/Object");
      } else {
        // If srcType is any other type, merge(srcType, dstType) = TOP.
        mergedType = TOP;
      }
    } else if (dstType == NULL) {
      // If dstType is the NULL type, merge(srcType, dstType) = srcType, or TOP if srcType is not a
      // an array type or a reference type.
      mergedType =
          (srcType & DIM_MASK) != 0 || (srcType & KIND_MASK) == REFERENCE_KIND ? srcType : TOP;
    } else {
      // If dstType is any other type, merge(srcType, dstType) = TOP whatever srcType.
      mergedType = TOP;
    }
    if (mergedType != dstType) {
      dstTypes[dstIndex] = mergedType;
      return true;
    }
    return false;
  }

  // -----------------------------------------------------------------------------------------------
  // Frame output methods, to generate StackMapFrame attributes
  // -----------------------------------------------------------------------------------------------

  /**
   * 让给定的 {@link MethodWriter} 访问此 {@link Frame} 的输入帧。访问通过
   * {@link MethodWriter#visitFrameStart}、{@link MethodWriter#visitAbstractType} 和
   * {@link MethodWriter#visitFrameEnd} 方法完成。
   *
   * @param methodWriter the {@link MethodWriter} that should visit the input frame of this {@link
   *     Frame}.
   */
  final void accept(final MethodWriter methodWriter) {
    // Compute the number of locals, ignoring TOP types that are just after a LONG or a DOUBLE, and
    // all trailing TOP types.
    int[] localTypes = inputLocals;
    int numLocal = 0;
    int numTrailingTop = 0;
    int i = 0;
    while (i < localTypes.length) {
      int localType = localTypes[i];
      i += (localType == LONG || localType == DOUBLE) ? 2 : 1;
      if (localType == TOP) {
        numTrailingTop++;
      } else {
        numLocal += numTrailingTop + 1;
        numTrailingTop = 0;
      }
    }
    // Compute the stack size, ignoring TOP types that are just after a LONG or a DOUBLE.
    int[] stackTypes = inputStack;
    int numStack = 0;
    i = 0;
    while (i < stackTypes.length) {
      int stackType = stackTypes[i];
      i += (stackType == LONG || stackType == DOUBLE) ? 2 : 1;
      numStack++;
    }
    // Visit the frame and its content.
    int frameIndex = methodWriter.visitFrameStart(owner.bytecodeOffset, numLocal, numStack);
    i = 0;
    while (numLocal-- > 0) {
      int localType = localTypes[i];
      i += (localType == LONG || localType == DOUBLE) ? 2 : 1;
      methodWriter.visitAbstractType(frameIndex++, localType);
    }
    i = 0;
    while (numStack-- > 0) {
      int stackType = stackTypes[i];
      i += (stackType == LONG || stackType == DOUBLE) ? 2 : 1;
      methodWriter.visitAbstractType(frameIndex++, stackType);
    }
    methodWriter.visitFrameEnd();
  }

  /**
   * 使用 StackMapTable 属性中所用的 JVMS verification_type_info 格式，将给定的抽象类型放入给定的
   * ByteVector 中。
   *
   * @param symbolTable the type table to use to lookup and store type {@link Symbol}.
   * @param abstractType an abstract type, restricted to {@link Frame#CONSTANT_KIND}, {@link
   *     Frame#REFERENCE_KIND} or {@link Frame#UNINITIALIZED_KIND} types.
   * @param output where the abstract type must be put.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4">JVMS
   *     4.7.4</a>
   */
  static void putAbstractType(
      final SymbolTable symbolTable, final int abstractType, final ByteVector output) {
    int arrayDimensions = (abstractType & Frame.DIM_MASK) >> DIM_SHIFT;
    if (arrayDimensions == 0) {
      int typeValue = abstractType & VALUE_MASK;
      switch (abstractType & KIND_MASK) {
        case CONSTANT_KIND:
          output.putByte(typeValue);
          break;
        case REFERENCE_KIND:
          output
              .putByte(ITEM_OBJECT)
              .putShort(symbolTable.addConstantClass(symbolTable.getType(typeValue).value).index);
          break;
        case UNINITIALIZED_KIND:
          output.putByte(ITEM_UNINITIALIZED).putShort((int) symbolTable.getType(typeValue).data);
          break;
        default:
          throw new AssertionError();
      }
    } else {
      // Case of an array type, we need to build its descriptor first.
      StringBuilder typeDescriptor = new StringBuilder(32);  // SPRING PATCH: larger initial size
      while (arrayDimensions-- > 0) {
        typeDescriptor.append('[');
      }
      if ((abstractType & KIND_MASK) == REFERENCE_KIND) {
        typeDescriptor
            .append('L')
            .append(symbolTable.getType(abstractType & VALUE_MASK).value)
            .append(';');
      } else {
        switch (abstractType & VALUE_MASK) {
          case Frame.ITEM_ASM_BOOLEAN:
            typeDescriptor.append('Z');
            break;
          case Frame.ITEM_ASM_BYTE:
            typeDescriptor.append('B');
            break;
          case Frame.ITEM_ASM_CHAR:
            typeDescriptor.append('C');
            break;
          case Frame.ITEM_ASM_SHORT:
            typeDescriptor.append('S');
            break;
          case Frame.ITEM_INTEGER:
            typeDescriptor.append('I');
            break;
          case Frame.ITEM_FLOAT:
            typeDescriptor.append('F');
            break;
          case Frame.ITEM_LONG:
            typeDescriptor.append('J');
            break;
          case Frame.ITEM_DOUBLE:
            typeDescriptor.append('D');
            break;
          default:
            throw new AssertionError();
        }
      }
      output
          .putByte(ITEM_OBJECT)
          .putShort(symbolTable.addConstantClass(typeDescriptor.toString()).index);
    }
  }
}
