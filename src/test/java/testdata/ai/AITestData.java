package testdata.ai;

import com.oberdiah.deepcomplexity.GoodEnough;
import com.oberdiah.deepcomplexity.RequiredScore;

import static com.oberdiah.deepcomplexity.GoodEnough.GoodEnoughReason.GAPS_FROM_MULTIPLICATION;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;

public class AITestData {
	// 1. Short overflow, but int is fine — subtle wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short tricky1(short x) {
		if (x == 30000) return (short) (x + 10000);  // Wraps past MAX_SHORT
		return 0;
	}
	
	// 2. Flip sign bit
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short tricky2(short x) {
		if (x == -32768) return (short) (-x);  // Still -32768 due to two's complement
		return 0;
	}
	
	// 3. Comparison after cast wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
	public static short tricky3(short x) {
		int y = x + 40000;
		short z = (short) y;
		if (z < 0) return 1;
		return 0;
	}
	
	// 4. Multiplication causes wrap only in short
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short tricky4(short x) {
		if (x == 256) return (short) (x * 256);  // 256 * 256 = 65536 → 0 in short
		return 0;
	}
	
	// 5. Branch on overflowed result
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
	public static short tricky5(short x) {
		short y = (short) (x + 32767);
		if (y < 0) return 1;  // Will be negative only when wrapping occurs
		return 0;
	}
	
	// 6. Wrap during subtraction
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
	public static short tricky6(short x) {
		if (x == -30000) return (short) (x - 10000);  // Wraps
		return 0;
	}
	
	// 7. Control flow based on int, but returned value cast
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short tricky7(short x) {
		int y = x * 5000;
		if (y > 100000) return 1;
		return 0;
	}
	
	// 8. Double cast wrap
	@RequiredScore(1.0)
	@ExpectedExpressionSize(14)
	public static short tricky8(short x) {
		int y = 70000;
		if (x == 123) return (short) ((short) y + x);  // Outer cast wraps
		return 0;
	}
	
	// 10. Opposite sign wrapping check
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short tricky9(short x) {
		if (x == 16384) return (short) (x * 2);  // Exactly 32768 → wraps to -32768
		return 0;
	}
	
	// Test 1: Overflow at short boundary with conditional logic
	@RequiredScore(1.0)
	@ExpectedExpressionSize(15)
	public static short overflowWithCondition(short x) {
		short result = 0;
		
		if (x > 0) {
			// This will overflow for large positive x values
			result = (short) (x + 32767);
		} else {
			// Different behavior for negative x
			result = (short) (x - 100);
		}
		
		return result;
	}
	
	// Test 2: Unexpected behaviour when casting after multiplication
	@RequiredScore(0.0)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	@ExpectedExpressionSize(5)
	public static short multiplicationCastTrap(short x) {
		int temp = x * 32768; // Intentionally outside short range
		return (short) temp;  // This truncates in an interesting way
	}
	
	// Test 3: Boundary value testing with cascading conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(28)
	public static short boundaryValueCascade(short x) {
		if (x == 32767) {
			return -10;
		} else if (x == 32767 - 1) {
			return 42;
		} else if (x > 32767 - 10) {
			return 100;
		} else {
			return 200;
		}
	}
	
	// Test 4: Bit manipulation with sign issues
	public static short bitFlipWraparound(short x) {
		short result = (short) ~x; // Bit flip
		
		if (result < 0) {
			result = (short) (result & 0x7FFF); // Clear sign bit
		} else {
			result = (short) (result | 0x8000); // Set sign bit
		}
		
		return result;
	}
	
	// Test 5: Division with potential near-zero divisor
	public static short divisionEdgeCase(short x) {
		short result;
		
		if (x >= -5 && x <= 5) {
			// Near-zero case handled specially
			result = (short) (10000 / (x + 10));
		} else {
			// Normal case
			result = (short) (x / 2);
		}
		
		return result;
	}
	
	// Test 6: Multiple casts between different types
	public static short multipleCastingTrap(short x) {
		float f = x;
		double d = f * 1.5;
		int i = (int) d;
		
		if (i > 20000) {
			return (short) (i - 5000);
		} else {
			return (short) i;
		}
	}
	
	// Test 7: Short value wrapping during incremental operations
	public static short forLoopWrapping(short x) {
		short count = 0;
		
		// For values near MAX_VALUE, this loop behaves unexpectedly
		// due to wrapping around to negative after increment
		for (short i = 0; i < 10 && x > 32700; i++) {
			x++;
			count = (short) (count + 1000);
		}
		
		return (short) (x + count);
	}
	
	// Test 8: Combination of multiplication and conditional boundary
	@RequiredScore(1.0)
	@ExpectedExpressionSize(29)
	public static short multiplyAndCompare(short x) {
		int doubled = x * 2;
		
		if (doubled > 32768 || doubled < -32768) {
			return -1;
		} else if (doubled == 32768) {
			return 1;
		} else {
			return 0;
		}
	}
	
	// Test 10: Mixed arithmetic with potential intermediate overflow
	@RequiredScore(1.0)
	@ExpectedExpressionSize(70)
	public static short mixedArithmeticOverflow(short x) {
		if (x > 120) {
			return 0;
		}
		
		int temp = x;
		
		if (x > 0) {
			temp = x * x; // Can overflow int for large x
			
			if (temp < 0) {
				// Integer overflow occurred
				return 32767;
			} else if (temp > 32767) {
				return (short) (temp % 10000);
			}
		} else {
			temp = x * -1; // Different behavior for negative
		}
		
		return (short) temp;
	}
	
	
	// Test 12: Casting with potential overflow in multipliers - This test passed
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short testMultiplierEquivalence(short x) {
		// This test creates a scenario where individual multipliers might overflow
		// when cast to short, but the combined result might not (or vice versa)
		int multiplier1 = 200;
		int multiplier2 = 300;
		
		// Different ways to compute the same result
		int result1 = (x * multiplier1) + (x * multiplier2);  // Separate multiplications
		int result2 = x * (multiplier1 + multiplier2);        // Combined multiplier
		
		// These should be mathematically equivalent, but might not be
		// if casting is done on the multipliers rather than the base case
		return (short) (result1 - result2);
	}
	
	// Test 13: Edge case with maximum short value
	@RequiredScore(1.0)
	@ExpectedExpressionSize(18)
	public static short testMaxShortEquivalence(short x) {
		if (x == 32767) {
			// This creates a scenario where casting the multiplier vs. the result
			// might lead to different outcomes due to overflow handling
			int a = x * 2;    // Overflows in int calculation
			int b = x + x;    // Different way to calculate same value
			
			// If casting is done on the multipliers, a and b might be treated differently
			return (short) (a - b);
		}
		return 0;
	}
	
	// Test 14: Multiple multipliers with wrapping behavior
	@RequiredScore(1.0)
	@ExpectedExpressionSize(36)
	public static short testMultipleWrapping(short x) {
		if (x > 100 && x < 200) {
			// Create multiple multipliers that might wrap differently
			// when cast individually vs. together
			int m1 = x * 400;
			int m2 = x * 500;
			int m3 = x * 600;
			
			// Different ways to combine the multipliers
			int combined1 = m1 + m2 + m3;
			int combined2 = x * (400 + 500 + 600);
			
			// If casting is done on the multipliers rather than the base case,
			// these might produce different results
			return (short) (combined1 - combined2);
		}
		return 0;
	}
	
	// Test 15: Casting with negative multipliers
	@RequiredScore(1.0)
	@ExpectedExpressionSize(26)
	public static short testNegativeEquivalence(short x) {
		if (x < 0) {
			// Negative values might be handled differently when casting
			int m1 = x * -300;
			int m2 = x * 500;
			
			// Different ways to combine the multipliers
			int combined1 = m1 + m2;
			int combined2 = x * (-300 + 500);
			
			// If casting is done incorrectly, these might produce different results
			return (short) (combined1 - combined2);
		}
		return 0;
	}
}