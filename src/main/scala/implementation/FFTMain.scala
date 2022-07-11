package implementation
import FFT._
import ComplexNumbers._
import Permutations._

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Random
import java.io.PrintWriter

object FFTMain {
  def main(args: Array[String]): Unit = {

    // This is for testing the FFT for any radix r
    val n_size = 6  // the size of the DFT
    val r = 3 //the radix r specification

    // creating n_size random complex numbers for input into FFT
    var index = 1
    val xi = for(i <- 0 until n_size) yield{
//      val start = -10
//      val end = 10
//      val rnd = new Random()
//      val value = start + rnd.nextInt((end-start) + 1)
//      val value2 = start + rnd.nextInt((end-start) + 1)
      val num = cmplx(index, index + 1)
      index += 2
      num
    }
    //val xi = List(cmplx(138828.10499857602, 173601.66334722503),cmplx(-410110.178645231,-401463.419424068),cmplx(86280.417921146,-492295.84887818), cmplx(324183.590607955,-466084.993285151), cmplx(-379932.300333563,400780.130324218), cmplx(368398.14913881,420746.270209735), cmplx(352380.284566766,-120056.103771629), cmplx(216285.915855303,-183953.351843056), cmplx(496446.93387681,97079.355405773))
    println("-----------------------------------------------")
    // Use the FFT algorithm of radix r to solve the DFT of n_size
    println("hello")
//    val soln2 = FFT_r(n_size, r, xi.toArray).map(x=>println(x.print_complex)).toSeq
//    val pw = new PrintWriter("FFT_out.txt")
//    soln2.map(pw.println(_))
//    pw.close()

    println("-----------------------------------------------")
    // Compare previous solution with the normal DFT of n_size computation
    val soln5 = DFT_compute(DFT_gen(n_size), xi.toArray, n_size).map(x=>println(x.print_complex)).toSeq
    soln5.map(println(_))

    println("-----------------------------------------------")
    val twids = T2(4,2)
    println("-----------------------------------------------")
    twids(0).toList.map(x=>x.print_complex)
    println("-----------------------------------------------")
    //twids(1).toList.map(x=>x.print_complex)
//
//    // This part is for testing the mixed radix implementation of the DFT
//    // We want to split the DFT_n into the DFT_ns and DFT_nr
//    // where ns and nr are integers whose radix is s and r respectively.
//    val Nw = 27 // size of the DFT
//
//    // generating some input complex numbers
//    val ll = for(i <- 0 until Nw) yield{
//      cmplx(i,0)
//    }
//    // We use the FFT mixed radix algorithm to solve the DFT of size Nw
//    // first input is the size of the DFT
//    // second input is the nr and third input is the ns
//    // where nr and ns are specified such that ns*nr = Nw
//    // fourth input radix of nr and fifth input is the radix of ns
//    // the sixth input is the the input array of size Nw
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
  }
}
