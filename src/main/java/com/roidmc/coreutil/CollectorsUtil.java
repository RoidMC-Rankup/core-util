package com.roidmc.coreutil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class CollectorsUtil {


    public static Collector<JsonElement, JsonArray, JsonArray> toJsonArray(){
        return new CollectorImpl<>(JsonArray::new,JsonArray::add,(left,right)->{
            left.addAll(right);
            return left;
        });
    }
    public static Collector<Map.Entry<String,JsonElement>, JsonObject,JsonObject> toJsonObject(){
        return new CollectorImpl<>(JsonObject::new,(obj,entry)->obj.add(entry.getKey(),entry.getValue()),(left,right)->{
            for (Map.Entry<String, JsonElement> stringJsonElementEntry : right.entrySet()) {
                left.add(stringJsonElementEntry.getKey(), stringJsonElementEntry.getValue());
            }
            return left;
        });
    }

    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;

        CollectorImpl(Supplier<A> supplier,
                      BiConsumer<A, T> accumulator,
                      BinaryOperator<A> combiner,
                      Function<A,R> finisher) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
        }

        CollectorImpl(Supplier<A> supplier,
                      BiConsumer<A, T> accumulator,
                      BinaryOperator<A> combiner) {
            this(supplier, accumulator, combiner, castingIdentity());
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
        }
    }

    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }
}
