package com.m12i.minque;

/**
 * クエリ文字列をパースして式オブジェクトとプレースホルダ管理オブジェクトを返すパーサー.
 */
final class ExpressionParser extends AbstractParser<ExpressionParser.ExpressionAndPlaceholders> {
	/**
	 * 式オブジェクトとプレースホルダ管理オブジェクトをまとめるコンテナ・オブジェクト.
	 */
	static final class ExpressionAndPlaceholders {
		/**
		 * クエリ文字列の解析結果から作られた式オブジェクト.
		 */
		final Expression expression;
		/**
		 * プレースホルダ管理オブジェクト.
		 */
		final Placeholders placeholders;
		private ExpressionAndPlaceholders(final Expression e, final Placeholders ph) {
			this.expression = e;
			this.placeholders = ph;
		}
	}
	
	public ExpressionAndPlaceholders parse(final Input in) {
		// 何はともあれ空白文字をスキップ
		parsers.skipWhitespace(in);
		if (in.reachedEof()) {
			// 空文字列もしくは空白文字のみからなるクエリの場合はエラー
			throw new ParseException("Empty string.", in);
		}
		
		// それ以外の場合は再帰的にクエリを構文解析
		try {
			final Placeholders ph = new Placeholders();
			final Expression e = parseExpression(in, true, ph);
			return new ExpressionAndPlaceholders(e, ph);
		} catch (final InputExeption e) {
			throw new ParseException(e);
		}
	}
	
	/**
	 * 式（論理演算・比較演算）をパースして返す.
	 * 再帰下降オプションは式のパースが「左結合」で実行されるようにするためのパラメータ。
	 * @param in 入力データ
	 * @param recursive 再帰下降オプション
	 * @return パースした式
	 * @throws InputExeption 入力データ読み取り中にエラーが発生した場合
	 */
	private Expression parseExpression(final Input in, final boolean recursive, final Placeholders ph) throws InputExeption {
		// 再帰呼び出し時を想定してとにかく空白文字をスキップ
		parsers.skipWhitespace(in);
		// 現在文字をチェック
		if (in.current() == '!') {
			// 現在文字が"!"であれば単行論理演算子とみなして処理を進める
			in.next();
			parsers.skipWhitespace(in);
			// 単行論理演算子と右辺の式（これから再帰的にパースされる）をもとに論理演算オブジェクトを生成して返す
			final Expression expr0 = parseExpression(in, true, ph);
			return Expression.logical(Operator.NOT, expr0);
		} else if(in.current() == '(') {
			// 現在文字が"("であれば結合方法を制御する丸括弧の開始とみなして処理を進める
			in.next();
			// 括弧の内側にあるのはともかく何かしらの式だからそれをパースする
			final Expression e = parseExpression(in, true, ph);
			// 式の後の空白文字をスキップ
			parsers.skipWhitespace(in);
			// 式の後には")"がつづく
			if (in.current() != ')') {
				throw ParseException.arg1ExpectedButFoundArg2(in, ')', in.current());
			}
			in.next();
			// 再帰下降オプションをチェック
			if (!recursive) {
				// 再帰下降を許されていない場合は先ほど読み取った式を返す
				return e;
			} else {
				// 再帰下降を許されている場合は後続の論理演算（もしあれば）も含めて処理した結果を返す
				return parseLogical(in, e, ph);
			}
		} else {
			// 現在文字が単行論理演算子でも丸括弧でもなかった場合は比較演算とみなして処理を進める
			// まず現在文字をチェックしてプロパティ記述をパースする
			final String prop = (in.current() == '"' || in.current() == '\'' 
					? parsers.parseQuotedString(in) : parseNonQuotedString(in));
			// 空白文字をスキップ
			parsers.skipWhitespace(in);
			// 次に演算子をパースする
			final Operator op = parseComparativeOperator(in);
			// 演算子が見つからなかった場合boolean型のプロパティとみなして式を補う
			if (op == null) {
				return Expression.comparative(Expression.property(prop), Operator.EQUALS, Expression.value(Boolean.TRUE));
			}
			
			// 空白文字をスキップ
			parsers.skipWhitespace(in);
			// 演算子の種類をチェックして値の記述をパースする
			final Expression expr0;
			// プロパティ、演算子、値の3値から比較演算オブジェクトを構成する
			if (op == Operator.IS_NOT_NULL || op == Operator.IS_NULL) { 
				expr0 = Expression.comparative(Expression.property(prop), op, Expression.value(null));
			} else {
				final String value;
				final Expression valExp;
				final char c = in.current();
				if (c == '"' || c == '\'') {
					// 現在文字が引用符である場合は、引用符で囲われた文字列としてパース
					value = parsers.parseQuotedString(in);
					valExp = Expression.value(value);
				} else {
					// 空白文字と特定の記号を含まない文字列としてパース
					value = parseNonQuotedString(in);
					valExp = Expression.value(value);
					if (value.equals("?")){
						ph.register(valExp);
					}
				}
				expr0 = Expression.comparative(Expression.property(prop), op, valExp);
			}
			// 再帰下降オプションをチェック
			if (!recursive) {
				// 再帰下降を許されていない場合は先ほど読み取った式を返す
				return expr0;
			} else {
				// 再帰下降を許されている場合は後続の論理演算（もしあれば）も含めて処理した結果を返す
				return parseLogical(in, expr0, ph);
			}
			
		}
	}
	
	/**
	 * 論理演算式をパースして返す.
	 * 引数として左辺にあたる式を受け取り、後続のクエリ文字列から論理演算子と右辺の式を読み取って、その3値から論理演算子式を構成して返す。
	 * 後続のクエリ文字列に論理演算子が見つからなかった場合は、引数として与えられた式をそのまま返す。
	 * @param in 入力データ
	 * @param left 論理演算の左辺
	 * @param ph プレースホルダ管理オブジェクト
	 * @return パースした式
	 * @throws InputExeption 入力データ読み取り中にエラーが発生した場合
	 */
	private Expression parseLogical(final Input in, final Expression left, final Placeholders ph) throws InputExeption {
		// まずは空白文字をスキップ
		parsers.skipWhitespace(in);
		// 現在文字をチェックして処理分岐
		final char c0 = in.current();
		if (c0 == 'a') {
			// 現在文字が"a"ならば"nd"が続かない限り構文エラー
			parsers.skipWord(in, "and");
			in.next();
			parsers.skipWhitespace(in);
			// 引数として渡された式（左辺）と最前パースした演算子、
			// そしてこれからパースする式（右辺）で論理演算オブジェクトを構成して返す
			// ＊ただし右辺の式をパースするにあたり「右結合」にならないよう再帰下降オプションはOFFにする
			final Expression right = parseExpression(in, false, ph);
			return parseLogical(in, Expression.logical(left, Operator.AND, right), ph);
		} else if (c0 == '&') {
			// 現在文字が"&"ならば"&"が続かない限り構文エラー
			parsers.skipWord(in, "&&");
			in.next();
			parsers.skipWhitespace(in);
			// "and"の場合と同様に処理
			final Expression right = parseExpression(in, false, ph);
			return parseLogical(in, Expression.logical(left, Operator.AND, right), ph);
		} else if (c0 == 'o') {
			// 現在文字が"o"ならば"r"が続かない限り構文エラー
			parsers.skipWord(in, "or");
			in.next();
			parsers.skipWhitespace(in);
			// "and"の場合と同様に処理
			final Expression right = parseExpression(in, false, ph);
			return parseLogical(in, Expression.logical(left, Operator.OR, right), ph);
		} else if (c0 == '|') {
			// 現在文字が"|"ならば"|"が続かない限り構文エラー
			parsers.skipWord(in, "||");
			in.next();
			parsers.skipWhitespace(in);
			// "and"の場合と同様に処理
			final Expression right = parseExpression(in, false, ph);
			return parseLogical(in, Expression.logical(left, Operator.OR, right), ph);
		} else {
			// 上記いずれのケースにも該当しないならば論理演算（演算子と右辺）は
			// 存在しないということだから引数として渡された式（左辺になるはずだった）をそのまま返す
			return left;
		}
	}
	
	/**
	 * 引用符なしで記述された文字列（プロパティもしくは値）をパースして返す.
	 * 使用可能な文字は空白文字でなくかつ{@code ")=!^*$<>&|"}に含まれないもののみ。
	 * @param in 入力データ
	 * @return パースした文字列
	 * @throws InputExeption 入力データ読み取り中にエラーが発生した場合
	 */
	private String parseNonQuotedString(final Input in) throws InputExeption {
		// パースした文字列を格納するバッファを初期化
		final StringBuilder sb = new StringBuilder();
		// 文字列の終端に達するまで繰り返し処理
		while (in.unlessEof()) {
			// 現在文字をチェック
			final char c = in.current();
			if (c <= ' ' || ")=!^*$<>&|".indexOf(c) != -1) {
				// 許された文字以外が登場したらそこで処理を終える
				break;
			}
			// 許された文字列であればバッファに追加して次に文字に進む
			sb.append(c);
			in.next();
		}
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			throw new ParseException("Value is not found.", in);
		}
	}
	
	/**
	 * 演算子をパースして返す.
	 * @param in 入力データ
	 * @return パースした演算子
	 * @throws InputExeption 入力データ読み取り中にエラーが発生した場合
	 */
	private Operator parseComparativeOperator(final Input in) throws InputExeption {
		if (forwardIfRestStartsWithOprator(in, "==")) {
			return Operator.EQUALS;
		} else if (forwardIfRestStartsWithOprator(in, "!=")) {
			return Operator.NOT_EQUALS;
		} else if (forwardIfRestStartsWithOprator(in, "^=")) {
			return Operator.STARTS_WITH;
		} else if (forwardIfRestStartsWithOprator(in, "*=")) {
			return Operator.CONTAINS;
		} else if (forwardIfRestStartsWithOprator(in, "$=")) {
			return Operator.ENDS_WITH;
		} else if (forwardIfRestStartsWithOprator(in, "<")) {
			return Operator.LESS_THAN;
		} else if (forwardIfRestStartsWithOprator(in, "<=")) {
			return Operator.LESS_THAN_EQUAL;
		} else if (forwardIfRestStartsWithOprator(in, ">")) {
			return Operator.GREATER_THAN;
		} else if (forwardIfRestStartsWithOprator(in, ">=")) {
			return Operator.GREATER_THAN_EQUAL;
		} else if (forwardIfRestStartsWithOprator(in, "is null")) {
			return Operator.IS_NULL;
		} else if (forwardIfRestStartsWithOprator(in, "is not null")) {
			return Operator.IS_NOT_NULL;
		} else {
			return null;
		}
	}
	
	private boolean forwardIfRestStartsWithOprator(final Input in, final String op) throws InputExeption {
		if (in.restStartsWith(op)) {
			for (int i = 0; i < op.length(); i ++) {
				in.next();
			}
			return true;
		} else {
			return false;
		}
	}
}
