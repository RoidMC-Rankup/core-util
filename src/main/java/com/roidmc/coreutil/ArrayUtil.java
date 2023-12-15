package com.roidmc.coreutil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.function.BiFunction;

public class ArrayUtil {

    public static <T,R> R[] map(T[] in, R[] out, BiFunction<T,Integer,R> function){
        for(int i = 0; i< in.length; i++){
            out[i]=function.apply(in[i],i);
        }
        return out;
    }

    public static <T> boolean includes(T key, T[] array) {
        for(T value : array){
            if(Objects.equals(key,value))return true;
        }
        return false;
    }


    public static JsonElement[] fromJsonArray(JsonArray array){
        JsonElement[] elements = new JsonElement[array.size()];
        for (int i = 0; i < array.size(); i++){
            elements[i] = array.get(i);
        }
        return elements;
    }
}
