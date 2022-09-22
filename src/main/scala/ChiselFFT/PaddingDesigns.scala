package ChiselFFT
import chisel3._
import Chisel.log2Ceil
import chisel3.util._
import scala.collection.mutable
import ComplexModules.FPComplex._
import IEEEConversions.FPConvert._
import chisel3.tester._
import chisel3.tester.RawTester.test
import java.io.PrintWriter


object PaddingDesigns {
//  class Non_streaming_Padding(streamingWidth: Int, paddedLength: Int, bw: Int) extends Module{
//    val io = IO(new Bundle() {
//      val en = Input(Bool())
//      val start = Input(Bool())
//      val in = Input(Vec(streamingWidth, new ComplexNum(bw)))
//      val out = Output(Vec(streamingWidth, new ComplexNum(bw)))
//      val new_iteration_ready = Output(Bool())
//    })
//    val regVecSize = streamingWidth
//    val shiftRegCount = paddedLength/regVecSize
//    val regs = VecInit.fill(shiftRegCount)(VecInit.fill(regVecSize)(RegInit(0.U.asTypeOf(new ComplexNum(bw)))))
//    val boolRegs = VecInit.fill(shiftRegCount)(RegInit(false.B))
//    val hotStates = VecInit.fill(shiftRegCount-1)(false.B)
//    when((~hotStates.asUInt.orR).asBool){
//      boolRegs(0) := io.start
//    }.otherwise{
//      boolRegs(0) := false.B
//    }
//
//    for(i <- 1 until shiftRegCount){
//      boolRegs(i) := boolRegs(i-1)
//      hotStates(i-1) := boolRegs(i-1)
//    }
//    io.new_iteration_ready := ~hotStates.asUInt.orR
//    io.out := regs(shiftRegCount-1)
//
//  }

//  class streaming_padding(stglength: Array[Int], inputLength: Int,streamingWidth: Int, paddedLength: Int, bw: Int, offsets: Array[Int], cycles: Array[Int], placements: Array[Int]) extends Module {
//    val io = IO(new Bundle{
//      val in = Input(Vec(streamingWidth, new ComplexNum(bw)))
//      val in_start = Input(Bool())
//      val out = Output(Vec(streamingWidth, new ComplexNum(bw)))
//      val out_finished = Output(Bool())
//    })
//    val offsets_ROM = Module(new constants_ROM(offsets, 48)).io
//    val cycles_ROM = Module(new constants_ROM(cycles, 21)).io
//    val placement_ROM = Module(new constants_ROM(placements, 48)).io
//    val stglength_ROM = Module(new constants_ROM(stglength, 42)).io
//
//    val shiftRegCount = paddedLength/streamingWidth
//    val regs = RegInit(VecInit.fill(shiftRegCount)(VecInit.fill(streamingWidth)(0.U.asTypeOf(new ComplexNum(bw)))))
//    val en_regs = RegInit(VecInit.fill(shiftRegCount)(false.B))
//    val flag_reg = RegInit(true.B)
//    val cnt = RegInit(0.U(log2Ceil(offsets.length + 1).W))
//
//    offsets_ROM.in_addr := cnt
//    cycles_ROM.in_addr := cnt
//    placement_ROM.in_addr := cnt
//    stglength_ROM.in_addr := cnt
//
//    val NORGateInp = VecInit.fill(shiftRegCount-1)(false.B)
//    val NORGateOut = (~NORGateInp.asUInt.orR).asBool
//
//    for(i <- 1 until shiftRegCount){
//      en_regs(i) := en_regs(i-1)
//      NORGateInp(i-1) := en_regs(i-1)
//    }
//
//    // mux at the input
//    when(NORGateOut){
//      en_regs(0) := io.in_start
//    }.otherwise{
//      en_regs(0) := false.B
//    }
//
//    val flagMUXInp = VecInit.fill(shiftRegCount)(false.B)
//    for(i <- 0 until shiftRegCount){
//      flagMUXInp(i) := en_regs(i)
//    }
//
//    when(en_regs(shiftRegCount-2)){
//      flag_reg := true.B
//    }.elsewhen(flagMUXInp(cycles_ROM.out_data - 1.U)){
//      flag_reg := false.B
//    }.otherwise{
//      flag_reg := flag_reg
//    }
//
//    io.out_finished := en_regs(shiftRegCount-1)
//
//    when(cnt === offsets.length.U - 1.U || en_regs(shiftRegCount-2)){
//      cnt := 0.U
//    }.elsewhen(en_regs(shiftRegCount-2)){
//      cnt := cnt + 1.U
//    }.otherwise{
//      cnt := cnt
//    }
//
//    val placement_wires = VecInit.fill(shiftRegCount-1)(VecInit.fill(streamingWidth)(0.U.asTypeOf(new ComplexNum(bw))))
//    val evenOddBit = offsets_ROM.out_data(0)
//
//    for(i <- 1 until shiftRegCount){
//      when(placement_ROM.out_data === i.U - 1.U){
//        when(flag_reg){
//          placement_wires(i - 1) := io.in
//        }.otherwise{
//          placement_wires(i- 1) := VecInit.fill(streamingWidth)(0.U.asTypeOf(new ComplexNum(bw)))
//        }
//      }.otherwise{
//        placement_wires(i - 1) := VecInit.fill(streamingWidth)(0.U.asTypeOf(new ComplexNum(bw)))
//      }
//      // might need to change this later
//      val paddi = Wire(UInt(log2Ceil(paddedLength).W))
//      paddi := (paddedLength.U - stglength_ROM.out_data) / 2.U
//
//      val regs1_streamingwidth_wire = Wire(UInt(log2Ceil(streamingWidth).W))
//      val regs2_streamingwidth_wire = Wire(UInt(log2Ceil(streamingWidth).W))
//      printf(p"${regs1_streamingwidth_wire}")
//
//      regs1_streamingwidth_wire := paddi % streamingWidth.U
//      regs2_streamingwidth_wire := streamingWidth.U - regs1_streamingwidth_wire
//
//      val def_pad = (paddedLength - inputLength) / 2
//      //used only for odd offset cases
//      val regs1_streamwidth = def_pad % streamingWidth
//      val regs2_streamwidth = streamingWidth - regs1_streamwidth
//      when(evenOddBit){
//        for (j <- 0 until regs1_streamwidth) {
//          regs(shiftRegCount-1)(j).Re := regs(shiftRegCount-2)(j).Re
//          regs(shiftRegCount-1)(j).Im := regs(shiftRegCount-2)(j).Im
//          regs(i)(j+regs2_streamwidth).Re := regs(i-1)(j+regs2_streamwidth).Re | placement_wires(i-1)(j).Re
//          regs(i)(j + regs2_streamwidth).Im := regs(i - 1)(j + regs2_streamwidth).Im | placement_wires(i - 1)(j).Im
//        }
//        for(j <- 0 until regs2_streamwidth){
////          printf(p"${placement_wires(i-1)(j+regs1_streamwidth).Re}")
//          if(i == 1){
//            regs(i - 1)(j).Re := placement_wires(i-1)(j + regs1_streamwidth).Re
//            regs(i - 1)(j).Im := placement_wires(i-1)(j + regs1_streamwidth).Im
//          }else{
//            regs(i-1)(j).Re := placement_wires(i-1)(j+regs1_streamwidth).Re | regs(i-2)(j).Re
//            regs(i-1)(j).Im := placement_wires(i-1)(j+regs1_streamwidth).Im | regs(i-2)(j).Im
//          }
//        }
//      }.otherwise{
//        for(j <- 0 until streamingWidth){
//          regs(i)(j).Re := regs(i-1)(j).Re | placement_wires(i-1)(j).Re
//          regs(i)(j).Im := regs(i-1)(j).Im | placement_wires(i-1)(j).Im
//        }
//      }
//    }
//    io.out := regs(shiftRegCount-1)
//  }

  // Did not map into SRAMs for some reason. Still synthesized into regs
  // but only utilizes 1% of all available regs on the U250 board
  class SRAM_Block_streaming(entries: Int, streamingwidth: Int, bw: Int) extends Module {
    val io = IO(new Bundle(){
      val in_en = Input(Bool())
      val in_rst = Input(Bool())
      val in_write_en = Input(Bool())
      val in_offset = Input(UInt(streamingwidth.W))
      val in_addr = Input(UInt(log2Ceil(entries/streamingwidth).W))
      val in_data = Input(Vec(streamingwidth, new ComplexNum(bw)))
      val out_data = Output(Vec(streamingwidth, new ComplexNum(bw)))
    })
    //need to divde the single RAM into smaller rams to allow for streaming
    // for instance if we have a streaming width of 2, then 96/48 = 2 separate SRAM blocks
    // need to be written in by two, and read out by two
    val entries_per_ram = entries/streamingwidth
    val sram_mems = for (i <- 0 until streamingwidth) yield {
      SyncReadMem(entries_per_ram, new ComplexNum(bw))
    }
    io.out_data := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
//    sram_mems(0)
    when(io.in_rst){ // clear the memory if reset (might move to within the enable statement)
      for(i <- 0 until entries/streamingwidth) {
        for (j <- 0 until streamingwidth) {
          sram_mems(j)(i) := 0.U.asTypeOf(new ComplexNum(bw))
        }
      }
    }.elsewhen(io.in_en) {
      when(io.in_write_en) {
        for (i <- 0 until streamingwidth) {
          when(i.U >= io.in_offset) {
            sram_mems(i)(io.in_addr) := io.in_data(i.U - io.in_offset)
          }
          when(i.U < io.in_offset) {
            sram_mems(i)(io.in_addr + 1.U) := io.in_data(i.U + io.in_offset)
          }
        }
        io.out_data := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
      }.otherwise {
        for (i <- 0 until streamingwidth) {
          io.out_data(i) := sram_mems(i)(io.in_addr)
        }
      }
//      when(io.in_offset === 0.U) {
//        val rdwrports = for (i <- 0 until streamingwidth) yield {
//          sram_mems(i)(io.in_addr)
//        }
//        when(io.in_write_en) {
//          for (i <- 0 until streamingwidth) {
//            rdwrports(i) := io.in_data(i)
//          }
//          io.out_data := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
//        }.otherwise {
//          for (i <- 0 until streamingwidth) {
//            io.out_data(i) := rdwrports(i)
//          }
//        }
//      }.otherwise{
//        when(io.in_write_en){
//          for(i <- 0 until streamingwidth){
//            when(i.U >= io.in_offset){
//              sram_mems(i)(io.in_addr) := io.in_data(i.U - io.in_offset)
//            }
//            when(i.U < io.in_offset){
//              sram_mems(i)(io.in_addr +  1.U) := io.in_data(i.U + io.in_offset)
//            }
//          }
//          io.out_data := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
//        }.otherwise{
//          for(i <- 0 until streamingwidth){
//            io.out_data(i) := sram_mems(i)(io.in_addr)
//          }
//        }
//      }
    }
  }

  class one_dimensional_padding_streaming(paddinglength: Int, streamingwidth: Int, segment_lengths: Array[Int], bw: Int) extends Module{
    val io = IO(new Bundle{
      val in = Input(Vec(streamingwidth, new ComplexNum(bw)))
      val in_ready = Input(Bool())
      val out = Output(Vec(streamingwidth, new ComplexNum(bw)))
      val out_ready = Output(Bool())
      val out_valid = Output(Bool())
    })
    val ccs_per_segment = for(i <- 0 until segment_lengths.length) yield{
      val c = segment_lengths(i)/streamingwidth
      if(segment_lengths(i)%streamingwidth != 0)
        c+1
      else
        c
    }
    val offset_per_segment = for(i <- 0 until segment_lengths.length) yield{
      val l = ((paddinglength - segment_lengths(i)) / 2)%streamingwidth
      l
    }
    val startind_per_segment = for(i <- 0 until segment_lengths.length) yield{
      val l = ((paddinglength - segment_lengths(i)) / 2)/streamingwidth
      l
    }
    println(s"ccs_per_segment: ${ccs_per_segment}")
    println(s"offset_per_segment: ${offset_per_segment}")
    println(s"startind_per_segment: ${startind_per_segment}")
    println("______________________________________________")
    // creating some ROMs to hold some constants that we will itearte through after each iteration
    val offs_seg = Module(new constants_ROM(offset_per_segment.toArray, offset_per_segment.max + 1)).io
    val ccs_seg = Module(new constants_ROM(ccs_per_segment.toArray, ccs_per_segment.max)).io
    val sind_seg = Module(new constants_ROM(startind_per_segment.toArray, paddinglength/streamingwidth)).io
    val cnt_ccs = RegInit(0.U(log2Ceil(ccs_per_segment.max).W))
    val cnt_stg = RegInit(0.U((log2Ceil(segment_lengths.length) + 1).W))

    val switchModes = RegInit(false.B) // indicates when to alternate read/write modes in buffer

    val SRAM_Buffers = VecInit.fill(2)(Module(new SRAM_Block_streaming(paddinglength,streamingwidth, bw)).io)

    // it will take paddinglength/streamingwidth cycles to finalize one iteration
    val ccs = paddinglength/streamingwidth // 96/2 = 48 cycles for output stage

    val counter = new Counter(ccs)
    val out_valid_reg = RegInit(false.B)
    val out_ready_reg = RegInit(true.B)
    io.out_valid := out_valid_reg
    io.out_ready := out_ready_reg
    when(io.in_ready){
      when(counter.value === (ccs - 1).U) { // controls switching buffer modes and tracking stage
        out_ready_reg := true.B
        when(switchModes){
          SRAM_Buffers(0).in_rst := true.B
          SRAM_Buffers(1).in_rst := false.B
        }.otherwise{
          SRAM_Buffers(0).in_rst := false.B
          SRAM_Buffers(1).in_rst := true.B
        }
        switchModes := ~switchModes
        when(cnt_stg === (segment_lengths.length - 1).U) {
          cnt_stg := 0.U
        }.otherwise {
          cnt_stg := cnt_stg + 1.U
        }
      }.otherwise {
        when(cnt_ccs === ccs_seg.out_data - 1.U) {
          out_ready_reg := false.B
        }.elsewhen(cnt_ccs < ccs_seg.out_data) {
          out_ready_reg := true.B
        }
        SRAM_Buffers(0).in_rst := false.B
        SRAM_Buffers(1).in_rst := false.B
        switchModes := switchModes
        cnt_stg := cnt_stg
      }

      sind_seg.in_addr := cnt_stg // take stage as input
      ccs_seg.in_addr := cnt_stg
      offs_seg.in_addr := cnt_stg
      SRAM_Buffers(0).in_offset := offs_seg.out_data
      SRAM_Buffers(1).in_offset := offs_seg.out_data

      when(cnt_ccs < ccs_seg.out_data){
        cnt_ccs := cnt_ccs + 1.U
        SRAM_Buffers(0).in_en := true.B
        SRAM_Buffers(1).in_en := true.B
      }.otherwise{
        when(switchModes){
          SRAM_Buffers(0).in_en := true.B
          SRAM_Buffers(1).in_en := false.B
        }.otherwise{
          SRAM_Buffers(0).in_en := false.B
          SRAM_Buffers(1).in_en := true.B
        }
        when(counter.value === (ccs - 1).U){
          cnt_ccs := 0.U
        }.otherwise{
          cnt_ccs := cnt_ccs
        }
      }

      counter.inc()
//      SRAM_Buffers(0).in_en := true.B
//      SRAM_Buffers(1).in_en := true.B
      SRAM_Buffers(0).in_write_en := ~switchModes
      SRAM_Buffers(1).in_write_en := switchModes
      SRAM_Buffers(0).in_data := io.in
      SRAM_Buffers(1).in_data := io.in
      out_valid_reg := true.B
//      printf(p"addr: ${sind_seg.out_data + cnt_ccs}")
      when(switchModes){
        io.out := SRAM_Buffers(0).out_data
        SRAM_Buffers(0).in_addr := counter.value
        SRAM_Buffers(1).in_addr := (sind_seg.out_data + cnt_ccs)
      }.otherwise{
        io.out := SRAM_Buffers(1).out_data
        SRAM_Buffers(1).in_addr := counter.value
        SRAM_Buffers(0).in_addr := sind_seg.out_data + cnt_ccs
      }
    }.otherwise{
      out_valid_reg := false.B
      out_ready_reg := true.B
      io.out := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
      SRAM_Buffers(0).in_en := false.B
      SRAM_Buffers(1).in_en := false.B
      SRAM_Buffers(0).in_write_en := false.B
      SRAM_Buffers(1).in_write_en := false.B
      SRAM_Buffers(0).in_data := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
      SRAM_Buffers(1).in_data := VecInit.fill(streamingwidth)(0.U.asTypeOf(new ComplexNum(bw)))
      SRAM_Buffers(1).in_addr := 0.U
      SRAM_Buffers(0).in_addr := 0.U
      SRAM_Buffers(0).in_offset := 0.U
      SRAM_Buffers(1).in_offset := 0.U
      SRAM_Buffers(0).in_rst := false.B
      SRAM_Buffers(1).in_rst := false.B
      switchModes := switchModes
      sind_seg.in_addr := 0.U
      ccs_seg.in_addr := 0.U
      offs_seg.in_addr := 0.U
      cnt_stg := cnt_stg
      cnt_ccs := cnt_ccs
    }
  }

  // just an example module of how to use chisel counter
  class counter_test(iterations: Int) extends Module {
    val io = IO(new Bundle{
      val in_inc_en = Input(Bool())
      val out_count = Output(UInt(log2Ceil(iterations).W))
    })
    val cnt = new Counter(iterations)
    io.out_count := cnt.value
    when(io.in_inc_en){
      cnt.inc()
    }
  }

  def main(args: Array[String]): Unit = {
//    test(new counter_test(12)){c=>
//      c.io.in_inc_en.poke(true.B)
//      for(i <- 0 until 28){
//        c.clock.step(1)
//        println(s"counter value: ${c.io.out_count.peek().litValue}")
//      }
//    }
    val pw = new PrintWriter("one_dimensional_padding_streaming.v")
    pw.println(getVerilogString(new one_dimensional_padding_streaming(96,2,Array(16,34,42,40,18,36,22),32)))
    pw.close()
    val entries_test = 96
    val streaming_width = 2
    //need to clear out the srams after each use!!!!
    test(new one_dimensional_padding_streaming(96,2,Array(16,34,42,40,18,36,22),32)){c=>
      c.io.in_ready.poke(true.B)
      for(i <- 0 until (entries_test/streaming_width) * 8){
        for(j <- 0 until streaming_width){
          c.io.in(j).Re.poke(((i*streaming_width + j)%(entries_test/streaming_width)+1).U)
          c.io.in(j).Im.poke((i*streaming_width + j+1).U)
        }
//        println(s"before clock: ${c.io.out_ready.peek().litValue}")
//        println(s"ccbefore: ${c.io.out_cnt.peek().litValue}")
//        println(s"sw_before: ${c.io.out_test.peek().litValue}")
        println(s"Clock Cycle: ${(i)%(entries_test/streaming_width)}")
        c.clock.step(1)
//        println(s"sw_after: ${c.io.out_test.peek().litValue}")
//        println(s"ccafter: ${c.io.out_cnt.peek().litValue}")
//        println(s"after clock: ${c.io.out_ready.peek().litValue}")
        for (j <- 0 until streaming_width) {
          println(s"Output${j}: ${c.io.out(j).Re.peek().litValue}")
        }
      }
    }
    println(s"----------------------------------------------------------")
    println("\n\n\n\n\n")
    test(new SRAM_Block_streaming(entries_test,streaming_width,32)){c=>
      c.io.in_en.poke(true.B)
      c.io.in_write_en.poke(true.B)
      c.io.in_offset.poke(3.U)
//      c.io.in_addr.poke(0.U)
//      c.io.in_data(0).Re.poke(1.U)
//      c.io.in_data(0).Im.poke(2.U)
//      c.io.in_data(1).Re.poke(3.U)
//      c.io.in_data(1).Im.poke(4.U)
//      c.clock.step(1)
//      c.io.in_write_en.poke(false.B)
//      c.clock.step(1)
//      println(s"output: ${c.io.out_data(0).Re.peek().litValue}")
//      println(s"output: ${c.io.out_data(0).Im.peek().litValue}")
//      println(s"output: ${c.io.out_data(1).Re.peek().litValue}")
//      println(s"output: ${c.io.out_data(1).Im.peek().litValue}")
      for(i <- 0 until entries_test/streaming_width){
        c.io.in_addr.poke(i.U)
        for(j <- 0 until streaming_width){
          c.io.in_data(j).Re.poke((i*streaming_width + j).U)
          c.io.in_data(j).Im.poke((i*streaming_width + j).U)
        }
        c.clock.step(1)
      }
      c.io.in_write_en.poke(false.B)
      for(i <- 0 until entries_test/streaming_width){
        c.io.in_addr.poke(i.U)
        c.clock.step(1)
        println(s"clock cycle: ${i+1}")
        for(j <- 0 until streaming_width){
          println(s"Output${i*streaming_width + j}: ${c.io.out_data(j).Re.peek().litValue}")
        }
      }
    }
//    test(new SRAM_Block(10, 32)) { c =>
//      c.io.in_en.poke(true.B)
//      c.io.in_write_en.poke(true.B)
//      for(i <- 0 until 10){
//        c.io.in_addr.poke(i.U)
//        c.io.in_data.poke((i+1).U)
//        c.clock.step(1)
//      }
//      c.io.in_write_en.poke(false.B)
//      for(i <- 0 until 10){
//        c.io.in_addr.poke(i.U)
//        c.clock.step(1)
//        println(s"Output: ${c.io.out_data.peek().litValue}")
//      }
//    }
  }

  // intialize a ROM for constants
  class constants_ROM(constants: Array[Int], maxSize: Int) extends Module {
    val io = IO(new Bundle{
      val in_addr = Input(UInt(log2Ceil(constants.length).W))
      val out_data = Output(UInt(log2Ceil(maxSize).W))
    })
    val offsets_Vec = VecInit(constants.map(x=>x.U)) // might need to have it work in  similar manner to the SRAM. (seq output)
    io.out_data := offsets_Vec(io.in_addr)
  }
}
