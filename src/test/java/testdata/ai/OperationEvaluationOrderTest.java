
package testdata.ai;

public class OperationEvaluationOrderTest {
    	// Testing short-circuit AND with side effects on left operand
	public static short shortCircuitAndLeft(short x) {
	    int a = 0;
	    boolean result = ++a > 0 && ++a > 1;
	    return (short) a;
	}
	
	// Testing short-circuit AND with side effects on right operand
	public static short shortCircuitAndRight(short x) {
	    int a = 0;
	    boolean result = false && ++a > 0;
	    return (short) a;
	}
	
	// Testing short-circuit OR with side effects on left operand
	public static short shortCircuitOrLeft(short x) {
	    int a = 0;
	    boolean result = ++a > 0 || ++a > 1;
	    return (short) a;
	}
	
	// Testing short-circuit OR with side effects on right operand
	public static short shortCircuitOrRight(short x) {
	    int a = 0;
	    boolean result = true || ++a > 0;
	    return (short) a;
	}
	
	// Testing nested short-circuit evaluation order
	public static short nestedShortCircuit(short x) {
	    int a = 0;
	    boolean result = (++a > 0 && false) || ++a > 1;
	    return (short) a;
	}
	
	// Testing ternary operator evaluation order
	public static short ternaryEvalOrder(short x) {
	    int a = 0;
	    int result = true ? ++a : ++a + 10;
	    return (short) a;
	}
	
	// Testing ternary with side effects in condition
	public static short ternaryConditionSideEffect(short x) {
	    int a = 0;
	    int result = ++a > 0 ? ++a : 99;
	    return (short) a;
	}
	
	// Testing method call order in boolean expression
	public static short methodCallOrder(short x) {
	    Counter c = new Counter();
	    boolean result = c.increment() && c.increment() && c.increment();
	    return (short) c.value;
	}
	
	// Testing assignment in condition with short-circuit
	public static short assignmentInCondition(short x) {
	    int a = 0;
	    int b = 0;
	    if ((a = 5) > 0 && (b = 10) > 0) {
	        return (short) a;
	    }
	    return (short) b;
	}
	
	// Testing post-increment in short-circuit context
	public static short postIncrementShortCircuit(short x) {
	    int a = 0;
	    boolean result = false && a++ > 0;
	    return (short) a;
	}
	
	// Testing pre-increment vs post-increment order
	public static short prePostIncrementOrder(short x) {
	    int a = 1;
	    int result = ++a + a++;
	    return (short) a;
	}
	
	// Testing array access with side effects in short-circuit
	public static short arrayAccessSideEffect(short x) {
	    int[] arr = {1, 2, 3};
	    int index = 0;
	    boolean result = false && arr[++index] > 0;
	    return (short) index;
	}
	
	// Testing complex expression with multiple operators
	public static short complexExpressionOrder(short x) {
	    int a = 1;
	    int result = a++ + ++a * a++ - --a;
	    return (short) a;
	}
	
	// Testing chained method calls with side effects
	public static short chainedMethodCalls(short x) {
	    Counter c = new Counter();
	    boolean result = c.increment() && c.getValue() > 0;
	    return (short) c.value;
	}
	
	// Testing side effects in function parameters
	public static short functionParameterOrder(short x) {
	    int a = 0;
	    int result = Math.max(++a, ++a + ++a);
	    return (short) a;
	}
	
	// Testing evaluation order with exception handling
	public static short exceptionEvalOrder(short x) {
	    int a = 0;
	    try {
	        boolean result = ++a > 0 && (10 / 0) > 0;
	    } catch (ArithmeticException e) {
	        // Expected to catch
	    }
	    return (short) a;
	}
	
	// Testing instanceof with side effects
	public static short instanceofSideEffect(short x) {
	    Counter c = new Counter();
	    Object obj = null;
	    boolean result = obj instanceof String && c.increment();
	    return (short) c.value;
	}
	
	// Testing comma operator simulation with method calls
	public static short commaOperatorSimulation(short x) {
	    Counter c = new Counter();
	    return (short) (c.increment() ? c.increment() ? c.value : 0 : 0);
	}
	
	// Testing side effects in array initialization
	public static short arrayInitSideEffect(short x) {
	    int a = 0;
	    int[] arr = {++a, ++a, ++a};
	    return (short) a;
	}
	
	// Testing evaluation order in switch expression
	public static short switchExpressionOrder(short x) {
	    int a = 0;
	    int result = switch (++a) {
	        case 1 -> ++a;
	        default -> 0;
	    };
	    return (short) a;
	}
	
	private static class Counter {
	    int value = 0;
	    
	    boolean increment() {
	        value++;
	        return true;
	    }
	    
	    int getValue() {
	        return value;
	    }
	}
}