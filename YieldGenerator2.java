package com.foxrain.sheep.whileblack.util;

import com.google.common.collect.Queues;
import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created with intellij IDEA.
 * by 2021 03 2021/03/19 11:09 오후 19
 * User we at 23 09
 * To change this template use File | Settings | File Templates.
 *
 * @author foxrain
 */
@Slf4j
public class YieldGenerator<RR>
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

//    private Function<YieldGenerator, Object> function;
    private Object yieldValue = null;
    private int callCount = 0;
    private Object returnValue;
    private RR finalResult;

    public RR getResult()
    {
        return finalResult;
    }

    public static void test()
    {
        final YieldGenerator gn = new YieldGenerator<Flux<Triple<Integer,Integer,Integer>>>((m) ->
        {
            Integer c = m.yield(Flux.just(5));
            String a = m.yield(Flux.just("Str"));
            Integer b = m.yield(Flux.just(4)); // yield * 고려..

            return m.returns(a.length()* a.length() + b * b == c * c ?
                Flux.just(new Triple[]{Triple.of(a.length(), b, c)}) :
                Flux.just(Triple.emptyArray()));
        });

//        ((Flux<Triple<Integer,Integer,Integer>>)gn.getResult())
//            .subscribe(t -> {
//                System.out.println(String.format("T : [(%d, %d, %d)]"
//                    , t.getLeft(), t.getMiddle(), t.getRight()));
//            });
        gn.resolveSubscribe(gn.getResult(), new Consumer<Triple<Integer,Integer,Integer>>()
        {
            @Override
            public void accept(Triple<Integer,Integer,Integer> t)
            {
                System.out.println(String.format("T : [(%d, %d, %d)]"
                    , t.getLeft(), t.getMiddle(), t.getRight()));
            }
        });
    }

//    public static YieldGenerator of(Function<YieldGenerator<RR>, RR> supplier)
//    {
//        return new YieldGenerator(supplier);
//    }

    public <MT, T> MT doREC(T v)
    {
        Return<MT> mtReturn = YieldGenerator.this.next(v);
        final TypeToken<T> typeToken = new TypeToken<T>(mtReturn.getValue().getClass())
        {
        };
        log.info("DoREC get value : {}", typeToken.getType());
        if (!mtReturn.isDone())
        {
//            final MT mt = (MT) ((Flux)mtReturn.getValue()).flatMap(v1 -> doREC(v1));
            final MT mt = (MT) resolveFlatMap(mtReturn.getValue(), v1 -> doREC(v1));
//            final ConnectableFlux<T> publish = ((Flux)mt).publish();
            final Object subscribe = resolveMethodWithNoParam(mt, "publish");
//            publish.subscribe();
            resolveMethodWithNoParam(subscribe, "connect");
//            publish.connect();
        }
        return mtReturn.getValue();
    }

    public YieldGenerator(Function<YieldGenerator<RR>, RR> supplier)
    {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<?> submit = executorService.submit(() ->
        {
            try
            {
                log.info("STARTED");
                finalResult = supplier.apply(this);
            } catch (Exception e)
            {
                exceptionRaisedByProducer = e;
                log.error("There was an error ", e);
            }
        });
        final Object mt = doREC(null);
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

    private <MT, T> MT resolveFlatMap(MT target, Function<T, MT> func)
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

    private <MT, MR> MR resolveMethodWithNoParam(MT target, String parameterName)
    {
        final java.lang.reflect.Method flatMap = findMethodWithoutNoParameter(target, parameterName);
        try
        {
            return (MR) flatMap.invoke(target, null);
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

    public static <T> java.lang.reflect.Method findMethodWithoutNoParameter(T target, String methodName)
    {
        Class<?> tclass = target.getClass();

        java.lang.reflect.Method[] mList = tclass.getMethods();
        for (java.lang.reflect.Method m : mList)
        {
            if (m.getName().equalsIgnoreCase(methodName))
            {
                Class<?>[] paramsTypes = m.getParameterTypes();
                if (paramsTypes.length == 0)
                {
                    return m;
                }
            }
        }

        throw new RuntimeException("No "+methodName+"() method");
    }

    public static <T> java.lang.reflect.Method findSubscribe(T t)
    {
        Class<?> tclass = t.getClass();

        java.lang.reflect.Method[] mList = tclass.getMethods();
        for (java.lang.reflect.Method m : mList)
        {
            if (m.getName().equalsIgnoreCase("subscribe"))
            {
                Class<?>[] paramsTypes = m.getParameterTypes();
                if (paramsTypes.length == 1)
                {
                    if (paramsTypes[0].getName().equals("java.util.function.Consumer"))
                    {
                        return m;
                    }
                }
            }
        }

        throw new RuntimeException("No flatmap(Function) method");
    }

    private <MT, T> MT resolveSubscribe(MT target, Consumer<T> consum)
    {
        final java.lang.reflect.Method subsc = findSubscribe(target);
        try
        {
            return (MT) subsc.invoke(target, new Consumer[]{consum});
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

    public <MT, T> Return<MT> next(T t)
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

    public <MT, T> T yield(MT expression)
    {
        returnValue = expression;
        nextItemAvailable = true;

        itemAvailableOrHasFinished.set();
        isNextItemReady.await();
        return (T) yieldValue;
    }

    public <R> R returns(R expression)
    {
        nextItemAvailable = true;
        setDoneTrue();
        returnValue = expression;
        log.info("final value : {}", returnValue);
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

