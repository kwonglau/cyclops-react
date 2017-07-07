package cyclops.control;

import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.hkt.Higher2;
import com.aol.cyclops2.types.functor.Transformable;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.Witness;
import cyclops.monads.Witness.writer;
import cyclops.typeclasses.*;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.monad.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

@AllArgsConstructor(access= AccessLevel.PRIVATE)
@Getter
public final class Writer<W, T> implements Transformable<T>, Iterable<T>,Higher2<writer,W,T> {


    private final Tuple2<T,W> value;
    private final Monoid<W> monoid;

    public <R> Writer<W, R> map(Function<? super T,? extends  R> mapper) {
        return writer(mapper.apply(value.v1), value.v2, monoid);
    }

    public <R> R visit(BiFunction<? super Tuple2<T,W>,? super Monoid<W>,? extends R> fn){
        return fn.apply(value,monoid);
    }
    public <R> Writer<W, R> flatMap(Function<? super T,? extends  Writer<W, ? extends R>> fn) {
        Writer<W, ? extends R> writer = fn.apply(value.v1);
        return writer(writer.value.v1, writer.monoid.apply(value.v2, writer.value.v2), writer.monoid);
    }

    public Writer<W,T> tell(W write){
        return writer(value.v1,monoid.apply(write,value.v2),monoid);
    }

    public <R> Writer<W,R> set(R value){
            return writer(value,this.value.v2,monoid);
    }

    /*
     * Perform a For Comprehension over a Writer, accepting 3 generating function.
             * This results in a four level nested internal iteration over the provided Writers.
      *
              *  <pre>
      * {@code
      *
      *   import static com.aol.cyclops2.reactor.Writers.forEach4;
      *
         forEach4(Writer.just(1),
                 a-> Writer.just(a+1),
                 (a,b) -> Writer.<Integer>just(a+b),
                 a                  (a,b,c) -> Writer.<Integer>just(a+b+c),
                 Tuple::tuple)
      *
      * }
      * </pre>
             *
             * @param value1 top level Writer
      * @param value2 Nested Writer
      * @param value3 Nested Writer
      * @param value4 Nested Writer
      * @param yieldingFunction Generates a result per combination
      * @return Writer with a combined value generated by the yielding function
      */
    public  <R1, R2, R3, R4> Writer<W,R4> forEach4(Function<? super T, ? extends Writer<W,R1>> value2,
                                                   BiFunction<? super T, ? super R1, ? extends Writer<W,R2>> value3,
                                                   Fn3<? super T, ? super R1, ? super R2, ? extends Writer<W,R3>> value4,
                                                   Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R4> yieldingFunction) {


        return this.flatMap(in -> {

            Writer<W,R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Writer<W,R2> b = value3.apply(in,ina);
                return b.flatMap(inb -> {

                    Writer<W,R3> c = value4.apply(in,ina,inb);

                    return c.map(in2 -> {

                        return yieldingFunction.apply(in, ina, inb, in2);

                    });

                });


            });


        });

    }



    /**
     * Perform a For Comprehension over a Writer, accepting 2 generating function.
     * This results in a three level nested internal iteration over the provided Writers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Writers.forEach3;
     *
    forEach3(Writer.just(1),
    a-> Writer.just(a+1),
    (a,b) -> Writer.<Integer>just(a+b),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Writer
     * @param value2 Nested Writer
     * @param value3 Nested Writer
     * @param yieldingFunction Generates a result per combination
     * @return Writer with a combined value generated by the yielding function
     */
    public <R1, R2, R4> Writer<W,R4> forEach3(Function<? super T, ? extends Writer<W,R1>> value2,
                                               BiFunction<? super T, ? super R1, ? extends Writer<W,R2>> value3,
                                               Fn3<? super T, ? super R1, ? super R2, ? extends R4> yieldingFunction) {

        return this.flatMap(in -> {

            Writer<W,R1> a = value2.apply(in);
            return a.flatMap(ina -> {
                Writer<W,R2> b = value3.apply(in,ina);
                return b.map(in2 -> {
                    return yieldingFunction.apply(in, ina, in2);

                });



            });

        });

    }


    /**
     * Perform a For Comprehension over a Writer, accepting a generating function.
     * This results in a two level nested internal iteration over the provided Writers.
     *
     *  <pre>
     * {@code
     *
     *   import static com.aol.cyclops2.reactor.Writers.forEach;
     *
    forEach(Writer.just(1),
    a-> Writer.just(a+1),
    Tuple::tuple)
     *
     * }
     * </pre>
     *
     * @param value1 top level Writer
     * @param value2 Nested Writer
     * @param yieldingFunction Generates a result per combination
     * @return Writer with a combined value generated by the yielding function
     */
    public <R1, R4> Writer<W,R4> forEach2(Function<? super T, Writer<W,R1>> value2,
                                           BiFunction<? super T, ? super R1, ? extends R4> yieldingFunction) {

        return this.flatMap(in -> {

            Writer<W,R1> a = value2.apply(in);
            return a.map(in2 -> {
                return yieldingFunction.apply(in, in2);

            });

        });

    }

    public static <W, T> Writer<W, T> writer(T value, Monoid<W> combiner) {
        return new Writer<W,T>(Tuple.tuple(value, combiner.zero()), combiner);
    }
    public static <W, T> Writer<W, T> writer(T value, W initial, Monoid<W> combiner) {
        return new Writer<W,T>(Tuple.tuple(value, initial), combiner);
    }
    public static <W, T> Writer<W, T> writer(Tuple2<T,W> values, Monoid<W> combiner) {
        return new Writer<W,T>(values, combiner);
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(value.v1).iterator();
    }

    public static <W,W1,T> Nested<Higher<writer,W>,W1,T> nested(Writer<W,Higher<W1,T>> nested,Monoid<W> monoid, InstanceDefinitions<W1> def2){
        return Nested.of(nested, Instances.definitions(monoid),def2);
    }
    public <W1> Product<Higher<writer,W>,W1,T> product(Monoid<W> monoid,Active<W1,T> active){
        return Product.of(allTypeclasses(monoid),active);
    }
    public <W1> Coproduct<W1,Higher<writer,W>,T> coproduct(Monoid<W> monoid,InstanceDefinitions<W1> def2){
        return Coproduct.right(this,def2, Instances.definitions(monoid));
    }

    public Active<Higher<writer,W>,T> allTypeclasses(Monoid<W> monoid){
        return Active.of(this, Instances.definitions(monoid));
    }
    public <W2,R> Nested<Higher<writer,W>,W2,R> mapM(Monoid<W> monoid,Function<? super T,? extends Higher<W2,R>> fn, InstanceDefinitions<W2> defs){
        return Nested.of(map(fn), Instances.definitions(monoid), defs);
    }
    public static <W,T> Writer<W,T> narrowK2(final Higher2<writer, W,T> t) {
        return (Writer<W,T>)t;
    }
    public static <W,T> Writer<W,T> narrowK(final Higher<Higher<writer, W>,T> t) {
        return (Writer)t;
    }
    public static class Instances {

        public static <W> InstanceDefinitions<Higher<writer, W>> definitions(Monoid<W> monoid){
            return new InstanceDefinitions<Higher<writer, W>>() {

                @Override
                public <T, R> Functor<Higher<writer, W>> functor() {
                    return Writer.Instances.functor();
                }

                @Override
                public <T> Pure<Higher<writer, W>> unit() {
                    return Writer.Instances.unit(monoid);
                }

                @Override
                public <T, R> Applicative<Higher<writer, W>> applicative() {
                    return Writer.Instances.applicative(monoid);
                }

                @Override
                public <T, R> Monad<Higher<writer, W>> monad() {
                    return Writer.Instances.monad(monoid);
                }

                @Override
                public <T, R> Maybe<MonadZero<Higher<writer, W>>> monadZero() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<MonadPlus<Higher<writer, W>>> monadPlus() {
                    return Maybe.none();
                }

                @Override
                public <T> MonadRec<Higher<writer, W>> monadRec() {
                    return Instances.monadRec(monoid);
                }

                @Override
                public <T> Maybe<MonadPlus<Higher<writer, W>>> monadPlus(Monoid<Higher<Higher<writer, W>, T>> m) {
                    return Maybe.none();
                }

                @Override
                public <C2, T> Maybe<Traverse<Higher<writer, W>>> traverse() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<Foldable<Higher<writer, W>>> foldable() {
                    return Maybe.just(Writer.Instances.foldable());
                }

                @Override
                public <T> Maybe<Comonad<Higher<writer, W>>> comonad() {
                    return Maybe.none();
                }

                @Override
                public <T> Maybe<Unfoldable<Higher<writer, W>>> unfoldable() {
                    return Maybe.none();
                }
            };
        }
        public static <W> Functor<Higher<writer, W>> functor() {
            return new Functor<Higher<writer, W>>() {
                @Override
                public <T, R> Higher<Higher<writer, W>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<writer, W>, T> ds) {
                    return narrowK(ds).map(fn);
                }
            };
        }
        public static <W> Pure<Higher<writer, W>> unit(Monoid<W> monoid) {
            return new Pure<Higher<writer, W>>() {

                @Override
                public <T> Higher<Higher<writer, W>, T> unit(T value) {
                    return Writer.writer(value,monoid);
                }
            };
        }
        public static <W> Applicative<Higher<writer, W>> applicative(Monoid<W> monoid) {
            return new Applicative<Higher<writer, W>>() {

                @Override
                public <T, R> Higher<Higher<writer, W>, R> ap(Higher<Higher<writer, W>, ? extends Function<T, R>> fn, Higher<Higher<writer, W>, T> apply) {
                    Writer<W, ? extends Function<T, R>> f = narrowK(fn);
                    Writer<W, T> ap = narrowK(apply);
                    return f.flatMap(fn1->ap.map(a->fn1.apply(a)));
                }

                @Override
                public <T, R> Higher<Higher<writer, W>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<writer, W>, T> ds) {
                    return Writer.Instances.<W>functor().map(fn,ds);
                }

                @Override
                public <T> Higher<Higher<writer, W>, T> unit(T value) {
                    return Writer.Instances.<W>unit(monoid).unit(value);
                }
            };
        }
        public static <W> Monad<Higher<writer, W>> monad(Monoid<W> monoid) {
            return new Monad<Higher<writer, W>>() {


                @Override
                public <T, R> Higher<Higher<writer, W>, R> ap(Higher<Higher<writer, W>, ? extends Function<T, R>> fn, Higher<Higher<writer, W>, T> apply) {
                    return Writer.Instances.<W>applicative(monoid).ap(fn,apply);
                }

                @Override
                public <T, R> Higher<Higher<writer, W>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<writer, W>, T> ds) {
                    return Writer.Instances.<W>functor().map(fn,ds);
                }

                @Override
                public <T> Higher<Higher<writer, W>, T> unit(T value) {
                    return Writer.Instances.<W>unit(monoid).unit(value);
                }

                @Override
                public <T, R> Higher<Higher<writer, W>, R> flatMap(Function<? super T, ? extends Higher<Higher<writer, W>, R>> fn, Higher<Higher<writer, W>, T> ds) {
                    return narrowK(ds).flatMap(fn.andThen(h->narrowK(h)));
                }
            };
        }

        public static <W> Foldable<Higher<writer,W>> foldable() {
            return new Foldable<Higher<writer, W>>() {


                @Override
                public <T> T foldRight(Monoid<T> monoid, Higher<Higher<writer, W>, T> ds) {
                    return monoid.foldRight(narrowK(ds).getValue().v1);

                }

                @Override
                public <T> T foldLeft(Monoid<T> monoid, Higher<Higher<writer, W>, T> ds) {
                    return monoid.foldLeft(narrowK(ds).getValue().v1);
                }
            };
        }
        public static <W, T, R> MonadRec<Higher<writer, W>> monadRec(Monoid<W> monoid) {
            return new MonadRec<Higher<writer, W>>() {
                @Override
                public <T, R> Higher<Higher<writer, W>, R> tailRec(T initial, Function<? super T, ? extends Higher<Higher<writer, W>, ? extends Xor<T, R>>> fn) {
                    Writer<W,? extends Xor<T, R>> next[] = new Writer[1];
                    next[0] = Writer.writer(Xor.secondary(initial),monoid);

                    boolean cont = true;
                    do {
                        cont = next[0].visit((p,__) -> p.v1.visit(s -> {
                            next[0] = narrowK(fn.apply(s));
                            return true;
                        }, pr -> false));
                    } while (cont);
                    return next[0].map(Xor::get);
                }
            };
        }


    }
}