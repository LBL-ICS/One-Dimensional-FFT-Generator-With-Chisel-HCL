package ChiselFFT
import IEEEConversions.FPConvert._
import FloatingPointDesigns.FPArithmetic._
import ComplexModules.FPComplex._
import chisel3.tester.RawTester.test
import chisel3._
import chisel3.tester._
import ChiselFFT.FFTDesigns._
import implementation.FFT._
import implementation.ComplexNumbers._
import implementation._
import GoldenModels.FFT_GoldenModels._

import java.io.PrintWriter
import scala.util.Random
import GoldenModels.FFT_GoldenModels._
import Chisel.{isPow2, log2Ceil}

import scala.collection.mutable

object Testing {
  def testing_streaming_mr(N: Int, nr: Int, ns: Int, r: Int, s: Int, w: Int, bw: Int): Unit ={
    test(new FFT_mr_v2_streamingv2(N,nr,ns,r,s, w, bw)){c=>
      val w1 = w // this will be the input and output width
      var w2 = Permutations.getw2(w1,N,nr,ns,s)
      val CMultLatency = 2
      val T_L = CMultLatency
      var fftlatency1 = 0
      var fftlatency2 = 0
      val perm_latency1 = (N/w1)*2
      if(w1 < nr && w2 < ns){
        fftlatency1 = getfftstreamedlatency(nr,r,w1,bw)
        fftlatency2 = getfftstreamedlatency(ns,s,w2,bw)
      }else if(w1 < nr && w2 >= ns){
        fftlatency1 = getfftstreamedlatency(nr,r,w1,bw) // in this case the fft1 is reduced streaming width
        fftlatency2 = getFFTLatency(ns,s,ns,bw)
      }else if(w1 >= nr && w2 < ns){
        fftlatency1 = getFFTLatency(nr,r,nr,bw) // the fft1 is full streaming width
        fftlatency2 = getfftstreamedlatency(ns,s,w2,bw)
      }else if(w1 >= nr && w2 >= ns){
        fftlatency1 = getFFTLatency(nr,r,nr,bw) // this function should also give the DFT latency as well if we set the nr==r
        fftlatency2 = getFFTLatency(ns,s,ns,bw)
      }
      val total_latency = perm_latency1*3 + T_L + fftlatency1 + fftlatency2 + 1
      c.io.in_ready.poke(true.B)
      for(i <- 0 until N/w1){
        for(j <- 0 until w1){
          c.io.in(j).Re.poke(convert_string_to_IEEE_754((i*w1 + j + 1).toDouble.toString, bw).U)
          c.io.in(j).Im.poke(convert_string_to_IEEE_754("0.0", bw).U)
        }
        c.clock.step(1)
      }
      c.io.in_ready.poke(false.B)
      for(i <- 0 until total_latency + (N/w1) - 1 - ((N/w1) - 1)){
        println(s"Clock Cycle: ${(N/w1) + i}")
        println(s"valid: ${c.io.out_validate.peek().litValue}")
        for(j <- 0 until w1) {
          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, bw)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue,bw)}")
        }
        println("Actual Output_--_--_-_--_-_---__-----__--___--__-")
        for(j <- 0 until w1){
          println(s"Test Real Output: ${convert_long_to_float(c.io.out_test(j).Re.peek().litValue, bw)}")
          println(s"Test Imaginary Output: ${convert_long_to_float(c.io.out_test(j).Im.peek().litValue,bw)}")
        }
        println("Permutation Stage1------------------------------------")
        for(j <- 0 until w1){
          println(s"Test Real Output: ${convert_long_to_float(c.io.out_test1(j).Re.peek().litValue, bw)}")
          println(s"Test Imaginary Output: ${convert_long_to_float(c.io.out_test1(j).Im.peek().litValue,bw)}")
        }
        println("FFT1------------------------------------")
        for(j <- 0 until w2){
          println(s"Test Real Output: ${convert_long_to_float(c.io.out_test2(j).Re.peek().litValue, bw)}")
          println(s"Test Imaginary Output: ${convert_long_to_float(c.io.out_test2(j).Im.peek().litValue,bw)}")
        }
        println("Permutation Stage2------------------------------------")
        for(j <- 0 until w2){
          println(s"Test Real Output: ${convert_long_to_float(c.io.out_test3(j).Re.peek().litValue, bw)}")
          println(s"Test Imaginary Output: ${convert_long_to_float(c.io.out_test3(j).Im.peek().litValue,bw)}")
        }
        println("Twiddle Factors------------------------------------")
        for(j <- 0 until w2){
          println(s"Test Real Output: ${convert_long_to_float(c.io.out_test4(j).Re.peek().litValue, bw)}")
          println(s"Test Imaginary Output: ${convert_long_to_float(c.io.out_test4(j).Im.peek().litValue,bw)}")
        }
        println("FFT2------------------------------------")
        for(j <- 0 until w1){
          println(s"Test Real Output: ${convert_long_to_float(c.io.out_test5(j).Re.peek().litValue, bw)}")
          println(s"Test Imaginary Output: ${convert_long_to_float(c.io.out_test5(j).Im.peek().litValue,bw)}")
        }
        println("Permutation Stage3------------------------------------")
        c.clock.step(1)
      }
    }
  }

  def testing_iterations(N: Int, r: Int, depth: Int, width: Int, bw: Int ): Unit = {
    test(new FFT_sr_v2_streaming_and_iterative2(N,r,depth,width,bw)){c=>
      val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
      var mult_count = 0
      for(i <- 0 until r-1){
        for(j <- 0 until r-1){
          val n  = DFTr_Constants(i+1)(j+1)
          val c1 = FFT.isReducable(n.re.abs)
          val c2 = FFT.isReducable(n.im.abs)
          if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
            mult_count += 1
          }
        }
      }
      val inner_square_size = r-1
      var last_ind = r-1
      var start_ind = 1
      val inner_inner_square_size = inner_square_size/2
      val ind_sq = for(i <- 0 until inner_inner_square_size)yield{
        val row = for(j <- 0 until inner_inner_square_size)yield{
          if(((i+1)*(j+1) % r) > r/2){((i+1)*(j+1) % r) - r}else{((i+1)*(j+1) % r)}
        }
        row
      }
      ind_sq.map(x=>println(x))
      val ind_sq_unique = mutable.ArrayBuffer[Int]()
      val ind_sq_unique_address = mutable.ArrayBuffer[(Int, Int)]()
      val cases = mutable.ArrayBuffer[(Int,Int)]()
      val cases_addr = mutable.ArrayBuffer[(Int,Int)]()
      for(i <- 0 until inner_inner_square_size){
        for(j <- 0 until inner_inner_square_size){
          if(!ind_sq_unique.contains(ind_sq(i)(j).abs)){
            ind_sq_unique += ind_sq(i)(j)
            val temp = (i,j)
            ind_sq_unique_address += temp
          }
          if(!cases.contains((ind_sq(i)(j).abs, j))){
            val temp = (ind_sq(i)(j).abs, j)
            cases += temp
            val temp2 = (i,j)
            cases_addr += temp2
          }
        }
      }
      println(ind_sq_unique.zipWithIndex.toList)
      println(ind_sq_unique_address.toList)
      println(cases.toList)
      println(cases_addr.toList)
      def returnMapping2(num: Int, arr: Array[(Int,Int)]):(Int,Int)={//for multiply access values
        var returnval = (0,0)
        for(i <- 0 until arr.length){
          if(num == arr(i)._1){
            returnval = ind_sq_unique_address(i)
          }
        }
        returnval
      }
      val cases_addr_adjusted = cases.map(x=>returnMapping2(x._1.abs,ind_sq_unique.zipWithIndex.toArray))
      println(cases_addr_adjusted.toList)

      def returnMapping3(num: (Int, Int), arr: Array[(Int,Int)]):Int={
        var returnval = 0
        for(i <- 0 until arr.length){
          if(num == arr(i)){
            returnval = i
          }
        }
        returnval
      }
      val new_adj_case_sq = ind_sq.map(x=>x.zipWithIndex.map(y=>(returnMapping3((y._1.abs,y._2), cases.toArray),y._1 < 0)))
      new_adj_case_sq.map(x=>println(x.toList))
      var mult_layer_count1 = 0
      var subadd_layer_count1 = 0
      val CMultLatency = 2
      val CAddLatency = 1
      val initial_Latency = 1
      val multlayerlatency = if(mult_layer_count1>0){1}else{0}
      val subaddlayerlatency = if(subadd_layer_count1 > 0){1}else{0}
      val multadd_latency = if(inner_inner_square_size>1){((Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt)yield{(inner_inner_square_size/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)}else{0}
      val end_latency = 1
      var DFT_latency = initial_Latency + multlayerlatency + subaddlayerlatency + multadd_latency + end_latency
      if(r == 2){
        DFT_latency = 1
      }
      println(s"the dft latency: ${DFT_latency}")
      val DFTs_per_stage = width/r
      val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
      val number_of_stages_depth = depth // ideally has to be a divisor of the number of stages
      val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
      var T_L = CMultLatency
      val Twid_latency = (TotalStages+1) * T_L
      val Perm_latency = (N/width)*2
      val Total_Latency = (depth)*T_L + (depth) * DFT_latency + (depth) * Perm_latency + 1 // time for first output val, then wait (N/w) - 1 cycles
      val Latency_exclude_first = (depth)*T_L + (depth) * DFT_latency + (depth) * Perm_latency + 1
      var overrall_latency = 0
      val increment_points = mutable.ArrayBuffer[Int]() // iteration cnt  - 1
      for(i <- 0 until (number_of_stages/number_of_stages_depth)){ // the total number of iterations (number of stages depth must be a divisor of number of stages)
        if(i == 0){
          increment_points += Latency_exclude_first-1 + (N/width)// must be incremented by this point remember the count starts from zero
          overrall_latency += Latency_exclude_first-1 + (N/width) // some cycles to take into account the propagation
        }else{
          increment_points += overrall_latency + Latency_exclude_first - 1 + (N/width)
          overrall_latency += Latency_exclude_first - 1 + (N/width)
        }
      }
      println("increment point check")
      increment_points.map(x=>println(x))
      increment_points += overrall_latency + Perm_latency
      overrall_latency += Perm_latency
      overrall_latency += (N/width)
      println(s"The Overall Latency of Computation: ${overrall_latency}")
      c.io.in_ready.poke(true.B)
      for(i <- 0 until N/width){
        for(j <- 0 until width){
          c.io.in(j).Re.poke(convert_string_to_IEEE_754((i*width + j + 1).toDouble.toString, bw).U)
          c.io.in(j).Im.poke(convert_string_to_IEEE_754(("0.0"), bw).U)
        }
        c.clock.step(1)
      }
      c.io.in_ready.poke(false.B)
      for(i <-0 until overrall_latency - ((N/width)-1)){
        println(s"clock cycle: ${(N/width)+i}")
        println(s"validate: ${c.io.out_validate.peek().litValue}")
        for(j <- 0 until width){
          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, bw)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue, bw)}")
        }
        c.clock.step(1)
        println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_")
      }
    }
  }
  def testing_iterations2(N: Int, r: Int, depth: Int, width: Int, bw: Int ): Unit = {
    test(new FFT_sr_v2_iterative2(N,r,depth,width,bw)){c=>
      val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
      var mult_count = 0
      for(i <- 0 until r-1){
        for(j <- 0 until r-1){
          val n  = DFTr_Constants(i+1)(j+1)
          val c1 = FFT.isReducable(n.re.abs)
          val c2 = FFT.isReducable(n.im.abs)
          if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
            mult_count += 1
          }
        }
      }
      val inner_square_size = r-1
      var last_ind = r-1
      var start_ind = 1
      val inner_inner_square_size = inner_square_size/2
      val ind_sq = for(i <- 0 until inner_inner_square_size)yield{
        val row = for(j <- 0 until inner_inner_square_size)yield{
          if(((i+1)*(j+1) % r) > r/2){((i+1)*(j+1) % r) - r}else{((i+1)*(j+1) % r)}
        }
        row
      }
      ind_sq.map(x=>println(x))
      val ind_sq_unique = mutable.ArrayBuffer[Int]()
      val ind_sq_unique_address = mutable.ArrayBuffer[(Int, Int)]()
      val cases = mutable.ArrayBuffer[(Int,Int)]()
      val cases_addr = mutable.ArrayBuffer[(Int,Int)]()
      for(i <- 0 until inner_inner_square_size){
        for(j <- 0 until inner_inner_square_size){
          if(!ind_sq_unique.contains(ind_sq(i)(j).abs)){
            ind_sq_unique += ind_sq(i)(j)
            val temp = (i,j)
            ind_sq_unique_address += temp
          }
          if(!cases.contains((ind_sq(i)(j).abs, j))){
            val temp = (ind_sq(i)(j).abs, j)
            cases += temp
            val temp2 = (i,j)
            cases_addr += temp2
          }
        }
      }
      println(ind_sq_unique.zipWithIndex.toList)
      println(ind_sq_unique_address.toList)
      println(cases.toList)
      println(cases_addr.toList)
      def returnMapping2(num: Int, arr: Array[(Int,Int)]):(Int,Int)={//for multiply access values
        var returnval = (0,0)
        for(i <- 0 until arr.length){
          if(num == arr(i)._1){
            returnval = ind_sq_unique_address(i)
          }
        }
        returnval
      }
      val cases_addr_adjusted = cases.map(x=>returnMapping2(x._1.abs,ind_sq_unique.zipWithIndex.toArray))
      println(cases_addr_adjusted.toList)

      def returnMapping3(num: (Int, Int), arr: Array[(Int,Int)]):Int={
        var returnval = 0
        for(i <- 0 until arr.length){
          if(num == arr(i)){
            returnval = i
          }
        }
        returnval
      }
      val new_adj_case_sq = ind_sq.map(x=>x.zipWithIndex.map(y=>(returnMapping3((y._1.abs,y._2), cases.toArray),y._1 < 0)))
      new_adj_case_sq.map(x=>println(x.toList))
      var mult_layer_count1 = 0
      var cases_no_mult = mutable.ArrayBuffer[(Int, Int)]()
      for(i <- 0 until cases.length) yield {
        if (!FFT.isReducable(DFTr_Constants(cases_addr_adjusted(i)._1 + 1)(cases_addr_adjusted(i)._2 + 1).re)._1 || !FFT.isReducable(DFTr_Constants(cases_addr_adjusted(i)._1 + 1)(cases_addr_adjusted(i)._2 + 1).im)._1) {
          mult_layer_count1 += 1
        } else {
          cases_no_mult += cases_addr_adjusted(i)
        }
      }
      var subadd_layer_count1 = 0
      var cases_no_add = mutable.ArrayBuffer[(Int, Int)]()
      for(i <- 0 until cases.length)yield {
        if (!(DFTr_Constants(cases_addr_adjusted(i)._1 + 1)(cases_addr_adjusted(i)._2 + 1).re.abs < 0.00005) && !(DFTr_Constants(cases_addr_adjusted(i)._1 + 1)(cases_addr_adjusted(i)._2 + 1).im.abs < 0.00005)) {
          subadd_layer_count1 += 1
        } else {
          cases_no_add += cases_addr_adjusted(i)
        }
      }
      val CMultLatency = 2
      val CAddLatency = 1
      val initial_Latency = 1
      val multlayerlatency = if(mult_layer_count1>0){1}else{0}
      val subaddlayerlatency = if(subadd_layer_count1 > 0){1}else{0}
      val multadd_latency = if(inner_inner_square_size>1){((Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt)yield{(inner_inner_square_size/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)}else{0}
      val end_latency = 1
      var DFT_latency = initial_Latency + multlayerlatency + subaddlayerlatency + multadd_latency + end_latency
      val Perm_latency = 1
      val Twid_latency = 2
      if(r==2){
        DFT_latency = 1
      }
      val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
      val latency = (depth*Perm_latency) + (depth*Twid_latency) + (depth*DFT_latency)
      var overrall_latency = 0
      val increment_points = mutable.ArrayBuffer[Int]()
      for(i <- 0 until number_of_stages/depth){
        if(i == 0){
          increment_points += latency
          overrall_latency += latency
        }else{
          increment_points += overrall_latency + latency
          overrall_latency += latency
        }
      }
      increment_points += overrall_latency + Perm_latency
      println("Here are the icnrement points:")
      increment_points.map(x=>println(x))
      overrall_latency += Perm_latency + 1
      println(s"The overrall Latency is: ${overrall_latency}")
      c.io.in_ready.poke(true.B)
      for(i <- 0 until N/width){
        for(j <- 0 until width){
          c.io.in(j).Re.poke(convert_string_to_IEEE_754((i*width + j + 1).toDouble.toString, bw).U)
          c.io.in(j).Im.poke(convert_string_to_IEEE_754(("0.0"), bw).U)
        }
        c.clock.step(1)
      }
      c.io.in_ready.poke(false.B)
      for(i <-0 until overrall_latency - ((N/width)-1)){
        println(s"clock cycle: ${(N/width)+i}")
        println(s"validate: ${c.io.out_validate.peek().litValue}")
        for(j <- 0 until width){
          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, bw)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue, bw)}")
        }
        c.clock.step(1)
        println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_")
      }
    }
  }
  def testing_streaming(N: Int, r:Int, w: Int,bw: Int): Unit = {
    test(new FFT_sr_v2_streaming(N,r,w,bw)){c=>
      val DFTs_per_stage = w/r
      val DFT_latency = if(r==2){1}else{4}
      val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
      val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
      var T_L = 2
      val Twid_latency = TotalStages * T_L
      val Perm_latency = (N/w)*2
      val Total_Latency = Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency + 1 + (N/w) - 1
      c.io.in_ready.poke(true.B)
      for(j <- 0 until N/w) {
        for (i <- 0 until w) {
          c.io.in(i).Re.poke(convert_string_to_IEEE_754((j*w + i + 1).toDouble.toString, bw).U)
          c.io.in(i).Im.poke(convert_string_to_IEEE_754("0", bw).U)
        }
        c.clock.step(1)
      }
      c.io.in_ready.poke(false.B)
      for(i <- 0 until Total_Latency - ((N/w)-1)){
        println(s"Clock Cycle: ${(N/w) + i}")
        println(s"validate: ${c.io.out_validate.peek().litValue}")
        for(j <- 0 until w){
          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, bw)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue,bw)}")
        }
        c.clock.step(1)
        println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_")
      }
    }
  }
  def main(args: Array[String]): Unit = {
    val N = 6
    val r = 3
    val base_r = 3
    val w = 3
    val ptype = 2
    val bw = 32
    val l2c = (log2Ceil((N/w)))
//    val pw3 = new PrintWriter("DFT_r_v2_rv.v")
//    pw3.println(getVerilogString(new DFT_r_v2_rv(2,bw)))
//    pw3.close()
//    val pw3 = new PrintWriter("FFT_sr_v2_streaming.v")
//    pw3.println(getVerilogString(new FFT_sr_v2_streaming(4,2,2,bw)))
//    pw3.close()
//    val pw3 = new PrintWriter("FFT_sr_v2_streaming_and_iterative.v")
//    pw3.println(getVerilogString(new FFT_sr_v2_streaming_and_iterative(9,3,1,3,bw)))
//    pw3.close()
//    test(new FFT_mr_basic(6,2,3,2,3,6,bw)){c=>
//      c.io.in_ready.poke(true.B)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(4).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
//      c.io.in(4).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(5).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
////      c.io.in(5).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(6).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
////      c.io.in(6).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(7).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
////      c.io.in(7).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(8).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////      c.io.in(8).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(9).Re.poke(convert_string_to_IEEE_754("10.0", 32).U)
////      c.io.in(9).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(10).Re.poke(convert_string_to_IEEE_754("11.0", 32).U)
////      c.io.in(10).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(11).Re.poke(convert_string_to_IEEE_754("12.0", 32).U)
////      c.io.in(11).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      for(i <- 0 until 50){
//        c.clock.step(1)
//        println(s"clock_cycle: ${2+i}")
//        println(s"validate: ${c.io.out_validate.peek().litValue}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
//        println("--------------------------------")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_t(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_t(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_t(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_t(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_t(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_t(2).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_t(3).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_t(3).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_t(4).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_t(4).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_t(5).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_t(5).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(8).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(8).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(9).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(9).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(10).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(10).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(11).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(11).Im.peek().litValue, 32)}")
//      }
//    }
//    test(new FFT_sr_v2_iterative2(4,2,1,4,32)){c=> // for now, do not set streaming-width equal to the N-width, also the streaming width must be a multiple of the radx and a divisor of the N. The iterative depth must be a divisor of the total number of stages required for the computation of the fft
//      c.io.in_ready.poke(true.B)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //      c.clock.step(1)
////      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
////      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //c.clock.step(1)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("10.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("11.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("12.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("13.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("14.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("15.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("16.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(2).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
////      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.clock.step(1)
////      c.clock.step(1)
////      c.io.in(0).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
////      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(1).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
////      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////      c.io.in(2).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      for(i <- 0 until 150){
//        c.clock.step(1)
//        println(s"Clock Cycle: ${1+i}")
//        c.io.in_ready.poke(false.B)
//        println(s"here is Twid: ${c.io.out_cnt.peek().litValue}")
//        println(s"validated Output: ${c.io.out_validate.peek().litValue}")
//        c.io.in(0).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(1).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(2).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(3).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(3).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(2).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(3).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(3).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(4).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(4).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(5).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(5).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(6).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(6).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(7).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(7).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(2).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(3).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(3).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(2).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(2).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(3).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(3).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(4).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(4).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(5).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(5).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(6).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(6).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(7).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(7).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(2).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(2).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//        println("-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- ")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(0).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(0).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out_test(1).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(1).Im.peek().litValue, 32)}")
////        println("-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- ")
////        println("-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- ")
//
//      }
//    }


//    test(new FFT_sr_v2_streaming_and_iterative2(27,3,1,3,32)){c=> // for now, do not set streaming-width equal to the N-width, also the streaming width must be a multiple of the radx and a divisor of the N. The iterative depth must be a divisor of the total number of stages required for the computation of the fft
//      c.io.in_ready.poke(true.B)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //      c.clock.step(1)
//      //      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      //      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("10.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("11.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("12.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("13.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("14.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("15.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("16.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("17.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("18.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("19.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("20.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("21.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("22.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("23.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("24.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("25.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("26.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("27.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //      c.io.in(2).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
//      //      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //      c.clock.step(1)
//      //      c.clock.step(1)
//      //      c.io.in(0).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
//      //      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //      c.io.in(1).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
//      //      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      //      c.io.in(2).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
//      //      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      for(i <- 0 until 150){
//        c.clock.step(1)
//        println(s"Clock Cycle: ${9+i}")
//        c.io.in_ready.poke(false.B)
//        println(s"here is Twid: ${c.io.out_cnt.peek().litValue}")
//        println(s"validated Output: ${c.io.out_validate.peek().litValue}")
//        c.io.in(0).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(1).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(2).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(2).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(3).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(3).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(4).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(4).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(5).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(5).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(6).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(6).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(7).Re.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        //        c.io.in(7).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//        println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out_test(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(2).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(2).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(2).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(3).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(3).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(4).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(4).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(5).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(5).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(6).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(6).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(7).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(7).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(2).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(2).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//        println("-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- ")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(0).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(0).Im.peek().litValue, 32)}")
//        //        println(s"Real Output: ${convert_long_to_float(c.io.out_test(1).Re.peek().litValue, 32)}")
//        //        println(s"Imaginary Output: ${convert_long_to_float(c.io.out_test(1).Im.peek().litValue, 32)}")
//        //        println("-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- ")
//        //        println("-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- ")
//
//      }
//    }
//    test(new TwiddleFactorsStreamed(4,2,2,0,bw)){c=>
//      c.io.in_en.poke(true.B)
//      //c.clock.step(1)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0",bw).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("2.0",bw).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("3.0",bw).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("4.0",bw).U)
//      println(s"outputRe: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, bw)}")
//      println(s"outputRe: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, bw)}")
//      println("________________________________________________")
//      c.clock.step(2)
//      println(s"outputRe: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, bw)}")
//      println(s"outputRe: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, bw)}")
//      println("________________________________________________")
//      c.clock.step(1)
//      println(s"outputRe: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, bw)}")
//      println(s"outputRe: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, bw)}")
//      println("________________________________________________")
//      println("________________________________________________")
//      c.clock.step(1)
//      println(s"outputRe: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, bw)}")
//      println(s"outputRe: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, bw)}")
//      println("________________________________________________")
//      println("________________________________________________")
//      c.clock.step(1)
//      println(s"outputRe: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, bw)}")
//      println(s"outputRe: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, bw)}")
//      println("________________________________________________")
//      println("________________________________________________")
//      c.clock.step(1)
//      println(s"outputRe: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, bw)}")
//      println(s"outputRe: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, bw)}")
//      println(s"outputIm: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, bw)}")
//      println("________________________________________________")
//    }
//    val pw4 = new PrintWriter("FFT_mr_basic.v")
//    pw4.println(getVerilogString(new FFT_mr_basic(24,3,8,3,2,24,bw)))
//    pw4.close()
//    val pw = new PrintWriter("DFT_r.v")
//    pw.println(getVerilogString(new DFT_r(2,32)))
//    pw.close()
//    val pw2 = new PrintWriter("FFT_sr.v")
//    pw2.println(getVerilogString(new FFT_sr(8,2,8,bw/*bit width precision*/)))
//    pw2.close()
//    genDFTInOutFile(N = 27,r = 3,bw = 32, runs = 100) // this is how to generate input/output files for testing

//    test(new FPComplexMult_reducable(bw, Math.sin(Math.PI/3), Math.cos(Math.PI/3))){c=>
//      c.io.in_a.Re.poke(convert_string_to_IEEE_754("1.4", bw).U)
//      c.io.in_a.Im.poke(convert_string_to_IEEE_754("2.3", bw).U)
//      c.io.in_b.Re.poke(convert_string_to_IEEE_754(Math.sin(Math.PI/3).toString, bw).U)
//      c.io.in_b.Im.poke(convert_string_to_IEEE_754(Math.cos(Math.PI/3).toString, bw).U)
//      c.clock.step(2)
//      println(s"Hardware Result Re: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, bw)}")
//      println(s"Hardware Result Im: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, bw)}")
//      val cmplx_mult = complex_mult(cmplx(1.4,2.3), cmplx(Math.sin(Math.PI/3), Math.cos(Math.PI/3)))
//      println(s"Software Result Re: ${cmplx_mult.re}")
//      println(s"Software Result Im: ${cmplx_mult.im}")
//    }
    //println(s"l2c: ${l2c}")
//    println(isReducable(1.0))
    val pN =6
    val pr = 3
    val pw = 3
    val pwin = 2

//    test(new PermutationsWithStreaming_mr(pN,pr,pr,pw,pwin,2,32)){c=>
//      for(i <- 0 until (pN/pw)*2 + 1){
//        c.io.in_en(i).poke(true.B)
//      }
//
//      for(i <- 0 until pN/pwin){
//        for(j <- 0 until pwin){
//          c.io.in(j).Re.poke(convert_string_to_IEEE_754((i*pwin + j).toDouble.toString, bw).U)
//          c.io.in(j).Im.poke(convert_string_to_IEEE_754("0.0", bw).U)
//        }
//        c.clock.step(1)
//      }
//
//      for(i <- 0 until pN/pw - pN/pwin) {
//        println(s"Clock cycle: ${i+1}")
//        c.clock.step(1)
//      }
//      for(i <- 0 until pN/pw){
//        c.clock.step(1)
//      }
//      for(i <- 0 until pN/pw){
//        for(j <- 0 until pw){
//          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, bw)}")
//          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue, bw)}")
//        }
//        c.clock.step(1)
//        println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-")
//      }
//    }

    test(new PermutationsWithStreaming_mr(pN,pr,pr,pw,pwin,2,32)){c=>
      for(i <- 0 until (pN/pwin)*2 + 1){
        c.io.in_en(i).poke(true.B)
      }

      for(i <- 0 until pN/pwin) {
        println(s"Clock cycle: ${i+1}")
        for (j <- 0 until pw) {
          println(s"The test output port${j}: ${c.io.out_t(j).peek().litValue}")
        }
        for(j <- 0 until pwin){
          c.io.in(j).Re.poke(convert_string_to_IEEE_754((i*pwin + j).toDouble.toString, bw).U)
          c.io.in(j).Im.poke(convert_string_to_IEEE_754("0.0", bw).U)
        }
        c.clock.step(1)
      }
      for(i <- 0 until pN/pwin){
        c.clock.step(1)
      }
      for(i <- 0 until pN/pw){
        for(j <- 0 until pw){
          println(s"Real Output: ${convert_long_to_float(c.io.out(j).Re.peek().litValue, bw)}")
          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(j).Im.peek().litValue, bw)}")
        }
        c.clock.step(1)
        println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-")
      }
    }
//    test(new PermutationsWithStreaming(N,r,base_r,w,ptype,bw)) { c =>
//      for(i <- 0 until (N/w)*2 + 1) {
//        c.io.in_en(i).poke(true.B)
//      }
//      //c.clock.step(1)
//      for(i <- 0 until 4) {
//        for (j <- 0 until N / w) {
//          for (i <- 0 until w) {
//            c.io.in(i).Re.poke(convert_string_to_IEEE_754((j * w + i).toDouble.toString, 32).U)
//            c.io.in(i).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//          }
//          c.clock.step(1)
//          println(s"clock: ${c.io.out_t.peek().litValue}")
//        }
//        for(i <- 0 until (N/w)*2 + 1) {
//          c.io.in_en(i).poke(false.B)
//        }
//        for(i <- 0 until (N/w)*2 + 1) {
//          c.io.in_en(i).poke(true.B)
//        }
//        //println(s"clock: ${convert_long_to_float(c.io.out_t.Re.peek().litValue,bw)}")
//        for (i <- 0 until N / w) {
//          c.clock.step(1)
//          println(s"clock: ${c.io.out_t.peek().litValue}")
//        }
//
//        for (j <- 0 until N / w) {
//          for (i <- 0 until w) {
//            println(s"Real Output: ${convert_long_to_float(c.io.out(i).Re.peek().litValue, 32)}")
//            println(s"Imaginary Output: ${convert_long_to_float(c.io.out(i).Im.peek().litValue, 32)}")
//          }
//          println("________________________________________________")
//          c.clock.step(1)
//          println(s"clock: ${c.io.out_t.peek().litValue}")
//        }
//      }
//    }
////    test(new Streaming_Permute_Config(8,2,2,1,2)){c=>
////      for(i <- 0 until 8/2) {
////        for(j <- 0 until 2){
////          println(s"output${j}: ${c.io.out(j).peek().litValue}")
////        }
////        c.clock.step(1)
////      }
////    }
//    test(new RAM_Block(4,2,32)){c=>
//      c.io.en.poke(true.B)
//      c.io.re.poke(true.B)
//      c.io.wr.poke(true.B)
//      c.io.in_data.Re.poke(1.U)
//      c.io.in_data.Im.poke(2.U)
//      c.io.in_raddr.poke(3.U)
//      c.io.in_waddr.poke(2.U)
//      c.clock.step(1)
//      println(s"output: ${c.io.out_data.Re.peek().litValue}")
//      c.io.in_raddr.poke(2.U)
//      c.clock.step(1)
//      println(s"output: ${c.io.out_data.Im.peek().litValue}")
//    }
//    test(new FPComplexAdder(32)){c=>
//      c.io.in_a.Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in_a.Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in_b.Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in_b.Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.clock.step(1)
//      println(s"Real Output: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, 32)}")
//    }
//
//    test(new FPComplexMult(32)){c=>
//      c.io.in_a.Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in_a.Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in_b.Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in_b.Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, 32)}")
//    }
//
//    test(new FPComplexMultiAdder(4,32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out.Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out.Im.peek().litValue, 32)}")
//    }
    println("--------")
//    test(new DFT_r_v2_rv(3, 32)){c=>
//      val CMultLatency = 2
//      val CAddLatency = 1
//      val DFT_latency = CMultLatency + ((Math.log10(3)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(3)/Math.log10(2)).floor.toInt)yield{(3/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency) + 1
//      println(DFT_latency)
//      for(i <- 0 until 1) {
//        c.io.in_ready.poke(true.B)
//        c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//        c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//        c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//        c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//        c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//        c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
////        c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(4).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
////        c.io.in(4).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(5).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
////        c.io.in(5).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(6).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
////        c.io.in(6).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(7).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
////        c.io.in(7).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(8).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////        c.io.in(8).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        //c.io.in_ready.poke(true.B)
////        c.io.in(9).Re.poke(convert_string_to_IEEE_754("10.0", 32).U)
////        c.io.in(9).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(10).Re.poke(convert_string_to_IEEE_754("11.0", 32).U)
////        c.io.in(10).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(11).Re.poke(convert_string_to_IEEE_754("12.0", 32).U)
////        c.io.in(11).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(12).Re.poke(convert_string_to_IEEE_754("13.0", 32).U)
////        c.io.in(12).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(13).Re.poke(convert_string_to_IEEE_754("14.0", 32).U)
////        c.io.in(13).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////        c.io.in(8).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////        c.io.in(8).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//        for (i <- 0 until 20) {
////          for(j <- 0 until 10){
//            c.clock.step(1)
//            println(s"validateoutput: ${c.io.out_validate.peek().litValue}")
////            c.io.in_ready.poke(true.B)
////            if(j == 0) {
////              c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
////              c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
////              c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
////              c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
////              c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////            }else if(j == 1){
////              c.io.in(0).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
////              c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(1).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////              c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(2).Re.poke(convert_string_to_IEEE_754("10.0", 32).U)
////              c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(3).Re.poke(convert_string_to_IEEE_754("11.0", 32).U)
////              c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////            }else{
////              c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.0", 32).U)
////              c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(1).Re.poke(convert_string_to_IEEE_754("13.0", 32).U)
////              c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(2).Re.poke(convert_string_to_IEEE_754("14.0", 32).U)
////              c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////              c.io.in(3).Re.poke(convert_string_to_IEEE_754("15.0", 32).U)
////              c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////            }
//            println(s"Clock cycle ${ i + 1}")
//            println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//            println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//            println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//            println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//            println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//            println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(8).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(8).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(9).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(9).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(10).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(10).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(11).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(11).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(12).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(12).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(13).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(13).Im.peek().litValue, 32)}")
////          println(s"Real Output: ${convert_long_to_float(c.io.out(8).Re.peek().litValue, 32)}")
////          println(s"Imaginary Output: ${convert_long_to_float(c.io.out(8).Im.peek().litValue, 32)}")
////          }
////          c.io.in_ready.poke(true.B)
////          c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
////          c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////          c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
////          c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
////          c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
////          c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//        }
//      }
//    }
//    println("--------")
//    test(new TwiddleFactors(4,2,2,0,32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("1.4", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("2.4", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("2.9", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("3.4", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("4.984", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("4.4", 32).U)
//      c.clock.step(2)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//    }
//    println("--------")
//    test(new PermutationsBasic(8,2,1,32)){c=>
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(4).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
//      c.io.in(4).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(5).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
//      c.io.in(5).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(6).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
//      c.io.in(6).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      c.io.in(7).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
//      c.io.in(7).Im.poke(convert_string_to_IEEE_754("0", 32).U)
//      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
//      println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
//      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
//    }
    println("--------------------")
//    val N = 8
//    val r = 2
//    val w = 8
//    val bw = 32
//    val runs = 62500
//    genRandom(N*2*runs,"TestInputs", bw)

//    val DFTs_per_stage = N/r
//    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
//    val CMultLatency = 2
//    val CAddLatency = 1
//    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
//    val Twid_latency = (N/w) * CMultLatency
//    val Perm_latency = 0
//    val Total_Latency = (number_of_stages - 1) * Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency
//
//    val golden_input = scala.io.Source.fromFile("test.txt").getLines().map(x=>x.toDouble).toArray
//
//    val cmplx_inputs = (for(j <- 0 until runs)yield{
//      val inp = (for(i <- 0 until (2*N) by 2) yield{
//        cmplx(golden_input(i),golden_input(i+1))
//      }).toArray
//      inp
//    }).toArray
//
//    val sw_model = FFT_r_GoldenModel(N, r, cmplx_inputs)
println("-----------------------------------------------??????????????????")
//      test(new FFT_sr_v2(8,2,8,32)){c=>
//      val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
//      var mult_count = 0
//      for(i <- 0 until r-1){
//        for(j <- 0 until r-1){
//          val n  = DFTr_Constants(i+1)(j+1)
//          val c1 = FFT.isReducable(n.re.abs)
//          val c2 = FFT.isReducable(n.im.abs)
//          if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
//            mult_count += 1
//          }
//        }
//      }
//      val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
//      var T_L = 0
//      val CMultLatency = 2
//      val CAddLatency = 1
//      for(i <- 0 until TotalStages) {
//        val twid = Permutations.T2(N, r)(i)
//        var mult_count2 = 0
//        for (i <- 0 until w){
//          val n = twid(i)
//          val c1 = FFT.isReducable(n.re.abs)
//          val c2 = FFT.isReducable(n.im.abs)
//          if (!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
//            mult_count2 += 1
//          }
//        }
//        var T_latency = CMultLatency
//        if (mult_count2 == 0) {
//          T_latency = 0
//        }
//        println(mult_count2 + "yay")
//        T_L += T_latency
//      }
//      val DFTs_per_stage = N/r
//      val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
//      var DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
//      if(mult_count == 0){
//        DFT_latency = ((Math.log10(r)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).floor.toInt)yield{(r/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)
//      }
//      val Twid_latency = (N/w) * CMultLatency
//      val Perm_latency = 0
//      val Total_Latency = T_L + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency + 1
//      println(Total_Latency)
//      c.io.in_ready.poke(true.B)
//      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
//      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
//      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
//      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
//      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(4).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
//      c.io.in(4).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(5).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
//      c.io.in(5).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(6).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
//      c.io.in(6).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      c.io.in(7).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
//      c.io.in(7).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
////        c.io.in(8).Re.poke(convert_string_to_IEEE_754("9.0", 32).U)
////        c.io.in(8).Im.poke(convert_string_to_IEEE_754("0.0", 32).U)
//      var g = false
//      for(i <- 0 until 20) {
//        c.clock.step(1)
//        println(s"valid output ; ${c.io.out_validate.peek().litValue}")
//        println(s"Clock cycle ${i+1}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
//        println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
//        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
////        println(s"Real Output: ${convert_long_to_float(c.io.out(8).Re.peek().litValue, 32)}")
////        println(s"Imaginary Output: ${convert_long_to_float(c.io.out(8).Im.peek().litValue, 32)}")
//      }
//    }

  }
}
