package pl.waw.oledzki.spam_filter

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AppSuite extends AnyFunSuite {
  test("App has a greeting") {
    assert(FilterMail.greeting() != null)
  }
}
