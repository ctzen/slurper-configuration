package com.ctzen.config.spring

import org.springframework.beans.factory.annotation.Value


/**
 * For testing @Value annotations.
 *
 * @author cchang
 */
class Placeheld {

    @Value('${placeheld.name}')
    String name

    @Value('${placeheld.meaning}')
    int meaning

    @Value("#{config.get('placeheld.likes')}")
    Set<String> likes

    @Value('${placeheld.noSuchKey:default}')
    String defa

}
