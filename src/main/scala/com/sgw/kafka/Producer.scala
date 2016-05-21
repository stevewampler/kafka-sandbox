package com.sgw.kafka

import java.util.Properties
import com.sgw.utils.Timer
import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}

object Producer extends App {
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("acks", "all")
  props.put("retries", 0.toString)
  props.put("batch.size", 16384.toString)
  props.put("linger.ms", 1.toString)
  props.put("buffer.memory", 33554432.toString)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer(props)

  val numRecords = 1000000

  val (age, _) = Timer {
    (0 to numRecords).map(
      i => new ProducerRecord(
        "test", i.toString, i.toString
      ).asInstanceOf[ProducerRecord[Nothing, Nothing]]
    ).foreach(record => producer.send(record))
  }

  val rate: Double = numRecords.toDouble / age.time.toDouble

  println(s"Recs: $numRecords")
  println(s"Age : $age")
  println(s"Rec/Ms: $rate")

  producer.close()
}
