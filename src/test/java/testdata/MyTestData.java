package testdata;

import com.github.oberdiah.deepcomplexity.RequiredScore;

public class MyTestData {
	@RequiredScore(1.0)
	public static short test1(short incoming) {
		if (incoming < 3) {
			return 2;
		} else {
			return 1;
		}
	}
	
	@RequiredScore(1.0)
	public static short test2(short incoming) {
		if (incoming < 3) {
			return incoming;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short test3(short incoming) {
		if (incoming < 5 && incoming < 3) {
			return incoming;
		} else {
			return 0;
		}
	}
	
	@RequiredScore(0.75) // We've not implemented even/odd/modulo detection yet.
	public static short test4(short incoming1) {
		int bar = incoming1;
		
		if (incoming1 > 2) {
			incoming1 = (short) (-incoming1);
			bar += incoming1;
		}
		
		return (short) (incoming1 + bar);
	}
	
	@RequiredScore(1.0)
	public static short test5(short incoming) {
		if (incoming < 5) {
			if (incoming < 3) {
				return incoming;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	
	@RequiredScore(1.0)
	public static short negateTest(short incoming) {
		return (short) (-incoming);
	}
	
	@RequiredScore(1.0)
	public static short negateTest2(short incoming) {
		return (short) (-incoming + incoming);
	}
	
	@RequiredScore(1.0)
	public static short plusTest(short incoming) {
		return (short) (+incoming);
	}
	
	@RequiredScore(1.0)
	public static short incrementTest(short incoming) {
		int a = incoming;
		int b = a++;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short incrementTest2(short incoming) {
		int a = incoming;
		int b = ++a;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short decrementTest(short incoming) {
		int a = incoming;
		int b = a--;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short decrementTest2(short incoming) {
		int a = incoming;
		int b = --a;
		int c = a;
		return (short) (b - c);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest(short incoming) {
		return (short) (incoming - incoming);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest2(short incoming) {
		return (short) (incoming * 2 - incoming * 2);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest3(short incoming) {
		return (short) ((incoming + 1) * 2 - (incoming + 1) * 2);
	}
	
	@RequiredScore(1.0)
	public static short zeroTest4(short incoming) {
		int a = ((incoming * 2) - 1) * 2;
		int b = ((incoming * 2) - 1) * 2;
		return (short) (a - b);
	}
	
	@RequiredScore(0.5) // We've not implemented even/odd/modulo detection yet.
	public static short simpleAdd(short incoming1) {
		return (short) (incoming1 + incoming1);
	}
	
	@RequiredScore(1.0)
	public static short shortTest1(short incoming) {
		return incoming;
	}
	
	@RequiredScore(1.0)
	public static short shortTest2(short incoming) {
		return (short) (((int) incoming) + 1);
	}
	
	@RequiredScore(1.0)
	public static short shortTest3(short incoming) {
		return (short) (incoming + 1);
	}
	
	@RequiredScore(1.0)
	public static short multiplyTest1(short incoming) {
		int a = 0;
		int b = 0;
		if (incoming > 0 && incoming < 10) {
			a = incoming;
			b = incoming;
		}
		
		return (short) (a * 2 - b * 2);
	}
	
	@RequiredScore(1.0)
	public static short barTest1(short incoming1) {
		int bar = 0;
		
		if (incoming1 > 2) {
			incoming1 = (short) (-incoming1);
			bar += incoming1;
		} else {
			bar += 5;
		}
		
		return (short) (bar);
	}
	
	@RequiredScore(0.5)
	public static short barTest2(short incoming1) {
		int bar = 0;
		
		if (incoming1 > 2) {
			incoming1 = (short) (-incoming1);
			bar += incoming1;
		} else {
			bar += 5;
		}
		
		return (short) (bar * 2);
	}
	
	public static short barTest3(short incoming1) {
		int bar = 0;
		
		if (incoming1 > 2) {
			incoming1 = (short) (-incoming1);
			bar += incoming1;
		} else {
			bar += 5;
		}
		
		return (short) ((bar * 2) / 2);
	}
	
	@RequiredScore(0.3334)
	public static short largeTest1(short incoming1) {
		int foo = 0;
		int bar = 0;
		int incoming2 = incoming1;
		if (incoming1 < -10) {
			incoming2 = 0;
		}
		
		incoming2 += 1;
		
		if (incoming1 > 2) {
			incoming2 = (short) (incoming1 * incoming2);
			incoming1 = (short) (-incoming1);
			bar += incoming1;
			foo += 3;
		} else if (incoming2 < 3) {
			foo += 4;
		} else {
			bar += 5;
		}
		
		return (short) (incoming2 * foo + bar * 2);
	}
	
	@RequiredScore(0.5)
	public static short multiplicationTest1(short incoming1) {
		return (short) (incoming1 * (incoming1 + 1));
	}
	
	@RequiredScore(1.0)
	public static short combinedIfs1(short incoming1) {
		int a = incoming1;
		if (incoming1 < 0) {
			a = 0;
		} else if (incoming1 > 10) {
			a = 10;
		}
		
		int b = incoming1;
		if (incoming1 < 10) {
			b = 0;
		} else if (incoming1 > 20) {
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
	public static short combinedIfs3(short incoming1) {
		int a = incoming1;
		if (incoming1 < 0) {
			a = 0;
		} else if (incoming1 >= 10) {
			a = 0;
		}
		
		int b = incoming1;
		if (incoming1 < 10) {
			b = 0;
		} else if (incoming1 > 20) {
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
	
	public static short chainedConstraintTest1(short incoming) {
		int x = 0;
		int y = incoming + 5;
		
		if (incoming > y) {
			if (y > 5) {
				x = incoming;
			}
		}
		
		return (short) (x);
	}
	
	public static short chainedConstraintTest2(short incoming) {
		int x = 0;
		int y = incoming * 5;
		
		if (incoming > y && y > 5) {
			x = incoming;
		}
		
		return (short) (x);
	}
	
	public static short intelliJTest1(short incoming) {
		int a = 0;
		int b = 0;
		if (incoming > 0) {
			a = 1;
		}
		if (incoming * 5 > 0) {
			b = 1;
		}
		
		short v = 0;
		if (a + b > 1 && incoming < 0) {
			v = 1;
		}
		
		return v;
	}
	
	@RequiredScore(0.122)
	public static short constraintTest1(short incoming) {
		int a = 0;
		int b = 0;
		if (incoming > 0 && incoming < 10) {
			a = incoming;
		}
		if (incoming > 0 && incoming < 30) {
			b = incoming;
		}
		
		return (short) (a * b);
	}
	
	@RequiredScore(1.0)
	public static short constraintTest2(short incoming) {
		int a = 0;
		if (incoming > 10) {
			a = incoming;
		}
		if (incoming < 20) {
			a = incoming + 5;
		}
		
		int b = 0;
		if (incoming < 10) {
			b = a;
		}
		return (short) b;
	}
	
	@RequiredScore(1.0)
	public static short constraintTest3(short incoming) {
		int a = 0;
		if (incoming > 10) {
			a = incoming;
		}
		if (incoming < 20) {
			a = incoming + 5;
		}
		
		int b = 0;
		if (incoming < 10) {
			b = a;
		}
		return (short) b;
	}
	
	@RequiredScore(1.0)
	public static short constraintTest4(short incoming) {
		int a = 0;
		if (incoming > 7 || incoming < 2) {
			a = incoming;
		}
		
		int b = 0;
		if (incoming < 10) {
			b = a;
		}
		return (short) b;
	}
	
	@RequiredScore(1.0)
	public static short returnTest1(short incoming) {
		if (incoming > 0) {
			return incoming;
		}
		
		if (incoming > -10) {
			return -5;
		}
		
		if (incoming > -20) {
			return -10;
		}
		
		return -150;
	}
	
	@RequiredScore(1.0)
	public static short returnTest2(short i) {
		int a = -5;
		
		if (i > 0) {
			if (i < 20) {
				a = -10;
				return i;
			}
			a += -15;
		}
		
		return (short) a;
	}
	
	@RequiredScore(1.0)
	public static short returnTest3(short incoming) {
		if (incoming > 0) {
			if (incoming < 20) {
				return 5;
			}
		}
		
		int a = 0;
		if (incoming > 10) {
			a = 1;
		}
		
		return (short) a;
	}
	
	@RequiredScore(1.0)
	public static short returnTest4(short incoming) {
		if (incoming > 0) {
			if (incoming < 10) {
				return 5;
			}
		} else {
			if (incoming > -10) {
				return 10;
			} else {
				return 20;
			}
		}
		
		int a = 0;
		if (incoming > 15) {
			a = 1;
		}
		
		return (short) a;
	}
}
