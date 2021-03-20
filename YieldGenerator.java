package com.foxrain.sheep.whileblack.util;

import com.google.common.collect.Queues;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Created with intellij IDEA.
 * by 2021 03 2021/03/19 11:09 오후 19
 * User we at 23 09
 * To change this template use File | Settings | File Templates.
 *
 * @author foxrain
 */
@Slf4j
public class YieldGenerator<MT extends Flux<T>, T, R>
{
    private class Condition {
        private boolean isSet;
        public synchronized void set() {
            isSet = true;
            notify();
        }
        public synchronized void await() {
            try {
                if (isSet)
                    return;
                wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            finally {
                isSet = false;
            }
        }
    }

    private boolean hasFinished = false;
    private final Condition itemAvailableOrHasFinished = new Condition();
    private final Condition isNextItemReady = new Condition();
    private boolean nextItemAvailable;
    private Exception exceptionRaisedByProducer;

    private Function<YieldGenerator<MT, T, R>, R> function;
    private T yieldValue = null;
    private int callCount = 0;
    private MT returnValue;
    private R finalResult;

    public R getResult()
    {
        return finalResult;
    }

    public static void test()
    {
        final YieldGenerator<Flux<Integer>, Integer, Flux<Triple<Integer, Integer, Integer>>> gn = YieldGenerator.of((m) ->
        {
            Integer c = m.yield(Flux.just(5));
            Integer a = m.yield(Flux.just(3));
            Integer b = m.yield(Flux.just(4)); // yield * 고려..

            return m.returns(a * a + b * b == c * c ?
                Flux.just(new Triple[]{Triple.of(a, b, c)}) :
                Flux.just(Triple.emptyArray()));
        });

        gn.getResult()
            .subscribe(t -> {
                System.out.println(String.format("T : [(%d, %d, %d)]"
                    , t.getLeft(), t.getMiddle(), t.getRight()));
            });
    }

    public static <MT extends Flux<T>, T, R> YieldGenerator<MT, T, R> of(Function<YieldGenerator<MT, T, R>, R> supplier)
    {
        return new YieldGenerator(supplier);
    }

    public MT doREC(T v)
    {
        Return<MT> mtReturn = YieldGenerator.this.next(v);
        if (!mtReturn.isDone())
        {
            final MT mt = (MT) mtReturn.getValue().flatMap(v1 -> doREC(v1));
            final ConnectableFlux<T> publish = mt.publish();
            publish.subscribe();
            publish.connect();
        }
        return mtReturn.getValue();
    }

    public YieldGenerator(Function<YieldGenerator<MT, T, R>, R> supplier)
    {
        this.function = supplier;

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<?> submit = executorService.submit(() ->
        {
            try
            {
                log.info("STARTED");
                finalResult = this.function.apply(this);
            } catch (Exception e)
            {
                exceptionRaisedByProducer = e;
                log.error("There was an error ", e);
            }
        });
        final MT mt = doREC(null);
        try
        {
            submit.get();
            executorService.shutdown();
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
    }

    public static <T> java.lang.reflect.Method findFlatMap(T t)
    {
        Class<?> tclass = t.getClass();

        java.lang.reflect.Method[] mList = tclass.getMethods();
        for (java.lang.reflect.Method m : mList)
        {
            if (m.getName().equalsIgnoreCase("flatmap"))
            {
                Class<?>[] paramsTypes = m.getParameterTypes();
                if (paramsTypes.length == 1)
                {
                    if (paramsTypes[0].getName().equals("java.util.function.Function"))
                    {
                        return m;
                    }
                }
            }
        }

        throw new RuntimeException("No flatmap(Function) method");
    }

    private MT resolveFlatMap(MT target, Function<T, MT> func)
    {
        final java.lang.reflect.Method flatMap = findFlatMap(target);
        try
        {
            return (MT) flatMap.invoke(target, new Function[]{func});
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setDoneTrue()
    {
        hasFinished = true;
    }

    private boolean waitForNext() {
        if (nextItemAvailable)
            return true;
        if (exceptionRaisedByProducer != null)
            throw new RuntimeException(exceptionRaisedByProducer);
        if (callCount > 0)
            isNextItemReady.set();
        itemAvailableOrHasFinished.await();
        return true;
    }

    public Return<MT> next(T t)
    {
        yieldValue = t;
        if (!waitForNext())
        {
            throw new NoSuchElementException("No more item");
        }
        nextItemAvailable = false;
        callCount++;
        return new Return(returnValue, hasFinished);
    }

    public T yield(MT expression)
    {
        returnValue = expression;
        nextItemAvailable = true;

        itemAvailableOrHasFinished.set();
        isNextItemReady.await();
        return yieldValue;
    }

    public R returns(R expression)
    {
        nextItemAvailable = true;
        setDoneTrue();
        itemAvailableOrHasFinished.set();
        return expression;
    }

    public static void main(String[] args)
    {
        try
        {
            final ConcurrentLinkedQueue<Integer> empty = Queues.newConcurrentLinkedQueue();
            empty.add(3);
            empty.add(34);
            empty.add(63);
            empty.add(83);

            System.out.println(empty.poll());
            System.out.println(empty.poll());
            System.out.println(empty.poll());
            System.out.println(empty.poll());
            System.out.println(empty.poll());

            test();

            System.out.println("DO I MADE IT?");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Getter
    public static class Return<T>
    {
        private final T value;
        private final boolean done;
        public Return(T value, boolean done)
        {
            this.value = value;
            this.done = done;
        }
    }
}

