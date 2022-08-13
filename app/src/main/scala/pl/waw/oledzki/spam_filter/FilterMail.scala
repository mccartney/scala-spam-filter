package pl.waw.oledzki.spam_filter


import com.sun.mail.imap.protocol.IMAPResponse
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode
import com.yahoo.imapnio.async.client.{ImapAsyncClient, ImapAsyncCreateSessionResponse, ImapAsyncSessionConfig}
import com.yahoo.imapnio.async.data.MessageNumberSet.LastMessage
import com.yahoo.imapnio.async.data.{Capability, MessageNumberSet}
import com.yahoo.imapnio.async.request.{AuthPlainCommand, CapaCommand, FetchCommand, ImapRequest, ListCommand, NamespaceCommand, SelectFolderCommand}
import com.yahoo.imapnio.async.response.ImapResponseMapper

import java.net.{InetSocketAddress, URI}
import javax.xml.stream.events.Namespace
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

object FilterMail extends App {

  val imapClient = new ImapAsyncClient(5)


  val serverUri = new URI("imaps://imap.poczta.onet.pl:993")
  val config = new ImapAsyncSessionConfig
  config.setConnectionTimeoutMillis(5000)
  config.setReadTimeoutMillis(6000)
  val sniNames = null

  val localAddress: InetSocketAddress = null
  val session = imapClient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF)

  val x = Future {
    session.get
  }.map { s => 
    val capas = s.getSession.execute(new CapaCommand()).get

    val mapper = new ImapResponseMapper()
    val capabilities = mapper.readValue(capas.getResponseLines.asScala.toArray, classOf[Capability])

    s.getSession.execute(new AuthPlainCommand("mccartney@poczta.onet.pl", Source.fromFile("/tmp/haslo.txt", "ASCII").mkString, capabilities)).get
    s
  }.map { s =>
    println("A")
    println(">>> " + s.getSession.execute(new SelectFolderCommand("INBOX")).get.getResponseLines.asScala.toList)

    val response = s.getSession.execute(new FetchCommand(Array(new MessageNumberSet(LastMessage.LAST_MESSAGE)), "RFC822")).get.getResponseLines.asScala.toList

    response.zipWithIndex.foreach { case (line, idx) =>
      println(s"${idx} >>>> $line")
    }



    s
  }.onComplete {
    case Success(s) =>
      imapClient.shutdown()
    case Failure(e) =>
      e.printStackTrace()
      imapClient.shutdown()
  }
}
