package com.ctzen.config.spring

foo = 'I am spring'

bar = 'bar@root'

environments {

    unittest {

        bar = 'bar@unittest'

    }

}

// this is a comment

placeheld {

    aNull = null

    name = 'John'

    meaning = 42

    likes = [ 'comics', 'movies' ] as Set

}