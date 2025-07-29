package testdata.ai;

public class IfStatementVariableConstraintTrackerTest {
	// Testing basic constraint propagation through if statement
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