package com.m12i.minque;

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

	private boolean evaluate(Expression expr, E elem) {
		if (expr.isComparative()) {
			final String actual = accessor.accsess(elem, expr.getProperty());
			final Operator op = expr.getOperator();
			final String expected = expr.getValue();
			if (op == Operator.IS_NOT_NULL) {
				return actual != null;
			} else if (op == Operator.IS_NULL) {
				return actual == null;
			} else if (op == Operator.CONTAINS) {
				return actual.contains(expected);
			} else if (op == Operator.ENDS_WITH) {
				return actual.endsWith(expected);
			} else if (op == Operator.EQUALS) {
				return expected.equals(actual);
			} else if (op == Operator.NOT_EQUALS) {
				return !expected.equals(actual);
			} else if (op == Operator.STARTS_WITH) {
				return actual.startsWith(expected);
			} else {
				throw new RuntimeException("Unsupported comparative expression.");
			}
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
	
}
