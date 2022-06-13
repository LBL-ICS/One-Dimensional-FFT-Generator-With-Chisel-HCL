package implementation
import ComplexNumbers._

object Permutations {
  // Perform the L permutation
  def L( l: Array[cmplx], n: Int, m: Int): Array[cmplx] = {
    var new_l = l.toArray //create array to store the new result
    var copy_l = l.toArray //create array to hold copy of input array
    var t = l.length / n // determine the number of stages
    var nn = n/m // determine second number for iteration cycles
    for(o <-  0 until t) { // for each stage
      for (i <- 0 until m) {
        for (j <- 0 until nn) {
          new_l(i * nn + j + o * n) = copy_l(j * m + i + o * n) // notice switch between i and j, and nn and m
        }
      }
    }
    new_l
  }
  // Perform the R permutation (is obtained from repeated use of L)
  def R(l: Array[cmplx], n: Int, m:Int): Array[cmplx] = {
    val t_val = (Math.log10(n)/Math.log10(m)).toInt //number of stages
    var new_l = l //create copy of input array. Will also hold the final results
    for(i <- 0 until t_val){ // for each stage
      // compute L
      val L_n = Math.pow(m, t_val - i).toInt // L_n decreases each cycle
      val L_m = m // m stays constant
      new_l = L(new_l, L_n, L_m) // compute the L permutation and store
    }
    new_l // return the permuted results
  }
  // this function is not used at the moment
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

  // function used for generating the twiddle factors,
  def T2(N: Int, r:Int): Array[Array[cmplx]] = {
    val t = ((Math.log10(N) / Math.log10(r)) - 1).toInt // calculate the total number of sets of twiddle matrices needed
    val T_mtrx_t = for(i <- 0 until t) yield { // initialize array to hold all sets of twiddle matrices
      val T_mini = for(j <- 0 until N) yield{
        cmplx(0,0)
      }
      T_mini
    }
    var t_mtrx = T_mtrx_t.map(_.toArray).toArray
    for(tt <- 0 until t) { // cycle through all sets
      for(k <- 0 until (N/Math.pow(r, t-tt).toInt)) { // do for each k index
        for(l <- 0 until N/(N/Math.pow(r, t-tt).toInt * r)){ // repeat for l times to fill up the twid matrix
          for (i <- 0 until r) { // each group has r elements
            // fill in the twiddle matrices with the Wnk values
            t_mtrx(tt)( (N/(N/Math.pow(r, t-tt).toInt * r))*r*k + r*l + i) = Wnk(N, (i * Math.pow(r, t-tt-1).toInt)*k)
          }
        }
      }
    }
    t_mtrx // return the twiddle matrices
  }

  // this used for computing twiddle factors involved in a mixed radix computation of the FFT
  def T2_rs(N: Int, r:Int, s:Int): Array[cmplx] = {
    val T_mtrx_t = for(j <- 0 until N) yield{ // initialize matrix that will hold the results
        cmplx(0,0)
    }
    var t_mtrx = T_mtrx_t.toArray
    for(k <- 0 until r) { // for each k value
      for (i <- 0 until s) { // there are a total of s terms in equation
        t_mtrx(k*s + i) = Wnk(N, i *k) // fill in the twiddle array with the Wnk values
      }
    }
    t_mtrx // return the twiddle factor array
  }
}
