package com.jzy.jedis;
/********
 * 
 * ������˵��
 * command logging���ü����������������ȫ��ͳһ��˳��ִ��
 * ��������֮ǰ���Ѿ�����Ԥ֪�������ɴ洢����
 *
 */

public class SimpleCommandLog {
	private long timestamp;
	private long txnID;
	private StoredProcedure sp;
	private String[] parameters;
}

/*
 * ��Ҫ��һЩ����ת���ΪStoredProcedure
 * 
 */
class StoredProcedure {
	//UPDATE (X,Y) VALUES (5, 6) WHERE id = 5
	private String query;
	//private Parameter[] params;
	
}

