package com.mc.rxjava.demo.rxjavademo.utils

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    clear() // compositeDisposable is being reused multiple times in this app
    add(disposable)
}