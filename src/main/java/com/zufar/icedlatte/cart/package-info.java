@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "common :: *",
            "product :: api",
            "product :: converter",
            "product :: exception",
            "security :: api"
        })
package com.zufar.icedlatte.cart;
