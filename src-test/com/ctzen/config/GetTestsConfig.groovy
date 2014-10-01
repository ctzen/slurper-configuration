package com.ctzen.config

foo = 'I am foo'

bar = 123

foo2 = "${foo} too"

qux = 0
l1 {
    qux = 1
    l2 {
        qux = 2
        l3 {
            qux = 3
        }
    }
}

aNull = null

pojo = new TestPojo(name: 'I am pojo!')