package com.m12i.minque;

import java.util.ArrayList;
import java.util.List;

/**
 * バインド変数のためのプレースホルダを管理するオブジェクト.
 */
final class Placeholders {
	private final List<Expression> list = new ArrayList<Expression>();
	
	/**
	 * プレースホルダへの参照を追加する.
	 * @param e プレースホルダ
	 */
	void register(final Expression e) {
		list.add(e);
	}
	/**
	 * プレースホルダの総数を返す.
	 * @return プレースホルダの総数
	 */
	int amount() {
		return list.size();
	}
	/**
	 * プレースホルダに変数をバインドする.
	 * @param vars 変数
	 */
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
