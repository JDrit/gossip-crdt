package net.batchik.crdt.fiber;

/*
 * Copyright 2001-2005 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.*;
import java.util.concurrent.locks.ReentrantLock;

public class FiberStreamEncoder extends Writer {
    private static Logger logger = LogManager.getLogger(FiberStreamEncoder.class);

    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;

    private volatile boolean isOpen = true;

    private void ensureOpen() throws IOException {
        if (!isOpen)
            throw new IOException("Stream closed");
    }

    // Factories for java.io.OutputStreamWriter
    public static FiberStreamEncoder forOutputStreamWriter(OutputStream out,
                                                           ReentrantLock lock,
                                                           String charsetName)
            throws UnsupportedEncodingException, SuspendExecution
    {
        String csn = charsetName;
        if (csn == null)
            csn = Charset.defaultCharset().name();
        try {
            if (Charset.isSupported(csn))
                return new FiberStreamEncoder(out, lock, Charset.forName(csn));
        } catch (IllegalCharsetNameException x) { }
        throw new UnsupportedEncodingException (csn);
    }

    public static FiberStreamEncoder forOutputStreamWriter(OutputStream out,
                                                           ReentrantLock lock,
                                                           Charset cs) throws SuspendExecution
    {
        return new FiberStreamEncoder(out, lock, cs);
    }

    public static FiberStreamEncoder forOutputStreamWriter(OutputStream out,
                                                           ReentrantLock lock,
                                                           CharsetEncoder enc) throws SuspendExecution
    {
        return new FiberStreamEncoder(out, lock, enc);
    }


    // Factory for java.nio.channels.Channels.newWriter
    public static FiberStreamEncoder forEncoder(WritableByteChannel ch,
                                                CharsetEncoder enc,
                                                int minBufferCap) throws SuspendExecution
    {
        return new FiberStreamEncoder(ch, enc, minBufferCap);
    }


    // -- Public methods corresponding to those in OutputStreamWriter --

    public String getEncoding() {
        if (isOpen())
            return encodingName();
        return null;
    }

    public void flushBuffer() throws IOException, SuspendExecution {
        if (isOpen())
            implFlushBuffer();
        else
            throw new IOException("Stream closed");
    }

    @Override
    @Suspendable
    public void write(int c) throws IOException {
        char cbuf[] = new char[1];
        cbuf[0] = (char) c;
        write(cbuf, 0, 1);
    }

    @Override
    @Suspendable
    public void write(char cbuf[], int off, int len) throws IOException {
        ensureOpen();
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        try {
            implWrite(cbuf, off, len);
        } catch (SuspendExecution suspendExecution) {
            throw new AssertionError(suspendExecution);
        }
    }

    @Override
    @Suspendable
    public void write(String str, int off, int len) throws IOException {
        /* Check the len before creating a char buffer */
        if (len < 0)
            throw new IndexOutOfBoundsException();
        char cbuf[] = new char[len];
        str.getChars(off, off + len, cbuf, 0);
        write(cbuf, 0, len);
    }

    @Override
    @Suspendable
    public void flush() throws IOException {
        ensureOpen();
        try {
            implFlush();
        } catch (SuspendExecution suspendExecution) {
            throw new AssertionError(suspendExecution);
        }
    }

    @Override
    @Suspendable
    public void close() throws IOException {
        if (!isOpen)
            return;
        try {
            implClose();
        } catch (SuspendExecution suspendExecution) {
            throw new AssertionError(suspendExecution);
        }
        isOpen = false;
    }

    private boolean isOpen() {
        return isOpen;
    }


    // -- Charset-based stream encoder impl --

    private Charset cs;
    private CharsetEncoder encoder;
    private ByteBuffer bb;

    // Exactly one of these is non-null
    private final OutputStream out;
    private WritableByteChannel ch;

    // Leftover first char in a surrogate pair
    private boolean haveLeftoverChar = false;
    private char leftoverChar;
    private CharBuffer lcb = null;

    private FiberStreamEncoder(OutputStream out, ReentrantLock lock, Charset cs) {
        this(out, lock,
                cs.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    private FiberStreamEncoder(OutputStream out, ReentrantLock lock, CharsetEncoder enc) {
        super(lock);
        this.out = out;
        this.ch = null;
        this.cs = enc.charset();
        this.encoder = enc;

        // This path disabled until direct buffers are faster
        if (false && out instanceof FileOutputStream) {
            ch = ((FileOutputStream)out).getChannel();
            if (ch != null)
                bb = ByteBuffer.allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
        }
        if (ch == null) {
            bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
        }
    }

    private FiberStreamEncoder(WritableByteChannel ch, CharsetEncoder enc, int mbc) {
        this.out = null;
        this.ch = ch;
        this.cs = enc.charset();
        this.encoder = enc;
        this.bb = ByteBuffer.allocate(mbc < 0
                ? DEFAULT_BYTE_BUFFER_SIZE
                : mbc);
    }

    private void writeBytes() throws IOException, SuspendExecution {
        bb.flip();
        int lim = bb.limit();
        int pos = bb.position();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (rem > 0) {
            if (ch != null) {
                if (ch.write(bb) != rem)
                    assert false : rem;
            } else {
                out.write(bb.array(), bb.arrayOffset() + pos, rem);
            }
        }
        bb.clear();
    }

    private void flushLeftoverChar(CharBuffer cb, boolean endOfInput)
            throws IOException, SuspendExecution
    {
        if (!haveLeftoverChar && !endOfInput)
            return;
        if (lcb == null)
            lcb = CharBuffer.allocate(2);
        else
            lcb.clear();
        if (haveLeftoverChar)
            lcb.put(leftoverChar);
        if ((cb != null) && cb.hasRemaining())
            lcb.put(cb.get());
        lcb.flip();
        while (lcb.hasRemaining() || endOfInput) {
            CoderResult cr = encoder.encode(lcb, bb, endOfInput);
            if (cr.isUnderflow()) {
                if (lcb.hasRemaining()) {
                    leftoverChar = lcb.get();
                    if (cb != null && cb.hasRemaining())
                        flushLeftoverChar(cb, endOfInput);
                    return;
                }
                break;
            }
            if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes();
                continue;
            }
            cr.throwException();
        }
        haveLeftoverChar = false;
    }

    void implWrite(char cbuf[], int off, int len)
            throws IOException, SuspendExecution
    {
        CharBuffer cb = CharBuffer.wrap(cbuf, off, len);

        if (haveLeftoverChar)
            flushLeftoverChar(cb, false);

        while (cb.hasRemaining()) {
            CoderResult cr = encoder.encode(cb, bb, false);
            if (cr.isUnderflow()) {
                assert (cb.remaining() <= 1) : cb.remaining();
                if (cb.remaining() == 1) {
                    haveLeftoverChar = true;
                    leftoverChar = cb.get();
                }
                break;
            }
            if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes();
                continue;
            }
            cr.throwException();
        }
    }

    void implFlushBuffer() throws IOException, SuspendExecution {
        if (bb.position() > 0)
            writeBytes();
    }

    void implFlush() throws IOException, SuspendExecution {
        implFlushBuffer();
        if (out != null)
            out.flush();
    }

    void implClose() throws IOException, SuspendExecution {
        flushLeftoverChar(null, true);
        try {
            for (;;) {
                CoderResult cr = encoder.flush(bb);
                if (cr.isUnderflow())
                    break;
                if (cr.isOverflow()) {
                    assert bb.position() > 0;
                    writeBytes();
                    continue;
                }
                cr.throwException();
            }

            if (bb.position() > 0)
                writeBytes();
            if (ch != null)
                ch.close();
            else
                out.close();
        } catch (IOException x) {
            encoder.reset();
            throw x;
        }
    }

    String encodingName() {
        return cs.name();
    }
}