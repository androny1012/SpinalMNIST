import spinal.core._
import spinal.lib._
import spinal.sim._
import spinal.core.sim._

object conv2d {
    def apply(
        inp : FM, 
        input_channel : Int, 
        output_channel : Int, 
        kernel_size : Int = 3, 
        stride : Int = 1, 
        padding : Int = 0, 
        group : Int = 1, 
        bias: Boolean = false,
        OUT_DW : Int = 8,
        WeightFile : String,
        Scale : Int, 
        Shift : Int): FM = {

            println(":conv2d("+input_channel+","+output_channel+","+kernel_size+","+stride+","+padding+")"+" ("+((inp.W+2*padding-kernel_size)/stride+1)+","+((inp.H+2*padding-kernel_size)/stride+1)+")")
            val Win = inp.W
            val Hin = inp.H
            val Wout = (Win+2*padding-kernel_size)/stride+1
            val Hout = (Hin+2*padding-kernel_size)/stride+1
            val l = new Conv(input_channel,output_channel,kernel_size,stride,padding,group,Win, Hin,OUT_DW,WeightFile,Scale,Shift)
            l.io.inp := inp.fm
            val oup = FM(OUT_DW,Wout,Hout,output_channel)
            oup.fm := l.io.oup
            oup

        }
}

class Conv(
    Chin        : Int,
    Chout       : Int,
    kernel_size : Int,
    stride      : Int,
    padding     : Int,
    group       : Int = 1,
    Win         : Int,
    Hin         : Int,
    OUT_DW      : Int,
    WeightFile  : String,
    Scale       : Int, 
    Shift       : Int
) extends Component {

    val Wout = (Win + 2 * padding - kernel_size) / stride + 1
    val Hout = (Hin + 2 * padding - kernel_size) / stride + 1

    val io = new Bundle {
        val inp = in (Flow(Vec(SInt(8 bits),Chin)))
        val oup = out (Flow(Vec(SInt(OUT_DW bits),Chout)))
    } simPublic()

    val conv_weight = Vec(Vec(Vec(Reg(SInt(8 bits)) init(0),kernel_size*kernel_size),Chin),Chout)

    // println(conv_weight.getClass.getSimpleName)

    val w_shapes = Array[Int](Chout, Chin, kernel_size, kernel_size)
    val w = LoadWeightMNIST(WeightFile,w_shapes)

    for(k <- 0 until Chout) {
        for(i <- 0 until Chin) {
            for(j <- 0 until kernel_size) {
                for(m <- 0 until kernel_size) {
                    conv_weight(k)(i)(j*kernel_size+m) := w(k)(i)(j)(m)
                }
            }
        }
    }

    val linebuffer1 = Vec(Vec(Reg(SInt(8 bits)) init(0),Win),Chin) simPublic()
    val linebuffer2 = Vec(Vec(Reg(SInt(8 bits)) init(0),Win),Chin) simPublic()
    val linebuffer3 = Vec(Vec(Reg(SInt(8 bits)) init(0),Win),Chin) simPublic()
    for(chi <- 0 until Chin) {
        when(io.inp.valid) {
            linebuffer1(chi)(0) := io.inp.payload(chi)
            linebuffer2(chi)(0) := linebuffer1(chi)(Win-1)
            linebuffer3(chi)(0) := linebuffer2(chi)(Win-1)
            for(i <- 1 until Win) {
                linebuffer1(chi)(i) := linebuffer1(chi)(i-1)
                linebuffer2(chi)(i) := linebuffer2(chi)(i-1)
                linebuffer3(chi)(i) := linebuffer3(chi)(i-1)
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

    val mul = Vec(Vec(Vec(SInt(18 bits) ,kernel_size*kernel_size),Chout),Chin) simPublic()
    val win3x3 = Vec(Vec(SInt(8 bits) ,kernel_size*kernel_size),Chin) simPublic()

    for(chi <- 0 until Chin) {
        win3x3(chi)(8) := linebuffer1(chi)(0)
        win3x3(chi)(7) := linebuffer1(chi)(1)
        win3x3(chi)(6) := linebuffer1(chi)(2)
        win3x3(chi)(5) := linebuffer2(chi)(0)
        win3x3(chi)(4) := linebuffer2(chi)(1)
        win3x3(chi)(3) := linebuffer2(chi)(2)
        win3x3(chi)(2) := linebuffer3(chi)(0)
        win3x3(chi)(1) := linebuffer3(chi)(1)
        win3x3(chi)(0) := linebuffer3(chi)(2)
    }

    for(chi <- 0 until Chin) {
        for(cho <- 0 until Chout) {
            for(i <- 0 until kernel_size*kernel_size) {
                mul(chi)(cho)(i) := (win3x3(chi)(i) * conv_weight(cho)(chi)(i)).resized
            }
        }
    }

    val kernel_adder = Vec(Vec(Reg(SInt(18 bits)) init(0),Chin),Chout) simPublic()
    val chin_adder = Vec(SInt(18 bits), Chout) simPublic()
    // val chin_adder = Vec(Reg(SInt(18 bits)) init(0), Chout) simPublic()

    val out_delay = Vec(SInt(17 bits),Chout)
    val quant_scale = Vec(SInt(32 bits),Chout)
    val quant_shift = Vec(SInt(16 bits),Chout)
    val quant_out = Vec(SInt(8 bits),Chout)

    val out_vaild = Reg(Bool()) init(False) simPublic()
    // val out_vaild_delay = Reg(Bool()) init(False) simPublic()

    when(io.inp.valid && row >= 2 && col >= 2) {
        out_vaild := True
    }.otherwise {
        out_vaild := False
    }

    for(cho <- 0 until Chout) {
        for(chi <- 0 until Chin) {
            kernel_adder(cho)(chi) :=  mul(chi)(cho).reduceBalancedTree(_ + _)
        }
    }
    for(cho <- 0 until Chout) {
        chin_adder(cho) := kernel_adder(cho).reduceBalancedTree(_ + _)
    }
    // when(io.inp.valid && row >= 2 && col >= 2) {
    //     for(cho <- 0 until Chout) {
    //         chin_adder(cho) := kernel_adder(cho).reduceBalancedTree(_ + _)
    //     }
    // }.otherwise {
    //     for(cho <- 0 until Chout) {
    //         chin_adder(cho) := 0
    //     }
    // }

    for(cho <- 0 until Chout) {
        // io.oup.payload(cho) := addtree_out(cho)
        when(chin_adder(cho) < 0){
            out_delay(cho) := 0
            quant_scale(cho) := 0
            quant_shift(cho) := 0
            quant_out(cho) := 0
            // io.oup.payload(cho) := 0
        }.otherwise{
            out_delay(cho) := chin_adder(cho).sat(1)
            quant_scale(cho) := (out_delay(cho) * Scale).resized
            quant_shift(cho) := (quant_scale(cho) >> (15 + Shift)).resized
            quant_out(cho) := quant_shift(cho).sat(8)

            // out_delay(cho) := (addtree_out(0)(cho) >> 8).resized
            // io.oup.payload(cho) := (addtree_out(0)(cho) >> 8).resized
        }
        io.oup.payload(cho) := quant_out(cho)
    }
    
    io.oup.valid := Delay(out_vaild,1) 

}
