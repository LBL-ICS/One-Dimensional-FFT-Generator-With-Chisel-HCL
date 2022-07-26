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
    val Nd = 2
    val d = 2
    val size = Math.pow(Nd,d).round.toInt
//    val x = (for(j <- 0 until 2*size by 2) yield{
//      cmplx(file(j),file(j+1))
//    }).toArray
    val x = for(i <- 0 until size)yield{
      cmplx(i+1,0)
    }
    val results = MDFFT(d,Nd,x.toArray)
    results.map(x=>cmplx(x.re.round,x.im.round)).map(_.print_complex)
  }
}
