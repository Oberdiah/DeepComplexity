package testdata;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;
import com.oberdiah.deepcomplexity.GoodEnough;
import com.oberdiah.deepcomplexity.RequiredScore;

import static com.oberdiah.deepcomplexity.GoodEnough.GoodEnoughReason.*;

@SuppressWarnings("ALL")
public class MyTestData {
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short test1(short x) {
		if (x < 3) {
			return 2;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short test2(short x) {
		if (x < 3) {
			return x;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short test3(short x) {
		if (x < 5 && x < 3) {
			return x;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(0.75)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	@ExpectedExpressionSize(13)
	public static short test4(short x) {
		int bar = x;
		
		if (x > 2) {
			x = (short) (-x);
			bar += x;
		}
		
		return (short) (x + bar);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
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
	@ExpectedExpressionSize(9)
	public static short test6(short x) {
		if (x > x + 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short test7(short x) {
		int i = 0;
		if (x > 5) {
			int k = x % 8;
			i = k % 4;
		}
		
		return (short) i;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short test8(short x) {
		int q = 0 + 0;
		if (q++ > 0) {
			return (short) q;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short test9(short x) {
		int q = 0;
		if (++q > 0) {
			return (short) q;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short test10(short x) {
		boolean b = true;
		if (b = false == true) {
			return 1;
		}
		return (short) (b ? 2 : 3);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(19)
	public static short test11(short x) {
		int q = 0 + 0;
		if ((q++ > 0) && (q++ > 1) && (q++ > 2)) {
			return (short) q;
		}
		return (short) (q + 100);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short negateTest(short x) {
		return (short) (-x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short negateTest2(short x) {
		return (short) (-x + x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(3)
	public static short plusTest(short x) {
		return (short) (+x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short incrementTest(short x) {
		int a = x;
		int b = a++;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short incrementTest2(short x) {
		int a = x;
		int b = ++a;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short incrementTest3(short x) {
		int a = 0;
		a++;
		a++;
		a++;
		a++;
		return (short) a;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short decrementTest(short x) {
		int a = x;
		int b = a--;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short decrementTest2(short x) {
		int a = x;
		int b = --a;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short zeroTest(short x) {
		return (short) (x - x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short zeroTest2(short x) {
		return (short) (x * 2 - x * 2);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short zeroTest3(short x) {
		return (short) ((x + 1) * 2 - (x + 1) * 2);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short zeroTest4(short x) {
		int a = ((x * 2) - 1) * 2;
		int b = ((x * 2) - 1) * 2;
		return (short) (a - b);
	}
	
	@RequiredScore(0.0)
	@GoodEnough(GAPS_FROM_POWERS)
	@ExpectedExpressionSize(5)
	public static short zeroTest5(short x) {
		return (short) (x * x - x * x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short zeroTest6(short x) {
		int b = x % 100;
		
		return (short) (b - b);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(18)
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
	@ExpectedExpressionSize(14)
	public static short zeroTest8(short x) {
		int b = 0;
		if (x > 50) {
			b = x % 100;
		} else if (x > 25) {
			b = 3;
		}
		
		return (short) (b - b);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short oneTest(short x) {
		return (short) (x + 1 - x);
	}
	
	@ExpectedExpressionSize(4)
	public static short oneTest2(short x) {
		return (short) (x / x);
	}
	
	@ExpectedExpressionSize(8)
	public static short oneTest3(short x) {
		return (short) ((short) (x + 1) - x);
	}
	
	@RequiredScore(0.5)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	@ExpectedExpressionSize(4)
	public static short simpleAdd(short x) {
		return (short) (x + x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(1)
	public static short shortTest1(short x) {
		return x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short shortTest2(short x) {
		return (short) (((int) x) + 1);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short shortTest3(short x) {
		return (short) (x + 1);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short multiplyTest1(short x) {
		int a = 0;
		int b = 0;
		if (x > 0 && x < 10) {
			a = x;
			b = x;
		}
		
		return (short) (a * 2 - b * 2);
	}
	
	@RequiredScore(0.5)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	@ExpectedExpressionSize(11)
	public static short multiplyTest2(short x) {
		int a = x * 2;
		if (a == 7) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
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
	@ExpectedExpressionSize(14)
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
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(15)
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
	@ExpectedExpressionSize(34)
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
	@ExpectedExpressionSize(6)
	public static short multiplicationTest1(short x) {
		return (short) (x * (x + 1));
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(15)
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
	@ExpectedExpressionSize(15)
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
	@ExpectedExpressionSize(15)
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
	@ExpectedExpressionSize(15)
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
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
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
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short chainedConstraintTest2(short x) {
		int a = 0;
		int b = x * 5;
		
		if (x > b && b > 5) {
			a = x;
		}
		
		return (short) (a);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short chainedConstraintTest3(short x) {
		int a = 0;
		
		if (x > x * 5 && x > 5) {
			a = x;
		}
		
		return (short) (a);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
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
	@ExpectedExpressionSize(14)
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
	@ExpectedExpressionSize(13)
	public static short constraintTest2(short x) {
		if (x > -10 && x < 10) {
			return (short) (x + 32767);
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(14)
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
	@ExpectedExpressionSize(13)
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
	@ExpectedExpressionSize(12)
	public static short constraintTest5(short x) {
		int c = 0;
		if (x == 7) {
			c = 5;
		}
		
		if (x != 7) {
			return (short) c;
		}
		
		return (short) 10;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(20)
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
	@ExpectedExpressionSize(15)
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
	@ExpectedExpressionSize(15)
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
	@ExpectedExpressionSize(21)
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
	@ExpectedExpressionSize(20)
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
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short equalityTest1(short x) {
		if (x == x) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short equalityTest2(short x) {
		if (x == x + 1) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short equalityTest3(short x) {
		if (x == 5) {
			return x;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short polyadicTest1(short x) {
		return 5 - 10 - 15 * 2;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short polyadicTest2(short x) {
		return 5 + 7 + 9 - 2;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short polyadicTest3(short x) {
		if (true && false && true) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short operationEvalOrder1(short x) {
		int j = 1;
		int i = (j = 2) * (j = 3);
		return (short) j;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short operationEvalOrder2(short x) {
		int j = 1;
		int i = (j = 2) * (j = 3);
		return (short) i;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short returnTypeCasting(short x) {
		if (x == 0) {
			int a = x;
			int b = 0;
			return (short) (a - b);
		}
		return 0;
	}
	
	@ExpectedExpressionSize(5)
	public static short largeMultiplication1(short x) {
		return (short) (x * 65536);
	}
	
	@ExpectedExpressionSize(19)
	@RequiredScore(1.0)
	public static short addingVariables(short x) {
		int a = 0;
		if (x < 40) {
			a = x - 10;
		} else if (x < 50) {
			a = x - 5;
		} else {
			a = x;
		}
		
		if (a < 100 && a > 0) {
			return x;
		}
		return 0;
	}
	
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(22)
	public static short challenge1(short x) {
		int a = 0;
		
		if (x > 2 && x < 10) {
			a = x;
		}
		
		int c = 0;
		if (a <= 0 || a >= 10) {
			c = 1;
		}
		
		if (x == 5 && c == 1) {
			return 20;
		}
		
		return (short) 1;
	}
	
	@ExpectedExpressionSize(10)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	@RequiredScore(0.5)
	public static short challenge2(short x) {
		int a = 0;
		
		if (x > 2) {
			a = x;
		}
		
		int b = 0;
		if (a > 0) {
			b = a;
		}
		
		return (short) (a + b);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(25)
	public static short challenge3(short x) {
		int a = x % 100;
		int b = x % 99;
		int c = 0;
		if (a > 5 || (a > 3 && b > 15)) {
			c = 8;
		} else {
			c = 2;
		}
		
		if (a > 5) {
			return (short) c;
		}
		
		if (a > 3 && b > 15) {
			return (short) (c + 1);
		}
		
		return -5;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short modulo1(short x) {
		return (short) (x % 100);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short modulo2(short x) {
		if (x >= 0) {
			return (short) (x % 100);
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short modulo3(short x) {
		int a = x % 100;
		if (a > a + 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@GoodEnough(REQUIRES_KNOWLEDGE_OF_MODULUS)
	@RequiredScore(0.8824)
	@ExpectedExpressionSize(10)
	public static short modulo4(short x) {
		int a = x % 8;
		if (x == 5) {
			a = x % 10;
		}
		
		return (short) a;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short modulo5(short x) {
		int a = x % 100;
		if (x < 100 || x > 100) {
			a = x % 90;
		}
		
		int b = a;
		int c = a + 0;
		if (b > c) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@ExpectedExpressionSize(13)
	@RequiredScore(1.0)
	public static short twoVars1(short x) {
		int b = x % 100;
		int a = x % 100 + 0;
		
		if (a < b && a > b) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short twoVars2(short x) {
		int b = x % 100;
		int a = x % 99;
		
		if (a > b) {
			return (short) a;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short twoVars3(short x) {
		int b = x % 100;
		int a = x % 99;
		
		if (a > 10 && a < 20 && b == a) {
			return (short) b;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short twoVars4(short x) {
		int b = x % 100;
		int a = x % 90;
		
		if (a > 10 && a < 20) {
			if (b == a) {
				return (short) b;
			}
		}
		return 0;
	}
	
	@ExpectedExpressionSize(17)
	public static short twoVars5(short x) {
		int b = x % 99;
		int a = x % 100;
		
		if (b == a && a > 10 && a < 20) {
			return (short) b;
		} else {
			return 0;
		}
	}
	
	@ExpectedExpressionSize(17)
	public static short twoVars6(short x) {
		int b = x % 100;
		int a = x % 90;
		
		if (b == a) {
			if (a > 10 && a < 20) {
				return (short) b;
			}
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short twoVars7(short x) {
		int b = x % 100;
		int a = x % 100 + 0;
		
		if (a > b) {
			return (short) a;
		} else {
			return 0;
		}
	}
	
	@ExpectedExpressionSize(10)
	public static short wrappingComparison(short x) {
		if ((short) (x - 1) > 0) {
			return x;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
	public static short nearlyImpossible(short x) {
		short y = (short) (x - 1);
		if (x < 0 && y > 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short alsoNearlyImpossible(short x) {
		int a = x * -65536 - 65535;
		if (a < 0 && a - 1 > 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short actuallyImpossible(short x) {
		if (x < 0 && (x - 1) > 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(21)
	public static short valueCascade(short x) {
		if (x == 5) {
			return -10;
		} else if (x == 5 - 1) {
			return 42;
		} else if (x > 5 - 10) {
			return 100;
		} else {
			return 200;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short simpleValueCheck(short x) {
		if (x > (5 - 10)) {
			return 100;
		} else {
			return 200;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short simpleMethodTest1(short x) {
		return (short) simplePrivateMethod(x - 10, x);
	}
	
	private static int simplePrivateMethod(int x, int b) {
		if (x > 0 && x < 10) {
			return b;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(20)
	public static short simpleMethodTest2(short x) {
		if (x == 5) {
			return 0;
		}
		
		short q = simpleMethod2(x);
		
		if (q == 5) {
			return 1;
		}
		
		return (short) (q + 2);
	}
	
	private static short simpleMethod2(short p) {
		if (p > 0) {
			return 5;
		} else {
			return 4;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(14)
	public static short simpleMethodTest3(short x) {
		if (x == 5) {
			return -6;
		}
		
		return simpleMethod2(x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(15)
	public static short simpleMethodTest4(short x) {
		if (x == 5) {
			return -6;
		}
		
		return (short) simplePrivateMethod(x, 5);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short simpleMethodTest5(short x) {
		if (x == 5) {
			return -6;
		}
		
		short unused = simpleMethod2(x);
		
		return 9;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short simpleMethodTest6(short x) {
		int b = x;
		b++;
		return (short) simplePrivateMethod(b, x);
	}
	
	@ExpectedExpressionSize(2)
	public static short simpleClassTest0(short x) {
		MyClass nested = new MyClass();
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest1(short x) {
		MyClass nested = new MyClass(2);
		nested.x = 1;
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest2(short x) {
		MyClass nested = new MyClass(3);
		nested.x = 1;
		if (nested.x == 1) {
			return 2;
		}
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest3(short x) {
		MyClass nested = new MyClass(3);
		if (nested.x == 3) {
			return 2;
		}
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest4(short x) {
		MyClass nested = new MyClass(2);
		updateClassField(nested);
		return (short) nested.x;
	}
	
	private static void updateClassField(MyClass nested) {
		nested.x = 5;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short simpleClassTest5(short x) {
		MyClass nested = new MyClass(2);
		nested.addOne();
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(3)
	public static short simpleClassTest6(short x) {
		MyClass nested = new MyClass(2);
		return (short) (nested.getX() - nested.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short simpleClassTest7(short x) {
		MyClass nested = new MyClass(2);
		nested.addOne();
		if (nested.getX() == 3) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short simpleClassTest8(short x) {
		MyClass nested = new MyClass(2);
		nested.addOne();
		int v = nested.x;
		nested.addOne();
		return (short) (v + nested.getX());
	}
	
	@ExpectedExpressionSize(8)
	public static short simpleClassTest9(short x) {
		MyClass nested = new MyClass();
		if (nested.getX() == 1000) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest10(short x) {
		MyClass nested = new MyClass(50);
		if (x == 100) {
			nested = new MyClass(100);
		}
		nested.x = 5;
		return (short) nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short simpleClassTest11(short x) {
		MyClass nested = new MyClass(50).addOne();
		return (short) nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short simpleClassTest12(short x) {
		return (short) new MyClass(50).addOne().addOne().addOne().getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short simpleClassTest13(short x) {
		MyClass nested = new MyClass(50);
		MyNestingClass nesting = new MyNestingClass(nested);
		return (short) nesting.nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short simpleClassTest14(short x) {
		MyClass nested = new MyClass(50);
		MyNestingClass nesting = new MyNestingClass(nested);
		nesting.nested.x++;
		nesting.nested.addOne();
		return (short) nesting.nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest15(short x) {
		int nested = new MyClass(50).x = 5;
		return (short) nested;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short simpleClassTest16(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		int p = ((x > 0) ? a : b).x;
		return (short) p;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short simpleClassTest17(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		((x > 2) ? a : b).x = 5;
		return (short) ((x > 0) ? a : b).x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short simpleClassTest18(short x) {
		if (x <= 0) {
			return 0;
		}
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		((x > 0) ? a : b).x = 5;
		return (short) a.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest19(short x) {
		MyClass a = new MyClass(1);
		MyClass b = makeNewClass(2);
		if (x <= 0) {
			b = a;
		}
		b.x = 0;
		return (short) b.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest20(short x) {
		MyClass a = new MyClass(1);
		MyClass b = a;
		b.x = 0;
		return (short) a.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short simpleClassTest21(short x) {
		MyClass a = new MyClass(1);
		MyClass b = makeNewClass(2);
		if (x <= 0) {
			b = a;
		}
		b.x = 0;
		
		return (short) a.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short simpleClassTest22(short x) {
		int p = 0;
		
		if (x == 0) {
			x++;
			
			int q = (x == 2 ? new MyClass(p++) : new MyClass(-10)).x;
			
			return (short) p;
		}
		
		return -5;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short simpleClassTest23(short x) {
		MyClass a = new MyClass(1);
		MyClass b = makeNewClass(2);
		
		MyClass c = new MyClass(100);
		
		if (x > 0) {
			c = a;
		} else {
			c = b;
		}
		
		c.x = 0;
		
		return (short) (a.x + b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short simpleClassTest24(short x) {
		MyClass c = makeNewClass(100);
		
		MyClass a = new MyClass(1);
		MyClass b = makeNewClass(10);
		
		if (x > 0) {
			c = new MyClass(100);
			a = c;
		} else {
			c = new MyClass(1000);
			b = c;
		}
		
		return (short) (a.x + b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short simpleClassTest25(short x) {
		MyClass b = makeNewClass(10);
		
		if (x > 0) {
			b.x = 0;
		}
		
		return (short) (b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short simpleClassTest26(short x) {
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			b.x++;
		}
		
		return (short) (b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short simpleClassTest27(short x) {
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			b.x *= -1;
			b.maybeAdd();
		}
		
		b.maybeAdd();
		
		return (short) (b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short simpleClassTest28(short x) {
		MyClass b = new MyClass(10);
		b = makeNewClass(b.addOne().getX() + 1);
		
		return (short) (b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest29(short x) {
		MyClass nested = new MyClass(50);
		if (x == 100) {
			nested = makeNewClass(100);
		}
		nested.x = 5;
		return (short) nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(21)
	public static short simpleClassTest30(short x) {
		MyClass c = makeNewClass(100);
		
		MyClass a = new MyClass(1);
		MyClass b = makeNewClass(10);
		b.maybeAdd();
		
		if (x > 0) {
			c = new MyClass(100);
			a = c;
			a.maybeAdd();
		} else {
			c = new MyClass(1000);
			b = c;
		}
		
		b.maybeAdd();
		return (short) (a.x + b.x);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short simpleClassTest31(short x) {
		MyClass c = new MyClass(100);
		
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			a = c;
		} else {
			b = c;
		}
		
		b.maybeAdd();
		return (short) a.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest32(short x) {
		MyClass myClass = new MyClass(50);
		MyNestingClass nesting = new MyNestingClass(myClass);
		updateNesting(nesting, 5);
		return (short) nesting.nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest33(short x) {
		MyClass myClass = new MyClass(50);
		MyNestingClass nesting = new MyNestingClass(myClass);
		MyDoubleNestingClass doubleNesting = new MyDoubleNestingClass(nesting);
		updateDoubleNesting(doubleNesting, 5);
		return (short) doubleNesting.doubleNested.nested.getX();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short simpleClassTest34(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		if (x > 0) {
			if (x > 2) {
				a = b;
			}
			return (short) a.x;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short simpleClassTest35(short x) {
		MyClass q = new MyClass(2);
		MyClass a = (x > 0) ? new MyClass(3) : q;
		
		a.x = 5;
		
		return (x > 0) ? 0 : (short) q.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short simpleClassTest36(short x) {
		MyClass obj = new MyClass(1);
		MyClass a = obj;
		MyClass b = obj;
		if (x == x) {
			a.x++;
			b.x++;
		}
		return (short) obj.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short simpleClassTest37(short x) {
		MyClass obj = new MyClass(1);
		if (x == x) {
			MyClass a = obj;
			MyClass b = obj;
			if (x == x) {
				a.x++;
				b.x++;
				a.x++;
				a.x++;
				a.x++;
			}
		}
		return (short) obj.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short simpleClassTest38(short x) {
		MyClass obj = new MyClass(1);
		if (x == x) {
			MyClass a = obj;
			MyClass b = obj;
			if (x == x) {
				a.x = 1;
				b.x = 2;
				a.x = 3;
				
				if (b.x == 2) {
					return 5;
				}
			}
		}
		return (short) obj.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short simpleClassTest39(short x) {
		MyClass obj1 = new MyClass(1);
		MyClass obj2 = obj1;
		if (x > 0) {
			obj1.x++;
			((x > 2) ? obj2 : obj2).x++;
			return (short) obj1.x;
		}
		
		return -5;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short simpleClassTest40(short x) {
		MyClass foo = new MyClass(50);
		if (x > 100) {
			if (x > 200) {
				foo = new MyClass(100);
			}
		}
		return (short) foo.getX();
	}
	
	private static void updateNesting(MyNestingClass nesting, int val) {
		nesting.nested.x = val;
	}
	
	private static void updateDoubleNesting(MyDoubleNestingClass nesting, int val) {
		nesting.doubleNested.nested.x = val;
	}
	
	private static MyClass makeNewClass(int xVal) {
		return new MyClass(xVal);
	}
	
	public static class MyClass {
		public int x = 1000;
		
		public MyClass() {
		}
		
		public MyClass(int xArg) {
			x = xArg;
		}
		
		public MyClass addOne() {
			x++;
			return this;
		}
		
		public void maybeAdd() {
			if (x > 0) {
				x++;
			}
		}
		
		public int addAndGet(int amount) {
			return x += amount;
		}
		
		public int postIncrement() {
			return x++;
		}
		
		public int preIncrement() {
			return ++x;
		}
		
		public int getX() {
			return x;
		}
	}
	
	public static class MyNestingClass {
		public MyClass nested;
		
		public MyNestingClass(MyClass nestedArg) {
			nestedArg.x++;
			this.nested = nestedArg;
		}
		
		public MyNestingClass(int x) {
			this.nested = new MyClass(x);
		}
	}
	
	public static class MyDoubleNestingClass {
		public MyNestingClass doubleNested;
		
		public MyDoubleNestingClass(MyNestingClass nestingClass) {
			nestingClass.nested.addAndGet(5);
			this.doubleNested = nestingClass;
		}
	}
	
	@ExpectedExpressionSize(16)
	public static short negativeSquaredTest(short x) {
		if (x < 20 && x > -10) {
			int a = -2 * x;
			int b = -2 * x;
			
			return (short) (a * b);
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short ternaryTest1(short x) {
		int a = 0;
		int b = 0;
		
		if (x > 0) {
			a = 1;
		} else {
			b = 1;
		}
		
		return (short) ((a > b) ? a : b);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short ternaryTest2(short x) {
		int a = x;
		int b = 0;
		
		return (short) ((x > 0) ? a : b);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short ternaryTest3(short x) {
		return (short) ((x > x + 4) ? x : 0);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short ternaryTest4(short x) {
		if ((x > 0) ? x > 10 : x < 10) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
	public static short ternaryTest5(short x) {
		if ((x > 0) ? x > -10 : x < 10) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short ternaryTest6(short x) {
		if (x == 0) {
			return (x++ == 0) ? x : x;
		}
		
		return -10;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short earlyReturnTest1(short x) {
		MyClass nested = new MyClass(2);
		earlyReturnTest1Method(nested, x);
		return (short) nested.x;
	}
	
	private static void earlyReturnTest1Method(MyClass nested, int x) {
		nested.x = 0;
		if (x < 5) {
			nested.x = 2;
			return;
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(14)
	public static short earlyReturnTest2(short x) {
		MyClass nested = new MyClass(2);
		earlyReturnTest2Method(nested, x);
		return (short) nested.x;
	}
	
	private static void earlyReturnTest2Method(MyClass nested, int x) {
		nested.x = 0;
		if (x < 5) {
			nested.x = 2;
			return;
		}
		if (x < 10) {
			nested.x = 3;
			return;
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(19)
	public static short earlyReturnTest3(short x) {
		MyClass nested = new MyClass(2);
		earlyReturnTest3Method(nested, x);
		return (short) nested.x;
	}
	
	private static void earlyReturnTest3Method(MyClass nested, int x) {
		nested.x++;
		if (x < 10) {
			if (x < 5) {
				nested.x = 2;
				return;
			}
			nested.x += 3;
			if (x > 7) {
				return;
			}
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnTest4(short x) {
		MyClass nested = new MyClass(2);
		earlyReturnTest4Method(nested, x);
		return (short) nested.x;
	}
	
	private static void earlyReturnTest4Method(MyClass nested, int x) {
		if (x < 10) {
			return;
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short earlyReturnTest5(short x) {
		MyClass nested = new MyClass(2);
		earlyReturnTest5Method(nested, x);
		return (short) nested.x;
	}
	
	private static void earlyReturnTest5Method(MyClass nested, int x) {
		nested.x = 0;
		if (x < 10) {
			if (x < 5) {
				nested.x += 2;
				return;
			}
			nested.x += 3;
			return;
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(12)
	public static short earlyReturnTest6(short x) {
		MyClass nested = new MyClass(2);
		MyClass nested2 = new MyClass(3);
		earlyReturnTest6Method(nested, nested, x);
		return (short) (nested.x + nested2.x);
	}
	
	private static void earlyReturnTest6Method(MyClass nested, MyClass nested2, int x) {
		nested.x = 0;
		MyClass p = ((x == 3) ? nested : nested2);
		if (x < 5) {
			p.x = 2;
			return;
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short earlyReturnTest7(short x) {
		MyClass nested = new MyClass(2);
		earlyReturnTest7Method(nested, x);
		return (short) nested.x;
	}
	
	private static void earlyReturnTest7Method(MyClass nested, int x) {
		nested.x++;
		if (x < 10) {
			return;
		}
		nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short shortCircuit1(short x) {
		int a = 0;
		boolean result = false && a++ > 0;
		return (short) a;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short shortCircuit2(short x) {
		int a = 0;
		boolean result = true && a++ > 0;
		return (short) a;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short shortCircuit3(short x) {
		int a = 0;
		boolean result = false || a++ > 0;
		return (short) a;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short shortCircuit4(short x) {
		int a = 0;
		boolean result = true || a++ > 0;
		return (short) a;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short shortCircuit5(short x) {
		boolean p = false;
		boolean result = p || p;
		return (result ? (short) 1 : (short) 0);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short shortCircuit6(short x) {
		int q = 0;
		if (x > 0) {
			boolean t = q++ > 0 && q++ > 0 && true;
		}
		return (short) q;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short shortCircuit7(short x) {
		int q = 0;
		if (x > 0) {
			boolean t = ++q > 0 && ++q > 0 && true;
		}
		return (short) q;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short shortCircuit8(short x) {
		MyClass myClass = new MyClass(0);
		boolean result = addAndReturn(myClass, true) && addAndReturn(myClass, true);
		return (short) (myClass.x + ((result) ? 1 : 0));
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(5)
	public static short shortCircuit9(short x) {
		MyClass myClass = new MyClass(0);
		boolean result = addAndReturn(myClass, false) && addAndReturn(myClass, false);
		return (short) (myClass.x + ((result) ? 1 : 0));
	}
	
	private static boolean addAndReturn(MyClass myClass, boolean flag) {
		myClass.x += 10;
		return flag;
	}
	
	public static short interfaceSideEffect(short x) {
		ModifiableImpl impl = new ModifiableImpl(17);
		processModifiable(impl);
		return (short) impl.state;
	}
	
	private static void processModifiable(Modifiable m) {
		m.modify();
	}
	
	private interface Modifiable {
		void modify();
	}
	
	private static class ModifiableImpl implements Modifiable {
		int state;
		
		ModifiableImpl(int state) {
			this.state = state;
		}
		
		public void modify() {
			this.state = 75;
		}
	}
	
	public static short forLoops0(short x) {
		if (x < 0 || x > 10) {
			return 0;
		}
		
		int a = 0;
		for (int i = 0; i < x; i++) {
			a += 1;
		}
		return (short) a;
	}
	
	public static short forLoops1(short x) {
		int a = 0;
		for (int i = 0; i < 10; i++) {
			i++;
			a++;
		}
		return (short) a;
	}
	
	public static short forLoops2(short x) {
		int a = 0;
		for (int i = 0; i < 10; i++) {
			a = 5;
		}
		return (short) a;
	}
	
	public static short forLoops3(short x) {
		int a = 0;
		for (int i = 0; i < 10; i++) {
			a = i;
		}
		return (short) a;
	}
	
	public static short forLoops4(short x) {
		for (int i = 0; i < 10; i++) {
			if (i >= 10) {
				return 0;
			}
		}
		return (short) 1;
	}
	
	public static short forLoops5(short x) {
		for (int i = 0; i < 10; i++) {
			if (i < 0) {
				return 0;
			}
		}
		return (short) 1;
	}
	
	public static short forLoops6(short x) {
		if (x > 10 || x < 0) {
			return -1;
		}
		
		int b = 0;
		for (int i = 0; i < x; i++) {
			b++;
		}
		return (short) b;
	}
	
	public static short forLoops7(short x) {
		int b = 0;
		int a = 0;
		for (int i = 0; i < 10; i++) {
			b++;
			a = b;
		}
		return (short) a;
	}
	
	public static short forLoops8(short x) {
		MyClass a = new MyClass(1);
		int j = 0;
		for (int i = 0; i < 10; i++) {
			a.addOne();
			j += 2;
		}
		return (short) (a.x + j);
	}
	
	public static short forLoops9(short x) {
		MyClass a = new MyClass(1);
		for (int i = 0; i < 10; i++) {
			a = new MyClass(a.x + 1);
		}
		return (short) a.x;
	}
	
	public static short forLoops10(short x) {
		int p = 0;
		for (int i = 0; i++ < 1; i++) {
			p = i;
		}
		return (short) p;
	}
	
	public static short forLoops11(short x) {
		int p = 0;
		for (int i = 0; i++ < 10; i++) {
			p = i;
			if (i > 5) {
				return (short) p;
			}
		}
		
		return (short) p;
	}
	
	public static short forLoops12(short x) {
		MyClass p = new MyClass(1);
		for (int i = 0; i++ < 10; i++) {
			p.x = p.x + 1;
			if (i < 5) {
				p = new MyClass(p.x + 1);
			} else if (i < 8) {
				p.x = i;
			}
		}
		
		return (short) p.x;
	}
	
	public static short forLoops13(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(1);
		for (int i = 0; i++ < 10; i++) {
			a.x++;
			b.x++;
		}
		
		return (short) (a.x + b.x);
	}
	
	public static short forLoops14(short x) {
		int a = 0;
		int b = 0;
		for (int i = 0; i < 10; i++) {
			a = b + 1;
			b = a + 1;
		}
		
		return (short) (a + b);
	}
	
	public static short forLoops15(short x) {
		int a = 0;
		int b = 1;
		for (int i = 0; i < 10; i += 2) {
			a = a + b + 1;
			b = b * 2;
		}
		
		// 	Ideal final IR:
		//
		//	Loop(
		//	    target: $a
		//		condition: $i < 10
		//		variables: {
		//			a: { initial: a', next: $a + $b + 1 }
		//			b: { initial: b', next: $b * 2 }
		//		}
		//	)
		
		return (short) (a + b);
	}
	
	public static short forLoops16(short x) {
		int i1 = 0;
		int i2 = -3;
		int b = 1;
		while (i1 + i2 + b < 10) {
			b = b * 2;
			i1 += 1;
			i2 += 2;
		}
		
		// 	Ideal final IR:
		//
		//	Loop(
		//	    target: $b
		//		condition: $i1 + $i2 + $b < 10
		//		variables: {
		//			b: { initial: b', next: $b * 2 }
		//			i1: { initial: i1', next: $i1 + 1 }
		//			i2: { initial: i2', next: $i2 + 2 }
		//		}
		//	)
		
		return (short) b;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short sharedState1(short x) {
		MyClass state = new MyClass(0);
		return (short) state.addAndGet(1);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short sharedState2(short x) {
		MyClass state = new MyClass(0);
		return (short) (state.addAndGet(1) + state.addAndGet(2));
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short sharedState3(short x) {
		MyClass state = new MyClass(0);
		return (short) state.preIncrement();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short sharedState4(short x) {
		MyClass state = new MyClass(0);
		return (short) state.postIncrement();
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short aliasingAllDifferent(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = new MyNestingClass(2);
		MyNestingClass c = new MyNestingClass(3);
		return (short) aliasingStresser(a, b, c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short aliasingAeqB(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = a;
		MyNestingClass c = new MyNestingClass(3);
		return (short) aliasingStresser(a, b, c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short aliasingAeqC(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = new MyNestingClass(2);
		MyNestingClass c = a;
		return (short) aliasingStresser(a, b, c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short aliasingBeqC(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = new MyNestingClass(2);
		MyNestingClass c = b;
		return (short) aliasingStresser(a, b, c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short aliasingAllSame(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = a;
		MyNestingClass c = a;
		return (short) aliasingStresser(a, b, c);
	}
	
	private static int aliasingStresser(MyNestingClass a, MyNestingClass b, MyNestingClass c) {
		a.nested.x = 1;
		b.nested.x = 2;
		c.nested.x = 3;
		
		if (a.nested.x == 1 && b.nested.x == 2) return 1; // all different
		if (a.nested.x == 2 && b.nested.x == 2) return 2; // a == b
		if (a.nested.x == 3 && b.nested.x == 2) return 3; // a == c
		if (a.nested.x == 1 && b.nested.x == 3) return 4; // b == c
		if (a.nested.x == 3 && b.nested.x == 3) return 5; // all same
		
		return -1; // shouldn't happen
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short simpleModifyAliasing(short x) {
		MyNestingClass obj = new MyNestingClass(5);
		directModify(obj, obj);
		return (short) obj.nested.x;
	}
	
	private static void directModify(MyNestingClass a, MyNestingClass b) {
		a.nested.x++;
		b.nested.x++;
		a.nested.x++;
		a.nested.x++;
		a.nested.x++;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(7)
	public static short methodCallAliasing(short x) {
		MyNestingClass obj = new MyNestingClass(5);
		methodCallModify(obj, obj);
		return (short) obj.nested.x;
	}
	
	private static void methodCallModify(MyNestingClass a, MyNestingClass b) {
		a.nested.addOne();
		b.nested.addOne();
		a.nested.addAndGet(8);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short aliasingTest1(short x) {
		MyNestingClass a = new MyNestingClass(1);
		aliasingMethod(a, a);
		return (short) a.nested.getX();
	}
	
	private static void aliasingMethod(MyNestingClass a, MyNestingClass b) {
		a.nested.x = 0;
		if (b.nested.x == 0) {
			a.nested.x = 5;
		}
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short aliasingTest2(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = a;
		
		if (x > 0) {
			a.nested.x++;
			b.nested.x++;
		}
		
		return (short) a.nested.x;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short aliasingTest3(short x) {
		MyNestingClass a = new MyNestingClass(1);
		MyNestingClass b = a;
		
		if (x > 0) {
			a.nested.x = 1;
			b.nested.x = 2;
			a.nested.x = 3;
			
			if (b.nested.x == 2) {
				return 1;
			} else {
				return 2;
			}
		}
		
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short aliasingTest4(short x) {
		MyNestingClass a = new MyNestingClass(-1);
		MyNestingClass b = new MyNestingClass(-1);
		
		if (x > 0) {
			a.nested.x = 1;
			b.nested.x = 2;
			a.nested.x = 3;
			
			if (b.nested.x == 2) {
				return 1;
			} else {
				return 2;
			}
		}
		
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short aliasingTest5(short x) {
		MyNestingClass a = new MyNestingClass(-1);
		MyClass b = new MyClass(-1);
		
		if (x > 0) {
			a.nested.x = 1;
			b.x = 2;
			a.nested.x = 3;
			
			if (b.x == 2) {
				return 1;
			} else {
				return 2;
			}
		}
		
		return 0;
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(8)
	public static short aliasingTest6(short x) {
		MyClass b = new MyClass(-1);
		MyNestingClass a = new MyNestingClass(b);
		
		if (x > 0) {
			a.nested.x = 1;
			b.x = 2;
			a.nested.x = 3;
			
			if (b.x == 2) {
				return 1;
			} else {
				return 2;
			}
		}
		
		return 0;
	}
	
	@RequiredScore(0.4)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	@ExpectedExpressionSize(20)
	public static short nastyPerformanceTest(short x) {
		int a = 0;
		
		if (x > 2 && x < 10) {
			a = x;
		}
		
		int b = 0;
		if (a > 0 && a < 10) {
			b = a;
		}
		
		int c = 0;
		if (b > 0 && b < 10) {
			c = b;
		}
		
		return (short) (a + b + c);
	}
	
	@RequiredScore(1.0)
	@ExpectedExpressionSize(25)
	public static short nastyPerformanceTest2(short x) {
		MyClass foo = new MyClass(50);
		if (x > 100) {
			if (x > 200) {
				foo = new MyClass(100);
			}
		}
		foo.x++;
		foo.x++;
		return (short) foo.getX();
	}
}