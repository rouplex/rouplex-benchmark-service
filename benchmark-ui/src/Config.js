var config = module.exports = {};

config.baseUrl = "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT";
config.mainUrl = config.baseUrl + "/index.html";
config.restUrl = config.baseUrl + "/rouplex/benchmark";
config.signInUrl = config.restUrl + "/auth/sign-in";
config.signInViaGoogleUrl = config.signInUrl + "?provider=google";
config.signInViaRouplexUrl = config.signInUrl + "?provider=rouplex";
config.signOutUrl = config.restUrl + "/auth/sign-out";
