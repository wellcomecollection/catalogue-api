package weco.api.snapshot_generator.compress

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

object GzipCompressor {
  def apply(strings: Iterator[String]): Iterator[Byte] = {
    val stream = new ByteArrayOutputStream()
    val compressedStream = new GZIPOutputStream(stream)

    var hasWrittenTrailer = false

    val iterator = new Iterator[Iterator[Byte]] {
      override def hasNext: Boolean =
        !hasWrittenTrailer || strings.hasNext

      override def next(): Iterator[Byte] =
        // Is there a line we haven't handled yet?  If so, update the
        // compressed stream, then fetch the newly written bytes from the
        // underlying byte stream.
        //
        // Once we've done that, reset the underlying stream before the
        // next iteration.
        if (strings.hasNext) {
          val line = strings.next() + "\n"
          compressedStream.write(line.getBytes())
          val bytes = stream.toByteArray
          stream.reset()
          bytes.toIterator
        }
        // If we're run out of lines, then we're done.  The compressor
        // stream may not have written all the bytes yet, depending on
        // whether the last line finished on a suitable boundary.
        //
        // Finish writing the compressed stream, then retrieve the last
        // set of bytes and return.
        else {
          hasWrittenTrailer = true
          compressedStream.finish()

          // This closes both streams, so if we try to write to them again,
          // (which would be wrong), we'll get an error.
          compressedStream.close()

          stream.toByteArray.toIterator
        }
    }

    iterator.flatten
  }
}
