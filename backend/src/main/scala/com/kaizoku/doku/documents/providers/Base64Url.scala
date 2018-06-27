package com.kaizoku.doku.documents.providers

import java.util.UUID
import java.nio.ByteBuffer

import scala.annotation.tailrec

object Base64Url {

  private[this] val encodeTable: IndexedSeq[Char] = ('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('-', '_')
  private[this] val prefix                        = Array(127, 127, 127).map(_.toByte)
  // to force only positive BigInts
  private[this] val paddingZeros = Array(0, 0).map(_.toByte)
  private[this] val zero         = BigInt(0)

  implicit class Encoder(val b: Array[Byte]) {
    final def base64() =
      encode(b)
  }

  final def encode(uuid: UUID): String = {

    val bb = ByteBuffer.wrap(new Array[Byte](16))

    bb.putLong(uuid.getMostSignificantBits())
    bb.putLong(uuid.getLeastSignificantBits())

    encode(bb.array())
  }

  final def encode(b: Array[Byte]): String = {
    val pad = (3 - b.length % 3) % 3

    @tailrec
    def enc(z: BigInt, acc: Seq[Char]): Seq[Char] =
      if (z == zero) acc
      else {
        val (div, rem) = z /% 64
        enc(div, encodeTable(rem.toInt) +: acc)
      }

    if (b.size > 0) enc(BigInt(prefix ++ b ++ paddingZeros.take(pad)), IndexedSeq()).drop(4).dropRight(pad).mkString
    else ""
  }
}
