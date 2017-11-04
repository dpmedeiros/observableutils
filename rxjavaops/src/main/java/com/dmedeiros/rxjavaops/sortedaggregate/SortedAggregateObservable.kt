package com.dmedeiros.rxjavaops.sortedaggregate

import rx.Emitter
import rx.Observable
import rx.Subscriber

class SortedAggregateObservable<T> private constructor(private val observables: Array<out Observable<T>>,
                                                       private val comparator: Comparator<T>) {

    companion object {
        fun <T> create(vararg observables: Observable<T>, comparator: Comparator<T>) : Observable<T> {
            if (observables.isEmpty()) return Observable.empty()
            val sao = SortedAggregateObservable(observables, comparator = comparator)
            return Observable.create(sao.emitter, Emitter.BackpressureMode.NONE)
        }
    }

    private data class Item<out T>(val item : T,
                                   val associatedSubscriber: SortedAggregateObservable<T>.SortedAggregateSubscriber)

    private val subscribers = ArrayList<SortedAggregateSubscriber>(observables.size)
    private val accumulator = ArrayList<Item<T>>(observables.size)

    private val emitter = { emitter: Emitter<T> ->
        if (observables.isEmpty()) {
            emitter.onCompleted()
        } else {
            subscribers += createSubscribers(emitter)
            subscribeToObservables()
        }
    }

    private fun createSubscribers(emitter: Emitter<T>) = observables.map { SortedAggregateSubscriber(emitter) }.toList()

    private fun subscribeToObservables() =
            observables.forEachIndexed { i, observable -> observable.onBackpressureBuffer().subscribe(subscribers[i]) }

    private open inner class SortedAggregateSubscriber
    constructor(val parentEmitter: Emitter<T>) : Subscriber<T>() {

        override fun onStart() {
            request(1)
        }

        override fun onNext(t: T) {
            synchronized(this@SortedAggregateObservable) {
                accumulator += Item(t, this)
                emitIfReady()?.associatedSubscriber?.request(1)
            }
        }

        override fun onCompleted() {
            synchronized(this@SortedAggregateObservable) {
                if (subscribers.size == 1) {
                    parentEmitter.onCompleted()
                    subscribers.clear()
                    accumulator.clear()
                } else {
                    subscribers -= this
                    emitIfReady()?.associatedSubscriber?.request(1)
                }
            }
        }

        override fun onError(e: Throwable?) {
            synchronized(this@SortedAggregateObservable) {
                parentEmitter.onError(e)
                subscribers.forEach { it.unsubscribe() }
                subscribers.clear()
                accumulator.clear()
            }
        }

        private fun emitIfReady() : Item<T>? {
            if (accumulator.size >= subscribers.size) {
                accumulator.sortWith(Comparator { o1, o2 -> comparator.compare(o1.item, o2.item) })
                val item = accumulator.removeAt(0)
                parentEmitter.onNext(item.item)
                return item
            }
            return null
        }
    }

}