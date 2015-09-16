package com.ctzen.config;

/**
 * Common profile names.
 *
 * @author cchang
 */
public interface ConfigProfile {    // NOSONAR: Move constants to a class or enum.
                                    // This allows the constants to be used as annotation parameters.

    String DEV = "dev";
    String UNIT_TEST = "unittest";
    String INTEGRATION_TEST = "integrationtest";
    String QA = "qa";
    String PROD = "prod";

}
