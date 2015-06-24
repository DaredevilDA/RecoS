package com.jzy.jedis;

public class ConcurrentTxn extends AbstractTxn implements Runnable{
	
	private ConcurrentTxnManager ctm;
	
	public ConcurrentTxn(long txnID, int queryCount) {
		super(txnID, queryCount);
		ctm = new ConcurrentTxnManager(txnID);
	}
	
	public void run() {
		
		System.out.println("TxnID: " + this.txnID + "�߳̿�ʼ*************");	
		for (int i = 0; i < queryCount; i++) {
			
			Log log = generateOneQuery(ctm.getLSN());
			System.out.println("����" + txnID + "���ڲ����� " + log.lsn + " ����־..." );
			if (log.type != LogType.SELECT) {
					ctm.managerLog(log);

			}
			
			//��־������ʱ����
			try {
				Thread.sleep( (int)Math.random() * 10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		System.out.println("=======================" + txnID +" End");
		
		
	}
	
}
