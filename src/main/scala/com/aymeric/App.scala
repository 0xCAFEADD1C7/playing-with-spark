package com.aymeric

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}

// spark-submit --class com.aymeric.SparkTest --master local spark-test.jar
// où SparkTest contient la methode main

// QUESTION
// statistiques taille de la voiture / accidents
// rollover ?


// SUJET
// 1. objet business
// 2. methode employée, textuelle
// 3. calcul
// 4. deductions ?
// + indiquer comment lancer le code

object App {
  def readCSV(sqlContext: SQLContext, file : String) : DataFrame = {
    sqlContext.read
    .format("com.databricks.spark.csv")
    .option("header", "true")
    .option("inferSchema", "true")
    .load(file)
  }

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("USCrashes")setMaster("local[2]")
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)
    val persons = readCSV(sqlContext, "data/person.csv")
    val bank_holidays = readCSV(sqlContext, "data/bank_holidays.csv")

    persons
      .filter(persons("per_typ") === 1) // filter only conductors
      .join (bank_holidays, bank_holidays("monthe") === persons("month") && bank_holidays("daye") === persons("day"), "left_outer")
      .select("day", "month", "weekday", "age", "sex", "name")
//      .groupBy("month", "bank_holiday", "weekday", "age", "sex")
//      .count()
      .show(5000)
  }
}
