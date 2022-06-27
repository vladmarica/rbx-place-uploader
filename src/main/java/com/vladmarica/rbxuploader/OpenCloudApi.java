package com.vladmarica.rbxuploader;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;

public final class OpenCloudApi {
  private static final String ROOT_URL = "https://apis.roblox.com/universes/v1";
  private static final String API_KEY_HEADER = "x-api-key";
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final String apiKey;
  private final OkHttpClient client;
  private final ListeningExecutorService executorService;

  public OpenCloudApi(String apiKey) {
    this.apiKey = apiKey;
    this.client = new OkHttpClient();
    this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
  }

  public ListenableFuture<Void> publishPlace(long universeId, long placeId, byte[] data) {
    final String url =
        String.format(
            "%s/%d/places/%d/versions?versionType=Published", ROOT_URL, universeId, placeId);

    return executorService.submit(
        () -> {
          logger.atInfo().log("Uploading to %s", url);

          Request request =
              new Request.Builder()
                  .addHeader(API_KEY_HEADER, apiKey)
                  .url(url)
                  .post(RequestBody.create(MediaType.parse("application/octet-stream"), data))
                  .build();

          try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
              String errorMessage = "Failed to upload place " + placeId;
              JSONObject jsonResponse = new JSONObject(response.body().string());
              if (jsonResponse.has("message")) {
                errorMessage += ": " + jsonResponse.getString("message");
              }

              throw new OpenCloudApiException(errorMessage, response.code());
            }

            return null;
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        });
  }

  public void shutdown() {
    executorService.shutdown();
  }

  public static class OpenCloudApiException extends RuntimeException {
    public OpenCloudApiException(String msg, int code) {
      super(String.format("%d: %s", code, msg));
    }
  }
}
