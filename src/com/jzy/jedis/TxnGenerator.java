package com.jzy.jedis;

import java.util.Random;

public class TxnGenerator {
	private int numTx;	//��Ҫģ����¼�����
	private int maxKey;	//�򵥵�ָ����ǰcluster������key����Щkey�ǰ���1,2,3...���ģ�
	private Random rand = new Random(3000);
	
	
	public TxnGenerator(int numTx, int maxKey) {
		this.numTx = numTx;
		this.maxKey = maxKey;
	}
	
	/**
	 * ִ��numTx��simpleUpdate
	 * ÿ��updateֻ�ڹ̶���key��Χ���ҵ�targetKey��Ȼ��������
	 * �˷���������Ϊ�����Ĳ��Է���
	 */
	public void simpleUpdate () {
		
		for (long i = 0; i < numTx; i++) {
			
			Long timestamp =  System.currentTimeMillis();
			Long txID = i;
			String targetKey = Integer.toString(rand.nextInt(maxKey));	//���ĸ�key����update
			String oldValue = JedisUtil.jc.get(targetKey);
			if (oldValue == null) {
				oldValue = "";
			}
			String newValue = Character.toString(randomChar());
			
			
			SimpleARIESLog sal = new SimpleARIESLog(timestamp, txID, targetKey, oldValue, newValue);
			int targetSite = JedisUtil.hashTable.get(targetKey);
			sal.writeLog(targetSite);
			
			JedisUtil.jc.set(targetKey, newValue);

		}
	}
	
	/**
	 * ��������ɾ�Ĳ�Ķ���
	 */
	public void CRUD() {
		if (!JedisUtil.clusterIsEmpty()) {
			System.out.println("warning: Cluster is not empty at first."); 
		}
		// TODO
		
	}
	
	/**
	 * ����һ�������valueֵ�����ֵ�ĳ���Ϊһ���ַ�
	 * @return һ��������ַ�
	 */
	private char randomChar() {

        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int num = rand.nextInt(62);
        return str.charAt(num);
	    
	}
	
	
}
