package implementation
import ComplexNumbers._
import scala.reflect.ClassTag

object Permutations {
  // Perform the L permutation
  def L[T:ClassTag]( l: Array[T], n: Int, m: Int): Array[T] = {
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
  def R[T:ClassTag](l: Array[T], n: Int, m:Int): Array[T] = {
    val t_val = (Math.log10(n)/Math.log10(m)).round.toInt //number of stages
    var new_l = l //create copy of input array. Will also hold the final results
    for(i <- 0 until t_val){ // for each stage
      // compute L
      val L_n = Math.pow(m, t_val - i).toInt // L_n decreases each cycle
      val L_m = m // m stays constant
      new_l = L[T](new_l, L_n, L_m) // compute the L permutation and store
    }
    new_l // return the permuted results
  }
  // this function is not used at the moment
  def T(N: Int, r:Int): Array[Array[cmplx]] = {
    val t = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
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
    val t = ((Math.log10(N) / Math.log10(r)) - 1).round.toInt // calculate the total number of sets of twiddle matrices needed
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

  // splits the N inputs into N/w groups of w words
  def StreamingGroup[T:ClassTag](xi: Array[T], N: Int, w: Int): Array[Array[T]] = {
    val grouped_xi_t = for(i <- 0 until N/w) yield{ // split into N/w cycles
      val s_xi = for(j <- 0 until w) yield { // w elements each cycle
        xi(i*w + j)
      }
      s_xi
    }
    grouped_xi_t.map(_.toArray).toArray
  }

  // used for generating the read and store addresses and ports for the permutation streaming datapaths
  // at the momeent it only works for w that is divisor of N
  // N is size
  // w is width
  // r is radix
  // base_r is the radix which r is based on. It could be equal to r if r can not be split anymore
  // t just indicates whether to use the L permutation (0) or R permutation (1)
  def GenerateStreamingAddr(N: Int, w: Int, r: Int, base_r: Int, t:Int):Array[Array[((Int,Int),(Int,Int))]]  = {
    val base_indices = (for(i <- 0 until N)yield{i}).toArray // generate all the indices corresponding to the N elements
    val non_permuted = StreamingGroup[Int](base_indices, N, w) // group indices into streaming groups
    val permuted = StreamingGroup[Int](if(t == 0){L[Int](base_indices,N, r)}else{R[Int](base_indices,N,r)},N,w) // group permuted indices into streaming groups
    non_permuted.map(x => println(x.toList))
    println()
    permuted.map(x => println(x.toList))
    println()
    val unorganized_memory_addr = (for(i <- 0 until N/w) yield{ //find the mappings for each element ((read_address, read_port), (write_address, write_port))
      (for(j <- 0 until w) yield{
        var temp = ((0,0),(0,0))
        for(k <- 0 until N/w){
          for(l <- 0 until w){
            if(non_permuted(i)(j)==permuted(k)(l)){
              temp = ((i,j), (k,l))
            }
          }
        }
        temp
      }).toArray
    }).toArray
    var organize_into_unique_mem_stores = (for(i <- 0 until N/w) yield{(for(j <- 0 until w) yield{((0,0),(0,0))}).toArray}).toArray
    var index = 0
    for(i <- 0 until N/w){
      for(j <- 0 until N/w){
        for(k <- 0 until w){
          if(i == unorganized_memory_addr(j)(k)._2._1){
            // organize elements into a form that has unique read_ports for each group of w words
            organize_into_unique_mem_stores(i)(index) = unorganized_memory_addr(j)(k)
            index = (index + 1) % w
          }
        }
      }
    }
    var vertical_group_size = N/(w*r) // Vertical groups are considered to be ones that have the same read_port accross the vertical elements (see output)
    var horizontal_group_size = w/r // the w horizontal terms can be divided into groups of equivalent sizes.
    var rxr_group_size = r // horizontal group count = vertical group count
    if(w != r && r != N/w){
      var pow = r
      var is_pow = false
      while(pow != N){
        pow *= r
        if(pow == w) // trying to determine if w is a power of r
          is_pow = true
      }
      if(!is_pow){ // if w is not a power of r
        var increment_amount = (Math.log10(r)/Math.log10(base_r)).round.toInt
        var starting_point = N
        var power_set = 0
        var stage = 0
        var multiplier = 1
        var count = 0
        var stop_cond = false
        while(!stop_cond){
          if(starting_point == w){
            power_set = stage
            if(power_set == 0){
              multiplier = 1
            }else{
              multiplier = Math.pow(base_r, count-1).toInt
            }
            stop_cond = true
          }else{
            starting_point /= base_r
          }
          if(count == increment_amount - 1){
            if(stage == 0){
              stage = 1
            }else{
              stage += increment_amount
            }
          }
          count = (count + 1) % increment_amount
        }
        vertical_group_size = Math.pow(base_r, power_set).toInt * multiplier
        rxr_group_size = (N/w)/(vertical_group_size)
        horizontal_group_size = w/(rxr_group_size)
      }else{
        vertical_group_size = N/(w*r*r)
        horizontal_group_size = w/(r*r)
        rxr_group_size = r*r
      }
    }
    var arrange_into_4D = (for(i <- 0 until rxr_group_size) yield{
      val t1 = (for(j <- 0 until rxr_group_size) yield{
        val t2 = (for(k <- 0 until vertical_group_size) yield{
          val t3 = (for(l <- 0 until horizontal_group_size) yield{
            organize_into_unique_mem_stores(j*vertical_group_size+k)(i*horizontal_group_size+l)
          }).toArray
          t3
        }).toArray
        t2
      }).toArray
      t1
    }).toArray
    for(i <- 0 until rxr_group_size){
      arrange_into_4D(i) = circular_shift[Array[Array[((Int, Int),(Int, Int))]]](arrange_into_4D(i),i)
    }
    for(i <- 0 until rxr_group_size){
      for (j <- 0 until rxr_group_size){
        for (k <- 0 until vertical_group_size){
          for(l <- 0 until horizontal_group_size){
            organize_into_unique_mem_stores(j*vertical_group_size+k)(i*horizontal_group_size+l) = arrange_into_4D(i)(j)(k)(l)
          }
        }
      }
    }
    unorganized_memory_addr.map(x => println(x.toList))
    println("pairs of read, store (address, port). Horizontal terms are the w inputs/outputs per cycle")
    organize_into_unique_mem_stores.map(x => println(x.toList))
    println()
    organize_into_unique_mem_stores
  }

  def circular_shift[T:ClassTag](xi: Array[T], amnt: Int): Array[T] = {
    val cpy = xi // hold first element
    for(i <- 0 until amnt){
      val tmp = xi(0)
      for(j <- 0 until xi.length - 1){
        cpy(j) = cpy(j+1) // shift all other elements up
      }
      cpy(xi.length-1) = tmp // place first element at the end
    }
    cpy
  }
}
