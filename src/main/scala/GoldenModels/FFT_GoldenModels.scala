package GoldenModels

import IEEEConversions.FPConvert.{convert_long_to_float, convert_string_to_IEEE_754}
import chiseltest.RawTester.test
import SWFFT.ComplexNumbers.cmplx
import SWFFT.FFT.{DFT_compute, DFT_gen, FFT_r, MDFFT_PA, MDFFT_PB, MDFFT_goldenmodeluse}
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
  // use for 1D FFT
  def FFT_r_GoldenModel(N: Int, r: Int, inputs: Array[Array[cmplx]]): Array[Array[cmplx]] = {
    val solns = (for(i <- 0 until inputs.length)yield{
      val soln = FFT_r(N,r,inputs(i))
      soln
    }).toArray
    solns
  }

  // use for multi-dimensional FFTs
  def FFTMD_GoldenModel(d: Int, N: Int, inputs: Array[Array[cmplx]], FFT_Out_Files:IndexedSeq[(PrintWriter,PrintWriter)], Trans_Out_Files:IndexedSeq[(PrintWriter,PrintWriter)]): Array[Array[cmplx]] = { // 2D array holding FFT results for each input group
    val solns = (for(i <- 0 until inputs.length)yield{
      val soln = MDFFT_goldenmodeluse(d,N,inputs(i), FFT_Out_Files, Trans_Out_Files)
      soln
    }).toArray
    solns
  }

  // used for generating random values
  def genRandom(n: Int, directory: String,name: String,bw:Int): Unit = {
    val newfile2 = new File(directory+ "/" +  name+ ".txt")
    val newfile1 = new File(directory+"/" +  name+"_Decimal.txt")
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

  // N is size of dft/fft, just let r = N if computing dft, bw can be 16,32,64, or 128, and runs is the number of tests for the dft
  def genDFTInOutFile(N: Int, r: Int, bw: Int, runs: Int): Unit ={ // use this to generate the dft/fft input output files
    val subdirectory = file"./FFT${N}_GoldenModel" // name of directory to store results
    val directoryName = subdirectory.pathAsString
    if(!subdirectory.exists){
      subdirectory.createDirectoryIfNotExists()
    }else{
      subdirectory.clear()
    }
    for(i <- 0 until N){
      genRandom(2*runs,directoryName,s"inputfile${i}",bw)
    }
    val getFiles = (for(i <- 0 until N) yield{
      val file = scala.io.Source.fromFile(directoryName+s"/inputfile${i}_Decimal.txt").getLines().map(x=>x.toDouble)
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
    val newfile1 = new File(directoryName+  "/outputfile.txt")
    val newfile2 = new File(directoryName+  "/outputfile_Decimal.txt")
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

  // generate the golden model files but with a single file for the inputs
  def genDFTInOutFile_single_input_file(N: Int, r: Int, bw: Int, runs: Int): Unit ={ // use this to generate the dft/fft input output files
    val subdirectory = file"./FFT${N}_GoldenModel_SI" // name of directory to store results
    val directoryName = subdirectory.pathAsString
    if(!subdirectory.exists){
      subdirectory.createDirectoryIfNotExists()
    }else{
      subdirectory.clear()
    }
    genRandom(N*2*runs,directoryName, "inputfile",bw)
    //    for(i <- 0 until N){
    //      genRandom(2*runs,s"inputfile${i}",bw)
    //    }
    var file = scala.io.Source.fromFile(directoryName + s"/inputfile_Decimal.txt").getLines().map(x=>x.toDouble).toArray
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
    val newfile1 = new File(directoryName +  "/outputfile.txt")
    val newfile2 = new File(directoryName +  "/outputfile_Decimal.txt")
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

  // generate the golden model files for multidimensional FFTs (based on a single input file)
  def genDFTInOutFile_MD(d:Int, N: Int, bw: Int, runs: Int): Unit ={ // use this to generate the dft/fft input output files
    val subdirectory = file"./FFT${N}_${d}D_GoldenModel" // name of directory to store results
    val directoryName = subdirectory.pathAsString
    if(!subdirectory.exists){
      subdirectory.createDirectoryIfNotExists()
    }else{
      subdirectory.clear()
    }
    val FFT_Out_Files = for(i <- 0 until d)yield{
      val newfile = new File(directoryName +  s"/FFT_stage${i}_outputfile.txt")
      val newfile2 = new File(directoryName +  s"/FFT_stage${i}_outputfile_Decimal.txt")
      val pw = new PrintWriter(newfile)
      val pw2 = new PrintWriter(newfile2)
      (pw,pw2)
    }
    val Trans_Out_Files = for(i <- 0 until d)yield{
      val newfile = new File(directoryName +  s"/Trans_stage${i}_outputfile.txt")
      val newfile2 = new File(directoryName +  s"/Trans_stage${i}_outputfile_Decimal.txt")
      val pw = new PrintWriter(newfile)
      val pw2 = new PrintWriter(newfile2)
      (pw,pw2)
    }
    val size = Math.pow(N,d).round.toInt
    genRandom(size*2*runs,directoryName, s"/inputfile",bw)
    var file = scala.io.Source.fromFile(directoryName+s"/inputfile_Decimal.txt").getLines().map(x=>x.toDouble).toArray
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
    val newfile1 = new File(directoryName + "/outputfile.txt")
    val newfile2 = new File(directoryName + "/outputfile_Decimal.txt")
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

  // generate golden models using these functions
  def main(args: Array[String]): Unit = {
    val N = 4 // size of dft/fft
    val r = 2 // radix of fft if applicable(if just dft then N = r)
    val w = 9 // width of streaming (just set the same as N, the fft width implementation is not yet done)
    val bw = 32 // precision
    val runs = 3 // number of runs for inputs/outputs to be generated
//  genDFTInOutFile(N,r,bw,runs) // for example
//    genDFTInOutFile_single_input_file(N,r,bw,runs) // for single input file
    val d = 3
    genDFTInOutFile_MD(d,N,bw, runs)
  }
}
