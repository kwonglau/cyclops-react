package com.aol.simple.react.stream.lazy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Builder;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;

import com.aol.simple.react.RetryBuilder;
import com.aol.simple.react.async.Continueable;
import com.aol.simple.react.async.QueueFactories;
import com.aol.simple.react.async.QueueFactory;
import com.aol.simple.react.async.Subscription;
import com.aol.simple.react.capacity.monitor.LimitingMonitor;
import com.aol.simple.react.collectors.lazy.BatchingCollector;
import com.aol.simple.react.collectors.lazy.LazyResultConsumer;
import com.aol.simple.react.stream.BaseSimpleReact;
import com.aol.simple.react.stream.StreamWrapper;
import com.aol.simple.react.threads.ReactPool;
import com.nurkiewicz.asyncretry.RetryExecutor;

@Wither
@Builder
@Getter
@Slf4j
@AllArgsConstructor
public class LazyFutureStreamImpl<U> implements LazyFutureStream<U>{
	
	

	private final ExecutorService taskExecutor;
	private final RetryExecutor retrier;
	private final Optional<Consumer<Throwable>> errorHandler;
	private final StreamWrapper lastActive;
	private final boolean eager;
	private final Consumer<CompletableFuture> waitStrategy;
	private final LazyResultConsumer<U> lazyCollector;
	private final QueueFactory<U> queueFactory;
	private final BaseSimpleReact simpleReact;
	private final Continueable subscription;
	private final static ReactPool<BaseSimpleReact> pool = ReactPool.elasticPool(()->new LazyReact(Executors.newSingleThreadExecutor()));
	private final List originalFutures=  null;
	
	LazyFutureStreamImpl(final Stream<CompletableFuture<U>> stream,
			final ExecutorService executor, final RetryExecutor retrier){
		this(stream,executor,retrier,null);
	}
	LazyFutureStreamImpl(final Stream<CompletableFuture<U>> stream,
			final ExecutorService executor, final RetryExecutor retrier,List originalFutures) {
		
		this.simpleReact = new LazyReact();
		this.taskExecutor = Optional.ofNullable(executor).orElse(
				new ForkJoinPool(Runtime.getRuntime().availableProcessors()));
		Stream s = stream;
		this.lastActive = new StreamWrapper(s, false);
		this.errorHandler = Optional.of((e) -> log.error(e.getMessage(), e));
		this.eager = false;
		this.retrier = Optional.ofNullable(retrier).orElse(
				RetryBuilder.getDefaultInstance());
		this.waitStrategy = new LimitingMonitor();
		this.lazyCollector = new BatchingCollector<>(this);
		this.queueFactory = QueueFactories.unboundedQueue();//QueueFactories.boundedQueue(lazyCollector.getMaxActive().getMaxActive());
		this.subscription = new Subscription();
		
	}

	public BaseSimpleReact getPopulator(){
		return pool.nextReactor();
	}
	public void returnPopulator(BaseSimpleReact service){
		pool.populate(service);
	}
	
	@Override
	public <R, A> R collect(Collector<? super U, A, R> collector) {
		return block(collector);
	}
	
  
	
}