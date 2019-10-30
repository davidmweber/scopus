/*
 * Copyright 2019 David Weber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.monadic.scopus

import za.co.monadic.scopus.opus.{OpusEncoder, OpusDecoderShort}

import scala.util.Try

/**
  *
  */
object Stub extends App {

  val enc = OpusEncoder(Sf8000, 1, Audio)
  enc.setUseDtx(1)
  // Transmit special short packets if silence is detected

  val dec = OpusDecoderShort(Sf8000, 1)

  val coded: Try[Array[Byte]] = enc(new Array[Short](160))
  // Transmit

  // On receive end
  val decoded: Try[Array[Short]] = dec(coded.get)

  println("Done....")
}
