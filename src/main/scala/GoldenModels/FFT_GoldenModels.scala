package GoldenModels

import ChiselFFT.FFTDesigns.FFT_sr
import IEEEConversions.FPConvert.{convert_long_to_float, convert_string_to_IEEE_754}
import chiseltest.RawTester.test
import implementation.ComplexNumbers.cmplx
import implementation.FFT.{DFT_compute, DFT_gen, FFT_r, MDFFT, MDFFTv2}
import chisel3.tester.RawTester.test
import chisel3._
import chisel3.tester._

import java.io.{File, PrintWriter}
import java.nio.file.Files
import scala.reflect.io
import scala.reflect.io.{Directory, RootPath}
import better.files._

import scala.util.Random

object FFT_GoldenModels {
  def FFT_r_GoldenModel(N: Int, r: Int, inputs: Array[Array[cmplx]]): Array[Array[cmplx]] = { // 2D array holding FFT results for each input group
    val solns = (for(i <- 0 until inputs.length)yield{
      val soln = FFT_r(N,r,inputs(i))
      soln
    }).toArray
    solns
  }

  def FFTMD_GoldenModel(d: Int, N: Int, inputs: Array[Array[cmplx]], FFT_Out_Files:IndexedSeq[(PrintWriter,PrintWriter)], Trans_Out_Files:IndexedSeq[(PrintWriter,PrintWriter)]): Array[Array[cmplx]] = { // 2D array holding FFT results for each input group
    val solns = (for(i <- 0 until inputs.length)yield{
      val soln = MDFFTv2(d,N,inputs(i), FFT_Out_Files, Trans_Out_Files)
      soln
    }).toArray
    solns
  }

  def genRandom(n: Int, name: String,bw:Int): Unit = {
    val newfile2 = new File("./InputOutputFiles/" +  name+ ".txt")
    val newfile1 = new File("./InputOutputFiles/" +  name+"_Decimal.txt")
    val pw = new PrintWriter(newfile1)
    val pw2 = new PrintWriter(newfile2)
    val start = -5000000000.0.toLong
    val end = 5000000000.0.toLong
    val rnd = new Random()
    for (i <- 0 until n) {
      val temp = (start + rnd.nextLong((end - start) + 1)) * 0.000000001 + 0.1
      if (i == n - 1) {
        pw.print(temp)
        pw2.print(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
      } else {
        pw.println(temp)
        pw2.println(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
      }
    }
    pw.close()
    pw2.close()
  }
  def genRandom2(n: Int, name: String,bw:Int): Unit = {
    val newfile2 = new File("./InputOutputFiles_SingleInput/" +  name+ ".txt")
    val newfile1 = new File("./InputOutputFiles_SingleInput/" +  name+"_Decimal.txt")
    val pw = new PrintWriter(newfile1)
    val pw2 = new PrintWriter(newfile2)
    val start = -5000000000.0.toLong
    val end = 5000000000.0.toLong
    val rnd = new Random()
    for (i <- 0 until n) {
      val temp = (start + rnd.nextLong((end - start) + 1)) * 0.000000001 +0.1
      if (i == n - 1) {
        pw.print(temp)
        pw2.print(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
      } else {
        pw.println(temp)
        pw2.println(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
      }
    }
    pw.close()
    pw2.close()
  }
  def genRandom3(n: Int, name: String,bw:Int): Unit = {
    val newfile2 = new File("./InputOutputFilesMD/" +  name+ ".txt")
    val newfile1 = new File("./InputOutputFilesMD/" +  name+"_Decimal.txt")
    val pw = new PrintWriter(newfile1)
    val pw2 = new PrintWriter(newfile2)
    val start = -5000000000.0.toLong
    val end = 5000000000.0.toLong
    val rnd = new Random()
    for (i <- 0 until n) {
      val temp = (start + rnd.nextLong((end - start) + 1)) * 0.00000001 +0.1
      if (i == n - 1) {
        pw.print(temp)
        pw2.print(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
      } else {
        pw.println(temp)
        pw2.println(convert_string_to_IEEE_754(temp.toString, bw).toLong.toHexString)
      }
    }
    pw.close()
    pw2.close()
  }

  // N is size of dft/fft, just let r = N if computing dft, bw can be 16,32,64, or 128, and runs is the number of tests for the dft
  def genDFTInOutFile(N: Int, r: Int, bw: Int, runs: Int): Unit ={ // use this to generate the dft/fft input output files
    val subdirectory = file"./InputOutputFiles"
    subdirectory.clear()
    for(i <- 0 until N){
      genRandom(2*runs,s"inputfile${i}",bw)
    }
    val getFiles = (for(i <- 0 until N) yield{
      val file = scala.io.Source.fromFile(s"./InputOutputFiles/inputfile${i}_Decimal.txt").getLines().map(x=>x.toDouble)
      file.toArray
    }).toArray
    val cmplxFiles = (for(i <- 0 until N) yield{
      (for(j <- 0 until 2*runs by 2) yield{
        cmplx(getFiles(i)(j),getFiles(i)(j+1))
      }).toArray
    }).toArray
    val cmplxInput = (for(i <- 0 until runs) yield{
      (for(j <- 0 until N) yield{
        cmplxFiles(j)(i)
      }).toArray
    }).toArray
    val goldenModelOutputs = FFT_r_GoldenModel(N, r, cmplxInput)
    val newfile1 = new File("./InputOutputFiles/" +  "outputfile.txt")
    val newfile2 = new File("./InputOutputFiles/" +  "outputfile_Decimal.txt")
    val pw1 = new PrintWriter(newfile1) // will have the hex IEEE 754 representations
    val pw2 = new PrintWriter(newfile2)
    for(i <- 0 until runs){
      for(j<- 0 until N){
        pw2.println(goldenModelOutputs(i)(j).re.toString)
        pw2.println(goldenModelOutputs(i)(j).im.toString)
        pw1.println(convert_string_to_IEEE_754(goldenModelOutputs(i)(j).re.toString, bw).toLong.toHexString)
        pw1.println(convert_string_to_IEEE_754(goldenModelOutputs(i)(j).im.toString, bw).toLong.toHexString)
      }
    }
    pw1.close()
    pw2.close()
  }

  def genDFTInOutFile_single_input_file(N: Int, r: Int, bw: Int, runs: Int): Unit ={ // use this to generate the dft/fft input output files
    val subdirectory = file"./InputOutputFiles_SingleInput"
    subdirectory.clear()
    genRandom2(N*2*runs,s"inputfile",bw)
    //    for(i <- 0 until N){
    //      genRandom(2*runs,s"inputfile${i}",bw)
    //    }
    var file = scala.io.Source.fromFile(s"./InputOutputFiles_SingleInput/inputfile_Decimal.txt").getLines().map(x=>x.toDouble).toArray
    //file.toArray
    //    val getFiles = (for(i <- 0 until N) yield{
    //      val file = scala.io.Source.fromFile(s"./InputOutputFiles_SingleInput/inputfile${i}_Decimal.txt").getLines().map(x=>x.toDouble)
    //      file.toArray
    //    }).toArray
    val cmplxFiles = (for(i <- 0 until runs) yield{
      (for(j <- 0 until 2*N by 2) yield{
        cmplx(file(i*2*N + j),file(i*2*N + j+1))
      }).toArray
    }).toArray
    val cmplxInput = (for(i <- 0 until runs) yield{
      (for(j <- 0 until N) yield{
        cmplxFiles(i)(j)
      }).toArray
    }).toArray
    val goldenModelOutputs = FFT_r_GoldenModel(N, r, cmplxInput)
    val newfile1 = new File("./InputOutputFiles_SingleInput/" +  "outputfile.txt")
    val newfile2 = new File("./InputOutputFiles_SingleInput/" +  "outputfile_Decimal.txt")
    val pw1 = new PrintWriter(newfile1) // will have the hex IEEE 754 representations
    val pw2 = new PrintWriter(newfile2)
    for(i <- 0 until runs){
      for(j<- 0 until N){
        pw2.println(goldenModelOutputs(i)(j).re.toString)
        pw2.println(goldenModelOutputs(i)(j).im.toString)
        pw1.println(convert_string_to_IEEE_754(goldenModelOutputs(i)(j).re.toString, bw).toLong.toHexString)
        pw1.println(convert_string_to_IEEE_754(goldenModelOutputs(i)(j).im.toString, bw).toLong.toHexString)
      }
    }
    pw1.close()
    pw2.close()
  }

  def genDFTInOutFile_MD(d:Int, N: Int, bw: Int, runs: Int): Unit ={ // use this to generate the dft/fft input output files
    val subdirectory = file"./InputOutputFilesMD"
    subdirectory.clear()
    val FFT_Out_Files = for(i <- 0 until d)yield{
      val newfile = new File("./InputOutputFilesMD/" +  s"FFT_stage${i}_outputfile.txt")
      val newfile2 = new File("./InputOutputFilesMD/" +  s"FFT_stage${i}_outputfile_Decimal.txt")
      val pw = new PrintWriter(newfile)
      val pw2 = new PrintWriter(newfile2)
      (pw,pw2)
    }
    val Trans_Out_Files = for(i <- 0 until d)yield{
      val newfile = new File("./InputOutputFilesMD/" +  s"Trans_stage${i}_outputfile.txt")
      val newfile2 = new File("./InputOutputFilesMD/" +  s"Trans_stage${i}_outputfile_Decimal.txt")
      val pw = new PrintWriter(newfile)
      val pw2 = new PrintWriter(newfile2)
      (pw,pw2)
    }
    val size = Math.pow(N,d).round.toInt
    genRandom3(size*2*runs,s"inputfile",bw)
    var file = scala.io.Source.fromFile(s"./InputOutputFilesMD/inputfile_Decimal.txt").getLines().map(x=>x.toDouble).toArray
    val cmplxFiles = (for(i <- 0 until runs) yield{
      (for(j <- 0 until 2*size by 2) yield{
        cmplx(file(i*2*size + j),file(i*2*size + j+1))
      }).toArray
    }).toArray
    val cmplxInput = (for(i <- 0 until runs) yield{
      (for(j <- 0 until size) yield{
        cmplxFiles(i)(j)
      }).toArray
    }).toArray
    val goldenModelOutputs = FFTMD_GoldenModel(d,N,cmplxInput, FFT_Out_Files, Trans_Out_Files)
    val newfile1 = new File("./InputOutputFilesMD/" +  "outputfile.txt")
    val newfile2 = new File("./InputOutputFilesMD/" +  "outputfile_Decimal.txt")
    val pw1 = new PrintWriter(newfile1) // will have the hex IEEE 754 representations
    val pw2 = new PrintWriter(newfile2)
    for(i <- 0 until runs){
      for(j<- 0 until size){
        pw2.println(goldenModelOutputs(i)(j).re.toString)
        pw2.println(goldenModelOutputs(i)(j).im.toString)
        pw1.println(convert_string_to_IEEE_754(goldenModelOutputs(i)(j).re.toString, bw).toLong.toHexString)
        pw1.println(convert_string_to_IEEE_754(goldenModelOutputs(i)(j).im.toString, bw).toLong.toHexString)
      }
    }
    pw1.close()
    pw2.close()
    for(i <- 0 until d){
      FFT_Out_Files(i)._1.close()
      FFT_Out_Files(i)._2.close()
      Trans_Out_Files(i)._1.close()
      Trans_Out_Files(i)._2.close()
    }
  }

  // Just an example
  def main(args: Array[String]): Unit = {
    val N = 96 // size of dft/fft
    val r = 96 // radix of fft if applicable(if just dft then N = r)
    val w = 9 // width of streaming (just set the same as N, the fft width implementation is not yet done)
    val bw = 32 // precision
    val runs = 1 // number of runs for inputs/outputs to be generated
    //val name = "TestInputs"
    //genRandom(N*2*runs,name, bw) // generate about twice of the total N for all runs, because we are working with complex numbes
    //    genDFTInOutFile(N,r,bw,runs) // for example
    //    genDFTInOutFile_single_input_file(N,r,bw,runs) // for single input file
    val d = 3
    genDFTInOutFile_MD(d,N,bw, runs)
    //    val DFTs_per_stage = N/r
    //    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    //    val CMultLatency = 2
    //    val CAddLatency = 1
    //    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
    //    val Twid_latency = (N/w) * CMultLatency
    //    val Perm_latency = 0
    //    val Total_Latency = (number_of_stages - 1) * Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency

    //    val golden_input = scala.io.Source.fromFile(name + "_Decimal.txt").getLines().map(x=>x.toDouble).toArray
    //    val pw2 = new PrintWriter("inputfile0.txt")
    //    val pw3 = new PrintWriter("inputfile1.txt")
    //    pw2.close()
    //    pw3.close()
    //    var index = 0
    //    val cmplx_inputs = (for(j <- 0 until runs)yield{
    //      val inp = (for(i <- 0 until N) yield{
    //        val temp = cmplx(golden_input(index),golden_input(index+1))
    //        index+=2
    //        temp
    //      }).toArray
    //      inp
    //    }).toArray
    //    cmplx_inputs.map(x=>x.map(y=>println(y.re)))
    //    val sw_model = FFT_r_GoldenModel(N, r, cmplx_inputs)
    //
    //    val pw1 = new PrintWriter("outputfile.txt")
    //    for(i <- 0 until runs){
    //      for(j<- 0 until N){
    //        pw1.println(convert_string_to_IEEE_754(sw_model(i)(j).re.toString, bw).toLong.toHexString)
    //        pw1.println(convert_string_to_IEEE_754(sw_model(i)(j).im.toString, bw).toLong.toHexString)
    //      }
    //    }
    //    pw1.close()

    //    test(new FFT_sr(N,r,w,bw)){c=>
    //      val input_List = scala.io.Source.fromFile(name + ".txt").getLines().map(x=>convert_string_to_IEEE_754(x, bw)).toArray
    //      val inp3 = input_List.slice(0, input_List.length/2)
    //      val inp4 = input_List.slice(input_List.length/2,input_List.length)
    //      var hw_model = (for(i <- 0 until runs)yield{
    //        val t = (for(j <- 0 until N)yield{
    //          cmplx(0,0)
    //        }).toArray
    //        t
    //      }).toArray
    //      var index = 0
    //      for(j <- 0 until runs) {
    //        for (i <- 0 until N) {
    //          c.io.in(i).Re.poke(input_List(index).U)
    //          println(input_List(index))
    //          c.io.in(i).Im.poke(input_List(index+ 1).U)
    //          index += 2
    //        }
    //        c.clock.step(1)
    //      }
    //      for(j <-0 until runs) {
    //        if(j == 0){
    //          c.clock.step(Total_Latency-runs)
    //        }else{
    //          c.clock.step(1)
    //        }
    //        for (i <- 0 until N) {
    //          val val1 = convert_long_to_float(c.io.out(i).Re.peek().litValue, bw).toDouble
    //          val val2 = convert_long_to_float(c.io.out(i).Im.peek().litValue, bw).toDouble
    //          println(s"Real Output: ${val1}")
    //          println(s"Imaginary Output: ${val2}")
    //          hw_model(j)(i) = cmplx(val1, val2)
    //        }
    //      }
    //      sw_model.map(x=>x.map(y=>y.print_mag)) // sw results stored here
    //      println()
    //      hw_model.map(x=>x.map(y=>y.print_mag)) // hw results stored here
    //    }
  }
}
