package com.sgw.kafka

import java.util.Properties
import com.sgw.utils.{Time, Timer}
import org.apache.kafka.common.metrics.{KafkaMetric, MetricsReporter}

import scala.collection.JavaConverters._

import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}

object ConsumerPollster {
  def poll[K, V](consumer: KafkaConsumer[K, V], pollTimeout: Long = 100)(callback: Iterator[ConsumerRecord[K, V]] => Boolean): Boolean = {
    val (pollAndGetIteratorTime, iterator) = Timer {
      val (pollTime, records) = Timer {
        consumer.poll(pollTimeout)
      }
      println(s"Poll Time: ${pollTime}")
      records.iterator().asScala
    }
    println(s"Poll + Get Iterator Time: $pollAndGetIteratorTime")

    val result = callback(iterator)
    println(s"Poll Result: $result")

    result
  }

  def pollUntil[K, V](consumer: KafkaConsumer[K, V], pollTimeout: Long = 5000)(callback: Iterator[ConsumerRecord[K, V]] => Boolean): Unit = {
    while (poll(consumer, pollTimeout)(callback)) { }
  }
}

class TestMetricsReporter extends MetricsReporter {
  /**
   * Configure this class with the given key-value pairs
   */
  def configure(configs: java.util.Map[String, _]): Unit = {
    println(s"TestMetricsReporter.configure: $configs")
  }

  /**
   * This is called when the reporter is first registered to initially register all existing metrics
   * @param metrics All currently existing metrics
   */
  def init(metrics: java.util.List[KafkaMetric]): Unit = {
    println("TestMetricsReporter.init: metrics=")
    metrics.asScala.foreach(println)
  }

  /**
   * This is called whenever a metric is updated or added
   * @param metric
   */
  def metricChange(metric: KafkaMetric): Unit = {
    println(s"TestMetricsReporter.metricChange metric=$metric")
  }

  /**
   * This is called whenever a metric is removed
   * @param metric
   */
  def metricRemoval(metric: KafkaMetric): Unit = {
    println(s"TestMetricsReporter.metricRemoval metric=$metric")
  }

  /**
   * Called when the metrics repository is closed.
   */
  def close: Unit = {
    println("TestMetricReporter.close")
  }
}

case class ConsumerMetrics(maxRecords: Int = 100000) {
  private var _totalRecords = 0

  println(s"Max Records: $maxRecords")

  def totalRecords = _totalRecords

  def callback(iterator: Iterator[ConsumerRecord[String, String]]): Boolean = {
    _totalRecords = _totalRecords + iterator.length
    println(s"_totalRecords = $totalRecords")
    println(s"_totalRecords < maxRecords = ${_totalRecords < maxRecords}")
    _totalRecords < maxRecords
  }
}

object Consumer extends App {
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "test")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "5000")
  props.put("auto.offset.reset", "earliest")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  //props.put("metric.reporters", s"[${classOf[TestMetricsReporter].getName}]")

  val consumer = new KafkaConsumer[String, String](props)

  consumer.subscribe(List("test").asJava)

  val metrics = ConsumerMetrics(maxRecords = 100000)

  val (totalTime, _) = Timer {
    ConsumerPollster.pollUntil(consumer)(metrics.callback)
  }

  val rateTotalTime: Double = metrics.totalRecords.toDouble / totalTime.time.toDouble

  println(s"Recs: ${metrics.totalRecords}")
  println(s"Total Time: ${totalTime.toSimplifiedUnits}")
  println(s"Rec/Ms (total time): $rateTotalTime")

  consumer.commitSync()
}
