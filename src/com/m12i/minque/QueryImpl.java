package com.m12i.minque;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean evaluate(Expression expr, E elem) {
		if (expr.isComparative()) {
			final Object actual = accessor.accsess(elem, expr.getProperty());
			final Operator op = expr.getOperator();
			final String expected = expr.getValue();
			
			if (op.forNullable) {
				return nullable(actual, op);
			} else if (actual != null) {
				if (op.forString) {
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
	
	private boolean string(final Object actual, final String expected, final Operator op) {
		final String s = actual.toString();
		if (op == Operator.CONTAINS) {
			return s.contains(expected);
		} else if (op == Operator.ENDS_WITH) {
			return s.endsWith(expected);
		} else if (op == Operator.EQUALS) {
			return expected.equals(s);
		} else if (op == Operator.NOT_EQUALS) {
			return !expected.equals(s);
		} else if (op == Operator.STARTS_WITH) {
			return s.startsWith(expected);
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
	}
	
	@SuppressWarnings("rawtypes")
	private Comparable[] makeComparablePair(Object actual, String expected) {
		final Comparable[] result = new Comparable[2];
		try {
			if (actual instanceof Integer) {
				result[0] = (Integer) actual;
				result[1] = Integer.valueOf(expected);
			} else if (actual instanceof Long) {
				result[0] = (Long) actual;
				result[1] = Long.valueOf(expected);
			} else if (actual instanceof Float) {
				result[0] = (Float) actual;
				result[1] = Float.valueOf(expected);
			} else if (actual instanceof Double) {
				result[0] = (Double) actual;
				result[1] = Double.valueOf(expected);
			} else if (actual instanceof Short) {
				result[0] = (Short) actual;
				result[1] = Short.valueOf(expected);
			} else if (actual instanceof Byte) {
				result[0] = (Byte) actual;
				result[1] = Byte.valueOf(expected);
			} else if (actual instanceof Character && expected.length() == 1) {
				result[0] = (Character) actual;
				result[1] = expected.charAt(0);
			} else if (actual instanceof BigDecimal) {
				result[0] = (BigDecimal) actual;
				result[1] = new BigDecimal(expected);
			} else if (actual instanceof BigInteger) {
				result[0] = (BigInteger) actual;
				result[1] = new BigInteger(expected);
			} else if (actual instanceof String) {
				result[0] = actual.toString();
				result[1] = expected;
			}
		} catch (final NumberFormatException e) {
			// Do nothing.
		}
		return result;
	}	
}
