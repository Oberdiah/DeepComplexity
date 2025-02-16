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
	public static short zeroTest(short incoming) {
		return (short) (incoming - incoming);
	}
	
	@RequiredScore(0.5) // We've not implemented even/odd/modulo detection yet.
	public static short simpleAdd(short incoming1) {
		return (short) (incoming1 + incoming1);
	}
	
	@RequiredScore(0.75) // We've not implemented even/odd/modulo detection yet.
	public static short test3(short incoming1) {
		int foo = 0;
		int bar = incoming1;
		int incoming2 = incoming1 * 2;
		
		if (incoming1 > 2) {
			incoming2 = incoming1 * incoming2;
			incoming1 = (short) (0 - incoming1);
			bar += incoming1;
			foo += 3;
		}
		
		return (short) (incoming1 + bar);
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
	public static short barTest1(short incoming1) {
		int bar = 0;
		
		if (incoming1 > 2) {
			incoming1 = (short) (0 - incoming1);
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
			incoming1 = (short) (0 - incoming1);
			bar += incoming1;
		} else {
			bar += 5;
		}
		
		return (short) (bar * 2);
	}
	
	public static short barTest3(short incoming1) {
		int bar = 0;
		
		if (incoming1 > 2) {
			incoming1 = (short) (0 - incoming1);
			bar += incoming1;
		} else {
			bar += 5;
		}
		
		return (short) ((bar * 2) / 2);
	}
	
	public static short largeTest1(short incoming1) {
		int foo = 0;
		int bar = 0;
		int incoming2 = incoming1;
		if (incoming1 < 0 - 10) {
			incoming2 = 0;
		}
		
		incoming2 += 1;
		
		if (incoming1 > 2) {
			incoming2 = (short) (incoming1 * incoming2);
			incoming1 = (short) (0 - incoming1);
			bar += incoming1;
			foo += 3;
		} else if (incoming2 < 3) {
			foo += 4;
		} else {
			bar += 5;
		}
		
		return (short) (incoming2 * foo + bar * 2);
	}
	
	public static short multiplicationTest1(short incoming1) {
		return (short) (incoming1 * (incoming1 + 1));
	}
	
	@RequiredScore(0.67)
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
	
	public static short combinedIfs2(short incoming1) {
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
		
		return (short) (a * b);
	}
}
