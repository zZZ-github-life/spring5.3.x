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
 * 方法字节码中的一个位置。标签（label）用于跳转、goto 和 switch 指令，以及 try catch 块。标签所指
 * 定的是紧随其后的那条 <i>指令</i>。但请注意，标签与它所指定的指令之间可能存在其他元素（例如其他标
 * 签、栈映射帧、行号等）。
 *
 * @author Eric Bruneton
 */
public class Label {

  /**
   * 一个标志，表示某个标签仅用于调试属性。这样的标签不是基本块的开头，也不是跳转指令的目标或异常处理
   * 器。在控制流图分析算法中可以安全地忽略它（用于优化目的）。
   */
  static final int FLAG_DEBUG_ONLY = 1;

  /**
   * 一个标志，表示某个标签是跳转指令的目标，或者是异常处理器的开头。
   */
  static final int FLAG_JUMP_TARGET = 2;

  /** 表示标签的字节码偏移量已知的标志。 */
  static final int FLAG_RESOLVED = 4;

  /** 表示标签对应于可到达基本块的标志。 */
  static final int FLAG_REACHABLE = 8;

  /**
   * 一个标志，表示与某个标签对应的基本块以子程序调用结束。按照 {@link MethodWriter#visitJumpInsn} 中
   * 的构造方式，设置了此标志的标签至少有两个出边：
   *
   * <ul>
   * <li>第一个出边对应于字节码中 jsr 指令之后的那条指令，即从 jsr 调用返回后继续执行的位置。这是一条虚拟控制流边，因为执行流从不会直接从 jsr 跳到下一条指令，而是转到子程序并最终返回到 jsr 之后的指令。此虚拟边用于在 {@link #addSubroutineRetSuccessors} 中计算以 ret 指令结尾的基本块的真实出边。
   * <li>第二个出边对应于 jsr 指令的目标，
   * </ul>
   */
  static final int FLAG_SUBROUTINE_CALLER = 16;

  /**
   * 一个标志，表示与某个标签对应的基本块是子程序的开头。
   */
  static final int FLAG_SUBROUTINE_START = 32;

  /** 表示标签对应的基本块是子程序结尾的标志。 */
  static final int FLAG_SUBROUTINE_END = 64;

  /**
   * 当 {@link #otherLineNumbers} 数组需要调整大小以存储新的源代码行号时，要添加到该数组的元素个数。
   */
  static final int LINE_NUMBERS_CAPACITY_INCREMENT = 4;

  /**
   * 当 {@link #forwardReferences} 数组需要调整大小以存储新的前向引用时，要添加到该数组的元素个数。
   */
  static final int FORWARD_REFERENCES_CAPACITY_INCREMENT = 6;

  /**
   * 用于提取指向此标签的前向引用类型的位掩码。提取出的类型要么是
   * {@link #FORWARD_REFERENCE_TYPE_SHORT}，要么是 {@link #FORWARD_REFERENCE_TYPE_WIDE}。
   *
   * @see #forwardReferences
   */
  static final int FORWARD_REFERENCE_TYPE_MASK = 0xF0000000;

  /**
   * 在字节码中用两个字节存储的前向引用的类型。例如，来自 ifnull 指令的前向引用就属于这种情况。
   */
  static final int FORWARD_REFERENCE_TYPE_SHORT = 0x10000000;

  /**
   * 在字节码中用四个字节存储的前向引用的类型。例如，来自 lookupswitch 指令的前向引用就属于这种情况。
   */
  static final int FORWARD_REFERENCE_TYPE_WIDE = 0x20000000;

  /**
   * 用于提取指向此标签的前向引用“句柄”的位掩码。提取出的句柄是存储前向引用值的字节码偏移量（使用 2
   * 个或 4 个字节，具体由 {@link #FORWARD_REFERENCE_TYPE_MASK} 指示）。
   *
   * @see #forwardReferences
   */
  static final int FORWARD_REFERENCE_HANDLE_MASK = 0x0FFFFFFF;

  /**
   * 用作哨兵元素，指示标签列表的末尾。
   *
   * @see #nextListElement
   */
  static final Label EMPTY_LIST = new Label();

  /**
   * 与此标签关联的用户管理状态。警告：该字段由 ASM tree 包使用。要在 ASM tree 包中使用它，必须在
   * MethodNode 中重写 getLabelNode 方法。
   */
  public Object info;

  /**
   * 此标签或其对应基本块的类型和状态。必须是以下值的零个或多个组合：{@link #FLAG_DEBUG_ONLY}、
   * {@link #FLAG_JUMP_TARGET}、{@link #FLAG_RESOLVED}、{@link #FLAG_REACHABLE}、
   * {@link #FLAG_SUBROUTINE_CALLER}、{@link #FLAG_SUBROUTINE_START}、{@link #FLAG_SUBROUTINE_END}。
   */
  short flags;

  /**
   * 与此标签对应的源代码行号，或者 0。如果有多个源代码行号对应于此标签，第一个存储在该字段中，其余的
   * 存储在 {@link #otherLineNumbers} 中。
   */
  private short lineNumber;

  /**
   * 与此标签对应的源代码行号（{@link #lineNumber} 之外的），或者为 null。该数组的第一个元素是它包含
   * 的源代码行号数量 n，这些行号存储在索引 1 到 n（含）之间。
   */
  private int[] otherLineNumbers;

  /**
   * 此标签在其方法的字节码中的偏移量，以字节为单位。当且仅当设置了 {@link #FLAG_RESOLVED} 标志时才设
   * 置该值。
   */
  int bytecodeOffset;

  /**
   * 指向此标签的前向引用。第一个元素是前向引用的数量乘以 2（这对应于该数组中实际使用的最后一个元素的
   * 索引）。之后，每个前向引用用两个连续的整数来描述，分别记为 'sourceInsnBytecodeOffset' 和
   * 'reference'：
   *
   * <ul>
   * <li>'sourceInsnBytecodeOffset' 是包含该前向引用的指令的字节码偏移量，
   * <li>'reference' 包含类型以及字节码中必须存储前向引用值的偏移量，可以通过 {@link #FORWARD_REFERENCE_TYPE_MASK} 和 {@link #FORWARD_REFERENCE_HANDLE_MASK} 提取。
   * </ul>
   *
   * <p>例如，对于字节码偏移量为 x 的 ifnull 指令，'sourceInsnBytecodeOffset' 等于 x，而 'reference'
   * 的类型为 {@link #FORWARD_REFERENCE_TYPE_SHORT}，值为 x + 1（因为 ifnull 指令使用一个 2 字节的字
   * 节码偏移量操作数，它存储在指令本身开始之后一个字节处）。对于字节码偏移量为 x 的 lookupswitch 指
   * 令的 default 分支，'sourceInsnBytecodeOffset' 等于 x，而 'reference' 的类型为
   * {@link #FORWARD_REFERENCE_TYPE_WIDE}，值介于 x + 1 和 x + 4 之间（因为 lookupswitch 指令使用一个
   * 4 字节的字节码偏移量操作数，它存储在指令本身开始之后一到四个字节处）。
   */
  private int[] forwardReferences;

  // -----------------------------------------------------------------------------------------------

  // Fields for the control flow and data flow graph analysis algorithms (used to compute the
  // maximum stack size or the stack map frames). A control flow graph contains one node per "basic
  // block", and one edge per "jump" from one basic block to another. Each node (i.e., each basic
  // block) is represented with the Label object that corresponds to the first instruction of this
  // basic block. Each node also stores the list of its successors in the graph, as a linked list of
  // Edge objects.
  //
  // The control flow analysis algorithms used to compute the maximum stack size or the stack map
  // frames are similar and use two steps. The first step, during the visit of each instruction,
  // builds information about the state of the local variables and the operand stack at the end of
  // each basic block, called the "output frame", <i>relatively</i> to the frame state at the
  // beginning of the basic block, which is called the "input frame", and which is <i>unknown</i>
  // during this step. The second step, in {@link MethodWriter#computeAllFrames} and {@link
  // MethodWriter#computeMaxStackAndLocal}, is a fix point algorithm
  // that computes information about the input frame of each basic block, from the input state of
  // the first basic block (known from the method signature), and by the using the previously
  // computed relative output frames.
  //
  // The algorithm used to compute the maximum stack size only computes the relative output and
  // absolute input stack heights, while the algorithm used to compute stack map frames computes
  // relative output frames and absolute input frames.

  /**
   * 与此标签对应的基本块的输入栈中的元素个数。该字段在 {@link MethodWriter#computeMaxStackAndLocal}
   * 中计算。
   */
  short inputStackSize;

  /**
   * 与此标签对应的基本块末尾处输出栈中的元素个数。该字段仅对以 RET 指令结束的基本块进行计算。
   */
  short outputStackSize;

  /**
   * 与此标签对应的基本块中，输出栈相对于输入栈顶部所达到的最大高度。该最大值始终为正数或
   * {@literal null}。
   */
  short outputStackMax;

  /**
   * 此基本块所属子程序的 id，或者 0。如果该基本块属于多个子程序，则这里是包含它的“最外层”子程序的 id
   * （约定是：调用另一个子程序的子程序比被调用者“更外层”）。如果方法包含 JSR 指令，该字段在
   * {@link MethodWriter#computeMaxStackAndLocal} 中计算。
   */
  short subroutineId;

  /**
   * 与此标签对应的基本块的输入和输出栈映射帧。仅当使用 {@link MethodWriter#COMPUTE_ALL_FRAMES} 或
   * {@link MethodWriter#COMPUTE_INSERTED_FRAMES} 选项时才使用该字段。
   */
  Frame frame;

  /**
   * 此标签的后继标签，顺序与其在 {@link MethodVisitor#visitLabel} 中被访问的顺序一致。该链表不包含仅
   * 用于调试信息的标签。如果使用 {@link MethodWriter#COMPUTE_ALL_FRAMES} 或
   * {@link MethodWriter#COMPUTE_INSERTED_FRAMES} 选项，那么它也不包含表示相同字节码偏移量的连续标签
   * （在这种情况下，只有第一个标签会出现在该列表中）。
   */
  Label nextBasicBlock;

  /**
   * 与此标签对应的基本块在其方法的控制流图中的出边。这些边存储在 {@link Edge} 对象的链表中，各对象通
   * 过其 {@link Edge#nextEdge} 字段相互链接。
   */
  Edge outgoingEdges;

  /**
   * 此标签所属标签列表中的下一个元素；如果它不属于任何列表，则为 {@literal null}。所有标签列表都必须
   * 以 {@link #EMPTY_LIST} 哨兵结尾，以确保当且仅当此标签不属于任何标签列表时该字段为 null。注意，同
   * 一时刻可以存在多个标签列表，但一个标签同一时刻最多只能属于一个列表（除非某些列表共享公共尾部，但
   * 实际中并不这样做）。
   *
   * <p>标签列表用于在 {@link MethodWriter#computeAllFrames} 和
   * {@link MethodWriter#computeMaxStackAndLocal} 中分别计算栈映射帧和最大栈大小，也用于在
   * {@link #markSubroutine} 和 {@link #addSubroutineRetSuccessors} 中计算属于子程序的基本块及其出边
   * 。在这些方法之外，该字段应为 null（此属性是这些方法的前置条件和后置条件）。
   */
  Label nextListElement;

  // -----------------------------------------------------------------------------------------------
  // Constructor and accessors
  // -----------------------------------------------------------------------------------------------

  /** 构造一个新标签。 */
  public Label() {
    // Nothing to do.
  }

  /**
   * 返回与此标签对应的字节码偏移量。该偏移量从方法字节码的起始位置算起。<i>此方法面向
   * {@link Attribute} 的子类，类生成器或适配器通常不需要使用它。</i>
   *
   * @return the bytecode offset corresponding to this label.
   * @throws IllegalStateException if this label is not resolved yet.
   */
  public int getOffset() {
    if ((flags & FLAG_RESOLVED) == 0) {
      throw new IllegalStateException("Label offset position has not been resolved yet");
    }
    return bytecodeOffset;
  }

  /**
   * 返回与此标签的字节码偏移量对应的“规范”{@link Label} 实例（如果已知），否则返回标签本身。规范实例
   * 是（按 {@link MethodVisitor#visitLabel} 的访问顺序）对应于此字节码偏移量的第一个标签。对于尚未被
   * 访问的标签，无法得知该实例。
   *
   * <p><i>仅当使用 {@link MethodWriter#COMPUTE_ALL_FRAMES} 选项时才应使用此方法。</i>
   *
   * @return the label itself if {@link #frame} is null, otherwise the Label's frame owner. This
   *     corresponds to the "canonical" label instance described above thanks to the way the label
   *     frame is set in {@link MethodWriter#visitLabel}.
   */
  final Label getCanonicalInstance() {
    return frame == null ? this : frame.owner;
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to manage line numbers
  // -----------------------------------------------------------------------------------------------

  /**
   * 添加与此标签对应的源代码行号。
   *
   * @param lineNumber a source line number (which should be strictly positive).
   */
  final void addLineNumber(final int lineNumber) {
    if (this.lineNumber == 0) {
      this.lineNumber = (short) lineNumber;
    } else {
      if (otherLineNumbers == null) {
        otherLineNumbers = new int[LINE_NUMBERS_CAPACITY_INCREMENT];
      }
      int otherLineNumberIndex = ++otherLineNumbers[0];
      if (otherLineNumberIndex >= otherLineNumbers.length) {
        int[] newLineNumbers = new int[otherLineNumbers.length + LINE_NUMBERS_CAPACITY_INCREMENT];
        System.arraycopy(otherLineNumbers, 0, newLineNumbers, 0, otherLineNumbers.length);
        otherLineNumbers = newLineNumbers;
      }
      otherLineNumbers[otherLineNumberIndex] = lineNumber;
    }
  }

  /**
   * 让给定的访问者访问此标签及其源代码行号（如果适用）。
   *
   * @param methodVisitor a method visitor.
   * @param visitLineNumbers whether to visit of the label's source line numbers, if any.
   */
  final void accept(final MethodVisitor methodVisitor, final boolean visitLineNumbers) {
    methodVisitor.visitLabel(this);
    if (visitLineNumbers && lineNumber != 0) {
      methodVisitor.visitLineNumber(lineNumber & 0xFFFF, this);
      if (otherLineNumbers != null) {
        for (int i = 1; i <= otherLineNumbers[0]; ++i) {
          methodVisitor.visitLineNumber(otherLineNumbers[i], this);
        }
      }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Methods to compute offsets and to manage forward references
  // -----------------------------------------------------------------------------------------------

  /**
   * 在方法的字节码中放入对此标签的引用。如果已知该标签的字节码偏移量，则计算标签与引用它的指令之间的
   * 相对字节码偏移量并直接写入。否则，写入一个 null 相对偏移量，并为该标签声明一个新的前向引用。
   *
   * @param code the bytecode of the method. This is where the reference is appended.
   * @param sourceInsnBytecodeOffset the bytecode offset of the instruction that contains the
   *     reference to be appended.
   * @param wideReference whether the reference must be stored in 4 bytes (instead of 2 bytes).
   */
  final void put(
      final ByteVector code, final int sourceInsnBytecodeOffset, final boolean wideReference) {
    if ((flags & FLAG_RESOLVED) == 0) {
      if (wideReference) {
        addForwardReference(sourceInsnBytecodeOffset, FORWARD_REFERENCE_TYPE_WIDE, code.length);
        code.putInt(-1);
      } else {
        addForwardReference(sourceInsnBytecodeOffset, FORWARD_REFERENCE_TYPE_SHORT, code.length);
        code.putShort(-1);
      }
    } else {
      if (wideReference) {
        code.putInt(bytecodeOffset - sourceInsnBytecodeOffset);
      } else {
        code.putShort(bytecodeOffset - sourceInsnBytecodeOffset);
      }
    }
  }

  /**
   * 添加指向此标签的前向引用。仅当是真正的前向引用时（即仅当此标签尚未解析时）才能调用此方法。对于后
   * 向引用，可以并且必须直接计算并存储该引用的相对字节码偏移量。
   *
   * @param sourceInsnBytecodeOffset the bytecode offset of the instruction that contains the
   *     reference stored at referenceHandle.
   * @param referenceType either {@link #FORWARD_REFERENCE_TYPE_SHORT} or {@link
   *     #FORWARD_REFERENCE_TYPE_WIDE}.
   * @param referenceHandle the offset in the bytecode where the forward reference value must be
   *     stored.
   */
  private void addForwardReference(
      final int sourceInsnBytecodeOffset, final int referenceType, final int referenceHandle) {
    if (forwardReferences == null) {
      forwardReferences = new int[FORWARD_REFERENCES_CAPACITY_INCREMENT];
    }
    int lastElementIndex = forwardReferences[0];
    if (lastElementIndex + 2 >= forwardReferences.length) {
      int[] newValues = new int[forwardReferences.length + FORWARD_REFERENCES_CAPACITY_INCREMENT];
      System.arraycopy(forwardReferences, 0, newValues, 0, forwardReferences.length);
      forwardReferences = newValues;
    }
    forwardReferences[++lastElementIndex] = sourceInsnBytecodeOffset;
    forwardReferences[++lastElementIndex] = referenceType | referenceHandle;
    forwardReferences[0] = lastElementIndex;
  }

  /**
   * 将此标签的字节码偏移量设置为给定值，并解析指向此标签的前向引用（如果有）。当此标签被添加到方法的
   * 字节码中（即其字节码偏移量变为已知）时必须调用此方法。此方法会填补先前添加到该标签的每个前向引用
   * 在字节码中留下的空白。
   *
   * @param code the bytecode of the method.
   * @param bytecodeOffset the bytecode offset of this label.
   * @return {@literal true} if a blank that was left for this label was too small to store the
   *     offset. In such a case the corresponding jump instruction is replaced with an equivalent
   *     ASM specific instruction using an unsigned two bytes offset. These ASM specific
   *     instructions are later replaced with standard bytecode instructions with wider offsets (4
   *     bytes instead of 2), in ClassReader.
   */
  final boolean resolve(final byte[] code, final int bytecodeOffset) {
    this.flags |= FLAG_RESOLVED;
    this.bytecodeOffset = bytecodeOffset;
    if (forwardReferences == null) {
      return false;
    }
    boolean hasAsmInstructions = false;
    for (int i = forwardReferences[0]; i > 0; i -= 2) {
      final int sourceInsnBytecodeOffset = forwardReferences[i - 1];
      final int reference = forwardReferences[i];
      final int relativeOffset = bytecodeOffset - sourceInsnBytecodeOffset;
      int handle = reference & FORWARD_REFERENCE_HANDLE_MASK;
      if ((reference & FORWARD_REFERENCE_TYPE_MASK) == FORWARD_REFERENCE_TYPE_SHORT) {
        if (relativeOffset < Short.MIN_VALUE || relativeOffset > Short.MAX_VALUE) {
          // Change the opcode of the jump instruction, in order to be able to find it later in
          // ClassReader. These ASM specific opcodes are similar to jump instruction opcodes, except
          // that the 2 bytes offset is unsigned (and can therefore represent values from 0 to
          // 65535, which is sufficient since the size of a method is limited to 65535 bytes).
          int opcode = code[sourceInsnBytecodeOffset] & 0xFF;
          if (opcode < Opcodes.IFNULL) {
            // Change IFEQ ... JSR to ASM_IFEQ ... ASM_JSR.
            code[sourceInsnBytecodeOffset] = (byte) (opcode + Constants.ASM_OPCODE_DELTA);
          } else {
            // Change IFNULL and IFNONNULL to ASM_IFNULL and ASM_IFNONNULL.
            code[sourceInsnBytecodeOffset] = (byte) (opcode + Constants.ASM_IFNULL_OPCODE_DELTA);
          }
          hasAsmInstructions = true;
        }
        code[handle++] = (byte) (relativeOffset >>> 8);
        code[handle] = (byte) relativeOffset;
      } else {
        code[handle++] = (byte) (relativeOffset >>> 24);
        code[handle++] = (byte) (relativeOffset >>> 16);
        code[handle++] = (byte) (relativeOffset >>> 8);
        code[handle] = (byte) relativeOffset;
      }
    }
    return hasAsmInstructions;
  }

  // -----------------------------------------------------------------------------------------------
  // Methods related to subroutines
  // -----------------------------------------------------------------------------------------------

  /**
   * 查找属于以与此标签对应的基本块开头的子程序的基本块，并将这些块标记为属于该子程序。此方法沿着控制
   * 流图查找所有从当前基本块可达的块，且不跟随任何 jsr 目标。
   *
   * <p>注意：此方法的前置条件和后置条件是所有标签的 {@link #nextListElement} 都必须为 null。
   *
   * @param subroutineId the id of the subroutine starting with the basic block corresponding to
   *     this label.
   */
  final void markSubroutine(final short subroutineId) {
    // Data flow algorithm: put this basic block in a list of blocks to process (which are blocks
    // belonging to subroutine subroutineId) and, while there are blocks to process, remove one from
    // the list, mark it as belonging to the subroutine, and add its successor basic blocks in the
    // control flow graph to the list of blocks to process (if not already done).
    Label listOfBlocksToProcess = this;
    listOfBlocksToProcess.nextListElement = EMPTY_LIST;
    while (listOfBlocksToProcess != EMPTY_LIST) {
      // Remove a basic block from the list of blocks to process.
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
      basicBlock.nextListElement = null;

      // If it is not already marked as belonging to a subroutine, mark it as belonging to
      // subroutineId and add its successors to the list of blocks to process (unless already done).
      if (basicBlock.subroutineId == 0) {
        basicBlock.subroutineId = subroutineId;
        listOfBlocksToProcess = basicBlock.pushSuccessors(listOfBlocksToProcess);
      }
    }
  }

  /**
   * 查找以与此标签对应的基本块开头的子程序结尾的基本块，并为其中每个块添加一条指向给定子程序调用之后
   * 的基本块的出边。换言之，当此子程序从给定的调用方基本块被调用时，通过添加对应于该子程序返回的边来
   * 补全控制流图。
   *
   * <p>注意：此方法的前置条件和后置条件是所有标签的 {@link #nextListElement} 都必须为 null。
   *
   * @param subroutineCaller a basic block that ends with a jsr to the basic block corresponding to
   *     this label. This label is supposed to correspond to the start of a subroutine.
   */
  final void addSubroutineRetSuccessors(final Label subroutineCaller) {
    // Data flow algorithm: put this basic block in a list blocks to process (which are blocks
    // belonging to a subroutine starting with this label) and, while there are blocks to process,
    // remove one from the list, put it in a list of blocks that have been processed, add a return
    // edge to the successor of subroutineCaller if applicable, and add its successor basic blocks
    // in the control flow graph to the list of blocks to process (if not already done).
    Label listOfProcessedBlocks = EMPTY_LIST;
    Label listOfBlocksToProcess = this;
    listOfBlocksToProcess.nextListElement = EMPTY_LIST;
    while (listOfBlocksToProcess != EMPTY_LIST) {
      // Move a basic block from the list of blocks to process to the list of processed blocks.
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = basicBlock.nextListElement;
      basicBlock.nextListElement = listOfProcessedBlocks;
      listOfProcessedBlocks = basicBlock;

      // Add an edge from this block to the successor of the caller basic block, if this block is
      // the end of a subroutine and if this block and subroutineCaller do not belong to the same
      // subroutine.
      if ((basicBlock.flags & FLAG_SUBROUTINE_END) != 0
          && basicBlock.subroutineId != subroutineCaller.subroutineId) {
        basicBlock.outgoingEdges =
            new Edge(
                basicBlock.outputStackSize,
                // By construction, the first outgoing edge of a basic block that ends with a jsr
                // instruction leads to the jsr continuation block, i.e. where execution continues
                // when ret is called (see {@link #FLAG_SUBROUTINE_CALLER}).
                subroutineCaller.outgoingEdges.successor,
                basicBlock.outgoingEdges);
      }
      // Add its successors to the list of blocks to process. Note that {@link #pushSuccessors} does
      // not push basic blocks which are already in a list. Here this means either in the list of
      // blocks to process, or in the list of already processed blocks. This second list is
      // important to make sure we don't reprocess an already processed block.
      listOfBlocksToProcess = basicBlock.pushSuccessors(listOfBlocksToProcess);
    }
    // Reset the {@link #nextListElement} of all the basic blocks that have been processed to null,
    // so that this method can be called again with a different subroutine or subroutine caller.
    while (listOfProcessedBlocks != EMPTY_LIST) {
      Label newListOfProcessedBlocks = listOfProcessedBlocks.nextListElement;
      listOfProcessedBlocks.nextListElement = null;
      listOfProcessedBlocks = newListOfProcessedBlocks;
    }
  }

  /**
   * 将此标签在方法的控制流图中的后继（除去对应于 jsr 目标的后继，以及已经在某个标签列表中的后继）添
   * 加到给定的待处理块列表中，并返回新列表。
   *
   * @param listOfLabelsToProcess a list of basic blocks to process, linked together with their
   *     {@link #nextListElement} field.
   * @return the new list of blocks to process.
   */
  private Label pushSuccessors(final Label listOfLabelsToProcess) {
    Label newListOfLabelsToProcess = listOfLabelsToProcess;
    Edge outgoingEdge = outgoingEdges;
    while (outgoingEdge != null) {
      // By construction, the second outgoing edge of a basic block that ends with a jsr instruction
      // leads to the jsr target (see {@link #FLAG_SUBROUTINE_CALLER}).
      boolean isJsrTarget =
          (flags & Label.FLAG_SUBROUTINE_CALLER) != 0 && outgoingEdge == outgoingEdges.nextEdge;
      if (!isJsrTarget && outgoingEdge.successor.nextListElement == null) {
        // Add this successor to the list of blocks to process, if it does not already belong to a
        // list of labels.
        outgoingEdge.successor.nextListElement = newListOfLabelsToProcess;
        newListOfLabelsToProcess = outgoingEdge.successor;
      }
      outgoingEdge = outgoingEdge.nextEdge;
    }
    return newListOfLabelsToProcess;
  }

  // -----------------------------------------------------------------------------------------------
  // Overridden Object methods
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回此标签的字符串表示。
   *
   * @return a string representation of this label.
   */
  @Override
  public String toString() {
    return "L" + System.identityHashCode(this);
  }
}
