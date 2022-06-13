package implementation
import FFT._
import ComplexNumbers._
import Permutations._
import scala.util.Random

object FFTMain {
  def main(args: Array[String]): Unit = {

    // This is for testing the FFT for any radix r
    val n_size = 25 // the size of the DFT
    val r = 5 //the radix r specification

    // creating n_size random complex numbers for input into FFT
    val xi = for(i <- 0 until n_size) yield{
      val start = -10
      val end = 10
      val rnd = new Random()
      val value = start + rnd.nextInt((end-start) + 1)
      cmplx(value, 0)
    }
    println("-----------------------------------------------")
    // Use the FFT algorithm of radix r to solve the DFT of n_size
    val soln2 = FFT_r(n_size, r, xi.toArray).map(complex_mag(_)).toSeq
    soln2.map(println(_))

    println("-----------------------------------------------")
    // Compare previous solution with the normal DFT of n_size computation
    val soln5 = DFT_compute(DFT_gen(n_size), xi.toArray, n_size).map(complex_mag(_)).toSeq
    soln5.map(println(_))

    println("-----------------------------------------------")

    // This part is for testing the mixed radix implementation of the DFT
    // We want to split the DFT_n into the DFT_ns and DFT_nr
    // where ns and nr are integers whose radix is s and r respectively.
    val Nw = 108 // size of the DFT

    // generating some input complex numbers
    val ll = for(i <- 0 until Nw) yield{
      cmplx(i,0)
    }
    // We use the FFT mixed radix algorithm to solve the DFT of size Nw
    // first input is the size of the DFT
    // second input is the nr and third input is the ns
    // where nr and ns are specified such that ns*nr = Nw
    // fourth input radix of nr and fifth input is the radix of ns
    // the sixth input is the the input array of size Nw
    val soln3 = FFT_mr(Nw,4,27,2,3, ll.toArray)
    soln3.map(_.print_complex)
    println("-------------------------------")
    // Let us compute the same DFT using the normal DFT matrix for comparison
    val soln4 = DFT_compute(DFT_gen(Nw),ll.toArray,Nw).map(_.print_complex)

    //    val k = T2_rs(6,3,2)
    //    k.map(_.print_complex)
    //    Wnk(6,1).print_complex
    //    Wnk(6,2).print_complex
  }
}
