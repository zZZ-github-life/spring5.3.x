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
 * 关于异常处理器的信息。它对应于 Java 虚拟机规范（JVMS）中定义的 Code 属性的 exception_table 数组
 * 的一个元素。Handler 实例可以通过其 {@link #nextHandler} 字段链接在一起，以描述完整的 JVMS
 * exception_table 数组。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3">JVMS
 *     4.7.3</a>
 * @author Eric Bruneton
 */
final class Handler {

  /**
   * 此 JVMS exception_table 条目的 start_pc 字段。对应于异常处理器作用域的开始（包含）。
   */
  final Label startPc;

  /**
   * 此 JVMS exception_table 条目的 end_pc 字段。对应于异常处理器作用域的结束（不包含）。
   */
  final Label endPc;

  /**
   * 此 JVMS exception_table 条目的 handler_pc 字段。对应于异常处理器代码的开始。
   */
  final Label handlerPc;

  /**
   * 此 JVMS exception_table 条目的 catch_type 字段。它是此处理器所处理的异常类型的内部名称在常量池中
   * 的索引，若要捕获任何异常则为 0。
   */
  final int catchType;

  /**
   * 此处理器所处理的异常类型的内部名称，若要捕获任何异常则为 {@literal null}。
   */
  final String catchTypeDescriptor;

  /** 下一个异常处理器。 */
  Handler nextHandler;

  /**
   * 构造一个新的 Handler。
   *
   * @param startPc 此 JVMS exception_table 条目的 start_pc 字段。
   * @param endPc 此 JVMS exception_table 条目的 end_pc 字段。
   * @param handlerPc 此 JVMS exception_table 条目的 handler_pc 字段。
   * @param catchType 此 JVMS exception_table 条目的 catch_type 字段。
   * @param catchTypeDescriptor 此处理器所处理异常类型的内部名称，或 {@literal null} 以捕获所有异常。
   */
  Handler(
      final Label startPc,
      final Label endPc,
      final Label handlerPc,
      final int catchType,
      final String catchTypeDescriptor) {
    this.startPc = startPc;
    this.endPc = endPc;
    this.handlerPc = handlerPc;
    this.catchType = catchType;
    this.catchTypeDescriptor = catchTypeDescriptor;
  }

  /**
   * 根据给定的 Handler 构造一个新的 Handler，其作用域不同。
   *
   * @param handler 一个现有的 Handler。
   * @param startPc 此 JVMS exception_table 条目的 start_pc 字段。
   * @param endPc 此 JVMS exception_table 条目的 end_pc 字段。
   */
  Handler(final Handler handler, final Label startPc, final Label endPc) {
    this(startPc, endPc, handler.handlerPc, handler.catchType, handler.catchTypeDescriptor);
    this.nextHandler = handler.nextHandler;
  }

  /**
   * 从以给定元素开头的 Handler 列表中移除 start 与 end 之间的范围。
   *
   * @param firstHandler Handler 列表的起始。可以为 {@literal null}。
   * @param start 要移除范围的起始。
   * @param end 要移除范围的结束。可以为 {@literal null}。
   * @return 移除了 start-end 范围的异常处理器列表。
   */
  static Handler removeRange(final Handler firstHandler, final Label start, final Label end) {
    if (firstHandler == null) {
      return null;
    } else {
      firstHandler.nextHandler = removeRange(firstHandler.nextHandler, start, end);
    }
    int handlerStart = firstHandler.startPc.bytecodeOffset;
    int handlerEnd = firstHandler.endPc.bytecodeOffset;
    int rangeStart = start.bytecodeOffset;
    int rangeEnd = end == null ? Integer.MAX_VALUE : end.bytecodeOffset;
    // Return early if [handlerStart,handlerEnd[ and [rangeStart,rangeEnd[ don't intersect.
    if (rangeStart >= handlerEnd || rangeEnd <= handlerStart) {
      return firstHandler;
    }
    if (rangeStart <= handlerStart) {
      if (rangeEnd >= handlerEnd) {
        // If [handlerStart,handlerEnd[ is included in [rangeStart,rangeEnd[, remove firstHandler.
        return firstHandler.nextHandler;
      } else {
        // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ = [rangeEnd,handlerEnd[
        return new Handler(firstHandler, end, firstHandler.endPc);
      }
    } else if (rangeEnd >= handlerEnd) {
      // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ = [handlerStart,rangeStart[
      return new Handler(firstHandler, firstHandler.startPc, start);
    } else {
      // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ =
      //     [handlerStart,rangeStart[ + [rangeEnd,handerEnd[
      firstHandler.nextHandler = new Handler(firstHandler, end, firstHandler.endPc);
      return new Handler(firstHandler, firstHandler.startPc, start);
    }
  }

  /**
   * 返回以给定元素开头的 Handler 列表的元素数量。
   *
   * @param firstHandler Handler 列表的起始。可以为 {@literal null}。
   * @return 以 'handler' 开头的 Handler 列表的元素数量。
   */
  static int getExceptionTableLength(final Handler firstHandler) {
    int length = 0;
    Handler handler = firstHandler;
    while (handler != null) {
      length++;
      handler = handler.nextHandler;
    }
    return length;
  }

  /**
   * 返回与以给定元素开头的 Handler 列表相对应的 JVMS exception_table 的字节大小。<i>这包括
   * exception_table_length 字段。</i>
   *
   * @param firstHandler Handler 列表的起始。可以为 {@literal null}。
   * @return exception_table_length 和 exception_table 结构的字节大小。
   */
  static int getExceptionTableSize(final Handler firstHandler) {
    return 2 + 8 * getExceptionTableLength(firstHandler);
  }

  /**
   * 放入与以给定元素开头的 Handler 列表相对应的 JVMS exception_table。<i>这包括
   * exception_table_length 字段。</i>
   *
   * @param firstHandler Handler 列表的起始。可以为 {@literal null}。
   * @param output 放置 exception_table_length 和 exception_table 结构的位置。
   */
  static void putExceptionTable(final Handler firstHandler, final ByteVector output) {
    output.putShort(getExceptionTableLength(firstHandler));
    Handler handler = firstHandler;
    while (handler != null) {
      output
          .putShort(handler.startPc.bytecodeOffset)
          .putShort(handler.endPc.bytecodeOffset)
          .putShort(handler.handlerPc.bytecodeOffset)
          .putShort(handler.catchType);
      handler = handler.nextHandler;
    }
  }
}
