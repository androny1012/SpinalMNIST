import spinal.core._
import spinal.lib._
import spinal.sim._
import spinal.core.sim._

import java.io._

object ConvSim {
    def main(args : Array[String]) {
        val writer1 = new PrintWriter(new File("res1.txt" ))
        val (mat,label) = LoadMnist()
        var successCnt = 0
        var delay = 20000//18432

        val test_num = 10000
        SimConfig
            // .withWave
            // .withFstWave
            .withConfig(SpinalConfig(
                defaultClockDomainFrequency = FixedFrequency(100 MHz),
                defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)))
            .doSim(new Mnist()){dut =>//.withWave
            //Fork a process to generate the reset and the clock on the dut
            dut.clockDomain.forkStimulus(period = 10)

            dut.io.inp.valid #= false
            dut.clockDomain.waitSampling(10)

            for(num <- 0 until test_num+10) {
                for(i <- 0 until 28) {
                    for(j <- 0 until 28) {
                        if(num >= 0 && num < test_num ) {
                            dut.io.inp.valid #= true
                            // dut.io.inp.payload #= ((j+28*i)%128).toInt
                            for(c <- 0 until 1) {
                                dut.io.inp.payload(c) #= (mat(num)(c)(i)(j)).toInt
                            }
                        }else {
                            dut.io.inp.valid #= false
                            for(c <- 0 until 1) {
                                dut.io.inp.payload(c) #= 0
                            }
                        }

                        dut.clockDomain.waitRisingEdge()

                        if(dut.io.oup.valid.toBoolean == true) {
                            // println(dut.io.oup.payload(0).toInt)

                            // writer1.print(dut.io.oup.payload(0).toInt)
                            // writer1.print("  ")
                            // oCnt = oCnt + 1
                            // if(oCnt == 5){
                            //     writer1.print("\n")
                            //     oCnt = 0
                            // }

                            // for(o_num <- 0 until 10) {
                            //     writer1.print(dut.io.oup.payload(o_num).toInt)
                            //     writer1.print("\n")
                            // }

                            var max = dut.io.oup.payload(0).toInt
                            var index = 0
                            for(j <- 0 until 10) {
                                if(max < dut.io.oup.payload(j).toInt) {
                                    max = dut.io.oup.payload(j).toInt
                                    index = j
                                }
                            }
                            // println(num,index)
                            // println(num,f"${label(num).toInt}")

                            if(index == label(num).toInt) {
                                successCnt = successCnt + 1
                            }
                            if((num+1)%100 == 0) {
                                print(successCnt + "/" + (num+1))
                                println("  " + ((successCnt.toDouble/(num.toDouble+1.0)).toDouble)*100 + "%")
                            } 
                                              
                        }
                    }
                }
            }
            print(successCnt + "/" + test_num)
            println("  " + ((successCnt.toDouble/test_num.toDouble).toDouble)*100 + "%")
        }
        writer1.close()
    }
}