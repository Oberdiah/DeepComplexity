package testdata.ai;

import com.oberdiah.deepcomplexity.RequiredScore;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;

public class VariableDeclarationTrackingTest {
	// Testing sequential variable assignments with same value
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short sequentialAssignment(short x) {
		int a = 10;
		int b = a;
		int c = b;
		int d = c;
		int e = d;
		return (short) e;
	}
	
	// Testing variable swapping without temporary
	@RequiredScore(1.0)
	@ExpectedExpressionSize(10)
	public static short variableSwap(short x) {
		int a = 3;
		int b = 5;
		a = a + b;
		b = a - b;
		a = a - b;
		return (short) a;
	}
	
	// Testing multiple variable declarations in one line
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short multipleDeclaration(short x) {
		int a = 5, b = 10, c = a + b;
		return (short) c;
	}
	
	// Testing variable shadowing in nested scopes
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short variableShadowing(short x) {
		int a = 10;
		{
			a = 20;
			return (short) a;
		}
	}
	
	// Testing variable reassignment chain
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short reassignmentChain(short x) {
		int a = 1;
		int b = 2;
		int c = 3;
		a = b;
		b = c;
		c = a + b + c;
		return (short) c;
	}
	
	// Testing variable dependency tracking
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short dependencyTracking(short x) {
		int a = 10;
		int b = a;
		int c = a + b;
		a = 5;
		return (short) c;
	}
	
	// Testing circular variable references
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short circularReference(short x) {
		int a = 3;
		int b = 4;
		int temp = a;
		a = b;
		b = temp;
		return (short) a;
	}
	
	// Testing variable initialization order
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short initializationOrder(short x) {
		int a = getTwo() + getThree();
		int b = a;
		return (short) b;
	}
	
	private static int getTwo() {
		return 2;
	}
	
	private static int getThree() {
		return 3;
	}
	
	// Testing variable modification through operations
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short variableModification(short x) {
		int a = 5;
		int b = a;
		int c = a;
		a *= 5;
		return (short) a;
	}
	
	// Testing variable state after conditional assignment
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short conditionalAssignment(short x) {
		int a = 50;
		int b = 100;
		int c = a;
		if (true) {
			c = b;
		}
		return (short) c;
	}
	
	// Testing variable aliasing with arrays
	public static short arrayAliasing(short x) {
		int[] arr1 = {42};
		int[] arr2 = arr1;
		int a = arr2[0];
		arr1[0] = 99;
		return (short) a;
	}
	
	// Testing variable state in loop iterations
	public static short loopVariable(short x) {
		int a = 0;
		int b = 0;
		for (int i = 0; i < 3; i++) {
			a = i;
			b = a;
		}
		return (short) b;
	}
	
	// Testing variable interference between similar names
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short similarNames(short x) {
		int var1 = 10;
		int var11 = 15;
		int result = var1;
		result = var11;
		return (short) result;
	}
	
	// Testing variable state after exception handling
	public static short exceptionHandling(short x) {
		int a = 5;
		int b = a;
		try {
			a = 7;
			b = a;
		} catch (Exception e) {
			// Never reached
		}
		return (short) b;
	}
	
	// Testing variable state with method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short methodCall(short x) {
		int a = 10;
		int b = a;
		a = modifyValue(a);
		return (short) a;
	}
	
	private static int modifyValue(int val) {
		return val + 2;
	}
	
	// Testing variable final assignment tracking
	public static short finalAssignment(short x) {
		int a;
		int b;
		int c;
		a = 77;
		b = 88;
		c = 99;
		a = b;
		return (short) a;
	}
	
	// Testing variable state with increment operations
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short incrementOperation(short x) {
		int a = 5;
		int b = a;
		++a;
		return (short) a;
	}
	
	// Testing variable state with compound assignments
	@RequiredScore(1.0)
	@ExpectedExpressionSize(6)
	public static short compoundAssignment(short x) {
		int a = 5;
		int b = 10;
		int c = 20;
		a += b + c;
		return (short) a;
	}
	
	// Testing variable state in nested method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short nestedMethodCall(short x) {
		int a = 8;
		int b = doubleValue(a);
		return (short) b;
	}
	
	private static int doubleValue(int val) {
		return val * 2;
	}
	
	// Testing variable state with ternary operator
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short ternaryOperator(short x) {
		int a = 100;
		int b = 200;
		int c = 300;
		int result = (a < b) ? b : c;
		return (short) result;
	}
}