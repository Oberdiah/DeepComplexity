package testdata.ai;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;
import com.oberdiah.deepcomplexity.RequiredScore;

public class ParameterOrderEvaluationTest {
	// Testing parameter evaluation order with side effects
	@ExpectedExpressionSize(8)
	public static short parameterOrder(short x) {
		Counter c = new Counter();
		multiParamMethod(c.increment(), c.increment(), c.increment());
		return (short) c.value;
	}
	
	// Testing parameter order with array modifications
	public static short arrayParamOrder(short x) {
		int[] arr = {1};
		multiParamMethod(arr[0], arr[0] = 2, arr[0] = 3);
		return (short) arr[0];
	}
	
	// Testing field modification in parameter evaluation
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short fieldParamOrder(short x) {
		FieldClass obj = new FieldClass(5);
		multiParamMethod(obj.field, obj.field = 10, obj.field + 5);
		return (short) obj.field;
	}
	
	// Testing method calls with shared state in parameters
	@RequiredScore(1.0)
	@ExpectedExpressionSize(20)
	public static short sharedStateParam(short x) {
		SharedState state = new SharedState(0);
		return (short) multiParamSum(state.addAndGet(1), state.addAndGet(2), state.addAndGet(3));
	}
	
	// Testing exception handling in parameter evaluation order
	public static short exceptionParamOrder(short x) {
		Counter c = new Counter();
		try {
			riskyMethod(c.increment(), throwException(), c.increment());
		} catch (RuntimeException e) {
			// Expected to catch exception
		}
		return (short) c.value;
	}
	
	// Testing nested method calls in parameter evaluation
	@ExpectedExpressionSize(12)
	public static short nestedCallParam(short x) {
		Counter c = new Counter();
		multiParamMethod(incrementTwice(c), c.increment(), incrementTwice(c));
		return (short) c.value;
	}
	
	// Testing static field modifications in parameters
	public static short staticFieldParam(short x) {
		StaticCounter.value = 10;
		multiParamMethod(StaticCounter.value, StaticCounter.value = 20, StaticCounter.value + 10);
		return (short) StaticCounter.value;
	}
	
	// Testing parameter order with object creation
	@ExpectedExpressionSize(6)
	public static short objectCreationParam(short x) {
		Counter c = new Counter();
		multiParamMethod(new Counter().increment(), c.increment(), c.increment());
		return (short) c.value;
	}
	
	// Testing array element assignment order in parameters
	public static short arrayElementParam(short x) {
		int[] arr = {5, 10, 15};
		multiParamMethod(arr[0], arr[1] = arr[2], arr[0] = arr[1] + 10);
		return (short) arr[0];
	}
	
	// Testing parameter evaluation with volatile fields
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short volatileParam(short x) {
		VolatileClass obj = new VolatileClass(3);
		multiParamMethod(obj.volatileField, obj.volatileField = 7, obj.volatileField + 4);
		return (short) obj.volatileField;
	}
	
	// Testing parameter order with synchronized methods
	public static short synchronizedParam(short x) {
		SyncCounter sync = new SyncCounter();
		multiParamMethod(sync.increment(), sync.increment(), sync.increment());
		return (short) sync.getValue();
	}
	
	// Testing parameter evaluation with inheritance
	public static short inheritanceParam(short x) {
		ChildClass child = new ChildClass(4);
		multiParamMethod(child.getValue(), child.doubleValue(), child.getValue());
		return (short) child.getValue();
	}
	
	// Testing parameter order with boolean short-circuit
	@ExpectedExpressionSize(19)
	public static short shortCircuitParam(short x) {
		Counter c = new Counter();
		booleanMethod(c.increment() > 0 && c.increment() > 0, c.increment() > 0);
		return (short) c.value;
	}
	
	// Testing parameter evaluation with generic methods
	public static short genericParam(short x) {
		GenericClass<Integer> gen = new GenericClass<>(3);
		multiParamMethod(gen.getValue(), gen.setValue(6), gen.getValue());
		return (short) gen.getValue().intValue();
	}
	
	// Testing parameter order with constructor calls
	@ExpectedExpressionSize(8)
	public static short constructorParam(short x) {
		Counter c = new Counter();
		multiParamMethod(c.increment(), new Counter(c.increment()).value, c.increment());
		return (short) c.value;
	}
	
	// Testing parameter evaluation with string concatenation
	public static short stringConcatParam(short x) {
		Counter c = new Counter();
		stringMethod("" + c.increment(), "" + c.increment(), "" + c.increment());
		return (short) c.value;
	}
	
	// Testing parameter order with ternary operator
	@ExpectedExpressionSize(6)
	public static short ternaryParam(short x) {
		Counter c = new Counter();
		multiParamMethod(c.increment(), true ? c.increment() : c.increment(), c.value);
		return (short) c.value;
	}
	
	// Testing parameter evaluation with array access
	public static short arrayAccessParam(short x) {
		int[] arr = {1, 2, 3};
		Counter c = new Counter();
		multiParamMethod(arr[c.increment() - 1], arr[c.increment() - 1], arr[c.increment() - 1]);
		return (short) c.value;
	}
	
	// Testing parameter order with multiple assignment
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short multipleAssignmentParam(short x) {
		int a = 10, b = 20;
		multiParamMethod(a, a = b, b = 30);
		return (short) b;
	}
	
	// Testing parameter evaluation with recursive calls
	public static short recursiveParam(short x) {
		Counter c = new Counter();
		recursiveMethod(c.increment(), 2);
		return (short) c.value;
	}
	
	private static void multiParamMethod(int a, int b, int c) {
	}
	
	private static int multiParamSum(int a, int b, int c) {
		return a + b + c;
	}
	
	private static void riskyMethod(int a, int b, int c) {
	}
	
	private static int throwException() {
		throw new RuntimeException();
	}
	
	private static int incrementTwice(Counter c) {
		c.increment();
		return c.increment();
	}
	
	private static void booleanMethod(boolean a, boolean b) {
	}
	
	private static void stringMethod(String a, String b, String c) {
	}
	
	private static void recursiveMethod(int param, int depth) {
		if (depth > 0) {
			recursiveMethod(param, depth - 1);
		}
	}
	
	private static class Counter {
		int value = 0;
		
		Counter() {
		}
		
		Counter(int initial) {
			this.value = initial;
		}
		
		int increment() {
			return ++value;
		}
	}
	
	private static class FieldClass {
		int field;
		
		FieldClass(int field) {
			this.field = field;
		}
	}
	
	private static class SharedState {
		int value;
		
		SharedState(int value) {
			this.value = value;
		}
		
		int addAndGet(int amount) {
			return value += amount;
		}
	}
	
	private static class StaticCounter {
		static int value = 0;
	}
	
	private static class VolatileClass {
		volatile int volatileField;
		
		VolatileClass(int value) {
			this.volatileField = value;
		}
	}
	
	private static class SyncCounter {
		private int value = 0;
		
		synchronized int increment() {
			return ++value;
		}
		
		synchronized int getValue() {
			return value;
		}
	}
	
	private static class ParentClass {
		protected int value;
		
		ParentClass(int value) {
			this.value = value;
		}
		
		int getValue() {
			return value;
		}
	}
	
	private static class ChildClass extends ParentClass {
		ChildClass(int value) {
			super(value);
		}
		
		int doubleValue() {
			return value *= 2;
		}
	}
	
	private static class GenericClass<T> {
		private T value;
		
		GenericClass(T value) {
			this.value = value;
		}
		
		T getValue() {
			return value;
		}
		
		T setValue(T newValue) {
			T old = value;
			value = newValue;
			return old;
		}
	}
}