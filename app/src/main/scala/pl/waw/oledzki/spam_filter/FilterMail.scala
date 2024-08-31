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
    println(">1")
    val imap = new IMAPSClient("TLS", true)
    imap.connect("imap.poczta.onet.pl", 993)
    imap.login(user, password)

    println(">2")

    // List all mailboxes
    // imap.list("", "*")

    imap.select("INBOX")

    println(">3")

    imap.fetch("1:*", "(INTERNALDATE)")

    println(">4")

    val lastMessages = imap.getReplyStrings.toList.filter(_.contains("FETCH"))
      .takeRight(20)
      .map(line => line.split(" ")(1).toInt)

    println(s">5 ${lastMessages.size}")

    lastMessages.foreach { messageId =>
      imap.fetch(messageId.toString, "BODY.PEEK[HEADER]")
      val allHeaders = imap.getReplyString
      val headers = imap.getReplyStrings.toList
        .filter(_.matches(".*:...*"))
        .map { line =>
          val (key, value) = line.splitAt(line.indexOf(":"))
          key -> value.substring(2)
        }.toMap

      System.out.println(headers.get("From"))

      if (applyFilter(allHeaders)) {
        System.out.println(s"Moving $messageId to Trash")

        imap.copy(messageId.toString, "Trash")
        imap.store(messageId.toString, "+FLAGS", "(\\Deleted)")
      }
    }

    println(s">6 BEFORE CLOSE")

    try {
      imap.close()
    } catch {
      case _: Exception =>
    }
  }


  def applyFilter(messageHeaders: String): Boolean = {
    val Spamerzy = List(
      "mailing_reklamowy@onet.pl", "mailingi@onet.pl",
      "tolpa.pl", "Norton", "laconexion", "palaceestate", "poitiers",
      "milka.pl", "iPhone", "{ Grzegon }", "simonsnewsletter.ca", "c.ru",
      "{ Mccartney }", "quickship.pl", "Fwd:\\n", "itcoin", "message has been altered",
      "hou.top", "{Grzegon}", "{Mccartney}", "BRAK POTWIERDZENIA",
      "sunderlandecho", "fastymail", "pastapoint0", "cyberski.net", "Centrum dystrybucji - Onet",
      "QUICK LOAN", "stxkr.pl", "dezeen.com", "jrojbh.ru", "From: śledzenie przesyłki",
      "swagbucks.com", "milka.pl", "lucindaburman", "turbomail.pl", "servicecentralinc.com",
      "podkalicki.com", "ezvacuum.com", "Vuitton", "timedlacb.pl",
      "silver-stage.de", "o2.pl.com", "argongames", "expertsender.com",
      "sendcampaigns.pl",
      "campaignzone.pl", "newsletternews.pl",
      "pressingphotoshop.org.uk",
      "xjkasjfaosenagiasejge", "fentanyl",
      "perfections.shop", "rrbv.de",
      "emaillabs.net.pl", "clicknextxpro.pl",
      )
    Spamerzy.exists(messageHeaders.contains)
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
