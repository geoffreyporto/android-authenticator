package org.tokend.authenticator.base.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Contains common repository logic. Is a parent of all repositories.
 */
abstract class Repository {
    protected val errorsSubject: PublishSubject<Throwable> =
            PublishSubject.create<Throwable>()
    /**
     * Emits repository errors.
     */
    val errors: Observable<Throwable>
        get() = errorsSubject

    protected val loadingSubject: BehaviorSubject<Boolean> =
            BehaviorSubject.createDefault(false)
    /**
     * Emits repository loading states.
     * Will emit actual loading state on subscribe.
     * @see Repository.isLoading
     */
    val loading: Observable<Boolean>
        get() = loadingSubject

    /**
     * Indicates whether repository is loading something now.
     */
    var isLoading: Boolean = false
        protected set(value) {
            if (field != value) {
                field = value
                loadingSubject.onNext(value)
            }
            field = value
        }

    /**
     * Indicates whether data is in actual state.
     */
    var isFresh = false
        protected set
    /**
     * Indicates whether repository has no data because it was never updated.
     */
    var isNeverUpdated = true
        protected set

    /**
     * Instantly starts data update.
     */
    abstract fun update(): Completable

    /**
     * Starts data update only on subscription.
     */
    open fun updateDeferred(): Completable {
        return Completable.defer { update() }
    }

    /**
     * Marks data as not fresh
     * @see Repository.isFresh
     */
    open fun invalidate() {
        synchronized(this) {
            isFresh = false
        }
    }

    /**
     * Instantly starts data update if it's not fresh.
     * @see Repository.isFresh
     */
    open fun updateIfNotFresh(): Completable {
        return synchronized(this) {
            if (!isFresh) {
                update()
            } else {
                Completable.complete()
            }
        }
    }

    /**
     * Starts data update it it's not fresh only on subscription.
     * @see Repository.isFresh
     */
    open fun updateIfNotFreshDeferred(): Completable {
        return Completable.defer { updateIfNotFresh() }
    }
}