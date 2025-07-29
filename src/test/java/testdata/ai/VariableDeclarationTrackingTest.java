package testdata.ai;

public class VariableDeclarationTrackingTest {
	// Testing sequential variable assignments with same value
	public static short sequentialAssignment(short x) {
		int a = 10;
		int b = a;
		int c = b;
		int d = c;
		int e = d;
		return (short) e;
	}
	
	// Testing variable swapping without temporary
	public static short variableSwap(short x) {
		int a = 3;
		int b = 5;
		a = a + b;
		b = a - b;
		a = a - b;
		return (short) a;
	}
	
	// Testing multiple variable declarations in one line
	public static short multipleDeclaration(short x) {
		int a = 5, b = 10, c = a + b;
		return (short) c;
	}
	
	// Testing variable shadowing in nested scopes
	public static short variableShadowing(short x) {
		int a = 10;
		{
			a = 20;
			return (short) a;
		}
	}
	
	// Testing variable reassignment chain
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
	public static short dependencyTracking(short x) {
		int a = 10;
		int b = a;
		int c = a + b;
		a = 5;
		return (short) c;
	}
	
	// Testing circular variable references
	public static short circularReference(short x) {
		int a = 3;
		int b = 4;
		int temp = a;
		a = b;
		b = temp;
		return (short) a;
	}
	
	// Testing variable initialization order
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
	public static short variableModification(short x) {
		int a = 5;
		int b = a;
		int c = a;
		a *= 5;
		return (short) a;
	}
	
	// Testing variable state after conditional assignment
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
	public static short incrementOperation(short x) {
		int a = 5;
		int b = a;
		++a;
		return (short) a;
	}
	
	// Testing variable state with compound assignments
	public static short compoundAssignment(short x) {
		int a = 5;
		int b = 10;
		int c = 20;
		a += b + c;
		return (short) a;
	}
	
	// Testing variable state in nested method calls
	public static short nestedMethodCall(short x) {
		int a = 8;
		int b = doubleValue(a);
		return (short) b;
	}
	
	private static int doubleValue(int val) {
		return val * 2;
	}
	
	// Testing variable state with ternary operator
	public static short ternaryOperator(short x) {
		int a = 100;
		int b = 200;
		int c = 300;
		int result = (a < b) ? b : c;
		return (short) result;
	}
}