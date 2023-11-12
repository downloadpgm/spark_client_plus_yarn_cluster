package AdultPredML

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.DataFrame

object AdultPredML {

  def main (args: Array[String] ) {

    val spark = SparkSession.builder().appName("Iniciando com Spark").getOrCreate()
    
    spark.conf.set("spark.sql.shuffle.partitions",10)

    val df = spark.read.format("csv").option("inferSchema","true").load("hdfs://hdpmst:9000/data/adult.data").toDF("age","workclass","fnlwgt","education","education-num","marital_status","occupation","relationship","race","sex","capital_gain","capital_loss","hours_per_week","native_country","income")

    import org.apache.spark.ml.feature.SQLTransformer

    val sql = new SQLTransformer().setStatement("""
       SELECT age,
          case when workclass like "%?%" then "Private" else workclass end as workclass,
          fnlwgt,education,marital_status,
          case when occupation like "%?%" then "Prof-specialty" else occupation end as occupation,
          relationship,race,sex,capital_gain,capital_loss,hours_per_week,
          case when native_country like "%?%" then "United-States" else native_country end as native_country,
          income
       FROM __THIS__""")

    import org.apache.spark.ml.feature.RFormula
    val rf = new RFormula().setFormula("income ~ .")

    import org.apache.spark.ml.feature.StandardScaler
    val stdScaler = new StandardScaler().
       setWithStd(true).
       setWithMean(true).
       setInputCol("features").
       setOutputCol("scaledFeatures")

    import org.apache.spark.ml.classification.LogisticRegression
    val lr = new LogisticRegression().setFeaturesCol("scaledFeatures")

    val Array(trainingData, testData) = df.randomSplit(Array(0.7,0.3),11L)

    trainingData.cache
    testData.cache

    import org.apache.spark.ml.Pipeline
    val pipeline = new Pipeline().setStages(Array(sql,rf,stdScaler,lr))
    val pipelinemodel = pipeline.fit(trainingData)

    val pred = pipelinemodel.transform(testData)
    println("Logistic Regression")
    pred.groupBy("label","prediction").count().show(5)

    //val pred = pipelinemodel.transform(testData)
    //pred.write.mode("overwrite").format("json").save("hdfs://hdpmst:9000/model/pred")

    //val pipelinemodel_final = pipeline.fit(df)
    //pipelinemodel_final.write.overwrite.save("hdfs://hdpmst:9000/model/adult")
  }
}