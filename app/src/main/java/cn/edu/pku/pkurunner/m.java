package cn.edu.pku.pkurunner;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public final /* synthetic */ class m implements Consumer {

    /* renamed from: a, reason: collision with root package name */
    public final /* synthetic */ ObservableEmitter f7208a;

    public m(ObservableEmitter emitter) {
        this.f7208a = emitter;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f7208a.onError((Throwable) obj);
    }
}
