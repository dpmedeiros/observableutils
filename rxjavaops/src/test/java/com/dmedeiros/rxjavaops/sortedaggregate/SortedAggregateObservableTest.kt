package com.dmedeiros.rxjavaops.sortedaggregate

import org.junit.Test
import rx.Observable
import kotlin.Comparator
import kotlin.test.assertEquals

class SortedAggregateObservableTest {
    @Test
    fun testMultipleObservables() {
        val a = Observable.from(arrayOf(53, 22, 1, 129, 18, 6))
        val b = Observable.from(arrayOf(64, 128, 12, 3, 5, 9))
        val c = Observable.from(arrayOf(2, 3, 4, 5, 6))
        val d = Observable.from(arrayOf(12, 7, 19))

        val list = SortedAggregateObservable.create(a, b, c, d, comparator = Comparator { o1: Int, o2: Int -> o1 - o2 })
            .toList().toBlocking().first()

        assertEquals(listOf(2, 3, 4, 5, 6, 12, 7, 19, 53, 22, 1, 64, 128, 12, 3, 5, 9, 129, 18, 6), list)
    }
}