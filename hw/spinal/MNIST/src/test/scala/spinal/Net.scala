package Net

case class NetConfig() {
  var weightList = List[List[Int]]()
}

trait Net {
  def conv2d(x : NetConfig, input_channel : Int, output_channel : Int, kernel_size : Int, stride : Int = 1, padding : Int = 0, bias: Boolean = false): NetConfig = {
    x.weightList = x.weightList :+ List((input_channel * output_channel * kernel_size * kernel_size))
    x
  }
  def adder2d(x : NetConfig, input_channel : Int, output_channel : Int, kernel_size : Int, stride : Int = 1, padding : Int = 0, bias: Boolean = false): NetConfig = {
    x.weightList = x.weightList :+ List((input_channel * output_channel * kernel_size * kernel_size))
    x
  }
  def BatchNorm(x : NetConfig, input_channel : Int): NetConfig = {
    x.weightList = x.weightList :+ List(input_channel,input_channel,input_channel,input_channel,1)
    x
  }
  def relu(x : NetConfig): NetConfig = {
    x
  }
  def avgpool(x : NetConfig, kernel_size : Int, stride : Int = 1): NetConfig = {
    x
  }
}

object Golden {
  def conv2d(inp : Array[Array[Array[Int]]], ic : Int, oc : Int, kernel_size : Int, w : Array[Array[Array[Array[Int]]]], stride : Int = 1, padding : Int = 0): Array[Array[Array[Int]]] = {
    val wide = inp(0).length + padding*2
    val wide2 = inp(0)(0).length + padding*2
    var tinp = Array.ofDim[Int](ic,wide,wide2)
    if(padding == 1) {
      for(i <- 0 until ic) {
        for(j <- 0 until wide) {
          for(k <- 0 until wide2) {
            if(j == 0 || j == wide-1 || k == 0 || k == wide2-1) {
              tinp(i)(j)(k) = 0
            }else {
              tinp(i)(j)(k) = inp(i)(j-1)(k-1)
            }
          }
        }
      }
    }else {
      tinp = inp
    }
    val wideout = (inp(0).length+2*padding-kernel_size)/stride+1
    val wideout2 = (inp(0)(0).length+2*padding-kernel_size)/stride+1
    val oup = Array.ofDim[Int](oc,wideout,wideout)
    for(x <- 0 until wideout) {//calculate conv
      for(y <- 0 until wideout2) {
        for(i <- 0 until oc) {
          for(j <- 0 until ic) {
            for(m <- 0 until kernel_size) {
              for(n <- 0 until kernel_size) {
                // oup(i)(x)(y) = oup(i)(x)(y) + (tinp(j)(stride * x+m)(stride * y+n) * w(i*kernel_size*kernel_size*ic+j*kernel_size*kernel_size+m*kernel_size+n))
                oup(i)(x)(y) = oup(i)(x)(y) + (tinp(j)(stride * x+m)(stride * y+n) * w(i)(j)(m)(n))
              }
            }
          }
        }
      }
    }
    oup
  }
  def adder2d(inp : Array[Array[Array[Int]]], ic : Int, oc : Int, kernel_size : Int, w : Array[Int], stride : Int = 1, padding : Int = 1): Array[Array[Array[Int]]] = {
    val wide = inp(0).length + padding*2
    val wide2 = inp(0)(0).length + padding*2
    var tinp = Array.ofDim[Int](ic,wide,wide2)
    if(padding == 1) {
      for(i <- 0 until ic) {
        for(j <- 0 until wide) {
          for(k <- 0 until wide2) {
            if(j == 0 || j == wide-1 || k == 0 || k == wide-1) {
              tinp(i)(j)(k) = 0
            }else {
              tinp(i)(j)(k) = inp(i)(j-1)(k-1)
            }
          }
        }
      }
    }else {
      tinp = inp
    }
    val wideout = (inp(0).length+2*padding-kernel_size)/stride+1
    val wideout2 = (inp(0)(0).length+2*padding-kernel_size)/stride+1
    val oup = Array.ofDim[Int](oc,wideout,wideout)
    for(x <- 0 until wideout) {//calculate conv
      for(y <- 0 until wideout2) {
        for(i <- 0 until oc) {
          for(j <- 0 until ic) {
            for(m <- 0 until kernel_size) {
              for(n <- 0 until kernel_size) {
                oup(i)(x)(y) = oup(i)(x)(y) - (tinp(j)(stride * x+m)(stride * y+n) - w(i*kernel_size*kernel_size*ic+j*kernel_size*kernel_size+m*kernel_size+n)).abs
              }
            }
          }
        }
      }
    }
    oup
  }
  def BatchNorm(inp : Array[Array[Array[Int]]], weight : Array[Int]): Array[Array[Array[Int]]] = {
    val oup = inp.zipWithIndex.map{case (value,idx) => value.map(_.map(x => x * weight(idx) + weight(inp.length + idx)).toArray).toArray}.toArray
    oup
  }
  def relu(inp : Array[Array[Array[Int]]]): Array[Array[Array[Int]]] = {
    inp.map(_.map(_.map(xi => if(xi>=0)xi else 0)))
  }
  def AvgPool(inp : Array[Array[Array[Int]]], kernel_size : Int, stride : Int, padding : Int = 0): Array[Array[Array[Int]]] = {
    val wide = inp(0).length + padding*2
    val wide2 = inp(0)(0).length + padding*2
    var tinp = Array.ofDim[Int](inp.length,wide,wide2)
    if(padding == 1) {
      for(i <- 0 until inp.length) {
        for(j <- 0 until wide) {
          for(k <- 0 until wide2) {
            if(j == 0 || j == wide-1 || k == 0 || k == wide2-1) {
              tinp(i)(j)(k) = 0
            }else {
              tinp(i)(j)(k) = inp(i)(j-1)(k-1)
            }
          }
        }
      }
    }else {
      tinp = inp
    }
    val wideout = (inp(0).length+2*padding-kernel_size)/stride+1
    val wideout2 = (inp(0)(0).length+2*padding-kernel_size)/stride+1
    val oup = Array.ofDim[Int](inp.length,wideout,wideout2)
    for(c <- 0 until inp.length) {
      for(x <- 0 until wideout) {
        for(y <- 0 until wideout2) {
          var Isum : Int = 0
          for(m <- 0 until kernel_size) {
            for(n <- 0 until kernel_size) {
              Isum = Isum + tinp(c)(x+m)(y+n)
            }
          }
          oup(c)(x)(y) = Isum / kernel_size / kernel_size
        }
      }
    }
    oup
  }

    def MaxPool(inp : Array[Array[Array[Int]]], kernel_size : Int, stride : Int, padding : Int = 0): Array[Array[Array[Int]]] = {
        val wide = inp(0).length + padding*2
        val wide2 = inp(0)(0).length + padding*2
        var tinp = Array.ofDim[Int](inp.length,wide,wide2)
        if(padding == 1) {
        for(i <- 0 until inp.length) {
            for(j <- 0 until wide) {
                for(k <- 0 until wide2) {
                    if(j == 0 || j == wide-1 || k == 0 || k == wide2-1) {
                        tinp(i)(j)(k) = 0
                    }else {
                    tinp(i)(j)(k) = inp(i)(j-1)(k-1)
                    }
                }
            }
        }
        }else {
            tinp = inp
        }
        val wideout = (inp(0).length+2*padding-kernel_size)/stride+1
        val wideout2 = (inp(0)(0).length+2*padding-kernel_size)/stride+1
        val oup = Array.ofDim[Int](inp.length,wideout,wideout2)
        for(c <- 0 until inp.length) {
            for(x <- 0 until wideout) {
                for(y <- 0 until wideout2) {
                    var MaxPixel : Int = 0
                    for(m <- 0 until kernel_size) {
                        for(n <- 0 until kernel_size) {
                            if(tinp(c)(x*2+m)(y*2+n) > MaxPixel) {
                                MaxPixel = tinp(c)(x*2+m)(y*2+n)
                            }
                        }
                    }
                    oup(c)(x)(y) = (MaxPixel/256).toInt
                }
            }
        }
        oup
    }

    def Flatten(inp : Array[Array[Array[Int]]]): Array[Array[Array[Int]]] = {

        val wide = inp(0).length
        val wide2 = inp(0)(0).length
        val oup = Array.ofDim[Int](inp.length * wide * wide2,1,1)

        for(c <- 0 until inp.length) {
            for(x <- 0 until wide) {
                for(y <- 0 until wide2) {
                    oup(c*wide*wide2 + x * wide2 + y)(0)(0) = inp(c)(x)(y)
                }
            }
        }
        oup
    }

}