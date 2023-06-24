import spinal.core._
import spinal.lib._
import spinal.sim._
import spinal.core.sim._

object maxpool2d {
    def apply(
        inp : FM, 
        input_channel : Int, 
        kernel_size : Int = 2, 
        stride : Int = 2, 
        padding : Int = 0,
        group : Int = 1): FM = {

            val Win = inp.W
            val Hin = inp.H
            val Cin = inp.Channel
            val Wout = (Win+2*padding-kernel_size)/stride+1
            val Hout = (Hin+2*padding-kernel_size)/stride+1

            println(":MaxPool("+kernel_size+","+stride+","+padding+")"+" ("+Wout+","+Hout+")")

            val l = new MaxPool(Cin,kernel_size,stride,padding,group,Win, Hin)
            l.io.inp := inp.fm
            val oup = FM(inp.DW, Wout, Hout, Cin)
            oup.fm := l.io.oup
            oup
    }
}

class MaxPool(
    Chin  : Int,
    kernel_size : Int,
    stride : Int,
    padding : Int,
    group : Int = 1,
    Win        : Int,
    Hin        : Int
) extends Component {

    val Wout = (Win + 2 * padding - kernel_size) / stride + 1
    val Hout = (Hin + 2 * padding - kernel_size) / stride + 1

    val io = new Bundle {
        val inp = in (Flow(Vec(SInt(8 bits),Chin)))
        val oup = out (Flow(Vec(SInt(8 bits),Chin)))
    } simPublic()


    val linebuffer1 = Vec(Vec(Reg(SInt(8 bits)) init(0),Win),Chin) simPublic()
    val linebuffer2 = Vec(Vec(Reg(SInt(8 bits)) init(0),Win),Chin) simPublic()
    for(chi <- 0 until Chin) {
        when(io.inp.valid) {
            linebuffer1(chi)(0) := io.inp.payload(chi)
            linebuffer2(chi)(0) := linebuffer1(chi)(Win-1)
            for(i <- 1 until Win) {
                linebuffer1(chi)(i) := linebuffer1(chi)(i-1)
                linebuffer2(chi)(i) := linebuffer2(chi)(i-1)
            }
        }
    }

    val col = Reg(UInt(5 bits)) init(0) simPublic()
    when(io.inp.valid) {
        when(col < Win-1) {
            col := col + 1
        }.otherwise {
            col := 0
        }
    }

    val row = Reg(UInt(5 bits)) init(0) simPublic()
    when(io.inp.valid && col === Win-1) {
        when(row < Hin - 1) {
            row := row + 1
        }.otherwise {
            row := 0
        }
    }

    val win2x2 = Vec(Vec(SInt(8 bits) ,kernel_size*kernel_size),Chin) simPublic()

    for(chi <- 0 until Chin) {
        win2x2(chi)(3) := linebuffer1(chi)(0)
        win2x2(chi)(2) := linebuffer1(chi)(1)
        win2x2(chi)(1) := linebuffer2(chi)(0)
        win2x2(chi)(0) := linebuffer2(chi)(1)
    }

    val max = Vec(Reg(SInt(8 bits)) init(0), Chin) simPublic()
    val max1 = Vec(SInt(8 bits),Chin) simPublic()
    val max2 = Vec(SInt(8 bits),Chin) simPublic()
    for(chi <- 0 until Chin) {
        when(win2x2(chi)(3) > win2x2(chi)(2)){  
            max1(chi) := win2x2(chi)(3)
        }.otherwise {
            max1(chi) := win2x2(chi)(2)
        }
        when(win2x2(chi)(1) > win2x2(chi)(0)){  
            max2(chi) := win2x2(chi)(1)
        }.otherwise {
            max2(chi) := win2x2(chi)(0)
        }
        when(max2(chi) > max1(chi)){  
            max(chi) := max2(chi)
        }.otherwise {
            max(chi) := max1(chi)
        }
    }


    val pool_out = Vec(Reg(SInt(8 bits)) init(0),Chin) simPublic()
    val out_vaild = Reg(Bool()) init(False) simPublic()

    for(chi <- 0 until Chin) {
        pool_out(chi) := max(chi)
    }
        
    when(io.inp.valid && row >= 1 && col >= 1 && row%2 === 1 && col%2 === 1) {
        out_vaild := True
    }.otherwise {
        out_vaild := False
    }

    for(chi <- 0 until Chin) {
        io.oup.payload(chi) := pool_out(chi)
    }
    
    io.oup.valid := Delay(out_vaild,2)

}
