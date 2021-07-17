package com.jssh.netty.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Locks {

	private static final SimpleLock simpleLock = new SimpleLock();

	public static Lock getLock() {
		return simpleLock;
	}

	static class LockHolder {

		private ReentrantLock lock;
		// private AtomicInteger count;
		private int count;

		public LockHolder(ReentrantLock lock) {
			this.lock = lock;
			// this.count = new AtomicInteger(0);
		}

		public ReentrantLock getLock() {
			return lock;
		}

		public int incrementAndGet() {
			// return count.incrementAndGet();
			return ++count;
		}

		public int decrementAndGet() {
			// return count.decrementAndGet();
			return --count;
		}
	}

	static class SimpleLock implements Lock {

		private static final Map<String, LockHolder> locks = new ConcurrentHashMap<>();

		@Override
		public void lock(String key, LockHandler<Object> handler) throws Exception {
			LockHolder lockHolder = (lockHolder = locks.get(key)) == null ? new LockHolder(new ReentrantLock())
					: lockHolder;
			while (true) {
				LockHolder _holder = lockHolder;
				_holder.getLock().lock();
				try {
					lockHolder = locks.computeIfAbsent(key, k -> {
						return _holder;
					});

					if (_holder == lockHolder) {
						lockHolder.incrementAndGet();
						break;
					}
				} finally {
					_holder.getLock().unlock();
				}
			}

			lockHolder.getLock().lock();
			try {
				handler.handle(lockHolder.getLock());
			} finally {
				try {
					if (lockHolder.decrementAndGet() == 0) {
						locks.remove(key);
					}
				} catch (Exception e) {
					// ignore
				}
				lockHolder.getLock().unlock();
			}
		}
	}

	public static void main(String[] args) throws Exception {

		int[] a = new int[] { 0 };

		ExecutorService executorService = Executors.newFixedThreadPool(10);
		
		long l1 = System.nanoTime();
		for (int i = 0; i < 10; i++) {

			executorService.submit(() -> {

				try {
					for (int j = 0; j < 1000000; j++) {
						Locks.getLock().lock("1", l -> {
							a[0] = a[0] + 1;
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		executorService.shutdown();

		executorService.awaitTermination(10, TimeUnit.SECONDS);

		long l2 = System.nanoTime();
		System.out.println(a[0]);
		System.out.println((double) (l2 - l1) / 1000000.d);
		System.out.println(SimpleLock.locks);
	}
}
