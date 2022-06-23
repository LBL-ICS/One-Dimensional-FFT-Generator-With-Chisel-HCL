package GoldenModels

import ChiselFFT.FFTDesigns.FFT_sr
import IEEEConversions.FPConvert.{convert_long_to_float, convert_string_to_IEEE_754}
import chiseltest.RawTester.test
import implementation.ComplexNumbers.cmplx
import implementation.FFT.FFT_r
import chisel3.tester.RawTester.test
import chisel3._
import chisel3.tester._

import java.io.PrintWriter
import scala.util.Random

object FFT_GoldenModels {
    def FFT_r_GoldenModel(N: Int, r: Int, inputs: Array[Array[cmplx]]): Array[Array[cmplx]] = { // 2D array holding FFT results for each input group
      val solns = (for(i <- 0 until inputs.length)yield{
        val soln = FFT_r(N,r,inputs(i))
        soln
      }).toArray
      solns
    }

    def genRandom(n: Int, name: String,bw:Int): Unit = {
      val pw = new PrintWriter(name + ".txt")
      val pw2 = new PrintWriter(name + "_IEEE754.txt")
      val start = -500000000000000.0.toLong
      val end = 500000000000000.0.toLong
      val rnd = new Random()
      for (i <- 0 until n) {
        val temp = (start + rnd.nextLong((end - start) + 1)) * 0.000000001
        if (i == n - 1) {
          pw.print(temp)
          pw2.print(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
        } else {
          pw.println(temp)
          pw2.println(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
        }
      }
      pw.close()
      pw2.close()
    }

  // Just an example
  def main(args: Array[String]): Unit = {
    val N = 2
    val r = 2
    val w = 2
    val bw = 32
    val runs = 250000
    val name = "TestInputs"
    genRandom(N*2*runs,name, bw)

    val DFTs_per_stage = N/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val CMultLatency = 2
    val CAddLatency = 1
    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
    val Twid_latency = (N/w) * CMultLatency
    val Perm_latency = 0
    val Total_Latency = (number_of_stages - 1) * Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency

    val golden_input = scala.io.Source.fromFile(name + ".txt").getLines().map(x=>x.toDouble).toArray
    val pw2 = new PrintWriter("inputfile0.txt")
    val pw3 = new PrintWriter("inputfile1.txt")
    golden_input.slice(0,golden_input.length/2).map(x=>pw2.println(convert_string_to_IEEE_754(x.toString,bw).toLong.toHexString))
    golden_input.slice(golden_input.length/2, golden_input.length).map(x=>pw3.println(convert_string_to_IEEE_754(x.toString,bw).toLong.toHexString))
    val in0 = golden_input.slice(0,golden_input.length/2)
    val in1 = golden_input.slice(golden_input.length/2, golden_input.length)
    pw2.close()
    pw3.close()

    val cmplx_inputs = (for(j <- 0 until runs*2 by 2)yield{
      val inp = Array(cmplx(in0(j),in0(j + 1))
        ,cmplx(in1(j),in1(j+1)))
      inp
    }).toArray

    val sw_model = FFT_r_GoldenModel(N, r, cmplx_inputs)

    val pw1 = new PrintWriter("outputfile.txt")
    for(i <- 0 until runs){
      for(j<- 0 until N){
        pw1.println(convert_string_to_IEEE_754(sw_model(i)(j).re.toString, bw).toLong.toHexString)
        pw1.println(convert_string_to_IEEE_754(sw_model(i)(j).im.toString, bw).toLong.toHexString)
      }
    }
    pw1.close()

//    test(new FFT_sr(N,r,w,bw)){c=>
//      val input_List = scala.io.Source.fromFile(name + ".txt").getLines().map(x=>convert_string_to_IEEE_754(x, bw)).toArray
//      var hw_model = (for(i <- 0 until runs)yield{
//        val t = (for(j <- 0 until N)yield{
//          cmplx(0,0)
//        }).toArray
//        t
//      }).toArray
//      for(j <- 0 until runs) {
//        var index = 0
//        for (i <- 0 until (2 * N) by 2) {
//          c.io.in(index).Re.poke(input_List(i).U)
//          c.io.in(index).Im.poke(input_List(i + 1).U)
//          index += 1
//        }
//        c.clock.step(1)
//      }
//      for(j <-0 until runs) {
//        if(j == 0){
//          c.clock.step(Total_Latency-runs)
//        }else{
//          c.clock.step(1)
//        }
//        for (i <- 0 until N) {
//          val val1 = convert_long_to_float(c.io.out(i).Re.peek().litValue, bw).toDouble
//          val val2 = convert_long_to_float(c.io.out(i).Im.peek().litValue, bw).toDouble
//          println(s"Real Output: ${val1}")
//          println(s"Imaginary Output: ${val2}")
//          hw_model(j)(i) = cmplx(val1, val2)
//        }
//      }
//      sw_model.map(x=>x.map(y=>y.print_mag)) // sw results stored here
//      println()
//      hw_model.map(x=>x.map(y=>y.print_mag)) // hw results stored here
//    }
  }
}
