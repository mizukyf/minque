package com.m12i.minque;

import java.util.ArrayList;
import java.util.List;

final class Placeholders {
	private final List<Expression> list = new ArrayList<Expression>();
	
	void register(final Expression e) {
		list.add(e);
	}
	
	int amount() {
		return list.size();
	}
	
	void bind(final Object... vars) {
		final int listSize = list.size();
		if (vars.length != listSize) {
			throw new IllegalArgumentException(
					String.format("Number of bind variables must be %s.",
							list.size()));
		}
		for (int i = 0; i < listSize; i++) {
			list.get(i).setValue(vars[i].toString());
		}
	}
}
