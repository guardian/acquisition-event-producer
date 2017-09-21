package com.gu.acquisition.fixtures

import ophan.thrift.event.AbTest
import com.gu.acquisition.utils.AbTestConverter

case class ExampleABTest(testName: String, variantName: String)

object ExampleABTest {
  implicit val abTestConverter = new AbTestConverter[ExampleABTest] {
    override def asAbTest(test: ExampleABTest): AbTest = AbTest(test.testName, test.variantName)
  }
}