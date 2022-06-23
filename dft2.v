module full_subber(
  input  [7:0] io_in_a,
  input  [7:0] io_in_b,
  output [7:0] io_out_s,
  output       io_out_c
);
  wire [8:0] _result_T = io_in_a - io_in_b; // @[Arithmetic.scala 72:23]
  wire [9:0] _result_T_2 = _result_T - 9'h0; // @[Arithmetic.scala 72:34]
  wire [8:0] result = _result_T_2[8:0]; // @[Arithmetic.scala 71:22 72:12]
  assign io_out_s = result[7:0]; // @[Arithmetic.scala 73:23]
  assign io_out_c = result[8]; // @[Arithmetic.scala 74:23]
endmodule
module twoscomplement(
  input  [7:0] io_in,
  output [7:0] io_out
);
  wire [7:0] _x_T = ~io_in; // @[Arithmetic.scala 28:16]
  assign io_out = 8'h1 + _x_T; // @[Arithmetic.scala 28:14]
endmodule
module full_adder(
  input  [23:0] io_in_a,
  input  [23:0] io_in_b,
  output [23:0] io_out_s,
  output        io_out_c
);
  wire [24:0] _result_T = io_in_a + io_in_b; // @[Arithmetic.scala 58:23]
  wire [25:0] _result_T_1 = {{1'd0}, _result_T}; // @[Arithmetic.scala 58:34]
  wire [24:0] result = _result_T_1[24:0]; // @[Arithmetic.scala 57:22 58:12]
  assign io_out_s = result[23:0]; // @[Arithmetic.scala 59:23]
  assign io_out_c = result[24]; // @[Arithmetic.scala 60:23]
endmodule
module twoscomplement_1(
  input  [23:0] io_in,
  output [23:0] io_out
);
  wire [23:0] _x_T = ~io_in; // @[Arithmetic.scala 28:16]
  assign io_out = 24'h1 + _x_T; // @[Arithmetic.scala 28:14]
endmodule
module shifter(
  input  [23:0] io_in_a,
  input  [4:0]  io_in_b,
  output [23:0] io_out_s
);
  wire [23:0] _result_T = io_in_a >> io_in_b; // @[Arithmetic.scala 42:25]
  wire [54:0] _GEN_0 = {{31'd0}, _result_T}; // @[Arithmetic.scala 41:26 42:14 44:14]
  assign io_out_s = _GEN_0[23:0]; // @[Arithmetic.scala 39:22]
endmodule
module leadingOneDetector(
  input  [23:0] io_in,
  output [4:0]  io_out
);
  wire [1:0] _hotValue_T = io_in[1] ? 2'h2 : 2'h1; // @[Mux.scala 47:70]
  wire [1:0] _hotValue_T_1 = io_in[2] ? 2'h3 : _hotValue_T; // @[Mux.scala 47:70]
  wire [2:0] _hotValue_T_2 = io_in[3] ? 3'h4 : {{1'd0}, _hotValue_T_1}; // @[Mux.scala 47:70]
  wire [2:0] _hotValue_T_3 = io_in[4] ? 3'h5 : _hotValue_T_2; // @[Mux.scala 47:70]
  wire [2:0] _hotValue_T_4 = io_in[5] ? 3'h6 : _hotValue_T_3; // @[Mux.scala 47:70]
  wire [2:0] _hotValue_T_5 = io_in[6] ? 3'h7 : _hotValue_T_4; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_6 = io_in[7] ? 4'h8 : {{1'd0}, _hotValue_T_5}; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_7 = io_in[8] ? 4'h9 : _hotValue_T_6; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_8 = io_in[9] ? 4'ha : _hotValue_T_7; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_9 = io_in[10] ? 4'hb : _hotValue_T_8; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_10 = io_in[11] ? 4'hc : _hotValue_T_9; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_11 = io_in[12] ? 4'hd : _hotValue_T_10; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_12 = io_in[13] ? 4'he : _hotValue_T_11; // @[Mux.scala 47:70]
  wire [3:0] _hotValue_T_13 = io_in[14] ? 4'hf : _hotValue_T_12; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_14 = io_in[15] ? 5'h10 : {{1'd0}, _hotValue_T_13}; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_15 = io_in[16] ? 5'h11 : _hotValue_T_14; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_16 = io_in[17] ? 5'h12 : _hotValue_T_15; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_17 = io_in[18] ? 5'h13 : _hotValue_T_16; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_18 = io_in[19] ? 5'h14 : _hotValue_T_17; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_19 = io_in[20] ? 5'h15 : _hotValue_T_18; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_20 = io_in[21] ? 5'h16 : _hotValue_T_19; // @[Mux.scala 47:70]
  wire [4:0] _hotValue_T_21 = io_in[22] ? 5'h17 : _hotValue_T_20; // @[Mux.scala 47:70]
  assign io_out = io_in[23] ? 5'h18 : _hotValue_T_21; // @[Mux.scala 47:70]
endmodule
module FP_adder(
  input         clock,
  input  [31:0] io_in_a,
  input  [31:0] io_in_b,
  output [31:0] io_out_s
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  wire [7:0] subber_io_in_a; // @[FPArithmetic.scala 76:24]
  wire [7:0] subber_io_in_b; // @[FPArithmetic.scala 76:24]
  wire [7:0] subber_io_out_s; // @[FPArithmetic.scala 76:24]
  wire  subber_io_out_c; // @[FPArithmetic.scala 76:24]
  wire [7:0] complement_io_in; // @[FPArithmetic.scala 82:28]
  wire [7:0] complement_io_out; // @[FPArithmetic.scala 82:28]
  wire [23:0] adder_io_in_a; // @[FPArithmetic.scala 86:23]
  wire [23:0] adder_io_in_b; // @[FPArithmetic.scala 86:23]
  wire [23:0] adder_io_out_s; // @[FPArithmetic.scala 86:23]
  wire  adder_io_out_c; // @[FPArithmetic.scala 86:23]
  wire [23:0] complementN_0_io_in; // @[FPArithmetic.scala 92:31]
  wire [23:0] complementN_0_io_out; // @[FPArithmetic.scala 92:31]
  wire [23:0] complementN_1_io_in; // @[FPArithmetic.scala 94:31]
  wire [23:0] complementN_1_io_out; // @[FPArithmetic.scala 94:31]
  wire [23:0] shifter_io_in_a; // @[FPArithmetic.scala 98:25]
  wire [4:0] shifter_io_in_b; // @[FPArithmetic.scala 98:25]
  wire [23:0] shifter_io_out_s; // @[FPArithmetic.scala 98:25]
  wire [23:0] complementN_2_io_in; // @[FPArithmetic.scala 143:31]
  wire [23:0] complementN_2_io_out; // @[FPArithmetic.scala 143:31]
  wire [23:0] leadingOneFinder_io_in; // @[FPArithmetic.scala 163:34]
  wire [4:0] leadingOneFinder_io_out; // @[FPArithmetic.scala 163:34]
  wire [7:0] subber2_io_in_a; // @[FPArithmetic.scala 165:25]
  wire [7:0] subber2_io_in_b; // @[FPArithmetic.scala 165:25]
  wire [7:0] subber2_io_out_s; // @[FPArithmetic.scala 165:25]
  wire  subber2_io_out_c; // @[FPArithmetic.scala 165:25]
  wire  sign_0 = io_in_a[31]; // @[FPArithmetic.scala 38:23]
  wire  sign_1 = io_in_b[31]; // @[FPArithmetic.scala 39:23]
  wire [8:0] _T_2 = 9'h100 - 9'h2; // @[FPArithmetic.scala 43:62]
  wire [8:0] _GEN_31 = {{1'd0}, io_in_a[30:23]}; // @[FPArithmetic.scala 43:34]
  wire [8:0] _GEN_0 = _GEN_31 > _T_2 ? _T_2 : {{1'd0}, io_in_a[30:23]}; // @[FPArithmetic.scala 43:68 44:14 46:14]
  wire [8:0] _GEN_32 = {{1'd0}, io_in_b[30:23]}; // @[FPArithmetic.scala 48:34]
  wire [8:0] _GEN_1 = _GEN_32 > _T_2 ? _T_2 : {{1'd0}, io_in_b[30:23]}; // @[FPArithmetic.scala 48:68 49:14 51:14]
  wire [22:0] frac_0 = io_in_a[22:0]; // @[FPArithmetic.scala 56:23]
  wire [22:0] frac_1 = io_in_b[22:0]; // @[FPArithmetic.scala 57:23]
  wire [23:0] whole_frac_0 = {1'h1,frac_0}; // @[FPArithmetic.scala 61:26]
  wire [23:0] whole_frac_1 = {1'h1,frac_1}; // @[FPArithmetic.scala 62:26]
  wire [7:0] exp_1 = _GEN_1[7:0]; // @[FPArithmetic.scala 42:19]
  wire [7:0] exp_0 = _GEN_0[7:0]; // @[FPArithmetic.scala 42:19]
  wire [7:0] out_exp = subber_io_out_c ? exp_1 : exp_0; // @[FPArithmetic.scala 104:34 105:15 115:15]
  wire [7:0] sub_exp = subber_io_out_c ? complement_io_out : subber_io_out_s; // @[FPArithmetic.scala 104:34 106:15 116:15]
  wire  out_s = subber_io_out_c ? sign_1 : sign_0; // @[FPArithmetic.scala 104:34 107:13 117:13]
  wire [22:0] out_frac = subber_io_out_c ? frac_1 : frac_0; // @[FPArithmetic.scala 104:34 108:16 118:16]
  wire [23:0] _GEN_8 = subber_io_out_c ? shifter_io_out_s : whole_frac_0; // @[FPArithmetic.scala 104:34 112:21 87:19]
  wire [23:0] _GEN_9 = subber_io_out_c ? whole_frac_1 : shifter_io_out_s; // @[FPArithmetic.scala 104:34 88:19 122:21]
  wire  _new_s_T = ~adder_io_out_c; // @[FPArithmetic.scala 138:15]
  wire  _D_T_1 = sign_0 ^ sign_1; // @[FPArithmetic.scala 151:39]
  wire  D = _new_s_T | sign_0 ^ sign_1; // @[FPArithmetic.scala 151:28]
  wire  E = _new_s_T & ~adder_io_out_s[23] | _new_s_T & ~_D_T_1 | adder_io_out_c & adder_io_out_s[23] & _D_T_1; // @[FPArithmetic.scala 154:99]
  wire  _GEN_25 = sub_exp >= 8'h17 ? out_s : ~adder_io_out_c & sign_0 | sign_0 & sign_1 | ~adder_io_out_c & sign_1; // @[FPArithmetic.scala 138:11 173:39 174:13]
  wire  new_s = io_in_a[30:0] == 31'h0 & io_in_b[30:0] == 31'h0 ? 1'h0 : _GEN_25; // @[FPArithmetic.scala 169:62 170:13]
  wire [23:0] adder_result = new_s & sign_0 != sign_1 ? complementN_2_io_out : adder_io_out_s; // @[FPArithmetic.scala 157:18 158:47 159:20]
  wire [4:0] _subber2_io_in_b_T_1 = 5'h18 - leadingOneFinder_io_out; // @[FPArithmetic.scala 167:42]
  wire [8:0] _GEN_33 = {{1'd0}, out_exp}; // @[FPArithmetic.scala 181:20]
  wire [23:0] _new_out_frac_T_2 = 24'h800000 - 24'h1; // @[FPArithmetic.scala 183:51]
  wire [7:0] _new_out_exp_T_3 = out_exp + 8'h1; // @[FPArithmetic.scala 185:32]
  wire [8:0] _GEN_13 = _GEN_33 == _T_2 ? _T_2 : {{1'd0}, _new_out_exp_T_3}; // @[FPArithmetic.scala 181:56 182:21 185:21]
  wire [23:0] _GEN_14 = _GEN_33 == _T_2 ? _new_out_frac_T_2 : {{1'd0}, adder_result[23:1]}; // @[FPArithmetic.scala 181:56 183:22 186:22]
  wire [53:0] _GEN_2 = {{31'd0}, adder_result[22:0]}; // @[FPArithmetic.scala 197:57]
  wire [53:0] _new_out_frac_T_7 = _GEN_2 << _subber2_io_in_b_T_1; // @[FPArithmetic.scala 197:57]
  wire [7:0] _GEN_15 = subber2_io_out_c ? 8'h1 : subber2_io_out_s; // @[FPArithmetic.scala 192:39 193:23 196:23]
  wire [53:0] _GEN_16 = subber2_io_out_c ? 54'h400000 : _new_out_frac_T_7; // @[FPArithmetic.scala 192:39 194:24 197:24]
  wire [7:0] _GEN_17 = leadingOneFinder_io_out == 5'h1 & adder_result == 24'h0 & (_D_T_1 & io_in_a[30:0] == io_in_b[30:0
    ]) ? 8'h0 : _GEN_15; // @[FPArithmetic.scala 189:141 190:21]
  wire [53:0] _GEN_18 = leadingOneFinder_io_out == 5'h1 & adder_result == 24'h0 & (_D_T_1 & io_in_a[30:0] == io_in_b[30:
    0]) ? 54'h0 : _GEN_16; // @[FPArithmetic.scala 189:141 139:18]
  wire [7:0] _GEN_19 = D ? _GEN_17 : 8'h0; // @[FPArithmetic.scala 140:17 188:26]
  wire [53:0] _GEN_20 = D ? _GEN_18 : 54'h0; // @[FPArithmetic.scala 139:18 188:26]
  wire [8:0] _GEN_21 = ~D ? _GEN_13 : {{1'd0}, _GEN_19}; // @[FPArithmetic.scala 180:26]
  wire [53:0] _GEN_22 = ~D ? {{30'd0}, _GEN_14} : _GEN_20; // @[FPArithmetic.scala 180:26]
  wire [8:0] _GEN_23 = E ? {{1'd0}, out_exp} : _GEN_21; // @[FPArithmetic.scala 177:26 178:19]
  wire [53:0] _GEN_24 = E ? {{31'd0}, adder_result[22:0]} : _GEN_22; // @[FPArithmetic.scala 177:26 179:20]
  wire [53:0] _GEN_26 = sub_exp >= 8'h17 ? {{31'd0}, out_frac} : _GEN_24; // @[FPArithmetic.scala 173:39 175:20]
  wire [8:0] _GEN_27 = sub_exp >= 8'h17 ? {{1'd0}, out_exp} : _GEN_23; // @[FPArithmetic.scala 173:39 176:19]
  wire [8:0] _GEN_29 = io_in_a[30:0] == 31'h0 & io_in_b[30:0] == 31'h0 ? 9'h0 : _GEN_27; // @[FPArithmetic.scala 169:62 171:19]
  wire [53:0] _GEN_30 = io_in_a[30:0] == 31'h0 & io_in_b[30:0] == 31'h0 ? 54'h0 : _GEN_26; // @[FPArithmetic.scala 169:62 172:20]
  reg [31:0] reg_out_s; // @[FPArithmetic.scala 201:24]
  wire [7:0] new_out_exp = _GEN_29[7:0]; // @[FPArithmetic.scala 137:27]
  wire [8:0] _reg_out_s_T = {new_s,new_out_exp}; // @[FPArithmetic.scala 202:24]
  wire [22:0] new_out_frac = _GEN_30[22:0]; // @[FPArithmetic.scala 136:28]
  full_subber subber ( // @[FPArithmetic.scala 76:24]
    .io_in_a(subber_io_in_a),
    .io_in_b(subber_io_in_b),
    .io_out_s(subber_io_out_s),
    .io_out_c(subber_io_out_c)
  );
  twoscomplement complement ( // @[FPArithmetic.scala 82:28]
    .io_in(complement_io_in),
    .io_out(complement_io_out)
  );
  full_adder adder ( // @[FPArithmetic.scala 86:23]
    .io_in_a(adder_io_in_a),
    .io_in_b(adder_io_in_b),
    .io_out_s(adder_io_out_s),
    .io_out_c(adder_io_out_c)
  );
  twoscomplement_1 complementN_0 ( // @[FPArithmetic.scala 92:31]
    .io_in(complementN_0_io_in),
    .io_out(complementN_0_io_out)
  );
  twoscomplement_1 complementN_1 ( // @[FPArithmetic.scala 94:31]
    .io_in(complementN_1_io_in),
    .io_out(complementN_1_io_out)
  );
  shifter shifter ( // @[FPArithmetic.scala 98:25]
    .io_in_a(shifter_io_in_a),
    .io_in_b(shifter_io_in_b),
    .io_out_s(shifter_io_out_s)
  );
  twoscomplement_1 complementN_2 ( // @[FPArithmetic.scala 143:31]
    .io_in(complementN_2_io_in),
    .io_out(complementN_2_io_out)
  );
  leadingOneDetector leadingOneFinder ( // @[FPArithmetic.scala 163:34]
    .io_in(leadingOneFinder_io_in),
    .io_out(leadingOneFinder_io_out)
  );
  full_subber subber2 ( // @[FPArithmetic.scala 165:25]
    .io_in_a(subber2_io_in_a),
    .io_in_b(subber2_io_in_b),
    .io_out_s(subber2_io_out_s),
    .io_out_c(subber2_io_out_c)
  );
  assign io_out_s = reg_out_s; // @[FPArithmetic.scala 204:14]
  assign subber_io_in_a = _GEN_0[7:0]; // @[FPArithmetic.scala 42:19]
  assign subber_io_in_b = _GEN_1[7:0]; // @[FPArithmetic.scala 42:19]
  assign complement_io_in = subber_io_out_s; // @[FPArithmetic.scala 83:22]
  assign adder_io_in_a = sign_0 & ~sign_1 ? complementN_0_io_out : _GEN_8; // @[FPArithmetic.scala 127:45 128:21]
  assign adder_io_in_b = sign_1 & ~sign_0 ? complementN_1_io_out : _GEN_9; // @[FPArithmetic.scala 131:45 132:21]
  assign complementN_0_io_in = subber_io_out_c ? shifter_io_out_s : whole_frac_0; // @[FPArithmetic.scala 104:34 112:21 87:19]
  assign complementN_1_io_in = subber_io_out_c ? whole_frac_1 : shifter_io_out_s; // @[FPArithmetic.scala 104:34 88:19 122:21]
  assign shifter_io_in_a = subber_io_out_c ? whole_frac_0 : whole_frac_1; // @[FPArithmetic.scala 104:34 109:23 119:23]
  assign shifter_io_in_b = sub_exp[4:0];
  assign complementN_2_io_in = adder_io_out_s; // @[FPArithmetic.scala 144:25]
  assign leadingOneFinder_io_in = new_s & sign_0 != sign_1 ? complementN_2_io_out : adder_io_out_s; // @[FPArithmetic.scala 157:18 158:47 159:20]
  assign subber2_io_in_a = subber_io_out_c ? exp_1 : exp_0; // @[FPArithmetic.scala 104:34 105:15 115:15]
  assign subber2_io_in_b = {{3'd0}, _subber2_io_in_b_T_1}; // @[FPArithmetic.scala 167:21]
  always @(posedge clock) begin
    reg_out_s <= {_reg_out_s_T,new_out_frac}; // @[FPArithmetic.scala 202:39]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  reg_out_s = _RAND_0[31:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module FP_subber(
  input         clock,
  input  [31:0] io_in_a,
  input  [31:0] io_in_b,
  output [31:0] io_out_s
);
  wire  FP_adder_clock; // @[FPArithmetic.scala 216:26]
  wire [31:0] FP_adder_io_in_a; // @[FPArithmetic.scala 216:26]
  wire [31:0] FP_adder_io_in_b; // @[FPArithmetic.scala 216:26]
  wire [31:0] FP_adder_io_out_s; // @[FPArithmetic.scala 216:26]
  wire  _adjusted_in_b_T_1 = ~io_in_b[31]; // @[FPArithmetic.scala 219:23]
  FP_adder FP_adder ( // @[FPArithmetic.scala 216:26]
    .clock(FP_adder_clock),
    .io_in_a(FP_adder_io_in_a),
    .io_in_b(FP_adder_io_in_b),
    .io_out_s(FP_adder_io_out_s)
  );
  assign io_out_s = FP_adder_io_out_s; // @[FPArithmetic.scala 222:14]
  assign FP_adder_clock = clock;
  assign FP_adder_io_in_a = io_in_a; // @[FPArithmetic.scala 220:22]
  assign FP_adder_io_in_b = {_adjusted_in_b_T_1,io_in_b[30:0]}; // @[FPArithmetic.scala 219:39]
endmodule
module multiplier(
  input  [23:0] io_in_a,
  input  [23:0] io_in_b,
  output [47:0] io_out_s
);
  assign io_out_s = io_in_a * io_in_b; // @[Arithmetic.scala 84:23]
endmodule
module full_adder_2(
  input  [7:0] io_in_a,
  input  [7:0] io_in_b,
  output [7:0] io_out_s
);
  wire [8:0] _result_T = io_in_a + io_in_b; // @[Arithmetic.scala 58:23]
  wire [9:0] _result_T_1 = {{1'd0}, _result_T}; // @[Arithmetic.scala 58:34]
  wire [8:0] result = _result_T_1[8:0]; // @[Arithmetic.scala 57:22 58:12]
  assign io_out_s = result[7:0]; // @[Arithmetic.scala 59:23]
endmodule
module FP_multiplier(
  input         clock,
  input  [31:0] io_in_a,
  input  [31:0] io_in_b,
  output [31:0] io_out_s
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  wire [23:0] multiplier_io_in_a; // @[FPArithmetic.scala 290:28]
  wire [23:0] multiplier_io_in_b; // @[FPArithmetic.scala 290:28]
  wire [47:0] multiplier_io_out_s; // @[FPArithmetic.scala 290:28]
  wire [7:0] subber_io_in_a; // @[FPArithmetic.scala 295:24]
  wire [7:0] subber_io_in_b; // @[FPArithmetic.scala 295:24]
  wire [7:0] subber_io_out_s; // @[FPArithmetic.scala 295:24]
  wire  subber_io_out_c; // @[FPArithmetic.scala 295:24]
  wire [7:0] complementN_io_in; // @[FPArithmetic.scala 301:29]
  wire [7:0] complementN_io_out; // @[FPArithmetic.scala 301:29]
  wire [7:0] adderN_io_in_a; // @[FPArithmetic.scala 305:24]
  wire [7:0] adderN_io_in_b; // @[FPArithmetic.scala 305:24]
  wire [7:0] adderN_io_out_s; // @[FPArithmetic.scala 305:24]
  wire  s_0 = io_in_a[31]; // @[FPArithmetic.scala 255:20]
  wire  s_1 = io_in_b[31]; // @[FPArithmetic.scala 256:20]
  wire [8:0] _T_2 = 9'h100 - 9'h2; // @[FPArithmetic.scala 260:62]
  wire [8:0] _GEN_13 = {{1'd0}, io_in_a[30:23]}; // @[FPArithmetic.scala 260:34]
  wire [8:0] _GEN_0 = _GEN_13 > _T_2 ? _T_2 : {{1'd0}, io_in_a[30:23]}; // @[FPArithmetic.scala 260:68 261:14 263:14]
  wire [8:0] _GEN_14 = {{1'd0}, io_in_b[30:23]}; // @[FPArithmetic.scala 265:34]
  wire [8:0] _GEN_1 = _GEN_14 > _T_2 ? _T_2 : {{1'd0}, io_in_b[30:23]}; // @[FPArithmetic.scala 265:68 266:14 268:14]
  wire [22:0] exp_check_0 = {{15'd0}, io_in_a[30:23]}; // @[FPArithmetic.scala 271:25 272:18]
  wire [22:0] _cond_holder_T_1 = exp_check_0 + 23'h1; // @[FPArithmetic.scala 276:34]
  wire [22:0] exp_check_1 = {{15'd0}, io_in_b[30:23]}; // @[FPArithmetic.scala 271:25 273:18]
  wire [22:0] _cond_holder_T_3 = 23'h7f - exp_check_1; // @[FPArithmetic.scala 276:80]
  wire [22:0] _cond_holder_T_4 = ~_cond_holder_T_3; // @[FPArithmetic.scala 276:42]
  wire [22:0] _cond_holder_T_6 = _cond_holder_T_1 + _cond_holder_T_4; // @[FPArithmetic.scala 276:40]
  wire [22:0] frac_0 = io_in_a[22:0]; // @[FPArithmetic.scala 280:23]
  wire [22:0] frac_1 = io_in_b[22:0]; // @[FPArithmetic.scala 281:23]
  wire  new_s = s_0 ^ s_1; // @[FPArithmetic.scala 312:19]
  wire [7:0] _new_exp_T_1 = adderN_io_out_s + 8'h1; // @[FPArithmetic.scala 323:34]
  wire [22:0] _cond_holder_T_8 = exp_check_0 + 23'h2; // @[FPArithmetic.scala 325:36]
  wire [22:0] _cond_holder_T_13 = _cond_holder_T_8 + _cond_holder_T_4; // @[FPArithmetic.scala 325:42]
  wire [23:0] _new_mant_T_2 = {multiplier_io_out_s[46:24], 1'h0}; // @[FPArithmetic.scala 331:73]
  wire [7:0] _GEN_2 = multiplier_io_out_s[47] ? _new_exp_T_1 : adderN_io_out_s; // @[FPArithmetic.scala 322:60 323:15 328:15]
  wire [22:0] cond_holder = multiplier_io_out_s[47] ? _cond_holder_T_13 : _cond_holder_T_6; // @[FPArithmetic.scala 322:60 325:19 330:19]
  wire [23:0] _GEN_5 = multiplier_io_out_s[47] ? {{1'd0}, multiplier_io_out_s[46:24]} : _new_mant_T_2; // @[FPArithmetic.scala 322:60 326:16 331:16]
  reg [31:0] reg_out_s; // @[FPArithmetic.scala 333:24]
  wire [22:0] _T_12 = ~cond_holder; // @[FPArithmetic.scala 334:51]
  wire [22:0] _T_14 = 23'h1 + _T_12; // @[FPArithmetic.scala 334:49]
  wire [22:0] _GEN_15 = {{14'd0}, _T_2}; // @[FPArithmetic.scala 334:42]
  wire [8:0] _GEN_6 = cond_holder > _GEN_15 ? _T_2 : {{1'd0}, _GEN_2}; // @[FPArithmetic.scala 339:61 340:15]
  wire [8:0] _GEN_9 = _GEN_15 >= _T_14 ? 9'h1 : _GEN_6; // @[FPArithmetic.scala 334:67 335:15]
  wire [7:0] new_exp = _GEN_9[7:0]; // @[FPArithmetic.scala 315:23]
  wire [23:0] _new_mant_T_4 = 24'h800000 - 24'h1; // @[FPArithmetic.scala 341:45]
  wire [23:0] _GEN_7 = cond_holder > _GEN_15 ? _new_mant_T_4 : _GEN_5; // @[FPArithmetic.scala 339:61 341:16]
  wire [23:0] _GEN_10 = _GEN_15 >= _T_14 ? 24'h400000 : _GEN_7; // @[FPArithmetic.scala 334:67 336:16]
  wire [22:0] new_mant = _GEN_10[22:0]; // @[FPArithmetic.scala 317:24]
  wire [31:0] _reg_out_s_T_1 = {new_s,new_exp,new_mant}; // @[FPArithmetic.scala 337:37]
  wire [7:0] exp_0 = _GEN_0[7:0]; // @[FPArithmetic.scala 259:19]
  wire [7:0] exp_1 = _GEN_1[7:0]; // @[FPArithmetic.scala 259:19]
  multiplier multiplier ( // @[FPArithmetic.scala 290:28]
    .io_in_a(multiplier_io_in_a),
    .io_in_b(multiplier_io_in_b),
    .io_out_s(multiplier_io_out_s)
  );
  full_subber subber ( // @[FPArithmetic.scala 295:24]
    .io_in_a(subber_io_in_a),
    .io_in_b(subber_io_in_b),
    .io_out_s(subber_io_out_s),
    .io_out_c(subber_io_out_c)
  );
  twoscomplement complementN ( // @[FPArithmetic.scala 301:29]
    .io_in(complementN_io_in),
    .io_out(complementN_io_out)
  );
  full_adder_2 adderN ( // @[FPArithmetic.scala 305:24]
    .io_in_a(adderN_io_in_a),
    .io_in_b(adderN_io_in_b),
    .io_out_s(adderN_io_out_s)
  );
  assign io_out_s = reg_out_s; // @[FPArithmetic.scala 349:14]
  assign multiplier_io_in_a = {1'h1,frac_0}; // @[FPArithmetic.scala 285:24]
  assign multiplier_io_in_b = {1'h1,frac_1}; // @[FPArithmetic.scala 286:24]
  assign subber_io_in_a = 8'h7f; // @[FPArithmetic.scala 296:20]
  assign subber_io_in_b = _GEN_1[7:0]; // @[FPArithmetic.scala 259:19]
  assign complementN_io_in = subber_io_out_s; // @[FPArithmetic.scala 302:23]
  assign adderN_io_in_a = _GEN_0[7:0]; // @[FPArithmetic.scala 259:19]
  assign adderN_io_in_b = complementN_io_out; // @[FPArithmetic.scala 307:20]
  always @(posedge clock) begin
    if (exp_0 == 8'h0 | exp_1 == 8'h0) begin // @[FPArithmetic.scala 344:43]
      reg_out_s <= 32'h0; // @[FPArithmetic.scala 345:17]
    end else begin
      reg_out_s <= _reg_out_s_T_1; // @[FPArithmetic.scala 347:17]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  reg_out_s = _RAND_0[31:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module FPComplexMult(
  input         clock,
  input  [31:0] io_in_b_Re,
  input  [31:0] io_in_b_Im,
  output [31:0] io_out_s_Re,
  output [31:0] io_out_s_Im
);
  wire  FP_subber_clock; // @[FPComplex.scala 44:24]
  wire [31:0] FP_subber_io_in_a; // @[FPComplex.scala 44:24]
  wire [31:0] FP_subber_io_in_b; // @[FPComplex.scala 44:24]
  wire [31:0] FP_subber_io_out_s; // @[FPComplex.scala 44:24]
  wire  FP_adder_clock; // @[FPComplex.scala 45:24]
  wire [31:0] FP_adder_io_in_a; // @[FPComplex.scala 45:24]
  wire [31:0] FP_adder_io_in_b; // @[FPComplex.scala 45:24]
  wire [31:0] FP_adder_io_out_s; // @[FPComplex.scala 45:24]
  wire  FP_multiplier_clock; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_io_in_a; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_io_in_b; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_io_out_s; // @[FPComplex.scala 47:26]
  wire  FP_multiplier_1_clock; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_1_io_in_a; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_1_io_in_b; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_1_io_out_s; // @[FPComplex.scala 47:26]
  wire  FP_multiplier_2_clock; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_2_io_in_a; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_2_io_in_b; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_2_io_out_s; // @[FPComplex.scala 47:26]
  wire  FP_multiplier_3_clock; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_3_io_in_a; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_3_io_in_b; // @[FPComplex.scala 47:26]
  wire [31:0] FP_multiplier_3_io_out_s; // @[FPComplex.scala 47:26]
  FP_subber FP_subber ( // @[FPComplex.scala 44:24]
    .clock(FP_subber_clock),
    .io_in_a(FP_subber_io_in_a),
    .io_in_b(FP_subber_io_in_b),
    .io_out_s(FP_subber_io_out_s)
  );
  FP_adder FP_adder ( // @[FPComplex.scala 45:24]
    .clock(FP_adder_clock),
    .io_in_a(FP_adder_io_in_a),
    .io_in_b(FP_adder_io_in_b),
    .io_out_s(FP_adder_io_out_s)
  );
  FP_multiplier FP_multiplier ( // @[FPComplex.scala 47:26]
    .clock(FP_multiplier_clock),
    .io_in_a(FP_multiplier_io_in_a),
    .io_in_b(FP_multiplier_io_in_b),
    .io_out_s(FP_multiplier_io_out_s)
  );
  FP_multiplier FP_multiplier_1 ( // @[FPComplex.scala 47:26]
    .clock(FP_multiplier_1_clock),
    .io_in_a(FP_multiplier_1_io_in_a),
    .io_in_b(FP_multiplier_1_io_in_b),
    .io_out_s(FP_multiplier_1_io_out_s)
  );
  FP_multiplier FP_multiplier_2 ( // @[FPComplex.scala 47:26]
    .clock(FP_multiplier_2_clock),
    .io_in_a(FP_multiplier_2_io_in_a),
    .io_in_b(FP_multiplier_2_io_in_b),
    .io_out_s(FP_multiplier_2_io_out_s)
  );
  FP_multiplier FP_multiplier_3 ( // @[FPComplex.scala 47:26]
    .clock(FP_multiplier_3_clock),
    .io_in_a(FP_multiplier_3_io_in_a),
    .io_in_b(FP_multiplier_3_io_in_b),
    .io_out_s(FP_multiplier_3_io_out_s)
  );
  assign io_out_s_Re = FP_subber_io_out_s; // @[FPComplex.scala 62:17]
  assign io_out_s_Im = FP_adder_io_out_s; // @[FPComplex.scala 63:17]
  assign FP_subber_clock = clock;
  assign FP_subber_io_in_a = FP_multiplier_io_out_s; // @[FPComplex.scala 58:17]
  assign FP_subber_io_in_b = FP_multiplier_1_io_out_s; // @[FPComplex.scala 59:17]
  assign FP_adder_clock = clock;
  assign FP_adder_io_in_a = FP_multiplier_2_io_out_s; // @[FPComplex.scala 60:17]
  assign FP_adder_io_in_b = FP_multiplier_3_io_out_s; // @[FPComplex.scala 61:17]
  assign FP_multiplier_clock = clock;
  assign FP_multiplier_io_in_a = 32'hbf800000; // @[FPComplex.scala 50:28]
  assign FP_multiplier_io_in_b = io_in_b_Re; // @[FPComplex.scala 51:28]
  assign FP_multiplier_1_clock = clock;
  assign FP_multiplier_1_io_in_a = 32'ha50d3131; // @[FPComplex.scala 52:28]
  assign FP_multiplier_1_io_in_b = io_in_b_Im; // @[FPComplex.scala 53:28]
  assign FP_multiplier_2_clock = clock;
  assign FP_multiplier_2_io_in_a = 32'hbf800000; // @[FPComplex.scala 54:28]
  assign FP_multiplier_2_io_in_b = io_in_b_Im; // @[FPComplex.scala 55:28]
  assign FP_multiplier_3_clock = clock;
  assign FP_multiplier_3_io_in_a = 32'ha50d3131; // @[FPComplex.scala 56:28]
  assign FP_multiplier_3_io_in_b = io_in_b_Re; // @[FPComplex.scala 57:28]
endmodule
module FPComplexAdder(
  input         clock,
  input  [31:0] io_in_a_Re,
  input  [31:0] io_in_a_Im,
  input  [31:0] io_in_b_Re,
  input  [31:0] io_in_b_Im,
  output [31:0] io_out_s_Re,
  output [31:0] io_out_s_Im
);
  wire  FP_adder_clock; // @[FPComplex.scala 19:25]
  wire [31:0] FP_adder_io_in_a; // @[FPComplex.scala 19:25]
  wire [31:0] FP_adder_io_in_b; // @[FPComplex.scala 19:25]
  wire [31:0] FP_adder_io_out_s; // @[FPComplex.scala 19:25]
  wire  FP_adder_1_clock; // @[FPComplex.scala 19:25]
  wire [31:0] FP_adder_1_io_in_a; // @[FPComplex.scala 19:25]
  wire [31:0] FP_adder_1_io_in_b; // @[FPComplex.scala 19:25]
  wire [31:0] FP_adder_1_io_out_s; // @[FPComplex.scala 19:25]
  FP_adder FP_adder ( // @[FPComplex.scala 19:25]
    .clock(FP_adder_clock),
    .io_in_a(FP_adder_io_in_a),
    .io_in_b(FP_adder_io_in_b),
    .io_out_s(FP_adder_io_out_s)
  );
  FP_adder FP_adder_1 ( // @[FPComplex.scala 19:25]
    .clock(FP_adder_1_clock),
    .io_in_a(FP_adder_1_io_in_a),
    .io_in_b(FP_adder_1_io_in_b),
    .io_out_s(FP_adder_1_io_out_s)
  );
  assign io_out_s_Re = FP_adder_io_out_s; // @[FPComplex.scala 26:17]
  assign io_out_s_Im = FP_adder_1_io_out_s; // @[FPComplex.scala 27:17]
  assign FP_adder_clock = clock;
  assign FP_adder_io_in_a = io_in_a_Re; // @[FPComplex.scala 22:23]
  assign FP_adder_io_in_b = io_in_b_Re; // @[FPComplex.scala 23:23]
  assign FP_adder_1_clock = clock;
  assign FP_adder_1_io_in_a = io_in_a_Im; // @[FPComplex.scala 24:23]
  assign FP_adder_1_io_in_b = io_in_b_Im; // @[FPComplex.scala 25:23]
endmodule
module FPComplexMultiAdder(
  input         clock,
  input  [31:0] io_in_0_Re,
  input  [31:0] io_in_0_Im,
  input  [31:0] io_in_1_Re,
  input  [31:0] io_in_1_Im,
  output [31:0] io_out_Re,
  output [31:0] io_out_Im
);
  wire  FPComplexAdder_clock; // @[FPComplex.scala 71:28]
  wire [31:0] FPComplexAdder_io_in_a_Re; // @[FPComplex.scala 71:28]
  wire [31:0] FPComplexAdder_io_in_a_Im; // @[FPComplex.scala 71:28]
  wire [31:0] FPComplexAdder_io_in_b_Re; // @[FPComplex.scala 71:28]
  wire [31:0] FPComplexAdder_io_in_b_Im; // @[FPComplex.scala 71:28]
  wire [31:0] FPComplexAdder_io_out_s_Re; // @[FPComplex.scala 71:28]
  wire [31:0] FPComplexAdder_io_out_s_Im; // @[FPComplex.scala 71:28]
  FPComplexAdder FPComplexAdder ( // @[FPComplex.scala 71:28]
    .clock(FPComplexAdder_clock),
    .io_in_a_Re(FPComplexAdder_io_in_a_Re),
    .io_in_a_Im(FPComplexAdder_io_in_a_Im),
    .io_in_b_Re(FPComplexAdder_io_in_b_Re),
    .io_in_b_Im(FPComplexAdder_io_in_b_Im),
    .io_out_s_Re(FPComplexAdder_io_out_s_Re),
    .io_out_s_Im(FPComplexAdder_io_out_s_Im)
  );
  assign io_out_Re = FPComplexAdder_io_out_s_Re; // @[FPComplex.scala 126:12]
  assign io_out_Im = FPComplexAdder_io_out_s_Im; // @[FPComplex.scala 126:12]
  assign FPComplexAdder_clock = clock;
  assign FPComplexAdder_io_in_a_Re = io_in_0_Re; // @[FPComplex.scala 88:38]
  assign FPComplexAdder_io_in_a_Im = io_in_0_Im; // @[FPComplex.scala 88:38]
  assign FPComplexAdder_io_in_b_Re = io_in_1_Re; // @[FPComplex.scala 89:38]
  assign FPComplexAdder_io_in_b_Im = io_in_1_Im; // @[FPComplex.scala 89:38]
endmodule
module DFT_r(
  input         clock,
  input  [31:0] io_in_0_Re,
  input  [31:0] io_in_0_Im,
  input  [31:0] io_in_1_Re,
  input  [31:0] io_in_1_Im,
  output [31:0] io_out_0_Re,
  output [31:0] io_out_0_Im,
  output [31:0] io_out_1_Re,
  output [31:0] io_out_1_Im
);
  wire  FPComplexMult_clock; // @[FFTDesigns.scala 17:28]
  wire [31:0] FPComplexMult_io_in_b_Re; // @[FFTDesigns.scala 17:28]
  wire [31:0] FPComplexMult_io_in_b_Im; // @[FFTDesigns.scala 17:28]
  wire [31:0] FPComplexMult_io_out_s_Re; // @[FFTDesigns.scala 17:28]
  wire [31:0] FPComplexMult_io_out_s_Im; // @[FFTDesigns.scala 17:28]
  wire  FPComplexMultiAdder_clock; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_io_in_0_Re; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_io_in_0_Im; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_io_in_1_Re; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_io_in_1_Im; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_io_out_Re; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_io_out_Im; // @[FFTDesigns.scala 23:26]
  wire  FPComplexMultiAdder_1_clock; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_1_io_in_0_Re; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_1_io_in_0_Im; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_1_io_in_1_Re; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_1_io_in_1_Im; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_1_io_out_Re; // @[FFTDesigns.scala 23:26]
  wire [31:0] FPComplexMultiAdder_1_io_out_Im; // @[FFTDesigns.scala 23:26]
  FPComplexMult FPComplexMult ( // @[FFTDesigns.scala 17:28]
    .clock(FPComplexMult_clock),
    .io_in_b_Re(FPComplexMult_io_in_b_Re),
    .io_in_b_Im(FPComplexMult_io_in_b_Im),
    .io_out_s_Re(FPComplexMult_io_out_s_Re),
    .io_out_s_Im(FPComplexMult_io_out_s_Im)
  );
  FPComplexMultiAdder FPComplexMultiAdder ( // @[FFTDesigns.scala 23:26]
    .clock(FPComplexMultiAdder_clock),
    .io_in_0_Re(FPComplexMultiAdder_io_in_0_Re),
    .io_in_0_Im(FPComplexMultiAdder_io_in_0_Im),
    .io_in_1_Re(FPComplexMultiAdder_io_in_1_Re),
    .io_in_1_Im(FPComplexMultiAdder_io_in_1_Im),
    .io_out_Re(FPComplexMultiAdder_io_out_Re),
    .io_out_Im(FPComplexMultiAdder_io_out_Im)
  );
  FPComplexMultiAdder FPComplexMultiAdder_1 ( // @[FFTDesigns.scala 23:26]
    .clock(FPComplexMultiAdder_1_clock),
    .io_in_0_Re(FPComplexMultiAdder_1_io_in_0_Re),
    .io_in_0_Im(FPComplexMultiAdder_1_io_in_0_Im),
    .io_in_1_Re(FPComplexMultiAdder_1_io_in_1_Re),
    .io_in_1_Im(FPComplexMultiAdder_1_io_in_1_Im),
    .io_out_Re(FPComplexMultiAdder_1_io_out_Re),
    .io_out_Im(FPComplexMultiAdder_1_io_out_Im)
  );
  assign io_out_0_Re = FPComplexMultiAdder_io_out_Re; // @[FFTDesigns.scala 50:17]
  assign io_out_0_Im = FPComplexMultiAdder_io_out_Im; // @[FFTDesigns.scala 50:17]
  assign io_out_1_Re = FPComplexMultiAdder_1_io_out_Re; // @[FFTDesigns.scala 50:17]
  assign io_out_1_Im = FPComplexMultiAdder_1_io_out_Im; // @[FFTDesigns.scala 50:17]
  assign FPComplexMult_clock = clock;
  assign FPComplexMult_io_in_b_Re = io_in_1_Re; // @[FFTDesigns.scala 29:29]
  assign FPComplexMult_io_in_b_Im = io_in_1_Im; // @[FFTDesigns.scala 29:29]
  assign FPComplexMultiAdder_clock = clock;
  assign FPComplexMultiAdder_io_in_0_Re = io_in_0_Re; // @[FFTDesigns.scala 33:28 35:26]
  assign FPComplexMultiAdder_io_in_0_Im = io_in_0_Im; // @[FFTDesigns.scala 33:28 35:26]
  assign FPComplexMultiAdder_io_in_1_Re = io_in_1_Re; // @[FFTDesigns.scala 33:28 35:26]
  assign FPComplexMultiAdder_io_in_1_Im = io_in_1_Im; // @[FFTDesigns.scala 33:28 35:26]
  assign FPComplexMultiAdder_1_clock = clock;
  assign FPComplexMultiAdder_1_io_in_0_Re = io_in_0_Re; // @[FFTDesigns.scala 33:28 37:28]
  assign FPComplexMultiAdder_1_io_in_0_Im = io_in_0_Im; // @[FFTDesigns.scala 33:28 37:28]
  assign FPComplexMultiAdder_1_io_in_1_Re = FPComplexMult_io_out_s_Re; // @[FFTDesigns.scala 33:28 41:32]
  assign FPComplexMultiAdder_1_io_in_1_Im = FPComplexMult_io_out_s_Im; // @[FFTDesigns.scala 33:28 41:32]
endmodule
module PermutationsBasic(
  input  [31:0] io_in_0_Re,
  input  [31:0] io_in_0_Im,
  input  [31:0] io_in_1_Re,
  input  [31:0] io_in_1_Im,
  output [31:0] io_out_0_Re,
  output [31:0] io_out_0_Im,
  output [31:0] io_out_1_Re,
  output [31:0] io_out_1_Im
);
  assign io_out_0_Re = io_in_0_Re; // @[FFTDesigns.scala 103:17]
  assign io_out_0_Im = io_in_0_Im; // @[FFTDesigns.scala 103:17]
  assign io_out_1_Re = io_in_1_Re; // @[FFTDesigns.scala 103:17]
  assign io_out_1_Im = io_in_1_Im; // @[FFTDesigns.scala 103:17]
endmodule
module dft2(
  input         clock,
  input         reset,
  input  [31:0] io_in_0_Re,
  input  [31:0] io_in_0_Im,
  input  [31:0] io_in_1_Re,
  input  [31:0] io_in_1_Im,
  output [31:0] io_out_0_Re,
  output [31:0] io_out_0_Im,
  output [31:0] io_out_1_Re,
  output [31:0] io_out_1_Im
);
  wire  DFT_r_clock; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_in_0_Re; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_in_0_Im; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_in_1_Re; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_in_1_Im; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_out_0_Re; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_out_0_Im; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_out_1_Re; // @[FFTDesigns.scala 326:34]
  wire [31:0] DFT_r_io_out_1_Im; // @[FFTDesigns.scala 326:34]
  wire [31:0] PermutationsBasic_io_in_0_Re; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_in_0_Im; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_in_1_Re; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_in_1_Im; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_out_0_Re; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_out_0_Im; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_out_1_Re; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_io_out_1_Im; // @[FFTDesigns.scala 331:35]
  wire [31:0] PermutationsBasic_1_io_in_0_Re; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_in_0_Im; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_in_1_Re; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_in_1_Im; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_out_0_Re; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_out_0_Im; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_out_1_Re; // @[FFTDesigns.scala 333:37]
  wire [31:0] PermutationsBasic_1_io_out_1_Im; // @[FFTDesigns.scala 333:37]
  DFT_r DFT_r ( // @[FFTDesigns.scala 326:34]
    .clock(DFT_r_clock),
    .io_in_0_Re(DFT_r_io_in_0_Re),
    .io_in_0_Im(DFT_r_io_in_0_Im),
    .io_in_1_Re(DFT_r_io_in_1_Re),
    .io_in_1_Im(DFT_r_io_in_1_Im),
    .io_out_0_Re(DFT_r_io_out_0_Re),
    .io_out_0_Im(DFT_r_io_out_0_Im),
    .io_out_1_Re(DFT_r_io_out_1_Re),
    .io_out_1_Im(DFT_r_io_out_1_Im)
  );
  PermutationsBasic PermutationsBasic ( // @[FFTDesigns.scala 331:35]
    .io_in_0_Re(PermutationsBasic_io_in_0_Re),
    .io_in_0_Im(PermutationsBasic_io_in_0_Im),
    .io_in_1_Re(PermutationsBasic_io_in_1_Re),
    .io_in_1_Im(PermutationsBasic_io_in_1_Im),
    .io_out_0_Re(PermutationsBasic_io_out_0_Re),
    .io_out_0_Im(PermutationsBasic_io_out_0_Im),
    .io_out_1_Re(PermutationsBasic_io_out_1_Re),
    .io_out_1_Im(PermutationsBasic_io_out_1_Im)
  );
  PermutationsBasic PermutationsBasic_1 ( // @[FFTDesigns.scala 333:37]
    .io_in_0_Re(PermutationsBasic_1_io_in_0_Re),
    .io_in_0_Im(PermutationsBasic_1_io_in_0_Im),
    .io_in_1_Re(PermutationsBasic_1_io_in_1_Re),
    .io_in_1_Im(PermutationsBasic_1_io_in_1_Im),
    .io_out_0_Re(PermutationsBasic_1_io_out_0_Re),
    .io_out_0_Im(PermutationsBasic_1_io_out_0_Im),
    .io_out_1_Re(PermutationsBasic_1_io_out_1_Re),
    .io_out_1_Im(PermutationsBasic_1_io_out_1_Im)
  );
  assign io_out_0_Re = PermutationsBasic_1_io_out_0_Re; // @[FFTDesigns.scala 358:16]
  assign io_out_0_Im = PermutationsBasic_1_io_out_0_Im; // @[FFTDesigns.scala 358:16]
  assign io_out_1_Re = PermutationsBasic_1_io_out_1_Re; // @[FFTDesigns.scala 358:16]
  assign io_out_1_Im = PermutationsBasic_1_io_out_1_Im; // @[FFTDesigns.scala 358:16]
  assign DFT_r_clock = clock;
  assign DFT_r_io_in_0_Re = PermutationsBasic_io_out_0_Re; // @[FFTDesigns.scala 347:39]
  assign DFT_r_io_in_0_Im = PermutationsBasic_io_out_0_Im; // @[FFTDesigns.scala 347:39]
  assign DFT_r_io_in_1_Re = PermutationsBasic_io_out_1_Re; // @[FFTDesigns.scala 347:39]
  assign DFT_r_io_in_1_Im = PermutationsBasic_io_out_1_Im; // @[FFTDesigns.scala 347:39]
  assign PermutationsBasic_io_in_0_Re = io_in_0_Re; // @[FFTDesigns.scala 341:31]
  assign PermutationsBasic_io_in_0_Im = io_in_0_Im; // @[FFTDesigns.scala 341:31]
  assign PermutationsBasic_io_in_1_Re = io_in_1_Re; // @[FFTDesigns.scala 341:31]
  assign PermutationsBasic_io_in_1_Im = io_in_1_Im; // @[FFTDesigns.scala 341:31]
  assign PermutationsBasic_1_io_in_0_Re = DFT_r_io_out_0_Re; // @[FFTDesigns.scala 351:43]
  assign PermutationsBasic_1_io_in_0_Im = DFT_r_io_out_0_Im; // @[FFTDesigns.scala 351:43]
  assign PermutationsBasic_1_io_in_1_Re = DFT_r_io_out_1_Re; // @[FFTDesigns.scala 351:43]
  assign PermutationsBasic_1_io_in_1_Im = DFT_r_io_out_1_Im; // @[FFTDesigns.scala 351:43]
endmodule

