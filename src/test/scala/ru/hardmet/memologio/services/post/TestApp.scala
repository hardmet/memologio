package ru.hardmet.memologio
package services
package post

import io.circe
import io.circe.Decoder
import io.circe.parser._

trait Sentiment

object Sentiment {
  case object Fear extends Sentiment

  implicit val sentimentDecoder: Decoder[Sentiment] = Decoder.decodeString.map {
    case "Fear" => Fear
  }
}

case class CryptoData(value: String, valueClassification: Sentiment)

object CryptoData {
implicit val cryptoDecoder: Decoder[CryptoData] = Decoder.instance { json =>
  val data = json.downField("data").downArray
  for {
    value <- data.get[String]("value")
    valueClassification <- data.get[Sentiment]("value_classification")
  } yield CryptoData(value, valueClassification)
}


  def decodeRaw(extendedObject: String): Either[circe.Error, Array[CryptoData]] =
    parse(extendedObject).flatMap(json => json.hcursor.downField("data").as[Array[CryptoData]])
}

object TestApp extends App {

  val extendedJson =
    """
      |{
      |  "name" : "Fear and Greed Index",
      |  "data" : [
      |    {
      |      "value" : "31",
      |      "value_classification" : "Fear",
      |      "timestamp" : "1631318400",
      |      "time_until_update" : "54330"
      |    }
      |  ],
      |  "metadata" : {
      |    "error" : null
      |  }
      |}
      |""".stripMargin

  println(parse(extendedJson).flatMap(_.as[CryptoData]))

  //  val result: Either[circe.Error, Array[CryptoData]] = CryptoData.decodeRaw(extendedJson) // here should be Array
  //println(result.map(_.mkString(", ")))


  val cryptoDataJson =
    """
      |{
      |      "value" : "31",
      |      "value_classification" : "Fear",
      |      "timestamp" : "1631318400",
      |      "time_until_update" : "54330"
      |    }
      |""".stripMargin


  //println(decode[CryptoData](cryptoDataJson))
}