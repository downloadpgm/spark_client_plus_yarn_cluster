
package AdultPredMLlib

import org.apache.spark._

object AdultPredMLlib {

  def main (args: Array[String] ) {

    //val spark = SparkSession.builder().appName("Iniciando com Spark").getOrCreate()    
	//val sc = spark.SparkContext

    val conf = new SparkConf().setAppName("Iniciando com Spark")
    val sc = new SparkContext(conf)

    // Feature extraction & Data Munging --------------

    val rdd = sc.textFile("adult.data").map(x => x.split(",")).map( x => x.map( y => y.trim()))

    rdd.cache

    // Conversion to 1-to-k binary encoding vectors 

    def oneHotEncColumns(rddx: org.apache.spark.rdd.RDD[Array[String]], idx: Int):org.apache.spark.rdd.RDD[Array[Double]] = {
      val categories = rddx.map(r => r(idx)).distinct.zipWithIndex.collectAsMap
      //print(hdr(idx) + " : ")
      //println(categories)
      val numCategories = categories.size
      val vetcateg = rddx.map(r => {
        val categoryIdx = categories(r(idx)).toInt
        val categoryFeatures = if (numCategories > 2) Array.ofDim[Double](numCategories - 1) else Array.ofDim[Double](1)
        if (numCategories > 2) { 
          if (categoryIdx > 0) categoryFeatures(categoryIdx - 1) = 1.0
        }
        else categoryFeatures(0) = categoryIdx
        categoryFeatures
        })
      vetcateg
    }

    def mergeArray(rddx: org.apache.spark.rdd.RDD[Array[String]], idx: Int*):org.apache.spark.rdd.RDD[Array[Double]] = {
      var i = 0
      var arr1 = oneHotEncColumns(rddx,idx(i))
      for (j <- 1 until idx.size) {
        var arr2 = oneHotEncColumns(rddx,idx(j))
        var flt1 = arr1.zip(arr2).map(x => (x._1.toList ++ x._2.toList).toArray)
        arr1 = flt1
      }
      arr1
    }

    val concat = mergeArray(rdd,1,3,5,6,8,9,13)
					 
    val categ_label = rdd.map(x => x(14)).distinct.zipWithIndex.collectAsMap

    val rdd1 = rdd.map(x => Array(x(0),x(1).replace("?","Private"),x(2),x(3),x(4),x(5),x(6).replace("?","Prof-specialty"),
                              x(7),x(8),x(9),x(10),x(11),x(12),x(13).replace("?","United-States"),x(14)))

    val rdd2 = rdd1.map(x => {
      val y = Array(x(0),x(2),x(10),x(11),x(12),categ_label(x(14)))
      y.map(  z => z.toString.toDouble)
    })

    val vect = concat.zip(rdd2).map(x => (x._1.toList ++ x._2.toList).toArray)

    // Splitting dataset as train/test sets  --------------

    import org.apache.spark.mllib.linalg.Vectors
    import org.apache.spark.mllib.regression.LabeledPoint

    val data = vect.map(x => {
       val arr_size = x.size - 1
       val l = x(arr_size)
       val f = x.slice(0, arr_size)
       LabeledPoint(l, Vectors.dense(f))
     })

    val sets = data.randomSplit(Array(0.7,0.3), 11L)
    val trainSet = sets(0)
    val testSet = sets(1)

    // Standardizing features ------------------------------

    import org.apache.spark.mllib.feature.StandardScaler
    val vectors = trainSet.map(lp => lp.features)
    val scaler = new StandardScaler(withMean = true, withStd = true).fit(vectors)

    val trainScaled = trainSet.map(lp => LabeledPoint(lp.label,scaler.transform(lp.features)))
    val testScaled = testSet.map(lp => LabeledPoint(lp.label,scaler.transform(lp.features)))

    //trainScaled.cache
    //testScaled.cache
    sc.setCheckpointDir("/root/checkpt")
    trainScaled.checkpoint()

    // MLlib logistic regression STD --------------

    import org.apache.spark.mllib.classification.LogisticRegressionWithSGD

    val model = new LogisticRegressionWithSGD
    model.optimizer.setNumIterations(100).setRegParam(0.01)
    val lr = model.run(trainScaled)

    val validPredicts = testScaled.map(x => (lr.predict(x.features),x.label))

    import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
    val metrics = new BinaryClassificationMetrics(validPredicts)

    println("areaUnderPR = ", metrics.areaUnderPR)

    println("areaUnderROC = ", metrics.areaUnderROC)

    import org.apache.spark.mllib.evaluation.MulticlassMetrics
    val metrics1 = new MulticlassMetrics(validPredicts)

    println("accuracy = ", metrics1.accuracy)

    println("confusionMatrix = ")
    println(metrics1.confusionMatrix)

  }
}

