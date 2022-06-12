package implementation
import ComplexNumbers._

object Permutations {
  def L( l: Array[cmplx], n: Int, m: Int): Array[cmplx] = {
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
    new_l
  }

  def R(l: Array[cmplx], n: Int, m:Int): Array[cmplx] = {
    val t_val = (Math.log10(n)/Math.log10(m)).toInt
    var new_l = l
    for(i <- 0 until t_val){
      val L_n = Math.pow(m, t_val - i).toInt
      val L_m = m
      new_l = L(new_l, L_n, L_m)
    }
    new_l
  }

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

  def T2(N: Int, r:Int): Array[Array[cmplx]] = {
    val t = ((Math.log10(N) / Math.log10(r)) - 1).toInt
    val T_mtrx_t = for(i <- 0 until t) yield {
      val T_mini = for(j <- 0 until N) yield{
        cmplx(0,0)
      }
      T_mini
    }
    var t_mtrx = T_mtrx_t.map(_.toArray).toArray
    for(tt <- 0 until t) { // cycle through all sets of groups
      for(k <- 0 until (N/Math.pow(r, t-tt).toInt)) { // repeat for l times to fill up the twiddle matrix
        for(l <- 0 until N/(N/Math.pow(r, t-tt).toInt * r)){ // do for each k index
          for (i <- 0 until r) { // each group has r elements
            t_mtrx(tt)( (N/(N/Math.pow(r, t-tt).toInt * r))*r*k + r*l + i) = Wnk(N, (i * Math.pow(r, t-tt-1).toInt)*k)
          }
        }
      }
    }
    t_mtrx
  }
}
