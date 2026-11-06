package net.nikomitk.ssoauth2.pubclient.authcodeflow.states;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import net.nikomitk.ssoauth2.states.ClosableState;
import net.nikomitk.ssoauth2.states.ExpiringState;
import net.nikomitk.ssoauth2.states.OAuth2FlowState;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

final public class AwaitingAuthCode implements AuthCodeFlowState, ClosableState, ExpiringState {

    private final HttpServer server;
    @Getter
    private final String redirectUri;
    @Getter
    private final String stateParam;
    @Getter
    private final String codeVerifier;
    @Getter
    private final String userUri;
    private final CountDownLatch countDownLatch;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> timeout;

    public AwaitingAuthCode(final HttpServer server, final String redirectUri, final String stateParam, final String codeVerifier, final String userUri) {

        this.server = server;
        this.redirectUri = redirectUri;
        this.stateParam = stateParam;
        this.codeVerifier = codeVerifier;
        this.userUri = userUri;
        countDownLatch = new CountDownLatch(1);
    }

    @Override
    public void setTimeout(final ScheduledFuture<?> future) {

        timeout = future;
        if (closed.get())
            future.cancel(false); // closed before the future arrived
    }

    @Override
    public void close() {

        if (closed.compareAndSet(false, true)) {
            final var t = timeout;
            if (t != null)
                t.cancel(false);
            countDownLatch.countDown();

            server.stop(0);
        }
    }

    public void waitUntilFinished() throws InterruptedException {

        countDownLatch.await();
    }

    @Override
    public Set<Class<? extends OAuth2FlowState>> getPossibleNextStates() {

        return Set.of(Initial.class, AuthCodePresent.class);
    }

}