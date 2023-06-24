import spinal.core._
import spinal.lib._
import spinal.sim._
import spinal.core.sim._

object fc {
    def apply(
        inp : FM, 
        Chin : Int, 
        image_size : Int, 
        Chout : Int,
        OUT_DW : Int,
        WeightFile : String,
        Scale : Int, 
        Shift : Int): FM = {
            println(":fc("+Chin*image_size+","+Chout+")")
            val l = new FullConnect(Chin,image_size,Chout,OUT_DW,WeightFile,Scale,Shift)
            l.io.inp := inp.fm
            val oup = FM(OUT_DW,1,1,Chout)
            oup.fm := l.io.oup
            oup
        }
}

class FullConnect(
    Chin        : Int, 
    image_size  : Int, 
    Chout       : Int,
    OUT_DW      : Int,
    WeightFile  : String,
    Scale       : Int, 
    Shift       : Int
) extends Component {

    val io = new Bundle {
        val inp = in (Flow(Vec(SInt(8 bits),Chin)))
        val oup = out (Flow(Vec(SInt(OUT_DW bits),Chout)))
    } simPublic()

    val conv_weight = Vec(Vec(Reg(SInt(8 bits)) init(0),Chin * image_size),Chout)

    // println(conv_weight.getClass.getSimpleName)

    val w_shapes = Array[Int](Chout, Chin * image_size, 1, 1)
    val w = LoadWeightMNIST(WeightFile,w_shapes)

    for(k <- 0 until Chout) {
        for(i <- 0 until Chin * image_size) {
            conv_weight(k)(i) := w(k)(i)(0)(0)
        }
    }

    val input_cnt = Reg(UInt(8 bits)) init(0) simPublic()
    when(io.inp.valid) {
        when(input_cnt < image_size - 1) {
            input_cnt := input_cnt + 1
        }.otherwise {
            input_cnt := 0
        }
    }

    val partial_sum = Vec(Vec(Reg(SInt(18 bits)) init(0),Chin),Chout) simPublic()

    val fc_weight = Vec(Vec(SInt(8 bits),Chin),Chout) simPublic()
    val fc_weight_addr = Vec(UInt(8 bits),Chin) simPublic()

    for(chi <- 0 until Chin) {
        fc_weight_addr(chi) := (input_cnt + chi * image_size).resized
        for(cho <- 0 until Chout) {
            fc_weight(cho)(chi) := conv_weight(cho)(fc_weight_addr(chi))
            when(io.inp.valid && input_cnt === 0) {
                partial_sum(cho)(chi) := (io.inp.payload(chi) * fc_weight(cho)(chi)).resized
            }
            when(io.inp.valid) {
                partial_sum(cho)(chi) := partial_sum(cho)(chi) +(io.inp.payload(chi) * fc_weight(cho)(chi)).resized
            }

            when(io.oup.valid) {
                partial_sum(cho)(chi) := 0
            }
        }
    }

    val out_vaild = Reg(Bool()) init(False) simPublic()
    when(io.inp.valid && input_cnt === image_size - 1) {
        out_vaild := True
    }.otherwise {
        out_vaild := False
    }

    for(cho <- 0 until Chout) {
        io.oup.payload(cho) := partial_sum(cho).reduceBalancedTree(_ + _)
    }
    
    io.oup.valid := Delay(out_vaild,1) 

}
