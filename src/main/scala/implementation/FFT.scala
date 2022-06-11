package implementation

import scala.collection.mutable

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
    var rads = (2*Math.PI) / N
    cmplx(Math.cos(rads), Math.sin(rads))
  }

  def L( l: List[Int], n: Int, m: Int): List[Int] = {
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
    new_l.toList
  }

  def R(l: List[Int], n: Int, m:Int): List[Int] = {
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
    val inp: cmplx = cmplx(12.5, -5.6)
    val inp2: cmplx = cmplx(15.5, -2.6)
    complex_add(inp,inp2).print_complex
    complex_sub(inp,inp2).print_complex
    complex_mult(inp,inp2).print_complex
    inp2.print_mag
    complex_exp(W(8), 8).print_complex
    complex_scalar(W(4), -12).print_complex
    val n_size = 27
    val r = 3
    var lizt = for(i <- 0 until n_size) yield {
      i
    }
    println(R(lizt.toList, n_size, r))
    println("Hello Cruel World!")
  }


}
