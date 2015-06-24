package com.jzy.jedis;

import java.awt.peer.SystemTrayPeer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class EncodingVSDirect implements Runnable{
	private AbstractTxn[] txns;
	private int queryCount;
	
	private static long minTxnID;
	private static long lsn ;
	private static long maxLSN;
	private static final int bufferSize = 500;
	private static String[] logBuffer = new String[bufferSize];
	private static String[] logBuffer2 = new String[bufferSize];
	private static int bufferPointer = 0;
	private static Map<String, String> globalDict = new HashMap<String, String>();
	private static int dictValue = 0;
	private static int commitTimes = 0;
	
	private static  FileOutputStream fos; //дͼ5.2
			
		
	private static JedisPool pool = new JedisPool("192.168.171.120", 30031);
	private static JedisPool pool2 = new JedisPool("192.168.171.120", 30032);
	
	static {
		pool.getResource().flushDB();
		System.out.println("------���ݿ��ʼ����ճɹ�������----");
		
		pool2.getResource().flushDB();
		System.out.println("------���ݿ�2��ʼ���ɹ���������------");
		

	}
	Jedis jedis, jedis2;

	public EncodingVSDirect(int numTxn, long startLSN, int queryCount, long minTxnID) {
		this.txns = new AbstractTxn[numTxn];
		this.lsn = startLSN;
		this.queryCount = queryCount;
		this.minTxnID = minTxnID;
		this.maxLSN = lsn + numTxn * queryCount -1 ;
		
		jedis = pool.getResource();
		jedis2 = pool2.getResource();
		
		try {
			this.fos = new FileOutputStream(new File("results\\EncodingVSNoEncoding.dat"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {

		long start = System.currentTimeMillis();
		int threadID = Integer.parseInt(Thread.currentThread().getName());
		this.txns[threadID] = new AbstractTxn(threadID + minTxnID, queryCount);
		
		System.out.println("TxnID: " + threadID + "�߳̿�ʼ*************");
		for (int i = 0; i < queryCount; i++) {		
			
			//writeBuffer(threadID);
			writeBufferWithEncoding(threadID);
			
			// ��־������ʱ����
			try {
				Thread.sleep((int) Math.random() * 10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		System.out.println("=======================" + Thread.currentThread().getName() + " End");
		long end = System.currentTimeMillis();
		System.out.println(end - start);
		

	}
	
	public synchronized void writeBuffer(int threadID) {
		
		Log log = txns[threadID].generateOneQuery(lsn++);
		if (log.type != LogType.SELECT) {
			
			// ȫ���ֵ�����ύ��ʽ
			if (bufferPointer < bufferSize) {
				//System.out.println(log.type);
				
				System.out.println("����" + (threadID + minTxnID) + "����д��� " + log.lsn + " ����־..." + log.type);
				//String compressedLog = log.toStringWithTxnIDEncoding(globalDict.get(Long.toString(log.txnID)));
				//logBuffer[bufferPointer] = compressedLog== null?"":compressedLog;
				logBuffer2[bufferPointer] = log.toString() == null?"":log.toString();
				
				++bufferPointer ;
				
				
				//�������������ύ
				if (bufferPointer == bufferSize || log.lsn == maxLSN) {

					System.out.println(Thread.currentThread().getName() + "^^^^^^^^^^^^^^^^^^^^^^����������������" + bufferPointer);
					
					//ѹ��

					//jedis.rpush("commit", logBuffer);
					//System.out.println(Thread.currentThread().getName() + "+++++++++++++++++++++++++++++++++++++++commit�ɹ�"  + commitTimes);
					
					//jedis.hmset("commit_dict" + Integer.toString(commitTimes), globalDict);
					//System.out.println(Thread.currentThread().getName() + "+++++++++++++++++++++++++++++++++++commit_dict�ɹ�" + commitTimes);

					//jedis.save();
					//System.out.println(Thread.currentThread().getName() +"=======================================save�ɹ�"+ commitTimes);
					
					//��ѹ��
					jedis2.rpush("commit", logBuffer2);
					jedis2.save();
					
					
										
					++commitTimes;
					globalDict.clear();
					bufferPointer = 0;
					dictValue = 0;
	
			
				}
			}
		}
	}
	
	public synchronized void writeBufferWithEncoding(int threadID) {
		
		Log log = txns[threadID].generateOneQuery(lsn++);
		if (log.type != LogType.SELECT) {
			
			// ȫ���ֵ�����ύ��ʽ
			if (bufferPointer < bufferSize) {
				//System.out.println(log.type);
				if (!globalDict.containsKey(Long.toString(log.txnID))) {
					globalDict.put(Long.toString(log.txnID),
							Integer.toString(dictValue));
					++dictValue;
				}
				System.out.println("����" + (threadID + minTxnID) + "����д��� " + log.lsn + " ����־..." + log.type);
				String compressedLog = log.toStringWithTxnIDEncoding(globalDict.get(Long.toString(log.txnID)));
				logBuffer[bufferPointer] = compressedLog== null?"":compressedLog;
				//logBuffer2[bufferPointer] = log.toString() == null?"":log.toString();
				
				++bufferPointer ;
				
				
				//�������������ύ
				if (bufferPointer == bufferSize || log.lsn == maxLSN) {

					System.out.println(Thread.currentThread().getName() + "^^^^^^^^^^^^^^^^^^^^^^����������������" + bufferPointer);
					
					//ѹ��

					jedis.rpush("commit", logBuffer);
					System.out.println(Thread.currentThread().getName() + "+++++++++++++++++++++++++++++++++++++++commit�ɹ�"  + commitTimes);
					
					jedis.hmset("commit_dict" + Integer.toString(commitTimes), globalDict);
					System.out.println(Thread.currentThread().getName() + "+++++++++++++++++++++++++++++++++++commit_dict�ɹ�" + commitTimes);

					jedis.save();
					System.out.println(Thread.currentThread().getName() +"=======================================save�ɹ�"+ commitTimes);
					
					//��ѹ��
					//jedis2.rpush("commit", logBuffer2);
					//jedis2.save();
					
					
										
					++commitTimes;
					globalDict.clear();
					bufferPointer = 0;
					dictValue = 0;
	
			
				}
			}
		}
	}
	
	
	

}
			
