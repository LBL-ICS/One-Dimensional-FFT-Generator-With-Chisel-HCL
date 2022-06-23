package ChiselFFT
import IEEEConversions.FPConvert._
import FloatingPointDesigns.FPArithmetic._
import ComplexModules.FPComplex._
import chisel3.tester.RawTester.test
import chisel3._
import chisel3.tester._
import ChiselFFT.FFTDesigns._
import implementation.FFT._
import implementation.ComplexNumbers._
import java.io.PrintWriter
import scala.util.Random
import GoldenModels.FFT_GoldenModels._

object Testing {
  def main(args: Array[String]): Unit = {
//    val N = 16
//    val r = 4
//    val base_r = 2
//    val w = 4
//    val ptype = 1
//    val bw = 32
//    test(new PermutationsWithStreaming(N,r,base_r,w,ptype,bw)) { c =>
//      c.io.in_en.poke(true.B)
//      c.clock.step(1)
//      for (j <- 0 until N / w) {
//        println(s"clock: ${c.io.out_t.peek().litValue}")
//        for (i <- 0 until w) {
//          c.io.in(i).Re.poke(convert_string_to_IEEE_754((j * w +i).toDouble.toString, 32).U)
//          c.io.in(i).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        }
//        c.clock.step(1)
//      }
//      println(s"clock: ${c.io.out_t.peek().litValue}")
//      c.clock.step(N/w)
//      println(s"clock: ${c.io.out_t.peek().litValue}")
//      for(j <- 0 until N/w) {
//        for(i <- 0 until w){
//          println(s"Real Output: ${convert_long_to_float(c.io.out(i).Re.peek().litValue, 32)}")
//          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(i).Im.peek().litValue, 32)}")
//        }
//        c.clock.step(1)
//        println(s"clock: ${c.io.out_t.peek().litValue}")
//      }
//    }
////    test(new Streaming_Permute_Config(8,2,2,1,2)){c=>
////      for(i <- 0 until 8/2) {
////        for(j <- 0 until 2){
////          println(s"output${j}: ${c.io.out(j).peek().litValue}")
////        }
////        c.clock.step(1)
////      }
////    }
//    test(new RAM_Block(4,2,32)){c=>
//      c.io.en.poke(true.B)
//      c.io.re.poke(true.B)
//      c.io.wr.poke(true.B)
//      c.io.in_data.Re.poke(1.U)
//      c.io.in_data.Im.poke(2.U)
//      c.io.in_raddr.poke(3.U)
//      c.io.in_waddr.poke(2.U)
//      c.clock.step(1)
//      println(s"output: ${c.io.out_data.Re.peek().litValue}")
//      c.io.in_raddr.poke(2.U)
//      c.clock.step(1)
//      println(s"output: ${c.io.out_data.Im.peek().litValue}")
//    }
//    test(new FPComplexAdder(32)){c=>
//      c.io.in_a.Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in_a.Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in_b.Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in_b.Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.clock.step(1)
//      println(s"Real Output: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, 32)}")
//    }
//
//    test(new FPComplexMult(32)){c=>
//      c.io.in_a.Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in_a.Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in_b.Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in_b.Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, 32)}")
//    }
//
//    test(new FPComplexMultiAdder(4,32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out.Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out.Im.peek().litValue, 32)}")
//    }
//    println("--------")
//    test(new DFT_r(4, 32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("2.9", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.984", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.clock.step(4)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//    }
//    println("--------")
//    test(new TwiddleFactors(4,2,2,0,32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("1.4", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("2.4", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("2.9", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("3.4", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("4.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("4.4", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//    }
//    println("--------")
//    test(new PermutationsBasic(8,2,1,32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(4).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
//      c.io.in(4).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(5).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
//      c.io.in(5).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(6).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
//      c.io.in(6).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(7).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
//      c.io.in(7).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
//    }
    println("--------------------")
//    val N = 8
//    val r = 2
//    val w = 8
//    val bw = 32
//    val runs = 62500
//    genRandom(N*2*runs,"TestInputs", bw)

//    val DFTs_per_stage = N/r
//    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
//    val CMultLatency = 2
//    val CAddLatency = 1
//    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
//    val Twid_latency = (N/w) * CMultLatency
//    val Perm_latency = 0
//    val Total_Latency = (number_of_stages - 1) * Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency
//
//    val golden_input = scala.io.Source.fromFile("test.txt").getLines().map(x=>x.toDouble).toArray
//
//    val cmplx_inputs = (for(j <- 0 until runs)yield{
//      val inp = (for(i <- 0 until (2*N) by 2) yield{
//        cmplx(golden_input(i),golden_input(i+1))
//      }).toArray
//      inp
//    }).toArray
//
//    val sw_model = FFT_r_GoldenModel(N, r, cmplx_inputs)

//    test(new FFT_sr(N,r,w,bw)){c=>
//      val input_List = scala.io.Source.fromFile("test.txt").getLines().map(x=>convert_string_to_IEEE_754(x, bw)).toArray
//      var hw_model = (for(i <- 0 until runs)yield{
//        val t = (for(j <- 0 until N)yield{
//          cmplx(0,0)
//        }).toArray
//        t
//      }).toArray
//      for(j <- 0 until runs) {
//        var index = 0
//        for (i <- 0 until (2*N) by 2) {
//          c.io.in(index).Re.poke(input_List(i).U)
//          c.io.in(index).Im.poke(input_List(i+1).U)
//          index += 1
//        }
//        c.clock.step(Total_Latency)
//        for(i <- 0 until N){
//          val val1 = convert_long_to_float(c.io.out(i).Re.peek().litValue, bw).toDouble
//          val val2 = convert_long_to_float(c.io.out(i).Im.peek().litValue, bw).toDouble
//          println(s"Real Output: ${val1}")
//          println(s"Imaginary Output: ${val2}")
//          hw_model(j)(i) = cmplx(val1,val2)
//        }
//      }
//      sw_model.map(x=>x.map(y=>y.print_mag))
//      println()
//      hw_model.map(x=>x.map(y=>y.print_mag))
//    }
    val pw = new PrintWriter("FFT_sr.v")
    pw.println(getVerilogString(new FFT_sr(9/*N inputs*/,3/*radix*/,9/*width*/,32/*bit width precision*/)))
    pw.close()
  }
}
