import java.io.{File, FileInputStream}

object LoadWeightMNIST {
    def apply(FileName : String, WeightShape :Array[Int] ):Array[Array[Array[Array[Int]]]] = {
    
    val w = Array.ofDim[Int](WeightShape(0),WeightShape(1),WeightShape(2),WeightShape(3))
    // val w = Array.ofDim[Int](8,1,3,3)

    val file = new File(FileName)
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)

    for(k <- 0 until WeightShape(0)) {
      for(i <- 0 until WeightShape(1)) {
        for(j <- 0 until WeightShape(2)) {
          for(m <- 0 until WeightShape(3)) {
            val t = k*WeightShape(3)*WeightShape(2)*WeightShape(1)+i*WeightShape(3)*WeightShape(2)+j*WeightShape(3)+m
            if(bytes(t)<0) {
              // w(k)(i)(j)(m) = 256 + bytes(t).toInt
              w(k)(i)(j)(m) = bytes(t).toInt
            }else {
              w(k)(i)(j)(m) = bytes(t).toInt
            }
          }
        }
      }
    }

    in.close()
    w
  }
}