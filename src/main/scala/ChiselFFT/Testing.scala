package ChiselFFT
import IEEEConversions.FPConvert._
import FloatingPointDesigns.FPArithmetic._
import ComplexModules.FPComplex._
import chisel3.tester.RawTester.test
import chisel3._
import chisel3.tester._
import ChiselFFT.FFTDesigns._

object Testing {
  def main(args: Array[String]): Unit = {
    test(new FPComplexAdder(32)){c=>
      c.io.in_a.Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in_a.Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
      c.io.in_b.Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in_b.Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
      c.clock.step(1)
      println(s"Real Output: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, 32)}")
    }

    test(new FPComplexMult(32)){c=>
      c.io.in_a.Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in_a.Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
      c.io.in_b.Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in_b.Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
      c.clock.step(2)
      println(s"Real Output: ${convert_long_to_float(c.io.out_s.Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out_s.Im.peek().litValue, 32)}")
    }

    test(new FPComplexMultiAdder(4,32)){c=>
      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in(0).Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in(1).Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
      c.io.in(2).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in(2).Im.poke(convert_string_to_IEEE_754("-3.5", 32).U)
      c.io.in(3).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in(3).Im.poke(convert_string_to_IEEE_754("-17.32", 32).U)
      c.clock.step(2)
      println(s"Real Output: ${convert_long_to_float(c.io.out.Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out.Im.peek().litValue, 32)}")
    }
    println("--------")
    test(new DFT_r(4, 32)){c=>
      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(2).Re.poke(convert_string_to_IEEE_754("2.9", 32).U)
      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.984", 32).U)
      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.clock.step(4)
      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
    }
    println("--------")
    test(new TwiddleFactors(4,2,2,0,32)){c=>
      c.io.in(0).Re.poke(convert_string_to_IEEE_754("12.3", 32).U)
      c.io.in(0).Im.poke(convert_string_to_IEEE_754("1.4", 32).U)
      c.io.in(1).Re.poke(convert_string_to_IEEE_754("-7.984", 32).U)
      c.io.in(1).Im.poke(convert_string_to_IEEE_754("2.4", 32).U)
      c.clock.step(2)
      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
      c.io.in(0).Re.poke(convert_string_to_IEEE_754("2.9", 32).U)
      c.io.in(0).Im.poke(convert_string_to_IEEE_754("3.4", 32).U)
      c.io.in(1).Re.poke(convert_string_to_IEEE_754("4.984", 32).U)
      c.io.in(1).Im.poke(convert_string_to_IEEE_754("4.4", 32).U)
      c.clock.step(2)
      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
    }
    println("--------")
    test(new PermutationsBasic(8,2,1,32)){c=>
      c.io.in(0).Re.poke(convert_string_to_IEEE_754("1.0", 32).U)
      c.io.in(0).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(1).Re.poke(convert_string_to_IEEE_754("2.0", 32).U)
      c.io.in(1).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(2).Re.poke(convert_string_to_IEEE_754("3.0", 32).U)
      c.io.in(2).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(3).Re.poke(convert_string_to_IEEE_754("4.0", 32).U)
      c.io.in(3).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(4).Re.poke(convert_string_to_IEEE_754("5.0", 32).U)
      c.io.in(4).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(5).Re.poke(convert_string_to_IEEE_754("6.0", 32).U)
      c.io.in(5).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(6).Re.poke(convert_string_to_IEEE_754("7.0", 32).U)
      c.io.in(6).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      c.io.in(7).Re.poke(convert_string_to_IEEE_754("8.0", 32).U)
      c.io.in(7).Im.poke(convert_string_to_IEEE_754("0", 32).U)
      println(s"Real Output: ${convert_long_to_float(c.io.out(0).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(0).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(1).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(1).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(2).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(2).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(3).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(3).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(4).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(4).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(5).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(5).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(6).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(6).Im.peek().litValue, 32)}")
      println(s"Real Output: ${convert_long_to_float(c.io.out(7).Re.peek().litValue, 32)}")
      println(s"Imaginary Output: ${convert_long_to_float(c.io.out(7).Im.peek().litValue, 32)}")
    }
  }
}