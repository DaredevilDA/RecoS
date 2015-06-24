package com.jzy.jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.util.JedisClusterCRC16;

public class JedisUtil {
	
	public static JedisCluster jc = null;
	public static List<Jedis> clusterNodes = new ArrayList<Jedis>();
	public static List<Jedis> singleNodes = new ArrayList<Jedis>();
	public static Map<String, Integer> hashTable = new HashMap<String, Integer>();
	
	private static Random rand = new Random(1000);
	
//	/*
//	 * ���Ƽ������¼���get��������Ϊ���������ӣ�����ÿ��redisʵ�������������ӣ��Ⲣ���������뿴����
//	 */
//	public static JedisCluster getJc() {
//		return jc;
//	}
//
//	public static List<Jedis> getLj() {
//		return singleNodes;
//	}
//	
//	public static Map<String, Integer> getHashTable() {
//		return hashTable;
//	}
	
	/**
	 * ��̬��ʼ������
	 */
	public static void init(){
		
		/**
		 * ��ʼ��cluster
		 */
		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
		jedisClusterNodes.add(new HostAndPort(Parameters.HOST, Parameters.CLUSTER_PORT));
		jc = new JedisCluster(jedisClusterNodes);
		for(JedisPool jp : jc.getClusterNodes().values()) {
			//��Clusterʼ�ձ�����ȫ�ֵ����ӣ������������Ӵ򿪡��رա���һ�����Ҫ����Ϊredis������������Ŀ��
			clusterNodes.add(jp.getResource());
			
		}
		flushCluster();

		/**
		 * ��ʼ��single
		 */
		for (int i = 0; i < Parameters.SINGLE_LEN; i++) {
			Jedis jedis = new Jedis(Parameters.HOST, Parameters.SINGLE_BEGIN_PORT + i);
			jedis.flushDB();
			singleNodes.add(jedis);
		}
		
		/**
		 * ���hashTable
		 */
		hashTable.clear();
		
	}
		
	
	public static void singleSet(int num, String key, String value) {
		singleNodes.get(num).set(key, value);
	}
	
	public static String singleGet (int num, String key) {
		String res = singleNodes.get(num).get(key);
		if ( res != null) {
			return res;
		}
		return null;
	}
	
	public static void singleRpush(int num, String key, String value) {
		singleNodes.get(num).rpush(key, value);
	}
	
	public static void singleRpush(int num, String key, String[] values) {
		singleNodes.get(num).rpush(key, values);
	}
	
	public static List<String> singleLrange(int num, String key) {
		return singleNodes.get(num).lrange(key, 0, -1);
		
	}
	
	public static void singleHmset(int num, String key, Map<String, String> hash) {
		singleNodes.get(num).hmset(key, hash);
	}
	
	public static void singleSave(int num) {
		singleNodes.get(num).save();
	}

	
	/**
	 * ����һ��key��targetSite
	 * @param String
	 * @return
	 */
	public static int targetSite(String key) {
		int slotsPerSite = JedisCluster.HASHSLOTS / Parameters.CLUSTER_LEN; //ÿ���ڵ㸺����ٸ���slot	
		int slot = JedisClusterCRC16.getCRC16(key) % JedisCluster.HASHSLOTS; //i���key���ĸ�����
		return slot / slotsPerSite;
	}
	
	
	/**
	 * �ر�����
	 */
	public static void destroy() {
		if (jc != null) {
			jc.close();
		}
		if (singleNodes.size() > 0 ) {
			for (Jedis jedis : singleNodes) {
				jedis.close();
			}
		}
	}
	
	
	/**
	 * ���췽��Ϊprivate
	 */
	private JedisUtil(){}
	
	/**
	 * ���cluster
	 */
	public static void flushCluster() {
		for (int i = 0; i < clusterNodes.size(); i++) {
			clusterNodes.get(i).flushDB();
		}
	}
	
	public static boolean clusterIsEmpty() {
		for (int i = 0; i < clusterNodes.size(); i++) {
			if (clusterNodes.get(i).dbSize() > 0) return false;
		}
		return true;
	}
	
	/**
	 * �Ѿ�����
	 * ��cluster�е����нڵ㵥���ó���
	 * @return �ڵ��list
	 */
	public static List<Jedis> getAllJedisFromCluster() {
		List<Jedis> list = new ArrayList<Jedis>();
		for(JedisPool jp : jc.getClusterNodes().values()) {
			Jedis jc = jp.getResource();
			list.add(jc);
		}
		return list;
	}
	
	/**
	 * ��cluster��ָ���Ľڵ㵥���ó���
	 * ע�⣺jc���ṩ������Map<String, JedisPool>�����ǰ��ն˿�˳�������
	 * @param witch 1, 2, 3...��ʾ�ڼ����ڵ㣨�����ӵ��Ǹ��˿ڿ�ʼ�㣩
	 * @return
	 * @throws Exception �����˽ڵ�������Χ
	 */
	public static Jedis getJedisFromCluster(int which) throws Exception {
		if (which > Parameters.CLUSTER_LEN) {
			throw new Exception("no such node");
		}
		
		int targetPort = Parameters.CLUSTER_PORT + which - 1; //��������ơ���
		
		Map<String, JedisPool> map = jc.getClusterNodes();
		for (Entry<String, JedisPool> entry : map.entrySet()){
			String[] split = entry.getKey().split(":"); //e.g 192.168.171.120:30021
			if (targetPort == Integer.parseInt(split[split.length - 1])) {//���ð�ź���Ǹ��˿ںź�targetPort��ͬ
				return entry.getValue().getResource();
			}
		}
		return null;
	}
	

	
	/**
	 * ���յ����ݿ���װ������Ԫ�أ��Ա�ĳЩ����������
	 * �����key��0, 1, 2... value��key��ͬ
	 */
	public static void clusterDataInit(int numPerSite) {
		if (!clusterIsEmpty()) {
			System.out.println("cluster is not empty");
		} else {
			int slotsPerSite = JedisCluster.HASHSLOTS / Parameters.CLUSTER_LEN; //ÿ���ڵ㸺����ٸ���slot
			for (long i = 0; i < Parameters.CLUSTER_LEN * numPerSite; i++) {
				int slot = JedisClusterCRC16.getCRC16(Long.toString(i)) % JedisCluster.HASHSLOTS; //i���key���ĸ�����
				int targetSite = slot / slotsPerSite + 1;	//i���key���ĸ�site�site�ı�Ŵ�1��ʼ��

				hashTable.put(Long.toString(i), targetSite);//��hashTable��д��key��Ӧ��site�ţ������ж����ĸ��ڵ���д��־
				jc.set(Long.toString(i), Long.toString(i));	//����<"123", "123">���ּ�ֵ��
			}
		}
	}

	/**
	 * ����һ�������valueֵ�����ֵ�ĳ���Ϊn���ַ�
	 * @return �ַ���
	 */
	public static String randomString(int n) {
		
		
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String res = "";
        for (int i = 0; i < n; i++) {
        	int num = rand.nextInt(62);
			res += str.charAt(num);
		}
        return res;
	}


}
