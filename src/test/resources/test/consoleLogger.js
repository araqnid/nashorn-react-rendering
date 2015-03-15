define(function() {
  var consoleLogger = {
    logMessage: function() {
      console.log("logMessage");
    },
    logMessageWithObject: function() {
      console.log("logMessageWithObject", { a : 1 }, ["foo", "bar"], function() { return "bleah" });
    },
    logInfo: function() {
      console.info("logInfo");
    },
    logInfoWithObject: function() {
      console.info("logInfoWithObject", { a : 1 }, ["foo", "bar"], function() { return "bleah" });
    },
    logWarning: function() {
      console.warn("logWarning");
    },
    logWarningWithObject: function() {
      console.warn("logWarningWithObject", { a : 1 }, ["foo", "bar"], function() { return "bleah" });
    },
    logError: function() {
      console.error("logError");
    },
    logErrorWithObject: function() {
      console.error("logErrorWithObject", { a : 1 }, ["foo", "bar"], function() { return "bleah" });
    }
  };
  return consoleLogger;
});

