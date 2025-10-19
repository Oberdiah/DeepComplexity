package testdata.ai;

import com.github.oberdiah.deepcomplexity.RequiredScore;

import com.github.oberdiah.deepcomplexity.ExpectedExpressionSize;

public class IfStatementVariableConstraintTrackerTest {
	// Testing basic constraint propagation through if statement
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short basicConstraint(short x) {
		int a = x;
		if (a > 5) {
			if (a > 3) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation with equality
	@RequiredScore(1.0)
	@ExpectedExpressionSize(17)
	public static short equalityConstraint(short x) {
		int a = x;
		if (a == 10) {
			if (a == 10) {
				return (short) a;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation with negation
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short negationConstraint(short x) {
		int a = x;
		if (a != 5) {
			if (a == 5) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation with less than
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short lessThanConstraint(short x) {
		int a = x;
		if (a < 10) {
			if (a < 15) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation in else branch
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short elseConstraint(short x) {
		int a = x;
		if (a > 100) {
			return 0;
		} else {
			if (a <= 100) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing multiple variable constraints
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short multiVariableConstraint(short x) {
		int a = x;
		int b = x;
		if (a > 5) {
			if (b > 5) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint with modified variable
	@RequiredScore(1.0)
	@ExpectedExpressionSize(15)
	public static short modifiedVariableConstraint(short x) {
		int a = x;
		if (a > 5) {
			a = 3;
			if (a > 5) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation with logical AND
	@RequiredScore(1.0)
	@ExpectedExpressionSize(21)
	public static short logicalAndConstraint(short x) {
		int a = x;
		if (a > 5 && a < 20) {
			if (a > 3) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation with logical OR
	@RequiredScore(1.0)
	@ExpectedExpressionSize(21)
	public static short logicalOrConstraint(short x) {
		int a = x;
		if (a < 5 || a > 20) {
			if (a == 10) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing nested constraint refinement
	@RequiredScore(1.0)
	@ExpectedExpressionSize(23)
	public static short nestedRefinement(short x) {
		int a = x;
		if (a > 0) {
			if (a > 5) {
				if (a > 3) {
					return 1;
				}
			}
		}
		return 0;
	}
	
	// Testing constraint with aliased variables
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short aliasedConstraint(short x) {
		int a = x;
		int b = a;
		if (a > 10) {
			if (b > 5) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint invalidation after assignment
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short constraintInvalidation(short x) {
		int a = x;
		if (a > 10) {
			int b = 2;
			if (a > 5) {
				return (short) b;
			}
		}
		int b = 1;
		return (short) b;
	}
	
	// Testing constraint with boundary values
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short boundaryConstraint(short x) {
		int a = x;
		if (a >= 10) {
			if (a < 10) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint propagation through multiple conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(24)
	public static short multipleCondition(short x) {
		int a = x;
		if (a == 42) {
			if (a > 40) {
				if (a < 50) {
					return (short) a;
				}
			}
		}
		return 0;
	}
	
	// Testing constraint with field access
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short fieldConstraint(short x) {
		TestClass obj = new TestClass(x);
		if (obj.value > 5) {
			if (obj.value > 3) {
				return 1;
			}
		}
		return 0;
	}
	
	private static class TestClass {
		int value;
		
		TestClass(int v) {
			this.value = v;
		}
	}
	
	// Testing constraint with method return value
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short methodConstraint(short x) {
		int a = getValue(x);
		if (a > 10) {
			if (a > 5) {
				return 1;
			}
		}
		return 0;
	}
	
	private static int getValue(int x) {
		return x;
	}
	
	// Testing constraint reset in different branches
	@RequiredScore(1.0)
	@ExpectedExpressionSize(23)
	public static short branchConstraintReset(short x) {
		int a = x;
		if (a > 0) {
			if (a > 10) {
				return 1;
			}
			return 2;
		} else {
			if (a > 10) {
				return 3;
			}
			return 4;
		}
	}
	
	// Testing constraint with combined conditions
	@RequiredScore(1.0)
	@ExpectedExpressionSize(18)
	public static short combinedConstraint(short x) {
		int a = x;
		int b = x + 1;
		if (a > 5) {
			if (b > 6) {
				return 1;
			}
		}
		return 0;
	}
	
	// Testing constraint contradiction detection
	@RequiredScore(1.0)
	@ExpectedExpressionSize(14)
	public static short contradiction(short x) {
		int a = x;
		if (a > 10 && a < 5) {
			return 1;
		}
		return 0;
	}
	
	// Testing constraint with loop-modified variable
	public static short loopModifiedConstraint(short x) {
		int a = x;
		if (a > 0) {
			for (int i = 0; i < 3; i++) {
				a++;
			}
			if (a == x) {
				return 1;
			}
		}
		return 0;
	}
}