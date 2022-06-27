package com.vladmarica.rbxuploader;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PlaceUploader {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final String apiKey;
  private final long universeId;
  private final List<Long> placeIds;
  private final String fileName;

  public PlaceUploader(String apiKey, long universeId, List<Long> placeIds, String fileName) {
    this.apiKey = apiKey;
    this.universeId = universeId;
    this.placeIds = placeIds;
    this.fileName = fileName;
  }

  public void execute() throws FileNotFoundException, IOException, PlaceUploadException {
    File file = new File(fileName);
    if (!file.exists()) {
      throw new FileNotFoundException(fileName);
    }

    if (!file.isFile()) {
      throw new PlaceUploadException(fileName + " is not a file");
    }

    if (!file.getName().endsWith("rbxl")) {
      throw new PlaceUploadException("File " + fileName + "is not a .rbxl file");
    }

    OpenCloudApi api = new OpenCloudApi(apiKey);
    final byte[] fileData = Files.readAllBytes(Path.of(file.toURI()));

    Futures.addCallback(
        Futures.allAsList(
            placeIds.stream()
                .map(placeId -> api.publishPlace(universeId, placeId, fileData))
                .toList()),
        new FutureCallback<>() {
          @Override
          public void onSuccess(@Nullable List<Void> result) {
            logger.atInfo().log("Successfully finished uploading to all places");
            api.shutdown();
          }

          @Override
          public void onFailure(Throwable t) {
            logger.atSevere().withCause(t).log();
            api.shutdown();
          }
        },
        MoreExecutors.directExecutor());
  }
}
