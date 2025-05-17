package testdata;

import com.github.oberdiah.deepcomplexity.GoodEnough;
import com.github.oberdiah.deepcomplexity.RequiredScore;

import static com.github.oberdiah.deepcomplexity.GoodEnough.GoodEnoughReason.GAPS_FROM_MULTIPLICATION;
import static com.github.oberdiah.deepcomplexity.GoodEnough.GoodEnoughReason.GAPS_FROM_POWERS;

public class MyTestData {
	public static short throwawayTest(short x) {
		int multiplier1 = 200;
		int multiplier2 = 300;
		
		int result2 = x * (multiplier1 + multiplier2);
		return (short) (result2);
	}
	
	@RequiredScore(1.0)
	public static short test1(short x) {
		if (x < 3) {
			return 2;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	public static short test2(short x) {
		if (x < 3) {
			return x;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short test3(short x) {
		if (x < 5 && x < 3) {
			return x;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(0.75)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short test4(short x) {
		int bar = x;
		
		if (x > 2) {
			x = (short) (-x);
			bar += x;
		}
		
		return (short) (x + bar);
	}
	
	@RequiredScore(1.0)
	public static short test5(short x) {
		if (x < 5) {
			if (x < 3) {
				return x;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short negateTest(short x) {
		return (short) (-x);
	}
	
	@RequiredScore(1.0)
	public static short negateTest2(short x) {
		return (short) (-x + x);
	}
	
	@RequiredScore(1.0)
	public static short plusTest(short x) {
		return (short) (+x);
	}
	
	@RequiredScore(1.0)
	public static short incrementTest(short x) {
		int a = x;
		int b = a++;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short incrementTest2(short x) {
		int a = x;
		int b = ++a;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short decrementTest(short x) {
		int a = x;
		int b = a--;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short decrementTest2(short x) {
		int a = x;
		int b = --a;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest(short x) {
		return (short) (x - x);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest2(short x) {
		return (short) (x * 2 - x * 2);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest3(short x) {
		return (short) ((x + 1) * 2 - (x + 1) * 2);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest4(short x) {
		int a = ((x * 2) - 1) * 2;
		int b = ((x * 2) - 1) * 2;
		return (short) (a - b);
	}
	
	@RequiredScore(0.0)
	@GoodEnough(GAPS_FROM_POWERS)
	public static short zeroTest5(short x) {
		return (short) (x * x - x * x);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest6(short x) {
		int b = x % 100;
		
		return (short) (b - b);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest7(short x) {
		int a = 0;
		if (x > 50) {
			a = 5;
		} else if (x > 20) {
			a = 2;
		} else if (x > 5) {
			a = 1;
		}
		
		int b = a + 10;
		
		return (short) (a - b);
	}
	
	
	@RequiredScore(1.0)
	public static short oneTest(short x) {
		return (short) (x + 1 - x);
	}
	
	public static short oneTest2(short x) {
		return (short) (x / x);
	}
	
	@RequiredScore(1.0)
	public static short oneTest3(short x) {
		return (short) ((short) (x + 1) - x);
	}
	
	@RequiredScore(0.5)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short simpleAdd(short x) {
		return (short) (x + x);
	}
	
	@RequiredScore(1.0)
	public static short shortTest1(short x) {
		return x;
	}
	
	@RequiredScore(1.0)
	public static short shortTest2(short x) {
		return (short) (((int) x) + 1);
	}
	
	@RequiredScore(1.0)
	public static short shortTest3(short x) {
		return (short) (x + 1);
	}
	
	@RequiredScore(1.0)
	public static short multiplyTest1(short x) {
		int a = 0;
		int b = 0;
		if (x > 0 && x < 10) {
			a = x;
			b = x;
		}
		
		return (short) (a * 2 - b * 2);
	}
	
	@RequiredScore(1.0)
	public static short barTest1(short x) {
		int bar = 0;
		
		if (x > 2) {
			x = (short) (-x);
			bar += x;
		} else {
			bar += 5;
		}
		
		return (short) (bar);
	}
	
	@RequiredScore(0.5)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short barTest2(short x) {
		int bar = 0;
		
		if (x > 2) {
			x = (short) (-x);
			bar += x;
		} else {
			bar += 5;
		}
		
		return (short) (bar * 2);
	}
	
	public static short barTest3(short x) {
		int bar = 0;
		
		if (x > 2) {
			x = (short) (-x);
			bar += x;
		} else {
			bar += 5;
		}
		
		return (short) ((bar * 2) / 2);
	}
	
	@RequiredScore(0.3334)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short largeTest1(short x) {
		int foo = 0;
		int bar = 0;
		int a = x;
		if (x < -10) {
			a = 0;
		}
		
		a += 1;
		
		if (x > 2) {
			a = (short) (x * a);
			x = (short) (-x);
			bar += x;
			foo += 3;
		} else if (a < 3) {
			foo += 4;
		} else {
			bar += 5;
		}
		
		return (short) (a * foo + bar * 2);
	}
	
	@RequiredScore(0.5)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short multiplicationTest1(short x) {
		return (short) (x * (x + 1));
	}
	
	@RequiredScore(1.0)
	public static short combinedIfs1(short x) {
		int a = x;
		if (x < 0) {
			a = 0;
		} else if (x > 10) {
			a = 10;
		}
		
		int b = x;
		if (x < 10) {
			b = 0;
		} else if (x > 20) {
			b = 10;
		}
		
		return (short) (a + b);
	}
	
	@RequiredScore(0.129)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short combinedIfs2(short x) {
		int a = x;
		if (x < 0) {
			a = 0;
		} else if (x > 10) {
			a = 10;
		}
		
		int b = x;
		if (x <= 10) {
			b = 0;
		} else if (x > 20) {
			b = 10;
		}
		
		return (short) (a * b);
	}
	
	@RequiredScore(1.0)
	public static short combinedIfs3(short x) {
		int a = x;
		if (x < 0) {
			a = 0;
		} else if (x >= 10) {
			a = 0;
		}
		
		int b = x;
		if (x < 10) {
			b = 0;
		} else if (x > 20) {
			b = 0;
		}
		
		return (short) (a * b);
	}
	
	@RequiredScore(0.129)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short combinedIfs4(short x) {
		int a = x;
		if (x < 0) {
			a = 0;
		} else if (x > 10) {
			a = 10;
		}
		
		int b = x;
		if (x < 10) {
			b = 0;
		} else if (x > 20) {
			b = 10;
		}
		
		return (short) (a * b);
	}
	
	public static short chainedConstraintTest1(short x) {
		int a = 0;
		int b = x + 5;
		
		if (x > b) {
			if (b > 5) {
				a = x;
			}
		}
		
		return (short) (a);
	}
	
	public static short chainedConstraintTest2(short x) {
		int a = 0;
		int b = x * 5;
		
		if (x > b && b > 5) {
			a = x;
		}
		
		return (short) (a);
	}
	
	public static short intelliJTest1(short x) {
		int a = 0;
		int b = 0;
		if (x > 0) {
			a = 1;
		}
		if (x * 5 > 0) {
			b = 1;
		}
		
		short v = 0;
		if (a + b > 1 && x < 0) {
			v = 1;
		}
		
		return v;
	}
	
	@RequiredScore(0.122)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short constraintTest1(short x) {
		int a = 0;
		int b = 0;
		if (x > 0 && x < 10) {
			a = x;
		}
		if (x > 0 && x < 30) {
			b = x;
		}
		
		return (short) (a * b);
	}
	
	@RequiredScore(1.0)
	public static short constraintTest2(short x) {
		if (x > -10 && x < 10) {
			return (short) (x + 32767);
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short constraintTest3(short x) {
		int a = 0;
		if (x >= 10) {
			a = x;
		}
		if (x < 20) {
			a = x + 5;
		}
		
		int b = 0;
		if (x < 10) {
			b = a;
		}
		return (short) b;
	}
	
	@RequiredScore(1.0)
	public static short constraintTest4(short x) {
		int a = 0;
		if (x > 7 || x < 2) {
			a = x;
		}
		
		int b = 0;
		if (x < 10) {
			b = a;
		}
		return (short) b;
	}
	
	@RequiredScore(1.0)
	public static short returnTest1(short x) {
		if (x > 0) {
			return x;
		}
		
		if (x > -10) {
			return -5;
		}
		
		if (x > -20) {
			return -10;
		}
		
		return -150;
	}
	
	@RequiredScore(1.0)
	public static short returnTest2(short x) {
		int a = -5;
		
		if (x > 0) {
			if (x < 20) {
				a = -10;
				return x;
			}
			a += -15;
		}
		
		return (short) a;
	}
	
	@RequiredScore(1.0)
	public static short returnTest3(short x) {
		if (x > 0) {
			if (x < 20) {
				return 5;
			}
		}
		
		int a = 0;
		if (x > 10) {
			a = 1;
		}
		
		return (short) a;
	}
	
	@RequiredScore(1.0)
	public static short returnTest4(short x) {
		if (x > 0) {
			if (x < 10) {
				return 5;
			}
		} else {
			if (x > -10) {
				return 10;
			} else {
				return 20;
			}
		}
		
		int a = 0;
		if (x > 15) {
			a = 1;
		}
		
		return (short) a;
	}
	
	@RequiredScore(1.0)
	public static short performanceTest1(short x) {
		if (x < 0) {
			return -10;
		} else if (x < 5) {
			return 42;
		} else if (x < 10) {
			return 100;
		} else {
			return (short) (x + 1);
		}
	}
	
	public static short equalityTest1(short x) {
		if (x == x) {
			return 1;
		}
		return 0;
	}
	
	public static short equalityTest2(short x) {
		if (x == x + 1) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short equalityTest3(short x) {
		if (x == 5) {
			return x;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short polyadicTest1(short x) {
		return 5 - 10 - 15 * 2;
	}
	
	@RequiredScore(1.0)
	public static short polyadicTest2(short x) {
		return 5 + 7 + 9 - 2;
	}
	
	@RequiredScore(1.0)
	public static short polyadicTest3(short x) {
		if (true && false && true) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short operationEvalOrder1(short x) {
		int j = 1;
		int i = (j = 2) * (j = 3);
		return (short) j;
	}
	
	@RequiredScore(1.0)
	public static short operationEvalOrder2(short x) {
		int j = 1;
		int i = (j = 2) * (j = 3);
		return (short) i;
	}
	
	@RequiredScore(1.0)
	public static short returnTypeCasting(short x) {
		if (x == 0) {
			int a = x;
			int b = 0;
			return (short) (a - b);
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short largeMultiplication1(short x) {
		return (short) (x * 65536);
	}
	
	@RequiredScore(0.5014)
	public static short addingVariables(short x) {
		int a = 0;
		if (x < 40) {
			a = x * 3;
		} else if (x < 50) {
			a = x * 2;
		} else {
			a = x;
		}
		
		if (a < 100) {
			return x;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short modulo1(short x) {
		return (short) (x % 100);
	}
	
	@RequiredScore(1.0)
	public static short modulo2(short x) {
		if (x >= 0) {
			return (short) (x % 100);
		}
		return 0;
	}
	
	public static short twoVars1(short x) {
		int b = x % 100;
		int a = x % 100;
		
		if (a < b && b > a) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static short twoVars2(short x) {
		int b = x % 100;
		int a = x % 100;
		
		if (b > 10 && a > b) {
			return (short) a;
		} else {
			return 0;
		}
	}
	
	public static short wrappingComparison(short x) {
		if ((short) (x - 1) > 0) {
			return x;
		} else {
			return 0;
		}
	}
	
	public static short nearlyImpossible(short x) {
		short y = (short) (x - 1);
		if (x < 0 && y > 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	public static short actuallyImpossible(short x) {
		if (x < 0 && (x - 1) > 0) {
			return 0;
		} else {
			return 1;
		}
	}
}
