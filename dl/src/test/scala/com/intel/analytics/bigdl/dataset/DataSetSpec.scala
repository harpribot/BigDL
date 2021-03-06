/*
 * Licensed to Intel Corporation under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Intel Corporation licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.dataset

import java.io.File
import java.nio.file.Paths

import com.intel.analytics.bigdl.dataset.image._
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.{Engine, RandomGenerator}
import org.apache.spark.SparkContext
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class DataSetSpec extends FlatSpec with Matchers with BeforeAndAfter {
  var sc: SparkContext = null

  before {
    val nodeNumber = 1
    val coreNumber = 1
    Engine.init(nodeNumber, coreNumber, true)
    sc = new SparkContext("local[1]", "DataSetSpec")
  }

  after {
    if (sc != null) {
      sc.stop()
    }
  }

  private def processPath(path: String): String = {
    if (path.contains(":")) {
      path.substring(1)
    } else {
      path
    }
  }

  "mnist data source" should "load image correct" in {
    val resource = getClass().getClassLoader().getResource("mnist")

    val dataSet = DataSet.array(com.intel.analytics.bigdl.models.lenet.Utils.load(
      Paths.get(processPath(resource.getPath()) + File.separator, "t10k-images.idx3-ubyte"),
      Paths.get(processPath(resource.getPath()) + File.separator, "t10k-labels.idx1-ubyte")
    ))
    dataSet.size() should be(10000)
    var iter = dataSet.data(train = false)
    iter.map(_.label).min should be(1.0f)
    iter = dataSet.data(train = false)
    iter.map(_.label).max should be(10.0f)
  }

  "mnist rdd data source" should "load image correct" in {
    val resource = getClass().getClassLoader().getResource("mnist")

    val dataSet = DataSet.array(com.intel.analytics.bigdl.models.lenet.Utils.load(
      Paths.get(processPath(resource.getPath()) + File.separator, "t10k-images.idx3-ubyte"),
      Paths.get(processPath(resource.getPath()) + File.separator, "t10k-labels.idx1-ubyte")
    ), sc)

    dataSet.size() should be(10000)
    var rdd = dataSet.data(train = false)
    rdd.map(_.label).min should be(1.0f)
    rdd = dataSet.data(train = false)
    rdd.map(_.label).max should be(10.0f)
  }

  "cifar data source" should "load image correct" in {
    val resource = getClass().getClassLoader().getResource("cifar")
    val dataSet = DataSet.ImageFolder.images(Paths.get(processPath(resource.getPath())),
      BGRImage.NO_SCALE)
    dataSet.size() should be(7)
    val labelMap = LocalImageFiles.readLabels(Paths.get(processPath(resource.getPath())))
    labelMap("airplane") should be(1)
    labelMap("deer") should be(2)

    val iter = dataSet.toLocal().data(train = false)
    val img1 = iter.next()
    img1.label() should be(1f)
    img1.content(2) should be(234 / 255f)
    img1.content(1) should be(125 / 255f)
    img1.content(0) should be(59 / 255f)
    img1.content((22 + 4 * 32) * 3 + 2) should be(253 / 255f)
    img1.content((22 + 4 * 32) * 3 + 1) should be(148 / 255f)
    img1.content((22 + 4 * 32) * 3) should be(31 / 255f)
    val img2 = iter.next()
    img2.label() should be(1f)
    val img3 = iter.next()
    img3.label() should be(2f)
    val img4 = iter.next()
    img4.label() should be(2f)
    img4.content((9 + 8 * 32) * 3 + 2) should be(40 / 255f)
    img4.content((9 + 8 * 32) * 3 + 1) should be(51 / 255f)
    img4.content((9 + 8 * 32) * 3) should be(37 / 255f)
    val img5 = iter.next()
    img5.label() should be(2f)
    val img6 = iter.next()
    img6.label() should be(2f)
    val img7 = iter.next()
    img7.label() should be(1f)
  }

  "cifar rdd data source" should "load image correct" in {
    val resource = getClass().getClassLoader().getResource("cifar")
    val dataSet = DataSet.ImageFolder.images(Paths.get(processPath(resource.getPath())),
      sc, BGRImage.NO_SCALE)
    dataSet.size() should be(7)
    val labelMap = LocalImageFiles.readLabels(Paths.get(processPath(resource.getPath())))
    labelMap("airplane") should be(1)
    labelMap("deer") should be(2)

    val rdd = dataSet.toDistributed().data(train = false)
    rdd.filter(_.label() == 1f).count() should be(3)
    rdd.filter(_.label() == 2f).count() should be(4)
    val images = rdd.map(_.clone())
      .filter(_.label() == 1f)
      .collect()
      .sortWith(_.content(0) < _.content(0))
    val img1 = images(1)
    img1.label() should be(1f)
    img1.content(2) should be(234 / 255f)
    img1.content(1) should be(125 / 255f)
    img1.content(0) should be(59 / 255f)
    img1.content((22 + 4 * 32) * 3 + 2) should be(253 / 255f)
    img1.content((22 + 4 * 32) * 3 + 1) should be(148 / 255f)
    img1.content((22 + 4 * 32) * 3) should be(31 / 255f)

    val images2 = rdd.map(_.clone())
      .filter(_.label() == 2)
      .collect()
      .sortWith(_.content(0) < _.content(0))
    val img4 = images2(0)
    img4.label() should be(2f)
    img4.content((9 + 8 * 32) * 3 + 2) should be(40 / 255f)
    img4.content((9 + 8 * 32) * 3 + 1) should be(51 / 255f)
    img4.content((9 + 8 * 32) * 3) should be(37 / 255f)
  }

  "imagenet data source" should "load image correct" in {
    val resource = getClass().getClassLoader().getResource("imagenet")
    val dataSet = DataSet.ImageFolder.paths(Paths.get(processPath(resource.getPath())))
    dataSet.size() should be(11)

    val labelMap = LocalImageFiles.readLabels(Paths.get(processPath(resource.getPath())))
    labelMap("n02110063") should be(1)
    labelMap("n04370456") should be(2)
    labelMap("n15075141") should be(3)
    labelMap("n99999999") should be(4)

    val pathToImage = LocalImgReader(BGRImage.NO_SCALE)
    val imageDataSet = dataSet -> pathToImage

    val images = imageDataSet.toLocal().data(train = false)
      .map(_.clone())
      .toArray
      .sortWith(_.content(0) < _.content(0))
    val labels = images.map(_.label())
    labels.mkString(",") should be("2.0,3.0,1.0,4.0,1.0,1.0,4.0,3.0,4.0,3.0,2.0")

    images(6).content((100 + 100 * 213) * 3 + 2) should be(35 / 255f)
    images(6).content((100 + 100 * 213) * 3 + 1) should be(30 / 255f)
    images(6).content((100 + 100 * 213) * 3) should be(36 / 255f)
    val path1 = java.io.File.createTempFile("UnitTest", "datasource1.jpg").getAbsolutePath
    images(6).save(path1)
    println(s"save test image to $path1")

    images(8).content((100 + 100 * 556) * 3 + 2) should be(24 / 255f)
    images(8).content((100 + 100 * 556) * 3 + 1) should be(24 / 255f)
    images(8).content((100 + 100 * 556) * 3) should be(24 / 255f)
    val path2 = java.io.File.createTempFile("UnitTest", "datasource2.jpg").getAbsolutePath
    images(8).save(path2)
    println(s"save test image to $path2")
  }

  "imagenet sequence data source" should "load image correct" in {
    val resource = getClass().getClassLoader().getResource("imagenet")
    val tmpFile = java.io.File.createTempFile("UnitTest", System.nanoTime().toString)
    require(tmpFile.delete())
    require(tmpFile.mkdir())

    // Convert the test imagenet files to seq files
    val files = (DataSet.ImageFolder.paths(Paths.get(processPath(resource.getPath())))
      -> LocalImgReader(BGRImage.NO_SCALE)
      -> BGRImgToLocalSeqFile(2, Paths.get(tmpFile.getAbsolutePath(), "imagenet"))
      ).toLocal().data(train = false).map(s => {
      println(s);
      s
    }).toArray

    files.length should be(6)

    val imageIter = (DataSet.SeqFileFolder.paths(Paths.get(tmpFile.getAbsolutePath()), 11)
      -> LocalSeqFileToBytes() -> BytesToBGRImg()).toLocal().data(train = false)

    val img = imageIter.next()
    img.label() should be(4f)
    img.content((100 + 100 * 213) * 3 + 2) should be(35 / 255f)
    img.content((100 + 100 * 213) * 3 + 1) should be(30 / 255f)
    img.content((100 + 100 * 213) * 3) should be(36 / 255f)
    imageIter.next()
    img.label() should be(4f)
    img.content((100 + 100 * 556) * 3 + 2) should be(24 / 255f)
    img.content((100 + 100 * 556) * 3 + 1) should be(24 / 255f)
    img.content((100 + 100 * 556) * 3) should be(24 / 255f)
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.next()
    imageIter.hasNext should be(false)
  }

  "image preprocess" should "be same with torch result" in {
    Engine.setNodeNumber(None)
    val resourceImageNet = getClass().getClassLoader().getResource("imagenet")
    def test(imgFolder: String, imgFileName: String, tensorFile: String): Unit = {
      val img1Path = Paths.get(processPath(resourceImageNet.getPath()), imgFolder, imgFileName)
      val iter = (DataSet.array(Array(LocalLabeledImagePath(1.0f, img1Path)))
        -> LocalImgReader()
        -> BGRImgCropper(224, 224)
        -> HFlip()
        -> BGRImgNormalizer((0.4, 0.5, 0.6), (0.1, 0.2, 0.3))
        -> BGRImgToBatch(1)
        ).toLocal().data(train = false)
      val image1 = iter.next().data

      val resourceTorch = getClass().getClassLoader().getResource("torch")
      val tensor1Path = Paths.get(processPath(resourceTorch.getPath()), tensorFile)
      val tensor1 = com.intel.analytics.bigdl.utils.File.loadTorch[Tensor[Float]](
        tensor1Path.toString).addSingletonDimension()
      image1.size() should be(tensor1.size())
      image1.map(tensor1, (a, b) => {
        a should be(b +- 0.0001f)
        b
      })
    }
    RandomGenerator.RNG.setSeed(100)
    test("n02110063", "n02110063_11239.JPEG", "n02110063_11239.t7")
    RandomGenerator.RNG.setSeed(100)
    test("n04370456", "n04370456_5753.JPEG", "n04370456_5753.t7")
    RandomGenerator.RNG.setSeed(100)
    test("n15075141", "n15075141_38508.JPEG", "n15075141_38508.t7")
    RandomGenerator.RNG.setSeed(100)
    test("n99999999", "n03000134_4970.JPEG", "n03000134_4970.t7")
  }

  "RDD from DataSet" should "give different position every time" in {
    val data = (1 to 4).toArray
    val trainRDD = DataSet.rdd(sc.parallelize(data, 1).mapPartitions(_ => {
      RandomGenerator.RNG.setSeed(100)
      (1 to 100).iterator
    })).data(train = true)
    trainRDD.mapPartitions(iter => {
      Iterator.single(iter.next())
    }).collect()(0) should be(22)

    trainRDD.mapPartitions(iter => {
      Iterator.single(iter.next())
    }).collect()(0) should be(41)

    trainRDD.mapPartitions(iter => {
      Iterator.single(iter.next())
    }).collect()(0) should be(62)

  }
}
