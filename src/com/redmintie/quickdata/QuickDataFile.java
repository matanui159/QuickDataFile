package com.redmintie.quickdata;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map.Entry;

public class QuickDataFile implements Closeable, AutoCloseable {
	public static final int BYTE = 1;
	public static final int SHORT = 2;
	public static final int INTEGER = 4;
	public static final int LONG = 8;
	public static final int FLOAT = 20;
	public static final int DOUBLE = 24;
	public static final int BOOLEAN = 32;
	public static final int STRING = 34;
	
	private static final int FALSE = 32;
	private static final int TRUE = 33;
	
	private RandomAccessFile file;
	private HashMap<String, Long> values = new HashMap<String, Long>();
	public QuickDataFile(String file) throws IOException {
		this(new File(file));
	}
	public QuickDataFile(File file) throws IOException {
		this.file = new RandomAccessFile(file, "rws");
		try {
			defrag();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.err.println("Could not load saved data: corrupted file.");
			values.clear();
			this.file.setLength(0);
		}
	}
	public void defrag() throws IOException {
		file.seek(0);
		HashMap<String, Object> values = new HashMap<String, Object>();
		while (file.getFilePointer() < file.length()) {
			String name = file.readUTF();
			int type = file.readByte();
			switch (type) {
			case BYTE:
				values.put(name, file.readByte());
				break;
			case SHORT:
				values.put(name, file.readShort());
				break;
			case INTEGER:
				values.put(name, file.readInt());
				break;
			case LONG:
				values.put(name, file.readLong());
				break;
			case FLOAT:
				values.put(name, file.readFloat());
				break;
			case DOUBLE:
				values.put(name, file.readDouble());
				break;
			case FALSE:
			case TRUE:
				values.put(name, type == TRUE);
				break;
			case STRING:
				values.put(name, file.readUTF());
				file.skipBytes(file.readUnsignedShort());
				break;
			default:
				throw new IOException("Unknown value type: 0x" + Integer.toHexString(type) + ".");
			}
		}
		
		file.seek(0);
		for (Entry<String, Object> entry : values.entrySet()) {
			file.writeUTF(entry.getKey());
			this.values.put(entry.getKey(), file.getFilePointer());
			if (entry.getValue() instanceof Integer) {
				file.writeByte(INTEGER);
				file.writeInt((Integer)entry.getValue());
			}
			if (entry.getValue() instanceof Double) {
				file.writeByte(DOUBLE);
				file.writeDouble((Double)entry.getValue());
			}
			if (entry.getValue() instanceof String) {
				file.writeByte(STRING);
				file.writeUTF((String)entry.getValue());
				file.writeShort(0);
			}
		}
		file.setLength(file.getFilePointer());
	}
	private int storeHeader(String name, int type, int size) throws IOException {
		if (values.containsKey(name)) {
			file.seek(values.get(name));
			if (file.readByte() == type) {
				if (type == STRING) {
					long pointer = file.getFilePointer();
					int s  = getUTFSize(file.readUTF()) + file.readUnsignedShort();
					if (size <= s) {
						file.seek(pointer);
						return s;
					}
				} else {
					return 0;
				}
			}
		}
		file.seek(file.length());
		file.writeUTF(name);
		values.put(name, file.getFilePointer());
		file.writeByte(type);
		return size;
	}
	private void loadHeader(String name, int type, String desc) throws IOException {
		if (!values.containsKey(name)) {
			throw new IllegalArgumentException(name + " does not exist.");
		}
		file.seek(values.get(name));
		if (file.readByte() != type) {
			throw new IllegalArgumentException(name + " is not " + desc + ".");
		}
	}
	public void storeInt(String name, int value) throws IOException {
		storeHeader(name, INTEGER, 0);
		file.writeInt(value);
	}
	public int loadInt(String name) throws IOException {
		loadHeader(name, INTEGER, "an integer");
		return file.readInt();
	}
	public void storeDouble(String name, double value) throws IOException {
		storeHeader(name, DOUBLE, 0);
		file.writeDouble(value);
	}
	public double loadDouble(String name) throws IOException {
		loadHeader(name, DOUBLE, "a double");
		return file.readDouble();
	}
	private int getUTFSize(String value) {
		int size = 4;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				size += 1;
			} else if (c > 0x07FF) {
				size += 3;
			} else {
				size += 2;
			}
		}
		return size;
	}
	public void storeString(String name, String value) throws IOException {
		int size = getUTFSize(value);
		size = storeHeader(name, STRING, size) - size;
		file.writeUTF(value);
		file.writeShort(size);
	}
	public String loadString(String name) throws IOException {
		loadHeader(name, STRING, "a string");
		return file.readUTF();
	}
	@Override
	public void close() throws IOException {
		file.close();
	}
}