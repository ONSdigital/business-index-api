package services

import java.util.{Date, Properties}
import javax.mail.Message.RecipientType._
import javax.mail._
import javax.mail.internet._

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import uk.gov.ons.bi.ingest.helper.Utils._

/**
  * Created by Volodymyr.Glushak on 09/03/2017.
  */
class MailAgent(implicit config: Config) {

  private[this] val logger = LoggerFactory.getLogger(getClass)
  private[this] val properties = new Properties()
  private[this] val props = List("mail.smtp.host", "mail.smtp.port", "mail.smtp.auth", "mail.smtp.starttls.enable")
  props.foreach { p =>
    properties.put(p, configOverride(p))
  }

  private[this] val auth = if (configOverride("mail.smtp.auth").toBoolean)
    new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(configOverride("mail.smtp.user"), configOverride("mail.smtp.password"))
      }
    }
  else null

  // throws MessagingException
  def sendMessage(subject: String, content: String, to: String, from: String): Unit = {
    val message = createMessage
    message.setFrom(new InternetAddress(from))
    setMessageRecipients(message, to, TO)

    message.setSentDate(new Date())
    message.setSubject(subject)
    message.setText(content)
    Transport.send(message)
    logger.debug(s"Sent email $subject")
  }

  private[this] def createMessage: Message = {
    val session = Session.getDefaultInstance(properties, auth)
    new MimeMessage(session)
  }

  // throws AddressException, MessagingException
  private[this] def setMessageRecipients(message: Message, recipient: String, recipientType: Message.RecipientType) {
    // had to do the asInstanceOf[...] call here to make scala happy
    Option(InternetAddress.parse(recipient).asInstanceOf[Array[Address]])
      .filter(_.length > 0).foreach { addressArray =>
      message.setRecipients(recipientType, addressArray)
    }
  }

}