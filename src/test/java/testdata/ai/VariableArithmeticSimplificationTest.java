package testdata.ai;

public class VariableArithmeticSimplificationTest {
	// Testing simple subtraction x - x should equal 0
	public static short selfSubtraction(short x) {
		return (short) (x - x);
	}
	
	// Testing 2x - x should equal x
	public static short doubleMinusX(short x) {
		return (short) (2 * x - x);
	}
	
	// Testing x + x - x should equal x
	public static short addSubSame(short x) {
		return (short) (x + x - x);
	}
	
	// Testing x - x + x should equal x
	public static short subAddSame(short x) {
		return (short) (x - x + x);
	}
	
	// Testing 3x - 2x should equal x
	public static short threeMinusTwoX(short x) {
		return (short) (3 * x - 2 * x);
	}
	
	// Testing x * 2 / 2 should equal x (for non-zero x)
	public static short multiplyDivide(short x) {
		if (x == 0) return 0;
		return (short) (x * 2 / 2);
	}
	
	// Testing (x + 5) - 5 should equal x
	public static short addSubConstant(short x) {
		return (short) ((x + 5) - 5);
	}
	
	// Testing x + 0 should equal x
	public static short addZero(short x) {
		return (short) (x + 0);
	}
	
	// Testing x * 1 should equal x
	public static short multiplyOne(short x) {
		return (short) (x * 1);
	}
	
	// Testing x / 1 should equal x
	public static short divideOne(short x) {
		return (short) (x / 1);
	}
	
	// Testing -(-x) should equal x
	public static short doubleNegation(short x) {
		return (short) (-(-x));
	}
	
	// Testing x + x - 2*x should equal 0
	public static short complexZero(short x) {
		return (short) (x + x - 2 * x);
	}
	
	// Testing variable aliasing with arithmetic
	public static short aliasingArithmetic(short x) {
		int a = x;
		int b = a;
		return (short) (a - b);
	}
	
	// Testing x * 0 should equal 0
	public static short multiplyZero(short x) {
		return (short) (x * 0);
	}
	
	// Testing 0 - x + x should equal 0
	public static short zeroMinusAdd(short x) {
		return (short) (0 - x + x);
	}
	
	// Testing (x - 1) + 1 should equal x
	public static short subAddConstant(short x) {
		return (short) ((x - 1) + 1);
	}
	
	// Testing x ^ x should equal 0 (XOR)
	public static short xorSelf(short x) {
		return (short) (x ^ x);
	}
	
	// Testing x & x should equal x (bitwise AND)
	public static short andSelf(short x) {
		return (short) (x & x);
	}
	
	// Testing x | 0 should equal x (bitwise OR)
	public static short orZero(short x) {
		return (short) (x | 0);
	}
	
	// Testing complex expression that should simplify to x
	public static short complexSimplification(short x) {
		return (short) (x + x + x - x - x);
	}
}
                    