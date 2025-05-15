package testdata;

import com.github.oberdiah.deepcomplexity.RequiredScore;

public class AITestData {
	// 1. Short overflow, but int is fine — subtle wrap
	@RequiredScore(1.0)
	public static short tricky1(short x) {
		if (x == 30000) return (short) (x + 10000);  // Wraps past Short.MAX_VALUE
		return 0;
	}
	
	// 2. Flip sign bit
	@RequiredScore(1.0)
	public static short tricky2(short x) {
		if (x == -32768) return (short) (-x);  // Still -32768 due to two's complement
		return 0;
	}
	
	// 3. Comparison after cast wrap
	@RequiredScore(1.0)
	public static short tricky3(short x) {
		int y = x + 40000;
		short z = (short) y;
		if (z < 0) return 1;
		return 0;
	}
	
	// 4. Multiplication causes wrap only in short
	@RequiredScore(1.0)
	public static short tricky4(short x) {
		if (x == 256) return (short) (x * 256);  // 256 * 256 = 65536 → 0 in short
		return 0;
	}
	
	// 5. Branch on overflowed result
	@RequiredScore(1.0)
	public static short tricky5(short x) {
		short y = (short) (x + 32767);
		if (y < 0) return 1;  // Will be negative only when wrapping occurs
		return 0;
	}
	
	// 6. Wrap during subtraction
	@RequiredScore(1.0)
	public static short tricky6(short x) {
		if (x == -30000) return (short) (x - 10000);  // Wraps
		return 0;
	}
	
	// 7. Control flow based on int, but returned value cast
	public static short tricky7(short x) {
		int y = x * 5000;
		if (y > 100000) return (short) y;
		return 0;
	}
	
	// 8. Double cast wrap
	@RequiredScore(1.0)
	public static short tricky8(short x) {
		int y = 70000;
		if (x == 123) return (short) ((short) y + x);  // Outer cast wraps
		return 0;
	}
	
	// 9. Overflow only on some paths
	public static short tricky9(short x) {
		int res = 1;
		if (x > 30000) res = x * x;  // Squaring overflows
		return (short) res;
	}
	
	// 10. Opposite sign wrapping check
	@RequiredScore(1.0)
	public static short trickyA(short x) {
		if (x == 16384) return (short) (x * 2);  // Exactly 32768 → wraps to -32768
		return 0;
	}
	
	// Test 1: Overflow at short boundary with conditional logic
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
	@RequiredScore(0.0) // We know we'll not manage this until we've got even/odd/modulo
	public static short multiplicationCastTrap(short x) {
		int temp = x * 32768; // Intentionally outside short range
		return (short) temp;  // This truncates in an interesting way
	}
	
	// Test 3: Boundary value testing with cascading conditions
	public static short boundaryValueCascade(short x) {
		if (x == Short.MAX_VALUE) {
			return -10;
		} else if (x == Short.MAX_VALUE - 1) {
			return 42;
		} else if (x > Short.MAX_VALUE - 10) {
			return 100;
		} else {
			return (short) (x + 1);
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
	public static short incrementalWrapping(short x) {
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
	public static short multiplyAndCompare(short x) {
		int doubled = x * 2;
		
		if (doubled > Short.MAX_VALUE || doubled < Short.MIN_VALUE) {
			return -1;
		} else if (doubled == Short.MAX_VALUE) {
			return 1;
		} else {
			return (short) doubled;
		}
	}
	
	// Test 9: Compound operation with underflow potential
	public static short compoundUnderflow(short x) {
		short y = (short) (x - 1);
		
		if (x < 0 && y > 0) {
			// Underflow occurred
			return Short.MIN_VALUE;
		} else {
			return (short) (y * 2);
		}
	}
	
	// Test 10: Mixed arithmetic with potential intermediate overflow
	public static short mixedArithmeticOverflow(short x) {
		int temp = x;
		
		if (x > 0) {
			temp = x * x; // Can overflow int for large x
			
			if (temp < 0) {
				// Integer overflow occurred
				return Short.MAX_VALUE;
			} else if (temp > Short.MAX_VALUE) {
				return (short) (temp % 10000);
			}
		} else {
			temp = x * x * -1; // Different behavior for negative
		}
		
		return (short) temp;
	}
}
