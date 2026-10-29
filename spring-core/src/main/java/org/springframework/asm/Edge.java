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
 * 方法控制流图中的一条边。该图的每个节点都是一个基本块，用与其第一条指令对应的 Label 表示。每条边
 * 从一个节点指向另一个节点，即从一个基本块指向另一个基本块（分别称为前驱块和后继块）。一条边对应于
 * 跳转或 ret 指令，或者对应于异常处理器。
 *
 * @see Label
 * @author Eric Bruneton
 */
final class Edge {

  /**
   * 对应于跳转或 ret 指令的控制流图边。仅与 {@link ClassWriter#COMPUTE_FRAMES} 一起使用。
   */
  static final int JUMP = 0;

  /**
   * 对应于异常处理器的控制流图边。仅与 {@link ClassWriter#COMPUTE_MAXS} 一起使用。
   */
  static final int EXCEPTION = 0x7FFFFFFF;

  /**
   * 关于此控制流图边的信息。
   *
   * <ul>
   * <li>如果使用 {@link ClassWriter#COMPUTE_MAXS}，此字段包含栈大小增量（对于与跳转指令对应的边），或者值 EXCEPTION（对于与异常处理器对应的边）。栈大小增量是指跳转指令执行后的栈大小减去前驱基本块（即包含该跳转指令的基本块）开始处的栈大小。
   * <li>如果使用 {@link ClassWriter#COMPUTE_FRAMES}，此字段包含值 JUMP（对于与跳转指令对应的边），或者所处理异常类型在 {@link ClassWriter} 类型表中的索引（对于与异常处理器对应的边）。
   * </ul>
   */
  final int info;

  /** 此控制流图边的后继块。 */
  final Label successor;

  /**
   * 基本块出边列表中的下一条边。参见 {@link Label#outgoingEdges}。
   */
  Edge nextEdge;

  /**
   * 构造一个新的 Edge。
   *
   * @param info see {@link #info}.
   * @param successor see {@link #successor}.
   * @param nextEdge see {@link #nextEdge}.
   */
  Edge(final int info, final Label successor, final Edge nextEdge) {
    this.info = info;
    this.successor = successor;
    this.nextEdge = nextEdge;
  }
}
