package com.ctzen.config;

/**
 * Common profile names.
 *
 * @author cchang
 */
public interface ConfigProfile {    // NOSONAR: Move constants to a class or enum.
                                    // This allows the constants to be used as annotation parameters.

    static final String DEV = "dev";
    static final String UNIT_TEST = "unittest";
    static final String INTEGRATION_TEST = "integrationtest";
    static final String QA = "qa";
    static final String PROD = "prod";

}
