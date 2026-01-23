package testdata.ai;

import com.oberdiah.deepcomplexity.RequiredScore;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;

public class LongChainedComplexConditionalTest {
	// Testing long chain of if-else with simple conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(27)
	public static short chainedIfBasic(short x) {
		int result = 0;
		if (x > 10) {
			result = 1;
		} else if (x > 5) {
			result = 2;
		} else if (x > 0) {
			result = 3;
		} else if (x > -5) {
			result = 4;
		} else {
			result = 5;
		}
		return (short) result;
	}
	
	// Testing chained if with compound conditions using AND
	@RequiredScore(1.0)
	@ExpectedExpressionSize(35)
	public static short chainedIfCompoundAnd(short x) {
		int result = 0;
		if (x > 20 && x < 25) {
			result = 1;
		} else if (x >= 10 && x <= 15) {
			result = 2;
		} else if (x > 0 && x < 5) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with compound conditions using OR
	@RequiredScore(1.0)
	@ExpectedExpressionSize(38)
	public static short chainedIfCompoundOr(short x) {
		int result = 0;
		if (x < -100 || x > 100) {
			result = 1;
		} else if (x < -50 || x > 50) {
			result = 2;
		} else if (x < -10 || x > 10) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with mixed AND/OR conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(53)
	public static short chainedIfMixedConditions(short x) {
		int result = 0;
		if ((x > 100 && x < 110) || x < -100) {
			result = 1;
		} else if ((x > 50 && x < 60) || (x > -60 && x < -50)) {
			result = 2;
		} else if (x >= 0 && x <= 20) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with negated conditions
	public static short chainedIfNegated(short x) {
		int result = 0;
		if (!(x > 0)) {
			if (!(x < -10)) {
				result = 1;
			} else if (!(x < -20)) {
				result = 2;
			} else {
				result = 3;
			}
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing nested if statements within chained if-else
	@RequiredScore(1.0)
	@ExpectedExpressionSize(40)
	public static short nestedInChained(short x) {
		int result = 0;
		if (x > 0) {
			if (x > 10) {
				if (x > 20) {
					result = 1;
				} else {
					result = 2;
				}
			} else {
				result = 3;
			}
		} else if (x < 0) {
			if (x < -10) {
				result = 4;
			} else {
				if (x < -5) {
					result = 5;
				} else {
					result = 6;
				}
			}
		} else {
			result = 7;
		}
		return (short) result;
	}
	
	// Testing chained if with variable modifications
	@RequiredScore(1.0)
	@ExpectedExpressionSize(30)
	public static short chainedIfVariableModification(short x) {
		int value = x;
		int result = 0;
		if (value > 10) {
			value += 5;
			result = value;
		} else if (value > 5) {
			value *= 2;
			result = value;
		} else if (value > 0) {
			value += 10;
			result = value;
		} else {
			result = value;
		}
		return (short) result;
	}
	
	// Testing chained if with multiple variable dependencies
	public static short chainedIfMultipleVars(short x) {
		int a = x;
		int b = x * 2;
		int result = 0;
		if (a > 10 && b > 20) {
			result = a + b;
		} else if (a > 5 && b > 10) {
			result = a * b;
		} else if (a > 0 || b > 0) {
			result = Math.abs(a - b);
		} else {
			result = a + b;
		}
		return (short) result;
	}
	
	// Testing deeply nested chained conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(32)
	public static short deeplyNestedChained(short x) {
		int result = 0;
		if (x > 0) {
			if (x > 10) {
				if (x > 20) {
					if (x > 30) {
						result = 1;
					} else {
						result = 2;
					}
				} else {
					result = 3;
				}
			} else {
				if (x > 5) {
					result = 4;
				} else {
					result = 5;
				}
			}
		} else {
			result = 6;
		}
		return (short) result;
	}
	
	// Testing chained if with method calls in conditions
	public static short chainedIfMethodCalls(short x) {
		int result = 0;
		if (isPositive(x) && isEven(x)) {
			result = 1;
		} else if (isPositive(x) && !isEven(x)) {
			result = 2;
		} else if (!isPositive(x) && isEven(x)) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	private static boolean isPositive(short x) {
		return x > 0;
	}
	
	private static boolean isEven(short x) {
		return x % 2 == 0;
	}
	
	// Testing chained if with object field conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(34)
	public static short chainedIfObjectFields(short x) {
		TestObject obj = new TestObject(x);
		int result = 0;
		if (obj.value > 20 && obj.doubled > 40) {
			result = 1;
		} else if (obj.value > 10 || obj.doubled > 20) {
			result = 2;
		} else if (obj.value >= 0) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	private static class TestObject {
		int value;
		int doubled;
		
		TestObject(int val) {
			this.value = val;
			this.doubled = val * 2;
		}
	}
	
	// Testing chained if with array access conditions
	public static short chainedIfArrayAccess(short x) {
		int[] arr = {x, x + 1, x + 2};
		int result = 0;
		if (arr[0] > 20 && arr[1] > 21) {
			result = 1;
		} else if (arr[0] > 10 && arr[2] > 12) {
			result = 2;
		} else if (arr[1] > arr[0]) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with early returns
	@RequiredScore(1.0)
	@ExpectedExpressionSize(31)
	public static short chainedIfEarlyReturn(short x) {
		if (x > 100) {
			return 1;
		} else if (x > 50) {
			return 2;
		} else if (x > 0) {
			return 3;
		} else if (x > -50) {
			return 4;
		}
		return 5;
	}
	
	// Testing chained if with complex arithmetic in conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(50)
	public static short chainedIfComplexArithmetic(short x) {
		int result = 0;
		if ((x * x + 2 * x + 1) > 100) {
			result = 1;
		} else if ((x * 2 + 5) > 25 && (x / 2) < 10) {
			result = 2;
		} else if ((x % 3) == 0 || (x % 5) == 0) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with string-like operations on numbers
	public static short chainedIfStringLikeOps(short x) {
		int abs = Math.abs(x);
		int result = 0;
		if (abs > 999) {
			result = 1;
		} else if (abs > 99) {
			result = 2;
		} else if (abs > 9) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with side effects in conditions
	public static short chainedIfSideEffects(short x) {
		Counter counter = new Counter();
		int result = 0;
		if (counter.incrementAndCheck() > 0 && x > 10) {
			result = counter.getValue() + x;
		} else if (counter.incrementAndCheck() > 1 && x > 5) {
			result = counter.getValue() * x;
		} else if (counter.incrementAndCheck() > 2) {
			result = counter.getValue() + x;
		} else {
			result = counter.getValue();
		}
		return (short) result;
	}
	
	private static class Counter {
		private int value = 0;
		
		int incrementAndCheck() {
			return ++value;
		}
		
		int getValue() {
			return value;
		}
	}
	
	// Testing chained if with exception-prone conditions
	public static short chainedIfExceptionProne(short x) {
		int result = 0;
		try {
			if (x != 0 && (100 / x) > 10) {
				result = 1;
			} else if (x != 0 && (100 / x) > 5) {
				result = 2;
			} else if (x == 0) {
				result = 3;
			} else {
				result = 4;
			}
		} catch (ArithmeticException e) {
			result = 5;
		}
		return (short) result;
	}
	
	// Testing chained if with ternary operators in conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(64)
	public static short chainedIfTernary(short x) {
		int result = 0;
		int val = x > 0 ? x : -x;
		if (val > 50 ? true : false) {
			result = 1;
		} else if ((val > 20 ? val : 0) > 0) {
			result = 2;
		} else if (val >= 0 ? true : false) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with bitwise operations in conditions
	public static short chainedIfBitwise(short x) {
		int result = 0;
		if ((x & 0xFF) > 200) {
			result = 1;
		} else if ((x | 0x0F) > 20 && (x ^ 0x05) != x) {
			result = 2;
		} else if ((x << 1) > x) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	// Testing chained if with recursive method calls in conditions
	public static short chainedIfRecursive(short x) {
		int result = 0;
		if (fibonacci(Math.abs(x) % 10) > 20) {
			result = 1;
		} else if (fibonacci(Math.abs(x) % 8) > 10) {
			result = 2;
		} else if (fibonacci(Math.abs(x) % 6) > 5) {
			result = 3;
		} else {
			result = 4;
		}
		return (short) result;
	}
	
	private static int fibonacci(int n) {
		if (n <= 1) return n;
		if (n > 10) return 100; // Prevent stack overflow
		return fibonacci(n - 1) + fibonacci(n - 2);
	}
}