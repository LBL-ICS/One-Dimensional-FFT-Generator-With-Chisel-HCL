package SWFFT
import Permutations._
import ChiselFFT.Testing._
import ChiselFFT.FFTDesigns._
import better.files.StringInterpolations
import chisel3.getVerilogString

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Random
import java.io.{File, PrintWriter}

object FFTMain {
  def main(args: Array[String]): Unit = {
    //This is for testing the software based FFT Designs
    //testing the single radix FFT
    val N = 4 // the size of FFT
    val r = 2 // the radix of FFT
    val xi = (1 to N).toArray.map(x=>ComplexNumbers.cmplx(x,0))
    val results_sr = FFT.FFT_r(N,r,xi)

    //testing the mixed-radix FFT
    val N_mr = 6 // the size of FFT
    val r_mr = 2 // the radix r
    val s_mr = 3 // the radix s
    val nr_mr = 2 // the r^k
    val ns_mr = 3 // the s^l
    val xi_mr = (1 to N_mr).toArray.map(x=>ComplexNumbers.cmplx(x,0))
    val results_mr = FFT.FFT_mr(N_mr,nr_mr,ns_mr,r_mr,s_mr,xi_mr)

    //testing the multi-dimensional FFT
    val d_md = 3 // the dimension
    val N_md = 2 // the size of each dimension
    val xi_md = (1 to Math.pow(N_md,d_md).round.toInt).toArray.map(x=>ComplexNumbers.cmplx(x,0))
    val results_md = FFT.MDFFT_PB(d_md,N_md, xi_md)
  }
}
