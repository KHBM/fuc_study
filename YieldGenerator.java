package com.foxrain.sheep.whileblack.util;

import com.google.common.collect.Queues;
import org.apache.commons.lang3.tuple.Triple;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Created with intellij IDEA.
 * by 2021 03 2021/03/19 11:09 오후 19
 * User we at 23 09
 * To change this template use File | Settings | File Templates.
 *
 * @author foxrain
 */
public class YieldGenerator<MT extends Flux<T>, T, R>
{
    private boolean done = false;
    private Function<YieldGenerator<MT, T, R>, R> function;
    private ConcurrentLinkedQueue<Testers.Value<T>> yieldValue = Queues.newConcurrentLinkedQueue();
    private int callCount = 0;
    private MT returnValue;
    public final Function<T, MT> doRec;
    private final Object lock = new Object();
    private final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
    private R finalResult;

    public R getResult()
    {
        return finalResult;
    }

    public static void test()
    {
        final YieldGenerator<Flux<Integer>, Integer, Flux<Triple<Integer, Integer, Integer>>> gn = YieldGenerator.of((m) ->
        {
            Testers.Value<Integer> c = m.yield(Flux.just(5));
            Testers.Value<Integer> a = m.yield(Flux.just(3));
            Testers.Value<Integer> b = m.yield(Flux.just(40)); // yield * 고려..

            return m.returns(a.v() * a.v() + b.v() * b.v() == c.v() * c.v() ?
                Flux.just(new Triple[]{Triple.of(a.v(), b.v(), c.v())}) :
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
        Testers.Return<MT> mtReturn = YieldGenerator.this.next(v);
        if (mtReturn.isDone())
        {
            return mtReturn.getValue();
        }
        else
        {
            final MT mt = (MT) mtReturn.getValue().flatMap(v1 -> doREC(v1));
            final ConnectableFlux<T> publish = mt.publish();
            publish.subscribe();
            publish.connect();
            return mtReturn.getValue();
        }
    }

    public YieldGenerator(Function<YieldGenerator<MT, T, R>, R> supplier)
    {
        this.function = supplier;

        this.doRec = new Function<T, MT>()
        {
            @Override
            public MT apply(T v)
            {
                Testers.Return<MT> mtReturn = YieldGenerator.this.next(v);
                return mtReturn.isDone() ?
                        mtReturn.getValue()
                    :
//                        YieldGenerator.this.resolveFlatMap(mtReturn.getValue(), YieldGenerator.this.doRec); //TODO
                    (MT) mtReturn.getValue().flatMap(v1 -> doRec.apply(v1));
            }
        };

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<?> submit = executorService.submit(() ->
        {
            System.out.println("STARTED");
            finalResult = this.function.apply(this);
        });
//        final MT apply = this.doRec.apply(null);
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
        done = true;
    }

    public Testers.Return<MT> next(T t)
    {
        System.out.println("Next" + callCount);
        synchronized (lock)
        {
            if (callCount < 2)
                yieldValue.clear();
            yieldValue.add(new Testers.Value(t));
            callCount++;
            lock.notifyAll();
            while (atomicBoolean.get() == false)
            {
                waits();
            }
            atomicBoolean.set(false);
            return new Testers.Return(returnValue, done);
        }
    }

    public Testers.Value<T> yield(MT expression)
    {
        /////doRec.apply(expression);
        System.out.println("Yield" + callCount);
        synchronized (lock)
        {
            returnValue = expression;
            while (atomicBoolean.get() == true)
            {
                waits();
            }
            atomicBoolean.set(true);
            lock.notifyAll();
            while (callCount < 2 || yieldValue.isEmpty())
            {
                waits();
            }
            //atomicBoolean.set(true);
            return yieldValue.poll();
        }
    }

    public R returns(R expression)
    {
        System.out.println("Return"+callCount);
        synchronized (lock)
        {
            setDoneTrue();
            //returnValue = null;
            atomicBoolean.set(true);
            lock.notifyAll();
            return expression;
        }
    }

    private void waits()
    {
        try
        {
            lock.wait();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
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
}

