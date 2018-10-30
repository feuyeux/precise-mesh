package org.feuyeux.given.proto.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.feuyeux.given.proto.LandingServiceGrpc;
import org.feuyeux.given.proto.TalkRequest;
import org.feuyeux.given.proto.TalkResponse;
import org.feuyeux.given.proto.TalkResult;
import org.feuyeux.given.proto.utils.ProtoUtil;

@Slf4j
public class ProtoClient {
    private final ManagedChannel channel;
    private final LandingServiceGrpc.LandingServiceBlockingStub blockingStub;
    private final LandingServiceGrpc.LandingServiceStub asyncStub;

    public ProtoClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = LandingServiceGrpc.newBlockingStub(channel);
        asyncStub = LandingServiceGrpc.newStub(channel);

    }

    public static void main(String[] args) throws InterruptedException {
        ProtoClient protoClient = new ProtoClient("localhost", 50061);
        try {
            TalkRequest talkRequest = ProtoUtil.buildRequest();
            protoClient.talk(talkRequest);
            protoClient.talkOneAnswerMore(talkRequest);
            protoClient.talkMoreAnswerOne(ProtoUtil.buildRequests());
            protoClient.talkBidirectional(ProtoUtil.buildRequests());
        } finally {
            protoClient.shutdown();
        }
    }

    private static void printTalkResult(TalkResponse talkResponse) {
        final TalkResult result = talkResponse.getResults(0);
        log.info("status={}", talkResponse.getStatus());
        log.info("FIRST LINE: id={}, type={}, kv={}",
            result.getId(),
            result.getType(),
            result.getKvMap());
        log.info("RESPONSE:{}", talkResponse);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public TalkResponse talk(TalkRequest talkRequest) {
        TalkResponse talkResponse = blockingStub.talk(talkRequest);
        printTalkResult(talkResponse);
        return talkResponse;
    }

    public List<TalkResponse> talkOneAnswerMore(TalkRequest request) {
        List<TalkResponse> talkResponseList = new ArrayList<>();
        Iterator<TalkResponse> talkResponses = blockingStub.talkOneAnswerMore(request);
        talkResponses.forEachRemaining(r -> {
            talkResponseList.add(r);
            printTalkResult(r);
        });
        return talkResponseList;
    }

    public TalkResponse talkMoreAnswerOne(List<TalkRequest> requests) throws InterruptedException {
        return talking(requests).get(0);
    }

    public List<TalkResponse> talkBidirectional(List<TalkRequest> requests) throws InterruptedException {
        return talking(requests);
    }

    private List<TalkResponse> talking(List<TalkRequest> requests) throws InterruptedException {
        List<TalkResponse> talkResponseList = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final StreamObserver<TalkRequest> requestObserver = asyncStub.talkBidirectional(
            new StreamObserver<TalkResponse>() {
                @Override
                public void onNext(TalkResponse talkResponse) {
                    printTalkResult(talkResponse);
                    talkResponseList.add(talkResponse);
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("Failed: {0}", Status.fromThrowable(t));
                    finishLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    log.info("Finished");
                    finishLatch.countDown();
                }
            });
        try {
            requests.forEach(request -> {
                if (finishLatch.getCount() > 0) {
                    requestObserver.onNext(request);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {}
                }
            });
        } catch (Exception e) {
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            log.warn("can not finish within 1 minutes");
        }
        return talkResponseList;
    }
}
