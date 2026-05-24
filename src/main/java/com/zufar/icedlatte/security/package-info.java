@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common :: *", "ratelimit :: api", "user :: api", "user :: exception"})
package com.zufar.icedlatte.security;
