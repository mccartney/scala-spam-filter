package pl.waw.oledzki.spam_filter

import org.apache.commons.net.imap.IMAPSClient

import scala.io.Source

object FilterMail extends App {

  val imap = new IMAPSClient("TLS", true)
  imap.connect("imap.poczta.onet.pl", 993)
  imap.login(Source.fromFile("/tmp/user.txt").mkString, Source.fromFile("/tmp/haslo.txt").mkString)

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


  def applyFilter(messageHeaders: Map[String, String]): Boolean = {
    val MailingReklamowy = ".*mailing_reklamowy@onet.pl.*".r
    val Mailingi = ".*mailingi@onet.pl.*".r
    val Spamerzy = ".*(?:tolpa[.]pl)|(?:Norton).*".r

    System.out.println(messageHeaders.get("From"))
    messageHeaders.get("From") match {
      case Some(MailingReklamowy()) => true
      case Some(Mailingi()) => true
      case Some(Spamerzy()) => true
      case _ => false
    }
  }

}
