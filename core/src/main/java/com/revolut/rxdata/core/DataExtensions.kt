@file:Suppress("unused")

package com.revolut.rxdata.core

import io.reactivex.*
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

fun <T> Single<Data<T>>.extractDataOrError(): Single<T> = extractError().extractData()

fun <T> Single<Data<T>>.extractData(): Single<T> =
    filter { it.content != null }.map { it.content!! }.toSingle()

fun <T> Observable<Data<T>>.extractDataOrError(): Observable<T> = extractError().extractData()

fun <T> Observable<Data<T>>.extractData(): Observable<T> =
    filter { it.content != null }.map { it.content!! }

fun <T> Observable<Data<T>>.extractFirstData(): Single<T> =
    filter { data -> data.content != null }
        .map { data -> data.content ?: throw IllegalStateException("Data is not available") }
        .firstOrError()

fun <T> Observable<Data<T>>.extractData(default: T): Observable<T> =
    filter { it.content != null || !it.loading }.map { it.content ?: default }

fun <T> Observable<Data<T>>.mapErrorToData(content: T): Observable<Data<T>> = map {
    if (it.error != null) {
        Data(content = content, loading = it.loading, error = null)
    } else {
        it
    }
}

fun <T> Observable<Data<T>>.extractErrorStrict(): Observable<Data<T>> = flatMap {
    if (it.error != null) {
        Observable.error(it.error)
    } else {
        Observable.just(it)
    }
}

fun <T> Observable<Data<T>>.toCompletableWhenLoaded(transformer: (data: Data<T>) -> Completable): Completable =
    filterWhileLoading().flatMapCompletable { data -> transformer(data) }

fun <T> Single<Data<T>>.extractError(): Single<Data<T>> = flatMap {
    if (it.error != null) {
        Single.error(it.error)
    } else {
        Single.just(it)
    }
}

fun <T> Maybe<Data<T>>.extractError(): Maybe<Data<T>> = flatMap {
    if (it.error != null) {
        Maybe.error(it.error)
    } else {
        Maybe.just(it)
    }
}

fun <T> Flowable<Data<T>>.extractDataOrError(): Flowable<T> = extractError().extractData()

fun <T> Flowable<Data<T>>.extractData(): Flowable<T> =
    filter { it.content != null }.map { it.content!! }

fun <T> Flowable<Data<T>>.extractError(): Flowable<Data<T>> = flatMap {
    if (it.error != null) {
        Flowable.error(it.error)
    } else {
        Flowable.just(it)
    }
}

fun <T> Observable<Data<T>>.takeUntilLoaded(): Observable<Data<T>> =
    takeUntil { data -> !data.loading }

fun <T> Observable<Data<T>>.filterWhileLoading(): Observable<Data<T>> =
    filter { data -> !data.loading }

fun <T> Observable<Data<T>>.skipWhileLoading(): Observable<Data<T>> =
    skipWhile { data -> data.loading }

fun <T> Observable<Data<T>>.skipWhileDataIsNullAndLoading(): Observable<Data<T>> =
    skipWhile { data -> data.content == null && data.loading }

fun <T> Throwable.toData(): Data<T> = Data(
    error = this,
    loading = false
)

fun <T> T?.toData(loading: Boolean = false, error: Throwable? = null): Data<T> =
    Data(
        content = this,
        error = error,
        loading = loading
    )

fun <T> Observable<Data<T>>.skipWhileDataIsNullOrLoading(): Observable<Data<T>> =
    skipWhile { data -> data.content == null || data.loading }

fun <T> Data<T>.loadedWithoutError() = !loading && error == null

fun <T> Data<T>.requireContent(): T = content ?: kotlin.error("content must not be null")

/**
 * Maps data field of Data wrapper keeping error and loading fields.
 * Block is not invoked if data is null.
 */
inline fun <T, R> Data<T>.mapData(block: (T) -> R?): Data<R> = try {
    Data(
        content = content?.let { block(it) },
        error = error,
        loading = loading
    )
} catch (t: Throwable) {
    Data(
        content = null,
        error = t,
        loading = loading
    )
}

fun <T> Data<T>.isEmpty(): Boolean = content == null

fun <T> Data<T>.isNotEmpty(): Boolean = content != null

fun <T, R> Observable<Data<T>>.mapData(block: (T) -> R?): Observable<Data<R>> =
    map { data -> data.mapData(block) }

fun <T> Observable<Data<List<T>>>.emptyListIfNullContent(): Observable<Data<List<T>>> =
    map { data ->
        if (data.content == null) {
            data.copy(content = emptyList())
        } else {
            data
        }
    }

/**
 * Switch maps Observable<Data<T>> stream into Observable<Data<R>> invoking given block only if data is not null
 * in emitted item, otherwise Data<R> will be emitted with null data keeping errors/loading fields.
 */
fun <T, R> Observable<Data<T>>.switchMapData(block: (T) -> Observable<Data<R>>): Observable<Data<R>> =
    switchMap {
        if (it.content != null) {
            block(it.content)
        } else {
            Observable.just(Data(error = it.error, loading = it.loading))
        }
    }

fun <T, R> Observable<Data<T>>.switchMapDataContent(block: (T) -> Observable<R>): Observable<Data<R>> =
    switchMap { original ->
        if (original.content != null) {
            try {
                block(original.content).map { transformed ->
                    transformed.toData(
                        loading = original.loading,
                        error = original.error
                    )
                }
            } catch (t: Throwable) {
                Observable.just(
                    Data<R>(
                        error = combineErrors(original.error, t),
                        loading = original.loading
                    )
                )
            }
        } else {
            Observable.just(
                Data(
                    error = original.error,
                    loading = original.loading
                )
            )
        }
    }

fun <T, R> Observable<Data<T>>.switchMapDataContentSingle(block: (T) -> Single<R>): Observable<Data<R>> =
    switchMapSingle { original ->
        if (original.content != null) {
            try {
                block(original.content).map { transformed ->
                    transformed.toData(
                        loading = original.loading,
                        error = original.error
                    )
                }
            } catch (t: Throwable) {
                Single.just(
                    Data<R>(
                        error = combineErrors(original.error, t),
                        loading = original.loading
                    )
                )
            }
        } else {
            Single.just(
                Data(
                    error = original.error,
                    loading = original.loading
                )
            )
        }
    }

fun combineErrors(
    firstError: Throwable?,
    secondError: Throwable?
): Throwable? {
    return when {
        firstError != null && secondError != null -> CompositeException(
            listOf(
                firstError,
                secondError
            )
        )
        firstError != null -> firstError
        secondError != null -> secondError
        else -> null
    }
}

inline fun <T, R> Data<T>.map(mapper: (T?) -> R?): Data<R> =
    Data(content = mapper(content), error = error, loading = loading)

@Suppress("UNCHECKED_CAST")
fun <T> Observable<Data<T>>.doOnLoaded(onNext: (T) -> Unit): Observable<Data<T>> =
    doOnNext { data ->
        if (!data.loading && data.error == null && data.content != null) {
            onNext(data.content)
        }
    }

fun <T> Observable<Data<T>>.doOnDataError(onError: (Throwable) -> Unit): Observable<Data<T>> =
    doOnNext { data ->
        data.error?.let { onError(it) }
    }

fun <T> Observable<Data<T>>.filterError(): Observable<Data<T>> = filter { it.error == null }

fun <T> Observable<Data<T>>.extractDataIfLoaded(): Observable<T> {
    return filter { it.content != null && !it.loading }
        .map { it.content!! }
}

fun <T> Observable<Data<T>>.filterData(condition: (T) -> Boolean): Observable<Data<T>> =
    filter { data ->
        if (data.content != null) {
            condition(data.content)
        } else {
            true
        }
    }

fun <T> Observable<Data<T>>.filterDataChanged(): Observable<Data<T>> {
    return filter { it.content != null }
        .distinctUntilChanged({ t1, t2 -> t1.content?.equals(t2.content) ?: false })
}

fun <T> Observable<Data<T>>.extractError(): Observable<Data<T>> {
    return flatMap {
        if (it.error != null && it.content == null) {
            Observable.error(it.error)
        } else {
            Observable.just(it)
        }
    }
}

fun <T> Single<T>.sendDataOrError(where: BehaviorSubject<Data<T>>): Disposable =
    subscribe({
        where.sendData(it)
    }, where::sendError)

fun <T> BehaviorSubject<Data<T>>.sendLoading(loading: Boolean, resetError: Boolean = true) {
    val data = value?.content
    val error = if (resetError) null else value?.error
    onNext(Data(data, loading = loading, error = error))
}

fun <T> Single<T>.sendLoading(
    where: BehaviorSubject<Data<T>>,
    loading: Boolean = true,
    resetError: Boolean = true
): Single<T> =
    doOnSubscribe { where.sendLoading(loading, resetError) }

fun <T> BehaviorSubject<Data<T>>.sendData(
    value: T?,
    loading: Boolean = false,
    error: Throwable? = null
) {
    onNext(Data(value, loading = loading, error = error))
}

fun <T> BehaviorSubject<Data<T>>.sendError(error: Throwable) {
    val data = value?.content
    onNext(Data(data, loading = false, error = error))
}

fun <T> BehaviorSubject<Data<T>>.resendData() {
    value?.let {
        onNext(it)
    }
}

fun <T> BehaviorSubject<Data<T>>.isDataLoading(): Boolean = value?.loading ?: false

fun <Domain> Observable<Data<Domain>>.filterExpiredCache(expired: (Domain) -> Boolean): Observable<Data<Domain>> =
    map {
        val value = it.content
        val error = it.error
        val isLoading = it.loading

        if (value != null && expired(value) && isLoading && error == null) {
            //cache expired but is still loading -> we skip value
            Data(loading = true)
        } else {
            it
        }
    }

inline fun <T, D> Observable<Data<T>>.mapDataWithError(
    crossinline content: (T) -> D?,
    crossinline error: () -> Throwable
): Observable<Data<D>> =
    map { data ->
        val mappedItem = data.content?.let { content.invoke(it) }
        val mappedError = if (!data.loading && mappedItem == null) error.invoke() else data.error

        Data(content = mappedItem, error = mappedError, loading = data.loading)
    }

fun <T> T.asDataObservable(): Observable<Data<T>> = Observable.just(Data(this))

fun <T> T.asDataSingle(): Single<Data<T>> = Single.just(Data(this))
