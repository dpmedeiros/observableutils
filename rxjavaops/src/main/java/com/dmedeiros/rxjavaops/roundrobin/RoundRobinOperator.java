package com.dmedeiros.rxjavaops.roundrobin;

import java.util.*;

import rx.Observable;
import rx.Subscriber;

public final class RoundRobinOperator<T> implements Observable.Operator<T, T> {
    private final List<Observable<T>> mObservables;
    private final List<RoundRobinSubscriber> mRoundRobinSubscribers;
    public RoundRobinOperator(List<Observable<T>> observables) {
        mObservables = new ArrayList<>(observables);
        mRoundRobinSubscribers = new ArrayList<>(observables.size() + 1);
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> downstreamSubscriber) {
        RoundRobinSubscriber firstSubscriber =
                new RoundRobinSubscriber(0, downstreamSubscriber);
        mRoundRobinSubscribers.add(firstSubscriber);
        for (int i = 0; i < mObservables.size(); i++) {
            RoundRobinSubscriber childSubscriber =
                    new RoundRobinSubscriber(i + 1, downstreamSubscriber);
            mRoundRobinSubscribers.add(childSubscriber);
            downstreamSubscriber.add(childSubscriber);
        }
        Subscriber<? super T> upstreamSubscriber = new Subscriber<T>() {
            @Override
            public void onCompleted() {
                mRoundRobinSubscribers.get(0).onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                mRoundRobinSubscribers.forEach(RoundRobinSubscriber::unsubscribe);
                if (!downstreamSubscriber.isUnsubscribed()) {
                    downstreamSubscriber.onError(e);
                }
            }

            @Override
            public void onNext(T itemFromFirstObservable) {
                mRoundRobinSubscribers.get(0).onNext(itemFromFirstObservable);
            }
        };
        downstreamSubscriber.add(upstreamSubscriber);
        for (int i = 0; i < mObservables.size(); i++) {
            mObservables.get(i).subscribe(mRoundRobinSubscribers.get(i + 1));
        }
        return upstreamSubscriber;
    }

    private final class RoundRobinSubscriber extends Subscriber<T> {
        private final Subscriber<? super T> mDownstreamSubscriber;
        private final int mIndex;

        private RoundRobinSubscriber(int index,
                                     Subscriber<? super T> downstreamSubscriber) {
            mIndex = index;
            mDownstreamSubscriber = downstreamSubscriber;
        }

        @Override
        public void onStart() {
            if (mIndex == 0) {
                request(1);
            } else {
                request(0);
            }
        }

        @Override
        public void onCompleted() {
            mRoundRobinSubscribers.remove(this);
            if (mRoundRobinSubscribers.size() <= 0 && !mDownstreamSubscriber.isUnsubscribed()) {
                mDownstreamSubscriber.onCompleted();
            } else {
                int numSubscribers = mRoundRobinSubscribers.size();
                requestNextItem(mIndex % numSubscribers);
            }
        }

        @Override
        public void onError(Throwable e) {
            mRoundRobinSubscribers.forEach(Subscriber::unsubscribe);
            if (!mDownstreamSubscriber.isUnsubscribed()) {
                mDownstreamSubscriber.onError(e);
            }
        }

        @Override
        public void onNext(T item) {
            if (!mDownstreamSubscriber.isUnsubscribed()) {
                mDownstreamSubscriber.onNext(item);
                int numSubscribers = mRoundRobinSubscribers.size();
                requestNextItem((mIndex + 1) % numSubscribers);
            }
        }

        private void requestNextItem(int nextSubscriberIndex) {
            if (!mDownstreamSubscriber.isUnsubscribed()) {
                RoundRobinSubscriber nextSubscriber = mRoundRobinSubscribers.get(nextSubscriberIndex);
                nextSubscriber.request(1);
            }
        }
    }
}
