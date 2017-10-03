var config = module.exports = {};

config.env = "prod";
config.version = "1.0.0-" + config.env;

config.host = config.env == "dev" ? "http://localhost:8080" : "https://www.rouplex-demo.com";
config.basePath = ""; // config.env == "dev" ? "/benchmark-service-provider-jersey-1.0.0-SNAPSHOT" : "";
config.baseUrl = config.host + config.basePath;
config.mainUrl = config.baseUrl + "/index.html";
config.restUrl = config.baseUrl + "/rest/benchmark";

config.authUrl = config.restUrl + "/auth";
config.startSignInUsingGoogleOauth2Url = config.authUrl + "/start-sign-in-using-google-oauth2";
config.finishSignInUsingGoogleOauth2Url = config.authUrl + "/finish-sign-in-using-google-oauth2";
config.signInUsingBasicAuthUrl = config.authUrl + "/sign-in-using-basic-auth";
config.signOutUrl = config.authUrl + "/sign-out";
config.getSessionInfoUrl = config.authUrl + "/session-info";

config.tcpEchoBenchmarkUrl = config.restUrl + "/orchestrator/tcp-echo-benchmarks";

config.ec2InstanceTypes = [
  {key: "T2Nano", value: "EC2 T2 Nano", cost: 0.0059},
  {key: "T2Micro", value: "EC2 T2 Micro", cost: 0.0120},
  {key: "T2Small", value: "EC2 T2 Small", cost: 0.0230},
  {key: "T2Medium", value: "EC2 T2 Medium", cost: 0.0470},
  {key: "T2Large", value: "EC2 T2 Large", cost: 0.0940},
  {key: "T2Xlarge", value: "EC2 T2 XLarge", cost: 0.1880},
  {key: "T22xlarge", value: "EC2 T2 2XLarge", cost: 0.3760},

  {key: "M3Medium", value: "EC2 M3 Medium", cost: 0.0670},
  {key: "M3Large", value: "EC2 M3 Large", cost: 0.1330},
  {key: "M3Xlarge", value: "EC2 M3 XLarge", cost: 0.2660},
  {key: "M32xlarge", value: "EC2 M3 2XLarge", cost: 0.5320},

  {key: "M4Large", value: "EC2 M4 Large", cost: 0.1000},
  {key: "M4Xlarge", value: "EC2 M4 XLarge", cost: 0.2000},
  {key: "M42xlarge", value: "EC2 M4 2XLarge", cost: 0.4000},
  {key: "M44xlarge", value: "EC2 M4 4XLarge", cost: 0.8000},
  {key: "M410xlarge", value: "EC2 M4 10XLarge", cost: 2.0000},
  {key: "M416xlarge", value: "EC2 M4 16XLarge", cost: 3.2000},

  {key: "C38xlarge", value: "EC2 C3 8XLarge", cost: 1.680}
];

config.ec2Regions = [
  {key: "US_WEST_2", value: "EC2 Oregon (us-west-2)"}
];

config.nioProviders = [
  {key: "CLASSIC_NIO", value: "Classic NIO (No SSL support)"},
  {key: "ROUPLEX_NIOSSL", value: "Rouplex NIO (With SSL support)"}

  // todo: include the following provider later, for comparison with ROUPLEX_NIOSSL
  // {key: "THIRD_PARTY_SSL", value: "Third Party NIO (With SSL support)"}
];

// experimental from here on
config.paths = [
  {path: "/benchmark/start", label: "Start Benchmark"},
  {path: "/benchmark/list", label: "List Benchmarks"}
];

config.getSanitizedPath = function (unsanitizedPath) {
  if (!unsanitizedPath) {
    unsanitizedPath = window.location.search;
    if (unsanitizedPath.startsWith("?")) {
      unsanitizedPath = unsanitizedPath.substring(1);
    }
  }

  var sanitizedPath = config.paths[0].path;
  config.paths.map(function (x) {
    if (x.path === unsanitizedPath) {
      sanitizedPath = unsanitizedPath;
    }
  });

  return sanitizedPath;
};
