package com.dmedeiros.rxjavaops.sortedaggregate

import rx.Emitter
import rx.Observable
import rx.Subscriber

class SortedAggregateObservable<T> private constructor(private val observables: Array<out Observable<T>>,
                                                       private val comparator: Comparator<T>) {

    private val subscribers = mutableListOf<SortedAggregateSubscriber>()
    private val accumulator = mutableListOf<T>()

    private open inner class SortedAggregateSubscriber
    constructor(val parentEmitter: Emitter<T>) : Subscriber<T>() {

        override fun onNext(t: T) {
            accumulator += t
            if (accumulator.size >= subscribers.size) {
                accumulator.sortedWith(comparator).forEach { parentEmitter.onNext(it) }
                accumulator.clear()
            }
            nextSubscriber?.request(1)
        }

        override fun onCompleted() {
            if (subscribers.size == 1) {
                parentEmitter.onCompleted()
            } else {
                val next = nextSubscriber
                subscribers -= this
                next?.request(1)
            }
        }

        override fun onError(e: Throwable?) {
            parentEmitter.onError(e)
            subscribers.clear()
            accumulator.clear()
        }

        val nextSubscriber : SortedAggregateSubscriber?
            get() {
                val index = subscribers.indexOf(this)
                return if (index >= 0) { subscribers[(index + 1) % subscribers.size] } else null
            }

    }

    private val emitter = { emitter: Emitter<T> ->
        if (observables.isEmpty()) {
            emitter.onCompleted()
        } else {
            val firstObservable = observables[0]
            val firstSubscriber = object: SortedAggregateSubscriber(emitter) {
                override fun onStart() {
                    request(1)
                }
            }
            subscribers += firstSubscriber
            firstObservable.onBackpressureBuffer().subscribe(firstSubscriber)
            observables.asSequence().drop(1).forEach { observable ->
                val subscriber = SortedAggregateSubscriber(emitter)
                subscribers += subscriber
                observable.onBackpressureBuffer().subscribe(subscriber)
            }
        }
    }

    companion object {
        fun <T> create(vararg observables: Observable<T>, comparator: Comparator<T>) : Observable<T> {
            if (observables.isEmpty()) return Observable.empty()
            val sao = SortedAggregateObservable(observables, comparator = comparator)
            return Observable.create(sao.emitter, Emitter.BackpressureMode.NONE)
        }
    }

}