package com.m12i.minque;

enum Operator {
	AND, OR, NOT,
	EQUALS, NOT_EQUALS, STARTS_WITH, ENDS_WITH, CONTAINS, IS_NULL, IS_NOT_NULL,
	LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL;
	public final boolean forNullable;
	public final boolean forObject;
	public final boolean forString;
	public final boolean forOrdered;
	private Operator() {
		final String n = this.name();
		this.forNullable = n.contains("NULL");
		this.forObject = n.contains("EQUALS");
		this.forOrdered = n.contains("_THAN");
		this.forString = n.contains("_WITH") || n.equals("CONTAINS");
	}
}
