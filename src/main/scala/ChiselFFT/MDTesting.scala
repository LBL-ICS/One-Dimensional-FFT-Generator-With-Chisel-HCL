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
import MDFFTDesigns._

import scala.collection.mutable

object MDTesting {
  def main(args: Array[String]):Unit = {
    val N = 4
    val r = 2
    val bw = 32
//    test(new FFT2D(N,r,bw)){c=>
//      c.io.in_ready.poke(true.B)
//      for(i <- 0 until N){
//        for(j <- 0 until N){
//          c.io.in(i*N+j).Re.poke(convert_string_to_IEEE_754((i*N+j + 1).toDouble.toString, bw).U)
//          c.io.in(i*N+j).Im.poke(0.U)
//        }
//      }
//      val latency = getFFTLatency(N,r,N,bw) * 2 + 1
//      for(i <- 0 until latency){
//        println("_-_-_-_-_-Outputs_-_-_-_-_-_-_")
//        c.clock.step(1)
//        c.io.in_ready.poke(false.B)
//        println(s"Clock Cycle ${i+1}")
//        println(s"valid: ${c.io.out_validate.peek().litValue}")
//        for(k <- 0 until N){
//          for(l <- 0 until N){
//            println(s"Real Output: ${convert_long_to_float(c.io.out(k*N+l).Re.peek().litValue,bw)}")
//            println(s"Imaginary Outout: ${convert_long_to_float(c.io.out(k*N+l).Im.peek().litValue,bw)}")
//          }
//        }
//        println("_-_-_-_-_-_-_-_-_-_-_-_")
//      }
//    }
//    var file = scala.io.Source.fromFile(s"./InputOutputFilesMD/inputfile_Decimal.txt").getLines().map(x=>x.toDouble).toArray
    val Nd = 3 // the size of each dimension
    val d = 3 // the dimension
    val size = Math.pow(Nd,d).round.toInt // how many total data points are there given the N length and dimension
//    val x = (for(j <- 0 until 2*size by 2) yield{
//      cmplx(file(j),file(j+1))
//    }).toArray
    val x = for(i <- 0 until size)yield{ // list of all the inputs
      cmplx(i+1,0)
    }
    val xx = for(i <- 0 until size)yield{i+1}
    val results = Transpose_example(d,Nd,xx.toArray)
//    val results = MDFFT(d,Nd,x.toArray) // performs the FFT
//    results.map(x=>cmplx(x.re.round,x.im.round)).map(_.print_complex)
//    println("-------------------------------------------------------___")
//    val results2 = MDFFTv3(d,Nd,x.toArray) // performs the FFT
//    results2.map(x=>cmplx(x.re.round,x.im.round)).map(_.print_complex)
//    println("-------------------------------------------------------___")
//    var flag = false
//    for(i <- 0 until size){
//      if(!(results(i).re.toInt == results2(i).re.toInt && results(i).im.toInt == results2(i).im.toInt)){
//        flag = true
//      }
//    }
//    if(flag)
//      println("Incorrect Results")
//    else
//      println("correct results")
//
//    val pw3 = new PrintWriter("od_fft96_2_3.v")
//    pw3.println(getVerilogString(new FFT_mr_v2_streamingv2(96,32,3,2,3,2,32)))
//    pw3.close()
//    val pw4 = new PrintWriter("od_fft96_4_6.v")
//    pw4.println(getVerilogString(new FFT_mr_v2_streamingv2(96,32,3,2,3,4,32)))
//    pw4.close()
//    val pw5 = new PrintWriter("od_fft96_8_12.v")
//    pw5.println(getVerilogString(new FFT_mr_v2_streamingv2(96,32,3,2,3,8,32)))
//    pw5.close()
//    val pw6 = new PrintWriter("od_fft96_16_24.v")
//    pw6.println(getVerilogString(new FFT_mr_v2_streamingv2(96,32,3,2,3,16,32)))
//    pw6.close()
//    val pw7 = new PrintWriter("od_fft96_32_48.v")
//    pw7.println(getVerilogString(new FFT_mr_v2_streamingv2(96,32,3,2,3,32,32)))
//    pw7.close()
  }
}
