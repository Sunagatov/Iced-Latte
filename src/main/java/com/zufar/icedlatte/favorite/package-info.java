@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "common :: http",
            "product :: api",
            "product :: converter",
            "product :: exception",
            "security :: api"
        })
package com.zufar.icedlatte.favorite;
