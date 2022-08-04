package ChiselFFT
import chisel3._
import chisel3.util._
import ComplexModules.FPComplex._
import SWFFT._
import SWFFT.FFT._
import IEEEConversions.FPConvert._
import Chisel.{MuxLookup, log2Ceil, resetToBool}
import ComplexModules.FPComplex
import SWFFT.ComplexNumbers.cmplx

import scala.collection.mutable

object FFTDesigns {

  class DFT_Symmetric_NRV(r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(r, new ComplexNum(bw)))
      val out = Output(Vec(r, new ComplexNum(bw)))
    })
    val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
    val is_odd = if(r % 2 == 0){false}else{true}
    if(r == 2){
      val dft2 = Module(new DFT_NRV(2,bw)).io
      dft2.in := io.in
      io.out := dft2.out
    }
    else if(is_odd){
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
      val CAddLatency = 1
      val initial_Latency = 1
      val multlayerlatency = if(mult_layer_count1>0){1}else{0}
      val subaddlayerlatency = if(subadd_layer_count1 > 0){1}else{0}
      val multadd_latency = if(inner_inner_square_size>1){((Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt)yield{(inner_inner_square_size/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)}else{0}
      val end_latency = 1
      val initial_adds_subs = for(i <- 0 until inner_inner_square_size)yield{
        val ias = for(j <- 0 until 2)yield{
          if(j== 0){
            val instance = Module(new FPComplexAdder(bw)).io
            instance
          }else{
            val instance = Module(new FPComplexSub(bw)).io
            instance
          }
        }
        ias
      }
      for(i <- 0 until inner_inner_square_size){
        initial_adds_subs(i)(0).in_a := io.in(start_ind)
        initial_adds_subs(i)(0).in_b := io.in(last_ind)
        initial_adds_subs(i)(1).in_a := io.in(start_ind)
        initial_adds_subs(i)(1).in_b := io.in(last_ind)
        start_ind += 1
        last_ind -= 1
      }
      val top_row_init_sum = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
      val initial_layer_out = if(subaddlayerlatency + multlayerlatency > 0){RegInit(VecInit.fill(subaddlayerlatency + multlayerlatency)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val slctlncy = if(subaddlayerlatency + multlayerlatency > 0){subaddlayerlatency + multlayerlatency}else{2}
      for(i <- 0 until subaddlayerlatency + multlayerlatency){
        if(i == 0){
          for(j <- 0 until inner_inner_square_size){
            initial_layer_out(0)(j) := initial_adds_subs(j)(0).out_s
          }
        }else{
          initial_layer_out(i) := initial_layer_out(i-1)
        }
      }
      top_row_init_sum.in := initial_layer_out(slctlncy-1)
      val initial_mults = for(i <- 0 until cases.length) yield {
        val mults = for(j <- 0 until 2) yield{
          if(j == 0){
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, 0.0, if(mult_layer_count1>0){true}else{false},true)).io // these will at most be 1 regular mult
            mult_instance
          }else{
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,0.0,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im, if(mult_layer_count1>0){true}else{false},true)).io
            mult_instance
          }
        }
        mults
      }

      for(i <- 0 until cases.length){
        initial_mults(i)(0).in_a := initial_adds_subs(cases_addr(i)._2)(0).out_s
        initial_mults(i)(0).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re.toString,bw).U
        initial_mults(i)(0).in_b.Im := 0.U
        initial_mults(i)(1).in_a := initial_adds_subs(cases_addr(i)._2)(1).out_s
        initial_mults(i)(1).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im.toString,bw).U
        initial_mults(i)(1).in_b.Re := 0.U
      }
      val stage_adds_subs = for(i <- 0 until cases.length)yield{
        val instances = for(j <- 0 until 2) yield{
          if(j==0){
            val instance = Module(new FPComplexAdder_reducable(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im,if(subadd_layer_count1>0){true}else{false})).io
            instance
          }else{
            val instance = Module(new FPComplexSub_reducable(bw, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im,if(subadd_layer_count1>0){true}else{false})).io
            instance
          }
        }
        instances
      }
      for(i <- 0 until cases.length){
        stage_adds_subs(i)(0).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(0).in_b := initial_mults(i)(1).out_s
        stage_adds_subs(i)(1).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(1).in_b := initial_mults(i)(1).out_s
      }

      val fillIn = WireInit(VecInit.fill(inner_inner_square_size)(VecInit.fill(inner_inner_square_size)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw))))))
      for(i <- 0 until inner_inner_square_size){
        for(j <- 0 until inner_inner_square_size){
          if(new_adj_case_sq(i)(j)._2){
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
          }else{
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
          }
        }
      }

      val fpmultiadds = for(i <- 0 until inner_square_size)yield{
        val fpmadds = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
        fpmadds
      }

      for(i <- 0 until 2) {
        for (j <- 0 until inner_inner_square_size) {
          for (k <- 0 until inner_inner_square_size) {
            if(i == 0){
              fpmultiadds(j).in(k) := fillIn(j)(k)(i)
            }else{
              fpmultiadds(inner_square_size - j - 1 ).in(k) := fillIn(j)(k)(i)
            }
          }
        }
      }
      val results = WireInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until r){
        if(i == 0){
          results(0) := top_row_init_sum.out
        }else{
          results(i) := fpmultiadds(i-1).out
        }
      }
      val final_adder_layer = for(i <- 0 until r)yield{
        val adder = Module(new FPComplexAdder(bw)).io
        adder
      }
      val finallayer = RegInit(VecInit.fill(initial_Latency+multlayerlatency+subaddlayerlatency+multadd_latency)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until initial_Latency+multlayerlatency+subaddlayerlatency+multadd_latency){
        if(i == 0){
          finallayer(0) := io.in(0)
        }else{
          finallayer(i) := finallayer(i-1)
        }
      }
      for(i <- 0 until r){
        final_adder_layer(i).in_a := finallayer(initial_Latency+multlayerlatency+subaddlayerlatency+multadd_latency - 1)
        final_adder_layer(i).in_b := results(i)
        io.out(i) := final_adder_layer(i).out_s
      }
    }else{
      val inner_square_size = r-2
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
      val CAddLatency = 1
      val initial_Latency = 1
      val multlayerlatency = if(mult_layer_count1>0){1}else{0}
      val subaddlayerlatency = if(subadd_layer_count1 > 0){1}else{0}
      val multadd_latency = if(inner_inner_square_size>1){((Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt)yield{(inner_inner_square_size/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)}else{0}
      println(s"mulltaddlatency: ${multlayerlatency}")
      val end_latency = 1
      val initial_adds_subs = for(i <- 0 until inner_inner_square_size)yield{
        val ias = for(j <- 0 until 2)yield{
          if(j== 0){
            val instance = Module(new FPComplexAdder(bw)).io
            instance
          }else{
            val instance = Module(new FPComplexSub(bw)).io
            instance
          }
        }
        ias
      }
      for(i <- 0 until inner_inner_square_size){
        initial_adds_subs(i)(0).in_a := io.in(start_ind)
        initial_adds_subs(i)(0).in_b := io.in(last_ind)
        initial_adds_subs(i)(1).in_a := io.in(start_ind)
        initial_adds_subs(i)(1).in_b := io.in(last_ind)
        start_ind += 1
        last_ind -= 1
      }

      val top_row_init_sum = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
      val mid_row_init_sum = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
      var switch = true
      val initial_layer_out = if(subaddlayerlatency + multlayerlatency > 0){RegInit(VecInit.fill(subaddlayerlatency + multlayerlatency)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val initial_layer_out_mid = if(subaddlayerlatency + multlayerlatency > 0){RegInit(VecInit.fill(subaddlayerlatency + multlayerlatency)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val slctlncy = if(subaddlayerlatency + multlayerlatency > 0){subaddlayerlatency + multlayerlatency}else{2}
      println(s"slctltncy ${slctlncy}")
      for(i <- 0 until subaddlayerlatency + multlayerlatency){
        if(i == 0){
          for(j <- 0 until inner_inner_square_size){
            initial_layer_out(0)(j) := initial_adds_subs(j)(0).out_s
            if(switch) {
              initial_layer_out_mid(0)(j).Re := (~initial_adds_subs(j)(0).out_s.Re(bw - 1)) ## initial_adds_subs(j)(0).out_s.Re(bw - 2, 0)
              initial_layer_out_mid(0)(j).Im  := (~initial_adds_subs(j)(0).out_s.Im(bw - 1)) ## initial_adds_subs(j)(0).out_s.Im(bw - 2, 0)
              switch = false
            }else{
              initial_layer_out_mid(0)(j) := initial_adds_subs(j)(0).out_s
              switch = true
            }
          }
        }else{
          initial_layer_out(i) := initial_layer_out(i-1)
          initial_layer_out_mid(i) := initial_layer_out_mid(i-1)
        }
      }
      top_row_init_sum.in := initial_layer_out(slctlncy-1)
      mid_row_init_sum.in := initial_layer_out_mid(slctlncy-1)
      val initial_mults = for(i <- 0 until cases.length) yield {
        val mults = for(j <- 0 until 2) yield{
          if(j == 0){
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, 0.0, if(mult_layer_count1>0){true}else{false},true)).io
            mult_instance
          }else{
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,0.0,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im, if(mult_layer_count1>0){true}else{false},true)).io
            mult_instance
          }
        }
        mults
      }

      for(i <- 0 until cases.length){
        initial_mults(i)(0).in_a := initial_adds_subs(cases_addr(i)._2)(0).out_s
        initial_mults(i)(0).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re.toString,bw).U
        initial_mults(i)(0).in_b.Im := 0.U
        initial_mults(i)(1).in_a := initial_adds_subs(cases_addr(i)._2)(1).out_s
        initial_mults(i)(1).in_b.Re := 0.U
        initial_mults(i)(1).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im.toString,bw).U
      }
      val stage_adds_subs = for(i <- 0 until cases.length)yield{
        val instances = for(j <- 0 until 2) yield{
          if(j==0){
            val instance = Module(new FPComplexAdder_reducable(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im, if(subadd_layer_count1>0){true}else{false})).io
            instance
          }else{
            val instance = Module(new FPComplexSub_reducable(bw, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im,if(subadd_layer_count1>0){true}else{false})).io
            instance
          }
        }
        instances
      }

      for(i <- 0 until cases.length){
        stage_adds_subs(i)(0).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(0).in_b := initial_mults(i)(1).out_s
        stage_adds_subs(i)(1).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(1).in_b := initial_mults(i)(1).out_s
      }

      val fillIn = WireInit(VecInit.fill(inner_inner_square_size)(VecInit.fill(inner_inner_square_size)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw))))))
      for(i <- 0 until inner_inner_square_size){
        for(j <- 0 until inner_inner_square_size){
          if(new_adj_case_sq(i)(j)._2){
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
          }else{
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
          }
        }
      }
      val fpmultiadds = for(i <- 0 until inner_square_size)yield{
        val fpmadds = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
        fpmadds
      }
      for(i <- 0 until 2) {
        for (j <- 0 until inner_inner_square_size) {
          for (k <- 0 until inner_inner_square_size) {
            if(i == 0){
              fpmultiadds(j).in(k) := fillIn(j)(k)(i)
            }else{
              fpmultiadds(inner_square_size - j - 1 ).in(k) := fillIn(j)(k)(i)
            }
          }
        }
      }
      val results = WireInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
      var offst = 0
      for(i <- 0 until r){
        if(i == 0){
          results(0) := top_row_init_sum.out
        }else if(i == r/2){
          results(r/2) := mid_row_init_sum.out
          offst = 1
        }else{
          results(i) := fpmultiadds(i-1-offst).out
        }
      }
      val prep_adder_layer = for(i <- 0 until 2)yield{
        if(i == 0){
          val adder = Module(new FPComplexAdder(bw)).io
          adder
        }else{
          val adder = Module(new FPComplexSub(bw)).io
          adder
        }
      }
      for(i <- 0 until 2){
        prep_adder_layer(i).in_a := io.in(0)
        prep_adder_layer(i).in_b := io.in(r/2)
      }
      val finallayer = if(multlayerlatency+subaddlayerlatency+multadd_latency > 0){RegInit(VecInit.fill(multlayerlatency+subaddlayerlatency+multadd_latency)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val slctlncy2 = if(subaddlayerlatency + multlayerlatency + multadd_latency > 0){subaddlayerlatency + multlayerlatency + multadd_latency}else{2}

      for(i <- 0 until slctlncy2){
        if(i == 0){
          for(j <- 0 until 2){
            finallayer(0)(j) := prep_adder_layer(j).out_s
          }
        }else{
          finallayer(i) := finallayer(i-1)
        }
      }
      for(i <- 0 until r){}
      val final_adder_layer = for(i <- 0 until r)yield{
        val adder = Module(new FPComplexAdder(bw)).io
        adder
      }
      for(i <- 0 until r){
        final_adder_layer(i).in_a := finallayer(slctlncy2-1)(i % 2)//io.in(0)
        final_adder_layer(i).in_b := results(i)
        io.out(i) := final_adder_layer(i).out_s
      }
    }
  }

  class DFT_Symmetric(r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(r, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(r, new ComplexNum(bw)))
    })
    val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
    val is_odd = if(r % 2 == 0){false}else{true}
    if(r == 2){
      val dft2 = Module(new DFT(2,bw)).io
      dft2.in := io.in
      dft2.in_ready := io.in_ready
      io.out := dft2.out
      io.out_validate := dft2.out_validate
    }
    else if(is_odd){
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
      val CAddLatency = 1
      val initial_Latency = 1
      val multlayerlatency = if(mult_layer_count1>0){1}else{0}
      val subaddlayerlatency = if(subadd_layer_count1 > 0){1}else{0}
      val multadd_latency = if(inner_inner_square_size>1){((Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt)yield{(inner_inner_square_size/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)}else{0}
      val end_latency = 1
      val total_latency = initial_Latency + multlayerlatency + subaddlayerlatency + multadd_latency + end_latency + 1
      val regdelays = RegInit(VecInit.fill(total_latency)(false.B))
      for(i <- 0 until total_latency){
        if(i ==0){
          regdelays(0) := io.in_ready
        }else{
          regdelays(i) := regdelays(i-1)
        }
      }
      val valid_signal = Wire(Bool())
      valid_signal := regdelays(total_latency-1)
      io.out_validate := valid_signal
      val initial_adds_subs = for(i <- 0 until inner_inner_square_size)yield{
        val ias = for(j <- 0 until 2)yield{
          if(j== 0){
            val instance = Module(new FPComplexAdder(bw)).io
            instance
          }else{
            val instance = Module(new FPComplexSub(bw)).io
            instance
          }
        }
        ias
      }
      when(io.in_ready) {
        for (i <- 0 until inner_inner_square_size) {
          initial_adds_subs(i)(0).in_a := io.in(start_ind)
          initial_adds_subs(i)(0).in_b := io.in(last_ind)
          initial_adds_subs(i)(1).in_a := io.in(start_ind)
          initial_adds_subs(i)(1).in_b := io.in(last_ind)
          start_ind += 1
          last_ind -= 1
        }
      }.otherwise{
        for (i <- 0 until inner_inner_square_size) {
          initial_adds_subs(i)(0).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          initial_adds_subs(i)(0).in_b := 0.U.asTypeOf(new ComplexNum(bw))
          initial_adds_subs(i)(1).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          initial_adds_subs(i)(1).in_b := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
      val top_row_init_sum = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
      val initial_layer_out = if(subaddlayerlatency + multlayerlatency > 0){RegInit(VecInit.fill(subaddlayerlatency + multlayerlatency)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val slctlncy = if(subaddlayerlatency + multlayerlatency > 0){subaddlayerlatency + multlayerlatency}else{2}
      for(i <- 0 until subaddlayerlatency + multlayerlatency){
        if(i == 0){
          for(j <- 0 until inner_inner_square_size){
            initial_layer_out(0)(j) := initial_adds_subs(j)(0).out_s
          }
        }else{
          initial_layer_out(i) := initial_layer_out(i-1)
        }
      }

      top_row_init_sum.in := initial_layer_out(slctlncy-1)
      val initial_mults = for(i <- 0 until cases.length) yield {
        val mults = for(j <- 0 until 2) yield{
          if(j == 0){
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, 0.0, if(mult_layer_count1>0){true}else{false},true)).io // these will at most be 1 regular mult
            mult_instance
          }else{
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,0.0,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im, if(mult_layer_count1>0){true}else{false},true)).io
            mult_instance
          }
        }
        mults
      }

      for(i <- 0 until cases.length){
        initial_mults(i)(0).in_a := initial_adds_subs(cases_addr(i)._2)(0).out_s
        initial_mults(i)(0).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re.toString,bw).U
        initial_mults(i)(0).in_b.Im := 0.U
        initial_mults(i)(1).in_a := initial_adds_subs(cases_addr(i)._2)(1).out_s
        initial_mults(i)(1).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im.toString,bw).U
        initial_mults(i)(1).in_b.Re := 0.U
      }

      val stage_adds_subs = for(i <- 0 until cases.length)yield{
        val instances = for(j <- 0 until 2) yield{
          if(j==0){
            val instance = Module(new FPComplexAdder_reducable(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im,if(subadd_layer_count1>0){true}else{false})).io
            instance
          }else{
            val instance = Module(new FPComplexSub_reducable(bw, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im,if(subadd_layer_count1>0){true}else{false})).io
            instance
          }
        }
        instances
      }

      for(i <- 0 until cases.length){
        stage_adds_subs(i)(0).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(0).in_b := initial_mults(i)(1).out_s
        stage_adds_subs(i)(1).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(1).in_b := initial_mults(i)(1).out_s
      }

      val fillIn = WireInit(VecInit.fill(inner_inner_square_size)(VecInit.fill(inner_inner_square_size)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw))))))
      for(i <- 0 until inner_inner_square_size){
        for(j <- 0 until inner_inner_square_size){
          if(new_adj_case_sq(i)(j)._2){
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
          }else{
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
          }
        }
      }

      val fpmultiadds = for(i <- 0 until inner_square_size)yield{
        val fpmadds = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
        fpmadds
      }

      for(i <- 0 until 2) {
        for (j <- 0 until inner_inner_square_size) {
          for (k <- 0 until inner_inner_square_size) {
            if(i == 0){
              fpmultiadds(j).in(k) := fillIn(j)(k)(i)
            }else{
              fpmultiadds(inner_square_size - j - 1 ).in(k) := fillIn(j)(k)(i)
            }
          }
        }
      }
      val results = WireInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until r){
        if(i == 0){
          results(0) := top_row_init_sum.out
        }else{
          results(i) := fpmultiadds(i-1).out
        }
      }
      val final_adder_layer = for(i <- 0 until r)yield{
        val adder = Module(new FPComplexAdder(bw)).io
        adder
      }
      val finallayer = RegInit(VecInit.fill(initial_Latency+multlayerlatency+subaddlayerlatency+multadd_latency)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until initial_Latency+multlayerlatency+subaddlayerlatency+multadd_latency){
        if(i == 0){
          when(io.in_ready) {
            finallayer(0) := io.in(0)
          }.otherwise{
            finallayer(0) := 0.U.asTypeOf(new ComplexNum(bw))
          }
        }else{
          finallayer(i) := finallayer(i-1)
        }
      }
      val out_regs = RegInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until r){
        final_adder_layer(i).in_a := finallayer(initial_Latency+multlayerlatency+subaddlayerlatency+multadd_latency - 1)
        final_adder_layer(i).in_b := results(i)
        io.out(i) := out_regs(i)
      }
      when(regdelays(total_latency-2)){
        for (i <- 0 until r) {
          out_regs(i) := final_adder_layer(i).out_s
        }
      }.otherwise{
        for (i <- 0 until r) {
          out_regs(i) := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
    }else{
      //val inner_sqaure_size = r-2
      val inner_square_size = r-2
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
      val CAddLatency = 1
      val initial_Latency = 1
      val multlayerlatency = if(mult_layer_count1>0){1}else{0}
      val subaddlayerlatency = if(subadd_layer_count1 > 0){1}else{0}
      val multadd_latency = if(inner_inner_square_size>1){((Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(inner_inner_square_size)/Math.log10(2)).floor.toInt)yield{(inner_inner_square_size/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)}else{0}
      println(s"mulltaddlatency: ${multlayerlatency}")
      val end_latency = 1
      val total_latency = initial_Latency + multlayerlatency + subaddlayerlatency + multadd_latency + end_latency + 1
      val regdelays = RegInit(VecInit.fill(total_latency)(false.B))
      for(i <- 0 until total_latency){
        if(i ==0){
          regdelays(0) := io.in_ready
        }else{
          regdelays(i) := regdelays(i-1)
        }
      }
      val valid_signal = Wire(Bool())
      valid_signal := regdelays(total_latency-1)
      io.out_validate := valid_signal
      val initial_adds_subs = for(i <- 0 until inner_inner_square_size)yield{
        val ias = for(j <- 0 until 2)yield{
          if(j== 0){
            val instance = Module(new FPComplexAdder(bw)).io
            instance
          }else{
            val instance = Module(new FPComplexSub(bw)).io
            instance
          }
        }
        ias
      }
      when(io.in_ready) {
        for (i <- 0 until inner_inner_square_size) {
          initial_adds_subs(i)(0).in_a := io.in(start_ind)
          initial_adds_subs(i)(0).in_b := io.in(last_ind)
          initial_adds_subs(i)(1).in_a := io.in(start_ind)
          initial_adds_subs(i)(1).in_b := io.in(last_ind)
          start_ind += 1
          last_ind -= 1
        }
      }.otherwise{
        for (i <- 0 until inner_inner_square_size) {
          initial_adds_subs(i)(0).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          initial_adds_subs(i)(0).in_b := 0.U.asTypeOf(new ComplexNum(bw))
          initial_adds_subs(i)(1).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          initial_adds_subs(i)(1).in_b := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }

      val top_row_init_sum = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
      val mid_row_init_sum = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
      var switch = true
      val initial_layer_out = if(subaddlayerlatency + multlayerlatency > 0){RegInit(VecInit.fill(subaddlayerlatency + multlayerlatency)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val initial_layer_out_mid = if(subaddlayerlatency + multlayerlatency > 0){RegInit(VecInit.fill(subaddlayerlatency + multlayerlatency)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(inner_inner_square_size)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val slctlncy = if(subaddlayerlatency + multlayerlatency > 0){subaddlayerlatency + multlayerlatency}else{2}
      println(s"slctltncy ${slctlncy}")
      for(i <- 0 until subaddlayerlatency + multlayerlatency){
        if(i == 0){
          for(j <- 0 until inner_inner_square_size){
            initial_layer_out(0)(j) := initial_adds_subs(j)(0).out_s
            if(switch) {
              initial_layer_out_mid(0)(j).Re := (~initial_adds_subs(j)(0).out_s.Re(bw - 1)) ## initial_adds_subs(j)(0).out_s.Re(bw - 2, 0)
              initial_layer_out_mid(0)(j).Im  := (~initial_adds_subs(j)(0).out_s.Im(bw - 1)) ## initial_adds_subs(j)(0).out_s.Im(bw - 2, 0)
              switch = false
            }else{
              initial_layer_out_mid(0)(j) := initial_adds_subs(j)(0).out_s
              switch = true
            }
          }
        }else{
          initial_layer_out(i) := initial_layer_out(i-1)
          initial_layer_out_mid(i) := initial_layer_out_mid(i-1)
        }
      }
      top_row_init_sum.in := initial_layer_out(slctlncy-1)
      mid_row_init_sum.in := initial_layer_out_mid(slctlncy-1)

      val initial_mults = for(i <- 0 until cases.length) yield {
        val mults = for(j <- 0 until 2) yield{
          if(j == 0){
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, 0.0, if(mult_layer_count1>0){true}else{false},true)).io
            mult_instance
          }else{
            val mult_instance = Module(new FPComplexMult_reducable_forSymmetric(bw,0.0,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im, if(mult_layer_count1>0){true}else{false},true)).io
            mult_instance
          }
        }
        mults
      }

      for(i <- 0 until cases.length){
        initial_mults(i)(0).in_a := initial_adds_subs(cases_addr(i)._2)(0).out_s
        initial_mults(i)(0).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re.toString,bw).U
        initial_mults(i)(0).in_b.Im := 0.U
        initial_mults(i)(1).in_a := initial_adds_subs(cases_addr(i)._2)(1).out_s
        initial_mults(i)(1).in_b.Re := 0.U
        initial_mults(i)(1).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im.toString,bw).U
      }

      val stage_adds_subs = for(i <- 0 until cases.length)yield{
        val instances = for(j <- 0 until 2) yield{
          if(j==0){
            val instance = Module(new FPComplexAdder_reducable(bw,DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im, if(subadd_layer_count1>0){true}else{false})).io
            instance
          }else{
            val instance = Module(new FPComplexSub_reducable(bw, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).re, DFTr_Constants(cases_addr_adjusted(i)._1+1)(cases_addr_adjusted(i)._2+1).im,if(subadd_layer_count1>0){true}else{false})).io
            instance
          }
        }
        instances
      }

      for(i <- 0 until cases.length){
        stage_adds_subs(i)(0).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(0).in_b := initial_mults(i)(1).out_s
        stage_adds_subs(i)(1).in_a := initial_mults(i)(0).out_s
        stage_adds_subs(i)(1).in_b := initial_mults(i)(1).out_s
      }

      val fillIn = WireInit(VecInit.fill(inner_inner_square_size)(VecInit.fill(inner_inner_square_size)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw))))))
      for(i <- 0 until inner_inner_square_size){
        for(j <- 0 until inner_inner_square_size){
          if(new_adj_case_sq(i)(j)._2){
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
          }else{
            fillIn(i)(j)(0) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(0).out_s
            fillIn(i)(j)(1) := stage_adds_subs(new_adj_case_sq(i)(j)._1)(1).out_s
          }
        }
      }
      val fpmultiadds = for(i <- 0 until inner_square_size)yield{
        val fpmadds = Module(new FPComplexMultiAdder(inner_inner_square_size,bw)).io
        fpmadds
      }
      for(i <- 0 until 2) {
        for (j <- 0 until inner_inner_square_size) {
          for (k <- 0 until inner_inner_square_size) {
            if(i == 0){
              fpmultiadds(j).in(k) := fillIn(j)(k)(i)
            }else{
              fpmultiadds(inner_square_size - j - 1 ).in(k) := fillIn(j)(k)(i)
            }
          }
        }
      }
      val results = WireInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
      var offst = 0
      for(i <- 0 until r){
        if(i == 0){
          results(0) := top_row_init_sum.out
        }else if(i == r/2){
          results(r/2) := mid_row_init_sum.out
          offst = 1
        }else{
          results(i) := fpmultiadds(i-1-offst).out
        }
      }
      val prep_adder_layer = for(i <- 0 until 2)yield{
        if(i == 0){
          val adder = Module(new FPComplexAdder(bw)).io
          adder
        }else{
          val adder = Module(new FPComplexSub(bw)).io
          adder
        }
      }
      when(io.in_ready) {
        for (i <- 0 until 2) {
          prep_adder_layer(i).in_a := io.in(0)
          prep_adder_layer(i).in_b := io.in(r / 2)
        }
      }.otherwise{
        for (i <- 0 until 2) {
          prep_adder_layer(i).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          prep_adder_layer(i).in_b := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
      val finallayer = if(multlayerlatency+subaddlayerlatency+multadd_latency > 0){RegInit(VecInit.fill(multlayerlatency+subaddlayerlatency+multadd_latency)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw)))))}else{WireInit(VecInit.fill(2)(VecInit.fill(2)(0.U.asTypeOf(new ComplexNum(bw)))))}
      val slctlncy2 = if(subaddlayerlatency + multlayerlatency + multadd_latency > 0){subaddlayerlatency + multlayerlatency + multadd_latency}else{2}

      for(i <- 0 until slctlncy2){
        if(i == 0){
          for(j <- 0 until 2){
            finallayer(0)(j) := prep_adder_layer(j).out_s
          }
        }else{
          finallayer(i) := finallayer(i-1)
        }
      }
      for(i <- 0 until r){}
      val final_adder_layer = for(i <- 0 until r)yield{
        val adder = Module(new FPComplexAdder(bw)).io
        adder
      }
      val out_regs = RegInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until r){
        final_adder_layer(i).in_a := finallayer(slctlncy2 - 1)(i % 2) //io.in(0)
        final_adder_layer(i).in_b := results(i)
        io.out(i) := out_regs(i)
      }
      when(regdelays(total_latency-2)) {
        for (i <- 0 until r) {
          out_regs(i) := final_adder_layer(i).out_s
        }
      }.otherwise{
        for (i <- 0 until r) {
          out_regs(i) := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }

    }
  }

  class DFT(r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(r, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(r, new ComplexNum(bw)))
    })
    val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
    var mult_count = 0
    val mult_ind = mutable.ArrayBuffer[Int]()
    val mult_needs = for(i <- 0 until r-1) yield{
      val multiplier_ind = for(j <- 0 until r-1) yield {
        val n  = DFTr_Constants(i+1)(j+1)
        val c1 = FFT.isReducable(n.re.abs)
        val c2 = FFT.isReducable(n.im.abs)
        if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))){
          mult_count += 1
          mult_ind += (i*(r-1) + j)
          (i, j, 0, false, false, true)
        }else{
          mult_ind += (i*(r-1) + j)
          if((c1._1 && n.im.abs < 0.005)){
            (i,j, c1._2.abs, n.re < -0.0005, false, false)
          }else{
            (i,j, c2._2.abs, n.im < -0.0005, true, false)
          }
        }
      }
      multiplier_ind.toIndexedSeq
    }
    mult_needs.map(x=>println(x.toList))
    val CMultLatency = 2
    val CAddLatency = 1
    var DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).floor.toInt)yield{((r/Math.pow(2,l)).floor.toInt) % 2}).reduce(_+_)) * (CAddLatency) + 1
    if(mult_count == 0){
      DFT_latency = ((Math.log10(r)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).floor.toInt)yield{(r/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency) + 1
    }
    println(s"Teh DFT_NRV Latencyt : ${DFT_latency}")
    val regDelays = RegInit(VecInit.fill(DFT_latency)(false.B)/*(DFT_latency, Bool())*/)
    for(i <- 0 until DFT_latency){
      if(i == 0){
        regDelays(0) := io.in_ready
      }else{
        regDelays(i) := regDelays(i-1)
      }
    }
    val valid_signal = Wire(Bool())
    valid_signal := regDelays(DFT_latency-1)
    io.out_validate := valid_signal
    val adj = mutable.ArrayBuffer[(Int, Int, Int, Boolean, Boolean, Boolean)]()
    val mult_ind_adj = mutable.ArrayBuffer[Int]()
    val adj_non_mult = mutable.ArrayBuffer[(Int, Int, Int, Boolean, Boolean, Boolean)]()
    val mult_ind_adj_non_mult = mutable.ArrayBuffer[Int]()
    for(i <- 0 until r-1){
      for(j <- 0 until r-1){
        if(mult_needs(i)(j)._6){
          adj += mult_needs(i)(j)
          mult_ind_adj += mult_ind(i*(r-1)+j)
        }else{
          adj_non_mult += mult_needs(i)(j)
          mult_ind_adj_non_mult += mult_ind(i*(r-1)+j)
        }
      }
    }
    var cmplx_adjusts = for(i <- 0 until (r-1)*(r-1) - mult_count) yield{
      val cm = Module(new ComplexNum_AdjustOrder(bw)).io
      cm
    }
    val adj_wire = Wire(Vec((r-1)*(r-1), new ComplexNum(bw)))
    for(i <- 0 until (r-1)*(r-1) - mult_count){
      cmplx_adjusts(i).in := io.in(adj_non_mult(i)._2 + 1)
      cmplx_adjusts(i).in_adj := adj_non_mult(i)._3.U
      cmplx_adjusts(i).is_neg := adj_non_mult(i)._4.B
      cmplx_adjusts(i).is_flip := adj_non_mult(i)._5.B
      adj_wire(mult_ind_adj_non_mult(i)) := cmplx_adjusts(i).out
    }
    for(i <- 0 until mult_count){
      adj_wire(mult_ind_adj(i)) := io.in(adj(i)._2 + 1)
    }
    val FP_adds = (for (i <- 0 until r) yield {
      val fpadds = Module(new FPComplexMultiAdder(r, bw)).io
      fpadds
    }).toVector
    if(mult_count != 0) {
      var FP_Mults = (for (i <- 0 until mult_count) yield {
        val fpmult = Module(new FPComplexMult_reducable_SimpleCases(bw,DFTr_Constants(adj(i)._1 + 1)(adj(i)._2 + 1).re, DFTr_Constants(adj(i)._1 + 1)(adj(i)._2 + 1).im)).io
        fpmult
      })
      when(io.in_ready) {
        for (k <- 0 until mult_count) {
          FP_Mults(k).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(adj(k)._1 + 1)(adj(k)._2 + 1).re.toString, bw).U
          FP_Mults(k).in_a := adj_wire(mult_ind_adj(k)) //io.in(mult_ind_adj(k))
          FP_Mults(k).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(adj(k)._1 + 1)(adj(k)._2 + 1).im.toString, bw).U
        }
      }.otherwise {
        for (k <- 0 until mult_count) {
          FP_Mults(k).in_a.Re := 0.U
          FP_Mults(k).in_b.Re := 0.U
          FP_Mults(k).in_b.Im := 0.U
          FP_Mults(k).in_a.Im := 0.U
        }
      }
      val total_reg = r + ((r-1)*(r-1) - mult_count)
      val reg_syncs = (for (i <- 0 until CMultLatency) yield {
        val regs = RegInit(VecInit.fill(total_reg)(0.U.asTypeOf(new ComplexNum(bw))))
        //val regs = Reg(Vec(total_reg, new ComplexNum(bw)))
        regs
      }).toVector
      for (i <- 0 until CMultLatency) {
        if (i == 0) {
          when(io.in_ready) {
            for (j <- 0 until r) {
              reg_syncs(0)(j) := io.in(j)
            }
            for (j <- 0 until ((r-1)*(r-1))-mult_count){
              reg_syncs(0)(j+r) := adj_wire(mult_ind_adj_non_mult(j))
            }
          }.otherwise {
            for (j <- 0 until total_reg) {
              reg_syncs(i)(j).Re := 0.U
              reg_syncs(i)(j).Im := 0.U
            }
          }
        } else {
          reg_syncs(i) := reg_syncs(i - 1)
        }
      }
      val mult_results = Wire(Vec(r, Vec(r, new ComplexNum(bw))))
      for (i <- 0 until r) {
        mult_results(0)(i) := reg_syncs(CMultLatency - 1)(i)
        if (i != 0)
          mult_results(i)(0) := reg_syncs(CMultLatency - 1)(0)
      }
      for(i <- 0 until ((r-1)*(r-1)) - mult_count){
         mult_results(adj_non_mult(i)._1 + 1)(adj_non_mult(i)._2 + 1) := reg_syncs(CMultLatency-1)(i+r)
      }
      for(i <- 0 until mult_count){
        mult_results(adj(i)._1 + 1)(adj(i)._2 + 1) := FP_Mults(i).out_s
      }
      for (i <- 0 until r) {
        for (j <- 0 until r) {
          FP_adds(i).in(j) := mult_results(i)(j)
        }
      }
    }else{
      for (i <- 0 until r) {
        if(i == 0){
          for (j <- 0 until r) {
            FP_adds(i).in(j) := io.in(j)
          }
        }else{
          for (j <- 0 until r) {
            if(j == 0){
              FP_adds(i).in(0) := io.in(0)
            }else{
              FP_adds(i).in(j) := adj_wire((i-1)*(r-1) + j-1)
            }
          }
        }
      }
    }
    val result = RegInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
    //val result = Reg(Vec(r, new ComplexNum(bw)))
    when(regDelays(DFT_latency-2)) {
      for (i <- 0 until r) {
        result(i) := FP_adds(i).out
      }
    }.otherwise{
      for(i <- 0 until r){
        result(i).Re := 0.U
        result(i).Im := 0.U
      }
    }
    for(i <- 0 until r) {
      io.out(i) := result(i)
    }
  }

  class DFT_NRV(r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(r, new ComplexNum(bw)))
      val out = Output(Vec(r, new ComplexNum(bw)))
    })
    val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
    var mult_count = 0
    val mult_ind = mutable.ArrayBuffer[Int]()
    val mult_needs = for(i <- 0 until r-1) yield{
      val multiplier_ind = for(j <- 0 until r-1) yield {
        val n  = DFTr_Constants(i+1)(j+1)
        val c1 = FFT.isReducable(n.re.abs)
        val c2 = FFT.isReducable(n.im.abs)
        if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))){
          mult_count += 1
          mult_ind += (i*(r-1) + j)
          (i, j, 0, false, false, true)
        }else{
          mult_ind += (i*(r-1) + j)
          if((c1._1 && n.im.abs < 0.005)){
            (i,j, c1._2.abs, n.re < -0.0005, false, false)
          }else{
            (i,j, c2._2.abs, n.im < -0.0005, true, false)
          }
        }
      }
      multiplier_ind.toIndexedSeq
    }
    val CMultLatency = 2
    val CAddLatency = 1
    var DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).floor.toInt)yield{((r/Math.pow(2,l)).floor.toInt) % 2}).reduce(_+_)) * (CAddLatency)
    if(mult_count == 0){
      DFT_latency = ((Math.log10(r)/Math.log10(2)).floor.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).floor.toInt)yield{(r/Math.pow(2,l)).floor.toInt % 2}).reduce(_+_)) * (CAddLatency)
    }
    println(s"Teh DFT_NRV Latencyt : ${DFT_latency}")
    val adj = mutable.ArrayBuffer[(Int, Int, Int, Boolean, Boolean, Boolean)]()
    val mult_ind_adj = mutable.ArrayBuffer[Int]()
    val adj_non_mult = mutable.ArrayBuffer[(Int, Int, Int, Boolean, Boolean, Boolean)]()
    val mult_ind_adj_non_mult = mutable.ArrayBuffer[Int]()
    for(i <- 0 until r-1){
      for(j <- 0 until r-1){
        if(mult_needs(i)(j)._6){
          adj += mult_needs(i)(j)
          mult_ind_adj += mult_ind(i*(r-1)+j)
        }else{
          adj_non_mult += mult_needs(i)(j)
          mult_ind_adj_non_mult += mult_ind(i*(r-1)+j)
        }
      }
    }
    var cmplx_adjusts = for(i <- 0 until (r-1)*(r-1) - mult_count) yield{
      val cm = Module(new ComplexNum_AdjustOrder(bw)).io
      cm
    }
    //    var cmplx_adjusts = for(i <- 0 until r-1)yield{
    //      var c_mod = for(j <- 0 until r-1)yield{
    //        val cm = Module(new  ComplexNum_AdjustOrder(bw)).io
    //        cm
    //      }
    //      c_mod
    //    }
    val adj_wire = Wire(Vec((r-1)*(r-1), new ComplexNum(bw)))
    for(i <- 0 until (r-1)*(r-1) - mult_count){
      cmplx_adjusts(i).in := io.in(adj_non_mult(i)._2 + 1)
      cmplx_adjusts(i).in_adj := adj_non_mult(i)._3.U
      cmplx_adjusts(i).is_neg := adj_non_mult(i)._4.B
      cmplx_adjusts(i).is_flip := adj_non_mult(i)._5.B
      adj_wire(mult_ind_adj_non_mult(i)) := cmplx_adjusts(i).out
    }
    for(i <- 0 until mult_count){
      adj_wire(mult_ind_adj(i)) := io.in(adj(i)._2 + 1)
    }
    //    for(i <- 0 until r-1){
    //      for(j <- 0 until r-1){
    //        cmplx_adjusts(i)(j).in := io.in(1+j)
    //        cmplx_adjusts(i)(j).in_adj := mult_needs(i)(j)._3.U
    //        cmplx_adjusts(i)(j).is_neg := mult_needs(i)(j)._4.B
    //        cmplx_adjusts(i)(j).is_flip := mult_needs(i)(j)._5.B
    //        adj_wire(i*(r-1)+j) := cmplx_adjusts(i)(j).out
    //      }
    //    }
    val FP_adds = (for (i <- 0 until r) yield {
      val fpadds = Module(new FPComplexMultiAdder(r, bw)).io
      fpadds
    }).toVector
    if(mult_count != 0) {
      var FP_Mults = (for (i <- 0 until mult_count) yield {
        val fpmult = Module(new FPComplexMult_reducable_SimpleCases(bw, DFTr_Constants(adj(i)._1 + 1)(adj(i)._2 + 1).re, DFTr_Constants(adj(i)._1 + 1)(adj(i)._2 + 1).im)).io
        fpmult
      })
      for (k <- 0 until mult_count) {
        FP_Mults(k).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(adj(k)._1 + 1)(adj(k)._2 + 1).re.toString, bw).U
        FP_Mults(k).in_a := adj_wire(mult_ind_adj(k)) //io.in(mult_ind_adj(k))
        FP_Mults(k).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(adj(k)._1 + 1)(adj(k)._2 + 1).im.toString, bw).U
      }
      val total_reg = r + ((r-1)*(r-1) - mult_count)
      val reg_syncs = (for (i <- 0 until CMultLatency) yield {
        val regs = RegInit(VecInit.fill(total_reg)(0.U.asTypeOf(new ComplexNum(bw))))
        //val regs = Reg(Vec(total_reg, new ComplexNum(bw)))
        regs
      }).toVector
      for (i <- 0 until CMultLatency) {
        if (i == 0) {
          for (j <- 0 until r) {
            reg_syncs(0)(j) := io.in(j)
          }
          for (j <- 0 until ((r-1)*(r-1))-mult_count){
            reg_syncs(0)(j+r) := adj_wire(mult_ind_adj_non_mult(j))
          }
        } else {
          reg_syncs(i) := reg_syncs(i - 1)
        }
      }
      val mult_results = Wire(Vec(r, Vec(r, new ComplexNum(bw))))
      for (i <- 0 until r) {
        mult_results(0)(i) := reg_syncs(CMultLatency - 1)(i)
        if (i != 0)
          mult_results(i)(0) := reg_syncs(CMultLatency - 1)(0)
      }
      for(i <- 0 until ((r-1)*(r-1)) - mult_count){
        mult_results(adj_non_mult(i)._1 + 1)(adj_non_mult(i)._2 + 1) := reg_syncs(CMultLatency-1)(i+r)
      }
      for(i <- 0 until mult_count){
        mult_results(adj(i)._1 + 1)(adj(i)._2 + 1) := FP_Mults(i).out_s
      }
      for (i <- 0 until r) {
        for (j <- 0 until r) {
          FP_adds(i).in(j) := mult_results(i)(j)
        }
      }
    }else{
      for (i <- 0 until r) {
        if(i == 0){
          for (j <- 0 until r) {
            FP_adds(i).in(j) := io.in(j)
          }
        }else{
          for (j <- 0 until r) {
            if(j == 0){
              FP_adds(i).in(0) := io.in(0)
            }else{
              FP_adds(i).in(j) := adj_wire((i-1)*(r-1) + j-1)
            }
          }
        }
      }
    }
    val result = WireInit(VecInit.fill(r)(0.U.asTypeOf(new ComplexNum(bw))))
    //val result = Wire(Vec(r, new ComplexNum(bw)))
    for (i <- 0 until r) {
      result(i) := FP_adds(i).out
    }

    for(i <- 0 until r) {
      io.out(i) := result(i)
    }
  }

  class TwiddleFactorROM_forStreaming(N: Int, r: Int, w: Int, stage: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in_addr = Input(UInt(log2Ceil(N).W))
      val out_data = Output(Vec(w, new ComplexNum(bw)))
    })
    val Twid_Constants = Permutations.T2(N,r)(stage)
    val Twid_map_2D = for(i <- 0 until N/w)yield{
      val tm = for(j <- 0 until w) yield{
        Twid_Constants(i*w+j)
      }
      tm
    }
    val initial_vec_map_re = for(i <- 0 until N/w) yield{
      VecInit(Twid_map_2D(i).map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U(bw.W)))
    }
    val initial_vec_map_im = for(i <- 0 until N/w) yield{
      VecInit(Twid_map_2D(i).map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U(bw.W)))
    }
    val vec_map_re = VecInit(initial_vec_map_re)
    val vec_map_im = VecInit(initial_vec_map_im)
    for(i <- 0 until w){
      io.out_data(i).Re := vec_map_re(io.in_addr)(i)
      io.out_data(i).Im := vec_map_im(io.in_addr)(i)
    }
  }

  class TwiddleFactorROM_mr_forStreaming(N: Int, s: Int, r: Int, w: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in_addr = Input(UInt(log2Ceil(N).W))
      val out_data = Output(Vec(w, new ComplexNum(bw)))
    })
    val Twid_Constants = Permutations.T2_rs(N,s,r)
    val Twid_map_2D = for(i <- 0 until N/w)yield{
      val tm = for(j <- 0 until w) yield{
        Twid_Constants(i*w+j)
      }
      tm
    }
    val initial_vec_map_re = for(i <- 0 until N/w) yield{
      VecInit(Twid_map_2D(i).map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U(bw.W)))
    }
    val initial_vec_map_im = for(i <- 0 until N/w) yield{
      VecInit(Twid_map_2D(i).map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U(bw.W)))
    }
    val vec_map_re = VecInit(initial_vec_map_re)
    val vec_map_im = VecInit(initial_vec_map_im)
    for(i <- 0 until w){
      io.out_data(i).Re := vec_map_re(io.in_addr)(i)
      io.out_data(i).Im := vec_map_im(io.in_addr)(i)
    }
  }

  class TwiddleFactorsStreamed(N: Int, r:Int, w: Int, stage:Int, bw: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      //val in_en = Input(Bool())
      val in_en = Input(Vec(2, Bool()))
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val DFTr_Constants = Permutations.T2(N,r)(stage)
    var mults = 0
    val Twid_ROM = Module(new TwiddleFactorROM_forStreaming(N,r,w,stage,bw)).io
    for(i <- 0 until N){
      if(DFTr_Constants(i).re.abs > 0.00000001 && DFTr_Constants(i).im.abs > 0.00000001){
        mults += 1
      }
    }
    if(mults > 0) {
      val is_enabled = RegInit(false.B)
      val cnt = RegInit(0.U(log2Ceil(N/w).W))
      when(io.in_en.asUInt.orR){
        is_enabled := true.B
      }.otherwise{
        is_enabled := false.B
      }
      val multipliers = for (i <- 0 until w) yield {
        val instance = Module(new FPComplexMult(bw)).io
        instance
      }
      when(io.in_en.asUInt.orR){
        when(cnt === ((N/w)-1).U){
          cnt := 0.U
        }.otherwise{
          cnt := cnt + 1.U
        }
        for(i <- 0 until w){
          multipliers(i).in_a := io.in(i)
          multipliers(i).in_b := Twid_ROM.out_data(i)
        }
      }.otherwise{
        for(i <- 0 until w){
          multipliers(i).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          multipliers(i).in_b := 0.U.asTypeOf(new ComplexNum(bw))
        }
        cnt := 0.U
      }
      for(i <- 0 until w){
        io.out(i) := multipliers(i).out_s
      }
      Twid_ROM.in_addr := cnt
    }else{
      val is_enabled = RegInit(false.B)
      val cnt = RegInit(0.U(log2Ceil(N/w).W))
      when(io.in_en.asUInt.orR){
        is_enabled := true.B
      }.otherwise{
        is_enabled := false.B
      }
      val multipliers = for (i <- 0 until w) yield {
        val instance = Module(new ComplexNum_AdjustOrder(bw)).io
        instance
      }
      when(io.in_en.asUInt.orR){
        when(cnt === ((N/w)-1).U){
          cnt := 0.U
        }.otherwise{
          cnt := cnt + 1.U
        }
        for(i <- 0 until w){
          multipliers(i).in := io.in(i)
          multipliers(i).in_adj := 0.U
          when(Twid_ROM.out_data(i).Re(bw-2,0) === convert_string_to_IEEE_754("1.0", bw).U){
            multipliers(i).is_flip := false.B
            when(Twid_ROM.out_data(i).Re(bw-1) === 1.U){
              multipliers(i).is_neg := true.B
            }.otherwise{
              multipliers(i).is_neg := false.B
            }
          }.otherwise{
            multipliers(i).is_flip := true.B
            when(Twid_ROM.out_data(i).Im(bw-1) === 1.U){
              multipliers(i).is_neg := true.B
            }.otherwise{
              multipliers(i).is_neg := false.B
            }
          }
        }
      }.otherwise{
        cnt := 0.U
        for(i <- 0 until w){
          multipliers(i).in := 0.U.asTypeOf(new ComplexNum(bw))
          multipliers(i).in_adj := 0.U
          multipliers(i).is_flip := false.B
          multipliers(i).is_neg := false.B
        }
      }
      val result_regs = RegInit(VecInit.fill(2)(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw)))))
      for(j <- 0 until 2) {
        if(j == 0) {
          for (i <- 0 until w) {
            result_regs(0)(i) := multipliers(i).out
          }
        }else{
          result_regs(j) := result_regs(j-1)
        }
      }
      io.out := result_regs(1)
      Twid_ROM.in_addr := cnt
    }
  }

  class TwiddleFactorsStreamed_mr(N: Int, s:Int, r:Int, w: Int, bw: Int, delay: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      //val in_en = Input(Bool())
      val in_en = Input(Vec(2, Bool()))
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val DFTr_Constants = Permutations.T2_rs(N,s,r)
    var mults = 0
    val Twid_ROM = Module(new TwiddleFactorROM_mr_forStreaming(N,s,r,w,bw)).io
    for(i <- 0 until N){
      if(DFTr_Constants(i).re.abs > 0.00000001 && DFTr_Constants(i).im.abs > 0.00000001){
        mults += 1
      }
    }
    if(mults > 0) {
      val is_enabled = RegInit(false.B)
      val cnt = RegInit(0.U(log2Ceil(N/w).W))
      val cnt2 = RegInit(0.U(log2Ceil(delay).W))
      when(io.in_en.asUInt.orR){
        is_enabled := true.B
      }.otherwise{
        is_enabled := false.B
      }
      val multipliers = for (i <- 0 until w) yield {
        val instance = Module(new FPComplexMult(bw)).io
        instance
      }
      when(io.in_en.asUInt.orR){
        when(cnt2 === (delay - 1).U){
          cnt2 := 0.U
          cnt := 0.U
        }.elsewhen(cnt === ((N/w)-1).U && cnt2 =/= (delay - 1).U){
          cnt := cnt
          cnt2 := cnt2 + 1.U
        }.otherwise{
          cnt := cnt + 1.U
          cnt2 := cnt2 + 1.U
        }
        for(i <- 0 until w){
          multipliers(i).in_a := io.in(i)
          multipliers(i).in_b := Twid_ROM.out_data(i)
        }
      }.otherwise{
        for(i <- 0 until w){
          multipliers(i).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          multipliers(i).in_b := 0.U.asTypeOf(new ComplexNum(bw))
        }
        cnt := 0.U
        cnt2 := 0.U
      }
      for(i <- 0 until w){
        io.out(i) := multipliers(i).out_s
      }
      Twid_ROM.in_addr := cnt
    }else{
      val is_enabled = RegInit(false.B)
      val cnt = RegInit(0.U(log2Ceil(N/w).W))
      val cnt2 = RegInit(0.U(log2Ceil(delay).W))
      when(io.in_en.asUInt.orR){
        is_enabled := true.B
      }.otherwise{
        is_enabled := false.B
      }
      val multipliers = for (i <- 0 until w) yield {
        val instance = Module(new ComplexNum_AdjustOrder(bw)).io
        instance
      }
      when(io.in_en.asUInt.orR){
        when(cnt2 === (delay - 1).U){
          cnt2 := 0.U
          cnt := 0.U
        }.elsewhen(cnt === ((N/w)-1).U && cnt2 =/= (delay - 1).U){
          cnt := cnt
          cnt2 := cnt2 + 1.U
        }.otherwise{
          cnt := cnt + 1.U
          cnt2 := cnt2 + 1.U
        }
        for(i <- 0 until w){
          multipliers(i).in := io.in(i)
          multipliers(i).in_adj := 0.U
          when(Twid_ROM.out_data(i).Re(bw-2,0) === convert_string_to_IEEE_754("1.0", bw).U){
            multipliers(i).is_flip := false.B
            when(Twid_ROM.out_data(i).Re(bw-1) === 1.U){
              multipliers(i).is_neg := true.B
            }.otherwise{
              multipliers(i).is_neg := false.B
            }
          }.otherwise{
            multipliers(i).is_flip := true.B
            when(Twid_ROM.out_data(i).Im(bw-1) === 1.U){
              multipliers(i).is_neg := true.B
            }.otherwise{
              multipliers(i).is_neg := false.B
            }
          }
        }
      }.otherwise{
        cnt := 0.U
        cnt2 := 0.U
        for(i <- 0 until w){
          multipliers(i).in := 0.U.asTypeOf(new ComplexNum(bw))
          multipliers(i).in_adj := 0.U
          multipliers(i).is_flip := false.B
          multipliers(i).is_neg := false.B
        }
      }
      val result_regs = RegInit(VecInit.fill(2)(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw)))))
      for(j <- 0 until 2) {
        if(j == 0) {
          for (i <- 0 until w) {
            result_regs(0)(i) := multipliers(i).out
          }
        }else{
          result_regs(j) := result_regs(j-1)
        }
      }
      io.out := result_regs(1)
      Twid_ROM.in_addr := cnt
    }
  }

  class TwiddleFactorsStreamed_and_Iterative(N: Int, r:Int, w: Int, bw: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      //val in_en = Input(Bool())
      val in_twid_data = Input(Vec(w, new ComplexNum(bw)))
      val in_en = Input(Vec(2, Bool()))
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_cnt = Output(UInt(log2Ceil(N/w).W))
    })
    val list_of_twids = Permutations.T2(N,r)
    val initial_twid = for(i <- 0 until N) yield{
      cmplx(1,0)
    }
    val adjusted_rom_array = for(i <- 0 until (Math.log10(N)/Math.log10(r)).round.toInt) yield{
      if(i == 0){
        initial_twid.toArray
      }else{
        list_of_twids(i-1)
      }
    }
    var mults = 0
    for(j <- 0 until (Math.log10(N)/Math.log10(r)).round.toInt) {
      for (i <- 0 until N) {
        if (adjusted_rom_array(j)(i).re.abs > 0.00000001 && adjusted_rom_array(j)(i).im.abs > 0.00000001) {
          mults += 1
        }
      }
    }
    if(mults > 0) {
      val is_enabled = RegInit(false.B)
      val cnt = RegInit(0.U(log2Ceil(N/w).W))
      val multipliers = for (i <- 0 until w) yield {
        val instance = Module(new FPComplexMult(bw)).io
        instance
      }
      when(io.in_en.asUInt.orR){
        when(cnt === ((N/w)-1).U){
          cnt := 0.U
        }.otherwise{
          cnt := cnt + 1.U
        }
        for(i <- 0 until w){
          multipliers(i).in_a := io.in(i)
          multipliers(i).in_b := io.in_twid_data(i)
        }
      }.otherwise{
        for(i <- 0 until w){
          multipliers(i).in_a := 0.U.asTypeOf(new ComplexNum(bw))
          multipliers(i).in_b := 0.U.asTypeOf(new ComplexNum(bw))
        }
        cnt := 0.U
      }
      for(i <- 0 until w){
        io.out(i) := multipliers(i).out_s
      }
      io.out_cnt := cnt
    }else{
      val is_enabled = RegInit(false.B)
      val cnt = RegInit(0.U(log2Ceil(N/w).W))
      val multipliers = for (i <- 0 until w) yield {
        val instance = Module(new ComplexNum_AdjustOrder(bw)).io
        instance
      }
      when(io.in_en.asUInt.orR){
        when(cnt === ((N/w)-1).U){
          cnt := 0.U
        }.otherwise{
          cnt := cnt + 1.U
        }
        for(i <- 0 until w){
          multipliers(i).in := io.in(i)
          multipliers(i).in_adj := 0.U
          when(io.in_twid_data(i).Re(bw-2,0) === convert_string_to_IEEE_754("1.0", bw).U){
            multipliers(i).is_flip := false.B
            when(io.in_twid_data(i).Re(bw-1) === 1.U){
              multipliers(i).is_neg := true.B
            }.otherwise{
              multipliers(i).is_neg := false.B
            }
          }.otherwise{
            multipliers(i).is_flip := true.B
            when(io.in_twid_data(i).Im(bw-1) === 1.U){
              multipliers(i).is_neg := true.B
            }.otherwise{
              multipliers(i).is_neg := false.B
            }
          }
        }
      }.otherwise{
        cnt := 0.U
        for(i <- 0 until w){
          multipliers(i).in := 0.U.asTypeOf(new ComplexNum(bw))
          multipliers(i).in_adj := 0.U
          multipliers(i).is_flip := false.B
          multipliers(i).is_neg := false.B
        }
      }
      io.out_cnt := cnt
      val result_regs = RegInit(VecInit.fill(2)(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw)))))
      for(j <- 0 until 2) {
        if(j == 0) {
          for (i <- 0 until w) {
            result_regs(0)(i) := multipliers(i).out
          }
        }else{
          result_regs(j) := result_regs(j-1)
        }
      }
      io.out := result_regs(1)
    }
  }

  class TwiddleFactors(N: Int, r:Int, w: Int, stage:Int, bw: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    val locations = mutable.ArrayBuffer[(Int, Int )]()
    for(i <- 0 until TotalStages-1){
      for(j <- 0 until N){
        if(Permutations.T2(N,r)(i)(j).re == Permutations.T2(N,r)(i)(j).im){
          val possibility = j
          for(k <- i + 1 until TotalStages){
            if(Permutations.T2(N,r)(k)(j).re == Permutations.T2(N,r)(k)(j).im){
              //locations += (k, j)
            }
          }
        }
      }
    }
    val DFTr_Constants = Permutations.T2(N,r)(stage)
    var mults = 0
    for(i <- 0 until N){
      if(DFTr_Constants(i).re.abs > 0.00000001 && DFTr_Constants(i).im.abs > 0.00000001){
        mults += 1
      }
    }
    val addregs = if(mults > 0){true}else{false} // it seems not too hard to optimize, but I'll come back to this since it is simpler than mixed-radix streaming
    val multipliers = for(i <- 0 until N) yield{
      val instance = Module(new FPComplexMult_reducable_forSymmetric(bw, DFTr_Constants(i).re, DFTr_Constants(i).im, addregs, false)).io
      instance
    }
    for(i <- 0 until N){
      multipliers(i).in_a := io.in(i)
      multipliers(i).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(i).re.toString, bw).U
      multipliers(i).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(i).im.toString, bw).U
      io.out(i) := multipliers(i).out_s
    }
  }

  class TwiddleFactors_NoReduction(N: Int, r:Int, w: Int, bw: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_twid_data = Input(Vec(N, new ComplexNum(bw)))
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    val multipliers = for(i <- 0 until N) yield{
      val instance = Module(new FPComplexMult(bw)).io
      instance
    }
    for(i <- 0 until N){
      multipliers(i).in_a := io.in(i)
      multipliers(i).in_b.Re := io.in_twid_data(i).Re
      multipliers(i).in_b.Im := io.in_twid_data(i).Im
      io.out(i) := multipliers(i).out_s
    }
  }

  class TwiddleFactors_mr(N: Int, r:Int, s: Int, w: Int, bw: Int) extends Module { //mixed radix twiddle factors
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    val DFTr_Constants = Permutations.T2_rs(N,s,r)
    var mult_count = 0
    val mult_ind = mutable.ArrayBuffer[Int]()
    val mult_needs = for(i <- 0 until w) yield{
      val n  = DFTr_Constants(i)
      val c1 = FFT.isReducable(n.re.abs)
      val c2 = FFT.isReducable(n.im.abs)
      if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))){
        mult_count += 1
        mult_ind += (i)
        (i, 0, false, false, true)
      }else{
        mult_ind += (i)
        if((c1._1 && n.im.abs < 0.005)){
          (i, c1._2.abs, n.re < -0.0005, false, false)
        }else{
          (i, c2._2.abs, n.im < -0.0005, true, false)
        }
      }
    }
    val CMultLatency = 2
    var T_latency = CMultLatency
    if(mult_count == 0){
      T_latency = 0
    }
    val TwiddleFactorConstantsRe = VecInit(Permutations.T2_rs(N,s,r).map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U))
    Permutations.T2_rs(N,s,r).map(x=>x.print_complex)
    val TwiddleFactorConstantsIm = VecInit(Permutations.T2_rs(N,s,r).map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U))
    val TotalCycles = N/w
    val offset = w
    val adj = mutable.ArrayBuffer[(Int, Int, Boolean, Boolean, Boolean)]()
    val mult_ind_adj = mutable.ArrayBuffer[Int]()
    val adj_non_mult = mutable.ArrayBuffer[(Int, Int, Boolean, Boolean, Boolean)]()
    val mult_ind_adj_non_mult = mutable.ArrayBuffer[Int]()
    for(i <- 0 until w){
      if(mult_needs(i)._5){
        adj += mult_needs(i)
        mult_ind_adj += mult_ind(i)
      }else{
        adj_non_mult += mult_needs(i)
        mult_ind_adj_non_mult += mult_ind(i)
      }
    }
    var cmplx_adjusts = for(i <- 0 until w - mult_count) yield{
      val cm = Module(new ComplexNum_AdjustOrder(bw)).io
      cm
    }
    val adj_wire = Wire(Vec(w, new ComplexNum(bw)))
    for(i <- 0 until w - mult_count){
      cmplx_adjusts(i).in := io.in(adj_non_mult(i)._1)
      cmplx_adjusts(i).in_adj := adj_non_mult(i)._2.U
      cmplx_adjusts(i).is_neg := adj_non_mult(i)._3.B
      cmplx_adjusts(i).is_flip := adj_non_mult(i)._4.B
      adj_wire(mult_ind_adj_non_mult(i)) := cmplx_adjusts(i).out
    }
    for(i <- 0 until mult_count){
      adj_wire(mult_ind_adj(i)) := io.in(adj(i)._1)
    }
    if(mult_count != 0) {
      val FPMults = (for (i <- 0 until mult_count) yield {
        val fpmult = Module(new FPComplexMult_reducable_SimpleCases(bw, DFTr_Constants(adj(i)._1).re, DFTr_Constants(adj(i)._1).im)).io
        fpmult
      }).toVector
      for (k <- 0 until mult_count) {
        FPMults(k).in_b.Re := convert_string_to_IEEE_754(DFTr_Constants(adj(k)._1).re.toString, bw).U
        FPMults(k).in_a := adj_wire(mult_ind_adj(k)) //io.in(mult_ind_adj(k))
        FPMults(k).in_b.Im := convert_string_to_IEEE_754(DFTr_Constants(adj(k)._1).im.toString, bw).U
      }
      val total_reg = (w - mult_count)
      val reg_syncs = (for (i <- 0 until CMultLatency) yield {
        val regs = RegInit(VecInit.fill(total_reg)(0.U.asTypeOf(new ComplexNum(bw))))
        regs
      }).toVector
      for (i <- 0 until CMultLatency) {
        if (i == 0) {
          for (j <- 0 until w-mult_count){
            reg_syncs(0)(j) := adj_wire(mult_ind_adj_non_mult(j))
          }
        } else {
          reg_syncs(i) := reg_syncs(i - 1)
        }
      }
      val mult_results = Wire(Vec(w, new ComplexNum(bw)))
      for(i <- 0 until (w - mult_count)){
        mult_results(adj_non_mult(i)._1) := reg_syncs(CMultLatency-1)(i)
      }
      for(i <- 0 until mult_count){
        mult_results(adj(i)._1) := FPMults(i).out_s
      }
      for(i <- 0 until w){
        io.out(i) := mult_results(i)
      }
    }else{
      io.out := adj_wire
    }
  }

  class PermutationsSimple(N:Int, r: Int, Type: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val out = Output(Vec(N, new ComplexNum(bw)))
    })
    val PermutationModel = (if(Type == 1){Permutations.R[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}else{Permutations.L[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}).toVector
    for(i <- 0 until N){
      io.out(i) := io.in(PermutationModel(i))
    }
  }

  class Permutations_forIterative(N:Int, r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val in_perm_data = Input(Vec(N, UInt(log2Ceil(N).W)))
      val out = Output(Vec(N, new ComplexNum(bw)))
    })
    val results = RegInit(VecInit.fill(N)(0.U.asTypeOf(new ComplexNum(bw))))
    for(i <- 0 until N){
      results(i) := io.in(io.in_perm_data(i))
    }
    io.out := results
  }

  class PermutationsWithStreaming(N:Int, r: Int, base_r: Int, w:Int, ptype: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_en = Input(Vec((N/w)*2 + 1, Bool()))
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_t = Output(UInt(log2Ceil(N/w).W))
    })
    val is_enabled = Reg(Bool())
    val offset = (N/w).U
    val offset_switch = Reg(UInt(1.W))
    when(io.in_en.asUInt.orR){
      is_enabled := true.B
    }.otherwise{
      is_enabled := false.B
    }
    val M0 = (for(i <- 0 until w) yield{
      val M0_t = Module(new RAM_Block(N,w,bw)).io
      M0_t
    }).toVector
    val M1 = (for(i <- 0 until w) yield{
      val M1_t = Module(new RAM_Block(N,w,bw)).io
      M1_t
    }).toVector
    val Perm_Mod = Module(new Permute_Unit_Streaming(w, bw)).io
    val M0_config = Module(new M0_Config_ROM(N,r,base_r,ptype,w)).io
    val M1_config = Module(new M1_Config_ROM(N,r,base_r, ptype, w)).io
    val Perm_Config = Module(new Streaming_Permute_Config(N,r, base_r, ptype, w)).io
    val cnt = RegInit(0.U(log2Ceil(N/w).W))
    when(io.in_en.asUInt.orR){
      when(cnt === ((N/w)-1).U){
        cnt := 0.U
        offset_switch := ~offset_switch
      }.otherwise{
        cnt := cnt + 1.U
        offset_switch := offset_switch
      }
      for(i <- 0 until w){
        M0(i).en := true.B
        M0(i).re := true.B
        M0(i).wr := true.B
        M1(i).en := true.B
        M1(i).re := true.B
        M1(i).wr := true.B
        M0(i).in_raddr := M0_config.out(i) + (offset * ~offset_switch)
        M0(i).in_waddr := cnt + (offset * offset_switch)
        M0(i).in_data := io.in(i)
        M1(i).in_raddr := cnt + (offset * ~offset_switch)
        M1(i).in_waddr := M1_config.out(i) + (offset * offset_switch)
        M1(i).in_data := Perm_Mod.out(i)
        Perm_Mod.in_config(i) := Perm_Config.out(i)
        Perm_Mod.in(i) := M0(i).out_data
        Perm_Mod.in_en := true.B
        io.out(i) := M1(i).out_data
      }
    }.otherwise{
      for(i <- 0 until w){
        M0(i).en := false.B
        M0(i).re := false.B
        M0(i).wr := false.B
        M1(i).en := false.B
        M1(i).re := false.B
        M1(i).wr := false.B
        M0(i).in_raddr := 0.U
        M0(i).in_waddr := 0.U
        M0(i).in_data := io.in(i)
        M1(i).in_raddr := 0.U
        M1(i).in_waddr := 0.U
        M1(i).in_data := Perm_Mod.out(i)
        Perm_Mod.in_config(i) := Perm_Config.out(i)
        Perm_Mod.in(i) := M0(i).out_data
        Perm_Mod.in_en := false.B
        io.out(i) := M1(i).out_data
      }
      offset_switch := 0.U
      cnt := 0.U
    }
    M0_config.in_cnt := cnt
    M1_config.in_cnt := cnt
    Perm_Config.in_cnt := cnt
    io.out_t := cnt
  }

  class PermutationsWithStreaming_mr(N:Int, r: Int, base_r: Int, w:Int, win:Int, ptype: Int, bw: Int, delay: Int) extends Module{
    val ccs = if(win>w){N/w}else{N/win}
    val io = IO(new Bundle() {
      val in = Input(Vec(win, new ComplexNum(bw))) // win inputs at time
      val in_en = Input(Vec(ccs*2 + 1, Bool())) // it seems that latency will be around (N/win)*2
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_t = Output(Vec(w , UInt(log2Ceil(N/w).W)))
    })
    val is_enabled = Reg(Bool()) // not used anymore
    val offset = (N/w).U
    val offset_switch = Reg(UInt(1.W))
    if(win < w) {
      val M0 = VecInit(for(i <- 0 until w) yield{ //has w memory ports, each with N/w * 2 addresses
        val M0_t = Module(new RAM_Block(N,w,bw)).io
        M0_t
      })
      val M1 = VecInit(for(i <- 0 until w) yield{ // same as M1 //lets make it a vec
        val M1_t = Module(new RAM_Block(N,w,bw)).io // this should be valid since all the parameters are the same
        M1_t
      })
      val modded_list = for (i <- 0 until N/w) yield { // setting up a mapping for writing into the M0 block
        for (j <- 0 until w) yield {
          //        (j,listlol2(i*9+ j))
          (i, j)
        }
      }
      val modded_list2 = modded_list.flatten
      val modded_list3 = for (i <- 0 until N/win) yield {// too large of an array was causing problems, in reality it should be (N/win)
        for (j <- 0 until win) yield {
          modded_list2(i * win + j)
        }
      }
      val M0_access_addr = modded_list3.map(x => x.map(y => y._1))
      println("Hello")
      M0_access_addr.map(x => println(x.toList))
      println()
      val M0_access_port = modded_list3.map(x => x.map(y => y._2))
      M0_access_port.map(x => println(x.toList))
      println()
      val M0_vec_addr = for (i <- 0 until N/win) yield {
        val V = VecInit(M0_access_addr(i).map(_.U((log2Ceil((N/win) * 2)).W)))
        V
      }
      val M0_vec_port = for (i <- 0 until N/win) yield {
        val V = VecInit(M0_access_port(i).map(_.U((log2Ceil((w) * 2)).W)))
        V
      }
      val vvf_addr = VecInit(M0_vec_addr) // used to write into the M0 memory block , there are N/win blocks with win elements each
      val vvf_port = VecInit(M0_vec_port)

      val Perm_Mod = Module(new Permute_Unit_Streaming(w, bw)).io // nothing here should change
      val M0_config = Module(new M0_Config_ROM(N, r, base_r, ptype, w)).io // this should not change
      val M1_config = Module(new M1_Config_ROM(N, r, base_r, ptype, w)).io
      val Perm_Config = Module(new Streaming_Permute_Config(N, r, base_r, ptype, w)).io

      val cnt2 = RegInit(0.U(log2Ceil(N / w).W)) // secondary counter
      val cnt = RegInit(0.U(log2Ceil(N / win).W)) // this counter is the primary  counter // just need slightly more time to input the values into M0
      when(io.in_en.asUInt.orR) {
        when(cnt2 === ((N / w) - 1).U && cnt === ((N / win) - 1).U) { // cnt must have reached the end for the counters to start over
          cnt2 := 0.U
          cnt := 0.U
          offset_switch := ~offset_switch
        }.elsewhen(cnt2 === ((N / w) - 1).U) { // if cnt has not reached the end but cnt2 is at the end
          cnt2 := cnt2 // cnt2 stops momentarily to prevent any changes
          cnt := cnt + 1.U
          offset_switch := offset_switch
        }.otherwise { // if none of the counters have counted to the end
          cnt2 := cnt2 + 1.U
          cnt := cnt + 1.U
          offset_switch := offset_switch
        }
        for (i <- 0 until w) {
          M0(i).en := true.B // enable the memory blocks
          M0(i).re := true.B
          //M0(i).wr := true.B
          M1(i).en := true.B
          M1(i).re := true.B
          M1(i).wr := true.B
          M0(i).in_raddr := M0_config.out(i) + (offset * ~offset_switch) // read w elements from the M0 block each cc to go into the perm modules
          //M0(i).in_waddr := cnt + (offset * offset_switch) //select the address to write into the (this will need to change)
          //M0(i).in_data := io.in(i) // place the data to write into M0
          M1(i).in_raddr := cnt2 + (offset * ~offset_switch)
          M1(i).in_waddr := M1_config.out(i) + (offset * offset_switch)
          M1(i).in_data := Perm_Mod.out(i)
          Perm_Mod.in_config(i) := Perm_Config.out(i)
          Perm_Mod.in(i) := M0(i).out_data // still outputs data at N/w
          Perm_Mod.in_en := true.B
          io.out(i) := M1(i).out_data
        }
        for (i <- 0 until w) {
          M0(i).wr := false.B
          M0(i).in_waddr := 0.U
          M0(i).in_data := 0.U.asTypeOf(new ComplexNum(bw))
          io.out_t(i) := 8.U
        }
        for (i <- 0 until w) {
          for (j <- 0 until win) {
            when(vvf_port(cnt)(j) === i.U) {
              M0(i).wr := true.B
              M0(i).in_waddr := vvf_addr(cnt)(j) + (offset * offset_switch)
              M0(i).in_data := io.in(j)
              io.out_t(i) := (j).U
            }
          }
        }
      }.otherwise {
        for (i <- 0 until w) {
          M0(i).en := false.B
          M0(i).re := false.B
          M0(i).wr := false.B
          M1(i).en := false.B
          M1(i).re := false.B
          M1(i).wr := false.B
          M0(i).in_raddr := 0.U
          M0(i).in_waddr := 0.U
          M0(i).in_data := 0.U.asTypeOf(new ComplexNum(bw))
          M1(i).in_raddr := 0.U
          M1(i).in_waddr := 0.U
          M1(i).in_data := Perm_Mod.out(i)
          Perm_Mod.in_config(i) := Perm_Config.out(i)
          Perm_Mod.in(i) := M0(i).out_data
          Perm_Mod.in_en := false.B
          io.out(i) := M1(i).out_data
          io.out_t(i) := cnt2
        }
        offset_switch := 0.U
        cnt := cnt // no change
        cnt2 := cnt2
      }
      M0_config.in_cnt := cnt2 // still depends on cnt2, it is just the input that is affected by cnt
      M1_config.in_cnt := cnt2
      Perm_Config.in_cnt := cnt2
    }else{ // otherwise the win is greater than the w
      val input_delay_cycles = N/w - N/win // win is greater than w
      val input_delay_registers = RegInit(VecInit.fill(input_delay_cycles)(VecInit.fill(win)(0.U.asTypeOf(new ComplexNum(bw))))) // delaying the inputs to line up the ccs with N/w
      val counter_delay_registers = RegInit(VecInit.fill(input_delay_cycles)(false.B))
      val M0 = VecInit(for(i <- 0 until w) yield{ //has w memory ports, each with N/w * 2 addresses
        val M0_t = Module(new RAM_Block_MultiWrite(N,w,(win.toDouble/w.toDouble).ceil.toInt,bw)).io
        M0_t
      })
      val M1 = VecInit(for(i <- 0 until w) yield{ // same as M1 //lets make it a vec
        val M1_t = Module(new RAM_Block(N,w,bw)).io // this should be valid since all the parameters are the same
        M1_t
      })
      val modded_list4 = for(i <- 0 until N/w) yield{
        for(j <- 0 until w) yield{
          //        (j,listlol2(i*9+ j))
          (i,j)
        }
      }
      val modded_list5 = modded_list4.flatten
      val modded_list6 = for(i <- 0 until N/win) yield{
        for(j <- 0 until win) yield{
          modded_list5(i*(win)+ j)
        }
      }
      val mem_enables = mutable.ArrayBuffer[Array[Array[Boolean]]]()
      val mem_addr_adj = mutable.ArrayBuffer[Array[Array[Int]]]()
      val mem_input_pass = mutable.ArrayBuffer[Array[Array[Int]]]()
      for(i <- 0 until w){
        val temp = mutable.ArrayBuffer[Array[Boolean]]()
        val temp_ports1 = mutable.ArrayBuffer[Array[Int]]()
        val temp_addr1 = mutable.ArrayBuffer[Array[Int]]()
        val temp_inp1 = mutable.ArrayBuffer[Array[Int]]()
        for(j  <- 0 until N/win){
          val temp2 = mutable.ArrayBuffer[Boolean]()
          val temp_ports2 = mutable.ArrayBuffer[Int]()
          val temp_addr2 = mutable.ArrayBuffer[Int]()
          val temp_inp2 = mutable.ArrayBuffer[Int]()
          for(k <- 0 until win){
            if(modded_list6(j)(k)._2 == i){
              temp2 += true
              temp_ports2 += modded_list6(j)(k)._2
              temp_addr2 += modded_list6(j)(k)._1
              temp_inp2 += k
            }
          }
          for(k <- temp2.length until (win.toDouble/w.toDouble).ceil.toInt){
            temp2 += false
            temp_ports2 += 0
            temp_addr2 += 0
            temp_inp2 += 0
          }
          temp += temp2.toArray
          temp_ports1 += temp_ports2.toArray
          temp_addr1 += temp_addr2.toArray
          temp_inp1 += temp_inp2.toArray
        }
        mem_enables += temp.toArray
        mem_addr_adj += temp_addr1.toArray
        mem_input_pass += temp_inp1.toArray
      }

      modded_list4.map(x=>println(x.toList))
      println()
      modded_list6.map(x=>println(x.toList))
      println("_--_--_-_--_-_---__-----__--___--__-")
      for(i <- 0 until w) {
        println()
        mem_enables(i).map(x => println(x.toList))
      }

      val M0_mem_wen = VecInit(mem_enables.map(y=>VecInit(y.map(z=>VecInit(z.map(_.B))))).toArray) // this table contains all the write enables for each port
//      val M0_ports = VecInit(mem_ports_adj.map(y=>VecInit(y.map(z=>VecInit(z.map(_.B))))).toArray)
      val M0_addr = VecInit(mem_addr_adj.map(y=>VecInit(y.map(z=>VecInit(z.map(_.U(log2Ceil(N/w).W)))))).toArray)
      val M0_inp_order = VecInit(mem_input_pass.map(y=>VecInit(y.map(z=>VecInit(z.map(_.U(log2Ceil(win).W)))))).toArray)

      val Perm_Mod = Module(new Permute_Unit_Streaming(w, bw)).io // nothing here should change
      val M0_config = Module(new M0_Config_ROM(N, r, base_r, ptype, w)).io // this should not change
      val M1_config = Module(new M1_Config_ROM(N, r, base_r, ptype, w)).io
      val Perm_Config = Module(new Streaming_Permute_Config(N, r, base_r, ptype, w)).io

      val cnt2 = RegInit(0.U(log2Ceil(N / w).W)) // secondary counter
      val cnt = RegInit(0.U(log2Ceil(N / win).W)) // this counter is the primary  counter // just need slightly more time to input the values into M0 // since the win is now larger than the w, we will need to delay the counter some cycles before it begins to count
      when(io.in_en.asUInt.orR) {
        for(i <- 0 until input_delay_cycles){
          if(i == 0){
            counter_delay_registers(0) := true.B
          }else{
            counter_delay_registers(i) := counter_delay_registers(i-1)
          }
        }
        when(cnt2 === ((N / w) - 1).U && cnt === ((N / win) - 1).U) { // cnt must have reached the end for the counters to start over // need to be a little careful here since the primary counter will reach the end of the counter stages sooner than the descondary
          cnt2 := 0.U
          cnt := 0.U
          offset_switch := ~offset_switch
        }.elsewhen(cnt === ((N / win) - 1).U) { // if cnt has not reached the end but cnt2 is at the end
          cnt := cnt // cnt2 stops momentarily to prevent any changes
          cnt2 := cnt2 + 1.U
          offset_switch := offset_switch
        }.otherwise { // if none of the counters have counted to the end
          cnt2 := cnt2 + 1.U
          when(cnt2 >= delay.U){
            cnt := cnt + 1.U
          }.otherwise{
            cnt := 0.U
          }
//          when(counter_delay_registers(input_delay_cycles-1)){ // don't start counting immediately
//            cnt := cnt + 1.U
//          }.otherwise{
//            cnt := cnt
//          }
          offset_switch := offset_switch
        }
        for (i <- 0 until w) {
          M0(i).en := true.B // enable the memory blocks
          M0(i).re := true.B
          M1(i).en := true.B
          M1(i).re := true.B
          M1(i).wr := true.B
          M0(i).in_raddr := M0_config.out(i) + (offset * ~offset_switch) // read w elements from the M0 block each cc to go into the perm modules
          M1(i).in_raddr := cnt2 + (offset * ~offset_switch)
          M1(i).in_waddr := M1_config.out(i) + (offset * offset_switch)
          M1(i).in_data := Perm_Mod.out(i)
          Perm_Mod.in_config(i) := Perm_Config.out(i)
          Perm_Mod.in(i) := M0(i).out_data // still outputs data at N/w
          Perm_Mod.in_en := true.B
          io.out(i) := M1(i).out_data
        }
//        for (i <- 0 until w) {
//          for(j <- 0 until (win.toDouble/w.toDouble).ceil.toInt) {
//            M0(i).wr(j) := false.B
//            M0(i).in_waddr(j) := 0.U
//            M0(i).in_data(j) := 0.U.asTypeOf(new ComplexNum(bw))
//          }
//          io.out_t(i) := 8.U
//        }
        for(i <- 0 until input_delay_cycles){
          if(i == 0){
            input_delay_registers(0) := io.in
          }else{
            input_delay_registers(i) := input_delay_registers(i-1)
          }
        }
        for(i <- 0 until w){
//          for(j <-0 until N/win){
            for(k <- 0 until (win.toDouble/w.toDouble).ceil.toInt){
              M0(i).wr(k) := M0_mem_wen(i)(cnt)(k) //this provides the enables each clock cycle
              M0(i).in_waddr(k) := M0_addr(i)(cnt)(k) + (offset * offset_switch)
              M0(i).in_data(k) := input_delay_registers(input_delay_cycles-1)(M0_inp_order(i)(cnt)(k))
            }
          io.out_t(i) := 0.U
        }

//        for (i <- 0 until w) {
//          for (j <- 0 until win) {
//            when(vvf_port(cnt)(j) === i.U) {
//              for(k <- 0 until (win.toDouble/w.toDouble).ceil.toInt){
//                M0(i).in_waddr(k) := vvf_addr(cnt)(j) + (offset * offset_switch)
//              }
//              M0(i).in_data := io.in(j)
//              io.out_t(i) := (j).U
//            }
//          }
//        }
      }.otherwise {
        input_delay_registers := VecInit.fill(input_delay_cycles)(VecInit.fill(win)(0.U.asTypeOf(new ComplexNum(bw))))
        counter_delay_registers := VecInit.fill(input_delay_cycles)(false.B)
        for (i <- 0 until w) {
          M0(i).en := false.B
          M0(i).re := false.B
          M0(i).wr := VecInit.fill((win.toDouble/w.toDouble).ceil.toInt)(false.B)
          M1(i).en := false.B
          M1(i).re := false.B
          M1(i).wr := false.B
          M0(i).in_raddr := 0.U
          M0(i).in_waddr := VecInit.fill((win.toDouble/w.toDouble).ceil.toInt)(0.U)
          M0(i).in_data := VecInit.fill((win.toDouble/w.toDouble).ceil.toInt)(0.U.asTypeOf(new ComplexNum(bw)))
          M1(i).in_raddr := 0.U
          M1(i).in_waddr := 0.U
          M1(i).in_data := Perm_Mod.out(i)
          Perm_Mod.in_config(i) := Perm_Config.out(i)
          Perm_Mod.in(i) := M0(i).out_data
          Perm_Mod.in_en := false.B
          io.out(i) := M1(i).out_data
          io.out_t(i) := cnt2
        }
        offset_switch := 0.U
        cnt := 0.U // no change
        cnt2 := 0.U
      }
      M0_config.in_cnt := cnt2 // still depends on cnt2, it is just the input that is affected by cnt
      M1_config.in_cnt := cnt2
      Perm_Config.in_cnt := cnt2
    }
  }

  class PermutationsWithStreaming_and_iterative(N:Int, r: Int, base_r: Int, w:Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_en = Input(Vec((N/w)*2 + 1, Bool()))
      val in_perm = Input(Vec(w, UInt(log2Ceil(N).W)))
      val in_M0_config = Input(Vec(w, UInt(log2Ceil(N).W)))
      val in_M1_config = Input(Vec(w, UInt(log2Ceil(N).W)))
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_cnt = Output(UInt(log2Ceil(N/w).W))
      val out_t = Output(UInt(log2Ceil(N/w).W))
    })
    val is_enabled = Reg(Bool())
    val offset = (N/w).U
    val offset_switch = Reg(UInt(1.W))
    val M0 = (for(i <- 0 until w) yield{
      val M0_t = Module(new RAM_Block(N,w,bw)).io
      M0_t
    }).toVector
    val M1 = (for(i <- 0 until w) yield{
      val M1_t = Module(new RAM_Block(N,w,bw)).io
      M1_t
    }).toVector
    val Perm_Mod = Module(new Permute_Unit_Streaming(w, bw)).io
    val cnt = RegInit(0.U(log2Ceil(N/w).W))
    when(io.in_en.asUInt.orR){
      when(cnt === ((N/w)-1).U){
        cnt := 0.U
        offset_switch := ~offset_switch
      }.otherwise{
        cnt := cnt + 1.U
        offset_switch := offset_switch
      }
      for(i <- 0 until w){
        M0(i).en := true.B
        M0(i).re := true.B
        M0(i).wr := true.B
        M1(i).en := true.B
        M1(i).re := true.B
        M1(i).wr := true.B
        M0(i).in_raddr := io.in_M0_config(i) + (offset * ~offset_switch)
        M0(i).in_waddr := cnt + (offset * offset_switch)
        M0(i).in_data := io.in(i)
        M1(i).in_raddr := cnt + (offset * ~offset_switch)
        M1(i).in_waddr := io.in_M1_config(i) + (offset * offset_switch)
        M1(i).in_data := Perm_Mod.out(i)
        Perm_Mod.in_config(i) := io.in_perm(i)
        Perm_Mod.in(i) := M0(i).out_data
        Perm_Mod.in_en := true.B
        io.out(i) := M1(i).out_data
      }
    }.otherwise{
      for(i <- 0 until w){
        M0(i).en := false.B
        M0(i).re := false.B
        M0(i).wr := false.B
        M1(i).en := false.B
        M1(i).re := false.B
        M1(i).wr := false.B
        M0(i).in_raddr := 0.U
        M0(i).in_waddr := 0.U
        M0(i).in_data := io.in(i)
        M1(i).in_raddr := 0.U
        M1(i).in_waddr := 0.U
        M1(i).in_data := Perm_Mod.out(i)
        Perm_Mod.in_config(i) := io.in_perm(i)
        Perm_Mod.in(i) := M0(i).out_data
        Perm_Mod.in_en := false.B
        io.out(i) := M1(i).out_data
      }
      offset_switch := 0.U
      cnt := 0.U
    }
    io.out_cnt := cnt
    io.out_t := cnt
  }

  class Permute_Unit_Streaming(w: Int, bw: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_config = Input(Vec(w, UInt(log2Ceil(w).W)))
      val in_en = Input(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val hotvals = for(i <- 0 until w)yield{
      for(j <- 0 until w)yield{
        val hv = (io.in_config(j) === i.U, j.U)
        hv
      }
    }
    val pms = for(i <- 0 until w) yield{
      val pmx = PriorityMux(hotvals(i))
      pmx
    }
    for(i <- 0 until w){
      io.out(i) := io.in(pms(i))
    }
  }

  class M0_Config_ROM(N: Int, r: Int, base_r: Int, ptype: Int, w: Int) extends Module{
    val io = IO(new Bundle() {
      val in_cnt = Input(UInt((log2Ceil(N/w).W)))
      val out = Output(Vec(w, UInt((log2Ceil((N/w) * 2)).W)))
    })
    val cnt = RegInit(0.U(log2Ceil(N/w).W))
    var generated_address = Permutations.GenerateStreamingMappingV2(N,w,r,base_r, ptype)
    generated_address = Permutations.Mapping_Sort(generated_address,0)
    val M0_access = (for(i <- 0 until N/w)yield{
      val M0_slice = (for(j <- 0 until w)yield{
        generated_address(i)(j)._1._1
      }).toArray
      M0_slice
    }).toArray
    M0_access.map(x=> println(x.toList))
    val M0_vec = for(i <- 0 until N/w) yield{
      val V = VecInit(M0_access(i).map(_.U((log2Ceil((N/w) * 2)).W)))
      V
    }
    val vvf = VecInit(M0_vec)
    for(i <- 0 until w){
      io.out(i) := vvf(io.in_cnt)(i) //+ offset.U((log2Ceil((N/w) * 2)).W)
    }
  }

  class M1_Config_ROM(N: Int, r: Int, base_r: Int, ptype: Int, w: Int) extends Module{
    val io = IO(new Bundle() {
      val in_cnt = Input(UInt((log2Ceil(N/w).W)))
      val out = Output(Vec(w, UInt((log2Ceil((N/w) * 2)).W)))
    })
    var generated_address = Permutations.GenerateStreamingMappingV2(N,w,r,base_r, ptype)
    generated_address = Permutations.Mapping_Sort(generated_address,1)
    val M1_access = (for(i <- 0 until N/w)yield{
      val M1_slice = (for(j <- 0 until w)yield{
        generated_address(i)(j)._2._1
      }).toArray
      M1_slice
    }).toArray
    M1_access.map(x=> println(x.toList))
    val M1_vec = for(i <- 0 until N/w) yield{
      val V = VecInit(M1_access(i).map(_.U((log2Ceil((N/w) * 2)).W)))
      V
    }
    val vvf = VecInit(M1_vec)
    for(i <- 0 until w){
      io.out(i) := vvf(io.in_cnt)(i) //+ offset.U((log2Ceil((N/w) * 2)).W)
    }
  }

  class Streaming_Permute_Config(N: Int, r: Int, base_r: Int, ptype: Int, w: Int)extends Module{
    val io = IO(new Bundle() {
      val in_cnt = Input(UInt((log2Ceil(N/w).W)))
      val out = Output(Vec(w, UInt((log2Ceil(w)).W)))
    })
    var generated_address = Permutations.GenerateStreamingMappingV2(N,w,r,base_r, ptype)
    generated_address = Permutations.Mapping_Sort(generated_address,0)
    val M1_access = (for(i <- 0 until N/w)yield{
      val M1_slice = (for(j <- 0 until w)yield{
        generated_address(i)(j)._2._2
      }).toArray
      M1_slice
    }).toArray
    M1_access.map(x=> println(x.toList))
    val M1_vec = for(i <- 0 until N/w) yield{
      val V = VecInit(M1_access(i).map(_.U((log2Ceil(w).W))))
      V
    }
    val vvf = VecInit(M1_vec)
    for(i <- 0 until w){
      io.out(i) := vvf(io.in_cnt)(i)
    }
  }

  class RAM_Block(N: Int, w: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in_raddr = Input(UInt((log2Ceil(2 * (N/w))).W))
      val in_waddr = Input(UInt((log2Ceil(2 * (N/w))).W))
      val in_data = Input(new ComplexNum(bw))
      val re = Input(Bool())
      val wr = Input(Bool())
      val en = Input(Bool())
      val out_data = Output(new ComplexNum(bw))
    })
    val mem = Reg(Vec((N/w)*2, new ComplexNum(bw)))
    when(io.en){
      when(io.wr){
        mem(io.in_waddr) := io.in_data
      }
      when(io.re){
        io.out_data := mem(io.in_raddr)
      }.otherwise{
        io.out_data.Re := 0.U
        io.out_data.Im := 0.U
      }
    }.otherwise{
      io.out_data.Re := 0.U
      io.out_data.Im := 0.U
    }
  }

  class RAM_Block_MultiWrite(N: Int, w: Int, size:Int, bw: Int) extends Module{ //application specific
    val io = IO(new Bundle() {
      val in_raddr = Input(UInt((log2Ceil(2 * (N/w))).W))
      val in_waddr = Input(Vec(size ,UInt((log2Ceil(2 * (N/w))).W)))
      val in_data = Input(Vec(size ,new ComplexNum(bw)))
      val re = Input(Bool())
      val wr = Vec(size ,Input(Bool())) // enable for writes
      val en = Input(Bool())
      val out_data = Output(new ComplexNum(bw))
    })
    val mem = Reg(Vec((N/w)*2, new ComplexNum(bw)))
    when(io.en){
      for(i <- 0 until size){
        when(io.wr(i)){
          mem(io.in_waddr(i)) := io.in_data(i)
        }
      }
      when(io.re){
        io.out_data := mem(io.in_raddr)
      }.otherwise{
        io.out_data.Re := 0.U
        io.out_data.Im := 0.U
      }
    }.otherwise{
      io.out_data.Re := 0.U
      io.out_data.Im := 0.U
    }
  }

  class FFT_SingleRadix(N:Int, r: Int, w: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(N, new ComplexNum(bw)))
    })
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
    if(r==2){
      DFT_latency = 1
    }
    println(s"the dft latency: ${DFT_latency}")
    val DFTs_per_stage = N/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    var T_L = 0
    for(i <- 0 until TotalStages) {
      val twid = Permutations.T2(N, r)(i)
      var mult_count2 = 0
      for (i <- 0 until w){
        val n = twid(i)
        val c1 = FFT.isReducable(n.re.abs)
        val c2 = FFT.isReducable(n.im.abs)
        if (!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
          mult_count2 += 1
        }
      }
      var T_latency = CMultLatency
      if (mult_count2 == 0) {
        T_latency = 0
      }
      T_L += T_latency
    }
    val Twid_latency = (N/w) * CMultLatency
    val Perm_latency = 0
    val Total_Latency = T_L + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency + 1
    val regDelays = RegInit(VecInit.fill(Total_Latency)(false.B))
    //val regDelays = Reg(Vec(Total_Latency, Bool()))
    for(i <- 0 until Total_Latency){
      if(i == 0){
        regDelays(0) := io.in_ready
      }else{
        regDelays(i) := regDelays(i-1)
      }
    }
    val valid_signal = Wire(Bool())
    valid_signal := regDelays(Total_Latency-1)
    io.out_validate := valid_signal
    println(s"Total Latency: ${(Total_Latency, DFT_latency, Twid_latency, Perm_latency)}")
    val DFT_instances = (for(i <- 0 until number_of_stages) yield{
      val DFT_instnace_row = (for(j <- 0 until DFTs_per_stage) yield{
        val DFT_instance = Module(new DFT_Symmetric_NRV(r, bw)).io
        DFT_instance
      }).toVector
      DFT_instnace_row
    }).toVector
    val Input_Permutation = Module(new PermutationsSimple(N,r,1,bw)).io
    val Stage_Permutations = (for(i <- 0 until number_of_stages)yield{
      val stage_permutation = Module(new PermutationsSimple(N,r,0,bw)).io
      stage_permutation
    }).toVector
    val TwiddleFactorModules = (for(i <- 0 until number_of_stages - 1) yield {
      val Twid = Module(new TwiddleFactors(N,r,N,i,bw)).io
      Twid
    }).toVector
    when(io.in_ready){
      for(i <- 0 until N){
        Input_Permutation.in(i) := io.in(i)
      }
    }.otherwise{
      for(i <- 0 until N){
        Input_Permutation.in(i).Re := 0.U
        Input_Permutation.in(i).Im := 0.U
      }
    }
    val results = RegInit(VecInit.fill(N)(0.U.asTypeOf(new ComplexNum(bw))))
    //val results = Reg(Vec(N, new ComplexNum(bw)))
    for(i <- 0 until number_of_stages){
      for(j <- 0 until DFTs_per_stage){
        for(k <- 0 until r){
          if(i == 0){
            DFT_instances(i)(j).in(k) := Input_Permutation.out(j*r+k)
          }else{
            DFT_instances(i)(j).in(k) := TwiddleFactorModules(i-1).out(j*r+k)
          }
          Stage_Permutations(i).in(j*r+k) := DFT_instances(i)(j).out(k)
        }
      }
      if(i != 0){
        TwiddleFactorModules(i-1).in := Stage_Permutations(i-1).out
      }
      if(i == number_of_stages - 1) {
        when(regDelays(Total_Latency-2)) {
          results := Stage_Permutations(i).out
        }.otherwise {
          for (j <- 0 until N) {
            results(j).Re := 0.U
            results(j).Im := 0.U
          }
        }
      }
    }
    io.out := results
  }

  class FFT_SingleRadix_NRV(N:Int, r: Int, w: Int, bw: Int) extends Module{ //FFT no ready and validate input/output
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val out = Output(Vec(N, new ComplexNum(bw)))
    })
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
    if(r == 2){
      DFT_latency = 1
    }
    println(s"the dft latency: ${DFT_latency}")
    val DFTs_per_stage = N/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    var T_L = 0
    for(i <- 0 until TotalStages) {
      val twid = Permutations.T2(N, r)(i)
      var mult_count2 = 0
      for (i <- 0 until w){
        val n = twid(i)
        val c1 = FFT.isReducable(n.re.abs)
        val c2 = FFT.isReducable(n.im.abs)
        if (!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))) {
          mult_count2 += 1
        }
      }
      var T_latency = CMultLatency
      if (mult_count2 == 0) {
        T_latency = 0
      }
      T_L += T_latency
    }
    val Twid_latency = (N/w) * CMultLatency
    val Perm_latency = 0
    val Total_Latency = T_L + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency + 1
    println(s"Total Latency: ${(Total_Latency, DFT_latency, Twid_latency, Perm_latency)}")
    val DFT_instances = (for(i <- 0 until number_of_stages) yield{
      val DFT_instnace_row = (for(j <- 0 until DFTs_per_stage) yield{
        val DFT_instance = Module(new DFT_Symmetric_NRV(r, bw)).io
        DFT_instance
      }).toVector
      DFT_instnace_row
    }).toVector
    val Input_Permutation = Module(new PermutationsSimple(N,r,1,bw)).io
    val Stage_Permutations = (for(i <- 0 until number_of_stages)yield{
      val stage_permutation = Module(new PermutationsSimple(N,r,0,bw)).io
      stage_permutation
    }).toVector
    val TwiddleFactorModules = (for(i <- 0 until number_of_stages - 1) yield {
      val Twid = Module(new TwiddleFactors(N,r,N,i,bw)).io
      Twid
    }).toVector
    for(i <- 0 until N){
      Input_Permutation.in(i) := io.in(i)
    }
    val results = WireInit(VecInit.fill(N)(0.U.asTypeOf(new ComplexNum(bw))))
    //val results = Reg(Vec(N, new ComplexNum(bw)))
    for(i <- 0 until number_of_stages){
      for(j <- 0 until DFTs_per_stage){
        for(k <- 0 until r){
          if(i == 0){
            DFT_instances(i)(j).in(k) := Input_Permutation.out(j*r+k)
          }else{
            DFT_instances(i)(j).in(k) := TwiddleFactorModules(i-1).out(j*r+k)
          }
          Stage_Permutations(i).in(j*r+k) := DFT_instances(i)(j).out(k)
        }
      }
      if(i != 0){
        TwiddleFactorModules(i-1).in := Stage_Permutations(i-1).out
      }
      if(i == number_of_stages - 1) {
        results := Stage_Permutations(i).out
      }
    }
    io.out := results
  }

  class FFT_MixedRadix(N: Int, nr: Int, ns: Int, r: Int, s: Int, w: Int, bw: Int) extends Module{ // mixed radix fft, no ready and validate input/output
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(N, new ComplexNum(bw)))
      val out_t = Output(Vec(N, new ComplexNum(bw)))
    })
    val FFT_latency1 = getFFTLatency(ns,s,ns,bw)
    val FFT_latency2 = getFFTLatency(nr,r,nr,bw)
    val DFTr_Constants = Permutations.T2_rs(N,ns,nr)
    var mult_count = 0
    val mult_ind = mutable.ArrayBuffer[Int]()
    val mult_needs = for(i <- 0 until w) yield{
      val n  = DFTr_Constants(i)
      val c1 = FFT.isReducable(n.re.abs)
      val c2 = FFT.isReducable(n.im.abs)
      if(!((c1._1 && n.im.abs < 0.005) || (c2._1 && n.re.abs < 0.005))){
        mult_count += 1
        mult_ind += (i)
        (i, 0, false, false, true)
      }else{
        mult_ind += (i)
        if((c1._1 && n.im.abs < 0.005)){
          (i, c1._2.abs, n.re < -0.0005, false, false)
        }else{
          (i, c2._2.abs, n.im < -0.0005, true, false)
        }
      }
    }
    val CMultLatency = 2
    var T_latency = CMultLatency
    if(mult_count == 0){
      T_latency = 0
    }
    val total_latency = FFT_latency1 + FFT_latency2 + T_latency
    val regdelays = RegInit(VecInit.fill(total_latency)(false.B))
    for(i <- 0 until total_latency){
      if(i == 0){
        regdelays(0) := io.in_ready
      }else{
        regdelays(i) := regdelays(i-1)
      }
    }
    val out_regdelay = RegInit(false.B)

    val FFT_s_modules = for(i <- 0 until nr) yield {
      val instance = Module(new FFT_SingleRadix_NRV(ns,s,ns,bw)).io
      instance
    }
    val FFT_r_modules = for(i <- 0 until ns) yield {
      val instance = Module(new FFT_SingleRadix_NRV(nr,r,nr,bw)).io
      instance
    }
    val permutation_modules = for(i <- 0 until 3) yield{
      if(i == 1){
        val p_mod = Module(new PermutationsSimple(N,ns,0,bw)).io
        p_mod
      }else{
        val p_mod = Module(new PermutationsSimple(N, nr, 0, bw)).io
        p_mod
      }
    }
    val twid_factor = Module(new TwiddleFactors_mr(N,nr,ns,w,bw)).io
    when(io.in_ready){
      permutation_modules(0).in := io.in
    }.otherwise{
      for(i <- 0  until w){
        permutation_modules(0).in(i) := 0.U.asTypeOf(new ComplexNum(bw))
      }
    }
    twid_factor.in := permutation_modules(1).out
    val out_results = RegInit(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw))))
    when(regdelays(total_latency-1)){
      out_results := permutation_modules(2).out
      out_regdelay := regdelays(total_latency-1)
    }.otherwise{
      for(i <- 0 until w){
        out_results(i) := 0.U.asTypeOf(new ComplexNum(bw))
      }
      out_regdelay := false.B
    }
    io.out_validate := out_regdelay
    io.out := out_results
    for(i <- 0 until nr){
      for(j <- 0 until ns){
        FFT_s_modules(i).in(j) := permutation_modules(0).out(i*ns + j)
        permutation_modules(1).in(i*ns + j) := FFT_s_modules(i).out(j)
        FFT_r_modules(j).in(i) := twid_factor.out(j*nr + i)
        permutation_modules(2).in(j*nr + i) := FFT_r_modules(j).out(i)
      }
    }
    for(i <- 0 until nr){
      for(j <- 0 until ns){
        io.out_t(i*ns + j) := FFT_s_modules(i).out(j)
      }
    }
  }

  class FFT_SingleRadix_Streaming(N:Int, r: Int, w: Int, bw: Int) extends Module{ // streaming single radix fft, still in progress
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_test = Output(Vec(w, new ComplexNum(bw)))
    })
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
    if(r == 2){
      DFT_latency = 1
    }
    println(s"the dft latency: ${DFT_latency}")
    val DFTs_per_stage = w/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    var T_L = CMultLatency
    val Twid_latency = TotalStages * T_L
    val Perm_latency = (N/w)*2
    val Total_Latency = Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency + 1
    println(s"The total latency is: ${Total_Latency}")
    println(s"${(T_L, DFT_latency, Perm_latency)}")
    val DFT_regdelays = RegInit(VecInit.fill(number_of_stages)(VecInit.fill(DFT_latency)(false.B)))
    val Twid_regdelays = RegInit(VecInit.fill(number_of_stages-1)(VecInit.fill(T_L)(false.B)))
    val Perm_regdelays = RegInit(VecInit.fill(number_of_stages+1)(VecInit.fill(Perm_latency)(false.B)))
    val out_regdelay = RegInit(false.B)
    val DFT_modules = for(i <- 0 until number_of_stages)yield{
      val row = for(j <- 0 until w/r)yield{
        val instance = Module(new DFT_Symmetric_NRV(r,bw)).io
        instance
      }
      row
    }
    val Perm_modules = for(i <- 0 until number_of_stages + 1)yield{
      if(i == 0){
        val instance = Module(new PermutationsWithStreaming(N,r,r,w,1,bw)).io // bit reversal
        instance
      }else{
        val instance = Module(new PermutationsWithStreaming(N,r,r,w,0,bw)).io //stride permute
        instance
      }
    }
    val Twid_Modules = for(i <- 0 until number_of_stages - 1) yield{
      val instance = Module(new TwiddleFactorsStreamed(N,r,w,i,bw)).io
      instance
    }
    for(i <- 0 until number_of_stages + 1){
      for(j <- 0 until Perm_latency){
        if(j == 0){
          if(i == 0){
            Perm_regdelays(0)(0) := io.in_ready
            Perm_modules(0).in_en(0) := io.in_ready
            when(io.in_ready){
              Perm_modules(0).in := io.in
            }.otherwise{
              for(l <- 0 until w){
                Perm_modules(0).in(l) := 0.U.asTypeOf(new ComplexNum(bw))
              }
            }
          }else{
            Perm_regdelays(i)(0) := DFT_regdelays(i-1)(DFT_latency-1)
            Perm_modules(i).in_en(0) := DFT_regdelays(i-1)(DFT_latency-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                Perm_modules(i).in(k*r + l) := DFT_modules(i-1)(k).out(l)
              }
            }
          }
        }else{
          Perm_regdelays(i)(j) := Perm_regdelays(i)(j-1)
          Perm_modules(i).in_en(j) := Perm_regdelays(i)(j-1)
          if(j == Perm_latency-1){
            Perm_modules(i).in_en((N/w)*2) := Perm_regdelays(i)(Perm_latency-1)
          }
        }
      }
    }
    for(i <- 0 until number_of_stages){
      for(j <- 0 until DFT_latency){
        if(j == 0){
          if(i == 0){
            DFT_regdelays(0)(0) := Perm_regdelays(0)(Perm_latency-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                DFT_modules(0)(k).in(l) := Perm_modules(0).out(k*r+l)
              }
            }
          }else{
            DFT_regdelays(i)(0) := Twid_regdelays(i-1)(T_L-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                DFT_modules(i)(k).in(l) := Twid_Modules(i-1).out(k*r+l)
              }
            }
          }
        }else{
          DFT_regdelays(i)(j) := DFT_regdelays(i)(j-1)
        }
      }
    }
    for(i <- 0 until number_of_stages - 1){
      for(j <- 0 until T_L){
        if(j == 0){
          if(i == 0){
            Twid_regdelays(0)(0) := Perm_regdelays(1)(Perm_latency-1)
            Twid_Modules(0).in_en(0) := Perm_regdelays(1)(Perm_latency-1)
            Twid_Modules(0).in := Perm_modules(1).out
          }else{
            Twid_regdelays(i)(0) := Perm_regdelays(i+1)(Perm_latency-1)
            Twid_Modules(i).in_en(0) := Perm_regdelays(i+1)(Perm_latency-1)
            Twid_Modules(i).in := Perm_modules(i+1).out
          }
        }else{
          Twid_regdelays(i)(j) := Twid_regdelays(i)(j-1)
          Twid_Modules(i).in_en(j) := Twid_regdelays(i)(j-1)
        }
      }
    }
    val results = RegInit(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw))))
    when(Perm_regdelays(number_of_stages)(Perm_latency-1)){
      results := Perm_modules(number_of_stages).out
    }.otherwise{
      for(i <- 0 until w){
        results(i) := 0.U.asTypeOf(new ComplexNum(bw))
      }
    }
    out_regdelay := Perm_regdelays(number_of_stages)(Perm_latency-1)
    io.out_validate := out_regdelay
    io.out := results
    io.out_test := Perm_modules(0).out
  }

  class FFT_SingleRadix_Streaming_NRO(N:Int, r: Int, w: Int, bw: Int) extends Module{ // streaming single radix fft, still in progress
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
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
    if(r == 2){
      DFT_latency = 1
    }
    println(s"the dft latency: ${DFT_latency}")
    val DFTs_per_stage = w/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    var T_L = CMultLatency
    val Twid_latency = TotalStages * T_L
    val Perm_latency = (N/w)*2
    val Total_Latency = Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency + 1
    println(s"The total latency is: ${Total_Latency}")
    println(s"${(T_L, DFT_latency, Perm_latency)}")
    val DFT_regdelays = RegInit(VecInit.fill(number_of_stages)(VecInit.fill(DFT_latency)(false.B)))
    val Twid_regdelays = RegInit(VecInit.fill(number_of_stages-1)(VecInit.fill(T_L)(false.B)))
    val Perm_regdelays = RegInit(VecInit.fill(number_of_stages+1)(VecInit.fill(Perm_latency)(false.B)))
    //val out_regdelay = RegInit(false.B) // we don't need this now
    val DFT_modules = for(i <- 0 until number_of_stages)yield{
      val row = for(j <- 0 until w/r)yield{
        val instance = Module(new DFT_Symmetric_NRV(r,bw)).io
        instance
      }
      row
    }
    val Perm_modules = for(i <- 0 until number_of_stages + 1)yield{
      if(i == 0){
        val instance = Module(new PermutationsWithStreaming(N,r,r,w,1,bw)).io // bit reversal
        instance
      }else{
        val instance = Module(new PermutationsWithStreaming(N,r,r,w,0,bw)).io //stride permute
        instance
      }
    }
    val Twid_Modules = for(i <- 0 until number_of_stages - 1) yield{
      val instance = Module(new TwiddleFactorsStreamed(N,r,w,i,bw)).io
      instance
    }
    for(i <- 0 until number_of_stages + 1){
      for(j <- 0 until Perm_latency){
        if(j == 0){
          if(i == 0){
            Perm_regdelays(0)(0) := io.in_ready
            Perm_modules(0).in_en(0) := io.in_ready
            when(io.in_ready){
              Perm_modules(0).in := io.in
            }.otherwise{
              for(l <- 0 until w){
                Perm_modules(0).in(l) := 0.U.asTypeOf(new ComplexNum(bw))
              }
            }
          }else{
            Perm_regdelays(i)(0) := DFT_regdelays(i-1)(DFT_latency-1)
            Perm_modules(i).in_en(0) := DFT_regdelays(i-1)(DFT_latency-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                Perm_modules(i).in(k*r + l) := DFT_modules(i-1)(k).out(l)
              }
            }
          }
        }else{
          Perm_regdelays(i)(j) := Perm_regdelays(i)(j-1)
          Perm_modules(i).in_en(j) := Perm_regdelays(i)(j-1)
          if(j == Perm_latency-1){
            Perm_modules(i).in_en((N/w)*2) := Perm_regdelays(i)(Perm_latency-1)
          }
        }
      }
    }
    for(i <- 0 until number_of_stages){
      for(j <- 0 until DFT_latency){
        if(j == 0){
          if(i == 0){
            DFT_regdelays(0)(0) := Perm_regdelays(0)(Perm_latency-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                DFT_modules(0)(k).in(l) := Perm_modules(0).out(k*r+l)
              }
            }
          }else{
            DFT_regdelays(i)(0) := Twid_regdelays(i-1)(T_L-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                DFT_modules(i)(k).in(l) := Twid_Modules(i-1).out(k*r+l)
              }
            }
          }
        }else{
          DFT_regdelays(i)(j) := DFT_regdelays(i)(j-1)
        }
      }
    }
    for(i <- 0 until number_of_stages - 1){
      for(j <- 0 until T_L){
        if(j == 0){
          if(i == 0){
            Twid_regdelays(0)(0) := Perm_regdelays(1)(Perm_latency-1)
            Twid_Modules(0).in_en(0) := Perm_regdelays(1)(Perm_latency-1)
            Twid_Modules(0).in := Perm_modules(1).out
          }else{
            Twid_regdelays(i)(0) := Perm_regdelays(i+1)(Perm_latency-1)
            Twid_Modules(i).in_en(0) := Perm_regdelays(i+1)(Perm_latency-1)
            Twid_Modules(i).in := Perm_modules(i+1).out
          }
        }else{
          Twid_regdelays(i)(j) := Twid_regdelays(i)(j-1)
          Twid_Modules(i).in_en(j) := Twid_regdelays(i)(j-1)
        }
      }
    }
    //val results = RegInit(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw))))
//    when(Perm_regdelays(number_of_stages)(Perm_latency-1)){
//      io.out := Perm_modules(number_of_stages).out
//    }.otherwise{
//      for(i <- 0 until w){
//        io.out(i) := 0.U.asTypeOf(new ComplexNum(bw))
//      }
//    }
    //out_regdelay := Perm_regdelays(number_of_stages)(Perm_latency-1)
    io.out_validate := Perm_regdelays(number_of_stages)(Perm_latency-1) // the validate just outputs directly
    //io.out := results
    io.out := Perm_modules(number_of_stages).out // same case with the results
  }

  class FFT_SingleRadix_Streaming_and_Iterative(N:Int, r: Int, d:Int, w: Int, bw: Int) extends Module{//in progress
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_test = Output(Vec(w, new ComplexNum(bw)))
      val out_cnt = Output(UInt(bw.W))
    })
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
    if(r == 2){
      DFT_latency = 1
    }
    println(s"the dft latency: ${DFT_latency}")
    val DFTs_per_stage = w/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val number_of_stages_depth = d // ideally has to be a divisor of the number of stages
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
    var T_L = CMultLatency
    val Twid_latency = (TotalStages+1) * T_L
    val Perm_latency = (N/w)*2
    val Total_Latency = (d)*T_L + (d) * DFT_latency + (d) * Perm_latency + 1 // time for first output val, then wait (N/w) - 1 cycles
    val Latency_exclude_first = (d)*T_L + (d) * DFT_latency + (d) * Perm_latency + 1
    var overrall_latency = 0
    val increment_points = mutable.ArrayBuffer[Int]() // iteration cnt  - 1
    for(i <- 0 until (number_of_stages/number_of_stages_depth)){ // the total number of iterations (number of stages depth must be a divisor of number of stages)
      if(i == 0){
        increment_points += Latency_exclude_first-1 + (N/w)// must be incremented by this point
        overrall_latency += Latency_exclude_first-1 + (N/w) // some cycles to take into account the propagation
      }else{
        increment_points += overrall_latency + Latency_exclude_first - 1 + (N/w)
        overrall_latency += Latency_exclude_first - 1 + (N/w)
      }
    }
    println("increment point check")
    increment_points += overrall_latency + Perm_latency
    increment_points.map(x=>println(x))
    overrall_latency += Perm_latency
    overrall_latency += (N/w)
    println(s"The Overall Latency of Computation: ${overrall_latency}")

    println(s"The total latency is: ${Total_Latency}")
    println(s"${(T_L, DFT_latency, Perm_latency)}")
    val DFT_regdelays = RegInit(VecInit.fill(number_of_stages_depth)(VecInit.fill(DFT_latency)(false.B))) // DFT_NRV latancy
    val Twid_regdelays = RegInit(VecInit.fill(number_of_stages_depth)(VecInit.fill(T_L)(false.B)))
    val Perm_regdelays = RegInit(VecInit.fill(number_of_stages_depth)(VecInit.fill(Perm_latency)(false.B)))
    val final_regdelays = RegInit(VecInit.fill(N/w)(false.B))
    val out_regdelay = RegInit(false.B)
    val DFT_modules = for(i <- 0 until number_of_stages_depth)yield{
      val row = for(j <- 0 until w/r)yield{
        val instance = Module(new DFT_Symmetric_NRV(r,bw)).io
        instance
      }
      row
    }
    val Perm_modules = for(i <- 0 until number_of_stages_depth)yield{
      val instance = Module(new PermutationsWithStreaming_and_iterative(N,r,r,w,bw)).io //stride permute
      instance
    }
    val Twid_Modules = for(i <- 0 until number_of_stages_depth) yield{
      val instance = Module(new TwiddleFactorsStreamed_and_Iterative(N,r,w,bw)).io
      instance
    }
    //----------------------------------------------------------------------------------------------------------------
    val list_of_twids = Permutations.T2(N,r)
    val initial_twid = for(i <- 0 until N) yield{
      cmplx(1,0)
    }
    val adjusted_rom_array = for(i <- 0 until (Math.log10(N)/Math.log10(r)).round.toInt) yield{
      if(i == 0){
        initial_twid.toArray
      }else{
        list_of_twids(i-1)
      }
    }
    val twid_sets = for(i <- 0 until number_of_stages) yield {
      val Twid_Constants = adjusted_rom_array(i)
      val Twid_map_2D = for (i <- 0 until N / w) yield {
        val tm = for (j <- 0 until w) yield {
          Twid_Constants(i * w + j)
        }
        tm
      }
      Twid_map_2D
    }
    twid_sets.map(x=>x.map(y=>y.map(z=>z.print_complex)))
    val Twid_sets_vec_Re = VecInit(for(i <- 0 until number_of_stages) yield{
      val temp = for(j <- 0 until N/w) yield{
        val sub_temp = VecInit(twid_sets(i)(j).map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U(bw.W)))
        sub_temp
      }
      val temp_vec = VecInit(temp)
      temp_vec
    })
    val Twid_sets_vec_Im = VecInit(for(i <- 0 until number_of_stages) yield{
      val temp = for(j <- 0 until N/w) yield{
        val sub_temp = VecInit(twid_sets(i)(j).map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U(bw.W)))
        sub_temp
      }
      val temp_vec = VecInit(temp)
      temp_vec
    })
    val Perm_sets = VecInit(for(k <- 0 until 2) yield {
      var generated_address = Permutations.GenerateStreamingMappingV2(N, w, r, r, k)
      generated_address = Permutations.Mapping_Sort(generated_address, 0)
      val M1_access = (for (i <- 0 until N / w) yield {
        val M1_slice = (for (j <- 0 until w) yield {
          generated_address(i)(j)._2._2
        }).toArray
        M1_slice
      }).toArray
      M1_access.map(x => println(x.toList))
      val M1_vec = for (i <- 0 until N / w) yield {
        val V = VecInit(M1_access(i).map(_.U((log2Ceil(w).W))))
        V
      }
      val vvf = VecInit(M1_vec)
      vvf
    })
    //----------------------------------------------------------------------------------------------------------------
    val M0_config_sets = VecInit(for(k <- 0 until 2) yield{
      var generated_address = Permutations.GenerateStreamingMappingV2(N, w, r, r, k)
      generated_address = Permutations.Mapping_Sort(generated_address, 0)
      val M0_access = (for (i <- 0 until N / w) yield {
        val M0_slice = (for (j <- 0 until w) yield {
          generated_address(i)(j)._1._1
        }).toArray
        M0_slice
      }).toArray
      M0_access.map(x => println(x.toList))
      val M0_vec = for (i <- 0 until N / w) yield {
        val V = VecInit(M0_access(i).map(_.U((log2Ceil((N / w) * 2)).W)))
        V
      }
      val vvf = VecInit(M0_vec)
      vvf
    })
    val M1_config_sets = VecInit(for(k <- 0 until 2) yield {
      var generated_address = Permutations.GenerateStreamingMappingV2(N, w, r, r, k)
      generated_address = Permutations.Mapping_Sort(generated_address, 1)
      val M1_access = (for (i <- 0 until N / w) yield {
        val M1_slice = (for (j <- 0 until w) yield {
          generated_address(i)(j)._2._1
        }).toArray
        M1_slice
      }).toArray
      M1_access.map(x => println(x.toList))
      val M1_vec = for (i <- 0 until N / w) yield {
        val V = VecInit(M1_access(i).map(_.U((log2Ceil((N / w) * 2)).W)))
        V
      }
      val vvf = VecInit(M1_vec)
      vvf
    })
    val end_regdelay = RegInit(VecInit.fill(N/w)(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw)))))// to ensure that propagating inputs have enough time to move through the structure
    for(i <- 0 until N/w){
      if(i == 0){
        final_regdelays(0) := DFT_regdelays(number_of_stages_depth-1)(DFT_latency-1)
        for(j <- 0 until w/r){
          for(k <- 0 until r){
            end_regdelay(0)(j*r+k) := DFT_modules(number_of_stages_depth-1)(j).out(k)
          }
        }
      }else{
        end_regdelay(i) := end_regdelay(i-1)
        final_regdelays(i) := final_regdelays(i-1)
      }
    }
    val iteration_cnt = RegInit(0.U(log2Ceil((number_of_stages/number_of_stages_depth) + 1).W))
    val offset = RegInit(0.U(log2Ceil(d*((number_of_stages/number_of_stages_depth)+1)).W))
    for(i <- 0 until number_of_stages_depth){
      for(j <- 0 until w) {
        Twid_Modules(i).in_twid_data(j).Re := Twid_sets_vec_Re(i.U + offset)(Twid_Modules(i).out_cnt)(j)
        Twid_Modules(i).in_twid_data(j).Im := Twid_sets_vec_Im(i.U + offset)(Twid_Modules(i).out_cnt)(j)
      }
      if(i != 0){
        Perm_modules(i).in_perm := Perm_sets(0)(Perm_modules(i).out_cnt)
        Perm_modules(i).in_M0_config := M0_config_sets(0)(Perm_modules(i).out_cnt)
        Perm_modules(i).in_M1_config := M1_config_sets(0)(Perm_modules(i).out_cnt)
      }
    }
    when(iteration_cnt =/= 0.U){
//      Perm_regdelays(0)(0) := DFT_regdelays(number_of_stages_depth-1)(DFT_latency-1)
      Perm_regdelays(0)(0) := final_regdelays((N/w)-1)
//      Perm_modules(0).in_en(0) := DFT_regdelays(number_of_stages_depth-1)(DFT_latency-1)
      Perm_modules(0).in_en(0) := final_regdelays((N/w)-1)
//      for(i <- 0 until w/r){
//        for(j <- 0 until r){
//          Perm_modules(0).in(i*r + j) := DFT_modules(number_of_stages_depth-1)(i).out(j)
//        }
//      }
      Perm_modules(0).in := end_regdelay((N/w)-1)
      Perm_modules(0).in_perm := Perm_sets(0)(Perm_modules(0).out_cnt)
      Perm_modules(0).in_M0_config := M0_config_sets(0)(Perm_modules(0).out_cnt)
      Perm_modules(0).in_M1_config := M1_config_sets(0)(Perm_modules(0).out_cnt)
    }.otherwise{
      Perm_regdelays(0)(0) := io.in_ready
      Perm_modules(0).in_en(0) := io.in_ready
      Perm_modules(0).in := io.in
      Perm_modules(0).in_perm := Perm_sets(1)(Perm_modules(0).out_cnt)
      Perm_modules(0).in_M0_config := M0_config_sets(1)(Perm_modules(0).out_cnt)
      Perm_modules(0).in_M1_config := M1_config_sets(1)(Perm_modules(0).out_cnt)
    }
    val progress_cnt = RegInit(0.U(log2Ceil(overrall_latency).W))
    when(progress_cnt === (overrall_latency-1).U){
      progress_cnt := 0.U
    }.otherwise{
      progress_cnt := progress_cnt + 1.U
    }
    for(i <- 0 until increment_points.length){
      if(i == increment_points.length - 1){
        when(progress_cnt === (increment_points(i)-1).U){ // have reached increment point
          iteration_cnt := 0.U
          offset := 0.U
        }
      }else {
        when(progress_cnt === (increment_points(i)-1).U) {
          iteration_cnt := iteration_cnt + 1.U
          offset := offset + d.U
        }
      }
    }

    for(i <- 0 until number_of_stages_depth){
      for(j <- 0 until T_L){
        if(j == 0){
          if(i != 0){
            Twid_regdelays(i)(0) := Perm_regdelays(i)(Perm_latency-1)
            Twid_Modules(i).in_en(0) := Perm_regdelays(i)(Perm_latency-1)
            Twid_Modules(i).in := Perm_modules(i).out
          }else{
            Twid_regdelays(0)(0) := Perm_regdelays(0)(Perm_latency-1)
            Twid_Modules(0).in_en(0) := Perm_regdelays(0)(Perm_latency-1)
            Twid_Modules(0).in := Perm_modules(0).out
          }
        }else{
          Twid_regdelays(i)(j) := Twid_regdelays(i)(j-1)
          Twid_Modules(i).in_en(j) := Twid_regdelays(i)(j-1)
        }
      }
    }

    for(i <- 0 until number_of_stages_depth){
      for(j <- 0 until DFT_latency){
        if(j == 0){
          if(i == 0){
            DFT_regdelays(0)(0) := Twid_regdelays(0)(T_L-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                DFT_modules(0)(k).in(l) := Twid_Modules(0).out(k*r + l)
              }
            }
          }else{
            DFT_regdelays(i)(0) := Twid_regdelays(i)(T_L-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                DFT_modules(i)(k).in(l) := Twid_Modules(i).out(k*r + l)
              }
            }
          }
        }else{
          DFT_regdelays(i)(j) := DFT_regdelays(i)(j-1)
        }
      }
    }

    for(i <- 0 until number_of_stages_depth){
      for(j <- 0 until Perm_latency){
        if(j == 0){
          if(i == 0){

          }else{
            Perm_regdelays(i)(0) := DFT_regdelays(i-1)(DFT_latency-1)
            Perm_modules(i).in_en(0) := DFT_regdelays(i-1)(DFT_latency-1)
            for(k <- 0 until w/r){
              for(l <- 0 until r){
                Perm_modules(i).in(k*r + l) := DFT_modules(i-1)(k).out(l)
              }
            }
          }
        }else{
          Perm_regdelays(i)(j) := Perm_regdelays(i)(j-1)
          Perm_modules(i).in_en(j) := Perm_regdelays(i)(j-1)
          if(j == Perm_latency-1){
            Perm_modules(i).in_en((N/w)*2) := Perm_regdelays(i)(Perm_latency-1)
          }
        }
      }
    }

    val results = RegInit(VecInit.fill(w)(0.U.asTypeOf(new ComplexNum(bw))))
    when(progress_cnt >= increment_points(increment_points.length-1).U){
      when(Perm_regdelays(0)(Perm_latency-1)){
        out_regdelay := Perm_regdelays(0)(Perm_latency-1)
        results := Perm_modules(0).out
      }.otherwise{
        out_regdelay := false.B
        for(i <- 0 until w){
          results(i) := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
    }.otherwise{
      out_regdelay := false.B
      for(i <- 0 until w){
        results(i) := 0.U.asTypeOf(new ComplexNum(bw))
      }
    }
    io.out_validate := out_regdelay
    io.out := results
//        for(i <- 0 until w/r){
//          for(j <- 0 until r){
//            io.out_test(i*r + j) := DFT_modules(0)(i).out(j)
//          }
//        }
    io.out_test := Perm_modules(0).out
    io.out_cnt := Perm_regdelays(0)(0)
  }

  class FFT_SingleRadix_Iterative(N:Int, r: Int, d:Int, w: Int, bw: Int) extends Module { //in progress
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_test = Output(Vec(w, new ComplexNum(bw)))
      val out_cnt = Output(UInt(bw.W))
    })
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
    val latency = (d*Perm_latency) + (d*Twid_latency) + (d*DFT_latency)
    var overrall_latency = 0
    val increment_points = mutable.ArrayBuffer[Int]()
    for(i <- 0 until number_of_stages/d){
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
    val progress_cnt = RegInit(0.U(log2Ceil(overrall_latency).W))
    val list_of_twids = Permutations.T2(N,r)
    val initial_twid = for(i <- 0 until N) yield{
      cmplx(1,0)
    }
    val adjusted_rom_array = for(i <- 0 until (Math.log10(N)/Math.log10(r)).round.toInt) yield{
      if(i == 0){
        initial_twid.toArray
      }else{
        list_of_twids(i-1)
      }
    }
    val Perm_sets = VecInit(for(k <- 0 until 2) yield {
      if (k == 0){
        val generated_address = (if(k == 1){Permutations.L[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}else{Permutations.L[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}).toVector
        val vvf = VecInit(generated_address.map(x=>x.U))
        vvf
      }else{
        val generated_address = (if(k == 1){Permutations.R[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}else{Permutations.L[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}).toVector
        val vvf = VecInit(generated_address.map(x=>x.U))
        vvf
      }
    })
    val twid_sets_Re = VecInit(for(i <- 0 until number_of_stages) yield {
      val Twid_Constants = adjusted_rom_array(i)
      VecInit(Twid_Constants.map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U(bw.W)))
    })
    val twid_sets_Im = VecInit(for(i <- 0 until number_of_stages) yield {
      val Twid_Constants = adjusted_rom_array(i)
      VecInit(Twid_Constants.map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U(bw.W)))
    })
    val DFT_modules = for(i <- 0 until d)yield{
      val instances = for(j <- 0 until N/r)yield{
        val instance = Module(new DFT_Symmetric_NRV(r,bw)).io
        instance
      }
      instances
    }
    val Twid_modules = for(i <- 0 until d)yield{
      val instance = Module(new TwiddleFactors_NoReduction(N,r,w,bw)).io
      instance
    }
    val Perm_modules = for(i <- 0 until d)yield{
      val instance = Module(new Permutations_forIterative(N,r,bw)).io
      instance
    }
    val DFT_regdelays = RegInit(VecInit.fill(d)(VecInit.fill(DFT_latency)(false.B)))
    val Twid_regdelays = RegInit(VecInit.fill(d)(VecInit.fill(Twid_latency)(false.B)))
    val Perm_regdelays = RegInit(VecInit.fill(d)(VecInit.fill(Perm_latency)(false.B)))
    val out_regdelay = RegInit((false.B))
    for(i <- 0 until d){
      for(j <- 0 until DFT_latency){
        if(j == 0){
          if(i == 0){
            DFT_regdelays(0)(0) := Twid_regdelays(0)(Twid_latency-1)
            for(k <- 0 until N/r){
              for(l <- 0 until r){
                DFT_modules(0)(k).in(l) := Twid_modules(0).out(k*r+l)
              }
            }
          }else{
            DFT_regdelays(i)(0) := Twid_regdelays(i)(Twid_latency-1)
            for(k <- 0 until N/r){
              for(l <- 0 until r){
                DFT_modules(i)(k).in(l) := Twid_modules(i).out(k*r+l)
              }
            }
          }
        }else{
          DFT_regdelays(i)(j) := DFT_regdelays(i)(j-1)
        }
      }
    }
    for(i <- 0 until d){
      for(j <- 0 until Twid_latency){
        if(j == 0){
          if(i == 0){
            Twid_regdelays(0)(0) := Perm_regdelays(0)(0)
            Twid_modules(0).in := Perm_modules(0).out
          }else{
            Twid_regdelays(i)(0) := Perm_regdelays(i)(0)
            Twid_modules(i).in := Perm_modules(i).out
          }
        }else{
          Twid_regdelays(i)(j) := Twid_regdelays(i)(j-1)
        }
      }
    }

    val iteration_cnt = RegInit(0.U((log2Ceil(number_of_stages/d)+1).W))
    val offset = RegInit(0.U(((log2Ceil(d*(number_of_stages/d))+1)).W))
    for(i <- 0 until increment_points.length){
      if(i == increment_points.length - 1){
        when(progress_cnt === (increment_points(i)-1).U){
          iteration_cnt := 0.U
          offset := 0.U
        }
      }else{
        when(progress_cnt === (increment_points(i)-1).U){
          iteration_cnt := iteration_cnt + 1.U
          offset := offset + d.U
        }
      }
    }
    when(iteration_cnt =/= 0.U){
      Perm_regdelays(0)(0) := DFT_regdelays(d-1)(DFT_latency-1)
      for(k <- 0 until N/r){
        for(l <- 0 until r){
          Perm_modules(0).in(k*r+l) := DFT_modules(d-1)(k).out(l)
        }
      }
      Perm_modules(0).in_perm_data := Perm_sets(0)
    }.otherwise{
      Perm_regdelays(0)(0) := io.in_ready
      Perm_modules(0).in := io.in
      Perm_modules(0).in_perm_data := Perm_sets(1)
    }
    for(i <- 0 until d){
      for(j <- 0 until Perm_latency){
        if(j == 0){
          if(i == 0){

          }else{
            Perm_regdelays(i)(0) := DFT_regdelays(i-1)(DFT_latency-1)
            for(k <- 0 until N/r){
              for(l <- 0 until r){
                Perm_modules(i).in(k*r+l) := DFT_modules(i-1)(k).out(l)
              }
            }
          }
        }else{
          Perm_regdelays(i)(j) := Perm_regdelays(i)(j-1)
        }
      }
    }
    for(i <- 0 until d){
      for(j <- 0 until N){
        Twid_modules(i).in_twid_data(j).Re := twid_sets_Re(i.U+offset)(j)
        Twid_modules(i).in_twid_data(j).Im := twid_sets_Im(i.U+offset)(j)
      }
      if(i!=0){
        Perm_modules(i).in_perm_data := Perm_sets(0)
      }
    }
    when(progress_cnt === (overrall_latency-1).U){
      progress_cnt := 0.U
    }.otherwise{
      progress_cnt := progress_cnt + 1.U
    }

    val output_results = RegInit(VecInit.fill(N)(0.U.asTypeOf(new ComplexNum(bw))))
    when(progress_cnt >= (increment_points(increment_points.length-1)).U){
      when(Perm_regdelays(0)(Perm_latency-1)){
        out_regdelay := Perm_regdelays(0)(Perm_latency-1)
        output_results := Perm_modules(0).out
      }.otherwise{
        out_regdelay := false.B
        for(i <- 0 until N){
          output_results(i) := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
    }.otherwise{
      out_regdelay := false.B
      for(i <- 0 until N){
        output_results(i) := 0.U.asTypeOf(new ComplexNum(bw))
      }
    }
    io.out_validate := out_regdelay
    io.out := output_results
    io.out_test := Perm_modules(0).out
    io.out_cnt := Perm_regdelays(0)(0)
  }

  class FFT_MixedRadix_Streaming(N:Int, nr: Int, ns: Int, r: Int, s: Int, w: Int, bw: Int) extends Module{ // streaming single radix fft, still in progress
    val w1 = w // this will be the input and output width
    var w2 = Permutations.getw2(w1,N,nr,ns,s)
    println(s"The w2 is : ${w2}")
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out_validate = Output(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
//      val out_test = Output(Vec(w1, new ComplexNum(bw)))
//      val out_test1 = Output(Vec(w1, new ComplexNum(bw)))
//      val out_test2 = Output(Vec(w2, new ComplexNum(bw)))
//      val out_test3 = Output(Vec(w2, new ComplexNum(bw)))
//      val out_test4 = Output(Vec(w2, new ComplexNum(bw)))
//      val out_test5 = Output(Vec(w1, new ComplexNum(bw)))
//      val out_t_valid = Output(Bool())
    })
    var delay_cycles_stage2 = N/w1 - N/w2 // w2 will be bigger than w1
    // for the first N/w2 clock cycles, the output from the permutation should have a corresponding valid signal,
    // however, for the remaint N/w1-N/w2 clock cycles, the output from the permutation should have an invalid signal
    val CMultLatency = 2
    val T_L = CMultLatency
    var fftlatency1 = 0
    var fftlatency2 = 0
    val perm_latency1 = (N/w1)*2
    // 1. the w1<nr,w2<ns // already did
    // 2. the w1<nr, w2>=ns // not done yet
    // 3. the w1>=nr, w2<ns // not done yet
    // 4. the w1>=nr, w2>=ns // already did
    if(w1 < nr && w2 < ns){ //streaming width is less the a single FFTnr size - will have to be streamed as well \\ additionally this can only be the case if nr is greater than fundamental dft2 and 3 sizes
      println("case 1")
      fftlatency1 = getfftstreamedlatency(nr,r,w1,bw)
      fftlatency2 = getfftstreamedlatency(ns,s,w2,bw)
      val FFT_modules1 = Module(new FFT_SingleRadix_Streaming_NRO(nr,r,w1,bw)).io
      val FFT_modules2 = Module(new FFT_SingleRadix_Streaming_NRO(ns,s,w2,bw)).io // it seems that we have already accounted for the case where ns > s
      val Perm_module1 = Module(new PermutationsWithStreaming(N,ns,ns,w1,0,bw)).io
      val Perm_module2 = Module(new PermutationsWithStreaming_mr(N,nr,nr,w2,w1,0,bw,delay_cycles_stage2)).io
      val Perm_module3 = Module(new PermutationsWithStreaming_mr(N,ns,ns,w1,w2,0,bw,delay_cycles_stage2)).io
      val Twid_Modules = Module(new TwiddleFactorsStreamed_mr(N,nr,ns,w2,bw,((N/w1)))).io

      val DFT_regdelays1 = RegInit(VecInit.fill(fftlatency1)(false.B)) // we might not need  the dft regdelays for the streamed ffts (they already use it)
      val DFT_regdelays2 = RegInit(VecInit.fill(fftlatency2)(false.B))
      val Twid_regdelays = RegInit(VecInit.fill(T_L)(false.B))
      val Perm_regdelays1 = RegInit(VecInit.fill(3)(VecInit.fill(perm_latency1)(false.B)))
      val out_regdelay = RegInit(false.B)
      val results = RegInit(VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw))))
      for(i <- 0 until 3){
        for(j <- 0 until perm_latency1){
          if(j == 0){
            if(i == 0){
              Perm_regdelays1(0)(0) := io.in_ready
              Perm_module1.in_en(0) := io.in_ready
              Perm_module1.in := io.in
            }else if(i == 1){
              Perm_regdelays1(1)(0) := FFT_modules1.out_validate
              Perm_module2.in_en(0) := FFT_modules1.out_validate
              Perm_module2.in := FFT_modules1.out
            }else{
              Perm_regdelays1(2)(0) := FFT_modules2.out_validate
              Perm_module3.in_en(0) := FFT_modules2.out_validate
              Perm_module3.in := FFT_modules2.out
            }
          }else{
            Perm_regdelays1(i)(j) := Perm_regdelays1(i)(j-1)
            if(i == 0){
              Perm_module1.in_en(j) := Perm_regdelays1(0)(j-1)
            }else if(i == 1){
              Perm_module2.in_en(j) := Perm_regdelays1(1)(j-1)
            }else{
              Perm_module3.in_en(j) := Perm_regdelays1(2)(j-1)
            }
            if(j == perm_latency1 - 1){
              if(i == 0){
                FFT_modules1.in_ready := Perm_regdelays1(0)(perm_latency1-1)
                Perm_module1.in_en(perm_latency1) := Perm_regdelays1(0)(perm_latency1-1)
                FFT_modules1.in := Perm_module1.out
              }else if(i == 1){
                Perm_module2.in_en(perm_latency1) := Perm_regdelays1(1)(perm_latency1-1)
              }else{
                out_regdelay := Perm_regdelays1(2)(perm_latency1-1)
                Perm_module3.in_en(perm_latency1) := Perm_regdelays1(2)(perm_latency1-1)
              }
            }
          }
        }
      }
      for(i <- 0 until T_L){
        if(i == 0){
          Twid_regdelays(0) := Perm_regdelays1(1)(perm_latency1-1)
          Twid_Modules.in_en(0) := Perm_regdelays1(1)(perm_latency1-1)
          Twid_Modules.in := Perm_module2.out
        }else{
          Twid_regdelays(i) := Twid_regdelays(i-1)
          Twid_Modules.in_en(i) := Twid_regdelays(i-1)
          if(i == T_L - 1){
            FFT_modules2.in_ready := Twid_regdelays(T_L-1)
            FFT_modules2.in := Twid_Modules.out
          }
        }
      }
      when(Perm_regdelays1(2)(perm_latency1-1)){
        results := Perm_module3.out
      }.otherwise{
        for(i <- 0 until w1){
          results(i) := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
      io.out := results
//      io.out_test := Perm_module1.out// end of first version
//      io.out_test1 := FFT_modules1.out
//      io.out_test2 := Perm_module2.out
//      io.out_test3 := Twid_Modules.out
//      io.out_test4 := FFT_modules2.out
//      io.out_test5 := Perm_module3.out
      io.out_validate := out_regdelay // i think this is all we need for this case
    }else if(w1 < nr && w2 >=ns){
      println("case 2")
      fftlatency1 = getfftstreamedlatency(nr,r,w1,bw) // in this case the fft1 is reduced streaming width
      fftlatency2 = getFFTLatency(ns,s,ns,bw) // the fft2 is full streaming width
      val FFT_modules1 = Module(new FFT_SingleRadix_Streaming_NRO(nr,r,w1,bw)).io
      //same case with the other sets of FFT modules
      val FFT_modules2 = if(ns == s){ // it could either be that ns == s
        val row = for(j <- 0 until w2/ns)yield{ // dft modules
          val instance = Module(new DFT_Symmetric_NRV(s,bw)).io
          instance
        }
        row
      }else{ // or it is greater
        val row = for(j <- 0 until w2/ns) yield{
          val instance = Module(new FFT_SingleRadix_NRV(ns,s,ns,bw)).io
          instance
        }
        row
      }
      val Perm_module1 = Module(new PermutationsWithStreaming(N,ns,ns,w1,0,bw)).io //  no changes needed yet
      val Perm_module2 = Module(new PermutationsWithStreaming_mr(N,nr,nr,w2,w1,0,bw,delay_cycles_stage2)).io
      val Perm_module3 = Module(new PermutationsWithStreaming_mr(N,ns,ns,w1,w2,0,bw,delay_cycles_stage2)).io
      val Twid_Modules = Module(new TwiddleFactorsStreamed_mr(N,nr,ns,w2,bw,((N/w1)))).io


      val DFT_regdelays1 = RegInit(VecInit.fill(fftlatency1)(false.B))
      val DFT_regdelays2 = RegInit(VecInit.fill(fftlatency2)(false.B))
      val Twid_regdelays = RegInit(VecInit.fill(T_L)(false.B)) // twid has latency of 2
      val Perm_regdelays1 = RegInit(VecInit.fill(3)(VecInit.fill(perm_latency1)(false.B))) // there are 3 permutation modules
      val out_regdelay = RegInit(false.B) // this is the output register for validate
      val results = RegInit(VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw)))) // here is the output registers for outputs
      for(i <- 0 until 3){
        for(j <- 0 until perm_latency1){
          if(j == 0){
            if(i == 0){
              Perm_regdelays1(0)(0) := io.in_ready
              Perm_module1.in_en(0) := io.in_ready
              Perm_module1.in := io.in
            }else if(i==1){
              Perm_regdelays1(1)(0) := DFT_regdelays1(fftlatency1-1)
              Perm_module2.in_en(0) := DFT_regdelays1(fftlatency1-1)
              Perm_module2.in := FFT_modules1.out
            }else{
              Perm_regdelays1(2)(0) := DFT_regdelays2(fftlatency2-1)
              Perm_module3.in_en(0) := DFT_regdelays2(fftlatency2-1)
              for(k <- 0 until w2/ns){
                for(l <- 0 until ns){
                  Perm_module3.in(k*ns + l) := FFT_modules2(k).out(l)
                }
              }
            }
          }else{
            Perm_regdelays1(i)(j) := Perm_regdelays1(i)(j-1)
            if(i == 0){
              Perm_module1.in_en(j) := Perm_regdelays1(0)(j-1)
            }else if(i==1){
              Perm_module2.in_en(j) := Perm_regdelays1(1)(j-1)
            }else{
              Perm_module3.in_en(j) := Perm_regdelays1(2)(j-1)
            }
            if(j == perm_latency1 - 1){
              if(i == 0){
                Perm_module1.in_en(perm_latency1) := Perm_regdelays1(0)(perm_latency1-1)
              }else if(i==1){
                Perm_module2.in_en(perm_latency1) := Perm_regdelays1(1)(perm_latency1-1)
              }else{
                Perm_module3.in_en(perm_latency1) := Perm_regdelays1(2)(perm_latency1-1)
              }
            }
          }
        }
      }
      for(i <- 0 until T_L){
        if(i==0){
          Twid_regdelays(0) := Perm_regdelays1(1)(perm_latency1 -1 )
          Twid_Modules.in_en(0) := Perm_regdelays1(1)(perm_latency1-1)
          Twid_Modules.in := Perm_module2.out
        }else{
          Twid_regdelays(i) := Twid_regdelays(i-1)
          Twid_Modules.in_en(i) := Twid_regdelays(i-1)
        }
      }

      for(i <- 0 until fftlatency1){
        if(i == 0){
          DFT_regdelays1(0) := Perm_regdelays1(0)(perm_latency1-1)
          FFT_modules1.in := Perm_module1.out
          FFT_modules1.in_ready := Perm_regdelays1(0)(perm_latency1-1)
        }else{
          DFT_regdelays1(i) := DFT_regdelays1(i-1)
        }
      }

      for(i <- 0 until fftlatency2){
        if(i == 0){
          DFT_regdelays2(0) := Twid_regdelays(T_L - 1)
          for(k <- 0 until w2/ns){
            for(l <- 0 until ns){
              FFT_modules2(k).in(l) := Twid_Modules.out(k*ns + l)
            }
          }
        }else{
          DFT_regdelays2(i) := DFT_regdelays2(i-1)
        }
      }
      out_regdelay := Perm_regdelays1(2)(perm_latency1-1)
      when(Perm_regdelays1(2)(perm_latency1 -1)){
        results := Perm_module3.out
      }.otherwise{
        results := VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw)))
      }
      io.out_validate := out_regdelay
      io.out := results
//      io.out_test := Perm_module1.out// end of first version
//      io.out_test1 := FFT_modules1.out
//      io.out_test2 := Perm_module2.out
//      io.out_test3 := Twid_Modules.out
//      for(i <- 0 until w2/ns){
//        for(j <- 0 until ns){
//          io.out_test4(i*w2 + j) := FFT_modules2(i).out(j)
//        }
//      }
//      io.out_test5 := Perm_module3.out
    }else if(w1>=nr && w2 < ns){
      println("case 3")
      fftlatency1 = getFFTLatency(nr,r,nr,bw) // the fft1 is full streaming width
      fftlatency2 = getfftstreamedlatency(ns,s,w2,bw) // the fft2 is a reduced streaming width
      val FFT_modules1 = if(nr == r){ // if this this the case then we just need to instantiate dft modules
        val row = for(j <- 0 until w1/nr) yield{ // since nr == r
          val instance = Module(new DFT_Symmetric_NRV(r,bw)).io
          instance
        }
        row
      }else{ // otherwise we instantiate full streaming width fft modules
        val row = for(j <- 0 until w1/nr) yield{ // since nr != r // w is ideally a multiple of nr // also notice that this is based on the first streaming width
          val instance = Module(new FFT_SingleRadix_NRV(nr,r,nr,bw)).io
          instance
        }
        row
      }
      //same case with the other sets of FFT modules
      val FFT_modules2 = Module(new FFT_SingleRadix_Streaming_NRO(ns,s,w2,bw)).io // the streamed FFT2
      val Perm_module1 = Module(new PermutationsWithStreaming(N,ns,ns,w1,0,bw)).io
      val Perm_module2 = Module(new PermutationsWithStreaming_mr(N,nr,nr,w2,w1,0,bw,delay_cycles_stage2)).io
      val Perm_module3 = Module(new PermutationsWithStreaming_mr(N,ns,ns,w1,w2,0,bw,delay_cycles_stage2)).io
      val Twid_Modules = Module(new TwiddleFactorsStreamed_mr(N,nr,ns,w2,bw,((N/w1)))).io


      val DFT_regdelays1 = RegInit(VecInit.fill(fftlatency1)(false.B))
      val DFT_regdelays2 = RegInit(VecInit.fill(fftlatency2)(false.B))
      val Twid_regdelays = RegInit(VecInit.fill(T_L)(false.B)) // twid has latency of 2
      val Perm_regdelays1 = RegInit(VecInit.fill(3)(VecInit.fill(perm_latency1)(false.B))) // there are 3 permutation modules
      val out_regdelay = RegInit(false.B) // this is the output register for validate
      val results = RegInit(VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw)))) // here is the output registers for outputs
      for(i <- 0 until 3){
        for(j <- 0 until perm_latency1){
          if(j == 0){
            if(i == 0){
              Perm_regdelays1(0)(0) := io.in_ready
              Perm_module1.in_en(0) := io.in_ready
              Perm_module1.in := io.in
            }else if(i==1){
              Perm_regdelays1(1)(0) := DFT_regdelays1(fftlatency1-1)
              Perm_module2.in_en(0) := DFT_regdelays1(fftlatency1-1)
              for(k <- 0 until w1/nr){
                for(l <- 0 until nr){
                  Perm_module2.in(k*nr + l) := FFT_modules1(k).out(l)
                }
              }
            }else{
              Perm_regdelays1(2)(0) := DFT_regdelays2(fftlatency2-1)
              Perm_module3.in_en(0) := DFT_regdelays2(fftlatency2-1)
              Perm_module3.in := FFT_modules2.out
            }
          }else{
            Perm_regdelays1(i)(j) := Perm_regdelays1(i)(j-1)
            if(i == 0){
              Perm_module1.in_en(j) := Perm_regdelays1(0)(j-1)
            }else if(i==1){
              Perm_module2.in_en(j) := Perm_regdelays1(1)(j-1)
            }else{
              Perm_module3.in_en(j) := Perm_regdelays1(2)(j-1)
            }
            if(j == perm_latency1 - 1){
              if(i == 0){
                Perm_module1.in_en(perm_latency1) := Perm_regdelays1(0)(perm_latency1-1)
              }else if(i==1){
                Perm_module2.in_en(perm_latency1) := Perm_regdelays1(1)(perm_latency1-1)
              }else{
                Perm_module3.in_en(perm_latency1) := Perm_regdelays1(2)(perm_latency1-1)
              }
            }
          }
        }
      }
      for(i <- 0 until T_L){
        if(i==0){
          Twid_regdelays(0) := Perm_regdelays1(1)(perm_latency1 -1 )
          Twid_Modules.in_en(0) := Perm_regdelays1(1)(perm_latency1-1)
          Twid_Modules.in := Perm_module2.out
        }else{
          Twid_regdelays(i) := Twid_regdelays(i-1)
          Twid_Modules.in_en(i) := Twid_regdelays(i-1)
        }
      }

      for(i <- 0 until fftlatency1){
        if(i == 0){
          DFT_regdelays1(0) := Perm_regdelays1(0)(perm_latency1-1)
          for(k <- 0 until w1/nr){
            for(l <- 0 until nr){
              FFT_modules1(k).in(l) := Perm_module1.out(k*nr + l)
            }
          }
        }else{
          DFT_regdelays1(i) := DFT_regdelays1(i-1)
        }
      }

      for(i <- 0 until fftlatency2){
        if(i == 0){
          DFT_regdelays2(0) := Twid_regdelays(T_L - 1)
          FFT_modules2.in_ready := Twid_regdelays(T_L-1)
          FFT_modules2.in := Twid_Modules.out
        }else{
          DFT_regdelays2(i) := DFT_regdelays2(i-1)
        }
      }
      out_regdelay := Perm_regdelays1(2)(perm_latency1-1)
      when(Perm_regdelays1(2)(perm_latency1 -1)){
        results := Perm_module3.out
      }.otherwise{
        results := VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw)))
      }
      io.out_validate := out_regdelay
      io.out := results
//      io.out_test := Perm_module1.out// end of first version
//      for(i <- 0 until w1/nr){
//        for(j <- 0 until nr){
//          io.out_test1(i*nr + j) := FFT_modules1(i).out(j)
//        }
//      }
//      io.out_test2 := Perm_module2.out
//      io.out_test3 := Twid_Modules.out
//      io.out_test4 := FFT_modules2.out
//      io.out_test5 := Perm_module3.out
    }else if(w1 >= nr && w2 >= ns){ // streaming width is greater than or equal to the FFTnr size (this also implies the same case for the FFTns size as well)
      println("case 4")
      fftlatency1 = getFFTLatency(nr,r,nr,bw) // this function should also give the DFT_NRV latency as well if we set the nr==r
      fftlatency2 = getFFTLatency(ns,s,ns,bw)
      val FFT_modules1 = if(nr == r){ // if this this the case then we just need to instantiate dft modules
        val row = for(j <- 0 until w1/nr) yield{ // since nr == r
          val instance = Module(new DFT_Symmetric_NRV(r,bw)).io
          instance
        }
        row
      }else{ // otherwise we instantiate full streaming width fft modules
        val row = for(j <- 0 until w1/nr) yield{ // since nr != r // w is ideally a multiple of nr // also notice that this is based on the first streaming width
          val instance = Module(new FFT_SingleRadix_NRV(nr,r,nr,bw)).io
          instance
        }
        row
      }
      //same case with the other sets of FFT modules
      val FFT_modules2 = if(ns == s){
        val row = for(j <- 0 until w2/ns)yield{ // dft modules
          val instance = Module(new DFT_Symmetric_NRV(s,bw)).io
          instance
        }
        row
      }else{
        val row = for(j <- 0 until w2/ns) yield{
          val instance = Module(new FFT_SingleRadix_NRV(ns,s,ns,bw)).io
          instance
        }
        row
      }
      // so there is always a total of w1/nr dft modules in the first stage and w2/ns dft modules in the second stage
      val Perm_module1 = Module(new PermutationsWithStreaming(N,ns,ns,w1,0,bw)).io
      val Perm_module2 = Module(new PermutationsWithStreaming_mr(N,nr,nr,w2,w1,0,bw,delay_cycles_stage2)).io
      val Perm_module3 = Module(new PermutationsWithStreaming_mr(N,ns,ns,w1,w2,0,bw,delay_cycles_stage2)).io
      val Twid_Modules = Module(new TwiddleFactorsStreamed_mr(N,nr,ns,w2,bw,((N/w1)))).io


      val DFT_regdelays1 = RegInit(VecInit.fill(fftlatency1)(false.B))
      val DFT_regdelays2 = RegInit(VecInit.fill(fftlatency2)(false.B))
      val Twid_regdelays = RegInit(VecInit.fill(T_L)(false.B)) // twid has latency of 2
      val Perm_regdelays1 = RegInit(VecInit.fill(3)(VecInit.fill(perm_latency1)(false.B))) // there are 3 permutation modules
      val out_regdelay = RegInit(false.B) // this is the output register for validate
      val results = RegInit(VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw)))) // here is the output registers for outputs
      for(i <- 0 until 3){
        for(j <- 0 until perm_latency1){
          if(j == 0){
            if(i == 0){
              Perm_regdelays1(0)(0) := io.in_ready
              Perm_module1.in_en(0) := io.in_ready
              Perm_module1.in := io.in
            }else if(i==1){
              println(fftlatency1)
              Perm_regdelays1(1)(0) := DFT_regdelays1(fftlatency1-1)
              Perm_module2.in_en(0) := DFT_regdelays1(fftlatency1-1)
              for(k <- 0 until w1/nr){
                for(l <- 0 until nr){
                  Perm_module2.in(k*nr + l) := FFT_modules1(k).out(l)
                }
              }
            }else{
              Perm_regdelays1(2)(0) := DFT_regdelays2(fftlatency2-1)
              Perm_module3.in_en(0) := DFT_regdelays2(fftlatency2-1)
              for(k <- 0 until w2/ns){
                for(l <- 0 until ns){
                  Perm_module3.in(k*ns + l) := FFT_modules2(k).out(l)
                }
              }
            }
          }else{
            Perm_regdelays1(i)(j) := Perm_regdelays1(i)(j-1)
            if(i == 0){
              Perm_module1.in_en(j) := Perm_regdelays1(0)(j-1)
            }else if(i==1){
              Perm_module2.in_en(j) := Perm_regdelays1(1)(j-1)
            }else{
              Perm_module3.in_en(j) := Perm_regdelays1(2)(j-1)
            }
            if(j == perm_latency1 - 1){
              if(i == 0){
                Perm_module1.in_en(perm_latency1) := Perm_regdelays1(0)(perm_latency1-1)
              }else if(i==1){
                Perm_module2.in_en(perm_latency1) := Perm_regdelays1(1)(perm_latency1-1)
              }else{
                Perm_module3.in_en(perm_latency1) := Perm_regdelays1(2)(perm_latency1-1)
              }
            }
          }
        }
      }
      for(i <- 0 until T_L){
        if(i==0){
          Twid_regdelays(0) := Perm_regdelays1(1)(perm_latency1 -1 )
          Twid_Modules.in_en(0) := Perm_regdelays1(1)(perm_latency1-1)
          Twid_Modules.in := Perm_module2.out
        }else{
          Twid_regdelays(i) := Twid_regdelays(i-1)
          Twid_Modules.in_en(i) := Twid_regdelays(i-1)
        }
      }

      for(i <- 0 until fftlatency1){
        if(i == 0){
          DFT_regdelays1(0) := Perm_regdelays1(0)(perm_latency1-1)
          for(k <- 0 until w1/nr){
            for(l <- 0 until nr){
              FFT_modules1(k).in(l) := Perm_module1.out(k*nr + l)
            }
          }
        }else{
          DFT_regdelays1(i) := DFT_regdelays1(i-1)
        }
      }

      for(i <- 0 until fftlatency2){
        if(i == 0){
          DFT_regdelays2(0) := Twid_regdelays(T_L - 1)
          for(k <- 0 until w2/ns){
            for(l <- 0 until ns){
              FFT_modules2(k).in(l) := Twid_Modules.out(k*ns + l)
            }
          }
        }else{
          DFT_regdelays2(i) := DFT_regdelays2(i-1)
        }
      }
      out_regdelay := Perm_regdelays1(2)(perm_latency1-1)
      when(Perm_regdelays1(2)(perm_latency1 -1)){
        results := Perm_module3.out
      }.otherwise{
        results := VecInit.fill(w1)(0.U.asTypeOf(new ComplexNum(bw)))
      }
      io.out_validate := out_regdelay
      io.out := results
//      io.out_test := Perm_module1.out// end of first version
//      for(i <- 0 until w1/nr){
//        for(j <- 0 until nr){
//          io.out_test1(i*nr + j) := FFT_modules1(i).out(j)
//        }
//      }
//      io.out_test2 := Perm_module2.out
//      io.out_test3 := Twid_Modules.out
//      for(i <- 0 until w2/ns){
//        for(j <- 0 until ns){
//          io.out_test4(i*w2 + j) := FFT_modules2(i).out(j)
//        }
//      }
//      io.out_test5 := Perm_module3.out
    }
  }

}
