package testdata;

import com.github.oberdiah.deepcomplexity.RequiredScore;

public class MyTestData {
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
	
	@RequiredScore(0.75) // We've not implemented even/odd/modulo detection yet.
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
	
	@RequiredScore(0.5) // We've not implemented even/odd/modulo detection yet.
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
		int a = 0;
		if (x > 10) {
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
	
	public static short equalityTest3(short x) {
		if (x == 5) {
			return x;
		}
		return 0;
	}
}
