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
import implementation._
import GoldenModels.FFT_GoldenModels._

import java.io.PrintWriter
import scala.util.Random
import GoldenModels.FFT_GoldenModels._
import Chisel.{isPow2, log2Ceil}

object Testing {
  def main(args: Array[String]): Unit = {
    val N = 8
    val r = 2
    val base_r = 2
    val w = 8
    val ptype = 1
    val bw = 32
    val l2c = (log2Ceil((N/w)))

    val pw = new PrintWriter("DFT_r.v")
    pw.println(getVerilogString(new DFT_r(4, bw/*bit width precision*/)))
    pw.close()
    val pw2 = new PrintWriter("FFT_sr.v")
    pw2.println(getVerilogString(new FFT_sr(4,2,4,bw/*bit width precision*/)))
    pw2.close()
    genDFTInOutFile(N = 27,r = 3,bw = 32, runs = 100) // this is how to generate input/output files for testing

//    test(new FPComplexMult_reducable(bw, Math.sin(Math.PI/3), Math.cos(Math.PI/3))){c=>
//      c.io.in_a.Re.poke(convert_string_to_IEEE_754("1.4", bw).U)
//      c.io.in_a.Im.poke(convert_string_to_IEEE_754("2.3", bw).U)
//      c.io.in_b.Re.poke(convert_string_to_IEEE_754(Math.sin(Math.PI/3).toString, bw).U)
//      c.io.in_b.Im.poke(convert_string_to_IEEE_754(Math.cos(Math.PI/3).toString, bw).U)
//      c.clock.step(2)
//      println(s"Hardware Result Re: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, bw)}")
//      println(s"Hardware Result Im: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, bw)}")
//      val cmplx_mult = complex_mult(cmplx(1.4,2.3), cmplx(Math.sin(Math.PI/3), Math.cos(Math.PI/3)))
//      println(s"Software Result Re: ${cmplx_mult.re}")
//      println(s"Software Result Im: ${cmplx_mult.im}")
//    }
    //println(s"l2c: ${l2c}")
//    println(isReducable(1.0))
//    test(new PermutationsWithStreaming(N,r,base_r,w,ptype,bw)) { c =>
//      c.io.in_en.poke(true.B)
//      c.clock.step(1)
//      for (j <- 0 until N / w) {
//        println(s"clock: ${convert_long_to_float(c.io.out_t.Re.peek().litValue,bw)}")
//        for (i <- 0 until w) {
//          c.io.in(i).Re.poke(convert_string_to_IEEE_754((j * w +i).toDouble.toString, 32).U)
//          c.io.in(i).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        }
//        c.clock.step(1)
//      }
//      println(s"clock: ${convert_long_to_float(c.io.out_t.Re.peek().litValue,bw)}")
//      for(i <- 0 until N/w){
//        c.clock.step(1)
//        println(s"clock: ${convert_long_to_float(c.io.out_t.Re.peek().litValue,bw)}")
//      }
//
//      for(j <- 0 until N/w) {
//        for(i <- 0 until w){
//          println(s"Real Output: ${convert_long_to_float(c.io.out(i).Re.peek().litValue, 32)}")
//          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(i).Im.peek().litValue, 32)}")
//        }
//        c.clock.step(1)
//        println(s"clock: ${convert_long_to_float(c.io.out_t.Re.peek().litValue,bw)}")
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
    println("--------")
    test(new DFT_r(2, 32)){c=>
      val CMultLatency = 2
      val CAddLatency = 1
      val DFT_latency = CMultLatency + ((Math.log10(2)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(4)/Math.log10(2)).floor.toInt)yield{(4/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)
      println(DFT_latency)
      for(i <- 0 until 2) {
        c.io.in_ready.poke(true.B)
        c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
        c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
        c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
        c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
        for (i <- 0 until 1) {
          c.clock.step(1)
          println(s"Clock cycle ${i + 1}")
          println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
          println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
        }
      }
    }
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
println("-----------------------------------------------??????????????????")
    test(new FFT_sr(8,2,8,32)){c=>
      val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
      var mult_count = 0
      for(i <- 0 until r-1){
        for(j <- 0 until r-1){
          val n  = DFTr_Constants(i+1)(j+1)
          val c1 = FFT.isReducable(n.re.abs)
          val c2 = FFT.isReducable(n.im.abs)
          if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
            mult_count += 1
          }
        }
      }
      val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
      var T_L = 0
      val CMultLatency = 2
      val CAddLatency = 1
      for(i <- 0 until TotalStages) {
        val twid = Permutations.T2(N, r)(i)
        var mult_count2 = 0
        for (i <- 0 until w){
          val n = twid(i)
          val c1 = FFT.isReducable(n.re.abs)
          val c2 = FFT.isReducable(n.im.abs)
          if (!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
            mult_count2 += 1
          }
        }
        var T_latency = CMultLatency
        if (mult_count2 == 0) {
          T_latency = 0
        }
        println(mult_count2 + "yay")
        T_L += T_latency
      }
      val DFTs_per_stage = N/r
      val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
      var DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
      if(mult_count == 0){
        DFT_latency = ((Math.log10(r)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).floor.toInt)yield{(r/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)
      }
      val Twid_latency = (N/w) * CMultLatency
      val Perm_latency = 0
      val Total_Latency = T_L + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency
      println(Total_Latency)
      c.io.in_ready.poke(true.B)
      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(2).Re.poke(convert_string_to_IEEE_754("2.9", 32).U)
      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.984", 32).U)
      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(4).Re.poke(convert_string_to_IEEE_754("11.3", 32).U)
      c.io.in(4).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(5).Re.poke(convert_string_to_IEEE_754("-6.984", 32).U)
      c.io.in(5).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(6).Re.poke(convert_string_to_IEEE_754("1.9", 32).U)
      c.io.in(6).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(7).Re.poke(convert_string_to_IEEE_754("3.984", 32).U)
      c.io.in(7).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      var g = false
      for(i <- 0 until Total_Latency*2) {
        c.clock.step(1)
        println(s"Clock cycle ${i+1}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
        println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
        if(g){
          c.io.in_ready.poke(true.B)
          g = false
        }else{
          c.io.in_ready.poke(false.B)
          g = true
        }
      }
    }

  }
}
