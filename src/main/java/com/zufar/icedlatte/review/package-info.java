@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "common :: *",
                "product :: api",
                "security :: api",
                "user :: api"
        }
)
package com.zufar.icedlatte.review;
