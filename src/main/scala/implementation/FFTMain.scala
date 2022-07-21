package implementation
import FFT._
import ComplexNumbers._
import Permutations._
import ChiselFFT.Testing._
import ChiselFFT.FFTDesigns._
import chisel3.getVerilogString

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Random
import java.io.PrintWriter

object FFTMain {
  def main(args: Array[String]): Unit = {
//    val pw3 = new PrintWriter("FFT_sr_v2.v")
//    pw3.println(getVerilogString(new FFT_sr_v2(16,2,16,32)))
//    pw3.close()
    // This is for testing the FFT for any radix r
    val n_size = 9  // the size of the DFT
    val r = 3 //the radix r specification

    // creating n_size random complex numbers for input into FFT
    var index = 1
//    val xi = for(i <- 0 until n_size) yield{
////      val start = -10
////      val end = 10
////      val rnd = new Random()
////      val value = start + rnd.nextInt((end-start) + 1)
////      val value2 = start + rnd.nextInt((end-start) + 1)
//      val num = cmplx(index, index + 1)
//      index += 2
//      num
//    }
//    val xi = List(cmplx(-196195.372982558, 454325.73841146805),cmplx(273832.102471305,7787.559747425001),cmplx(-60081.945797721004,-363411.78263670404), cmplx(333698.46843567304,383057.76542950503), cmplx(-190371.948937075,472160.839080743), cmplx(-303262.405798048,445584.66593397), cmplx(347527.468102011,146269.011642233), cmplx(-383476.124726788,308621.58870464005), cmplx(-354129.615612036,151632.757725177))
//    println("-----------------------------------------------")
//    // Use the FFT algorithm of radix r to solve the DFT of n_size
//    println("hello")
////    val soln2 = FFT_r(n_size, r, xi.toArray).map(x=>println(x.print_complex)).toSeq
////    val pw = new PrintWriter("FFT_out.txt")
////    soln2.map(pw.println(_))
////    pw.close()
//
//    println("-----------------------------------------------")
//    // Compare previous solution with the normal DFT of n_size computation
//    val soln5 = DFT_compute(DFT_gen(n_size), xi.toArray, n_size).map(x=>println(x.print_complex)).toSeq
//    soln5.map(println(_))
//
//    println("-----------------------------------------------")
//    val list_of_perms = T2(8,2)
//    list_of_perms.map(x=>println(x.map(_.print_complex)))
//    val initial_perm = for(i <- 0 until 8) yield{
//      cmplx(1,0)
//    }
//    val adjusted_list = for(i <- 0 until 3) yield{
//      if(i == 0){
//        initial_perm.toArray
//      }else{
//        list_of_perms(i-1)
//      }
//    }
//    println("================= complex nums=====================")
//    val twids = T2(16,2)
//    twids(1).map(x=>x.print_complex)
//    println("-----------------------------------------------")
////    adjusted_list.map(x=>println(x.map(_.print_complex)))
//    twids(2).toList.map(x=>x.print_complex)
//    println("-----------------------------------------------")
//    for(i <- 0 until 8){
//      println(s"Here is FFT latency ${getFFTLatency(Math.pow(2,i+2).round.toInt,2,Math.pow(2,i+2).round.toInt,32)}")
//    }
//    println(s"Here is FFTmr latency ${getFFTLatency(2, 2,2,32)}")
    println("-----------------------------------------------")
    val ww = getw2(4,144,16,9,3)
    println(s"ww: ${ww}")
      var count = 0
    val Nt = 72
    val wt = ww
    val nr = 8
//    val ut = GenerateStreamingMapping(Nt,wt,nr,nr,2)

//    val Nf = 108
//    val wf = 4
//    val nf = 9
//    val ut = GenerateStreamingMappingV2(Nf,wf,nf,nf,0)


//    for(i <- 0 until Nf/wf){
//      for(j <- 0 until wf){
//        for(k <- 0 until wf){
//          if(ut(i)(j)._2._2 == ut(i)(k)._2._2 && j != k){
//            count += 1
//            println(s"${(i,j)} with ${(i,k)}")
//          }
//        }
//      }
//    }

//    for(i <- 0 until Nt/wt){
//      for(j <- 0 until wt){
//        for(k <- 0 until wt){
//          if(ut(i)(j)._2._2 == ut(i)(k)._2._2 && j != k){
//            count += 1
//            println(s"${(i,j)} with ${(i,k)}")
//          }
//        }
//      }
//    }


//    if(count >0){
//      println("cases found")
//    }else{
//      println("cases not found")
//    }

    println("-------------epic-------------------------")
//
//    val listlol = for(i <- 0 until 3 )yield{
//      for(j <- 0 until 2) yield{
//        j
//      }
//    }
//    val listlol2 = listlol.flatten
//    var cnt = 0
//    var stopped_index = 0
//    val modded_list = for(i <- 0 until 2) yield{
//      for(j <- 0 until 3) yield{
////        (j,listlol2(i*9+ j))
//        (i,j)
//      }
//    }
//    val modded_list2 = modded_list.flatten
//    val modded_list3 = for(i <- 0 until 3) yield{
//      for(j <- 0 until 2) yield{
//        modded_list2(i*2+ j)
//
//      }
//    }
//
//    val modded_list4 = for(i <- 0 until 9) yield{
//      for(j <- 0 until 4) yield{
//        //        (j,listlol2(i*9+ j))
//        (i,j)
//      }
//    }
//    val modded_list5 = modded_list4.flatten
//    val modded_list6 = for(i <- 0 until 4) yield{
//      for(j <- 0 until 9) yield{
//        modded_list5(i*9+ j)
//      }
//    }
//
//    val mem_ports = modded_list6.map(_.map(_._2))
//    val mem_addr = modded_list6.map(_.map(_._1))
//    val mem_enables = mutable.ArrayBuffer[Array[Array[Boolean]]]()
//    val mem_ports_adj = mutable.ArrayBuffer[Array[Array[Int]]]()
//    val mem_addr_adj = mutable.ArrayBuffer[Array[Array[Int]]]()
//    val mem_input_pass = mutable.ArrayBuffer[Array[Array[Int]]]()
//    for(i <- 0 until 4){
//      val temp = mutable.ArrayBuffer[Array[Boolean]]()
//      val temp_ports1 = mutable.ArrayBuffer[Array[Int]]()
//      val temp_addr1 = mutable.ArrayBuffer[Array[Int]]()
//      val temp_inp1 = mutable.ArrayBuffer[Array[Int]]()
//      for(j  <- 0 until 36/9){
//        val temp2 = mutable.ArrayBuffer[Boolean]()
//        val temp_ports2 = mutable.ArrayBuffer[Int]()
//        val temp_addr2 = mutable.ArrayBuffer[Int]()
//        val temp_inp2 = mutable.ArrayBuffer[Int]()
//        for(k <- 0 until 9){
//          if(modded_list6(j)(k)._2 == i){
//            temp2 += true
//            temp_ports2 += modded_list6(j)(k)._2
//            temp_addr2 += modded_list6(j)(k)._1
//            temp_inp2 += k
//          }
//        }
//        for(k <- temp2.length until (9.0/4.0).ceil.toInt){
//          temp2 += false
//          temp_ports2 += 0
//          temp_addr2 += 0
//          temp_inp2 += 0
//        }
//        temp += temp2.toArray
//        temp_ports1 += temp_ports2.toArray
//        temp_addr1 += temp_addr2.toArray
//        temp_inp1 += temp_inp2.toArray
//      }
//      mem_enables += temp.toArray
//      mem_ports_adj += temp_ports1.toArray
//      mem_addr_adj += temp_addr1.toArray
//      mem_input_pass += temp_inp1.toArray
//    }
//
//    modded_list4.map(x=>println(x.toList))
//    println()
//    modded_list6.map(x=>println(x.toList))
//    println("_--_--_-_--_-_---__-----__--___--__-")
//    for(i <- 0 until 4) {
//      println()
//      mem_enables(i).map(x => println(x.toList))
//    }
//    println("_--_--_-_--_-_---__-----__--___--__-")
//    for(i <- 0 until 4) {
//      println()
//      mem_ports_adj(i).map(x => println(x.toList))
//    }
//    println("_--_--_-_--_-_---__-----__--___--__-")
//    for(i <- 0 until 4) {
//      println()
//      mem_addr_adj(i).map(x => println(x.toList))
//    }
//    println("_--_--_-_--_-_---__-----__--___--__-")
//    for(i <- 0 until 4) {
//      println()
//      mem_input_pass(i).map(x => println(x.toList))
//    }
//    println("_--_--_-_--_-_---__-----__--___--__-")
//    // how to find the streaming width pair for the mixed radix streaming cases is the main problem
//    val w1 = 2 // how to find the corresponding size
//    findthew2(18,6,2,9,2,3)
//    def generate_streaming_possibilities(N: Int, r: Int): Array[(Int, Int)] ={
//      var index = 0
//      val lists = mutable.ArrayBuffer[(Int, Int)]()
//      for(i <- r until N by r){
//        if((N.toDouble/i.toDouble).isWhole){
//          val intint = (i,index)
//          lists += intint
//        }
//        index += 1
//      }
//      if(r == N){
//        val intint = (r,index)
//        lists += intint
//      }
//      lists.toArray
//    }
//    println(generate_streaming_possibilities(32,2).toList)
//    println(generate_streaming_possibilities(27,3).toList)
//    println(generate_streaming_possibilities(36,9).toList)
//    println(generate_streaming_possibilities(36,4).toList)
//    println(generate_streaming_possibilities(96,32).toList)
//    println(generate_streaming_possibilities(96,2).toList)
//
//
//    def getw2(w1: Int, N: Int, nr: Int, ns: Int, s: Int): Unit = {
//      var w2 = 0
//      if(w1 == 2){
//        w2 = 3
//      }else{
//        if(w1 < nr) {
//          if(ns != s) {
//            val generated_mappings = Permutations.generate_streaming_possibilities(ns, s)
//            var counter = 0
//            while (generated_mappings(counter) < w1) {
//              counter += 1
//            }
//            w2 = generated_mappings(counter)
//          }else{
//            val generated_mappings = Permutations.generate_streaming_possibilities(N, s)
//            var counter = 0
//            while (generated_mappings(counter) < w1) {
//              counter += 1
//            }
//            w2 = generated_mappings(counter)
//          }
//        }else{
//          val generated_mappings2 = Permutations.generate_streaming_possibilities(ns,s)
//          var flag = false
//          for(i <- 0 until generated_mappings2.length){
//            if(generated_mappings2(i) > w1 && (N.toDouble/generated_mappings2(i).toDouble).isWhole){
//              flag = true
//              println("The answer")
//              println(generated_mappings2(i))
//            }
//          }
//          if(!flag) {
//            val generated_mappings = Permutations.generate_streaming_possibilities(N, ns)
//            var counter = 0
//            while (generated_mappings(counter) < w1) {
//              counter += 1
//            }
//            w2 = generated_mappings(counter)
//          }else{
//            val generated_mappings = Permutations.generate_streaming_possibilities(ns, s)
//            var counter = 0
//            while (generated_mappings(counter) < w1 || !(N.toDouble/generated_mappings(counter).toDouble).isWhole) {
//              println(w1)
//              counter += 1
//            }
//            w2 = generated_mappings(counter)
//            println(generated_mappings(0))
//          }
//        }
//      }
//      println(s"the w2 will be: ${w2}") // we must not do 27
//    }
//
//    getw2(4,12,4, 3,3)
//    getw2(2,36,4, 9,3)
//
//
//    println("-_-_____-----------_____________")
//    def findthew2(N: Int, win: Int, nr: Int, ns: Int, r: Int, s: Int): Int = {
//      // the mr cases splits up the dft into ns fft nrs and nr fft ns
//      if(win < nr){
//        val modules_used = win/r
//        val modules_out = modules_used * s
//        println(modules_out)
//      }else {
//        if (win == 2) {
//          val wout = 3
//          println(wout)
//        } else {
//          val modules_used = win / nr
//          val total_modules = N / nr
//          val other_modules = N / ns
//          val ratio = N / win
//          println(modules_used)
//          println(total_modules)
//          println(other_modules)
//          println(ratio)
//          val pow = (Math.log10(ratio) / Math.log10(3)).round.toInt
//          println(pow)
//          val ratio2 = Math.pow(2, pow).round.toInt
//          val wout = N / ratio2
//          println(wout)
//        }
//      }
//      1
//    }

    //    listlol.map(x=>println(x.toList))
    //    println(listlol.flatten.toList)
    //    modded_list3.map(x=>println(x.toList))
    //    println()

    //testing_streaming(16,2,4,32)
    //testing_iterations(8,2,1,2,32) // iterative2 -- testing for N=w only
    // if it is the case that depth is equal to the number of stages, then we can simply consider the original implementation whether it be the full-sized FFT or the streaming width FFT
    // in the case that that the streaming width is equal to the full size of N, then we can simply consider aobut implementing the full-sized FFT or the full-streaming width iterative design.
    //twids(1).toList.map(x=>x.print_complex)
//
//    // This part is for testing the mixed radix implementation of the DFT
//    // We want to split the DFT_n into the DFT_ns and DFT_nr
//    // where ns and nr are integers whose radix is s and r respectively.
    val Nw = 27 // size of the DFT
//
//    // generating some input complex numbers
    val ll = for(i <- 0 until Nw) yield{
      cmplx(i+1,0)
    }
//    // We use the FFT mixed radix algorithm to solve the DFT of size Nw
//    // first input is the size of the DFT
//    // second input is the nr and third input is the ns
//    // where nr and ns are specified such that ns*nr = Nw
//    // fourth input radix of nr and fifth input is the radix of ns
//    // the sixth input is the the input array of size Nw
//    println("mixedradix ffft")
//    val soln3 = FFT_mr(Nw,3,9,3,9, ll.toArray)
//    soln3.map(_.print_complex)
//    println("-------------------------------")
//    // Let us compute the same DFT using the normal DFT matrix for comparison
//    val soln4 = DFT_compute(DFT_gen(Nw),ll.toArray,Nw).map(_.print_complex)
//
//    //    val k = T2_rs(6,3,2)
//    //    k.map(_.print_complex)
//    //    Wnk(6,1).print_complex
//    //    Wnk(6,2).print_complex
//    println("testing generation of streaming address")
//    val N = 64  // the size of the DFT
//    val rr = 4 //the radix r specification
//    val base_r = 2
//    val w = 8
//    val t = 1
//    // first input is size of FFT
//    // second input is size of w
//    // third input radix
//    // fourth input is base radix for which radix r is based on
//    // fifth input is to specify permutation type, 0 for stride-m (L) and 1 for digit reversal (R)
//    // at the moment, the w must be a divisor of N and multiple of the radix
//    var mappings = GenerateStreamingMapping(N, w,rr,base_r,t)
//    mappings = Mapping_Sort(mappings,0)
//    val M0_access = (for(i <- 0 until N/w)yield{
//      val M0_slice = (for(j <- 0 until w)yield{
//        mappings(i)(j)._2._1
//      }).toArray
//      M0_slice
//    }).toArray
//    M0_access.map(x=>println(x.toList))
//    var mem_map = (for(i <- 0 until N/w) yield{
//      val temp = (for(j <- 0 until w) yield{
//        mappings(i)(j)._2._2
//      }).toArray
//      temp
//    }).toArray
//    println()
//    mem_map.map(x=>println(x.toList))

    println("_--_--_-_--_-_---__-----__--___--__-")
//    FFT_mr(6,3,2,3,2,(for(i <- 0 until 6)yield{cmplx(i,0)}).toArray)
    testing_streaming_mr(6,2,3,2,3,2,32)
  }
}
