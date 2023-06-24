import java.io.{File, FileInputStream}

object LoadMnist {
  def apply():(Array[Array[Array[Array[Int]]]],Array[Int]) = {
    val num = 10000

    val mat = Array.ofDim[Int](num,1,28,28)
    val label = new Array[Int](num)

    val file = new File("./hw/data/mnist.bin")
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)

    for(k <- 0 until num) {
      for(i <- 0 until 1) {
        for(j <- 0 until 28) {
          for(m <- 0 until 28) {
            val t = k*784+i*28*28+j*28+m
            if(bytes(t)<0) {
              mat(k)(i)(j)(m) = 256 + bytes(t).toInt
            }else {
              mat(k)(i)(j)(m) = bytes(t).toInt
            }
            // mat(k)(i)(j)(m) = (mat(k)(i)(j)(m)/2)
          }
        }
      }
    }
    //val f2 = Figure()
    //f2.subplot(0) += image(mat)
    in.close()

    val file1 = new File("/home/anne/spinal/SpinalTemplateSbt/sw/mnist_label.bin")
    val in1 = new FileInputStream(file1)
    val bytes1 = new Array[Byte](file1.length.toInt)
    in1.read(bytes1)

    for(k <- 0 until num) {
        label(k) = bytes1(k).toInt
    }
    in1.close()

    (mat,label)
  }
}