package pl.waw.oledzki.spam_filter

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import org.apache.commons.net.imap.IMAPSClient

import scala.io.Source

class FilterMail extends LambdaMain {

  override def handleRequest(input: Input, context: Context): Output = {
    val (user, password) = retrieveCredentials()
    main(user, password)
    Output()
  }

  def main(user: String, password: String): Unit = {
    val imap = new IMAPSClient("TLS", true)
    imap.connect("imap.poczta.onet.pl", 993)
    imap.login(user, password)

    // List all mailboxes
    // imap.list("", "*")

    imap.select("INBOX")

    imap.fetch("1:*", "(INTERNALDATE)")

    val lastMessages = imap.getReplyStrings.toList.filter(_.contains("FETCH"))
      .takeRight(10)
      .map(line => line.split(" ")(1).toInt)

    lastMessages.foreach { messageId =>
      imap.fetch(messageId.toString, "BODY.PEEK[HEADER]")
      val headers = imap.getReplyStrings.toList
        .filter(_.contains(":"))
        .map { line =>
          val (key, value) = line.splitAt(line.indexOf(":"))
          key -> value.substring(2)
        }.toMap

      if (applyFilter(headers)) {
        System.out.println(s"Moving $messageId to Trash")

        imap.copy(messageId.toString, "Trash")
        imap.store(messageId.toString, "+FLAGS", "(\\Deleted)")
      }
    }

    try {
      imap.close()
    } catch {
      case _: Exception =>
    }
  }


  def applyFilter(messageHeaders: Map[String, String]): Boolean = {
    val MailingReklamowy = ".*mailing_reklamowy@onet.pl.*".r
    val Mailingi = ".*mailingi@onet.pl.*".r
    val Spamerzy = List("tolpa.pl", "Norton", "laconexion", "palaceestate")

    System.out.println(messageHeaders.get("From"))
    messageHeaders.get("From") match {
      case Some(MailingReklamowy()) => true
      case Some(Mailingi()) => true
      case Some(string) if Spamerzy.exists(_.contains(string)) => true
      case _ => false
    }
  }

  def retrieveCredentials(): (String, String) = {
    val secretName = "poczta/onet/grzegon"
    val region = "eu-west-1"

    val secrets = AWSSecretsManagerClientBuilder.standard()
      .withRegion(region)
      .build()

    val getSecretValueRequest = new GetSecretValueRequest()
      .withSecretId(secretName);
    val lookupResult = secrets.getSecretValue(getSecretValueRequest);

    // naive JSON parsing
    val values = lookupResult.getSecretString.split("\"")
    (values(1), values(3))
  }
}
