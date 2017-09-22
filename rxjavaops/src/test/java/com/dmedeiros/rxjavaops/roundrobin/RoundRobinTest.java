package com.dmedeiros.rxjavaops.roundrobin;

import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func2;

import java.util.Arrays;
import java.util.List;

/**
 * Created by dmedeiros on 8/3/17.
 */
public class RoundRobinTest {

    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    private final Func2<Integer, Integer, Integer> mComparator = (integer, integer2) -> integer - integer2;

//    @Test
//    public void testInorderOperator() {
//        List<Integer> result = Observable.just(
//            Observable.just(1, 3, 4, 2),
//            Observable.just(34, 12, 8, 16, 27),
//            Observable.just(9)
//        ).toList().lift(new RoundRobinOperator.Inorder<>(mComparator)).toList().toBlocking().first();
//        assertEquals(Arrays.asList(1, 3, 4, 2, 9, 34, 12, 8, 16, 27), result);
//    }

    @Test
    public void testRoundRobinOperator() {
        List<Integer> result = Observable.just(1, 3, 4, 2).lift(new RoundRobinOperator<>(Arrays.asList(
                Observable.just(34, 12, 8, 16, 27),
                Observable.just(9)
        ))).toList().toBlocking().first();
        Assert.assertEquals(Arrays.asList(1, 34, 9, 3, 12, 4, 8, 2, 16, 27), result);
    }
}