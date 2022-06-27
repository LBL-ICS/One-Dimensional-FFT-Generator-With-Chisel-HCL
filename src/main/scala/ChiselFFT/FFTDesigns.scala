package ChiselFFT
import chisel3._
import chisel3.util._
import ComplexModules.FPComplex._
import implementation._
import IEEEConversions.FPConvert._
import Chisel.{ MuxLookup,log2Ceil}

object FFTDesigns {
  class DFT_EnVal(DFT_Latency: Int, isPipeline: Boolean) extends Module{
    val io = IO(new Bundle() {
      val in_enable = Input(Bool())
      val out_enable = Output(Bool())
      val out_validate = Output(Bool())
    })
    val cnt = RegInit(0.U(log2Ceil(DFT_Latency).W))
    val start_condition = RegInit(false.B)
    val en_s = WireInit(false.B)
    val val_s = Wire(Bool())
    val can_Pipe = Reg(Bool())
    when(io.in_enable && start_condition =/= true.B){
      start_condition := true.B
      en_s := true.B
    }.elsewhen(!io.in_enable){
      start_condition := false.B
      cnt := 0.U
      en_s := false.B
    }
    when(start_condition){
      if(isPipeline){
        en_s := true.B
        when((cnt === DFT_Latency.U - 1.U) || can_Pipe) {
          val_s := true.B
        }.otherwise{
          val_s := false.B
        }
      }else{
        when(cnt === DFT_Latency.U - 1.U){
          en_s := true.B
          val_s := true.B
        }.otherwise{
          en_s := false.B
          val_s := false.B
        }
      }
      when(cnt === DFT_Latency.U-1.U){
        cnt := 0.U
        can_Pipe := true.B
      }.otherwise{
        cnt := cnt + 1.U
        can_Pipe := can_Pipe
      }
    }.otherwise{
      val_s := false.B
      can_Pipe := false.B
    }

    io.out_enable := en_s
    io.out_validate := val_s
  }
  class DFT_r(r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(r, new ComplexNum(bw)))
      val in_en = Input(Bool())
      val in_Override = Input(Bool())
      val out = Output(Vec(r, new ComplexNum(bw)))
    })
    val CMultLatency = 2
    val CAddLatency = 1
    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
    val EnVal_Control = Module(new DFT_EnVal(DFT_latency, true)).io
    EnVal_Control.in_enable := io.in_en
    val DFTr_Constants = FFT.DFT_gen(r).map(_.toVector).toVector
    val FP_Mults = (for(i <- 0 until r-1) yield{
      val fpmults = (for(j <- 0 until r-1)yield{
        val fpmult = Module(new FPComplexMult(bw)).io
        fpmult
      }).toVector
      fpmults
    }).toVector
    val FP_adds = (for(i <- 0 until r)yield{
      val fpadds = Module(new FPComplexMultiAdder(r, bw)).io
      fpadds
    }).toVector
    when((EnVal_Control.out_enable || io.in_Override) && io.in_en) {
      for (i <- 0 until r - 1) {
        for (j <- 0 until r - 1) {
          FP_Mults(i)(j).in_a.Re := convert_string_to_IEEE_754(DFTr_Constants(1 + i)(1 + j).re.toString, bw).U
          FP_Mults(i)(j).in_b := io.in(1 + j)
          FP_Mults(i)(j).in_a.Im := convert_string_to_IEEE_754(DFTr_Constants(1 + i)(1 + j).im.toString, bw).U
        }
      }
    }.otherwise{
      for (i <- 0 until r - 1) {
        for (j <- 0 until r - 1) {
          FP_Mults(i)(j).in_a.Re := 0.U
          FP_Mults(i)(j).in_b.Re := 0.U
          FP_Mults(i)(j).in_b.Im := 0.U
          FP_Mults(i)(j).in_a.Im := 0.U
        }
      }
    }
    val reg_syncs = (for(i <- 0 until CMultLatency)yield{
      val regs = Reg(Vec(r, new ComplexNum(bw)))
      regs
    }).toVector
    for(i <- 0 until CMultLatency){
      if(i==0){
        when((EnVal_Control.out_enable || io.in_Override) && io.in_en){
          for(j <- 0 until r){
            reg_syncs(i)(j) := io.in(j)
          }
        }.otherwise{
          for(j <- 0 until r){
            reg_syncs(i)(j).Re := 0.U
            reg_syncs(i)(j).Im := 0.U
          }
        }
      }else{
        reg_syncs(i) := reg_syncs(i-1)
      }
    }
    val mult_results = Wire(Vec(r, Vec(r, new ComplexNum(bw))))
    for(i <- 0 until r){
      mult_results(0)(i) := reg_syncs(CMultLatency-1)(i)
      if(i != 0 )
        mult_results(i)(0) := reg_syncs(CMultLatency-1)(0)
    }
    for(i <- 0 until r-1){
      for(j <- 0 until r-1){
        mult_results(i+1)(j+1) := FP_Mults(i)(j).out_s
      }
    }
    for(i <- 0 until r){
      for(j <- 0 until r){
        FP_adds(i).in(j) := mult_results(i)(j)
      }
    }
    val result = Wire(Vec(r, new ComplexNum(bw)))
    when((EnVal_Control.out_validate || io.in_Override) && io.in_en) {
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
  class TwiddleFactors(N: Int, r:Int, w: Int, stage:Int, bw: Int) extends Module {
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val out = Output(Vec(w, new ComplexNum(bw)))
    })
    val TotalStages = ((Math.log10(N) / Math.log10(r)).round - 1).toInt
//    val TRes = VecInit(for(i <- 0 until TotalStages)yield{
//      val Tre = VecInit(Permutations.T2(N,r)(i).map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U))
//      Tre
//    })
//    val TIms = VecInit(for(i <- 0 until TotalStages)yield{
//      val TIm = VecInit(Permutations.T2(N,r)(i).map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U))
//      TIm
//    })
    val TwiddleFactorConstantsRe = VecInit(Permutations.T2(N,r)(stage).map(x=>convert_string_to_IEEE_754(x.re.toString, bw).U))
    Permutations.T2(N,r)(stage).map(x=>x.print_complex)
    val TwiddleFactorConstantsIm = VecInit(Permutations.T2(N,r)(stage).map(x=>convert_string_to_IEEE_754(x.im.toString, bw).U))
    val TotalCycles = N/w
    val offset = w
    val CMultLatency = 2
    val FPMults = (for(i <- 0 until w)yield{
      val fpmult = Module(new FPComplexMult(bw)).io
      fpmult
    }).toVector
    val cnt = Reg(UInt(bw.W))
    val cnt2 = Reg(UInt(bw.W))
    when(cnt2 === (CMultLatency-1).U){
      cnt2 := 0.U
      when(cnt === ((N/w)-1).U){
        cnt := 0.U
      }.otherwise{
        cnt := cnt + 1.U
      }
    }.otherwise{
      cnt := cnt
      cnt2 := cnt2 + 1.U
    }
    for(i <- 0 until w){
      FPMults(i).in_a.Re := TwiddleFactorConstantsRe(cnt * w.U + i.U)
      FPMults(i).in_a.Im := TwiddleFactorConstantsIm(cnt * w.U + i.U)
      FPMults(i).in_b := io.in(cnt * w.U + i.U)
      io.out(i) := FPMults(i).out_s
    }
  }
  class PermutationsBasic(N:Int, r: Int, Type: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val out = Output(Vec(N, new ComplexNum(bw)))
    })
    val PermutationModel = (if(Type == 1){Permutations.R[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}else{Permutations.L[Int]((for(i <- 0 until N)yield{i}).toArray, N, r)}).toVector
    for(i <- 0 until N){
      io.out(i) := io.in(PermutationModel(i))
    }
  }
  class PermutationsWithStreaming(N:Int, r: Int, base_r: Int, w:Int, ptype: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(w, new ComplexNum(bw)))
      val in_en = Input(Bool())
      val out = Output(Vec(w, new ComplexNum(bw)))
      val out_t = Output(new ComplexNum(bw))
    })
    val is_enabled = Reg(Bool())
    val offset = (N/w).U
    val offset_switch = Reg(UInt(1.W))
    when(io.in_en){
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
    val Perm_Mod = Module(new PermutationModuleStreamed(w, bw)).io
    val M0_config = Module(new M0_Config_ROM(N,r,base_r,ptype,w)).io
    val M1_config = Module(new M1_Config_ROM(N,r,base_r, ptype, w)).io
    val Perm_Config = Module(new Streaming_Permute_Config(N,r, base_r, ptype, w)).io
    val cnt = RegInit(0.U(log2Ceil(N/w).W))
    when(is_enabled){
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
      cnt := cnt
    }
    M0_config.in_cnt := cnt
    M1_config.in_cnt := cnt
    Perm_Config.in_cnt := cnt
    io.out_t := Perm_Mod.out(2)
  }
  class PermutationModuleStreamed(w: Int, bw: Int) extends Module {
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
    var generated_address = Permutations.GenerateStreamingMapping(N,w,r,base_r, ptype)
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
    var generated_address = Permutations.GenerateStreamingMapping(N,w,r,base_r, ptype)
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
    var generated_address = Permutations.GenerateStreamingMapping(N,w,r,base_r, ptype)
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
  class FFT_sr(N:Int, r: Int, w: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val in_en = Input(Bool())
      val in_Override = Input(Bool())
      val out = Output(Vec(N, new ComplexNum(bw)))
      val out_test = Output(new ComplexNum(bw))
    })
    val DFTs_per_stage = N/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val CMultLatency = 2
    val CAddLatency = 1
    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
    val Twid_latency = (N/w) * CMultLatency
    val Perm_latency = 0
    val Total_Latency = (number_of_stages - 1) * Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency
    val enval_mod = Module(new DFT_EnVal(Total_Latency,true)).io
    enval_mod.in_enable := io.in_en
    println(s"Total Latency: ${(Total_Latency, DFT_latency, Twid_latency, Perm_latency)}")
    val DFT_instances = (for(i <- 0 until number_of_stages) yield{
      val DFT_instnace_row = (for(j <- 0 until DFTs_per_stage) yield{
        val DFT_instance = Module(new DFT_r(r, bw)).io
        DFT_instance
      }).toVector
      DFT_instnace_row
    }).toVector
    for(i <- 0 until number_of_stages){
      for (j <- 0 until DFTs_per_stage){
        DFT_instances(i)(j).in_Override := true.B
        DFT_instances(i)(j).in_en := io.in_en
      }
    }
    val Input_Permutation = Module(new PermutationsBasic(N,r,1,bw)).io
    val Stage_Permutations = (for(i <- 0 until number_of_stages)yield{
      val stage_permutation = Module(new PermutationsBasic(N,r,0,bw)).io
      stage_permutation
    }).toVector
    val TwiddleFactorModules = (for(i <- 0 until number_of_stages - 1) yield {
      val Twid = Module(new TwiddleFactors(N,r,N,i,bw)).io
      Twid
    }).toVector
    when((enval_mod.out_enable || io.in_Override) && io.in_en){
      for(i <- 0 until N){
        Input_Permutation.in(i) := io.in(i)
      }
    }.otherwise{
      for(i <- 0 until N){
        Input_Permutation.in(i).Re := 0.U
        Input_Permutation.in(i).Im := 0.U
      }
    }
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
      when((enval_mod.out_validate || io.in_Override) && io.in_en){
        if(i == number_of_stages - 1){
          io.out := Stage_Permutations(i).out
        }
      }.otherwise{
        if(i == number_of_stages - 1){
          for(j <- 0 until N){
            io.out(j).Re := 0.U
            io.out(j).Im := 0.U
          }
        }
      }
      io.out_test := DFT_instances(1)(0).out(0)
    }
  }
}
