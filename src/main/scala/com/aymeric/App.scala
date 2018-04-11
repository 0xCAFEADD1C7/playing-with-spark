package com.aymeric

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.sql.functions._

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
  def runTrainingAndEstimation(data: DataFrame, labelCol: String, numFeatCols: Array[String],
                               categFeatCols: Array[String] = Array()): DataFrame = {

    val lr = new LinearRegression().setLabelCol(labelCol)
    val assembler = new VectorAssembler()
    val pipeline = new Pipeline()

    // si on a aucune colonne catégorielle
    if (categFeatCols.length == 0) {

      assembler
        .setInputCols(numFeatCols)
        .setOutputCol("features")
      pipeline.setStages(Array(assembler, lr))

    } else {
      // sinon, nos colonnes vont etre les col numériques + les colonnes catégorielles binarisées
      var featureCols = numFeatCols

      // indexer = prends une colonne de chaines de caracteres, la transforme en colonne numerique, où chaque string
      // a un id unique trié par fréquence
      val indexers = categFeatCols.map(c =>
        new StringIndexer().setInputCol(c).setOutputCol(s"${c}_idx")
      )

      // transforme les valeurs numériques précédentes en colonnes booleennes
      val encoders = categFeatCols.map(c => {
        val outputCol = s"${c}_enc"
        featureCols = featureCols :+ outputCol
        new OneHotEncoder().setInputCol(s"${c}_idx").setOutputCol(outputCol)
      })

      assembler
        .setInputCols(featureCols)
        .setOutputCol("features")
      pipeline.setStages(indexers ++ encoders ++ Array(assembler, lr))
    }

    val Array(trainSet, testSet) = data
      .randomSplit(Array(0.99, 0.01), seed=12345)

    // Entrainement du modèle sur trainSet
    val modelLR = pipeline.fit(trainSet)

    // Prédiction sur testSet
    val predictions = modelLR.transform(testSet)
    predictions.select("prediction", labelCol).show()

    val predictionsAndObservations = predictions
      .select("prediction", labelCol)
      .rdd
      .map(row => (row.getDouble(0), row.getDouble(1)))
    val metrics = new RegressionMetrics(predictionsAndObservations)
    val rmse = metrics.rootMeanSquaredError
    println("RMSE: " + rmse)

    predictions
  }

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
    val accident = readCSV(sqlContext, "accident_hosp.csv")
    val bank_holidays = readCSV(sqlContext, "data/bank_holidays.csv")

    val c = accident
    val eh = c("ARR_HOUR")
    val em = c("ARR_MIN")
    val ah = c("HOUR")
    val am = c("MINUTE")

    val cleaned1 = c
      .withColumn("emerg_time",
        when(eh < 24 && em < 60 && ah < 24 && am < 60,
          (((eh + when(ah > eh, 24).otherwise(0)) - ah) * 60.0) + (em-am))
            .otherwise(null)
      )
//      .withColumn("dist3h", (- ah + 24 + 3)) ===> milieu de la nuit, 3h du mat
    // probleme de enum 'infinies' ex Ville. Solutions : enum finie, ou géolocaliser

    // city : jointure, etc GLC
    val cleaned = cleaned1.filter(cleaned1("emerg_time").isNotNull && cleaned1("emerg_time") < 120 && cleaned1("emerg_time") > 0)

    runTrainingAndEstimation(cleaned, "emerg_time", Array("MILEPT", "PERSONS"), Array("HOUR", "ROUTE", "RUR_URB", "RELJCT1", "RELJCT2", "TYP_INT", "LGT_COND", "WEATHER"))

//    persons
//      .filter(persons("per_typ") === 1) // filter only conductors
//      .join (bank_holidays, bank_holidays("monthe") === persons("month") && bank_holidays("daye") === persons("day"), "left_outer")
//      .select("day", "month", "weekday", "age", "sex", "name")
////      .groupBy("month", "bank_holiday", "weekday", "age", "sex")
////      .count()
//      .show(5000)
  }
}
