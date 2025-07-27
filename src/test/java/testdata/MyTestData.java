package testdata;

import com.github.oberdiah.deepcomplexity.GoodEnough;
import com.github.oberdiah.deepcomplexity.RequiredScore;

import static com.github.oberdiah.deepcomplexity.GoodEnough.GoodEnoughReason.*;

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
	public static short test6(short x) {
		if (x > x) {
			return 1;
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
	public static short oneTest(short x) {
		return (short) (x + 1 - x);
	}
	
	public static short oneTest2(short x) {
		return (short) (x / x);
	}
	
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
	
	@RequiredScore(0.5)
	@GoodEnough(GAPS_FROM_MULTIPLICATION)
	public static short multiplyTest2(short x) {
		int a = x * 2;
		if (a == 7) {
			return 1;
		} else {
			return 0;
		}
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
	
	@RequiredScore(1.0)
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
	
	@RequiredScore(1.0)
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
	public static short chainedConstraintTest2(short x) {
		int a = 0;
		int b = x * 5;
		
		if (x > b && b > 5) {
			a = x;
		}
		
		return (short) (a);
	}
	
	@RequiredScore(1.0)
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
	
	@RequiredScore(1.0)
	public static short equalityTest1(short x) {
		if (x == x) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
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
	
	public static short largeMultiplication1(short x) {
		return (short) (x * 65536);
	}
	
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
	
	@RequiredScore(1.0)
	public static short modulo3(short x) {
		int a = x % 100;
		if (a > a) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short modulo4(short x) {
		int a = x % 100;
		if (x < 100 || x > 100) {
			a = x % 90;
		}
		
		int b = a;
		int c = a;
		if (b > c) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static short twoVars1(short x) {
		int b = x % 100;
		int a = x % 100;
		
		if (a < b && a > b) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(0.0051)
	@GoodEnough(REQUIRES_IDENTIFYING_IDENTICAL_EXPRESSIONS)
	public static short twoVars2(short x) {
		int b = x % 100;
		int a = x % 100;
		
		if (a > b) {
			return (short) a;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short twoVars3(short x) {
		int b = x % 100;
		int a = x % 100;
		
		if (a > 10 && a < 20 && b == a) {
			return (short) b;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
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
	
	public static short twoVars5(short x) {
		int b = x % 100;
		int a = x % 100;
		
		if (b == a && a > 10 && a < 20) {
			return (short) b;
		} else {
			return 0;
		}
	}
	
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
	
	public static short wrappingComparison(short x) {
		if ((short) (x - 1) > 0) {
			return x;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short nearlyImpossible(short x) {
		short y = (short) (x - 1);
		if (x < 0 && y > 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	public static short alsoNearlyImpossible(short x) {
		int a = x * -65536 - 65535;
		if (a < 0 && a - 1 > 0) {
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
	
	@RequiredScore(1.0)
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
	public static short simpleValueCheck(short x) {
		if (x > (5 - 10)) {
			return 100;
		} else {
			return 200;
		}
	}
	
	@RequiredScore(1.0)
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
	
	public static short simpleClassTest0(short x) {
		MyClass nested = new MyClass();
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest1(short x) {
		MyClass nested = new MyClass(2);
		nested.x = 1;
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest2(short x) {
		MyClass nested = new MyClass(3);
		nested.x = 1;
		if (nested.x == 1) {
			return 2;
		}
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest3(short x) {
		MyClass nested = new MyClass(3);
		if (nested.x == 3) {
			return 2;
		}
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest4(short x) {
		MyClass nested = new MyClass(2);
		updateClassField(nested);
		return (short) nested.x;
	}
	
	private static void updateClassField(MyClass nested) {
		nested.x = 5;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest5(short x) {
		MyClass nested = new MyClass(2);
		nested.addOne();
		return (short) nested.x;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest6(short x) {
		MyClass nested = new MyClass(2);
		return (short) (nested.getX() - nested.x);
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest7(short x) {
		MyClass nested = new MyClass(2);
		nested.addOne();
		if (nested.getX() == 3) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest8(short x) {
		MyClass nested = new MyClass(2);
		nested.addOne();
		int v = nested.x;
		nested.addOne();
		return (short) (v + nested.getX());
	}
	
	public static short simpleClassTest9(short x) {
		MyClass nested = new MyClass();
		if (nested.getX() == 1000) {
			return 1;
		}
		return 0;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest10(short x) {
		MyClass nested = new MyClass(50);
		if (x == 100) {
			nested = new MyClass(100);
		}
		nested.x = 5;
		return (short) nested.getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest11(short x) {
		MyClass nested = new MyClass(50).addOne();
		return (short) nested.getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest12(short x) {
		return (short) new MyClass(50).addOne().addOne().addOne().getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest13(short x) {
		MyClass nested = new MyClass(50);
		MyNestingClass nesting = new MyNestingClass(nested);
		return (short) nesting.nested.getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest14(short x) {
		MyClass nested = new MyClass(50);
		MyNestingClass nesting = new MyNestingClass(nested);
		nesting.nested.x++;
		nesting.nested.addOne();
		return (short) nesting.nested.getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest15(short x) {
		int nested = new MyClass(50).x = 5;
		return (short) nested;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest16(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		int p = ((x > 0) ? a : b).x;
		return (short) p;
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest17(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		((x > 0) ? a : b).x = 5;
		return (short) a.x;
	}
	
	@RequiredScore(1.0)
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
	public static short simpleClassTest19(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		if (x <= 0) {
			b = a;
		}
		b.x = 0;
		return (short) b.getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest20(short x) {
		MyClass a = new MyClass(1);
		MyClass b = a;
		b.x = 0;
		return (short) a.getX();
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest21(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		if (x <= 0) {
			b = a;
		}
		b.x = 0;
		
		return (short) a.getX();
	}
	
	public static short simpleClassTest22(short x) {
		MyClass a = new MyClass(1);
		aliasingMethod(a, a);
		return (short) a.getX();
	}
	
	private static void aliasingMethod(MyClass a, MyClass b) {
		a.x = 0;
		if (b.x == 0) {
			a.x = 5;
		}
	}
	
	@RequiredScore(1.0)
	public static short simpleClassTest23(short x) {
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(2);
		
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
	public static short simpleClassTest24(short x) {
		MyClass c = new MyClass(100);
		
		MyClass a = new MyClass(1);
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			c = new MyClass(100);
			a = c;
		} else {
			c = new MyClass(1000);
			b = c;
		}
		
		return (short) (a.x + b.x);
	}
	
	public static short simpleClassTest25(short x) {
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			b.x = 0;
		}
		
		return (short) (b.x);
	}
	
	public static short simpleClassTest26(short x) {
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			b.x++;
		}
		
		return (short) (b.x);
	}
	
	public static short simpleClassTest27(short x) {
		MyClass b = new MyClass(10);
		
		if (x > 0) {
			b.x *= -1;
		}
		
		b.maybeAdd();
		
		return (short) (b.x);
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
		
		public int getX() {
			return x;
		}
	}
	
	public static class MyNestingClass {
		public MyClass nested;
		
		public MyNestingClass(MyClass nested) {
			nested.x++;
			this.nested = nested;
		}
	}
	
	public static short negativeSquaredTest(short x) {
		if (x < 20 && x > -10) {
			int a = -2 * x;
			int b = -2 * x;
			
			return (short) (a * b);
		}
		return 0;
	}
	
	@RequiredScore(1.0)
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
	public static short ternaryTest1NoTernary(short x) {
		int a = 0;
		int b = 0;
		
		if (x > 0) {
			a = 1;
		} else {
			b = 1;
		}
		
		int c = 0;
		if (a > b) {
			c = a;
		} else {
			c = b;
		}
		
		return (short) c;
	}
	
	@RequiredScore(1.0)
	public static short ternaryTest2(short x) {
		int a = x;
		int b = 0;
		
		return (short) ((x > 0) ? a : b);
	}
	
	public static short ternaryTest3(short x) {
		return (short) ((x > x + 4) ? x : 0);
	}
	
	public static short ternaryTest4(short x) {
		if ((x > 0) ? x > 10 : x < 10) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static short earlyReturnTest1(short x) {
		MyClass nested = new MyClass(2);
		
		updateClassField2(nested, x);
		
		return (short) nested.x;
	}
	
	private static void updateClassField2(MyClass nested, int x) {
		if (x < 5) {
			return;
		}
		nested.x = x;
	}
}
