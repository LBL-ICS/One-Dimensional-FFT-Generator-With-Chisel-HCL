package ChiselFFT
import IEEEConversions.FPConvert._
import FloatingPointDesigns.FPArithmetic._
import ComplexModules.FPComplex._
import chisel3.tester.RawTester.test
import FloatingPointDesigns.FPArithmetic._
import chisel3._
import chisel3.tester._
import ChiselFFT.FFTDesigns._
import SWFFT.FFT._
import SWFFT.ComplexNumbers._
import SWFFT._
import GoldenModels.FFT_GoldenModels._

import java.io.PrintWriter
import scala.util.Random
import GoldenModels.FFT_GoldenModels._
import Chisel.{isPow2, log2Ceil}
import MDFFTDesigns._

import scala.collection.mutable

object MDTesting {
  def main(args: Array[String]):Unit = {
    val N = 6
    val win = 2
    val nr = 2
    val ns = 3
    val r = 2
    val s = 3
    val bw = 32
    //    test(new FFT3D(N,win,nr,ns,r,s,bw)){c=>
    //      c.io.in_ready.poke(true.B)
    //      val total = getfftstreamingmrlatency(N,nr,ns,r,s,win,bw) * 3 + (N*N*N/win)*6 + 1
    //      for(i <- 0 until total + (N*N*N/win)*2 - 1){
    //        for(j <- 0 until win){
    //          c.io.in(j).Re.poke(convert_string_to_IEEE_754(((i*win+j+1)%(N*N*N)).toDouble.toString, bw).U)
    //          c.io.in(j).Im.poke(0.U)
    //        }
    //        c.clock.step(1)
    //        println("_-_-_-_-_-Outputs_-_-_-_-_-_-_")
    //        println(s"Clock Cycle ${i+1}")
    //        println(s"valid: ${c.io.out_validate.peek().litValue}")
    //        for(j <-0 until win){
    //          println(s"Real output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue,bw)}")
    //          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue,bw)}")
    //        }
    //      }
    //    }
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
  }
}
