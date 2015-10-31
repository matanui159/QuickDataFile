package com.redmintie.quickdata;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map.Entry;

public class QuickDataFile implements Closeable, AutoCloseable {
	public static final int NONE = 0;
	public static final int BYTE = 1;
	public static final int INTEGER = 4;
	public static final int LONG = 8;
	public static final int FLOAT = 20;
	public static final int DOUBLE = 24;
	public static final int STRING = 32;
	
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
			clear();
		}
	}
	public synchronized void defrag() throws IOException {
		file.seek(0);
		HashMap<String, Object> values = new HashMap<String, Object>();
		while (file.getFilePointer() < file.length()) {
			String name = file.readUTF();
			int type = file.readByte();
			switch (type) {
			case BYTE:
				values.put(name, file.readByte());
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
			case STRING:
				values.put(name, file.readUTF());
				file.skipBytes(file.readUnsignedShort());
				break;
			default:
				throw new IOException("Unknown value type: 0x" + Integer.toHexString(type) + ".");
			}
		}
		
		clear();
		for (Entry<String, Object> entry : values.entrySet()) {
			file.writeUTF(entry.getKey());
			this.values.put(entry.getKey(), file.getFilePointer());
			
			Object value = entry.getValue();
			if (value instanceof Byte) {
				file.writeByte(BYTE);
				file.writeByte((Byte)value);
			}
			if (value instanceof Integer) {
				file.writeByte(INTEGER);
				file.writeInt((Integer)value);
			}
			if (value instanceof Long) {
				file.writeByte(LONG);
				file.writeLong((Long)value);
			}
			if (value instanceof Float) {
				file.writeByte(FLOAT);
				file.writeFloat((Float)value);
			}
			if (value instanceof Double) {
				file.writeByte(DOUBLE);
				file.writeDouble((Double)value);
			}
			if (value instanceof String) {
				file.writeByte(STRING);
				file.writeUTF((String)value);
				file.writeShort(0);
			}
		}
	}
	private int getSize(int type, String value) throws IOException {
		if (type == STRING) {
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
		} else {
			return type & 0xF;
		}
	}
	private int saveHeader(String name, int type, String value) throws IOException {
		if (values.containsKey(name)) {
			long pointer = values.get(name);
			file.seek(pointer);
			
			int size = getSize(type, value);
			int t = file.readByte();
			String v = null;
			if (t == STRING) {
				v = file.readUTF();
			}
			int s = getSize(t, v);
			if (size == s || (t == STRING && size < s)) {
				file.seek(pointer);
				file.writeByte(type);
				return s - size;
			}
		}
		file.seek(file.length());
		file.writeUTF(name);
		values.put(name, file.getFilePointer());
		file.writeByte(type);
		return 0;
	}
	public synchronized int loadType(String name) throws IOException {
		if (values.containsKey(name)) {
			file.seek(values.get(name));
			return file.readByte();
		} else {
			return NONE;
		}
	}
	private void loadHeader(String name, int type, String desc) throws IOException {
		if (loadType(name) != type) {
			throw new IllegalArgumentException(name + " is not " + desc + ".");
		}
	}
	public synchronized void saveByte(String name, int value) throws IOException {
		saveHeader(name, BYTE, null);
		file.writeByte(value);
	}
	public synchronized byte loadByte(String name) throws IOException {
		loadHeader(name, BYTE, "a byte");
		return file.readByte();
	}
	public synchronized void saveInt(String name, int value) throws IOException {
		saveHeader(name, INTEGER, null);
		file.writeInt(value);
	}
	public synchronized int loadInt(String name) throws IOException {
		loadHeader(name, INTEGER, "an integer");
		return file.readInt();
	}
	public synchronized void saveLong(String name, long value) throws IOException {
		saveHeader(name, LONG, null);
		file.writeLong(value);
	}
	public synchronized long loadLong(String name) throws IOException {
		loadHeader(name, LONG, "a long");
		return file.readLong();
	}
	public synchronized void saveFloat(String name, float value) throws IOException {
		saveHeader(name, FLOAT, null);
		file.writeFloat(value);
	}
	public synchronized float loadFloat(String name) throws IOException {
		loadHeader(name, FLOAT, "a float");
		return file.readFloat();
	}
	public synchronized void saveDouble(String name, double value) throws IOException {
		saveHeader(name, DOUBLE, null);
		file.writeDouble(value);
	}
	public synchronized double loadDouble(String name) throws IOException {
		loadHeader(name, DOUBLE, "a double");
		return file.readDouble();
	}
	public synchronized void saveString(String name, String value) throws IOException {
		int diff = saveHeader(name, STRING, value);
		file.writeUTF(value);
		file.writeShort(diff);
	}
	public synchronized String loadString(String name) throws IOException {
		loadHeader(name, STRING, "a string");
		return file.readUTF();
	}
	public synchronized void clear() throws IOException {
		values.clear();
		file.setLength(0);
	}
	@Override
	public void close() throws IOException {
		file.close();
	}
}