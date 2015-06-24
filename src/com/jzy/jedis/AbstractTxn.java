package com.jzy.jedis;

import java.util.Random;

import redis.clients.jedis.Jedis;

public class AbstractTxn {

	protected Long txnID;
	protected String[] querys;
	protected int queryCount;
	protected static Random rand = new Random(3000);
	
	public AbstractTxn(Long id) {
		this.txnID = id;
	}
	
	public AbstractTxn(Long id, int queryCount) {
		this.txnID = id;
		this.queryCount = queryCount;
	}
	
	/**
	 * ����һ��query��Ҫ����Դ��������־
	 * @return
	 */
	public Log generateOneQuery(long LSN) {
		
		LogType type;	
		Long tupleID = (long)rand.nextInt(50000);
		
		if (JedisUtil.hashTable.containsKey(Long.toString(tupleID))) {
			//������ݿ��������key����ô����ѡ��DELETE����UPDATE��SELECT
			int randRes = rand.nextInt(10);
			
			if (randRes < 2) {
				type = LogType.SELECT; //select��д��־

			} else if(randRes >7) {
				type = LogType.DELETE; //delete
				
			} else { 
				type = LogType.UPDATE; //update
			}		
			
			
		} else { //INSERT
			type = LogType.INSERT;
		}
		
		String value = JedisUtil.randomString(50);
		return new Log(LSN, txnID, type, tupleID, value, 
							JedisUtil.targetSite(Long.toString(tupleID)));
	}
	
	/**
	 * ����־�ɹ����غ�ȥִ����������
	 * @param log 
	 */
	public void execOneQuery(Log log) {
		String strTupleID = Long.toString(log.tupleID);
		if (log.type == LogType.SELECT) {
			String res = JedisUtil.jc.get(strTupleID);
			
		} else if (log.type == LogType.UPDATE ) {
			JedisUtil.hashTable.put(strTupleID, log.siteNum);
			JedisUtil.jc.set(strTupleID, log.value);
			
		} else if (log.type == LogType.DELETE ) {
			if (JedisUtil.hashTable.containsKey(strTupleID)) {
				JedisUtil.hashTable.remove(strTupleID);
			}
			JedisUtil.jc.del(strTupleID);
			
		} else if (log.type == LogType.INSERT ) {
			JedisUtil.hashTable.put(strTupleID, log.siteNum);
			JedisUtil.jc.set(strTupleID, log.value);
		}
		
	}
				
}
