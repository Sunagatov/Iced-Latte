@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "common :: *",
                "ratelimit",
                "user :: api",
                "user :: exception"
        }
)
package com.zufar.icedlatte.security;
