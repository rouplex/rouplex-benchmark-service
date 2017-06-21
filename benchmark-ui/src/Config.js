var config = module.exports = {};

config.env = "prod";

config.autologin = false; //= config.env == "dev";

config.host = config.env == "dev" ? "http://localhost:8080" : "https://www.rouplex-demo.com:443";
config.baseUrl = config.host + "/benchmark-service-provider-jersey-1.0.0-SNAPSHOT";
config.mainUrl = config.baseUrl + "/index.html";
config.restUrl = config.baseUrl + "/rouplex/benchmark";
config.signInUrl = config.restUrl + "/auth/sign-in";
config.signInViaGoogleUrl = config.signInUrl + "?provider=google";
config.signInViaRouplexUrl = config.signInUrl + "?provider=rouplex";
config.signOutUrl = config.restUrl + "/auth/sign-out";
config.startTcpBenchmarkUrl = config.restUrl + "/orchestrator/tcp/start";

config.ec2InstanceTypes = [
  {key: "EC2_T2Nano", value: "T2 Nano"},
  {key: "EC2_T2Micro", value: "T2 Micro"},
  {key: "EC2_T2Small", value: "T2 Small"},
  {key: "EC2_T2Medium", value: "T2 Medium"},
  {key: "EC2_T2Large", value: "T2 Large"},
  {key: "EC2_T2Xlarge", value: "T2 XLarge"},
  {key: "EC2_T22xlarge", value: "T2 2XLarge"},

  {key: "EC2_M3Medium", value: "M3 Medium"},
  {key: "EC2_M3Large", value: "M3 Large"},
  {key: "EC2_M3Xlarge", value: "M3 XLarge"},
  {key: "EC2_M32xlarge", value: "M3 2XLarge"},

  {key: "EC2_M4Large", value: "M4 Large"},
  {key: "EC2_M4Xlarge", value: "M4 XLarge"},
  {key: "EC2_M42xlarge", value: "M4 2XLarge"},
  {key: "EC2_M44xlarge", value: "M4 4XLarge"},
  {key: "EC2_M410xlarge", value: "M4 10XLarge"},
  {key: "EC2_M416xlarge", value: "M4 16XLarge"}
];

config.ec2Regions = [
  {key: "EC2_US_WEST_2", value: "EC2 Oregon (us-west-2)"}
];

config.nioProviders = [
    {key: "CLASSIC_NIO", value: "Classic NIO (No SSL support)"},
    {key: "ROUPLEX_NIOSSL", value: "Rouplex NIO (With SSL support)"},
    {key: "SCALABLE_SSL", value: "Scalable NIO (With SSL support)"}
];
