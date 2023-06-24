import spinal.core._
import spinal.lib._

object FM {
    def apply(inp : FM): FM = {
        val ret = FM(inp.DW, inp.W, inp.H, inp.Channel)
        ret
    }
}

case class FM(
    DW : Int,
    W : Int,
    H : Int,
    Channel : Int,
) extends Bundle {

    val fm = Flow(Vec(SInt(DW bits),Channel))
    def setLayer(): FM = {
        val ret = FM(DW, W, H, Channel)
        ret.fm := fm
        ret
    }
}
