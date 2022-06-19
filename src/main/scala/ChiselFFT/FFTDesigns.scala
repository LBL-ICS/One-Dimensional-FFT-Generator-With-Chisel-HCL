package ChiselFFT
import chisel3._
import ComplexModules.FPComplex._
import implementation._
import IEEEConversions.FPConvert._

object FFTDesigns {
  class DFT_r(r: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(r, new ComplexNum(bw)))
      val out = Output(Vec(r, new ComplexNum(bw)))
    })
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
    for(i <- 0 until r-1){
      for(j <- 0 until r-1){
        FP_Mults(i)(j).in_a.Re := convert_string_to_IEEE_754(DFTr_Constants(1+i)(1+j).re.toString, bw).U
        FP_Mults(i)(j).in_b := io.in( 1 + j)
        FP_Mults(i)(j).in_a.Im := convert_string_to_IEEE_754(DFTr_Constants(1+i)(1+j).im.toString, bw).U
      }
    }
    val mult_results = Wire(Vec(r, Vec(r, new ComplexNum(bw))))
    for(i <- 0 until r){
      mult_results(0)(i) := io.in(i)
      if(i != 0 )
        mult_results(i)(0) := io.in(0)
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
    for(i <- 0 until r){
      io.out(i) := FP_adds(i).out
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
  class FFT_sr(N:Int, r: Int, w: Int, bw: Int) extends Module{
    val io = IO(new Bundle() {
      val in = Input(Vec(N, new ComplexNum(bw)))
      val out = Output(Vec(N, new ComplexNum(bw)))
    })
    val DFTs_per_stage = N/r
    val number_of_stages = (Math.log10(N)/Math.log10(r)).round.toInt
    val CMultLatency = 2
    val CAddLatency = 1
    val DFT_latency = CMultLatency + ((Math.log10(r)/Math.log10(2)).round.toInt + (for(l <- 0 until (Math.log10(r)/Math.log10(2)).round.toInt)yield{(r/Math.pow(2,l)).round.toInt % 2}).reduce(_+_)) * (CAddLatency)
    val Twid_latency = (N/w) * CMultLatency
    val Perm_latency = 0
    val Total_Latency = (number_of_stages - 1) * Twid_latency + (number_of_stages) * DFT_latency + (number_of_stages + 1) * Perm_latency
    println(s"Total Latency: ${(Total_Latency, DFT_latency, Twid_latency, Perm_latency)}")
    val DFT_instances = (for(i <- 0 until number_of_stages) yield{
      val DFT_instnace_row = (for(j <- 0 until DFTs_per_stage) yield{
        val DFT_instance = Module(new DFT_r(r, bw)).io
        DFT_instance
      }).toVector
      DFT_instnace_row
    }).toVector
    val Input_Permutation = Module(new PermutationsBasic(N,r,1,bw)).io
    val Stage_Permutations = (for(i <- 0 until number_of_stages)yield{
      val stage_permutation = Module(new PermutationsBasic(N,r,0,bw)).io
      stage_permutation
    }).toVector
    val TwiddleFactorModules = (for(i <- 0 until number_of_stages - 1) yield {
      val Twid = Module(new TwiddleFactors(N,r,N,i,bw)).io
      Twid
    }).toVector
    for(i <- 0 until N){
      Input_Permutation.in(i) := io.in(i)
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
      if(i == number_of_stages - 1){
        io.out := Stage_Permutations(i).out
      }
    }
  }
}
