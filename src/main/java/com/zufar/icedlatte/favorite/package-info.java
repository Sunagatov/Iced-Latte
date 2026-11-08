@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "common :: exception-handler",
            "common :: http",
            "common :: sync",
            "product :: api",
            "security :: api"
        })
package com.zufar.icedlatte.favorite;
