package com.jssh.netty.serial;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import io.netty.buffer.ByteBuf;

public class ChunkFile {

	private File srcFile;
	private long length;

	private RandomAccessFile outfile;
	private FileChannel output;
	private long pos;

	public ChunkFile(String file) {
		this(new File(file));
	}

	public ChunkFile(File file) {
		this(file, file.length());
	}

	public ChunkFile(File file, long length) {
		this.srcFile = file;
		this.length = length;
	}

	public synchronized boolean writeFrom(ByteBuf in) throws IOException {
		if (output == null) {
			outfile = new RandomAccessFile(srcFile, "rw");
			outfile.setLength(length);
			output = outfile.getChannel();
		}
		if (pos < length) {
			int read = 0;
			if (in != null && (read = Math.min(in.readableBytes(), (int) (length - pos))) > 0) {
				in.readBytes(output, pos, read);
				pos += read;
			}
		}
		return pos < length;
	}

	public void saveTo(File destFile) throws IOException {
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		try (RandomAccessFile from = new RandomAccessFile(srcFile, "r");
				RandomAccessFile to = new RandomAccessFile(destFile, "rw")) {
			from.getChannel().transferTo(0, length, to.getChannel());
		}
	}

	public boolean remove() {
		close();
		return srcFile.delete();
	}

	public void close() {
		if (output != null) {
			try {
				output.close();
			} catch (Exception e) {
			}
		}
		if (outfile != null) {
			try {
				outfile.close();
			} catch (Exception e) {
			}
		}
	}

	public long getPos() {
		return pos;
	}

	public void setPos(long pos) {
		this.pos = pos;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public File getSrcFile() {
		return srcFile;
	}

	public void setSrcFile(File srcFile) {
		this.srcFile = srcFile;
	}

	@Override
	protected void finalize() throws Throwable {
		this.close();

		if (this.srcFile != null && srcFile.getName().endsWith(".file-cache")) {
			this.srcFile.delete();
		}

		super.finalize();
	}
}
