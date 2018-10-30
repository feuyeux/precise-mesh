package org.feuyeux.given.proto.server;

import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.feuyeux.given.proto.LandingServiceGrpc;
import org.feuyeux.given.proto.TalkRequest;
import org.feuyeux.given.proto.TalkResponse;
import org.feuyeux.given.proto.utils.ProtoUtil;

/**
 * @author 六翁 lu.hl@alibaba-inc.com
 * @date 2018/10/22
 */
@Slf4j
public class LandingServiceImpl extends LandingServiceGrpc.LandingServiceImplBase {
    @Override
    public void talk(TalkRequest request, StreamObserver<TalkResponse> responseObserver) {
        log.debug("REQUEST:{}", request.toString());
        final TalkResponse response = ProtoUtil.buildResponse(request);
        log.debug("RESPONSE:{}", response.toString());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void talkOneAnswerMore(TalkRequest request, StreamObserver<TalkResponse> responseObserver) {
        final List<TalkResponse> responses = ProtoUtil.buildResponses(request);
        responses.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<TalkRequest> talkMoreAnswerOne(StreamObserver<TalkResponse> responseObserver) {
        final List<TalkRequest> talkRequests = new ArrayList<>();
        return new StreamObserver<TalkRequest>() {
            @Override
            public void onNext(TalkRequest request) {
                talkRequests.add(request);
            }

            @Override
            public void onError(Throwable t) {
                log.error("talkBidirectional onError");
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(ProtoUtil.buildResponse(talkRequests));
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<TalkRequest> talkBidirectional(StreamObserver<TalkResponse> responseObserver) {
        return new StreamObserver<TalkRequest>() {
            @Override
            public void onNext(TalkRequest request) {
                responseObserver.onNext(ProtoUtil.buildResponse(request));
            }

            @Override
            public void onError(Throwable t) {
                log.error("talkBidirectional onError");
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
