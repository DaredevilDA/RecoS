package com.jzy.jedis;

import java.util.HashMap;
import java.util.Map;

public class ConcurrentTxnManager {
	public static final int bufferSize = 10;
	
	public static String[] logBuffer = new String[bufferSize];
	public static int bufferPointer = 0;
	public static long lsn = 1000;
//	public static Map<Integer, Long> globalDict = new HashMap<Integer, Long>();
	public static Map<String, String> globalDict = new HashMap<String, String>();
	public static int dictPointer = 0;
	public static int saveTimes = 0;

	
	private long txnID;
	public ConcurrentTxnManager(long txnID) {
		this.txnID = txnID;
	}
	public long getLSN(){
		return lsn++;
	}
	public static  void   managerLog(Log log) {
		//System.out.println("���ڴ�������" + txnID + "��LSNΪ" + log.lsn + "����־");
		//System.out.println(log.toString());
		
		//ȫ���ֵ�����ύ��ʽ
		if( bufferPointer < bufferSize) {
			//System.out.println(log.type);
			if (!globalDict.containsValue(Long.toString(log.txnID)) ) {
				globalDict.put(Integer.toString(dictPointer), Long.toString(log.txnID));
				++dictPointer;
			} 
			logBuffer[bufferPointer++] = log.toString();
			
		} else { //��������
			
			System.out.println("^^^^^^^^^^^^^^^^^^^^^^����������������");
			JedisUtil.singleRpush(0, "commit", logBuffer);	
			System.out.println("+++++++++++++++++++++++++++++++++++++++commit�ɹ�");
			JedisUtil.singleHmset(0, "commit_dict" + Integer.toString(saveTimes++), globalDict);
			System.out.println("+++++++++++++++++++++++++++++++++++++++commit_dict�ɹ�");
			JedisUtil.singleSave(0);
			System.out.println("=======================================save�ɹ�");
			//System.exit(0);
			
			
			//System.exit(0);
			globalDict.clear();
			bufferPointer = 0;
			dictPointer = 0;
		}
		
		
		
	}
	
}
