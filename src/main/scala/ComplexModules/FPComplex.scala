package ComplexModules
import Chisel.log2Ceil
import chisel3._
import FloatingPointDesigns.FPArithmetic._

object FPComplex {
  class ComplexNum(bw: Int) extends Bundle{
    val Re = UInt(bw.W)
    val Im = UInt(bw.W)
  }
  class ComplexIO(bw: Int) extends Bundle{
    val in_a = Input(new ComplexNum(bw))
    val in_b = Input(new ComplexNum(bw))
    val out_s = Output(new ComplexNum(bw))
  }
  class FPComplexAdder(bw: Int) extends Module{
    val io = IO(new ComplexIO(bw))
    val FP_adders = (for(i <- 0 until 2)yield{
      val fpadd = Module(new FP_adder(bw)).io
      fpadd
    }).toVector
    FP_adders(0).in_a := io.in_a.Re
    FP_adders(0).in_b := io.in_b.Re
    FP_adders(1).in_a := io.in_a.Im
    FP_adders(1).in_b := io.in_b.Im
    io.out_s.Re := FP_adders(0).out_s
    io.out_s.Im := FP_adders(1).out_s
  }
  class FPComplexSub(bw: Int) extends Module{
    val io = IO(new ComplexIO(bw))
    val FP_subbers = (for(i <- 0 until 2)yield{
      val fpsub = Module(new FP_subber(bw)).io
      fpsub
    }).toVector
    FP_subbers(0).in_a := io.in_a.Re
    FP_subbers(0).in_b := io.in_b.Re
    FP_subbers(1).in_a := io.in_a.Im
    FP_subbers(1).in_b := io.in_b.Im
    io.out_s.Re := FP_subbers(0).out_s
    io.out_s.Im := FP_subbers(1).out_s
  }
  class FPComplexMult(bw: Int) extends Module{
    val io = IO(new ComplexIO(bw))
    val FP_sub = Module(new FP_subber(bw)).io
    val FP_add = Module(new FP_adder(bw)).io
    val FP_multipliers = (for(i <- 0 until  4) yield{
      val fpmult = Module(new FP_multiplier(bw)).io
      fpmult
    }).toVector
    FP_multipliers(0).in_a := io.in_a.Re
    FP_multipliers(0).in_b := io.in_b.Re
    FP_multipliers(1).in_a := io.in_a.Im
    FP_multipliers(1).in_b := io.in_b.Im
    FP_multipliers(2).in_a := io.in_a.Re
    FP_multipliers(2).in_b := io.in_b.Im
    FP_multipliers(3).in_a := io.in_a.Im
    FP_multipliers(3).in_b := io.in_b.Re
    FP_sub.in_a := FP_multipliers(0).out_s
    FP_sub.in_b := FP_multipliers(1).out_s
    FP_add.in_a := FP_multipliers(2).out_s
    FP_add.in_b := FP_multipliers(3).out_s
    io.out_s.Re := FP_sub.out_s
    io.out_s.Im := FP_add.out_s
  }
  class FPComplexMultiAdder(n: Int,bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(n, new ComplexNum(bw)))
      val out = Output(new ComplexNum(bw))
    })
    val FP_adders = (for(i <- 0 until n-1) yield {
      val FP_adder = Module(new FPComplexAdder(bw)).io
      FP_adder
    }).toVector
    var count_FP_adder = 0 // keeps track of the floating point adder in use
    var inputcounter = 0 // keeps track of inputs into the floating point adders
    var tracker_sumCount = n // is keeping track of the number elements that need to be summed
    var odd = 0 // indicates if the number of elements we are trying to sum up is an odd number.
    var odd_inner = 0
    var offset = 0 //this is used to keep the counter synchronized when dealing with an odd number of summations in a layer
    var count_lastadder = n-2 // keeps track of position of the last adder relative to the current summation layer
    if(tracker_sumCount%2!=0){ // if summing an odd number of elements
      odd = 1
      FP_adders(count_lastadder).in_b := io.in(tracker_sumCount-1)
      count_lastadder -= 1
      offset += 1
    }
    for(i <- 0 until tracker_sumCount/2){ // add all the multiplication results using the first layer of FP-adders
      FP_adders(count_FP_adder).in_a := io.in(inputcounter)
      FP_adders(count_FP_adder).in_b := io.in(inputcounter+1)
      inputcounter += 2
      count_FP_adder += 1
    }
    offset = 0
    tracker_sumCount /= 2
    inputcounter = 0
    for(i <- 0 until log2Ceil(n) - 1){ // for all the adding stages that are needed to compute the full sum
      if(tracker_sumCount == 0 || tracker_sumCount == 1){ // if we have reached the end of the adders
        FP_adders(count_FP_adder).in_a := FP_adders(count_lastadder).out_s
        if(odd_inner > 0){// connect the rest of the unconnected modules
          for(i <- 0 until odd_inner){
            if(count_FP_adder < n - 2 - odd && n-2-odd > 0){
              FP_adders(count_FP_adder+1).in_a := FP_adders(count_FP_adder).out_s
              count_FP_adder+=1
            }
          }
        }
      }else if(tracker_sumCount%2!=0){ // if trying to sum up an odd number of results
        odd_inner+=1
        FP_adders(count_lastadder).in_b := FP_adders(inputcounter + tracker_sumCount-1).out_s
        count_lastadder -= 1
        offset += 1
      }
      for(i <- 0 until tracker_sumCount/2){ // add all the multiplication results using the next layer FP-adders
        FP_adders(count_FP_adder).in_a := FP_adders(inputcounter).out_s
        FP_adders(count_FP_adder).in_b := FP_adders(inputcounter+1).out_s
        inputcounter += 2
        count_FP_adder += 1
      }
      inputcounter += offset
      offset = 0
      tracker_sumCount /= 2
    }
    if(odd == 1 && n-2-odd > 0){ // connect remaining module
      FP_adders(if(n-2 > 0) n-2 else 0).in_a := FP_adders(if(n-2 > 0) n-2-1 else 0).out_s
    }
    io.out := FP_adders(n-2).out_s
  }
}
