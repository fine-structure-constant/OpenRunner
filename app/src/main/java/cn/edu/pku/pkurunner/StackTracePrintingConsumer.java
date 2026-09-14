package cn.edu.pku.pkurunner;

import io.reactivex.functions.Consumer;

public final /* synthetic */ class StackTracePrintingConsumer implements Consumer {
    @Override
    public final void accept(Object obj) {
        ((Throwable) obj).printStackTrace();
    }
}
