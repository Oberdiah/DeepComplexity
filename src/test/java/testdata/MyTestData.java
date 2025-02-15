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

//	public static int test2(int incoming1, int incoming2) {
//		int foo = 0;
//		int bar = incoming1;
//		incoming2 += 1;
//
//		if (incoming1 > 2) {
//			incoming2 = incoming1 * incoming2;
//			incoming1 = 0 - incoming1;
//			bar += incoming1;
//			foo += 3;
//		} else if (incoming2 < 3) {
//			foo += 4;
//		} else {
//			bar += 5;
//		}
//
//		incoming1 += incoming2 * foo;
//
//		return incoming1 + bar * 2;
//	}
}
