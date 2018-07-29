package com.ww.util;

import java.util.concurrent.CountDownLatch;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ZkCuratorLock {

	final static Logger log = LoggerFactory.getLogger(ZkCuratorLock.class);
	
	private CuratorFramework client = null;
	//用于挂起当前请求，并等待上一个分布式锁释放
	private static CountDownLatch zkCountDownLatch = new CountDownLatch(1);
	//分布式锁的总结点名称
	private static final String ZK_LOCK_PROJECT = "zk-locks";
	//子节点名称
	private final static String TEST_LOCK_PATH = "first-test-lock";
	
	public CuratorFramework getClient() {
		return client;
	}
	
	public ZkCuratorLock(){}
	
	public ZkCuratorLock(CuratorFramework client){
		System.out.println("----client 被初始化了:" + client);
		this.client = client;
	}
	
	//初始化锁，在第一次加载时创建工作空间和锁的父节点
	public void init(){
		
		System.out.println("进入init。。。");
		try {
			if(client.checkExists().forPath("/" + ZK_LOCK_PROJECT) == null){
				
				client.create()
					.creatingParentContainersIfNeeded()
					.withMode(CreateMode.PERSISTENT)
					.withACL(Ids.OPEN_ACL_UNSAFE)   //这里权限设错了，坑。。。。
					.forPath("/" + ZK_LOCK_PROJECT);
			}
			
			//针对zk的分布式锁节点，创建相应的watcher事件监听
			addWatcherToLock("/" + ZK_LOCK_PROJECT);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.print("连接zk服务器异常。。。。");
		}
	}
	
	/**
	 * @Description: 关闭zk客户端连接
	 */
	public void closeZKClient() {
		if (client != null) {
			this.client.close();
		}
	}
	
	public boolean isConnect(){
		return client.isStarted();
	}
	
	public void getLock(){
		
		
		//使用死循环，当上一个锁被释放并且当前请求获得锁后才会跳出
		while(true){
			
			try {
				client.create()
					.creatingParentContainersIfNeeded()
					.withMode(CreateMode.EPHEMERAL) //一定注意这里创建的是临时节点
					.withACL(Ids.OPEN_ACL_UNSAFE)
					.forPath("/" + ZK_LOCK_PROJECT + "/" + TEST_LOCK_PATH);
				
				return;
			} catch (Exception e) {
				
				//如果没有获得锁，需要重新设置同步资源的值
				if(zkCountDownLatch.getCount() <=0){
					zkCountDownLatch = new CountDownLatch(1);
				}
				
				//阻塞线程
				try {
					zkCountDownLatch.await();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
	}
	
	public void addWatcherToLock(String path) throws Exception{
		
		final PathChildrenCache childrenCache = new PathChildrenCache(client, path, true);
		childrenCache.start(StartMode.BUILD_INITIAL_CACHE);
		
		childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				
				System.out.println("1--------------------------");
				if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)){
					System.out.println("2--------------------------");
					String eventPath = event.getData().getPath();
					if (eventPath.equals(path + "/" +TEST_LOCK_PATH)) {
						zkCountDownLatch.countDown();
					} 
				}
			}
		});
	}
	
	
	public boolean releaseLock(){
		try {
			if(client.checkExists().forPath("/" + ZK_LOCK_PROJECT + "/" + TEST_LOCK_PATH) !=null){
				System.out.println("3--------------------------");
				client.delete().forPath("/" + ZK_LOCK_PROJECT + "/" + TEST_LOCK_PATH);
			}else{
				System.out.println("4--------------------------");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		System.out.printf("分布式锁释放123 .....");
		return true;
	}
	
}
