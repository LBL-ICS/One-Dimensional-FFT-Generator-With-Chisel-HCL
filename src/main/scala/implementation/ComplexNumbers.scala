package implementation

object ComplexNumbers {
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
  def complex_scalar(in1:cmplx, scal: Double): cmplx = {
    in1.re *= scal
    in1.im *= scal
    in1
  }

  type Wnk = cmplx
  def Wnk(N: Double, nk: Double): cmplx = {
    var rads = (-1 * (2*Math.PI) / N)*nk
    cmplx(Math.cos(rads), Math.sin(rads))
  }


}
