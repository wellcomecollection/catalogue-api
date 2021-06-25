package weco.catalogue.snapshot_generator.compress

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.RandomGenerators
import weco.catalogue.snapshot_generator.test.utils.GzipUtils

import java.io.{File, FileOutputStream}

class GzipCompressorTest
    extends AnyFunSpec
    with Matchers
    with GzipUtils
    with RandomGenerators {
  it("creates a gzip-compressed file") {
    val strings = (1 to 1000).map { _ =>
      randomAlphanumeric(length = 1000)
    }

    // This code dumps the gzip contents to a gzip file.
    val tmpfile = File.createTempFile("stringToGzipFlowTest", ".txt.gz")
    val outStream = new FileOutputStream(tmpfile)

    GzipCompressor(strings.toIterator)
      .grouped(1024)
      .foreach { bytes =>
        outStream.write(bytes.toArray)
      }

    // Unzip the file, then open it and check it contains the strings
    // we'd expect.  Note that files have a trailing newline.
    val fileContents = readGzipFile(tmpfile.getPath)
    val expectedContents = strings.mkString("\n") + "\n"

    fileContents shouldBe expectedContents
  }
}
