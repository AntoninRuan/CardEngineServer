package fr.antoninruan.maoserver.utils.shell;

import org.jline.reader.LineReader;

import java.io.*;
import java.nio.charset.Charset;

public class CustomPrintStream extends PrintStream {

    private final LineReader lineReader;
    private final String prefix;

    public CustomPrintStream(OutputStream out, LineReader lineReader, String prefix) {
        super(out);
        this.lineReader = lineReader;
        this.prefix = prefix;
    }

    public CustomPrintStream(OutputStream out, boolean autoFlush, LineReader lineReader, String prefix) {
        super(out, autoFlush);
        this.lineReader = lineReader;
        this.prefix = prefix;
    }

    public CustomPrintStream(OutputStream out, boolean autoFlush, String encoding, LineReader lineReader, String prefix) throws UnsupportedEncodingException {
        super(out, autoFlush, encoding);
        this.lineReader = lineReader;
        this.prefix = prefix;
    }

    public CustomPrintStream(OutputStream out, boolean autoFlush, Charset charset, LineReader lineReader, String prefix) {
        super(out, autoFlush, charset);
        this.lineReader = lineReader;
        this.prefix = prefix;
    }

    @Override
    public void print(boolean b) {
    }

    @Override
    public void print(char c) {
    }

    @Override
    public void print(int i) {
    }

    @Override
    public void print(long l) {
    }

    @Override
    public void print(float f) {
    }

    @Override
    public void print(double d) {
    }

    @Override
    public void print(char[] s) {
    }

    @Override
    public void print(String s) {
    }

    @Override
    public void print(Object obj) {
    }

    @Override
    public void println() {
        lineReader.printAbove("");
    }

    @Override
    public void println(boolean x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(char x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(int x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(long x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(float x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(double x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(char[] x) {
        lineReader.printAbove(prefix + ": " + new String(x));
    }

    @Override
    public void println(String x) {
        lineReader.printAbove(prefix + ": " + x);
    }

    @Override
    public void println(Object x) {
        lineReader.printAbove(prefix + ": " + x.toString());
    }
}
