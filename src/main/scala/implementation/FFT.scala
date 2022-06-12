package implementation
import Permutations._
import ComplexNumbers._
import scala.util.Random

object FFT {
  def main(args: Array[String]): Unit = {
    val n_size = 49
    val r = 7

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


}
