import spinal.core._
import spinal.lib._
import spinal.core.sim._
import spinal.sim._

class Mnist extends Component {
    val OUT_DW = 8
    val io = new Bundle {
        val inp = in (Flow(Vec(SInt(8 bits),1)))
        val oup = out (Flow(Vec(SInt(18 bits),10)))
    }

    val inp = FM(8,28,28,1) simPublic()
    inp.fm.valid := io.inp.valid
    inp.fm.payload := io.inp.payload
    
    val conv1_weight_path = "./hw/data/conv1_weight.bin"
    val conv1_scale = 20697
    val conv1_shift = 9
    val conv2_weight_path = "./hw/data/conv2_weight.bin"
    val conv2_scale = 28089
    val conv2_shift = 9
    val conv3_weight_path = "./hw/data/conv3_weight.bin"
    val conv3_scale = 28089
    val conv3_shift = 9

    val l1 = conv2d(inp,1,8,3,1,0,1,false,OUT_DW,conv1_weight_path,conv1_scale,conv1_shift) simPublic()
    val p1 = maxpool2d(l1,8,2,2,0,1) simPublic()
    val l2 = conv2d(p1,8,8,3,1,0,1,false,OUT_DW,conv2_weight_path,conv2_scale,conv2_shift) simPublic()
    val p2 = maxpool2d(l2,8,2,2,0,1) simPublic()
    val f3 = fc(p2,8,25,10,18,conv3_weight_path,conv3_scale,conv3_shift) simPublic()

    io.oup := f3.fm

}

object MnistTop extends App {
    utils.Config.spinal.generateVerilog(new Mnist)
}