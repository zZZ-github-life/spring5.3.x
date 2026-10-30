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
 * 一个可动态扩展的字节向量。此类大致相当于建立在 ByteArrayOutputStream 之上的 DataOutputStream，但
 * 效率更高。
 *
 * @author Eric Bruneton
 */
public class ByteVector {

  /** 此向量的内容。只有前 {@link #length} 个字节包含真实数据。 */
  byte[] data;

  /** 此向量中实际的字节数。 */
  int length;

  /** 使用默认初始容量构造一个新的 {@link ByteVector}。 */
  public ByteVector() {
    data = new byte[64];
  }

  /**
   * 使用给定的初始容量构造一个新的 {@link ByteVector}。
   *
   * @param initialCapacity 要构造的 byte vector 的初始容量。
   */
  public ByteVector(final int initialCapacity) {
    data = new byte[initialCapacity];
  }

  /**
   * 根据给定的初始数据构造一个新的 {@link ByteVector}。
   *
   * @param data 新 byte vector 的初始数据。
   */
  ByteVector(final byte[] data) {
    this.data = data;
    this.length = data.length;
  }

  /**
   * 返回此向量中实际的字节数。
   *
   * @return 此向量中实际的字节数。
   */
  public int size() {
    return length;
  }

  /**
   * 将一个字节放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param byteValue 一个 byte。
   * @return 此 byte vector。
   */
  public ByteVector putByte(final int byteValue) {
    int currentLength = length;
    if (currentLength + 1 > data.length) {
      enlarge(1);
    }
    data[currentLength++] = (byte) byteValue;
    length = currentLength;
    return this;
  }

  /**
   * 将两个字节放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param byteValue1 一个 byte。
   * @param byteValue2 另一个 byte。
   * @return 此 byte vector。
   */
  final ByteVector put11(final int byteValue1, final int byteValue2) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    length = currentLength;
    return this;
  }

  /**
   * 将一个 short 放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param shortValue 一个 short。
   * @return 此 byte vector。
   */
  public ByteVector putShort(final int shortValue) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * 将一个字节和一个 short 放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param byteValue 一个 byte。
   * @param shortValue 一个 short。
   * @return 此 byte vector。
   */
  final ByteVector put12(final int byteValue, final int shortValue) {
    int currentLength = length;
    if (currentLength + 3 > data.length) {
      enlarge(3);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * 将两个字节和一个 short 放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param byteValue1 一个 byte。
   * @param byteValue2 另一个 byte。
   * @param shortValue 一个 short。
   * @return 此 byte vector。
   */
  final ByteVector put112(final int byteValue1, final int byteValue2, final int shortValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * 将一个 int 放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param intValue 一个 int。
   * @return 此 byte vector。
   */
  public ByteVector putInt(final int intValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * 将一个字节和两个 short 放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param byteValue 一个 byte。
   * @param shortValue1 一个 short。
   * @param shortValue2 另一个 short。
   * @return 此 byte vector。
   */
  final ByteVector put122(final int byteValue, final int shortValue1, final int shortValue2) {
    int currentLength = length;
    if (currentLength + 5 > data.length) {
      enlarge(5);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue1 >>> 8);
    currentData[currentLength++] = (byte) shortValue1;
    currentData[currentLength++] = (byte) (shortValue2 >>> 8);
    currentData[currentLength++] = (byte) shortValue2;
    length = currentLength;
    return this;
  }

  /**
   * 将一个 long 放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param longValue 一个 long。
   * @return 此 byte vector。
   */
  public ByteVector putLong(final long longValue) {
    int currentLength = length;
    if (currentLength + 8 > data.length) {
      enlarge(8);
    }
    byte[] currentData = data;
    int intValue = (int) (longValue >>> 32);
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    intValue = (int) longValue;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * 将一个 UTF8 字符串放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param stringValue 一个 String，其 UTF8 编码长度必须小于 65536。
   * @return 此 byte vector。
   */
  // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
  public ByteVector putUTF8(final String stringValue) {
    int charLength = stringValue.length();
    if (charLength > 65535) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    int currentLength = length;
    if (currentLength + 2 + charLength > data.length) {
      enlarge(2 + charLength);
    }
    byte[] currentData = data;
    // Optimistic algorithm: instead of computing the byte length and then serializing the string
    // (which requires two loops), we assume the byte length is equal to char length (which is the
    // most frequent case), and we start serializing the string right away. During the
    // serialization, if we find that this assumption is wrong, we continue with the general method.
    currentData[currentLength++] = (byte) (charLength >>> 8);
    currentData[currentLength++] = (byte) charLength;
    for (int i = 0; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= '\u0001' && charValue <= '\u007F') {
        currentData[currentLength++] = (byte) charValue;
      } else {
        length = currentLength;
        return encodeUtf8(stringValue, i, 65535);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * 将一个 UTF8 字符串放入此字节向量。必要时会自动扩展该字节向量。如果有空间（即 this.length -
   * offset - 2 &gt;= 0），字符串长度会在编码后的字符之前用两个字节进行编码。
   *
   * @param stringValue 要编码的 String。
   * @param offset 要编码的第一个字符的索引。假定之前的字符已编码，且每个字符仅使用一个字节。
   * @param maxByteLength 编码后字符串的最大字节长度，包括已编码的字符。
   * @return 此 byte vector。
   */
  final ByteVector encodeUtf8(final String stringValue, final int offset, final int maxByteLength) {
    int charLength = stringValue.length();
    int byteLength = offset;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        byteLength++;
      } else if (charValue <= 0x07FF) {
        byteLength += 2;
      } else {
        byteLength += 3;
      }
    }
    if (byteLength > maxByteLength) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    // Compute where 'byteLength' must be stored in 'data', and store it at this location.
    int byteLengthOffset = length - offset - 2;
    if (byteLengthOffset >= 0) {
      data[byteLengthOffset] = (byte) (byteLength >>> 8);
      data[byteLengthOffset + 1] = (byte) byteLength;
    }
    if (length + byteLength - offset > data.length) {
      enlarge(byteLength - offset);
    }
    int currentLength = length;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        data[currentLength++] = (byte) charValue;
      } else if (charValue <= 0x07FF) {
        data[currentLength++] = (byte) (0xC0 | charValue >> 6 & 0x1F);
        data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      } else {
        data[currentLength++] = (byte) (0xE0 | charValue >> 12 & 0xF);
        data[currentLength++] = (byte) (0x80 | charValue >> 6 & 0x3F);
        data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * 将一个字节数组放入此字节向量。必要时会自动扩展该字节向量。
   *
   * @param byteArrayValue 一个字节数组。可以为 {@literal null}，以将 {@code byteLength} 个 null 字节放入
   * 此 byte vector。
   * @param byteOffset 必须复制的 byteArrayValue 第一个字节的索引。
   * @param byteLength 必须复制的 byteArrayValue 字节数。
   * @return 此 byte vector。
   */
  public ByteVector putByteArray(
      final byte[] byteArrayValue, final int byteOffset, final int byteLength) {
    if (length + byteLength > data.length) {
      enlarge(byteLength);
    }
    if (byteArrayValue != null) {
      System.arraycopy(byteArrayValue, byteOffset, data, length, byteLength);
    }
    length += byteLength;
    return this;
  }

  /**
   * 扩展此字节向量，使其能够再接收 'size' 个字节。
   *
   * @param size 此 byte vector 应能额外接收的字节数。
   */
  private void enlarge(final int size) {
    if (length > data.length) {
      throw new AssertionError("Internal error");
    }
    int doubleCapacity = 2 * data.length;
    int minimalCapacity = length + size;
    byte[] newData = new byte[doubleCapacity > minimalCapacity ? doubleCapacity : minimalCapacity];
    System.arraycopy(data, 0, newData, 0, length);
    data = newData;
  }
}
