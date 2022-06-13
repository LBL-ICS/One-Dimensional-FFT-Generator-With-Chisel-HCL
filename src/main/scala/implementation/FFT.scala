package implementation
import Permutations._
import ComplexNumbers._
import scala.util.Random

object FFT {
  def main(args: Array[String]): Unit = {
    val n_size = 3
    val r = 3

    val xi = for(i <- 0 until n_size) yield{
      val start = -10
      val end = 10
      val rnd = new Random()
      val value = start + rnd.nextInt((end-start) + 1)
      cmplx(value, 0)
    }
    println("-----------------------------------------------")

    val soln2 = FFT_r(n_size, r, xi.toArray).map(complex_mag(_)).toSeq
    soln2.map(println(_))

    println("-----------------------------------------------")

    val Nw = 108
    val ll = for(i <- 0 until Nw) yield{
      cmplx(i,0)
    }
    val soln3 = FFT_mr(Nw,4,27,2,3, ll.toArray)
    soln3.map(_.print_complex)
    println("-------------------------------")
    val soln4 = DFT_compute(DFT_gen(Nw),ll.toArray,Nw).map(_.print_complex)

//    val k = T2_rs(6,3,2)
//    k.map(_.print_complex)
//    Wnk(6,1).print_complex
//    Wnk(6,2).print_complex
  }

  def DFT_gen(n: Int):Array[Array[cmplx]] = {
    var DFT_mtrx = for (i <- 0 until n) yield {
      val row = for (j <- 0 until n) yield {
        cmplx(0,0)
      }
      row.toArray
    }
    var DFT_arr = DFT_mtrx.toArray

    for(i <- 0 until n){
      for(j <- 0 until n){
        DFT_arr(i)(j) = Wnk(n,i*j)
      }
    }
    DFT_arr
  }

  def DFT_compute(DFT: Array[Array[cmplx]], xi: Array[cmplx], N: Int): Array[cmplx] = {
    var xk_t = for (i <- 0 until N) yield {
      cmplx(0,0)
    }
    var xk = xk_t.toArray
    for(i <- 0 until N){
      for(j <- 0 until N){
        xk(i) = complex_add(xk(i), complex_mult(DFT(i)(j), xi(j)))
      }
    }
    xk
  }

  def FFT_r(N: Int, r: Int, xr: Array[cmplx]): Array[cmplx] = {
    val twiddle_factors = T2(N, r)
    val l = (Math.log10(N)/Math.log10(r)).toInt
    val w = N/r
    val DFT_r = DFT_gen(r)
    val arr_t = for (i <- 0 until r) yield {
      cmplx(0, 0)
    }
    var arr = arr_t.toArray
    var Xk = R(xr,N, r)
    for(i <- 0 until l){
      if(i!=0){
        for(k <- 0 until N){
          Xk(k) = complex_mult( Xk(k), twiddle_factors(i-1)(k))
        }
      }
      for(j <- 0 until w){
        for(l <- 0 until r){
          arr(l) = Xk(j*r + l)
        }
        arr  = DFT_compute(DFT_r, arr, r)
        for(l <- 0 until r){
          Xk(j*r + l) = arr(l)
        }
      }
      Xk = L(Xk,N,r)
    }
    Xk
  }

  def FFT_mr(N: Int, nr:Int, ns: Int, r: Int, s: Int, xr:Array[cmplx]): Array[cmplx] = {
    var xk = xr
    val Twid = T2_rs(N,ns,nr)
    xk = L(xk,N,nr)
    for(i <- 0 until nr){
      var temp = FFT_r(ns,s,xk.slice(i*ns, i*ns+ns))
      for(j <- 0 until ns){
        xk(i*ns+j) = temp(j)
      }
    }
    xk = L(xk,N,ns)
    xk = xk.zip(Twid).map{case (a,b) => complex_mult(a,b)}
    for(i <- 0 until ns){
      var temp = FFT_r(nr,r,xk.slice(i*nr, i*nr+nr))
      for(j <- 0 until nr){
        xk(i*nr+j) = temp(j)
      }
    }
    xk = L(xk,N,nr)
    xk
  }


}
