package scala

import java.io.ByteArrayOutputStream

import akka.util.ByteString
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.{ ContentBody, FileBody, StringBody }
import play.api.http.Writeable
import play.api.mvc.{ AnyContentAsMultipartFormData, Codec }

/**
 * Created by Volodymyr.Glushak on 07/04/2017.
 */
object FakeMultipartUpload {
  implicit def writeableOf_multiPartFormData(implicit codec: Codec): Writeable[AnyContentAsMultipartFormData] = {
    val builder = MultipartEntityBuilder.create().setBoundary("12345678")

    def transform(multipart: AnyContentAsMultipartFormData): ByteString = {
      multipart.mdf.dataParts.foreach { part =>
        part._2.foreach { p2 =>
          builder.addPart(part._1, new StringBody(p2, ContentType.create("text/plain", "UTF-8")))
        }
      }
      multipart.mdf.files.foreach { file =>
        val part: ContentBody = new FileBody(
          file.ref.file,
          ContentType.create(file.contentType.getOrElse("application/octet-stream")), file.filename
        )
        builder.addPart(file.key, part)
      }

      val outputStream = new ByteArrayOutputStream
      builder.build.writeTo(outputStream)
      ByteString(outputStream.toByteArray)
    }

    new Writeable(transform, Some(builder.build.getContentType.getValue))
  }
}