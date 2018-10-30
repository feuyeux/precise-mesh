package org.feuyeux.given.proto.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.feuyeux.given.proto.ResultType;
import org.feuyeux.given.proto.TalkRequest;
import org.feuyeux.given.proto.TalkResponse;
import org.feuyeux.given.proto.TalkResult;

public class ProtoUtil {
    public static TalkRequest buildRequest() {
        return TalkRequest.newBuilder()
            .setMeta("user=eric")
            .setData("query=ai,from=0,size=1000,order=x,sort=y")
            .build();
    }

    public static TalkResponse buildResponse(TalkRequest request) {
        TalkResponse.Builder response = TalkResponse.newBuilder();
        response.setStatus(200);
        for (int i = 0; i < 10; i++) {
            response.addResults(getTalkResult(request));
        }
        return response.build();
    }

    private static TalkResult getTalkResult(TalkRequest request) {
        HashMap<String, String> kv = new HashMap<>();
        kv.put("request-data", request.getData());
        kv.put("request-meta", request.getMeta());
        kv.put("timestamp", String.valueOf(System.nanoTime()));
        return TalkResult.newBuilder()
            .setId(System.nanoTime())
            .setType(ResultType.SEARCH)
            .putAllKv(kv)
            .build();
    }

    public static List<TalkRequest> buildRequests() {
        return Arrays.asList(TalkRequest.newBuilder()
                .setMeta("user=eric")
                .setData("query=nlu")
                .build(),
            TalkRequest.newBuilder()
                .setMeta("user=eric")
                .setData("query=dialog")
                .build(),
            TalkRequest.newBuilder()
                .setMeta("user=eric")
                .setData("query=ai")
                .build());
    }

    public static List<TalkResponse> buildResponses(TalkRequest request) {
        List<TalkResponse> talkResponses = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TalkResponse response = buildResponse(request);
            talkResponses.add(response);
        }
        return talkResponses;
    }

    public static TalkResponse buildResponse(List<TalkRequest> talkRequests) {
        TalkResponse.Builder response = TalkResponse.newBuilder();
        response.setStatus(200);
        talkRequests.forEach(request -> {
            response.addResults(getTalkResult(request));
        });
        return response.build();
    }
}
