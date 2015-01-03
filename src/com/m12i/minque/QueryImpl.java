package com.m12i.minque;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class QueryImpl<E> implements Query<E> {
	private final Expression expression;
	private final Accessor<E> accessor;
	private final Placeholders ph;
	private final boolean hasPlaceholders;
	public QueryImpl(Expression expression, final Placeholders ph, Accessor<E> accessor) {
		this.expression = expression;
		this.accessor = accessor;
		this.ph = ph;
		this.hasPlaceholders = ph.amount() > 0;
	}

	@Override
	public List<E> selectFrom(Iterable<E> source) {
		if (hasPlaceholders) {
			throw new IllegalArgumentException("Bind variables is required for this query.");
		}
		final List<E> result = new ArrayList<E>();
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result.add(elem);
			}
		}
		return result;
	}

	@Override
	public List<E> selectFrom(Iterable<E> source, Object... vars) {
		ph.bind(vars);
		final List<E> result = new ArrayList<E>();
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result.add(elem);
			}
		}
		return result;
	}

	@Override
	public int countFrom(Iterable<E> source) {
		if (hasPlaceholders) {
			throw new IllegalArgumentException("Bind variables is required for this query.");
		}
		int result = 0;
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result ++;
			}
		}
		return result;
	}

	@Override
	public int countFrom(Iterable<E> source, Object... vars) {
		ph.bind(vars);
		int result = 0;
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				result ++;
			}
		}
		return result;
	}

	@Override
	public E selectOneFrom(Iterable<E> source) {
		if (hasPlaceholders) {
			throw new IllegalArgumentException("Bind variables is required for this query.");
		}
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				return elem;
			}
		}
		return null;
	}

	@Override
	public E selectOneFrom(Iterable<E> source, Object... vars) {
		ph.bind(vars);
		for (final E elem : source) {
			if (evaluate(expression, elem)) {
				return elem;
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	private boolean evaluate(Expression expr, E elem) {
		if (expr.isComparative()) {
			final Object actual = accessor.accsess(elem, expr.getProperty());
			final Operator op = expr.getOperator();
			final Object expected = expr.getValue();
			
			if (op.forNullable) {
				return nullable(actual, op);
			} else if (actual != null) {
				if (op.forObject) {
					return object(actual, expected, op);
				} else if (op.forString) {
					return string(actual, expected, op);
				} else if (op.forOrdered) {
					final Comparable[] pair = makeComparablePair(actual, expected);
					return pair[1] != null && ordered(pair[0], pair[1], op);
				}
			}
			return false;
		} else {
			final Operator op = expr.getOperator();
			if (! expr.hasLeft()) {
				if (op == Operator.NOT) {
					return ! evaluate(expr.getRight(), elem);
				} else {
					throw new RuntimeException("Unsupported logical expression.");
				}
			} else {
				final boolean leftResult = evaluate(expr.getLeft(), elem);
				if (leftResult && op == Operator.OR) {
					return true;
				} else if (!leftResult && op == Operator.AND) {
					return false;
				} else {
					return evaluate(expr.getRight(), elem);
				}
			}
		}
	}
	
	private boolean object(final Object actual, final Object expected, final Operator op) {
		final boolean asString = expected instanceof String;
		if (op == Operator.EQUALS) {
			return (asString ? actual.toString() : actual).equals(expected);
		} else if (op == Operator.NOT_EQUALS) {
			return !(asString ? actual.toString() : actual).equals(expected);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private boolean string(final Object actual, final Object expected, final Operator op) {
		final String s = actual.toString();
		final String s1 = expected.toString();
		if (op == Operator.CONTAINS) {
			return s.contains(s1);
		} else if (op == Operator.ENDS_WITH) {
			return s.endsWith(s1);
		} else if (op == Operator.STARTS_WITH) {
			return s.startsWith(s1);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private boolean nullable(final Object actual, final Operator op) {
		if (op == Operator.IS_NULL) {
			return actual == null;
		} else if (op == Operator.IS_NULL) {
			return actual != null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean ordered(final Comparable actual, final Comparable expected, final Operator op) {
		try {
			if (op == Operator.LESS_THAN) {
				return actual.compareTo(expected) < 0;
			} else if (op == Operator.LESS_THAN_EQUAL) {
				return actual.compareTo(expected) <= 0;
			} else if (op == Operator.GREATER_THAN) {
				return actual.compareTo(expected) > 0;
			} else if (op == Operator.GREATER_THAN_EQUAL) {
				return actual.compareTo(expected) >= 0;
			} else {
				throw new IllegalArgumentException();
			}
		} catch (final ClassCastException e) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	private Comparable[] makeComparablePair(Object actual, Object expected) {
		final String expectedString = expected.toString();
		final Comparable[] result = new Comparable[2];
		try {
			if (actual instanceof Integer) {
				result[0] = (Integer) actual;
				result[1] =  expected instanceof Integer ? (Comparable)expected : Integer.valueOf(expectedString);
			} else if (actual instanceof Long) {
				result[0] = (Long) actual;
				result[1] = expected instanceof Long ? (Comparable)expected : Long.valueOf(expectedString);
			} else if (actual instanceof Float) {
				result[0] = (Float) actual;
				result[1] = expected instanceof Float ? (Comparable)expected : Float.valueOf(expectedString);
			} else if (actual instanceof Double) {
				result[0] = (Double) actual;
				result[1] = expected instanceof Double ? (Comparable)expected : Double.valueOf(expectedString);
			} else if (actual instanceof Short) {
				result[0] = (Short) actual;
				result[1] = expected instanceof Short ? (Comparable)expected : Short.valueOf(expectedString);
			} else if (actual instanceof Byte) {
				result[0] = (Byte) actual;
				result[1] = expected instanceof Byte ? (Comparable)expected : Byte.valueOf(expectedString);
			} else if (actual instanceof Character && expectedString.length() == 1) {
				result[0] = (Character) actual;
				result[1] = expected instanceof Character ? (Comparable)expected : expectedString.charAt(0);
			} else if (actual instanceof BigDecimal) {
				result[0] = (BigDecimal) actual;
				result[1] = expected instanceof BigDecimal ? (Comparable)expected : new BigDecimal(expectedString);
			} else if (actual instanceof BigInteger) {
				result[0] = (BigInteger) actual;
				result[1] = expected instanceof BigInteger ? (Comparable)expected : new BigInteger(expectedString);
			} else if (actual instanceof String) {
				result[0] = actual.toString();
				result[1] = expectedString;
			} else if (actual instanceof Comparable 
					&& expected instanceof Comparable) {
				result[0] = (Comparable)actual;
				result[1] = (Comparable)expected;
			}
		} catch (final NumberFormatException e) {
			// Do nothing.
		}
		return result;
	}	
}
