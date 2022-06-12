package implementation

import scala.collection.mutable
import scala.util.Random

object FFT {
  class complex {
    var re: Double = re
    var im: Double = im
    def print_complex: Unit = {
      println(s"Re: ${re} Im: ${im}")
    }
    def print_mag: Unit = {
      println(s"Mag: ${complex_mag(this)}")
    }
  }
  type cmplx = complex
  def cmplx(r:Double, i: Double): complex = {
    var t = new complex
    t.re = r
    t.im = i
    t
  }
  def complex_add(in1: cmplx, in2: cmplx): cmplx = {
    cmplx(in1.re + in2.re, in1.im + in2.im)
  }
  def complex_sub(in1: cmplx, in2: cmplx): cmplx = {
    cmplx(in1.re - in2.re, in1.im - in2.im)
  }
  def complex_mult(in1: cmplx, in2: cmplx): cmplx = {
    cmplx(in1.re * in2.re - in1.im * in2.im, in1.re * in2.im + in1.im * in2.re)
  }
  def complex_mag(in1: cmplx): Double = {
    Math.sqrt(in1.re*in1.re + in1.im*in1.im)
  }
  def complex_exp(in1:cmplx, pow: Double): cmplx = {
    val rads = Math.atan(in1.im/in1.re) * pow
    val mag = Math.pow(complex_mag(in1), pow)
    val res = W((2*Math.PI)/rads)
    res.re *= mag
    res.im *= mag
    res
  }
  def complex_scalar(in1:cmplx, scal: Double): cmplx = {
    in1.re *= scal
    in1.im *= scal
    in1
  }


  type W = cmplx
  def W(N: Double): cmplx = {
    var rads = -1 * (2*Math.PI) / N
    cmplx(Math.cos(rads), Math.sin(rads))
  }

  type Wnk = cmplx
  def Wnk(N: Double, nk: Double): cmplx = {
    var rads = (-1 * (2*Math.PI) / N)*nk
    cmplx(Math.cos(rads), Math.sin(rads))
  }

  def L( l: Array[cmplx], n: Int, m: Int): Array[cmplx] = {
    var new_l = l.toArray
    var copy_l = l.toArray
    var t = l.length / n
    var nn = n/m
    for(o <-  0 until t) {
      for (i <- 0 until m) {
        for (j <- 0 until nn) {
          new_l(i * nn + j + o * n) = copy_l(i + j * m + o * n)
        }
      }
    }
    new_l
  }

  def R(l: Array[cmplx], n: Int, m:Int): Array[cmplx] = {
    val t_val = (Math.log10(n)/Math.log10(m)).toInt
    var new_l = l
    for(i <- 0 until t_val){
      val L_n = Math.pow(m, t_val - i).toInt
      val L_m = m
      new_l = L(new_l, L_n, L_m)
    }
    new_l
  }

  def main(args: Array[String]): Unit = {
//    val inp: cmplx = cmplx(12.5, -5.6)
//    val inp2: cmplx = cmplx(15.5, -2.6)
//    complex_add(inp,inp2).print_complex
//    complex_sub(inp,inp2).print_complex
//    complex_mult(inp,inp2).print_complex
//    inp2.print_mag
//    complex_exp(W(8), 8).print_complex
//    complex_scalar(W(4), -12).print_complex
    val n_size = 49
    val r = 7
    var lizt = for(i <- 0 until n_size) yield {
      i
    }
    //println(L(L(lizt.toList, n_size, r), n_size, r))
    val new_DFT = DFT_gen(n_size)
    val xi = for(i <- 0 until n_size) yield{
      val start = -10
      val end = 10
      val rnd = new Random()
      val value = start + rnd.nextInt((end-start) + 1)
      cmplx(value, 0)
    }

    val soln  = DFT_compute(new_DFT, xi.toArray, n_size).map(complex_mag(_)).toSeq
    soln.map(println(_))

    println("-----------------------------------------------")

    val soln2 = FFT_r(n_size, r, xi.toArray).map(complex_mag(_)).toSeq
    soln2.map(println(_))

    println("-----------------------------------------------")

    var Twid = T2(n_size, r).map(_.toList).toList

    println("Hello Cruel World!")
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

  def T(N: Int, r:Int): Array[Array[cmplx]] = {
    val t = ((Math.log10(N) / Math.log10(r)) - 1).toInt
    val T_mtrx_t = for(i <- 0 until t) yield {
      val T_mini = for(j <- 0 until N) yield{
        cmplx(0,0)
      }
      T_mini
    }
    var t_mtrx = T_mtrx_t.map(_.toArray).toArray
    for(tt <- 0 until t) { // cycle through all sets of groups
      for(l <- 0 until N/(N/Math.pow(r, t-tt).toInt * r)) { // repeat for l times to fill up the twiddle matrix
        for(k <- 0 until (N/Math.pow(r, t-tt).toInt)) { // do for each k index
          for (i <- 0 until r) { // each group has r elements
            t_mtrx(tt)( (N/Math.pow(r, t-tt).toInt)*r*l + r*k + i) = Wnk(N,(i * Math.pow(r, t-tt-1).toInt)*k)
          }
        }
      }
    }
    t_mtrx
  }

  def T2(N: Int, r:Int): Array[Array[cmplx]] = {
    val t = ((Math.log10(N) / Math.log10(r)) - 1).toInt
    val T_mtrx_t = for(i <- 0 until t) yield {
      val T_mini = for(j <- 0 until N) yield{
        cmplx(0,0)
      }
      T_mini
    }
    var t_mtrx = T_mtrx_t.map(_.toArray).toArray
    for(tt <- 0 until t) { // cycle through all sets of groups
      for(k <- 0 until (N/Math.pow(r, t-tt).toInt)) { // repeat for l times to fill up the twiddle matrix
         for(l <- 0 until N/(N/Math.pow(r, t-tt).toInt * r)){ // do for each k index
          for (i <- 0 until r) { // each group has r elements
            t_mtrx(tt)( (N/(N/Math.pow(r, t-tt).toInt * r))*r*k + r*l + i) = Wnk(N, (i * Math.pow(r, t-tt-1).toInt)*k)
          }
        }
      }
    }
    t_mtrx
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
