package testdata.ai;

import com.oberdiah.deepcomplexity.RequiredScore;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;

public class ShortArithmeticWrappingTest {
	// Testing short addition wrapping at maximum value
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short additionWrap(short x) {
		short max = 32767;
		return (short) (max + 1);
	}
	
	// Testing short subtraction wrapping at minimum value
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short subtractionWrap(short x) {
		short min = -32768;
		return (short) (min - 1);
	}
	
	// Testing short multiplication causing overflow wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short multiplicationWrap(short x) {
		short val = 16384;
		return (short) (val * 2);
	}
	
	// Testing short increment at boundary
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short incrementWrap(short x) {
		short val = 32767;
		val++;
		return val;
	}
	
	// Testing short decrement at boundary
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short decrementWrap(short x) {
		short val = -32768;
		val--;
		return val;
	}
	
	// Testing compound assignment addition wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short compoundAddWrap(short x) {
		short val = 32767;
		val += 2;
		return val;
	}
	
	// Testing compound assignment subtraction wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short compoundSubWrap(short x) {
		short val = -32768;
		val -= 2;
		return val;
	}
	
	// Testing double overflow wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short doubleOverflow(short x) {
		short val = 32767;
		return (short) (val + val + 2);
	}
	
	// Testing negative multiplication wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short negativeMultiplyWrap(short x) {
		short val = -16384;
		return (short) (val * 2);
	}
	
	// Testing chain of operations causing wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short chainOperationWrap(short x) {
		short val = 32765;
		val++;
		val++;
		val++;
		return val;
	}
	
	// Testing wrap in array indexing context
	public static short arrayWrap(short x) {
		short[] arr = {0, 1};
		short index = 32767;
		index += 2;
		return arr[Math.abs(index) % 2];
	}
	
	// Testing division by negative causing sign wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short divisionSignWrap(short x) {
		short val = -32768;
		return (short) (val / -2);
	}
	
	// Testing modulo with wrap values
	@ExpectedExpressionSize(8)
	public static short moduloWrap(short x) {
		short val = 32767;
		return (short) ((val + 2) % 2);
	}
	
	// Testing bitwise operations with wrapped values
	public static short bitwiseWrap(short x) {
		short val = 32767;
		val++;
		return (short) (val | 32767);
	}
	
	// Testing successive increments causing multiple wraps
	public static short multipleWrap(short x) {
		short val = 32766;
		for (int i = 0; i < 4; i++) {
			val++;
		}
		return val;
	}
	
	// Testing wrap in conditional expression
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short conditionalWrap(short x) {
		short val = 32767;
		short result = (short) (val + 1);
		return result == -32768 ? (short) 5 : (short) 0;
	}
	
	// Testing unary minus on minimum value
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short unaryMinusWrap(short x) {
		short min = -32768;
		return (short) (-min);
	}
	
	// Testing shift operations causing wrap
	public static short shiftWrap(short x) {
		short val = 32767;
		return (short) (val << 1);
	}
	
	// Testing cast from int with wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short castWrap(short x) {
		int val = 65535;
		return (short) val;
	}
	
	// Testing method parameter wrap propagation
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short parameterWrap(short x) {
		return wrapHelper((short) 32767);
	}
	
	private static short wrapHelper(short val) {
		return (short) (val + 2);
	}
}