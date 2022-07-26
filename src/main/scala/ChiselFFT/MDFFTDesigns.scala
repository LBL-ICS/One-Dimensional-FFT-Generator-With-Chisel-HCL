package ChiselFFT
import FFTDesigns._
import chisel3._
import chisel3.util._
import ComplexModules.FPComplex._
import implementation._
import IEEEConversions.FPConvert._
import Chisel.{MuxLookup, log2Ceil, resetToBool}
import ComplexModules.FPComplex
import implementation.ComplexNumbers.cmplx
import scala.collection.mutable

object MDFFTDesigns {
  class FFT2D(N: Int, r: Int,bw:Int) extends Module{ // simple non-streaming version
    val io = IO(new Bundle() {
      val in = Input(Vec(N*N, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(N*N, new ComplexNum(bw)))
    })
    val FFT1Ds = VecInit.fill(N)(Module(new FFT_sr_v2_nrv(N,r,N,bw)).io) // we can reuse
    val Permutes = Module(new PermutationsBasic(N*N,N,0,bw)).io // one permutation module
    val FFT1DLatency = getFFTLatency(N,r,N,bw)
    val TotalLatency = FFT1DLatency*2
    val regdelays = RegInit(VecInit.fill(TotalLatency)(false.B))
    val out_regdelay = RegInit(false.B)
    val out_regs = RegInit(VecInit.fill(N*N)(0.U.asTypeOf(new ComplexNum(bw))))
    out_regdelay := regdelays(TotalLatency-1)
    io.out_validate := out_regdelay
    io.out := out_regs
//    io.out_test := Permutes.out
    for(i <- 1 until TotalLatency){
      regdelays(i) := regdelays(i-1)
    }
    for(i <- 0 until N){
      for(j <- 0 until N){
        FFT1Ds(i).in(j) := Permutes.out(i*N + j)
      }
    }
    when(regdelays(TotalLatency-1)){
      for(i <- 0 until N){
        for(j<- 0 until N){
          out_regs(i*N+j) := FFT1Ds(i).out(j)
        }
      }
    }.otherwise{
      out_regs := VecInit.fill(N*N)(0.U.asTypeOf(new ComplexNum(bw)))
    }
    when(io.in_ready){
      regdelays(0) := io.in_ready
      Permutes.in.zipWithIndex.map(x=>x._1 := io.in(x._2))
    }.otherwise{
      regdelays(0) := false.B
      for(i <- 0 until N){
        for(j <- 0 until N){
          Permutes.in(i*N + j) := FFT1Ds(i).out(j)
        }
      }
    }
  }
}
