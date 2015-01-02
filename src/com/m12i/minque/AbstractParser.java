package com.m12i.minque;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.m12i.minque.Parsers.Options;

/**
 * パーサを実装する際に使用する抽象クラス.
 * @param <T> パースした結果得られるオブジェクトの型
 */
abstract class AbstractParser<T> {
	protected final Parsers.Options options;
	protected final Parsers parsers;
	public AbstractParser(final Parsers.Options options) {
		this.options = options;
		this.parsers = new Parsers(options);
	}
	public AbstractParser() {
		this.options = new Options();
		this.parsers = new Parsers(options);
	}
	/**
	 * 文字列を対象にしてパース処理を行う.
	 * @param string パース対象の文字列
	 * @return パース結果
	 */
	public final T parse(final String string) {
		try {
			return parse(Input.fromString(string));
		} catch (final InputExeption e) {
			throw new ParseException(e);
		}
	}
	/**
	 * ストリームを対象にしてパース処理を行う.
	 * ストリームから文字列をロードする際、システムのデフォルト・キャラクタセット（{@code Charset#defaultCharset()}）が使用されます。
	 * @param stream パース対象のストリーム
	 * @return パース結果
	 * @throws IOException パース中に発生したIOエラー
	 */
	public final T parse(final InputStream stream) {
		try {
			return parse(Input.fromStream(stream));
		} catch (final ParseException e) {
			throw new ParseException(e);
		} catch (final InputExeption e) {
			throw new ParseException(e);
		}
	}
	/**
	 * ストリームを対象にしてパース処理を行う.
	 * @param stream パース対象のストリーム
	 * @param charset ストリームから文字列をロードする際に使用するキャラクタセット
	 * @return パース結果
	 * @throws IOException パース中に発生したIOエラー
	 */
	public final T parse(final InputStream stream, final Charset charset) {
		try {
			return parse(Input.fromStream(stream, charset));
		} catch (InputExeption e) {
			throw new ParseException(e);
		}
	}
	
	public final T parse(final File file) {
		return parse(file, Charset.defaultCharset());
	}
	
	public final T parse(final File file, final Charset charset) {
		try {
			return parse(new FileInputStream(file), charset);
		} catch (FileNotFoundException e) {
			throw new ParseException(e);
		}
	}
	
	/**
	 * {@link Input}オブジェクトを使用してパース処理を行う.
	 * この抽象クラスを継承・拡張する具象クラスはこのメソッドを実装する必要がある。
	 * パース処理中に発生した例外は{@link ParseException}でラップして再スローすること。
	 * @param in {@link Input}オブジェクト
	 * @return パース結果
	 */
	public abstract T parse(final Input in);
}
