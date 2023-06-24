import java.io.{File, FileInputStream, PrintWriter}
import Net._

case class lenet() extends Net {
    var x = NetConfig()
    x = conv2d(x,3,16,3,1,1)
    x = BatchNorm(x,16)
    x = relu(x)
    x = avgpool(x,8,1)
    x = conv2d(x,64,10,1)
    x = BatchNorm(x,10)
    x = relu(x)
    x = avgpool(x,8,1)
}

object GoldenLenet {
    def main(args : Array[String]) {
    
        val (mat,label) = LoadMnist()
        val w1_shape = Array[Int](8, 1, 3, 3)
        val w1 = LoadWeightMNIST("./hw/data/conv1_weight.bin",w1_shape)

        val w2_shape = Array[Int](8, 8, 3, 3)
        val w2 = LoadWeightMNIST("./hw/data/conv2_weight.bin",w2_shape)

        val w3_shape = Array[Int](10, 200, 1, 1)
        val w3 = LoadWeightMNIST("./hw/data/conv3_weight.bin",w3_shape)

        var suc = 0
        val test_num = 10000
        for(image_index <- 0 until test_num) {

            var l1 = Golden.conv2d(mat(image_index),1,8,3,w1,1,0)
            var r1 = Golden.relu(l1)
            val p1 = Golden.MaxPool(r1,2,2,0)

            var l2 = Golden.conv2d(p1,8,8,3,w2,1,0)
            var r2 = Golden.relu(l2)
            val p2 = Golden.MaxPool(r2,2,2,0)

            var f3 = Golden.Flatten(p2)

            // for(c <- 0 until 200) {
            //     print(f" ${f3(c)(0)(0).toInt}%d")
            // }
                
            var l3 = Golden.conv2d(f3,200,10,1,w3,1,0)

            var max = l3(0)(0)(0)
            var index = 0
            for(j <- 0 until 10) {
                if(max < l3(j)(0)(0)) {
                    max = l3(j)(0)(0)
                    index = j
                }
            }
            // println(index)
            // println(f"${label(image_index).toInt}")
        
            if(index == label(image_index)) {
                suc = suc + 1
            }
            if(image_index%100 == 99) {
                println(suc+";"+(image_index+1))
            }
        }
        println(suc)
    }
}

        
   