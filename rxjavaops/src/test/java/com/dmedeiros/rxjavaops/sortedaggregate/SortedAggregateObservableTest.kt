package com.dmedeiros.rxjavaops.sortedaggregate

import org.junit.Test
import rx.Notification
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

    @Test
    fun testEmptyObservable() {
        val notification =
            SortedAggregateObservable.create(Observable.empty(), comparator = Comparator<Any> { _, _ ->  0 })
                .materialize().toBlocking().first()

        assertEquals(Notification.Kind.OnCompleted, notification.kind)
    }

    @Test
    fun testNoObservables() {
        val notification =
            SortedAggregateObservable.create(*emptyArray(), comparator = Comparator<Any> { _, _ -> 0} )
                .materialize().toBlocking().first()

        assertEquals(Notification.Kind.OnCompleted, notification.kind)
    }

    private fun <T> n(vararg t: T) = t.map { Notification.createOnNext(it) }.toTypedArray()

    @Test
    fun testWithErrors() {
        val errorNotification = Notification.createOnError<Int>(Throwable())
        val a = Observable.from(arrayOf(1, 2, 3, 4))
        val b = Observable.from(arrayOf(*n(2, 3, 5, 7), errorNotification, n(8, 9, 10))).dematerialize<Int>()

        val list = SortedAggregateObservable.create(a, b, comparator = Comparator { o1: Int, o2: Int -> o1 - o2})
                .materialize().toList().toBlocking().first()

        assertEquals(listOf(*n(1, 2, 2, 3, 3, 4, 5, 7), errorNotification), list)
    }
}