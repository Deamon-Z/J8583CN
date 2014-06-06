package org.zyp.cn8583;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ����һ���й����8583��ʽ��׼���࣬��ʼ������Դ��IsoMessage�ࡣ
 * This is the core class of the framework.
 * Contains the bitmap which is modified as fields are added/removed.
 * This class makes no assumptions as to what types belong in each field,
 * nor what fields should each different message type have; that is left
 * for the developer
 * @author zyplanke
 */
public class cnMessage {

	/** The message type id. */
    private String msgtypeid;
    /** Indicates if the message is binary-coded. */
    private boolean isbinary;
    /** This is where the field values are stored. */
    private Map<Integer,cnValue<?>> fields = new ConcurrentHashMap<Integer,cnValue<?>>();
    /** Stores the optional 8583 header. */
    private byte[] msgHeader;
    private int etx = -1;

    public cnMessage() {
    }

/**
 * Creates a new message with the specified 8583 header attributes .
 * @param headerlength  ����ͷ����������ͷ���ĳ��ȣ���λ�ֽڣ�
 */
    public cnMessage(String msgtypeid, int headerlength) {
    	this.msgtypeid = msgtypeid;
    	msgHeader = new byte[headerlength];
    }

    /** Returns the 8583 total header that this message was created with. */
    public byte[] getmsgHeader() {
    	return msgHeader;		
    }
    

    /**
     * ���ñ���ͷ�����ݣ����ڲ�ͬ�ı��ı��ĵĸ�ʽ��ȫ��ͬ������ֱ�����ñ��ĵ��ֽ����ݡ�
     * @param startindex �����ñ���ͷ����ʼ�ֽ�λ�á���0Ϊ��һ��λ�ã�
     * @param data Ҫ���õ����ݣ�������Ϊdata�ĳ��ȣ�startindex��data�ĳ��ȵĺ�ӦС�ڱ���ͷ���ܳ��ȣ�
     * @return �Ƿ����óɹ�
     */
    public boolean setMessageHeaderData(int startindex,  byte[] data) {
    	if(startindex + data.length > msgHeader.length) {
    		return false;
    	}
    	for(int i = 0; i < data.length; i++) {
    		msgHeader[startindex + i] = data[i];
    	}
    	return true;		
    }
  
    /**
     * �ӱ���ͷ��ȡ������
     * @param startindex	��ʼ�ֽ�λ�ã�0Ϊ��һ��λ�ã�ӦС�ڱ���ͷ���ܳ��ȣ�
     * @param count	��Ҫȡ�õ��ֽ������������� ����������β����ȡ��ʵ����ȡ��������ֽ���
     * @return ȡ�õ����ݣ���δȡ���򷵻�null��
     */
    public byte[] getMessageHeaderData(int startindex,  int count) {
    	if(startindex >= msgHeader.length) {
    		return null;
    	}
    	byte[] b = null;
    	if(msgHeader.length - startindex < count)
    		b = new byte[msgHeader.length - startindex];
    	else
    		b = new byte[count];
    	for(int i = 0; i < b.length; i++) {
    		b[i] = msgHeader[startindex + i];
    	}
    	return b;		
    }
    
    /** Sets the 8583 message type id. Ӧ��Ϊ4�ֽ��ַ���  */
    public void setMsgTypeID(String msgtypeid) {
    	this.msgtypeid = msgtypeid;
    }
    /** Returns the 8583 message type id. */
    public String getMsgTypeID() {
    	return msgtypeid;
    }

    /** Indicates whether the message should be binary. Default is false.
     * �������Ϊtrue, �����еĸ��������ն�������ɱ��ġ�(����ͷ���������ͱ�ʾ��λͼ����Ӱ��)
     * �����й���8583���ģ�һ��Ӧ������Ϊfalse�� 
     */
    public void setBinary(boolean flag) {
    	isbinary = flag;
    }
    /** Returns true if the message is binary coded; default is false.
     * ���Ϊtrue, �����еĸ��������ն�������ɱ��ġ�(����ͷ���������ͱ�ʾ��λͼ����Ӱ��) 
     */
    public boolean isBinary() {
    	return isbinary;
    }

    /** Sets the ETX character, which is sent at the end of the message as a terminator.
     * Default is -1, which means no terminator is sent. */
    public void setEtx(int value) {
    	etx = value;
    }

    /** Returns the stored value in the field, without converting or formatting it.
     * @param fieldid The field number. 1 is the secondary bitmap and is not returned as such;
     * real fields go from 2 to 128. */
    public Object getObjectValue(int fieldid) {
    	cnValue<?> v = fields.get(fieldid);
    	if (v == null) {
    		return null;
    	}
    	return v.getValue();
    }

    /** Returns the cnValue for the specified field.
     * @param  fieldid Ӧ����2-128��Χ
     */
    public cnValue<?> getField(int fieldid) {
    	return fields.get(fieldid);
    }

    /** Stored the field in the specified index. The first field is the secondary bitmap and has index 1,
     * so the first valid value for index must be 2. */
    public void setField(int fieldid, cnValue<?> field) {
    	if (fieldid < 2 || fieldid > 128) {
    		throw new IndexOutOfBoundsException("Field index must be between 2 and 128");
    	}
    	if (field == null) {
    		fields.remove(fieldid);
    	} else {
    		fields.put(fieldid, field);
    	}
    }

    /** Sets the specified value in the specified field, creating an cnValue internally.
     * @param fieldid The field number (2 to 128)
     * @param value The value to be stored.
     * @param t The 8583 cntype.
     * @param length The length of the field, used for ALPHA and NUMERIC values only, ignored
     * with any other type. */
    public void setValue(int fieldid, Object value, cnType t, int length) {
    	if (fieldid < 2 || fieldid > 128) {
    		throw new IndexOutOfBoundsException("Field index must be between 2 and 128");
    	}
    	if (value == null) {
    		fields.remove(fieldid);
    	} else {
    		cnValue v = null;
    		if (t.needsLength()) {
    			v = new cnValue<Object>(t, value, length);
    		} else {
    			v = new cnValue<Object>(t, value);
    		}
    		fields.put(fieldid, v);
    	}
    }

    /** Returns true is the message has a value in the specified field.
     * @param fieldid The field id. */
    public boolean hasField(int fieldid) {
    	return fields.get(fieldid) != null;
    }

    /** Writes a message to a stream, after writing the specified number of bytes indicating
     * the message's length. The message will first be written to an internal memory stream
     * which will then be dumped into the specified stream. This method flushes the stream
     * after the write. There are at most three write operations to the stream: one for the
     * length header, one for the message, and the last one with for the ETX.
     * @param outs The stream to write the message to.
     * @param lengthBytes The size of the message total length header. Valid ranges are 2 to 4. �����ĳ���ͷ��һ��4���ֽڣ�
     * @param radixoflengthBytes ��ʾ�������ĳ��ȵ��ֽڣ�lengthBytes���ı�ʾ���ƣ�ֻ��ȡ10��16��
     * @throws IllegalArgumentException if the specified length header is more than 4 bytes.
     * @throws IOException if there is a problem writing to the stream. 
     */
    public void write(OutputStream outs, int lengthBytes, int radixoflengthBytes) throws IOException {
		if (lengthBytes > 4) {
			throw new IllegalArgumentException("The length header can have at most 4 bytes");
		}
		byte[] data = writeInternal();

		int len = data.length;
		if (etx > -1) {
			len++;
		}
		if (lengthBytes >= 2) {
			if (radixoflengthBytes == 16) { // �����ʮ�����Ʊ�ʾ

				byte[] buf = new byte[lengthBytes];
				int pos = 0;
				if (lengthBytes == 4) {
					buf[0] = (byte) ((len & 0xff000000) >> 24);
					pos++;
				}
				if (lengthBytes > 2) {
					buf[pos] = (byte) ((len & 0xff0000) >> 16);
					pos++;
				}
				if (lengthBytes > 1) {
					buf[pos] = (byte) ((len & 0xff00) >> 8);
					pos++;
				}
				buf[pos] = (byte) (len & 0xff);
				outs.write(buf);

			} else if (radixoflengthBytes == 10) { // ���Ϊ10����
				int l = data.length;
				if (etx > -1) {
					l++;
				}
				byte[] buf = new byte[lengthBytes];
				int temp = 1;
				for(int i = 0; i < lengthBytes; i++) {
					buf[lengthBytes - 1 -i ] = (byte) (0x30+ ((len / (temp)) % 10));
					temp = temp * 10;
				}
				outs.write(buf);

			} else {
				throw new IllegalArgumentException("����������ֻ��Ϊ10��16");
			}
		}

		outs.write(data);
		// ETX
		if (etx > -1) {
			outs.write(etx);
		}
		outs.flush();
	}

    /** Creates and returns a ByteBuffer with the data of the message, including the length header.
     * The returned buffer is already flipped, so it is ready to be written to a Channel. */
    public ByteBuffer writeToBuffer(int lengthBytes) {
    	if (lengthBytes > 4) {
    		throw new IllegalArgumentException("The length header can have at most 4 bytes");
    	}

    	byte[] data = writeInternal();
    	ByteBuffer buf = ByteBuffer.allocate(lengthBytes + data.length + (etx > -1 ? 1 : 0));
    	if (lengthBytes > 0) {
    		int l = data.length;
    		if (etx > -1) {
    			l++;
    		}
    		byte[] bbuf = new byte[lengthBytes];
    		int pos = 0;
    		if (lengthBytes == 4) {
    			bbuf[0] = (byte)((l & 0xff000000) >> 24);
    			pos++;
    		}
    		if (lengthBytes > 2) {
    			bbuf[pos] = (byte)((l & 0xff0000) >> 16);
    			pos++;
    		}
    		if (lengthBytes > 1) {
    			bbuf[pos] = (byte)((l & 0xff00) >> 8);
    			pos++;
    		}
    		bbuf[pos] = (byte)(l & 0xff);
    		buf.put(bbuf);
    	}
    	buf.put(data);
    	//ETX
    	if (etx > -1) {
    		buf.put((byte)etx);
    	}
    	buf.flip();
    	return buf;
    }

    /** Writes the message to a memory buffer and returns it. The message does not include
     * the ETX character or the header length. */
    protected byte[] writeInternal() {
    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	try {
    		if (msgHeader != null) 
    			bout.write(msgHeader);	
    		//Message Type
    	    bout.write(msgtypeid.getBytes());
		} catch (IOException ex) {
			//should never happen, writing to a ByteArrayOutputStream
		}    

    	//Bitmap
    	ArrayList<Integer> keys = new ArrayList<Integer>();
    	keys.addAll(fields.keySet());
    	Collections.sort(keys);
    	BitSet bs = new BitSet(64);
    	for (Integer i : keys) {	// BitSet�����Զ���չ��С
    		bs.set(i - 1, true);
    	}
    	//Extend to 128 if needed
    	if (bs.length() > 64) {
    		BitSet b2 = new BitSet(128);
    		b2.or(bs);	// �õ�λͼ(������ĸ����������Զ���չ)
    		bs = b2;
    		bs.set(0, true);
    	}
    	//Write bitmap into stream
		int pos = 128; 	// ������λ���㣺 -- 1000 0000����ֵ���λΪ1��Ȼ������һλ���ȵȣ�
		int b = 0; 	// ������λ���㣺��ֵ������λȫ0
		for (int i = 0; i < bs.size(); i++) {
			if (bs.get(i)) {
				b |= pos;
			}
			pos >>= 1;
			if (pos == 0) { // ��һ���ֽ�ʱ��8λ������д��
				bout.write(b);
				pos = 128;
				b = 0;
			}
		}

    	//Fields
    	for (Integer i : keys) {
    		cnValue v = fields.get(i);
    		try {
    			v.write(bout, isbinary);
    		} catch (IOException ex) {
    			//should never happen, writing to a ByteArrayOutputStream
    		}
    	}
    	return bout.toByteArray();
    }
    
    /**
     * ���ݵ�ǰ�ı������ݣ��������ձ��ĵĵĳ��ȣ���λΪ�ֽڣ�
     * @return ��������ı����ֽڸ�����������ͷ���������ͱ�ʾ��λͼ�͸�����Ч�ı�����
     */
    public int estimatetotalmsglength() {
    	int totalmsglen = 0;
    	if(msgHeader != null)	// ����ͷ
    		totalmsglen += msgHeader.length;
    	if(msgtypeid != null)	// �������ͱ�ʾ
    		totalmsglen += msgtypeid.length();
   
    	// λͼ
    	ArrayList<Integer> keys = new ArrayList<Integer>();
    	keys.addAll(fields.keySet());
    	Collections.sort(keys);
    	if(keys.get(keys.size() -1) <= 64)	// �������һ����IDС�ڵ���64
    		totalmsglen += 8;
    	else
    		totalmsglen += 16;
    	
    	// ������
    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	for (Integer i : keys) {
    		cnValue v = fields.get(i);
    		try {
    			v.write(bout, isbinary);
    		} catch (IOException ex) {
    			//should never happen, writing to a ByteArrayOutputStream
    		}
    	}
    	totalmsglen += bout.toByteArray().length;
    	return totalmsglen;
    }
}

