package com.jzy.jedis;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ConTxn extends AbstractTxn implements Runnable {
	private static final int bufferSize = 15;
	private static String[] logBuffer = new String[bufferSize];
	private static int bufferPointer = 0;

	private static Map<String, String> globalDict = new HashMap<String, String>();
	private static int dictPointer = 0;

	private static int commitTimes = 0;

	private static long lsn = 1000;
	
	private static JedisPool pool = new JedisPool("192.168.171.120", 30031);
	static {
		pool.getResource().flushDB();
		System.out.println("------���ݿ��ʼ����ճɹ�������----");
	}

	public ConTxn(long txnID, int queryCount) {
		super(txnID, queryCount);
	}

	@Override
	public void run() {

		System.out.println("TxnID: " + this.txnID + "�߳̿�ʼ*************");
		for (int i = 0; i < queryCount; i++) {
			
			execute();

			// ��־������ʱ����
			try {
				Thread.sleep((int) Math.random() * 10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		System.out.println("=======================" + txnID + " End");

	}
	
	public void execute()  {
		
			Log log = generateOneQuery(lsn++);
			//System.out.println("����" + txnID + "���ڲ����� " + log.lsn + " ����־...");
			if (log.type != LogType.SELECT) {
				
				// ȫ���ֵ�����ύ��ʽ
				if (bufferPointer < bufferSize) {
					//System.out.println(log.type);
					if (!globalDict.containsValue(Long.toString(log.txnID))) {
						globalDict.put(Integer.toString(dictPointer),
								Long.toString(log.txnID));
						++dictPointer;
					}
					logBuffer[bufferPointer] = log.toString() == null?"":log.toString();
					System.out.println("����" + txnID + "����д��� " + log.lsn + " ����־..." + log.type);
					++bufferPointer ;
					
					//�������������ύ
					if (bufferPointer == bufferSize) {

						System.out.println(Thread.currentThread().getName() + "^^^^^^^^^^^^^^^^^^^^^^����������������" + bufferPointer);
						Jedis jedis = pool.getResource();
						
						if(logBuffer == null )return ;
						jedis.rpush("commit", logBuffer);
						System.out.println(Thread.currentThread().getName() + "+++++++++++++++++++++++++++++++++++++++commit�ɹ�"  + commitTimes);
						
						jedis.hmset("commit_dict" + Integer.toString(commitTimes++), globalDict);
						System.out.println(Thread.currentThread().getName() + "+++++++++++++++++++++++++++++++++++commit_dict�ɹ�" + commitTimes);

						jedis.save();
						System.out.println(Thread.currentThread().getName() +"=======================================save�ɹ�"+ commitTimes);
						
//						for(String str : logBuffer) {
//							System.out.println(str);
//						}
//						System.out.println("======================");
						
						globalDict.clear();
						bufferPointer = 0;
						dictPointer = 0;
					}

				} 

			}
		
	}
}
