package com.vladmarica.rbxuploader;

import com.google.common.flogger.FluentLogger;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class App {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String OPTION_API_KEY = "key";
  private static final String OPTION_UNIVERSE_ID = "universe";
  private static final String OPTION_PLACE_IDS = "place";
  private static final String OPTION_FILENAME = "rbxl";
  private static final String OPTION_HELP = "help";

  public static void main(String[] args) {
    CommandLineParser parser = new DefaultParser();
    Options options = createOptions();

    try {
      CommandLine line = parser.parse(options, args);
      if (line.hasOption(OPTION_HELP)) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("rbx-place-uploader", options);
        return;
      }

      String apiKey = line.getOptionValue(OPTION_API_KEY);
      if (apiKey == null) {
        throw new IllegalArgumentException("Missing argument: " + OPTION_API_KEY);
      }

      String universeIdString = line.getOptionValue(OPTION_UNIVERSE_ID);
      if (universeIdString == null) {
        throw new IllegalArgumentException("Missing argument: " + OPTION_UNIVERSE_ID);
      }
      long universeId = Long.parseLong(universeIdString);

      String[] placeIdStrings = line.getOptionValues(OPTION_PLACE_IDS);
      if (placeIdStrings == null) {
        throw new IllegalStateException("Missing arguments: " + OPTION_PLACE_IDS);
      }
      List<Long> placeIds = Arrays.stream(placeIdStrings).map(Long::parseLong).toList();

      String fileName = line.getOptionValue(OPTION_FILENAME);
      if (fileName == null) {
        throw new IllegalStateException("Missing argument: " + OPTION_FILENAME);
      }

      new PlaceUploader(apiKey, universeId, placeIds, fileName).execute();
    } catch (ParseException ex) {
      logger.atSevere().withCause(ex).log("Failed to parse options");
    } catch (NumberFormatException ex) {
      logger.atSevere().withCause(ex).log("Failed to parse ID");
    } catch (IOException | PlaceUploadException ex) {
      logger.atSevere().withCause(ex).log("Failed to upload place");
    }
  }

  private static Options createOptions() {
    Options options = new Options();

    options.addOption(
        Option.builder("h").longOpt(OPTION_HELP).desc("Display this help message").build());
    options.addOption(
        Option.builder("k")
            .longOpt(OPTION_API_KEY)
            .hasArg()
            .desc("Your Roblox OpenCloud API key")
            .build());
    options.addOption(Option.builder("u").longOpt(OPTION_UNIVERSE_ID).hasArg().build());
    options.addOption(
        Option.builder("p")
            .longOpt(OPTION_PLACE_IDS)
            .hasArgs()
            .numberOfArgs(Option.UNLIMITED_VALUES)
            .desc("The place ID(s) to upload to")
            .build());
    options.addOption(Option.builder("f").longOpt(OPTION_FILENAME).hasArg().build());

    return options;
  }
}
