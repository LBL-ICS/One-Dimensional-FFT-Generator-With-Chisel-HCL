package implementation
import Permutations._
import ComplexNumbers._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.util.Random

object FFT {
  // generate arbitrary size DFT matrix
  def DFT_gen(n: Int):Array[Array[cmplx]] = {
    // initializing the matrix
    var DFT_mtrx = for (i <- 0 until n) yield {
      val row = for (j <- 0 until n) yield {
        cmplx(0,0)
      }
      row.toArray
    }
    var DFT_arr = DFT_mtrx.toArray
    // filling in the matrix with the Wnk terms
    for(i <- 0 until n){
      for(j <- 0 until n){
        DFT_arr(i)(j) = Wnk(n,i*j)
      }
    }
    DFT_arr
  }

  // Perform the computation of the DFT (matrix * vector)
  def DFT_compute(DFT: Array[Array[cmplx]], xi: Array[cmplx], N: Int): Array[cmplx] = {
    // initializing output array
    var xk_t = for (i <- 0 until N) yield {
      cmplx(0,0)
    }
    var xk = xk_t.toArray
    // performing the matrix*vector operation
    for(i <- 0 until N){
      for(j <- 0 until N){
        xk(i) = complex_add(xk(i), complex_mult(DFT(i)(j), xi(j)))
      }
    }
    xk
  }

  // FFT computation for any radix r
  def FFT_r(N: Int, r: Int, xr: Array[cmplx]): Array[cmplx] = {
    val twiddle_factors = T2(N, r) // generate the twiddle factors
    val l = (Math.log10(N)/Math.log10(r)).round.toInt //calculate the number of stages
    val w = N/r // Calculate the number of DFT_r computations per stage
    val DFT_r = DFT_gen(r) // generate a DFT matrix of size r, the radix
    // initialize temporary array
    val temp_t = for (i <- 0 until r) yield {
      cmplx(0, 0)
    }
    var temp = temp_t.toArray
    var Xk = R[cmplx](xr,N, r) // Permute the inputs
    for(i <- 0 until l){ // for each stage
      if(i!=0){ // if not at the first stage
        for(k <- 0 until N){
          Xk(k) = complex_mult( Xk(k), twiddle_factors(i-1)(k)) // apply the twiddle factors
        }
      }
      for(j <- 0 until w){ // for all DFT_r computations required for the given stage
        for(l <- 0 until r){ // fill in the temporary matrix with r elements from Xk
          temp(l) = Xk(j*r + l)
        }
        temp  = DFT_compute(DFT_r, temp, r) // compute the DFT_r
        for(l <- 0 until r){ // update the corresponding Xk values with outputs from DFT_r
          Xk(j*r + l) = temp(l)
        }
      }
      Xk = L[cmplx](Xk,N,r) // Permute Xk
    }
    Xk // return the results of the FFT_r computation
  }

  // FFT computation for mixed radix specification (r and s)
  def FFT_mr(N: Int, nr:Int, ns: Int, r: Int, s: Int, xr:Array[cmplx]): Array[cmplx] = {
    var xk = xr // copy of the inputs array
    val Twid = T2_rs(N,ns,nr) // compute the twiddle factors for the mixed radix case
    xk = L[cmplx](xk,N,nr) // permute the inputs
    println("first_perm------------------------------")
    xk.map(_.print_complex)
    // We are splitting DFT_N into nr DFT_ns computations initially
    // afterward, we apply twiddle factors and compute ns DFT_nr computations
    for(i <- 0 until nr){
      var temp = FFT_r(ns,s,xk.slice(i*ns, i*ns+ns)) //compute the FFT_r based on size ns and radix s
      for(j <- 0 until ns){ // update xk values with new values
        xk(i*ns+j) = temp(j)
      }
    }
    println("first_dft------------------------------")
    xk.map(_.print_complex)
    xk = L[cmplx](xk,N,ns) // permute the xk
    println("second perm------------------------------")
    xk.map(_.print_complex)
    xk = xk.zip(Twid).map{case (a,b) => complex_mult(a,b)} // apply the twiddle factors
    println("twid_fact------------------------------")
    xk.map(_.print_complex)
    for(i <- 0 until ns){
      var temp = FFT_r(nr,r,xk.slice(i*nr, i*nr+nr)) // compute the FFT_r based on size nr and radix r
      for(j <- 0 until nr){ // update xk values with new values
        xk(i*nr+j) = temp(j)
      }
    }
    xk = L[cmplx](xk,N,nr) // permute the xk one last time
    println("third perm------------------------------")
    xk.map(_.print_complex)
    println("end------------------------------")
    xk // return the xk array, now holding the solution
  }
  def isReducable(num:Double):(Boolean,Int) = {
    val pow = (Math.log10(num)/Math.log10(2)).round
    //println(s"Pows: ${pow}")
    if((num - Math.pow(2,pow)).abs <= 0.000001)
      (true, pow.toInt)
    else
      (false, 0)
  }
}
