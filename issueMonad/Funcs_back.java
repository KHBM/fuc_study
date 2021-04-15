package schedule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created with intellij IDEA.
 * by 2021 04 2021/04/07 4:17 오후 07
 * User we at 16 17
 * To change this template use File | Settings | File Templates.
 *
 * @author foxrain
 */
@Log4j2
public class Funcs_back
{
    interface Functor<T,F extends Functor<?,?>>
    {
        <R> F map(Function<T,R> f);
    }

    interface Monad<T,M extends Monad<? ,?>> extends Functor<T,M>
    {
        M flatMap(Function<T, M> f);
    }

    static class Identity<T> implements Functor<T,Identity<?>>, Monad<T, Identity<?>>
    {
        private final T value;

        Identity(T value)
        {
            this.value = value;
        }

        public <R> Identity<R> map(Function<T,R> f)
        {
            final R result = f.apply(value);
            return new Identity<>(result);
        }

        @Override
        public Identity<?> flatMap(Function<T, Identity<?>> f)
        {
            return f.apply(value);
        }
    }

    static class FList<T> implements Functor<T, FList<?>>, Monad<T, FList<?>>
    {
        private final ImmutableList<T> list;

        FList(Iterable<T> value) {

            this.list = ImmutableList.copyOf(value);
        }

        @Override
        public <R> FList<?> map(Function<T, R> f)
        {
            ArrayList<R> result = new ArrayList<R>(list.size());

            for (T t : list)
            {
                result.add(f.apply(t));
            }

            return new FList<>(result);
        }

        @Override
        public FList<T> flatMap(Function<T, FList<?>> f)
        {
            // [1, 2, 3] with x -> [x*x] => [1, 4, 9]
            final FList map = this.map(f);
            return concat(map);
        }

        public List<T> getData()
        {
            return this.list;
        }

        public static FList<?> concat(FList<FList<?>> lists)
        {
            List<?> result = Lists.newArrayList();
            for (FList list : lists.getData())
            {
                result.addAll(list.getData());
            }
            return new FList<>(result);
        }
    }

    public static void main(String[] args)
    {
        try
        {
//        test1ExpandIntegers();

            final ArrayList<Integer> integers = Lists.newArrayList(1, 3, 5, 6, 7);
            FList<Integer> fList = new FList<>(integers);
            final FList<Integer> data1 =
                fList.flatMap(t -> new FList<>(Lists.newArrayList(String.valueOf(t))).flatMap(tv -> new FList<>(Lists.newArrayList(tv + t))))
                .flatMap(s ->
                {
                    System.out.println("what s : " + s);
                    return new FList<>(Lists.newArrayList(String.valueOf(s)));
                });
            final List<Integer> data = data1.getData();
            System.out.printf("[");
            for (Integer d : data)
            {
                System.out.print(d + ", ");
            }
            System.out.println("]");
            log.info(data);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void test1ExpandIntegers()
    {
        final ArrayList<Integer> integers = Lists.newArrayList(1, 3, 5, 6, 7);
        FList<Integer> fList = new FList<>(integers);
        final FList<Integer> fList1 = fList.flatMap((t) -> new FList<>(Lists.newArrayList(t, t)));
        final List<Integer> data = fList1.getData();
        System.out.printf("[");
        for (Integer d : data)
        {
            System.out.print(d +", ");
        }
        System.out.println("]");
        log.info(data);
    }

    static class FOptional<T> implements Functor<T,FOptional<?>>, Monad<T, FOptional<?>>
    {
        private final T valueOrNull;

        private FOptional(T valueOrNull)
        {
            this.valueOrNull = valueOrNull;
        }

        public <R> FOptional<R> map(Function<T,R> f)
        {
            if (valueOrNull == null)
                return empty();
            else
                return of(f.apply(valueOrNull));
        }

        public static <T> FOptional<T> of(T a)
        {
            return new FOptional<T>(a);
        }

        public static <T> FOptional<T> empty()
        {
            return new FOptional<T>(null);
        }

        @Override
        public FOptional<?> flatMap(Function<T, FOptional<?>> f)
        {
            return null;
        }
    }

    static FOptional<Integer> tryParse(String s)
    {
        try
        {
            final int i = Integer.parseInt(s);
            return FOptional.of(i);
        }
        catch (NumberFormatException e)
        {
            return FOptional.empty();
        }
    }

}

