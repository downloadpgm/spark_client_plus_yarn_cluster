# kafka sbt

To run, prepare a Spark environment having YARN/Standalone cluster manager

$ docker stack deploy -c docker-composer.yml kfk

1) download sbt
```shell
$ wget https://github.com/sbt/sbt/releases/download/v1.3.8/sbt-1.3.8.tgz
$ tar zxvf sbt-1.3.8.tgz
$ mv sbt /usr/local
$ export PATH=$PATH:/usr/local/sbt/bin
```

2) run sbt to prepare enviroment
```shell
$ sbt
```

3) create directory for build
```shell
$ mkdir app
$ cd app
$ # copy build.sbt and AdultPredML.scala
```

4) build and create jar
```shell
$ sbt package
$ cd ~
```

5) create pipelinemodel and save to HDFS (follow instructions on fit_lr_model_and_save.script)

6) run the package
```shell
$ spark-submit --master yarn --class AdultPredML.AdultPredML app/target/scala-2.11/adult-pred_2.11-1.0.0.jar
```