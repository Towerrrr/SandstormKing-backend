package com.t0r.sandstormkingbackend.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class MyListUtil {

    // 注意原来的引用指向的列表没有变化
    public static <T> LinkedList<T> shuffleLinkedList(LinkedList<T> list) {
        ArrayList<T> objects = new ArrayList<>(list);
        Collections.shuffle(objects);
        return new LinkedList<>(objects);
    }

}
