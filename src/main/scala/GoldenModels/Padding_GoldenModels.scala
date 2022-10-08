package GoldenModels

import scala.collection.mutable
import SWFFT.FFT._
import SWFFT.ComplexNumbers._
import IEEEConversions.FPConvert._
import chisel3._
import chisel3.tester._
import chisel3.tester.RawTester.test
import java.io.PrintWriter
import ChiselFFT.FFTMRDesigns._
object Padding_GoldenModels {
  def main(args: Array[String]):Unit = {
    val pd = padding_process(Array(1,2,3,4).map(x=>(cmplx(x.toDouble,0))), 12)
    val fllpd_nfft = full_process((for(i <- 1 to 32)yield{i}).map(x=>cmplx(x.toDouble, 0)).toArray, Array(12,48), 12, Array(Array(2,2,2,4,4,2,2,4,4,2,2,2), Array(2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2), Array(4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4)))
    val MDFFTr = MDFFT_PA(3,12,fllpd_nfft)
    for (i <- 0 until 12) {
      for (j <- 0 until 12) {
        for (k <- 0 until 12) {
          print(s"${MDFFTr(i * 12 * 12 + j * 12 + k).re.round} ")
        }
        print("\t\t")
        for (k <- 0 until 12) {
          print(s"${MDFFTr(i * 12 * 12 + j * 12 + k).im.round} ")
        }
        println()
      }
      println()
    }
    println("-=-=-=-=-=-=-=-=-=-=-=-=--=-=-=-=-=-=-=---=-=-=-=--=-")
    val fllpd = full_process_FFT((for(i <- 1 to 32)yield{i}).map(x=>cmplx(x.toDouble, 0)).toArray, Array(12,48), 12, Array(Array(2,2,2,4,4,2,2,4,4,2,2,2), Array(2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2,2,4,4,2), Array(4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4)))
    for(i <- 0 until 12){
      for(j <- 0 until 12){
        for(k <- 0 until 12){
          print(s"${fllpd(i*12*12 + j*12 + k).re.round} ")
        }
        print("\t\t")
        for(k <- 0 until 12){
          print(s"${fllpd(i*12*12 + j*12 + k).im.round} ")
        }
        println()
      }
      println()
    }
    // manual check
    var crrct = true
    for(i <-0 until fllpd.length){
      if((MDFFTr(i).re - fllpd(i).re).abs > 0.0001 || (MDFFTr(i).im - fllpd(i).im).abs > 0.0001){
        crrct = false
        MDFFTr(i).print_complex
        fllpd(i).print_complex
        println()
      }
    }
    if(crrct)
      println("the results are good")
    else
      println("the results are bad")
    println("Next--=---=---=---=---=--==-=--=--=---=--=--=--=--=---=--=--=--=--=---=--=--=--=-=-=-=-")
//    test(new FFT_MixedRadix_Streaming_v2(12,4,3,2,3,2,32)){c=>
//      c.io.in_en.poke(true.B)
//      c.io.in_ready.poke(true.B)
//      println("-----Stopping-----")
//      c.io.in_en.poke(false.B)
//      for (i <- 0 until 100) {
//        c.clock.step(1)
//      }
//      c.io.in_en.poke(true.B)
//      println("-----Resuming-----")
//      for(i <- 0 until 12/2){
//        println(s"Clock Cycle: ${i}")
//        for(j <- 0 until 2){
//          c.io.in(j).Re.poke(convert_string_to_IEEE_754((i*2+j+1).toDouble.toString, 32).U)
//          c.io.in(j).Im.poke(0.U)
//        }
//        c.clock.step(1)
//        println(s"valid: ${c.io.out_validate.peek().litValue}")
//        for(j <- 0 until 2){
//          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, 32)}")
//          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue, 32)}")
//        }
//      }
//      println("-----Stopping-----")
//      c.io.in_en.poke(false.B)
//      for(i <- 0 until 100){
//        c.clock.step(1)
//      }
//      c.io.in_en.poke(true.B)
//      println("-----Resuming-----")
//      for(i <- 0 until 130){
//        println(s"Clock Cycle: ${i+12/2}")
//        for (j <- 0 until 2) {
//          c.io.in(j).Re.poke(convert_string_to_IEEE_754(((i * 2 + j)%12 + 12 + 1).toDouble.toString, 32).U)
//          c.io.in(j).Im.poke(0.U)
//        }
//        c.clock.step(1)
//        println(s"valid: ${c.io.out_validate.peek().litValue}")
//        for (j <- 0 until 2) {
//          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, 32)}")
//          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue, 32)}")
//        }
//      }
//    }
  }

  def padding_process(pencil: Array[cmplx], paddinglength: Int): Array[cmplx] = {
    val strt_indx = (paddinglength-pencil.length)/2
    val padded_pencil = mutable.ArrayBuffer[cmplx]()
    for(i <- 0 until strt_indx){
      padded_pencil += cmplx(0,0)
    }
    for(i <- 0 until pencil.length){
      padded_pencil += pencil(i)
    }
    for(i <- 0 until strt_indx){
      padded_pencil += cmplx(0,0)
    }
    padded_pencil.toArray
  }


  def full_process(sphere: Array[cmplx], elmnts_slice: Array[Int], paddinglength: Int, segmentlengths: Array[Array[Int]]): Array[cmplx] = {
    val cylinder = (for(i <- 0 until elmnts_slice(0)*paddinglength) yield{
      cmplx(0,0)
    }).toArray
    val cuboid = (for(i <- 0 until elmnts_slice(1)*paddinglength)yield{
      cmplx(0,0)
    }).toArray
    val cube = (for(i <- 0 until paddinglength*paddinglength*paddinglength) yield{
      cmplx(0,0)
    }).toArray
    for(i <- 0 until segmentlengths.length){
      var prev_sg_lngth_acc = 0
      var offst = 0
      for(j <- 0 until segmentlengths(i).length){
        if(i == 0){
          val pd_pncil = padding_process(sphere.slice(prev_sg_lngth_acc,prev_sg_lngth_acc+segmentlengths(i)(j)),paddinglength)
          prev_sg_lngth_acc += segmentlengths(i)(j)
          for(k <- 0 until pd_pncil.length){
            cylinder(k*elmnts_slice(0)+offst) = pd_pncil(k)
          }
          offst += 1
        }else if (i == 1){
          val pd_pncil = padding_process(cylinder.slice(prev_sg_lngth_acc,prev_sg_lngth_acc+segmentlengths(i)(j)),paddinglength)
          prev_sg_lngth_acc += segmentlengths(i)(j)
          for(k <- 0 until pd_pncil.length){
            cuboid(k*elmnts_slice(1)+offst) = pd_pncil(k)
          }
          offst += 1
        }else{
          val pd_pncil = padding_process(cuboid.slice(prev_sg_lngth_acc,prev_sg_lngth_acc+segmentlengths(i)(j)),paddinglength)
          prev_sg_lngth_acc += segmentlengths(i)(j)
          for(k <- 0 until pd_pncil.length){
            cube(k+offst) = pd_pncil(k)
          }
          offst += paddinglength
        }
      }
    }
    cube
  }
//
  def full_process_FFT(sphere: Array[cmplx], elmnts_slice: Array[Int], paddinglength: Int, segmentlengths: Array[Array[Int]]): Array[cmplx] = {
    val pw = new PrintWriter("golden_output.txt")
    val cylinder = (for (i <- 0 until elmnts_slice(0) * paddinglength) yield {
      cmplx(0,0)
    }).toArray
    val cuboid = (for (i <- 0 until elmnts_slice(1) * paddinglength) yield {
      cmplx(0,0)
    }).toArray
    val cube = (for (i <- 0 until paddinglength * paddinglength * paddinglength) yield {
      cmplx(0,0)
    }).toArray
    for (i <- 0 until segmentlengths.length) {
      var prev_sg_lngth_acc = 0
      var offst = 0
      for (j <- 0 until segmentlengths(i).length) {
        if (i == 0) {
          var pd_pncil = padding_process(sphere.slice(prev_sg_lngth_acc, prev_sg_lngth_acc + segmentlengths(i)(j)), paddinglength)
          pd_pncil = FFT_mr(12,4,3,2,3,pd_pncil)
          pw.println("-=-=-=-=-first stage fft-=-=-=-=-=-")
          for(p <- 0 until pd_pncil.length){
            pw.println((pd_pncil(p).re, pd_pncil(p).im))
          }
          pw.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-")
          prev_sg_lngth_acc += segmentlengths(i)(j)
          for (k <- 0 until pd_pncil.length) {
            cylinder(k * elmnts_slice(0) + offst) = pd_pncil(k)
          }
          offst += 1
        } else if (i == 1) {
          var pd_pncil = padding_process(cylinder.slice(prev_sg_lngth_acc, prev_sg_lngth_acc + segmentlengths(i)(j)), paddinglength)
          pd_pncil = FFT_mr(12,4,3,2,3,pd_pncil)
          pw.println("-=-=-=-=-second stage fft-=-=-=-=-=-")
          for (p <- 0 until pd_pncil.length) {
            pw.println((pd_pncil(p).re, pd_pncil(p).im))
          }
          pw.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-")
          prev_sg_lngth_acc += segmentlengths(i)(j)
          for (k <- 0 until pd_pncil.length) {
            cuboid(k * elmnts_slice(1) + offst) = pd_pncil(k)
          }
          offst += 1
        } else {
          var pd_pncil = padding_process(cuboid.slice(prev_sg_lngth_acc, prev_sg_lngth_acc + segmentlengths(i)(j)), paddinglength)
          pd_pncil = FFT_mr(12,4,3,2,3,pd_pncil)
          pw.println("-=-=-=-=-third stage fft-=-=-=-=-=-")
          for (p <- 0 until pd_pncil.length) {
            pw.println((pd_pncil(p).re, pd_pncil(p).im))
          }
          pw.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-")
          prev_sg_lngth_acc += segmentlengths(i)(j)
          for (k <- 0 until pd_pncil.length) {
            cube(k + offst) = pd_pncil(k)
          }
          offst += paddinglength
        }
      }
    }
    pw.close()
    cube
  }
}
