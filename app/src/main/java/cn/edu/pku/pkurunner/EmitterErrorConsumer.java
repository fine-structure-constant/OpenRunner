package cn.edu.pku.pkurunner;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public final /* synthetic */ class EmitterErrorConsumer implements Consumer {

    public final /* synthetic */ ObservableEmitter emitter;

    public EmitterErrorConsumer(ObservableEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public final void accept(Object obj) {
        this.emitter.onError((Throwable) obj);
    }
}
