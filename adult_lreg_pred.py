
from pyspark.sql import SparkSession

spark = SparkSession.builder \
    .appName("Iniciando com Spark") \
    .getOrCreate()

spark.conf.set("spark.sql.shuffle.partitions",10)

df = spark.read.format("csv").option("inferSchema","true").load("hdfs://hdpmst:9000/data/adult.data").toDF("age","workclass","fnlwgt","education","education-num","marital_status","occupation","relationship","race","sex","capital_gain","capital_loss","hours_per_week","native_country","income")

from pyspark.ml.feature import SQLTransformer

sql = SQLTransformer().setStatement("""
SELECT age,
   case when workclass like "%?%" then "Private" else workclass end as workclass,
   fnlwgt,education,marital_status,
   case when occupation like "%?%" then "Prof-specialty" else occupation end as occupation,
   relationship,race,sex,capital_gain,capital_loss,hours_per_week,
   case when native_country like "%?%" then "United-States" else native_country end as native_country,
   income
FROM __THIS__""")

from pyspark.ml.feature import RFormula
rf = RFormula().setFormula("income ~ .")

from pyspark.ml.feature import StandardScaler
stdScaler = StandardScaler().\
setWithStd(True).\
setWithMean(True).\
setInputCol("features").\
setOutputCol("scaledFeatures")

from pyspark.ml.classification import LogisticRegression
lr = LogisticRegression().setFeaturesCol("scaledFeatures")

trainingData, testData = df.randomSplit([0.7,0.3],11L)

from pyspark.ml import Pipeline
pipeline = Pipeline().setStages([sql,rf,stdScaler,lr])
pipelinemodel = pipeline.fit(trainingData)

pred = pipelinemodel.transform(testData)

pred.groupBy("prediction","label").count().show()
