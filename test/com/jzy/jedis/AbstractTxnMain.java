package com.jzy.jedis;

/***
 * ������д��־
 * @author jiangzeyuan
 *
 */

public class AbstractTxnMain {
	
	public static void main(String[] args) {
		
		JedisUtil.init();
		JedisUtil.clusterDataInit(10000); //ÿ���ڵ�����10000�����ֵ
		long txnCnt = 0;
		long lsnCnt = 0;
		
		Long start = System.currentTimeMillis();
		
		while (txnCnt < 300) {
			AbstractTxn atxn = new AbstractTxn(txnCnt++);
			String logEntry = null;		
			//�������������־
			for (int i = 0; i < 10; i++) {
				Log log = atxn.generateOneQuery(lsnCnt++);				
				if (log.toString() != null) {
					JedisUtil.singleRpush(log.siteNum, "redo_log", log.toString());
				}
			} //for
			
		} //while
		
		Long end = System.currentTimeMillis();
		System.out.println(end-start);
		
		for (int i = 0; i < Parameters.CLUSTER_LEN; i++) {
			System.out.println("SINGLE_NODE: " + i);
			for (String str : JedisUtil.singleLrange(i, "redo_log")) {
				System.out.println(str);
			}

		}

	}
}