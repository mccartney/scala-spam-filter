package pl.waw.oledzki.spam_filter

import com.amazonaws.services.lambda.runtime.RequestHandler

trait LambdaMain extends RequestHandler[Input, Output] {

}

case class Input() {}
case class Output() {}
