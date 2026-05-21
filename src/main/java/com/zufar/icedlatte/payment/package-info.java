@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "cart :: api",
                "common :: *",
                "order :: api",
                "order :: exception",
                "product :: api",
                "security :: api"
        }
)
package com.zufar.icedlatte.payment;
